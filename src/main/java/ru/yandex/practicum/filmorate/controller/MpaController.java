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

    private static final List<MpaRating> RATINGS = Arrays.asList(
            MpaRating.G,
            MpaRating.PG,
            MpaRating.PG_13,
            MpaRating.R,
            MpaRating.NC_17
    );

    @GetMapping
    public List<MpaRating> getAllMpa() {
        log.info("Запрос на получение всех рейтингов MPA");
        return RATINGS;
    }

    @GetMapping("/{id}")
    public MpaRating getMpaById(@PathVariable int id) {
        log.info("Запрос на получение рейтинга MPA с id = {}", id);
        return RATINGS.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Рейтинг с id " + id + " не найден"));
    }
}