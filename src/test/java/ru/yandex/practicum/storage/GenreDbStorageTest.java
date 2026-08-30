package ru.yandex.practicum.storage;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Transactional
public class GenreDbStorageTest {

    @Autowired
    private GenreDbStorage genreStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int createGenre(String name) {
        jdbcTemplate.update("MERGE INTO genres (name) VALUES (?)", name);
        return jdbcTemplate.queryForObject("SELECT id FROM genres WHERE name = ?", Integer.class, name);
    }

    private int createFilm(String name, String description, LocalDate releaseDate, int duration, String mpaName) {
        jdbcTemplate.update("MERGE INTO mpa_ratings (name) VALUES (?)", mpaName);
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, (SELECT id FROM mpa_ratings WHERE name = ?))";
        jdbcTemplate.update(sql, name, description, releaseDate, duration, mpaName);
        return jdbcTemplate.queryForObject("SELECT id FROM films WHERE name = ?", Integer.class, name);
    }

    @Test
    public void testGetAllGenres() {
        jdbcTemplate.update("INSERT INTO genres (name) VALUES ('COMEDY'), ('DRAMA')");
        List<Genre> genres = genreStorage.getAllGenres();
        assertThat(genres).hasSize(2);
    }

    @Test
    public void testGetGenreById() {
        jdbcTemplate.update("INSERT INTO genres (name) VALUES ('ACTION')");
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM genres WHERE name = 'ACTION'", Integer.class);
        Optional<Genre> genre = genreStorage.getGenreById(id);
        assertThat(genre).isPresent();
        assertThat(genre.get().getName()).isEqualTo("ACTION");
    }

    @Test
    public void testGetGenreByIdNotFound() {
        Optional<Genre> genre = genreStorage.getGenreById(999);
        assertThat(genre).isEmpty();
    }

    @Test
    public void testGetGenreByName() {
        createGenre("THRILLER");
        Optional<Genre> genre = genreStorage.getGenreByName("THRILLER");
        assertThat(genre).isPresent();
        assertThat(genre.get().getName()).isEqualTo("THRILLER");
    }

    @Test
    public void testGetGenreByNameNotFound() {
        Optional<Genre> genre = genreStorage.getGenreByName("NONEXISTENT");
        assertThat(genre).isEmpty();
    }

    @Test
    public void testAddGenreToFilm() {
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");
        int genreId = createGenre("COMEDY");

        genreStorage.addGenreToFilm(filmId, genreId);

        List<Genre> genres = genreStorage.getGenresByFilmId(filmId);
        assertThat(genres).hasSize(1);
        assertThat(genres.get(0).getName()).isEqualTo("COMEDY");
    }

    @Test
    public void testRemoveGenreFromFilm() {
        int filmId = createFilm("Film2", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");
        int genreId1 = createGenre("DRAMA");
        int genreId2 = createGenre("ACTION");

        genreStorage.addGenreToFilm(filmId, genreId1);
        genreStorage.addGenreToFilm(filmId, genreId2);

        genreStorage.removeGenreFromFilm(filmId, genreId1);

        List<Genre> genres = genreStorage.getGenresByFilmId(filmId);
        assertThat(genres).hasSize(1);
        assertThat(genres.get(0).getName()).isEqualTo("ACTION");
    }

    @Test
    public void testGetGenresByFilmId() {
        int filmId = createFilm("Film3", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");
        int genreId1 = createGenre("COMEDY");
        int genreId2 = createGenre("CARTOON");

        genreStorage.addGenreToFilm(filmId, genreId1);
        genreStorage.addGenreToFilm(filmId, genreId2);

        List<Genre> genres = genreStorage.getGenresByFilmId(filmId);
        assertThat(genres).hasSize(2);
        assertThat(genres).extracting(Genre::getName).containsExactlyInAnyOrder("COMEDY", "CARTOON");
    }

    @Test
    public void testGetGenresByFilmIdNotFound() {
        List<Genre> genres = genreStorage.getGenresByFilmId(999);
        assertThat(genres).isEmpty();
    }
}