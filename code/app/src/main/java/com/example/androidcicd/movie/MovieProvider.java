package com.example.androidcicd.movie;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }

    public void updateMovie(Movie movie, String title, String genre, int year) {
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setYear(year);
        DocumentReference docRef = movieCollection.document(movie.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void addMovie(Movie movie, DataStatus dataStatus) {
        movieCollection.whereEqualTo("title", movie.getTitle()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Movie already exists, return error
                            dataStatus.onError("A movie with this title already exists!");
                        } else {
                            // Movie does not exist, proceed with adding it
                            DocumentReference docRef = movieCollection.document();
                            movie.setId(docRef.getId());
                            if (validMovie(movie, docRef)) {
                                docRef.set(movie)
                                        .addOnSuccessListener(aVoid -> dataStatus.onDataUpdated())
                                        .addOnFailureListener(e -> dataStatus.onError("Failed to add movie: " + e.getMessage()));
                            } else {
                                dataStatus.onError("Invalid Movie!");
                            }
                        }
                    } else {
                        dataStatus.onError("Error checking existing movies: " + task.getException().getMessage());
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
    public static void setInstanceForTesting(FirebaseFirestore firestore) {
        movieProvider = new MovieProvider(firestore);
    }
    public void movieExists(String title, MovieCheckCallback callback) {
        movieCollection.whereEqualTo("title", title).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onCheck(!task.getResult().isEmpty()); // Returns `true` if movie exists
                    } else {
                        callback.onError("Error checking database: " + task.getException().getMessage());
                    }
                });
    }

    // Callback interface
    public interface MovieCheckCallback {
        void onCheck(boolean exists);
        void onError(String error);
    }

}