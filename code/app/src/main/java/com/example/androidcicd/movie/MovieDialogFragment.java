package com.example.androidcicd.movie;

import static android.text.TextUtils.isDigitsOnly;
import static android.text.TextUtils.isEmpty;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.androidcicd.R;
import com.example.androidcicd.utils.TextValidator;
import com.google.firebase.firestore.FirebaseFirestore;

public class MovieDialogFragment extends DialogFragment {
    private EditText editMovieName;
    private EditText editMovieGenre;
    private EditText editMovieYear;
    private MovieProvider movieProvider;

    public static MovieDialogFragment newInstance(Movie movie) {
        Bundle args = new Bundle();
        args.putSerializable("Movie", movie);

        MovieDialogFragment fragment = new MovieDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_movie_details, null);
        editMovieName = view.findViewById(R.id.edit_title);
        editMovieGenre = view.findViewById(R.id.edit_genre);
        editMovieYear = view.findViewById(R.id.edit_year);
        movieProvider = MovieProvider.getInstance(FirebaseFirestore.getInstance());

        String tag = getTag();
        Bundle bundle = getArguments();
        Movie movie;

        if (tag != null && tag.equals("Movie Details") && bundle != null) {
            movie = (Movie) bundle.getSerializable("Movie");
            editMovieName.setText(movie.getTitle());
            editMovieGenre.setText(movie.getGenre());
            editMovieYear.setText(String.valueOf(movie.getYear()));
        } else {
            movie = null;
        }

        editMovieName.addTextChangedListener(new TextValidator(editMovieName) {
            @Override
            public void validate(TextView textView) {
                if (isEmpty(textView.getText())) {
                    textView.setError("Movie name cannot be empty!");
                }
            }
        });

        editMovieGenre.addTextChangedListener(new TextValidator(editMovieGenre) {
            @Override
            public void validate(TextView textView) {
                if (isEmpty(textView.getText())) {
                    textView.setError("Movie genre cannot be empty!");
                }
            }
        });

        editMovieYear.addTextChangedListener(new TextValidator(editMovieYear) {
            @Override
            public void validate(TextView textView) {
                if (isEmpty(textView.getText())) {
                    textView.setError("Movie year cannot be empty!");
                } else if (!isDigitsOnly(textView.getText())) {
                    textView.setError("Movie year must be numeric!");
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // Create the dialog fragment
        AlertDialog dialog = builder
                .setView(view)
                .setTitle("Movie Details")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", null) // Override this later
                .create();

        // Ensure the dialog does not close unless input is valid
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (!validInput()) return;

                String title = editMovieName.getText().toString().trim();
                String genre = editMovieGenre.getText().toString().trim();
                int year = Integer.parseInt(editMovieYear.getText().toString().trim());

                if (tag != null && tag.equals("Movie Details")) {
                    // Update existing movie
                    movieProvider.updateMovie(movie, title, genre, year);
                    dialog.dismiss();
                } else {
                    // Check if movie already exists before adding
                    movieProvider.movieExists(title, new MovieProvider.MovieCheckCallback() {
                        @Override
                        public void onCheck(boolean exists) {
                            if (exists) {
                                editMovieName.setError("A movie with this title already exists!");
                            } else {
                                movieProvider.addMovie(new Movie(title, genre, year), new MovieProvider.DataStatus() {
                                    @Override
                                    public void onDataUpdated() {
                                        dialog.dismiss(); // Close dialog only if the movie was successfully added
                                    }

                                    @Override
                                    public void onError(String error) {
                                        editMovieName.setError(error);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            editMovieName.setError(error);
                        }
                    });
                }
            });
        });

        return dialog; // Return the dialog properly
    }

    private boolean validInput() {
        Editable title = editMovieName.getText();
        Editable genre = editMovieGenre.getText();
        Editable year = editMovieYear.getText();
        if (isEmpty(title)) {
            editMovieName.setError("Movie name cannot be empty!");
            return false;
        } else if (isEmpty(genre)) {
            editMovieGenre.setError("Movie genre cannot be empty!");
            return false;
        } else if (isEmpty(year)) {
            editMovieYear.setError("Movie year cannot be empty!");
            return false;
        } else if (!isDigitsOnly(editMovieYear.getText())) {
            editMovieYear.setError("Movie year must be numeric!");
            return false;
        }
        return true;
    }
}
