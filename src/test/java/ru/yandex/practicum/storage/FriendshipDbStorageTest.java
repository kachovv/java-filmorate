package ru.yandex.practicum.storage;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.storage.FriendshipDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Transactional
public class FriendshipDbStorageTest {

    @Autowired
    private FriendshipDbStorage friendshipStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer createUser(String email, String login, String name, LocalDate birthday) {
        jdbcTemplate.update(
                "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                email, login, name, birthday
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    @Test
    public void testCreateFriendship() {
        Integer userId1 = createUser("user1@mail.ru", "login1", "User One", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("user2@mail.ru", "login2", "User Two", LocalDate.of(1995, 1, 1));
        Friendship friendship = friendshipStorage.createFriendship(userId1, userId2);

        assertThat(friendship.getId()).isNotNull();
        assertThat(friendship.getUserId()).isEqualTo(userId1);
        assertThat(friendship.getFriendId()).isEqualTo(userId2);
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?", Integer.class, userId1, userId2
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testCreateFriendshipDuplicate() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Friendship first = friendshipStorage.createFriendship(userId1, userId2);
        Friendship second = friendshipStorage.createFriendship(userId2, userId1);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getStatus()).isEqualTo(FriendshipStatus.PENDING);
    }

    @Test
    public void testUpdateStatus() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Friendship created = friendshipStorage.createFriendship(userId1, userId2);

        Friendship updated = friendshipStorage.updateStatus(created.getId(), FriendshipStatus.CONFIRMED);

        assertThat(updated.getStatus()).isEqualTo(FriendshipStatus.CONFIRMED);

        String statusFromDb = jdbcTemplate.queryForObject(
                "SELECT status FROM friendships WHERE id = ?",
                String.class, created.getId());
        assertThat(statusFromDb).isEqualTo("CONFIRMED");
    }

    @Test
    public void testUpdateStatusNotFound() {
        assertThrows(NotFoundException.class,
                () -> friendshipStorage.updateStatus(999, FriendshipStatus.CONFIRMED));
    }

    @Test
    public void testGetById() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Friendship created = friendshipStorage.createFriendship(userId1, userId2);
        Optional<Friendship> found = friendshipStorage.getById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId1);
        assertThat(found.get().getFriendId()).isEqualTo(userId2);
    }

    @Test
    public void testGetByUserPair() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        friendshipStorage.createFriendship(userId1, userId2);
        Optional<Friendship> found = friendshipStorage.getByUserPair(userId1, userId2);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId1);
        assertThat(found.get().getFriendId()).isEqualTo(userId2);

        Optional<Friendship> foundReverse = friendshipStorage.getByUserPair(userId2, userId1);

        assertThat(foundReverse).isPresent();
        assertThat(foundReverse.get().getId()).isEqualTo(found.get().getId());
    }

    @Test
    public void testGetSentRequests() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Integer userId3 = createUser("u3@mail.ru", "l3", "U3", LocalDate.of(2000, 1, 1));
        friendshipStorage.createFriendship(userId1, userId2);
        friendshipStorage.createFriendship(userId1, userId3);
        List<Friendship> sent = friendshipStorage.getSentRequests(userId1);

        assertThat(sent).hasSize(2);
        assertThat(sent).extracting("friendId").containsExactlyInAnyOrder(userId2, userId3);

        List<Friendship> sent2 = friendshipStorage.getSentRequests(userId2);
        assertThat(sent2).isEmpty();
    }

    @Test
    public void testGetReceivedRequests() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Integer userId3 = createUser("u3@mail.ru", "l3", "U3", LocalDate.of(2000, 1, 1));
        friendshipStorage.createFriendship(userId1, userId2);
        friendshipStorage.createFriendship(userId3, userId2);
        List<Friendship> received = friendshipStorage.getReceivedRequests(userId2);

        assertThat(received).hasSize(2);
        assertThat(received).extracting("userId").containsExactlyInAnyOrder(userId1, userId3);
    }

    @Test
    public void testGetConfirmedFriendships() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Integer userId3 = createUser("u3@mail.ru", "l3", "U3", LocalDate.of(2000, 1, 1));

        Friendship f1 = friendshipStorage.createFriendship(userId1, userId2);
        Friendship f2 = friendshipStorage.createFriendship(userId1, userId3);
        friendshipStorage.updateStatus(f1.getId(), FriendshipStatus.CONFIRMED);
        List<Friendship> confirmed1 = friendshipStorage.getConfirmedFriendships(userId1);

        assertThat(confirmed1).hasSize(1);
        assertThat(confirmed1.get(0).getFriendId()).isEqualTo(userId2);

        List<Friendship> confirmed2 = friendshipStorage.getConfirmedFriendships(userId2);
        assertThat(confirmed2).hasSize(1);
        assertThat(confirmed2.get(0).getUserId()).isEqualTo(userId1);
        assertThat(confirmed2.get(0).getFriendId()).isEqualTo(userId2);

        List<Friendship> confirmed3 = friendshipStorage.getConfirmedFriendships(userId3);
        assertThat(confirmed3).isEmpty();
    }

    @Test
    public void testDeleteFriendship() {
        Integer userId1 = createUser("u1@mail.ru", "l1", "U1", LocalDate.of(1990, 1, 1));
        Integer userId2 = createUser("u2@mail.ru", "l2", "U2", LocalDate.of(1995, 1, 1));
        Friendship created = friendshipStorage.createFriendship(userId1, userId2);
        friendshipStorage.deleteFriendship(created.getId());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE id = ?",
                Integer.class, created.getId()
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDeleteFriendshipNotFound() {
        assertThrows(NotFoundException.class,
                () -> friendshipStorage.deleteFriendship(999));
    }
}
