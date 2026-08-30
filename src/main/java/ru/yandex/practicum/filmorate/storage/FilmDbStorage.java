package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
        if (rs.getObject("mpa_rating_id") != null) {
            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getInt("mpa_rating_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpaRating(mpa);
        }
        return film;
    };

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film addFilm(Film film) {
        Integer mpaId = resolveMpaRatingId(film.getMpaRating());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (mpaId != null) {
                ps.setInt(5, mpaId);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);
        film.setId(keyHolder.getKey().intValue());
        if (mpaId != null) {
            film.setMpaRating(new MpaRating(mpaId, film.getMpaRating().getName()));
        }
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        Integer mpaId = resolveMpaRatingId(film.getMpaRating());
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                mpaId,
                film.getId()
        );
        if (rows == 0) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
        if (mpaId != null) {
            film.setMpaRating(new MpaRating(mpaId, film.getMpaRating().getName()));
        }
        return film;
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id";
        return jdbcTemplate.query(sql, FILM_ROW_MAPPER);
    }

    @Override
    public Optional<Film> getFilmById(int id) {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id WHERE f.id = ?";
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

    private Integer resolveMpaRatingId(MpaRating mpaRating) {
        if (mpaRating == null) return null;

        if (mpaRating.getId() != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?", Integer.class, mpaRating.getId());
            if (count > 0) {
                return mpaRating.getId();
            }
            // если id не найден и name отсутствует — 404
            if (mpaRating.getName() == null) {
                throw new NotFoundException("Рейтинг MPA с id " + mpaRating.getId() + " не найден");
            }
        }

        if (mpaRating.getName() != null) {
            jdbcTemplate.update("MERGE INTO mpa_ratings (name) KEY(name) VALUES (?)", mpaRating.getName());
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM mpa_ratings WHERE name = ?", Integer.class, mpaRating.getName());
        }
        return null;
    }
}