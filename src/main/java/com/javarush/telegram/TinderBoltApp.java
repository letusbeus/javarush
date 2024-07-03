package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = Data.TG_BOT_NAME; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = Data.TG_BOT_TOKEN; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = Data.OPEN_AI_TOKEN; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();

    private UserInfo me;
    private UserInfo partner;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);
            showMainMenu(Data.MAIN_MENU_COMMANDS); // вынесены в отдельный файл
            return;
        }

        // command GPT
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }
        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");
            Message msg = sendTextMessage("_Подождите пару секунд - ChatGPT думает..._");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }

        // command DATE
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text, Data.DATE_BUTTONS); // вынесены в отдельный файл
            return;
        }
        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор!\nПригласите вашего партнера на свидание за 5 сообщений :)");

                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("_Подождите пару секунд, Ваш собеседник набирает ответ..._");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        // command MESSAGE
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage(loadMessage("message"),
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }
        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);
                Message msg = sendTextMessage("_Подождите пару секунд - ChatGPT думает..._");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer);
                return;
            }
            list.add(message);
            return;
        }

        // command PROFILE
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            sendTextMessage(loadMessage("profile"));

            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Как вас зовут?");
            return;
        }
        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    me.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько вам лет?");
                    return;
                case 2:
                    me.age = message;
                    questionCount = 3;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 3:
                    me.occupation = message;
                    questionCount = 4;
                    sendTextMessage("У вас есть хобби?");
                    return;
                case 4:
                    me.hobby = message;
                    questionCount = 5;
                    sendTextMessage("Что вам не нравится в людях?");
                    return;
                case 5:
                    me.annoys = message;
                    questionCount = 6;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 6:
                    me.goals = message;

                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");

                    Message msg = sendTextMessage("_Подождите пару секунд - ChatGPT думает..._");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }

        // command OPENER
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            sendTextMessage(loadMessage("opener"));
            partner = new UserInfo();
            questionCount = 1;
            sendTextMessage("Как зовут вашего собеседника?");
            return;
        }
        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    partner.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько лет вашему собеседнику?");
                    return;
                case 2:
                    partner.age = message;
                    questionCount = 3;
                    sendTextMessage("Какие хобби у вашего собеседника?");
                    return;
                case 3:
                    partner.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Кем работает ваш собеседник?");
                    return;
                case 4:
                    partner.occupation = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    partner.goals = message;

                    String aboutFriend = partner.toString();
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage("_Подождите пару секунд - ChatGPT думает..._");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }

            return;
        }

        sendTextMessage("*Привет!*");
        sendTextMessage("_Привет!_");

        sendTextMessage("Вы написали: " + message);
        sendTextButtonsMessage("Выберите режим работы: ", "Старт", "start", "Стоп", "stop");

    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
