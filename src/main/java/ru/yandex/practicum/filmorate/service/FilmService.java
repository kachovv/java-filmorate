package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;

    @Qualifier("likeDbStorage")
    private final LikeStorage likeStorage;

    @Qualifier("genreDbStorage")
    private final GenreStorage genreStorage;

    public void addLike(int filmId, int userId) {
        log.debug("Попытка поставить лайк: фильм={}, пользователь={}", filmId, userId);
        userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        filmStorage.getFilmById(filmId).orElseThrow(() -> new NotFoundException("Фильм не найден"));
        likeStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        log.debug("Попытка удалить лайк: фильм={}, пользователь={}", filmId, userId);
        userStorage.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        filmStorage.getFilmById(filmId).orElseThrow(() -> new NotFoundException("Фильм не найден"));
        likeStorage.removeLike(filmId, userId);
        log.info("Пользователь {} убрал лайк с фильма {}", userId, filmId);
    }

    public List<Film> getTop10Films(int count) {
        log.debug("Запрос на получение {} популярных фильмов", count);
        Collection<Film> allFilms = filmStorage.getAllFilms();
        return allFilms.stream()
                .sorted((f1, f2) -> {
                    int likes1 = likeStorage.getLikesCount(f1.getId());
                    int likes2 = likeStorage.getLikesCount(f2.getId());
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    public Film getFilmById(int id) {
        Film film = filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
        List<Genre> genres = genreStorage.getGenresByFilmId(id);
        film.setGenres(new HashSet<>(genres));
        return film;
    }

    public List<Film> getAllFilms() {
        Collection<Film> films = filmStorage.getAllFilms();
        return films.stream()
                .peek(film -> {
                    List<Genre> genres = genreStorage.getGenresByFilmId(film.getId());
                    film.setGenres(new HashSet<>(genres));
                })
                .collect(Collectors.toList());
    }

    public Film addFilm(Film film) {
        validateFilm(film);
        Film created = filmStorage.addFilm(film);
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                Genre existingGenre = genreStorage.getGenreByName(genre.getName())
                        .orElseThrow(() -> new ValidationException("Жанр " + genre.getName() + " не найден"));
                genreStorage.addGenreToFilm(created.getId(), existingGenre.getId());
            }
        }
        created.setGenres(new HashSet<>(genreStorage.getGenresByFilmId(created.getId())));
        return created;
    }

    public Film updateFilm(Film film) {
        if (film.getId() == null) {
            throw new NotFoundException("Id должен быть указан");
        }
        validateFilm(film);
        Film updated = filmStorage.updateFilm(film);
        genreStorage.removeAllGenresFromFilm(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            for (Genre genre : film.getGenres()) {
                Genre existingGenre = genreStorage.getGenreByName(genre.getName())
                        .orElseThrow(() -> new ValidationException("Жанр " + genre.getName() + " не найден"));
                genreStorage.addGenreToFilm(film.getId(), existingGenre.getId());
            }
        }
        updated.setGenres(new HashSet<>(genreStorage.getGenresByFilmId(film.getId())));
        log.info("Фильм с id = {} успешно обновлён", updated.getId());
        return updated;
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
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FilmService.MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации: дата релиза {} раньше 28.12.1895", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Ошибка валидации: продолжительность фильма {} не положительна", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}