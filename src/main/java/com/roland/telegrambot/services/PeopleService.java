package com.roland.telegrambot.services;

import com.roland.telegrambot.models.Person;
import com.roland.telegrambot.repositories.PeopleRepository;
import com.roland.telegrambot.util.BotState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author Roland Pilpani 22.10.2022
 */
@Service
@Transactional(readOnly = true)
public class PeopleService {
    private final PeopleRepository peopleRepository;


    @Autowired
    public PeopleService(PeopleRepository peopleRepository) {
        this.peopleRepository = peopleRepository;
    }


    @Transactional(readOnly = true)
    public BotState getCurrentBotState(long chatId) {

        Person person;

        Optional<Person> personOptional = peopleRepository.findById(chatId);
        if(personOptional.isPresent()){
            return personOptional.get().getBotState();
        }else return BotState.ASK_NAME;
    }


    @Transactional
    public void setCurrentBotState(long chatId, BotState botState) {
        Person person;

        Optional<Person> personOptional = peopleRepository.findById(chatId);
        if (personOptional.isPresent()){
            person = personOptional.get();
        }
        else {
            person = new Person();
            person.setId(chatId);
        }

        person.setBotState(botState);
        peopleRepository.save(person);

    }

    @Transactional
    public void setCurrentPersonName(long chatId, String messageText) {
        Optional<Person> personOptional = peopleRepository.findById(chatId);
        if (personOptional.isPresent()){
            Person person = personOptional.get();
            person.setName(messageText);
            peopleRepository.save(person);
        }
    }

    @Transactional
    public void setCurrentPersonPhone(long chatId, String messageText) {
        Optional<Person> personOptional = peopleRepository.findById(chatId);
        if (personOptional.isPresent()){
            Person person = personOptional.get();
            person.setPhone(messageText);
            peopleRepository.save(person);
        }
    }
}
