package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

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
        Optional<Friendship> existing = getFriendship(userId, friendId);
        if (existing.isPresent()) {
            return existing.get();
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.setString(3, FriendshipStatus.PENDING.name());
            return ps;
        }, keyHolder);

        Friendship friendship = new Friendship();
        friendship.setId(((Number) keyHolder.getKeys().get("ID")).intValue());
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setStatus(FriendshipStatus.PENDING);
        return friendship;
    }

    @Override
    public Optional<Friendship> getById(Integer id) {
        String sql = "SELECT * FROM friendships WHERE id = ?";
        List<Friendship> list = jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Friendship> getFriendship(int userId, int friendId) {
        String sql = "SELECT * FROM friendships WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        List<Friendship> list = jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER,
                userId, friendId, friendId, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Friendship> getFriendshipsByUserId(int userId) {
        String sql = "SELECT * FROM friendships WHERE user_id = ?";
        return jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, userId);
    }

    @Override
    public void deleteFriendship(Integer id) {
        String sql = "DELETE FROM friendships WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            throw new NotFoundException("Дружба с id " + id + " не найдена");
        }
    }

    @Override
    public Friendship updateStatus(Integer friendshipId, FriendshipStatus status) {
        String sql = "UPDATE friendships SET status = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, status.name(), friendshipId);
        if (rows == 0) {
            throw new NotFoundException("Дружба с id " + friendshipId + " не найдена");
        }
        return getById(friendshipId)
                .orElseThrow(() -> new NotFoundException("Дружба с id " + friendshipId + " не найдена"));
    }

    @Override
    public List<Friendship> getSentRequests(Integer userId) {
        String sql = "SELECT * FROM friendships WHERE user_id = ? AND status = ?";
        return jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, userId, FriendshipStatus.PENDING.name());
    }

    @Override
    public List<Friendship> getReceivedRequests(Integer userId) {
        String sql = "SELECT * FROM friendships WHERE friend_id = ? AND status = ?";
        return jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, userId, FriendshipStatus.PENDING.name());
    }

    @Override
    public List<Friendship> getConfirmedFriendships(Integer userId) {
        String sql = "SELECT * FROM friendships WHERE (user_id = ? OR friend_id = ?) AND status = ?";
        return jdbcTemplate.query(sql, FRIENDSHIP_ROW_MAPPER, userId, userId, FriendshipStatus.CONFIRMED.name());
    }

    @Override
    public Optional<Friendship> getByUserPair(Integer userId, Integer friendId) {
        return getFriendship(userId, friendId);
    }
}