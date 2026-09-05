package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    List<Genre> getAllGenres();

    Optional<Genre> getGenreById(int id);

    Optional<Genre> getGenreByName(String name);

    void addGenreToFilm(int filmId, int genreId);

    void removeGenreFromFilm(int filmId, int genreId);

    List<Genre> getGenresByFilmId(int filmId);

    void removeAllGenresFromFilm(int filmId);
}