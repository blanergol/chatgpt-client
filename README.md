
# ChatGPT Клиент | ChatGPT Client

Это JavaFX приложение, которое позволяет взаимодействовать с ChatGPT через OpenAI API. Приложение поддерживает создание, редактирование, удаление и сохранение чатов локально в файл.

### Функции

- Ввод и отправка запросов в ChatGPT
- Получение ответов от ChatGPT и отображение их в интерфейсе
- Создание, редактирование и удаление чатов
- Сохранение чатов в файл и загрузка из файла
- Настройка API токена и модели через графический интерфейс
- Копирование ответов в буфер обмена

### Требования

- Java 11 или выше
- Maven 3.6.0 или выше

### Установка и запуск

1. Клонируйте репозиторий:
   ```sh
   git clone https://github.com/blanergol/chatgpt-client.git
   cd chatgpt-client
   ```

2. Убедитесь, что у вас установлен Java 11 или выше и Maven.

3. Запустите проект:
   ```sh
   mvn clean javafx:run
   ```

### Конфигурация

Приложение использует файл `config.ini` для хранения API токена и выбранной модели. Этот файл автоматически создается при первом запуске приложения.

### Структура проекта

- `src/main/java/com/chatgpt_client/ChatGPTClient.java` - основной класс приложения
- `config.ini` - файл конфигурации для API токена и модели
- `chats.json` - файл для хранения чатов

### Лицензия

Этот проект лицензирован под MIT License.

---

This is a JavaFX application that allows interaction with ChatGPT via the OpenAI API. The application supports creating, editing, deleting, and saving chats locally to a file.

### Features

- Input and send requests to ChatGPT
- Receive and display responses from ChatGPT
- Create, edit, and delete chats
- Save chats to and load from a file
- Configure API token and model via a graphical interface
- Copy responses to the clipboard

### Requirements

- Java 11 or higher
- Maven 3.6.0 or higher

### Installation and Running

1. Clone the repository:
   ```sh
   git clone https://github.com/blanergol/chatgpt-client.git
   cd chatgpt-client
   ```

2. Ensure you have Java 11 or higher and Maven installed.

3. Run the project:
   ```sh
   mvn clean javafx:run
   ```

### Configuration

The application uses the `config.ini` file to store the API token and selected model. This file is automatically created on the first run of the application.

### Project Structure

- `src/main/java/com/chatgpt_client/ChatGPTClient.java` - main application class
- `config.ini` - configuration file for API token and model
- `chats.json` - file for storing chats

### License

This project is licensed under the MIT License.
