package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Friendship {
    private Integer id;
    private Integer userId;
    private Integer friendId;
    private FriendshipStatus status;

    public Friendship(Integer userId, Integer friendId, FriendshipStatus status) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
    }
}
