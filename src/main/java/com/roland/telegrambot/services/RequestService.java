package com.roland.telegrambot.services;

import com.roland.telegrambot.models.Request;
import com.roland.telegrambot.repositories.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author Roland Pilpani 25.10.2022
 */
@Service
public class RequestService {
    private final RequestRepository requestRepository;

    @Autowired
    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public void saveRequest(Request request) {
        request.setCreationTime(LocalDateTime.now());
        requestRepository.save(request);
    }
}
