package ru.yandex.practicum.storage;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.storage.LikeDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Transactional
public class LikeDbStorageTest {

    @Autowired
    private LikeDbStorage likeStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int createUser(String email, String login, String name, LocalDate birthday) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, email, login, name, birthday);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private int createFilm(String name, String description, LocalDate releaseDate, int duration, String mpaName) {
        jdbcTemplate.update("MERGE INTO mpa_ratings (name) VALUES (?)", mpaName);
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, (SELECT id FROM mpa_ratings WHERE name = ?))";
        jdbcTemplate.update(sql, name, description, releaseDate, duration, mpaName);
        return jdbcTemplate.queryForObject("SELECT id FROM films WHERE name = ?", Integer.class, name);
    }

    @Test
    public void testAddLike() {
        int userId = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");

        likeStorage.addLike(filmId, userId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?",
                Integer.class, filmId, userId);
        assertThat(count).isEqualTo(1);

        int likesCount = likeStorage.getLikesCount(filmId);
        assertThat(likesCount).isEqualTo(1);
    }

    @Test
    public void testAddLikeDuplicate() {
        int userId = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");

        likeStorage.addLike(filmId, userId);
        assertThrows(Exception.class, () -> likeStorage.addLike(filmId, userId));
    }

    @Test
    public void testRemoveLike() {
        int userId = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");

        likeStorage.addLike(filmId, userId);
        likeStorage.removeLike(filmId, userId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?", Integer.class, filmId, userId);
        assertThat(count).isEqualTo(0);

        int likesCount = likeStorage.getLikesCount(filmId);
        assertThat(likesCount).isEqualTo(0);
    }

    @Test
    public void testRemoveLikeNotFound() {
        int userId = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");
        likeStorage.removeLike(filmId, userId);
    }

    @Test
    public void testGetLikesCount() {
        int userId1 = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int userId2 = createUser("user2@mail.ru", "user2", "User2", LocalDate.of(1995, 1, 1));
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");

        likeStorage.addLike(filmId, userId1);
        likeStorage.addLike(filmId, userId2);

        int likesCount = likeStorage.getLikesCount(filmId);
        assertThat(likesCount).isEqualTo(2);

        likeStorage.removeLike(filmId, userId1);
        likesCount = likeStorage.getLikesCount(filmId);
        assertThat(likesCount).isEqualTo(1);
    }

    @Test
    public void testGetUserIdsWhoLikedFilm() {
        int userId1 = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int userId2 = createUser("user2@mail.ru", "user2", "User2", LocalDate.of(1995, 1, 1));
        int filmId = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");

        likeStorage.addLike(filmId, userId1);
        likeStorage.addLike(filmId, userId2);

        List<Integer> userIds = likeStorage.getUserIdsWhoLikedFilm(filmId);
        assertThat(userIds).hasSize(2);
        assertThat(userIds).containsExactlyInAnyOrder(userId1, userId2);
    }

    @Test
    public void testGetFilmIdsLikedByUser() {
        int userId = createUser("user1@mail.ru", "user1", "User1", LocalDate.of(1990, 1, 1));
        int filmId1 = createFilm("Film1", "Desc", LocalDate.of(2020, 1, 1), 120, "PG_13");
        int filmId2 = createFilm("Film2", "Desc2", LocalDate.of(2021, 1, 1), 110, "PG_13");

        likeStorage.addLike(filmId1, userId);
        likeStorage.addLike(filmId2, userId);

        List<Integer> filmIds = likeStorage.getFilmIdsLikedByUser(userId);
        assertThat(filmIds).hasSize(2);
        assertThat(filmIds).containsExactlyInAnyOrder(filmId1, filmId2);
    }

    @Test
    public void testGetUserIdsWhoLikedFilmNotFound() {
        List<Integer> userIds = likeStorage.getUserIdsWhoLikedFilm(999);
        assertThat(userIds).isEmpty();
    }

    @Test
    public void testGetLikesCountForNonExistingFilm() {
        int count = likeStorage.getLikesCount(999);
        assertThat(count).isZero();
    }
}