package com.recommend.model;

/**
 * 电影推荐结果实体
 */
public class MovieRecommendation {
    private String movieId;
    private String title;
    private String category;
    private double predictedRating;

    public MovieRecommendation() {}

    public MovieRecommendation(String movieId, String title, String category, double predictedRating) {
        this.movieId = movieId;
        this.title = title;
        this.category = category;
        this.predictedRating = predictedRating;
    }

    // Getters and Setters
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPredictedRating() { return predictedRating; }
    public void setPredictedRating(double predictedRating) { this.predictedRating = predictedRating; }
}
