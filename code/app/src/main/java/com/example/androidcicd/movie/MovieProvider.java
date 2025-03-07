package com.example.androidcicd.movie;

import com.google.firebase.firestore.*;
import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    private MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public static void setInstanceForTesting(FirebaseFirestore firestore) {
        movieProvider = new MovieProvider(firestore);
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }

    public void addMovie(Movie movie, DataStatus dataStatus) {
        movieCollection.whereEqualTo("title", movie.getTitle()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        dataStatus.onError("A movie with this title already exists!");
                    } else {
                        DocumentReference docRef = movieCollection.document();
                        movie.setId(docRef.getId());
                        if (validMovie(movie, docRef)) {
                            docRef.set(movie);
                            dataStatus.onDataUpdated();
                        } else {
                            dataStatus.onError("Invalid movie data!");
                        }
                    }
                });
    }
    public void listenForUpdates(DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot document : snapshot) {
                    movies.add(document.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }
    public void movieExists(String title, MovieCheckCallback callback) {
        movieCollection.whereEqualTo("title", title).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onCheck(!task.getResult().isEmpty()); // Returns `true` if movie exists
                    } else {
                        callback.onCheck(false); // Returns `false` if an error occurs
                    }
                });
    }
    // Callback interface for checking if a movie exists
    public interface MovieCheckCallback {
        void onCheck(boolean exists);
    }

    public void updateMovie(Movie movie, String title, String genre, int year, DataStatus dataStatus) {
        movieCollection.whereEqualTo("title", title).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        dataStatus.onError("A movie with this title already exists!");
                    } else {
                        movie.setTitle(title);
                        movie.setGenre(genre);
                        movie.setYear(year);
                        DocumentReference docRef = movieCollection.document(movie.getId());
                        if (validMovie(movie, docRef)) {
                            docRef.set(movie);
                            dataStatus.onDataUpdated();
                        } else {
                            dataStatus.onError("Invalid movie data!");
                        }
                    }
                });
    }

    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    public boolean validMovie(Movie movie, DocumentReference docRef) {
        return movie.getId().equals(docRef.getId()) && !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }
}
