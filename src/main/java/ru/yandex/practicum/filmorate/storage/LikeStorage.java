package ru.yandex.practicum.filmorate.storage;

import java.util.List;

public interface LikeStorage {
    void addLike(int filmId, int userId);

    void removeLike(int filmId, int userId);

    int getLikesCount(int filmId);

    List<Integer> getUserIdsWhoLikedFilm(int filmId);

    List<Integer> getFilmIdsLikedByUser(int userId);
}