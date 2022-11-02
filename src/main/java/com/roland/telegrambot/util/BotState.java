package com.roland.telegrambot.util;

/**
 * @author Roland Pilpani 23.10.2022
 */
public enum BotState {
    ASK_NAME,
    ASK_PHONE_NUMBER,
    ASK_LANGUAGE,
    ASK_LANGUAGE_AND_FILL_NUMBER,
    DETECT_LANGUAGE,
    ANSWER_QUESTIONS,
    HANDLE_REQUEST_CALL_RU,
    HANDLE_REQUEST_CALL_KZ
}
