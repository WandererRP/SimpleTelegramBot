package com.roland.telegrambot.models;

import com.roland.telegrambot.util.BotState;
import com.roland.telegrambot.util.Language;

import javax.persistence.*;

/**
 * @author Roland Pilpani 22.10.2022
 */
@Entity
@Table
public class Person {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "bot_state")
    @Enumerated(EnumType.STRING)
    private BotState botState;

    public Person() {
    }

    public Person(long id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }
}
