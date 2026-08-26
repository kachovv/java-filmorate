package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FilmControllerTest {
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        FilmService filmService = new FilmService(userStorage, filmStorage);
        filmController = new FilmController(filmService);
    }

    @Test
    void shouldCreateValidFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2019, 1, 1));
        film.setDuration(100);

        Film created = filmController.create(film);

        assertNotNull(film.getId());
        assertEquals("Film", created.getName());
        assertEquals(100, created.getDuration());
    }

    @Test
    void shouldUpdateValidFilm() {
        Film film = new Film();
        film.setName("Old film");
        film.setDescription("Bad film");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(60);
        Film created = filmController.create(film);

        Film update = new Film();
        update.setId(created.getId());
        update.setName("Updated film");
        update.setDescription("Good film");
        update.setDuration(120);
        update.setReleaseDate(LocalDate.of(2010, 1, 1));
        Film updatedFilm = filmController.update(update);

        assertEquals("Updated film", updatedFilm.getName());
        assertEquals(120, updatedFilm.getDuration());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Description");
        film.setDuration(100);
        film.setReleaseDate(LocalDate.of(2020, 1, 1));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Название фильма не может быть пустым", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDateReleaseBefore1895() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(100);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(100);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Описание не должно превышать 200 символов", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDurationNegativeOrZero() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(0);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> filmController.create(film));
        assertEquals("Продолжительность фильма должна быть положительным числом", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUpdateWithoutId() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(100);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> filmController.update(film));
        assertEquals("Id должен быть указан", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUpdateNotFound() {
        Film film = new Film();
        film.setId(999);
        film.setName("Film");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(100);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> filmController.update(film));
        assertEquals("Фильм с id = 999 не найден", ex.getMessage());
    }
}