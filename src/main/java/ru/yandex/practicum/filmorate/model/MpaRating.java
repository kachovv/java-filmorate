package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class MpaRating {
    public static final MpaRating G = new MpaRating(1, "G");
    public static final MpaRating PG = new MpaRating(2, "PG");
    public static final MpaRating PG_13 = new MpaRating(3, "PG-13");
    public static final MpaRating R = new MpaRating(4, "R");
    public static final MpaRating NC_17 = new MpaRating(5, "NC-17");

    private Integer id;
    private String name;

    public MpaRating() {
    }

    public MpaRating(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
