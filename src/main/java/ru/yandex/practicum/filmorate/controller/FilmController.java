package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Получен запрос на создание фильма: {}", film);
        validateFilm(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Фильм успешно создан с id = {}", film.getId());
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film updatedFilm) {
        log.info("Получен запрос на обновление фильма с id = {}", updatedFilm.getId());
        if (updatedFilm.getId() == null) {
            log.warn("Попытка обновления без id");
            throw new ValidationException("Id должен быть указан");
        }
        if (!films.containsKey(updatedFilm.getId())) {
            log.warn("Фильм с id = {} не найден", updatedFilm.getId());
            throw new NotFoundException("Фильм с id = " + updatedFilm.getId() + " не найден");
        }
        validateFilm(updatedFilm);
        Film existing = films.get(updatedFilm.getId());
        existing.setName(updatedFilm.getName());
        existing.setDescription(updatedFilm.getDescription());
        existing.setReleaseDate(updatedFilm.getReleaseDate());
        existing.setDuration(updatedFilm.getDuration());
        log.info("Фильм с id = {} успешно обновлен", updatedFilm.getId());
        return existing;
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации: название фильма пустое");
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Ошибка валидации: описание превышет 200 символов (длина = {})", film.getDescription().length());
            throw new ValidationException("Описание не должно превышать 200 символов");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации: дата релиза {} раньше 28.12.1895", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Ошибка валидации: продолжительность фильма {} не положительна", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}