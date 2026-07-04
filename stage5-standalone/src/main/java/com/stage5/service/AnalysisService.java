package com.stage5.service;
import com.stage5.model.Movie;
import com.stage5.model.Rating;
import com.stage5.repository.MovieRepository;
import com.stage5.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class AnalysisService {
    @Autowired private MovieRepository movieRepository;
    @Autowired private RatingRepository ratingRepository;

    public List<Map<String,Object>> categoryStats() {
        return movieRepository.countByCategory().stream().map(row -> {
            Map<String,Object> m = new HashMap<>(); m.put("category",row[0]); m.put("count",row[1]); return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String,Object>> movieAvgRatings() {
        List<Map<String,Object>> result = new ArrayList<>();
        for (Object[] row : ratingRepository.avgRatingPerMovie()) {
            Long movieId = (Long)row[0]; Double avg = (Double)row[1]; Long cnt = (Long)row[2];
            Optional<Movie> movie = movieRepository.findById(movieId);
            Map<String,Object> m = new HashMap<>();
            m.put("movieId",movieId); m.put("title",movie.map(Movie::getTitle).orElse("Unknown"));
            m.put("category",movie.map(Movie::getCategory).orElse("Unknown"));
            m.put("avgRating",Math.round(avg*100.0)/100.0); m.put("ratingCount",cnt);
            result.add(m);
        }
        result.sort((a,b)->Double.compare((Double)b.get("avgRating"),(Double)a.get("avgRating")));
        return result;
    }

    public List<Map<String,Object>> categoryAvgRatings() {
        Map<String,List<Double>> catRatings = new HashMap<>();
        Map<String,Long> catCounts = new HashMap<>();
        for (Rating r : ratingRepository.findAll()) {
            Optional<Movie> movie = movieRepository.findById(r.getMovieId());
            if (movie.isPresent()) {
                String cat = movie.get().getCategory();
                catRatings.computeIfAbsent(cat,k->new ArrayList<>()).add(r.getRating());
                catCounts.merge(cat,1L,Long::sum);
            }
        }
        List<Map<String,Object>> result = new ArrayList<>();
        for (Map.Entry<String,List<Double>> e : catRatings.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(d->d).average().orElse(0);
            Map<String,Object> m = new HashMap<>();
            m.put("category",e.getKey()); m.put("avgRating",Math.round(avg*100.0)/100.0);
            m.put("totalRatings",catCounts.get(e.getKey()));
            result.add(m);
        }
        result.sort((a,b)->Double.compare((Double)b.get("avgRating"),(Double)a.get("avgRating")));
        return result;
    }

    public List<Map<String,Object>> userActivity() {
        return ratingRepository.userRatingStats().stream().map(row -> {
            Map<String,Object> m = new HashMap<>();
            m.put("userId",row[0]); m.put("ratingCount",row[1]);
            m.put("avgRating",Math.round((Double)row[2]*100.0)/100.0);
            return m;
        }).collect(Collectors.toList());
    }

    public List<Movie> getMoviesByCategory(String cat) {
        return movieRepository.findAll().stream().filter(m->m.getCategory().equalsIgnoreCase(cat)).collect(Collectors.toList());
    }
}