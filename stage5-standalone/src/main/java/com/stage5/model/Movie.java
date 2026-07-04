package com.stage5.model;
import javax.persistence.*;
@Entity @Table(name = "movies")
public class Movie {
    @Id private Long movieId;
    private String title; private String category;
    public Movie() {}
    public Movie(Long movieId, String title, String category) { this.movieId=movieId; this.title=title; this.category=category; }
    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId=movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title=title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category=category; }
}