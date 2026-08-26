package ru.yandex.practicum.storage;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Transactional
public class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testAddUser() {
        User user = new User();
        user.setEmail("new@mail.ru");
        user.setLogin("newLogin");
        user.setName("New Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User created = userStorage.addUser(user);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("new@mail.ru");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'new@mail.ru'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testGetUserById() {
        String insertSql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, "test@mail.ru", "testLogin", "Test Name", LocalDate.of(1990, 1, 1));
        Integer userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'test@mail.ru'", Integer.class);
        Optional<User> userOpt = userStorage.getUserById(userId);
        assertThat(userOpt).isPresent();
        assertThat(userOpt.get().getEmail()).isEqualTo("test@mail.ru");
    }

    @Test
    public void testUpdateUser() {
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "old@mail.ru", "oldLogin", "Old Name", LocalDate.of(1990, 1, 1));
        Integer userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'old@mail.ru'", Integer.class);
        User user = userStorage.getUserById(userId).orElseThrow();
        user.setEmail("new@mail.ru");
        user.setName("New Name");
        User updated = userStorage.updateUser(user);

        assertThat(updated.getEmail()).isEqualTo("new@mail.ru");
        assertThat(updated.getName()).isEqualTo("New Name");
        String emailFromDb = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, userId);
        assertThat(emailFromDb).isEqualTo("new@mail.ru");
    }

    @Test
    public void testGetAllUsers() {
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "user1@mail.ru", "login1", "Name1", LocalDate.of(2000, 1, 1));
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "user2@mail.ru", "login2", "Name2", LocalDate.of(2000, 1, 1));
        Collection<User> users = userStorage.getAllUsers();
        assertThat(users).hasSize(2);
    }

    @Test
    public void testDeleteUser() {
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                "delete@mail.ru", "deleteLogin", "Delete Name", LocalDate.of(1990, 1, 1));
        Integer userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'delete@mail.ru'", Integer.class);
        userStorage.deleteUser(userId);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDeleteUserNotFound() {
        assertThrows(NotFoundException.class, () -> userStorage.deleteUser(999));
    }
}
