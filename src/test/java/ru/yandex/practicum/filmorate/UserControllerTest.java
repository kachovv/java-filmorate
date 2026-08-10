package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerTest {
    private UserController userController;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        userController = new UserController(userService);
    }

    @Test
    void shouldCreateValidUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("Login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User created = userController.create(user);

        assertNotNull(created.getId());
        assertEquals("test@mail.ru", created.getEmail());
        assertEquals("Name", created.getName());
    }

    @Test
    void shouldUseLoginWhenNameEmpty() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("Login");
        user.setName(null);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User created = userController.create(user);
        assertEquals("Login", created.getName());
    }

    @Test
    void shouldUpdateValidUser() {
        User user = new User();
        user.setEmail("old@mail.ru");
        user.setLogin("oldLogin");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User created = userController.create(user);

        User update = new User();
        update.setId(created.getId());
        update.setEmail("new@mail.ru");
        update.setLogin("newLogin");
        update.setName("New Name");
        update.setBirthday(LocalDate.of(1980, 1, 1));

        User updated = userController.update(update);

        assertEquals("new@mail.ru", updated.getEmail());
        assertEquals("New Name", updated.getName());
        assertEquals(LocalDate.of(1980, 1, 1), updated.getBirthday());
    }

    @Test
    void shouldThrowExceptionWhenEmailWithoutAt() {
        User user = new User();
        user.setEmail("email.ru");
        user.setLogin("LoginPassword");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2001, 07, 01));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Email должен быть указан и содержать @", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenLoginIsBlank() {
        User user = new User();
        user.setEmail("test@email.ru");
        user.setLogin("");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2001, 07, 01));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Логин должен быть указан и не должен содержать пробелы", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBirthdayInFuture() {
        User user = new User();
        user.setEmail("test@email.ru");
        user.setLogin("LoginTest");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2050, 07, 01));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> userController.create(user));
        assertEquals("Дата рождения не может быть в будущем", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailDuplicateOnCreate() {
        User user1 = new User();
        user1.setEmail("same@mail.ru");
        user1.setLogin("login1");
        user1.setName("Name1");
        user1.setBirthday(LocalDate.of(2000, 1, 1));
        userController.create(user1);

        User user2 = new User();
        user2.setEmail("same@mail.ru");
        user2.setLogin("login2");
        user2.setName("Name2");
        user2.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> userController.create(user2));
        assertEquals("Этот email уже используется", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailDuplicateOnUpdate() {
        User user1 = new User();
        user1.setEmail("one@mail.ru");
        user1.setLogin("login1");
        user1.setName("Name1");
        user1.setBirthday(LocalDate.of(2000, 1, 1));
        User created1 = userController.create(user1);

        User user2 = new User();
        user2.setEmail("two@mail.ru");
        user2.setLogin("login2");
        user2.setName("Name2");
        user2.setBirthday(LocalDate.of(2000, 1, 1));
        User created2 = userController.create(user2);

        User update = new User();
        update.setId(created2.getId());
        update.setEmail("one@mail.ru");
        update.setLogin("login2");
        update.setName("Name2");
        update.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> userController.update(update));
        assertEquals("Этот email уже используется", ex.getMessage());
    }

    @Test
    void shouldUpdateWithSameEmail() {
        User user = new User();
        user.setEmail("same@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        User created = userController.create(user);

        User update = new User();
        update.setId(created.getId());
        update.setEmail("same@mail.ru");
        update.setLogin("newLogin");
        update.setName("New Name");
        update.setBirthday(LocalDate.of(1990, 1, 1));

        User updated = userController.update(update);
        assertEquals("newLogin", updated.getLogin());
        assertEquals("same@mail.ru", updated.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUpdateWithoutId() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> userController.update(user));
        assertEquals("Id должен быть указан", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUpdateNotFound() {
        User user = new User();
        user.setId(999);
        user.setEmail("test@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> userController.update(user));
        assertEquals("Пользователь с id = 999 не найден", ex.getMessage());
    }
}