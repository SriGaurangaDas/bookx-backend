package com.bookx.bookx_backend.service;

import com.bookx.bookx_backend.model.Book;
import com.bookx.bookx_backend.dto.BookDto;
import com.bookx.bookx_backend.dto.BookListingDto;
import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingType;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.model.BookListing;
import com.bookx.bookx_backend.model.User;
import com.bookx.bookx_backend.repository.BookListingRepository;
import com.bookx.bookx_backend.repository.BookRepository;
import com.bookx.bookx_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class BookListingServiceTest {

    @Mock
    private BookListingRepository bookListingRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookService bookService; // Although not directly used in deleteBookListing, good to have if other methods are tested

    @InjectMocks
    private BookListingService bookListingService;

    private User testUser;
    private Book testBook;
    private BookListing testListing;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book Title");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("1234567890");

        testListing = new BookListing();
        testListing.setId(1L);
        testListing.setLister(testUser);
        testListing.setBook(testBook);
    }

    @Test
    void deleteBookListing_success_deletesBookWhenLastListing() {
        // Arrange
        Long listingId = 1L;
        String authenticatedUsername = "testuser";

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.of(testListing));
        // Crucially, mock countByBookId to return 0 *after* the listing is conceptually deleted.
        // The actual deletion happens first, then the count.
        // So, when countByBookId is called for testBook.getId(), it should reflect a state where no listings are left.
        when(bookListingRepository.countByBookId(testBook.getId())).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> bookListingService.deleteBookListing(listingId, authenticatedUsername));

        // Assert
        verify(bookListingRepository).findById(listingId);
        verify(bookListingRepository).delete(testListing);
        verify(bookListingRepository).flush(); // Ensure flush is called
        verify(bookListingRepository).countByBookId(testBook.getId());
        verify(bookRepository).delete(testBook); // Book should be deleted
    }

    @Test
    void deleteBookListing_success_doesNotDeleteBookWhenOtherListingsExist() {
        // Arrange
        Long listingId = 1L;
        String authenticatedUsername = "testuser";

        // Ensure testListing and testUser are properly initialized from setUp()
        // If testListing.getLister() or testListing.getBook() are null, initialize them
        if (testListing.getLister() == null) testListing.setLister(testUser);
        if (testListing.getBook() == null) testListing.setBook(testBook);


        when(bookListingRepository.findById(listingId)).thenReturn(Optional.of(testListing));
        // Mock that there's still 1 listing remaining for this book after current one is deleted
        when(bookListingRepository.countByBookId(testBook.getId())).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> bookListingService.deleteBookListing(listingId, authenticatedUsername));

        // Assert
        verify(bookListingRepository).findById(listingId);
        verify(bookListingRepository).delete(testListing);
        verify(bookListingRepository).flush();
        verify(bookListingRepository).countByBookId(testBook.getId());
        verify(bookRepository, never()).delete(any(Book.class)); // Book should NOT be deleted
    }

    @Test
    void deleteBookListing_throwsNotFound_whenListingDoesNotExist() {
        // Arrange
        Long listingId = 99L; // Non-existent ID
        String authenticatedUsername = "testuser"; // Method signature requires it

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.deleteBookListing(listingId, authenticatedUsername);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bookListingRepository).findById(listingId); // Verify findById was called
        verify(bookListingRepository, never()).delete(any(BookListing.class)); // Verify no listing deletion
        verify(bookRepository, never()).delete(any(Book.class)); // Verify no book deletion
    }

    @Test
    void deleteBookListing_throwsForbidden_whenUserNotOwner() {
        // Arrange
        Long listingId = 1L;
        String authenticatedUsername = "currentUser"; // This user is trying to delete

        User ownerUser = new User(); // Renamed to avoid conflict with a potential field 'owner'
        ownerUser.setId(2L);
        ownerUser.setUsername("ownerUser"); // This user owns the listing

        // Ensure testListing from setUp() is used and its lister is set to ownerUser
        // testListing.setBook(testBook); // book can remain as setup in @BeforeEach
        testListing.setLister(ownerUser);

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.of(testListing));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.deleteBookListing(listingId, authenticatedUsername);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(bookListingRepository).findById(listingId); // Verify findById was called
        verify(bookListingRepository, never()).delete(any(BookListing.class)); // Verify no listing deletion
        verify(bookRepository, never()).delete(any(Book.class)); // Verify no book deletion
    }

    // --- Tests for createBookListing ---

    @Test
    void createBookListing_success() {
        // Arrange
        String authenticatedUsername = "testuser"; // Uses testUser from setUp()

        com.bookx.bookx_backend.dto.BookDto inputBookDtoForListing = new com.bookx.bookx_backend.dto.BookDto();
        inputBookDtoForListing.setTitle("New Creating Book");
        inputBookDtoForListing.setAuthor("Creator Author");
        inputBookDtoForListing.setIsbn("1122334455");

        com.bookx.bookx_backend.dto.BookListingDto inputListingDto = new com.bookx.bookx_backend.dto.BookListingDto();
        inputListingDto.setBook(inputBookDtoForListing); // Set the input BookDto
        inputListingDto.setListingType(com.bookx.bookx_backend.model.enums.ListingType.FOR_SALE); // Corrected enum
        inputListingDto.setPrice(new java.math.BigDecimal("19.99"));
        inputListingDto.setCondition(com.bookx.bookx_backend.model.enums.BookCondition.NEW);
        inputListingDto.setLocationLongitude(10.0);
        inputListingDto.setLocationLatitude(20.0);
        inputListingDto.setListerNotes("A great book for sale!");

        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(testUser));

        // 1. Mock bookService.findOrCreateBook to return a BookDto
        com.bookx.bookx_backend.dto.BookDto bookDtoReturnedByService = new com.bookx.bookx_backend.dto.BookDto();
        bookDtoReturnedByService.setId(2L); // This ID is crucial for the next step
        bookDtoReturnedByService.setTitle(inputBookDtoForListing.getTitle());
        bookDtoReturnedByService.setAuthor(inputBookDtoForListing.getAuthor());
        bookDtoReturnedByService.setIsbn(inputBookDtoForListing.getIsbn());
        when(bookService.findOrCreateBook(any(com.bookx.bookx_backend.dto.BookDto.class)))
                .thenReturn(bookDtoReturnedByService);

        // 2. Prepare the Book entity that BookListingService will fetch using the ID from bookDtoReturnedByService
        Book bookEntityForListing = new Book();
        bookEntityForListing.setId(bookDtoReturnedByService.getId());
        bookEntityForListing.setTitle(bookDtoReturnedByService.getTitle());
        bookEntityForListing.setAuthor(bookDtoReturnedByService.getAuthor());
        bookEntityForListing.setIsbn(bookDtoReturnedByService.getIsbn());
        // 3. Mock bookRepository.findById to return this entity
        when(bookRepository.findById(bookDtoReturnedByService.getId()))
                .thenReturn(Optional.of(bookEntityForListing));

        // 4. This is the BookListing entity that would be returned by the repository's save method
        BookListing savedListingEntity = new BookListing();
        savedListingEntity.setId(5L);
        savedListingEntity.setLister(testUser);
        savedListingEntity.setBook(bookEntityForListing); // Associated with the fetched Book entity
        savedListingEntity.setListingType(inputListingDto.getListingType());
        savedListingEntity.setPrice(inputListingDto.getPrice());
        savedListingEntity.setCondition(inputListingDto.getCondition());
        savedListingEntity.setLocationLongitude(inputListingDto.getLocationLongitude());
        savedListingEntity.setLocationLatitude(inputListingDto.getLocationLatitude());
        savedListingEntity.setListerNotes(inputListingDto.getListerNotes());
        savedListingEntity.setCreatedAt(java.time.Instant.now());
        savedListingEntity.setUpdatedAt(java.time.Instant.now());
        savedListingEntity.setListingStatus(com.bookx.bookx_backend.model.enums.ListingCurrentStatus.AVAILABLE);

        when(bookListingRepository.save(any(BookListing.class))).thenReturn(savedListingEntity);

        // Act
        com.bookx.bookx_backend.dto.BookListingDto resultDto = bookListingService.createBookListing(inputListingDto, authenticatedUsername);

        // Assert
        assertNotNull(resultDto);
        assertEquals(savedListingEntity.getId(), resultDto.getId());
        assertEquals(inputListingDto.getListingType(), resultDto.getListingType());
        assertEquals(0, inputListingDto.getPrice().compareTo(resultDto.getPrice()));
        assertNotNull(resultDto.getBook());
        // The resultDto.getBook() should match the details from bookDtoReturnedByService
        assertEquals(bookDtoReturnedByService.getTitle(), resultDto.getBook().getTitle());
        assertEquals(bookDtoReturnedByService.getId(), resultDto.getBook().getId());
        assertNotNull(resultDto.getLister());
        assertEquals(testUser.getId(), resultDto.getLister().getId());
        assertNotNull(resultDto.getCreatedAt());
        assertNotNull(resultDto.getUpdatedAt());
        assertEquals(inputListingDto.getLocationLongitude(), resultDto.getLocationLongitude());
        assertEquals(inputListingDto.getLocationLatitude(), resultDto.getLocationLatitude());
        assertEquals(inputListingDto.getListerNotes(), resultDto.getListerNotes());
        assertEquals(com.bookx.bookx_backend.model.enums.ListingCurrentStatus.AVAILABLE, resultDto.getListingStatus());

        verify(userRepository).findByUsername(authenticatedUsername);
        verify(bookService).findOrCreateBook(any(com.bookx.bookx_backend.dto.BookDto.class));
        // Verify that BookListingService used the ID from the DTO to fetch the Book entity
        verify(bookRepository).findById(bookDtoReturnedByService.getId());
        verify(bookListingRepository).save(any(BookListing.class));
    }

    @Test
    void createBookListing_userNotFound_throwsResponseStatusException() {
        // Arrange
        String authenticatedUsername = "nonexistentuser";
        com.bookx.bookx_backend.dto.BookDto inputBookDto = new com.bookx.bookx_backend.dto.BookDto();
        inputBookDto.setTitle("Test Book");

        com.bookx.bookx_backend.dto.BookListingDto inputListingDto = new com.bookx.bookx_backend.dto.BookListingDto();
        inputListingDto.setBook(inputBookDto);
        inputListingDto.setListingType(com.bookx.bookx_backend.model.enums.ListingType.FOR_SALE);
        inputListingDto.setPrice(new java.math.BigDecimal("10.00"));
        inputListingDto.setCondition(com.bookx.bookx_backend.model.enums.BookCondition.GOOD);

        // Mock userRepository to return an empty Optional, simulating user not found
        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.createBookListing(inputListingDto, authenticatedUsername);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        // Optionally, assert the reason or message if your service sets one
        assertTrue(exception.getReason().contains("Authenticated user not found"));

        verify(userRepository).findByUsername(authenticatedUsername);
        verify(bookService, never()).findOrCreateBook(any());
        verify(bookListingRepository, never()).save(any());
    }

    @Test
    void createBookListing_bookEntityNotFoundAfterFindOrCreate_throwsInternalServerError() {
        // Arrange
        String authenticatedUsername = "testuser";
        com.bookx.bookx_backend.dto.BookDto inputBookDtoForListing = new com.bookx.bookx_backend.dto.BookDto();
        inputBookDtoForListing.setTitle("Inconsistent Book");
        inputBookDtoForListing.setIsbn("0000000000");

        com.bookx.bookx_backend.dto.BookListingDto inputListingDto = new com.bookx.bookx_backend.dto.BookListingDto();
        inputListingDto.setBook(inputBookDtoForListing);
        inputListingDto.setListingType(com.bookx.bookx_backend.model.enums.ListingType.FOR_SALE);
        // ... other necessary fields for inputListingDto ...

        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(testUser));

        // Mock bookService.findOrCreateBook to return a BookDto
        com.bookx.bookx_backend.dto.BookDto bookDtoReturnedByService = new com.bookx.bookx_backend.dto.BookDto();
        bookDtoReturnedByService.setId(99L); // Some ID
        bookDtoReturnedByService.setTitle(inputBookDtoForListing.getTitle());
        when(bookService.findOrCreateBook(any(com.bookx.bookx_backend.dto.BookDto.class)))
                .thenReturn(bookDtoReturnedByService);

        // Mock bookRepository.findById to return empty, simulating inconsistency
        when(bookRepository.findById(bookDtoReturnedByService.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.createBookListing(inputListingDto, authenticatedUsername);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Critical error: Canonical book with ID " + bookDtoReturnedByService.getId() + " not found"));

        verify(userRepository).findByUsername(authenticatedUsername);
        verify(bookService).findOrCreateBook(any(com.bookx.bookx_backend.dto.BookDto.class));
        verify(bookRepository).findById(bookDtoReturnedByService.getId());
        verify(bookListingRepository, never()).save(any(BookListing.class));
    }

    @Test
    void createBookListing_saveFails_throwsDataAccessException() {
        // Arrange
        String authenticatedUsername = "testuser";

        com.bookx.bookx_backend.dto.BookDto inputBookDtoForListing = new com.bookx.bookx_backend.dto.BookDto();
        inputBookDtoForListing.setTitle("Save Fail Book");
        inputBookDtoForListing.setAuthor("Author Test");
        inputBookDtoForListing.setIsbn("1234567890123"); // Crucial: Added ISBN

        com.bookx.bookx_backend.dto.BookListingDto inputListingDto = new com.bookx.bookx_backend.dto.BookListingDto();
        inputListingDto.setBook(inputBookDtoForListing);
        inputListingDto.setListingType(com.bookx.bookx_backend.model.enums.ListingType.FOR_SALE);
        inputListingDto.setPrice(new java.math.BigDecimal("29.99"));
        inputListingDto.setCondition(com.bookx.bookx_backend.model.enums.BookCondition.GOOD);
        inputListingDto.setLocationLongitude(15.0);
        inputListingDto.setLocationLatitude(25.0);
        inputListingDto.setListerNotes("Attempting to save this should fail at repository level.");

        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(testUser));

        com.bookx.bookx_backend.dto.BookDto bookDtoReturnedByService = new com.bookx.bookx_backend.dto.BookDto();
        bookDtoReturnedByService.setId(2L); // Assuming an ID is returned
        bookDtoReturnedByService.setTitle(inputBookDtoForListing.getTitle());
        bookDtoReturnedByService.setAuthor(inputBookDtoForListing.getAuthor());
        bookDtoReturnedByService.setIsbn(inputBookDtoForListing.getIsbn());
        when(bookService.findOrCreateBook(any(com.bookx.bookx_backend.dto.BookDto.class)))
                .thenReturn(bookDtoReturnedByService);

        Book bookEntityForListing = new Book();
        bookEntityForListing.setId(bookDtoReturnedByService.getId());
        bookEntityForListing.setTitle(bookDtoReturnedByService.getTitle());
        bookEntityForListing.setAuthor(bookDtoReturnedByService.getAuthor());
        bookEntityForListing.setIsbn(bookDtoReturnedByService.getIsbn());
        when(bookRepository.findById(bookDtoReturnedByService.getId()))
                .thenReturn(Optional.of(bookEntityForListing));

        // Mock bookListingRepository.save to throw an exception
        when(bookListingRepository.save(any(BookListing.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Simulated save failure"));

        // Act & Assert
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            bookListingService.createBookListing(inputListingDto, authenticatedUsername);
        });

        verify(userRepository).findByUsername(authenticatedUsername);
        verify(bookService).findOrCreateBook(any(com.bookx.bookx_backend.dto.BookDto.class));
        verify(bookRepository).findById(bookDtoReturnedByService.getId());
        verify(bookListingRepository).save(any(BookListing.class)); // Verify save was attempted
    }

    // --- Tests for updateBookListing ---

    @Test
    void updateBookListing_success() {
        // Arrange
        Long listingId = 1L;
        String authenticatedUsername = "testuser"; // Matches testUser from setUp()

        // Original BookListing entity data
        Book originalBook = new Book();
        originalBook.setId(10L);
        originalBook.setTitle("Original Title");
        // Ensure other fields are set if your Book entity's equals/hashCode uses them

        BookListing originalListing = new BookListing();
        originalListing.setId(listingId);
        originalListing.setLister(testUser); // testUser is the owner
        originalListing.setBook(originalBook);
        originalListing.setListingType(com.bookx.bookx_backend.model.enums.ListingType.FOR_LEND);
        originalListing.setListingStatus(com.bookx.bookx_backend.model.enums.ListingCurrentStatus.AVAILABLE);
        originalListing.setCondition(com.bookx.bookx_backend.model.enums.BookCondition.GOOD);
        originalListing.setPrice(new java.math.BigDecimal("5.00"));
        originalListing.setListerNotes("Original notes");
        originalListing.setLocationLatitude(10.0);
        originalListing.setLocationLongitude(20.0);
        originalListing.setCreatedAt(java.time.Instant.now().minusSeconds(3600));
        originalListing.setUpdatedAt(java.time.Instant.now().minusSeconds(3600));

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.of(originalListing));

        // DTO with updates
        com.bookx.bookx_backend.dto.BookListingDto updateDto = new com.bookx.bookx_backend.dto.BookListingDto();
        updateDto.setListingType(com.bookx.bookx_backend.model.enums.ListingType.FOR_SALE);
        updateDto.setListingStatus(com.bookx.bookx_backend.model.enums.ListingCurrentStatus.RESERVED);
        updateDto.setCondition(com.bookx.bookx_backend.model.enums.BookCondition.FAIR);
        updateDto.setPrice(new java.math.BigDecimal("10.99"));
        updateDto.setListerNotes("Updated notes here");
        updateDto.setLocationLatitude(12.34);
        updateDto.setLocationLongitude(56.78);
        // ID, BookDto, and Lister in updateDto are typically ignored by the service method for update

        // Mock the save operation to return the updated entity
        // The service method modifies 'originalListing' instance and saves it.
        when(bookListingRepository.save(any(BookListing.class))).thenAnswer(invocation -> {
            BookListing saved = invocation.getArgument(0);
            // Simulate JPA updating the timestamp
            saved.setUpdatedAt(java.time.Instant.now());
            return saved;
        });

        // Act
        com.bookx.bookx_backend.dto.BookListingDto resultDto = bookListingService.updateBookListing(listingId, updateDto, authenticatedUsername);

        // Assert
        assertNotNull(resultDto);
        assertEquals(listingId, resultDto.getId());
        assertEquals(updateDto.getListingType(), resultDto.getListingType());
        assertEquals(updateDto.getListingStatus(), resultDto.getListingStatus());
        assertEquals(updateDto.getCondition(), resultDto.getCondition());
        assertEquals(0, updateDto.getPrice().compareTo(resultDto.getPrice()));
        assertEquals(updateDto.getListerNotes(), resultDto.getListerNotes());
        assertEquals(updateDto.getLocationLatitude(), resultDto.getLocationLatitude());
        assertEquals(updateDto.getLocationLongitude(), resultDto.getLocationLongitude());
        assertNotNull(resultDto.getUpdatedAt());
        // Check that createdAt is not changed, and updatedAt is more recent or changed
        assertEquals(originalListing.getCreatedAt(), resultDto.getCreatedAt());
        // Ensure there's a slight delay for Instant.now() to be different, or allow equality if execution is too fast
        assertTrue(resultDto.getUpdatedAt().isAfter(originalListing.getUpdatedAt()) || resultDto.getUpdatedAt().equals(originalListing.getUpdatedAt()) || resultDto.getUpdatedAt().isAfter(originalListing.getUpdatedAt().minusNanos(1000000))); // Allow for slight timing variations

        // Verify interactions
        verify(bookListingRepository).findById(listingId);
        // Use argThat for detailed verification of the saved entity's state
        verify(bookListingRepository).save(argThat(savedListing ->
            savedListing.getId().equals(listingId) &&
            savedListing.getLister().equals(testUser) && // Owner should not change
            savedListing.getBook().equals(originalBook) && // Book should not change
            savedListing.getListingType() == updateDto.getListingType() &&
            savedListing.getListingStatus() == updateDto.getListingStatus() &&
            savedListing.getCondition() == updateDto.getCondition() &&
            savedListing.getPrice().compareTo(updateDto.getPrice()) == 0 &&
            savedListing.getListerNotes().equals(updateDto.getListerNotes()) &&
            savedListing.getLocationLatitude().equals(updateDto.getLocationLatitude()) &&
            savedListing.getLocationLongitude().equals(updateDto.getLocationLongitude())
        ));
    }

    @Test
    void updateBookListing_notFound() {
        // Arrange
        Long listingId = 99L; // Non-existent ID
        String authenticatedUsername = "testuser";
        BookListingDto updateDto = new BookListingDto(); // Content doesn't matter much here

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.updateBookListing(listingId, updateDto, authenticatedUsername);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book listing with ID " + listingId + " not found."));
        verify(bookListingRepository).findById(listingId);
        verify(bookListingRepository, never()).save(any(BookListing.class));
    }

    @Test
    void updateBookListing_forbidden() {
        // Arrange
        Long listingId = 1L;
        String authenticatedUsername = "anotherUser"; // This user is trying to update

        User ownerUser = new User();
        ownerUser.setId(2L);
        ownerUser.setUsername("testuser"); // The actual owner of the listing from setUp()

        // originalListing is set up with testUser as lister in updateBookListing_success
        // For this test, we need to ensure the testListing (from global setUp) has its lister explicitly set if not already
        // or create a new one for clarity.
        BookListing existingListingOwnedByTestUser = new BookListing();
        existingListingOwnedByTestUser.setId(listingId);
        existingListingOwnedByTestUser.setLister(testUser); // testUser (username: "testuser") owns the listing
        existingListingOwnedByTestUser.setBook(testBook);

        BookListingDto updateDto = new BookListingDto();
        updateDto.setListerNotes("Attempting unauthorized update");

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.of(existingListingOwnedByTestUser));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.updateBookListing(listingId, updateDto, authenticatedUsername);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("You are not authorized to update this listing."));
        verify(bookListingRepository).findById(listingId);
        verify(bookListingRepository, never()).save(any(BookListing.class));
    }

    // --- Tests for getBookListingById ---

    @Test
    void getBookListingById_success() {
        // Arrange
        Long listingId = 1L;
        // testListing, testUser, and testBook are available from setUp()
        // Ensure testListing has all necessary fields for DTO mapping
        testListing.setListingType(ListingType.FOR_SALE);
        testListing.setPrice(BigDecimal.TEN);
        testListing.setCondition(BookCondition.GOOD);
        testListing.setListingStatus(ListingCurrentStatus.AVAILABLE);
        testListing.setListerNotes("Some notes");
        testListing.setLocationLatitude(10.0);
        testListing.setLocationLongitude(20.0);
        testListing.setCreatedAt(java.time.Instant.now());
        testListing.setUpdatedAt(java.time.Instant.now());

        when(bookListingRepository.findById(listingId)).thenReturn(Optional.of(testListing));

        // Act
        BookListingDto resultDto = bookListingService.getBookListingById(listingId);

        // Assert
        assertNotNull(resultDto);
        assertEquals(testListing.getId(), resultDto.getId());
        assertEquals(testListing.getListingType(), resultDto.getListingType());
        assertEquals(0, testListing.getPrice().compareTo(resultDto.getPrice()));
        assertEquals(testListing.getCondition(), resultDto.getCondition());
        assertEquals(testListing.getListingStatus(), resultDto.getListingStatus());
        assertEquals(testListing.getListerNotes(), resultDto.getListerNotes());
        assertEquals(testListing.getLocationLatitude(), resultDto.getLocationLatitude());
        assertEquals(testListing.getLocationLongitude(), resultDto.getLocationLongitude());

        assertNotNull(resultDto.getBook());
        assertEquals(testBook.getId(), resultDto.getBook().getId());
        assertEquals(testBook.getTitle(), resultDto.getBook().getTitle());

        assertNotNull(resultDto.getLister());
        assertEquals(testUser.getId(), resultDto.getLister().getId());
        assertEquals(testUser.getUsername(), resultDto.getLister().getUsername());

        verify(bookListingRepository).findById(listingId);
    }

    @Test
    void getBookListingById_notFound() {
        // Arrange
        Long listingId = 99L; // Non-existent ID
        when(bookListingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.getBookListingById(listingId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book listing with ID " + listingId + " not found."));
        verify(bookListingRepository).findById(listingId);
    }

    // --- Tests for getBookListingsByLister ---

    @Test
    void getBookListingsByLister_success() {
        // Arrange
        Long userId = 1L; // Corresponds to testUser.getId()
        BookListing listing1 = new BookListing(); // Simplified for brevity, assume mapping is tested elsewhere
        listing1.setId(10L);
        listing1.setLister(testUser);
        listing1.setBook(testBook);
        listing1.setListingType(ListingType.FOR_SALE);

        BookListing listing2 = new BookListing();
        listing2.setId(11L);
        listing2.setLister(testUser);
        listing2.setBook(new Book()); // Different book instance
        listing2.getBook().setId(2L);
        listing2.setListingType(ListingType.FOR_LEND);

        List<BookListing> listings = Arrays.asList(listing1, listing2);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(bookListingRepository.findByListerId(userId)).thenReturn(listings);

        // Act
        List<BookListingDto> resultDtos = bookListingService.getBookListingsByLister(userId);

        // Assert
        assertNotNull(resultDtos);
        assertEquals(2, resultDtos.size());
        // Basic check, detailed DTO mapping is assumed to be tested by getBookListingById_success etc.
        assertEquals(listing1.getId(), resultDtos.get(0).getId());
        assertEquals(listing2.getId(), resultDtos.get(1).getId());

        verify(userRepository).existsById(userId);
        verify(bookListingRepository).findByListerId(userId);
    }

    @Test
    void getBookListingsByLister_userNotFound() {
        // Arrange
        Long userId = 99L; // Non-existent user ID
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.getBookListingsByLister(userId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User with ID " + userId + " not found."));

        verify(userRepository).existsById(userId);
        verify(bookListingRepository, never()).findByListerId(anyLong());
    }

    @Test
    void getBookListingsByLister_userHasNoListings() {
        // Arrange
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(bookListingRepository.findByListerId(userId)).thenReturn(Collections.emptyList());

        // Act
        List<BookListingDto> resultDtos = bookListingService.getBookListingsByLister(userId);

        // Assert
        assertNotNull(resultDtos);
        assertTrue(resultDtos.isEmpty());

        verify(userRepository).existsById(userId);
        verify(bookListingRepository).findByListerId(userId);
    }

    // --- Tests for getBookListingsForBook ---

    @Test
    void getBookListingsForBook_success() {
        // Arrange
        Long bookId = 1L; // Corresponds to testBook.getId()

        BookListing listing1 = new BookListing();
        listing1.setId(20L);
        listing1.setBook(testBook);
        listing1.setLister(testUser);
        listing1.setListingType(ListingType.FOR_SALE);

        BookListing listing2 = new BookListing();
        listing2.setId(21L);
        listing2.setBook(testBook);
        listing2.setLister(new User()); // Potentially a different lister
        listing2.getLister().setId(2L);
        listing2.setListingType(ListingType.FOR_LEND);

        List<BookListing> listingsForBook = Arrays.asList(listing1, listing2);

        when(bookRepository.existsById(bookId)).thenReturn(true);
        when(bookListingRepository.findByBookId(bookId)).thenReturn(listingsForBook);

        // Act
        List<BookListingDto> resultDtos = bookListingService.getBookListingsForBook(bookId);

        // Assert
        assertNotNull(resultDtos);
        assertEquals(2, resultDtos.size());
        assertEquals(listing1.getId(), resultDtos.get(0).getId());
        assertEquals(listing2.getId(), resultDtos.get(1).getId());

        verify(bookRepository).existsById(bookId);
        verify(bookListingRepository).findByBookId(bookId);
    }

    @Test
    void getBookListingsForBook_bookNotFound() {
        // Arrange
        Long bookId = 99L; // Non-existent book ID
        when(bookRepository.existsById(bookId)).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.getBookListingsForBook(bookId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Book with ID " + bookId + " not found."));

        verify(bookRepository).existsById(bookId);
        verify(bookListingRepository, never()).findByBookId(anyLong());
    }

    @Test
    void getBookListingsForBook_bookHasNoListings() {
        // Arrange
        Long bookId = 1L;
        when(bookRepository.existsById(bookId)).thenReturn(true);
        when(bookListingRepository.findByBookId(bookId)).thenReturn(Collections.emptyList());

        // Act
        List<BookListingDto> resultDtos = bookListingService.getBookListingsForBook(bookId);

        // Assert
        assertNotNull(resultDtos);
        assertTrue(resultDtos.isEmpty());

        verify(bookRepository).existsById(bookId);
        verify(bookListingRepository).findByBookId(bookId);
    }

    @Test
    void searchBookListings_noFilters_returnsAllAvailable() {
        Pageable pageable = PageRequest.of(0, 10);
        BookListing listing1 = new BookListing();
        listing1.setId(1L);
        listing1.setBook(testBook);
        listing1.setLister(testUser);

        BookListing listing2 = new BookListing();
        listing2.setId(2L);
        listing2.setBook(new Book());
        listing2.getBook().setId(2L);
        listing2.setLister(testUser);

        List<BookListing> mockListings = Arrays.asList(listing1, listing2);
        Page<BookListing> mockPage = new PageImpl<>(mockListings, pageable, mockListings.size());

        when(bookListingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(mockPage);

        Page<BookListingDto> resultPage = bookListingService.searchBookListings(
                null, null, null, null, null, null, null, null, null, null, null, null, pageable);

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(2, resultPage.getContent().size());

        verify(bookListingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchBookListings_invalidRange_throwsBadRequest() {
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.searchBookListings(
                    null, null, null, null, null, null,
                    50.0, 40.0,
                    null, null, null, null, pageable);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("minLatitude cannot be greater than maxLatitude"));

        exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.searchBookListings(
                    null, null, null, null, null, null,
                    null, null,
                    50.0, 40.0,
                    null, null, pageable);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("minLongitude cannot be greater than maxLongitude"));

        exception = assertThrows(ResponseStatusException.class, () -> {
            bookListingService.searchBookListings(
                    null, null, null, null, null, null,
                    null, null, null, null,
                    BigDecimal.TEN, BigDecimal.ONE,
                    pageable);
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("minPrice cannot be greater than maxPrice"));

        verify(bookListingRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // We will add more tests here for other scenarios of deleteBookListing and other methods.
}