package com.example.moviebot.service;

import com.example.moviebot.config.BotConfig;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.FileWriter;
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
                case "BACK_BUTTON" -> startCommandReceived(chatId, update.getCallbackQuery().getMessage().getChat().getFirstName());
                case "GET_MSG_BUTTON" -> mainParse();
            }

        }

    }

    private void getMovieCommandReceived(long chatId){

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

    private void helpCommandReceived(long chatId){

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

    private void sendMessage(long chatId, String textToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {

        }
    }

    private void mainParse(){
        try {
            // Подключаемся к сайту
            Document doc = connectWithRetry("https://ekt.kinoafisha.info/movies/?date=2024-05-05");

            // Находим все элементы с классом "blocktitle"
            Elements newsBlocks = doc.getElementsByClass("movieItem_title");

            // Записываем полученные данные в файл
            writeToFile(newsBlocks, "movies.txt");

            // Проходим по всем найденным элементам
            for (Element newsBlock : newsBlocks) {
                // Извлекаем текст заголовка новости
                String title = newsBlock.text();

                // Выводим заголовок новости
                log.info("Название фильма: " + title);
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
                fileWriter.write("Название новости: " + element.text() + "\n");
            }
            System.out.println("Данные успешно записаны в файл " + fileName);
        } catch (IOException e) {
            System.out.println("Ошибка при записи данных в файл: " + e.getMessage());
        }
    }
}