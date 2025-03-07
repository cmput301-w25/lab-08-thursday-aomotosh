package com.example.androidcicd;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.androidcicd.movie.MovieProvider;
import com.example.androidcicd.movie.Movie;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Mock
    private MovieProvider.DataStatus mockDataStatus;

    private MovieProvider movieProvider;

    @Mock
    private Task<Void> mockSetTask;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock Firestore collection and document behavior
        when(mockFirestore.collection("movies")).thenReturn(mockMovieCollection);
        when(mockMovieCollection.document()).thenReturn(mockDocRef);
        when(mockMovieCollection.document(anyString())).thenReturn(mockDocRef);
        when(mockDocRef.getId()).thenReturn("123");

        // Mock Firestore set() behavior
        when(mockDocRef.set(any(Movie.class))).thenReturn(mockSetTask);
        when(mockSetTask.isSuccessful()).thenReturn(true); // Simulate successful Firestore write
        when(mockSetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockSetTask;
        });
        when(mockSetTask.addOnFailureListener(any())).thenReturn(mockSetTask);

        // Mock Firestore query behavior
        when(mockMovieCollection.whereEqualTo(anyString(), anyString())).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);
        when(mockTask.isSuccessful()).thenReturn(true);
        when(mockTask.getResult()).thenReturn(mockQuerySnapshot);

        // Ensure Firestore query calls the listener
        doAnswer(invocation -> {
            OnCompleteListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onComplete(mockTask);
            return null;
        }).when(mockTask).addOnCompleteListener(any());

        // Setup MovieProvider
        MovieProvider.setInstanceForTesting(mockFirestore);
        movieProvider = MovieProvider.getInstance(mockFirestore);
    }



    @Test
    public void testAddMovieSetsId() throws InterruptedException {
        // Movie to add
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);

        // Simulate Firestore returning NO duplicate movies
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        // Use CountDownLatch to wait for Firestore async operation
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockDataStatus).onDataUpdated();

        // Call addMovie
        movieProvider.addMovie(movie, mockDataStatus);

        // Wait for Firestore query to complete
        assertTrue("Firestore operation timed out", latch.await(2, TimeUnit.SECONDS));

        // Verify movie ID is correctly set
        assertEquals("123", movie.getId());

        // Ensure Firestore set() was called
        verify(mockDocRef).set(movie);
        verify(mockDataStatus).onDataUpdated();
    }

    @Test
    public void testAddMovieFailsIfDuplicateExists() throws InterruptedException {
        // Movie to add (with duplicate title)
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);

        // Simulate Firestore returning an existing movie (i.e., duplicate exists)
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);  // Simulate that Firestore found a duplicate

        // ✅ Ensure Firestore query calls onComplete when `.get()` is called
        doAnswer(invocation -> {
            OnCompleteListener<QuerySnapshot> listener = invocation.getArgument(0);
            listener.onComplete(mockTask);
            return null;
        }).when(mockTask).addOnCompleteListener(any());

        // Use CountDownLatch to wait for Firestore async operation
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown(); //
            return null;
        }).when(mockDataStatus).onError(anyString());

        // Call addMovie
        movieProvider.addMovie(movie, mockDataStatus);

        // Wait for Firestore query to complete (timeout after 2 seconds)
        assertTrue("Firestore operation timed out", latch.await(2, TimeUnit.SECONDS));

        // Ensure Firestore set() was NEVER called (because duplicate exists)
        verify(mockDocRef, never()).set(any(Movie.class));

        // Ensure error callback was triggered
        verify(mockDataStatus).onError("A movie with this title already exists!");
    }


    @Test
    public void testDeleteMovie() {
        // Create movie and set our id
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        // Call deleteMovie and verify delete method was called
        movieProvider.deleteMovie(movie);
        verify(mockDocRef).delete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateMovieShouldThrowErrorForDifferentIds() {
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("1");

        when(mockDocRef.getId()).thenReturn("123");

        movieProvider.updateMovie(movie, "Another Title", "Another Genre", 2026);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateMovieShouldThrowErrorForEmptyName() {
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        when(mockDocRef.getId()).thenReturn("123");

        movieProvider.updateMovie(movie, "", "Another Genre", 2026);
    }
}
