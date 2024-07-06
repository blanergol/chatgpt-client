package com.chatgpt_client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ChatGPTClient extends Application {

    private static final String CONFIG_FILE = "config.ini";
    private static final String CHATS_FILE = "chats.json";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODELS_URL = "https://api.openai.com/v1/models";
    private String apiKey = "";
    private String model = "gpt-3.5-turbo"; // Устанавливаем модель по умолчанию
    private final OkHttpClient httpClient = new OkHttpClient();
    private VBox outputArea;
    private TextArea inputField;
    private ComboBox<String> modelComboBox;
    private List<Chat> chats = new ArrayList<>();
    private ListView<Chat> chatListView;
    private Chat currentChat;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ChatGPT Client");

        loadConfig();

        BorderPane mainPane = new BorderPane();
        Scene scene = new Scene(mainPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        // Чаты
        chatListView = new ListView<>();
        chatListView.setPrefWidth(200);
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldChat, newChat) -> {
            if (newChat != null) {
                loadChat(newChat);
            }
        });
        Button newChatButton = new Button("Новый чат");
        newChatButton.setOnAction(e -> createNewChat());
        Button editChatButton = new Button("Изменить чат");
        editChatButton.setOnAction(e -> editChat());
        Button deleteChatButton = new Button("Удалить чат");
        deleteChatButton.setOnAction(e -> deleteChat());

        VBox chatBox = new VBox(10, new Label("Чаты"), chatListView, newChatButton, editChatButton, deleteChatButton);
        chatBox.setPadding(new Insets(10));
        mainPane.setLeft(chatBox);

        inputField = new TextArea();
        inputField.setPromptText("Введите ваш запрос");
        inputField.setPrefWidth(900);
        inputField.setPrefHeight(80);

        Button sendButton = new Button("Отправить");
        sendButton.setOnAction(event -> {
            String userInput = inputField.getText();
            sendRequest(userInput);
            inputField.clear();
        });

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));
        inputBox.setStyle("-fx-alignment: center;");

        HBox centeredBox = new HBox(inputBox);
        centeredBox.setPadding(new Insets(10, 10, 10, 10));
        centeredBox.setStyle("-fx-alignment: center;");
        BorderPane.setMargin(centeredBox, new Insets(0, 80, 0, 80));

        outputArea = new VBox();
        outputArea.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(outputArea);
        scrollPane.setFitToWidth(true);

        BorderPane chatPane = new BorderPane();
        chatPane.setCenter(scrollPane);
        chatPane.setBottom(centeredBox);

        mainPane.setCenter(chatPane);

        MenuBar menuBar = new MenuBar();

        // Настройки меню
        Menu settingsMenu = new Menu("Настройки");
        MenuItem apiTokenMenuItem = new MenuItem("Параметры");
        apiTokenMenuItem.setOnAction(e -> showSettingsWindow());
        settingsMenu.getItems().add(apiTokenMenuItem);

        // Файл меню
        Menu fileMenu = new Menu("Файл");
        MenuItem saveMenuItem = new MenuItem("Сохранить чаты");
        saveMenuItem.setOnAction(e -> saveChatsToFile());
        MenuItem closeMenuItem = new MenuItem("Закрыть");
        closeMenuItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(saveMenuItem, closeMenuItem);

        // О нас меню
        Menu aboutMenu = new Menu("О нас");
        MenuItem aboutMenuItem = new MenuItem("О программе");
        aboutMenuItem.setOnAction(e -> showAboutWindow());
        aboutMenu.getItems().add(aboutMenuItem);

        menuBar.getMenus().addAll(fileMenu, settingsMenu, aboutMenu);
        mainPane.setTop(menuBar);

        primaryStage.show();

        // Загружаем чаты после инициализации интерфейса
        loadChatsFromFile();
    }

    private void createNewChat() {
        TextInputDialog dialog = new TextInputDialog("Новый чат");
        dialog.setTitle("Создать новый чат");
        dialog.setHeaderText("Введите название нового чата:");
        dialog.setContentText("Название:");
        dialog.showAndWait().ifPresent(chatName -> {
            Chat newChat = new Chat(chatName);
            chats.add(newChat);
            chatListView.getItems().add(newChat);
            chatListView.getSelectionModel().select(newChat);
            saveChatsToFile();
        });
    }

    private void editChat() {
        if (currentChat == null) {
            showAlert("Ошибка", "Выберите чат для редактирования.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(currentChat.getName());
        dialog.setTitle("Изменить чат");
        dialog.setHeaderText("Введите новое название чата:");
        dialog.setContentText("Название:");
        dialog.showAndWait().ifPresent(chatName -> {
            currentChat.setName(chatName);
            chatListView.refresh();
            saveChatsToFile();
        });
    }

    private void deleteChat() {
        if (currentChat == null) {
            showAlert("Ошибка", "Выберите чат для удаления.");
            return;
        }
        chats.remove(currentChat);
        chatListView.getItems().remove(currentChat);
        currentChat = null;
        outputArea.getChildren().clear();
        saveChatsToFile();
    }

    private void loadChat(Chat chat) {
        currentChat = chat;
        outputArea.getChildren().clear();
        for (String message : chat.getMessages()) {
            Label messageLabel = new Label(message);
            outputArea.getChildren().add(messageLabel);
            outputArea.getChildren().add(new Separator());
        }
    }

    private void sendRequest(String userInput) {
        Label userQueryLabel = new Label("Запрос: " + userInput);
        userQueryLabel.setStyle("-fx-font-weight: bold;");
        outputArea.getChildren().add(userQueryLabel);
        currentChat.addMessage("Запрос: " + userInput);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", model);
        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", userInput);
        messagesArray.put(messageObject);
        jsonBody.put("messages", messagesArray);
        jsonBody.put("max_tokens", 150);

        String json = jsonBody.toString();

        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Ошибка: " + e.getMessage());
                    outputArea.getChildren().add(errorLabel);
                    currentChat.addMessage("Ошибка: " + e.getMessage());
                    saveChatsToFile();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    String message = choices.getJSONObject(0).getJSONObject("message").getString("content");

                    Platform.runLater(() -> {
                        HBox responseBox = createResponseBox(message);
                        outputArea.getChildren().add(responseBox);
                        currentChat.addMessage("Ответ: " + message);
                        outputArea.getChildren().add(new Separator());
                        saveChatsToFile();
                    });
                } else {
                    String responseData = response.body().string();
                    Platform.runLater(() -> {
                        Label errorLabel = new Label("Ошибка: " + response.message() + "\nДетали ошибки: " + responseData);
                        outputArea.getChildren().add(errorLabel);
                        currentChat.addMessage("Ошибка: " + response.message() + "\nДетали ошибки: " + responseData);
                        outputArea.getChildren().add(new Separator());
                        saveChatsToFile();
                    });
                }
            }
        });
    }

    private HBox createResponseBox(String message) {
        Label responseLabel = new Label("Ответ: " + message);
        Button copyButton = new Button("Копировать");
        copyButton.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(message);
            clipboard.setContent(content);
        });

        HBox responseBox = new HBox(10, responseLabel, copyButton);
        responseBox.setPadding(new Insets(5));
        responseBox.setStyle("-fx-alignment: baseline-left;");
        return responseBox;
    }

    private void showSettingsWindow() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setTitle("Настройки");

        TextField apiKeyField = new TextField(apiKey);
        apiKeyField.setPromptText("Введите ваш API Token");

        modelComboBox = new ComboBox<>();
        modelComboBox.setValue(model);
        fetchModels();

        Button saveButton = new Button("Сохранить");
        saveButton.setOnAction(event -> {
            apiKey = apiKeyField.getText();
            model = modelComboBox.getValue();
            saveConfig(apiKey, model);
            settingsStage.close();
        });

        VBox vbox = new VBox(10, new Label("API Token:"), apiKeyField, new Label("Модель:"), modelComboBox, saveButton);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox, 450, 300);
        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    private void fetchModels() {
        Request request = new Request.Builder()
                .url(MODELS_URL)
                .header("Authorization", "Bearer " + apiKey)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка");
                    alert.setHeaderText("Не удалось загрузить модели");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray models = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        modelComboBox.getItems().clear();
                        for (int i = 0; i < models.length(); i++) {
                            JSONObject model = models.getJSONObject(i);
                            modelComboBox.getItems().add(model.getString("id"));
                        }
                    });
                } else {
                    String responseData = response.body().string();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Ошибка");
                        alert.setHeaderText("Не удалось загрузить модели");
                        alert.setContentText(responseData);
                        alert.showAndWait();
                    });
                }
            }
        });
    }

    private void loadChatsFromFile() {
        try {
            if (Files.exists(Paths.get(CHATS_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(CHATS_FILE)));
                JSONArray jsonChats = new JSONArray(content);

                chats.clear();
                chatListView.getItems().clear();
                for (int i = 0; i < jsonChats.length(); i++) {
                    JSONObject jsonChat = jsonChats.getJSONObject(i);
                    Chat chat = new Chat(jsonChat.getString("name"));
                    JSONArray messages = jsonChat.getJSONArray("messages");
                    for (int j = 0; j < messages.length(); j++) {
                        chat.addMessage(messages.getString(j));
                    }
                    chats.add(chat);
                    chatListView.getItems().add(chat);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось загрузить чаты: " + e.getMessage());
        }
    }

    private void saveChatsToFile() {
        JSONArray jsonChats = new JSONArray();
        for (Chat chat : chats) {
            JSONObject jsonChat = new JSONObject();
            jsonChat.put("name", chat.getName());
            jsonChat.put("messages", new JSONArray(chat.getMessages()));
            jsonChats.put(jsonChat);
        }

        try {
            Files.write(Paths.get(CHATS_FILE), jsonChats.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось сохранить чаты: " + e.getMessage());
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                properties.load(Files.newBufferedReader(Paths.get(CONFIG_FILE)));
                apiKey = properties.getProperty("apiKey", "");
                model = properties.getProperty("model", "gpt-3.5-turbo");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig(String apiKey, String model) {
        Properties properties = new Properties();
        properties.setProperty("apiKey", apiKey);
        properties.setProperty("model", model);
        try {
            properties.store(Files.newBufferedWriter(Paths.get(CONFIG_FILE)), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAboutWindow() {
        Stage aboutStage = new Stage();
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.setTitle("О программе");

        Label authorLabel = new Label("Автор: Иван Иванов");
        Label versionLabel = new Label("Версия: 1.0");
        Label yearLabel = new Label("Год: 2023");
        Label copyrightLabel = new Label("© 2023 Иван Иванов. Все права защищены.");

        VBox vbox = new VBox(10, authorLabel, versionLabel, yearLabel, copyrightLabel);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox, 300, 200);
        aboutStage.setScene(scene);
        aboutStage.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class Chat {
        private String name;
        private final List<String> messages = new ArrayList<>();

        public Chat(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void addMessage(String message) {
            messages.add(message);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
