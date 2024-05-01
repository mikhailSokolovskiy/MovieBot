package com.example.moviebot.service;

import com.example.moviebot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    static final String HELP_TEXT = """
            Я бот помощник по выбору фильмов для просмотра.

            Я буду отправлять вам списки фильмов которые вы можете сегодня посмотреть, для этого просто нажмите в меню 'Получить список фильмов'
            
            Напишите /start для получения приветсвенного письма.
            
            Напишите /mydata для получения своих песональных данных.
            
            Напишите /deletedata для удаления своих персональных данных.
            
            Напишите /getmovie для получения списка фильмов.
            
            Напишите /help для получения информации по использованию бота.
            
            Напишите /settings для установки своих предпочтений при выборе фильмов.
            """;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "получить приветсветнное письмо"));
        listofCommands.add(new BotCommand("/mydata", "получить свои персональные данные"));
        listofCommands.add(new BotCommand("/deletedata", "удалить свои персональные данные"));
        listofCommands.add(new BotCommand("/getmovie", "получить список фильмов"));
        listofCommands.add(new BotCommand("/help", "как использовать этого бота"));
        listofCommands.add(new BotCommand("/settings", "установить свои предпочтения"));
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

                    startCommandRecived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":

                    sendMessage(chatId, HELP_TEXT);
                    break;
                default:

                    sendMessage(chatId, "Извените, я не знаю такие команды :(");

            }
        }

    }

    private void startCommandRecived(long chatId, String name) {

        String answer = "Привет, " + name + ". Я телеграм-бот, который любит смотреть фильмы, если хочешь тоже, то выбери соответсвующий пункт в меню!";

        log.info("Ответ пользователю: " + name);
        sendMessage(chatId, answer);
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
}