package com.example.moviebot.service;

import com.example.moviebot.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    static final String HELP_TEXT = """
            Я бот помощник по выбору фильмов для просмотра.

            Я буду отправлять вам списки фильмов которые вы можете сегодня посмотреть, для этого просто нажмите в меню 'Получить список фильмов'
                        
            Напишите /start для получения приветсвенного письма.
                        
            Напишите /getmovie для получения списка фильмов.
                        
            Напишите /help для получения информации по использованию бота.
                        
            """;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "получить приветсветнное письмо"));
        listofCommands.add(new BotCommand("/getmovie", "получить список фильмов"));
        listofCommands.add(new BotCommand("/help", "как использовать этого бота"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    int movieId = 0;

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();


            switch (messageText) {
                case "/start":

                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":

                    helpCommandReceived(chatId);
                    break;
                case "/getmovie":
                    getMovieCommandReceived(chatId);
                    break;
                default:

                    sendMessage(chatId, "Извените, я не знаю такие команды \uD83D\uDE22");

            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case "GET_MOVIE_BUTTON" -> getMovieCommandReceived(chatId);
                case "HELP_BUTTON" -> helpCommandReceived(chatId);
                case "BACK_BUTTON" ->
                        startCommandReceived(chatId, update.getCallbackQuery().getMessage().getChat().getFirstName());
                case "GET_MSG_BUTTON" -> sendMovieMessage(chatId,0);
                case "GET_DOC_BUTTON" -> sendMovieFile(chatId);
                case "GET_NEXT_MOVIE_BUTTON" -> {
                    movieId += 1;
                    if (movieId > movieInfo.size() - 1) movieId = 0;
                    sendMovieMessage(chatId, movieId);
                }
                case "GET_PREV_MOVIE_BUTTON" -> {
                    movieId -=1;
                    if (movieId < 0) movieId = movieInfo.size() - 1;
                    sendMovieMessage(chatId, movieId);
                }
            }

        }

    }

    private void getMovieCommandReceived(long chatId) {

        mainParse(chatId);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите способ получения списка фильмов");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        var backButton = new InlineKeyboardButton();
        backButton.setText("\uD83D\uDD19 Вернуться");
        backButton.setCallbackData("BACK_BUTTON");


        var getDocButton = new InlineKeyboardButton();
        getDocButton.setText("\uD83D\uDCDD Получить файл");
        getDocButton.setCallbackData("GET_DOC_BUTTON");

        var getMsgButton = new InlineKeyboardButton();
        getMsgButton.setText("\uD83D\uDCE9 Получить сообщение");
        getMsgButton.setCallbackData("GET_MSG_BUTTON");

        row1.add(backButton);
        row2.add(getMsgButton);
        row2.add(getDocButton);
        rowsInline.add(row2);
        rowsInline.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    private void helpCommandReceived(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(TelegramBot.HELP_TEXT);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        var backButton = new InlineKeyboardButton();
        backButton.setText("\uD83D\uDD19 Вернуться");
        backButton.setCallbackData("BACK_BUTTON");

        row1.add(backButton);
        rowsInline.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Привет, " + name + ". Я телеграм-бот, который любит смотреть фильмы, если хочешь тоже, то выбери соответсвующий пункт в меню!" + " \uD83D\uDE0A");

        log.info("Ответ пользователю: " + name);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        var todayButton = new InlineKeyboardButton();
        todayButton.setText("Получить список фильмов");
        todayButton.setCallbackData("GET_MOVIE_BUTTON");

        var tomorrowButton = new InlineKeyboardButton();
        tomorrowButton.setText("Помощь");
        tomorrowButton.setCallbackData("HELP_BUTTON");

        row1.add(todayButton);
        row1.add(tomorrowButton);
        rowsInline.add(row1);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }


    private void sendDocument(long chatid, InputFile sendFile) {
        SendDocument document = new SendDocument();
        document.setChatId(String.valueOf(chatid));
        document.setDocument(sendFile);
        document.setCaption("Список фильмов на сегодня. \uD83D\uDCDD");
        try {
            execute(document);
            System.out.println("file is sending");
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMovieFile(long chatId) {
        InputFile file = new InputFile();
        File myFile = new File("movies.txt");
        file.setMedia(myFile);
        sendDocument(chatId, file);
    }

    private void sendMessage(long chatId, String textToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    private void sendPicture(long chatId, String photoLink, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(String.valueOf(chatId));
        photo.setCaption(caption);
        InputStream stream = null;
        try {
            stream = new URL(photoLink).openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        photo.setPhoto(new InputFile(stream, photoLink));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        var prevButton = new InlineKeyboardButton();
        prevButton.setText("◀\uFE0F Предыдущий фильм");
        prevButton.setCallbackData("GET_PREV_MOVIE_BUTTON");

        var nextButton = new InlineKeyboardButton();
        nextButton.setText("Следующий фильм ▶\uFE0F");
        nextButton.setCallbackData("GET_NEXT_MOVIE_BUTTON");

        var backButton = new InlineKeyboardButton();
        backButton.setText("\uD83D\uDD19 Вернутся");
        backButton.setCallbackData("GET_MOVIE_BUTTON");

        row1.add(prevButton);
        row1.add(nextButton);
        row2.add(backButton);
        rowsInline.add(row1);
        rowsInline.add(row2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        photo.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(photo);
        } catch (TelegramApiException e) {

        }
    }

    private void sendMovieMessage(long chatId, int movieId) {
        int index = movieInfo.get(movieId).indexOf("\n");
        sendPicture(chatId, movieInfo.get(movieId).substring(0, index), movieInfo.get(movieId).substring(index + 1));
    }

    List<String> movieInfo = new ArrayList<>();

    private void mainParse(long chatId) {
        try {
            movieInfo.clear();
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedDate = today.format(formatter);
            System.out.println(formattedDate);
            // Подключаемся к сайту
            Document doc = connectWithRetry("https://ekt.kinoafisha.info/movies/?date=" + formattedDate);

            // Находим все элементы с классом "blocktitle"
            Elements titleBlock = doc.getElementsByClass("movieList_item movieItem  movieItem-grid grid_cell4 ");
            // Записываем полученные данные в файл
            writeToFile(titleBlock, "movies.txt");
            int i = 0;
            // Проходим по всем найденным элементам
            for (Element Block : titleBlock) {
                // Извлекаем текст заголовка новости
                String image = Block.getElementsByClass("picture_image").attr("data-picture") + "\n";
                String title = "Название фильма: " + Block.getElementsByClass("movieItem_title").text() + ". \n";
                String link = Block.getElementsByClass("movieItem_title").attr("href");
                String subTitle = "Карткое описание: " + Block.getElementsByClass("movieItem_subtitle").text() + ". \n";
                String details = "Жанр, год выпуска, страна издатель: " + Block.getElementsByClass("movieItem_details").text() + ". \n";
                // Выводим заголовок новости
                movieInfo.add(image + title + subTitle + details + link);
                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Функция для подключения к сайту с возможностью переподключения в случае ошибки
    private static Document connectWithRetry(String url) throws IOException {
        int maxRetries = 3;
        int retries = 0;
        while (true) {
            try {
                return Jsoup.connect(url).get();
            } catch (IOException e) {
                retries++;
                if (retries == maxRetries) {
                    throw e;
                }
                System.out.println("Ошибка при подключении к сайту. Попытка переподключения...");
            }
        }
    }

    // Функция для записи данных в файл
    private static void writeToFile(Elements data, String fileName) {
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            for (Element element : data) {
                // Извлекаем текст заголовка новости
                String title = "Название фильма: " + element.getElementsByClass("movieItem_title").text() + ". \n";
                String link = element.getElementsByClass("movieItem_title").attr("href") + "\r\n";
                String subTitle = "Карткое описание: " + element.getElementsByClass("movieItem_subtitle").text() + ". \n";
                String details = "Жанр, год выпуска, страна издатель: " + element.getElementsByClass("movieItem_details").text() + ". \n";
                // Выводим заголовок новости
                fileWriter.write(title + subTitle + details + link + "\n");
            }
            System.out.println("Данные успешно записаны в файл " + fileName);
        } catch (IOException e) {
            System.out.println("Ошибка при записи данных в файл: " + e.getMessage());
        }
    }
}