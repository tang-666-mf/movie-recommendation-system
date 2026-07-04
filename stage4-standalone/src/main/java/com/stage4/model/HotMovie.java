package com.stage4.model;

public class HotMovie {
    private String movieId;
    private String title;
    private long clickCount;
    private int rank;

    public HotMovie() {}

    public HotMovie(String movieId, String title, long clickCount, int rank) {
        this.movieId = movieId; this.title = title; this.clickCount = clickCount; this.rank = rank;
    }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}