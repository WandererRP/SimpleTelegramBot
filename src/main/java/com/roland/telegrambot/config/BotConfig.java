package com.roland.telegrambot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Roland Pilpani 22.10.2022
 */
//Класс для получение параметров бота из файла
@Configuration
@PropertySource("classpath:application.properties")
public class BotConfig {
    @Value("${bot.token}")
    String BOT_TOKEN;
    @Value("${bot.username}")
    String BOT_NAME;
    public String getBOT_TOKEN() {
        return BOT_TOKEN;
    }

    public String getBOT_NAME() {
        return BOT_NAME;
    }

    public void setBOT_TOKEN(String BOT_TOKEN) {
        this.BOT_TOKEN = BOT_TOKEN;
    }

    public void setBOT_NAME(String BOT_NAME) {
        this.BOT_NAME = BOT_NAME;
    }
}
