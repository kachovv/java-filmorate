package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {
    private final FilmService filmService;

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable int id) {
        log.info("Получен запрос на получение фильма с id = {}", id);
        return filmService.getFilmById(id);
    }

    @GetMapping("/popular")
        public List<Film> getPopular(@RequestParam(defaultValue = "10") int count) {
        log.info("Запрос на получение {} популярных фильмов", count);
        return filmService.getTop10Films(count);
    }

    @GetMapping
    public Collection<Film> findAll() {
        return filmService.getAllFilms();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.info("Получен запрос на создание фильма: {}", film);
        Film created = filmService.addFilm(film);
        log.info("Фильм успешно создан с id = {}", created.getId());
        return created;
    }

    @PutMapping
    public Film update(@RequestBody Film updatedFilm) {
        log.info("Получен запрос на обновление фильма с id = {}", updatedFilm.getId());
        if (updatedFilm.getId() == null) {
            log.warn("Попытка обновления без id");
            throw new ValidationException("Id должен быть указан");
        }
        Film updated = filmService.updateFilm(updatedFilm);
        log.info("Фильм с id = {} успешно обновлен", updatedFilm.getId());
        return updated;
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable int id, @PathVariable int userId) {
        log.info("Пользователь {} ставит лайк фильму {}", userId, id);
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable int id, @PathVariable int userId) {
        log.info("Пользователь {} удаляет лайк с фильма {}", userId, id);
        filmService.removeLike(id, userId);
    }
}