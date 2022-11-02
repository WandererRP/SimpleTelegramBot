package com.roland.telegrambot.repositories;

import com.roland.telegrambot.models.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Roland Pilpani 22.10.2022
 */
@Repository
public interface PeopleRepository extends JpaRepository<Person, Long> {

}
