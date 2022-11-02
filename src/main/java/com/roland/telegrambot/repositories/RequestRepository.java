package com.roland.telegrambot.repositories;

import com.roland.telegrambot.models.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Roland Pilpani 25.10.2022
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Integer> {
}
