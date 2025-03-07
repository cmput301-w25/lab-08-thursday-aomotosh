package com.example.androidcicd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import android.util.Log;

import com.example.androidcicd.movie.MovieProvider;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import com.example.androidcicd.movie.Movie;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class MovieProviderTest {
    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockMovieCollection;

    @Mock
    private DocumentReference mockDocRef;

    @Mock
    private Query mockQuery;

    @Mock
    private Task<QuerySnapshot> mockTask;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    private MovieProvider movieProvider;

    @Before
    public void setUp() {
        // Start up mocks
        MockitoAnnotations.openMocks(this);

        // Mock Firestore Collection & Queries
        when(mockFirestore.collection("movies")).thenReturn(mockMovieCollection);
        when(mockMovieCollection.document()).thenReturn(mockDocRef);
        when(mockMovieCollection.document(anyString())).thenReturn(mockDocRef);

        // Ensure the document reference returns a valid ID
        when(mockDocRef.getId()).thenReturn("123");

        // Mock Query and Firestore Query Snapshot
        Query mockQuery = mock(Query.class);
        Task<QuerySnapshot> mockTask = mock(Task.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);

        // Ensure `whereEqualTo()` returns a valid Query object
        when(mockMovieCollection.whereEqualTo(anyString(), anyString())).thenReturn(mockQuery);

        // Ensure `get()` on Query returns a valid Task<QuerySnapshot>
        when(mockQuery.get()).thenReturn(mockTask);
        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true); // Adjust for test cases

        // Setup the movie provider
        MovieProvider.setInstanceForTesting(mockFirestore);
        movieProvider = MovieProvider.getInstance(mockFirestore);
    }






    @Test
    public void testAddMovieSetsId() {
        // Movie to add
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);

        // Ensure Firestore returns a valid document ID
        when(mockDocRef.getId()).thenReturn("123");

        // Mock DataStatus callback
        MovieProvider.DataStatus mockDataStatus = mock(MovieProvider.DataStatus.class);

        // Add movie and check that we update our movie with the generated id
        movieProvider.addMovie(movie, mockDataStatus);

        // Verify that the movie ID is correctly set
        assertEquals("Movie was not updated with correct id.", "123", movie.getId());

        // Verify Firestore interactions
        verify(mockDocRef).set(movie);
        verify(mockDataStatus).onDataUpdated();
    }



    @Test
    public void testDeleteMovie() {
        // Create movie and set our id
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        // Call the delete movie and verify the firebase delete method was called.
        movieProvider.deleteMovie(movie);
        verify(mockDocRef).delete();
    }
    @Test
    public void testUpdateMovieShouldThrowErrorForDifferentIds() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("1");

        // Ensure Firestore returns a different ID to simulate a conflict
        when(mockDocRef.getId()).thenReturn("123");

        // Mock DataStatus callback
        MovieProvider.DataStatus mockDataStatus = new MovieProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                fail("Expected an error but update was successful");
            }

            @Override
            public void onError(String error) {
                assertEquals("A movie with this title already exists!", error);
                latch.countDown();
            }
        };

        // Call updateMovie and wait for Firestore to complete
        movieProvider.updateMovie(movie, "Another Title", "Another Genre", 2026, mockDataStatus);
        latch.await(); // Wait for Firestore operation
    }


    @Test
    public void testUpdateMovieShouldThrowErrorForEmptyName() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        // Mock Firestore document reference
        when(mockDocRef.getId()).thenReturn("123");

        // Mock DataStatus callback
        MovieProvider.DataStatus mockDataStatus = new MovieProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                fail("Expected an error but update was successful");
            }

            @Override
            public void onError(String error) {
                assertEquals("Invalid movie data!", error);
                latch.countDown();
            }
        };

        // Call updateMovie and wait for Firestore to complete
        movieProvider.updateMovie(movie, "", "Another Genre", 2026, mockDataStatus);
        latch.await(); // Wait for Firestore operation
    }


    @After
    public void tearDown() {
        String projectId = "lab8-af5b8";
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
        } catch (IOException exception) {
            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
