package com.roland.telegrambot;

import com.roland.telegrambot.config.BotConfig;
import com.roland.telegrambot.models.Request;
import com.roland.telegrambot.services.PeopleService;
import com.roland.telegrambot.services.RequestService;
import com.roland.telegrambot.util.BotState;
import com.roland.telegrambot.util.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roland Pilpani 21.10.2022
 */

@Component
public class Bot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final PeopleService peopleService;
    private final RequestService requestService;


    //Определение текстовых команд, которые сможет различать бот
    private final String KAZ = "Қазақ тілінде";
    private final String RUS = "На русском языке";
    private final String ABOUT_COMPANY_RU = "О компании";
    private final String ABOUT_COMPANY_KZ = "Компания туралы";
    private final String ABOUT_US_RU = "О нас";
    private final String ABOUT_US_KZ = "Біз туралы";
    private final String COMPANY_NEWS_RU = "Новости компании";
    private final String COMPANY_NEWS_KZ = "Компания жаңалықтары";
    private final String ABOUT_CHAT_BOTS_RU = "О чат-ботах";
    private final String ABOUT_CHAT_BOTS_KZ = "Чат бот туралы";
    private final String REQUEST_CALL_RU = "Заказать обратный звонок";
    private final String REQUEST_CALL_KZ = "Кері қоңырауды сұрау";

    private final String MAIN_MENU_RU = "Главное меню";
    private final String MAIN_MENU_KZ = "Басты мәзір";




    //Определение патерна, для распознования имени пользователя и номера телефона при запросе обратного звонка.
    //Полученные данные будут добавляться в БД
    private static final Pattern pattern;
    static
    {
    pattern = Pattern.compile("([а-яА-ЯёЁa-zA-ZҚқӘәІіҢңҒғҰұӨөҮү-]+ ?[а-яА-ЯёЁa-zA-ZҚқӘәІіҢңҒғҰұӨөҮү-]+ ?[а-яА-ЯёЁa-zA-ZҚқӘәІіҢңҒғҰұӨөҮү-]+) ?((\\+7|7|8)\\d{10})");
    }



    @Autowired
    public Bot(BotConfig config, PeopleService peopleService, RequestService requestService) {
        this.config = config;
        this.peopleService = peopleService;
        this.requestService = requestService;
    }

    //Стандартные методы телеграм бота, которые должны быть переобределены. Данные для подключения телеграм бота зранятся
    //в application/properties
    @Override
    public String getBotUsername() {
        return config.getBOT_NAME();
    }

    @Override
    public String getBotToken() {
        return config.getBOT_TOKEN();
    }

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage sendMessage = handleUpdate(update);
        if(sendMessage!=null) executeMessage(sendMessage);
    }


    //Метод, в котором заключена основная логика бота
    private SendMessage handleUpdate(Update update) {
        SendMessage replyMessage = null;

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String messageText = message.getText();
            long chatId = message.getChatId();
            BotState botState;


            //При введении команды /start пользователь добавляется в БД, и бот переводится состояние запроса имени пользователя
            if (messageText.equals("/start")) {
                botState = BotState.ASK_NAME;
                peopleService.setCurrentBotState(chatId, botState);

            } else {
                //при дальнейшем использовании бота, управление осуществляется по карте состояний бота.
                botState = peopleService.getCurrentBotState(chatId);
            }

            //Обработка полученного сообщения и подготовка ответа
            replyMessage = processInputMessage(botState, message);
        }

        return replyMessage;

    }

    private SendMessage processInputMessage(BotState botState, Message message) {
        SendMessage replyMessage = new SendMessage();
        String messageText = message.getText();

        //Если сообщение, введенное пользователем, не будет обработанно в ниже приведенных условиях,
        // будет выдан текст "Неизвестная Команда"
        String replyMessageText = "Неизвестная Команда";
        long chatId = message.getChatId();


        //Основная логика обработки сообщения
        if (botState.equals(BotState.ASK_NAME)) {
            replyMessageText = "Введите ваше имя?/" +
                    "\nАтыңызды енгізіңіз бе?";
            peopleService.setCurrentBotState(chatId, BotState.ASK_PHONE_NUMBER);
        }

        else if (botState.equals(BotState.ASK_PHONE_NUMBER)) {

            peopleService.setCurrentPersonName(chatId, messageText);//Добавление имени в БД.
            replyMessageText = "Введите номер телефона?" +
                    "\nТелефон нөмірін енгізіңіз бе?";
            peopleService.setCurrentBotState(chatId, BotState.ASK_LANGUAGE_AND_FILL_NUMBER);
        }

        else if (botState.equals(BotState.ASK_LANGUAGE_AND_FILL_NUMBER)) {
            peopleService.setCurrentPersonPhone(chatId, messageText);//Добавление номера в БД.
            replyMessageText = "Вас приветствует онлайн-консультант! Просим Вас выбрать язык интерфейса." +
                    "\nОнлайн кеңесшісіне қош келдіңіз! Интерфейс тілін таңдаңыз";
            prepareLanguageButtons(replyMessage);//Подготовка кнопок для выбора языка
            peopleService.setCurrentBotState(chatId, BotState.DETECT_LANGUAGE);
        }

        else if (botState.equals(BotState.ASK_LANGUAGE)) {
            //Данное условие не подключено. Можно использовать в дальнейшем для смены языка интерфейса
            prepareLanguageButtons(replyMessage);//Подготовка кнопок для выбора языка
            peopleService.setCurrentBotState(chatId, BotState.DETECT_LANGUAGE);

        }

        else if (botState.equals(BotState.DETECT_LANGUAGE)) {//Определяем выбранный язык и отправляем нужное меню
            if (messageText.equals(RUS)) {
                peopleService.setCurrentBotState(chatId, BotState.ANSWER_QUESTIONS);
                prepareMenuButtons(replyMessage, Language.RU);
                replyMessageText = "Главное меню";
            } else if (messageText.equals(KAZ)) {
                peopleService.setCurrentBotState(chatId, BotState.ANSWER_QUESTIONS);
                prepareMenuButtons(replyMessage, Language.KZ);
                replyMessageText = "Басты мәзір";
            } else {
                replyMessage.setChatId(chatId);
                replyMessage.setText("Просим Вас выбрать язык интерфейса");
                prepareLanguageButtons(replyMessage);
                return replyMessage;
            }
        }


        else if (botState.equals(BotState.HANDLE_REQUEST_CALL_RU)) {//Обработка запроса о звонке на русском языке
            if(messageText.equalsIgnoreCase("отмена")){
                peopleService.setCurrentBotState(chatId, BotState.ANSWER_QUESTIONS);
                prepareMenuButtons(replyMessage, Language.RU);
                replyMessageText = "Главное меню";
            }
            else {

                Matcher matcher = pattern.matcher(messageText);
                String name;
                String phoneNumber;

                if (matcher.find()) {
                    name = matcher.group(1);
                    phoneNumber = matcher.group(2);
                    System.out.println(name + "" + phoneNumber);
                    Request request = new Request(name, phoneNumber);
                    requestService.saveRequest(request);
                    peopleService.setCurrentBotState(chatId, BotState.ANSWER_QUESTIONS);
                    prepareMenuButtons(replyMessage, Language.RU);

                    replyMessageText = "Мы позвоним вам в ближайшее время!";

                } else {
                    replyMessageText = "Отправьте имя и номер одним сообщением или напишите \"Отмена\"";
                }
            }
        }

        else if (botState.equals(BotState.HANDLE_REQUEST_CALL_KZ)) { //Обработка запроса о звонке на казахском языке
            if(messageText.equalsIgnoreCase("болдырмау")){
                peopleService.setCurrentBotState(chatId, BotState.ANSWER_QUESTIONS);
                prepareMenuButtons(replyMessage, Language.RU);
                replyMessageText = "Главное меню";
            }
            else {

                Matcher matcher = pattern.matcher(messageText);
                String name;
                String phoneNumber;

                if (matcher.find()) {
                    name = matcher.group(1);
                    phoneNumber = matcher.group(2);
                    System.out.println(name + "" + phoneNumber);
                    Request request = new Request(name, phoneNumber);
                    requestService.saveRequest(request);
                    peopleService.setCurrentBotState(chatId, BotState.ANSWER_QUESTIONS);
                    prepareMenuButtons(replyMessage, Language.KZ);

                    replyMessageText = "Біз сізге жақын арада қоңырау шаламыз!";

                } else {
                    replyMessageText = "Аты мен нөмірін бір хабарламада жіберіңіз немесе «Болдырмау» сөзін жазыңыз.";
                }
            }

        }

        else if (botState.equals(BotState.ANSWER_QUESTIONS)) {//После регистрации, попадаем в этот раздел. Здесь бот отвечает на вопросы
            switch (messageText) {
                case ABOUT_COMPANY_RU:
                    replyMessageText = "Выберите нужное";
                    prepareCompanyInfoButtons(replyMessage, Language.RU);
                    break;
                case ABOUT_COMPANY_KZ:
                    replyMessageText = "Керектіні таңдаңыз";
                    prepareCompanyInfoButtons(replyMessage, Language.KZ);
                    break;
                case ABOUT_US_RU:
                    replyMessageText =
                            "Компания  занимается разработкой подсистемного программного обеспечения для мессенджера Telegram. Мы реализовали более 50 крупных проектов для 10 отраслей экономики как, государственные организации, национальные компании.\n" +
                                    "Е-mail: pilpani.roland@gmail.com,\n" +
                                    "Контакты: 8 747 558 17 47 Роланд\n";
                    break;
                case ABOUT_US_KZ:
                    replyMessageText = "Telegram мессенджері үшін ішкі жүйе бағдарламалық құралын жасауда. Біз мемлекеттік ұйымдар, ұлттық компаниялар сияқты экономиканың 10 секторы үшін 50-ден астам ірі жобаны жүзеге асырдық.\n" +
                            "Электрондық пошта: pilpani.roland@gmail.com,\n" +
                            "Байланыс телефоны: 8 747 558 17 47 Роланд";
                    break;
                case COMPANY_NEWS_RU:
                case COMPANY_NEWS_KZ:
                    replyMessageText = "https://habr.com/ru/news/";
                    break;
                case ABOUT_CHAT_BOTS_RU:
                    replyMessageText = "Чат-бот или другими словами «Виртуальный ассистент» — это универсальный менеджер который работает 24 часа в сутки, 7 дней в неделю и выполняет следующие задачи:\n" +
                            "Разгрузит контактный центр до 80% и ответит на стандартные вопросы \n" +
                            "Организует и проконтролирует работу ваших сотрудников\n" +
                            "Управляет счетами, заказами и доставкой\n" +
                            "Презентует ваш продукт клиенту, поможет оформить заказ и оплатить его онлайн\n" +
                            "Интеграция со всеми популярными CRM системами\n";
                    break;
                case ABOUT_CHAT_BOTS_KZ:
                    replyMessageText = "Чатбот немесе басқаша айтқанда «Виртуалды көмекші» - бұл тәулігіне 24 сағат, аптасына 7 күн жұмыс істейтін және келесі тапсырмаларды орындайтын әмбебап менеджер:\n" +
                            "Байланыс орталығын 80% дейін жүктеп, стандартты сұрақтарға жауап беріңіз.\n" +
                            "Қызметкерлеріңіздің жұмысын ұйымдастырыңыз және бақылаңыз\n" +
                            "Шот-фактураларды, тапсырыстарды және жеткізуді басқарады.\n" +
                            "Тауарыңызды клиентке ұсынады, тапсырыс беруге және оны онлайн төлеуге көмектеседі.\n" +
                            "Барлық танымал CRM жүйелерімен біріктіру.";
                    break;
                case MAIN_MENU_RU:
                    prepareMenuButtons(replyMessage, Language.RU);
                    replyMessageText = "Главное меню";
                    break;
                case MAIN_MENU_KZ:
                    prepareMenuButtons(replyMessage, Language.KZ);
                    replyMessageText = "Басты мәзір";
                    break;
                case REQUEST_CALL_RU:
                    peopleService.setCurrentBotState(chatId, BotState.HANDLE_REQUEST_CALL_RU);
                    replyMessageText = "Если Вы хотите заказать «Обратный звонок» просим написать Ваше имя и номер контактного телефона";
                    removeKeyboard(replyMessage);
                    break;
                case REQUEST_CALL_KZ:
                    peopleService.setCurrentBotState(chatId, BotState.HANDLE_REQUEST_CALL_KZ);
                    replyMessageText = "«Кері қоңырауға» тапсырыс бергіңіз келсе, аты-жөніңізді және байланыс телефоныңызды жазыңыз";
                    removeKeyboard(replyMessage);
                    break;


            }
        }


        replyMessage.setChatId(chatId);
        replyMessage.setText(replyMessageText);

        return replyMessage;
    }



    //Подготовка кнопок главного меню
    private void prepareMenuButtons(SendMessage sendMessage, Language lang) {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(lang.equals(Language.RU) ? ABOUT_COMPANY_RU : ABOUT_COMPANY_KZ));
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(lang.equals(Language.RU) ? ABOUT_CHAT_BOTS_RU : ABOUT_CHAT_BOTS_KZ));
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(lang.equals(Language.RU) ? REQUEST_CALL_RU : REQUEST_CALL_KZ));
        keyboardRows.add(keyboardRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(keyboardMarkup);
    }


    //Подготовка кнопок для вопросов о компании
    private void prepareCompanyInfoButtons(SendMessage sendMessage, Language lang) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(lang.equals(Language.RU) ? ABOUT_US_RU : ABOUT_US_KZ));
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(lang.equals(Language.RU) ? COMPANY_NEWS_RU : COMPANY_NEWS_KZ));
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(lang.equals(Language.RU) ? MAIN_MENU_RU : MAIN_MENU_KZ));
        keyboardRows.add(keyboardRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(keyboardMarkup);
    }


    //Подготовка кнопок для выбора языка
    private void prepareLanguageButtons(SendMessage sendMessage) {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(KAZ));
        keyboardRow.add(new KeyboardButton(RUS));

        keyboardRows.add(keyboardRow);
        keyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(keyboardMarkup);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void removeKeyboard(SendMessage sendMessage){
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);
        replyKeyboardRemove.setSelective(false);

        sendMessage.setReplyMarkup(replyKeyboardRemove);

    }





}

