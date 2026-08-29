package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Film> FILM_ROW_MAPPER = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        film.setMpaRating(MpaRating.valueOf(rs.getString("mpa_rating")));
        return film;
    };

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film addFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpaRating().name()
        );
        Integer id = jdbcTemplate.queryForObject("SELECT last_insert_id()", Integer.class);
        film.setId(id);
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpaRating().name(),
                film.getId()
        );
        if (rows == 0) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        return film;
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT * FROM films";
        return jdbcTemplate.query(sql, FILM_ROW_MAPPER);
    }

    @Override
    public Optional<Film> getFilmById(int id) {
        String sql = "SELECT * FROM films WHERE id = ?";
        List<Film> films = jdbcTemplate.query(sql, FILM_ROW_MAPPER, id);
        return films.stream().findFirst();
    }

    @Override
    public void deleteFilm(int id) {
        String sql = "DELETE FROM films WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
    }
}