package ru.yandex.practicum.storage;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Transactional
public class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("MERGE INTO mpa_ratings (name) VALUES ('PG_13')");
        jdbcTemplate.execute("MERGE INTO genres (name) VALUES ('COMEDY')");
    }

    @Test
    public void testAddFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);
        film.setMpaRating(MpaRating.PG_13);

        Film created = filmStorage.addFilm(film);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Film");
    }

    @Test
    public void testGetFilmById() {
        String insertSql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, (SELECT id FROM mpa_ratings WHERE name = 'PG_13'))";
        jdbcTemplate.update(insertSql, "Film 1", "Desc", LocalDate.of(2010, 1, 1), 120);
        Integer filmId = jdbcTemplate.queryForObject("SELECT id FROM films WHERE name = 'Film 1'", Integer.class);
        Optional<Film> filmOpt = filmStorage.getFilmById(filmId);
        assertThat(filmOpt).isPresent();
        assertThat(filmOpt.get().getName()).isEqualTo("Film 1");
    }

    @Test
    public void testUpdateFilm() {
        String insertSql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, (SELECT id FROM mpa_ratings WHERE name = 'PG_13'))";
        jdbcTemplate.update(insertSql, "Old Film", "Old desc", LocalDate.of(2010, 1, 1), 90);
        Integer filmId = jdbcTemplate.queryForObject("SELECT id FROM films WHERE name = 'Old Film'", Integer.class);
        Optional<Film> filmOpt = filmStorage.getFilmById(filmId);

        assertThat(filmOpt).isPresent();

        Film film = filmOpt.get();
        film.setName("Updated Film");
        film.setDescription("New desc");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);
        film.setMpaRating(MpaRating.PG_13);
        Film updated = filmStorage.updateFilm(film);
        assertThat(updated.getName()).isEqualTo("Updated Film");
        assertThat(updated.getDescription()).isEqualTo("New desc");
        assertThat(updated.getReleaseDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(updated.getDuration()).isEqualTo(120);

        String nameFromDb = jdbcTemplate.queryForObject("SELECT name FROM films WHERE id = ?", String.class, filmId);
        assertThat(nameFromDb).isEqualTo("Updated Film");
        Integer durationFromDb = jdbcTemplate.queryForObject("SELECT duration FROM films WHERE id = ?", Integer.class, filmId);
        assertThat(durationFromDb).isEqualTo(120);
    }

    @Test
    public void testUpdateFilmNotFound() {
        Film film = new Film();
        film.setId(999);
        film.setName("Nonexistent");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.now());
        film.setDuration(100);
        film.setMpaRating(MpaRating.G);
        assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(film));
    }

    @Test
    public void testGetAllFilms() {
        String insertSql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, (SELECT id FROM mpa_ratings WHERE name = 'PG_13'))";
        jdbcTemplate.update(insertSql, "Film 1", "Desc", LocalDate.of(2010, 1, 1), 120);
        jdbcTemplate.update(insertSql, "Film 2", "Desc2", LocalDate.of(2011, 1, 1), 90);
        Collection<Film> films = filmStorage.getAllFilms();
        assertThat(films).hasSize(2);
    }

    @Test
    public void testDeleteFilm() {
        String insertSql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, (SELECT id FROM mpa_ratings WHERE name = 'PG_13'))";
        jdbcTemplate.update(insertSql, "Delete Film", "Desc", LocalDate.of(2010, 1, 1), 120);
        Integer filmId = jdbcTemplate.queryForObject("SELECT id FROM films WHERE name = 'Delete Film'", Integer.class);
        filmStorage.deleteFilm(filmId);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, filmId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDeleteFilmNotFound() {
        assertThrows(NotFoundException.class, () -> filmStorage.deleteFilm(999));
    }
}