package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    @GetMapping
    public List<MpaRating> getAllMpa() {
        log.info("Запрос на получение всех рейтингов MPA");
        return Arrays.asList(MpaRating.values());
    }

    @GetMapping("/{id}")
    public MpaRating getMpaById(@PathVariable int id) {
        log.info("Запрос на получение рейтинга MPA с id = {}", id);
        MpaRating[] ratings = MpaRating.values();
        if (id < 1 || id > ratings.length) {
            throw new NotFoundException("Рейтинг с id " + id + " не найден");
        }
        return ratings[id - 1];
    }
}