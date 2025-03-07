package com.example.androidcicd;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.androidcicd.movie.Movie;
import com.example.androidcicd.movie.MovieArrayAdapter;
import com.example.androidcicd.movie.MovieDeleteDialogFragment;
import com.example.androidcicd.movie.MovieDialogFragment;
import com.example.androidcicd.movie.MovieProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button addMovieButton;
    private ListView movieListView;
    private MovieProvider movieProvider;
    private ArrayList<Movie> movieArrayList;
    private ArrayAdapter<Movie> movieArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        addMovieButton = findViewById(R.id.buttonAddMovie);
        movieListView = findViewById(R.id.listviewMovies);

        // Get MovieProvider instance
        movieProvider = MovieProvider.getInstance(FirebaseFirestore.getInstance());
        movieArrayList = new ArrayList<>();
        movieArrayAdapter = new MovieArrayAdapter(this, movieArrayList);
        movieListView.setAdapter(movieArrayAdapter);

        // Listen for updates in the movie list
        movieProvider.listenForUpdates(new MovieProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                movieArrayList.clear();
                movieArrayList.addAll(movieProvider.getMovies());
                movieArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e("Movie Update Error", error);
            }
        });

        // Set listener for adding movies
        addMovieButton.setOnClickListener(view -> {
            MovieDialogFragment movieDialogFragment = new MovieDialogFragment();
            movieDialogFragment.show(getSupportFragmentManager(), "Add Movie");
        });

        // Set listener for editing movies
        movieListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Movie movie = movieArrayAdapter.getItem(i);
            MovieDialogFragment movieDialogFragment = MovieDialogFragment.newInstance(movie);
            movieDialogFragment.show(getSupportFragmentManager(), "Movie Details");
        });

        // Set listener for deleting movies
        movieListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            Movie movie = movieArrayAdapter.getItem(i);
            MovieDeleteDialogFragment movieDeleteDialogFragment = MovieDeleteDialogFragment.newInstance(movie);
            movieDeleteDialogFragment.show(getSupportFragmentManager(), "Movie Delete");
            return true;
        });
    }
}
