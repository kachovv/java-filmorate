package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository("friendshipDbStorage")
public class FriendshipDbStorage implements FriendshipStorage {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Friendship> FRIENDSHIP_ROW_MAPPER = (rs, rowNum) -> {
        Friendship friendship = new Friendship();
        friendship.setId(rs.getInt("id"));
        friendship.setUserId(rs.getInt("user_id"));
        friendship.setFriendId(rs.getInt("friend_id"));
        friendship.setStatus(FriendshipStatus.valueOf(rs.getString("status")));
        return friendship;
    };

    public FriendshipDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Friendship createFriendship(Integer userId, Integer friendId) {
        Optional<Friendship> existing = getByUserPair(userId, friendId);
        if (existing.isPresent()) {
            return existing.get();
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.setString(3, FriendshipStatus.CONFIRMED.name());
            return ps;
        }, keyHolder);

        Friendship friendship = new Friendship();
        friendship.setId(((Number) keyHolder.getKeys().get("ID")).intValue());
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setStatus(FriendshipStatus.CONFIRMED);
        return friendship;
    }

    @Override
    public Optional<Friendship> getById(Integer id) {
        String sql = "SELECT * FROM friendships WHERE id = ?";
        List<Friendship> list = jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Friendship> getByUserPair(Integer userId, Integer friendId) {
        String sql = "SELECT * FROM friendships WHERE user_id = ? AND friend_id = ?";
        List<Friendship> list = jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, userId, friendId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Friendship> getConfirmedFriendships(Integer userId) {
        String sql = "SELECT * FROM friendships WHERE user_id = ? AND status = ?";
        return jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, userId, FriendshipStatus.CONFIRMED.name());
    }

    @Override
    public void deleteByUserPair(Integer userId, Integer friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int rows = jdbcTemplate.update(sql, userId, friendId);
        if (rows == 0) {
            log.info("Запись о дружбе между {} и {} не найдена, удаление пропущено", userId, friendId);
        } else {
            log.info("Удалена дружба между {} и {}", userId, friendId);
        }
    }
}