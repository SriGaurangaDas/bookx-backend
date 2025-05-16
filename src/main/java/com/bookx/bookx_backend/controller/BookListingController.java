package com.bookx.bookx_backend.controller;

import com.bookx.bookx_backend.dto.BookListingDto;
import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.model.enums.ListingType;
import com.bookx.bookx_backend.service.BookListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class BookListingController {

    private final BookListingService bookListingService;

    @PostMapping
    public ResponseEntity<BookListingDto> createBookListing(@Valid @RequestBody BookListingDto bookListingDto, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated to create a listing.");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        BookListingDto createdListing = bookListingService.createBookListing(bookListingDto, username);
        return new ResponseEntity<>(createdListing, HttpStatus.CREATED);
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<BookListingDto> getBookListingById(@PathVariable Long listingId) {
        BookListingDto bookListingDto = bookListingService.getBookListingById(listingId);
        return ResponseEntity.ok(bookListingDto);
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<BookListingDto>> getBookListingsForBook(@PathVariable Long bookId) {
        List<BookListingDto> listings = bookListingService.getBookListingsForBook(bookId);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookListingDto>> getBookListingsByLister(@PathVariable Long userId) {
        // Optional: Add check if authenticated user is requesting their own listings or if admin
        List<BookListingDto> listings = bookListingService.getBookListingsByLister(userId);
        return ResponseEntity.ok(listings);
    }

    @PutMapping("/{listingId}")
    public ResponseEntity<BookListingDto> updateBookListing(@PathVariable Long listingId,
                                                          @Valid @RequestBody BookListingDto bookListingDto,
                                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated to update a listing.");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        BookListingDto updatedListing = bookListingService.updateBookListing(listingId, bookListingDto, username);
        return ResponseEntity.ok(updatedListing);
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> deleteBookListing(@PathVariable Long listingId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated to delete a listing.");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        bookListingService.deleteBookListing(listingId, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookListingDto>> searchBookListings(
            @RequestParam(required = false) String titleKeyword,
            @RequestParam(required = false) String authorKeyword,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) ListingCurrentStatus listingStatus,
            @RequestParam(required = false) BookCondition condition,
            @RequestParam(required = false) Double minLatitude,
            @RequestParam(required = false) Double maxLatitude,
            @RequestParam(required = false) Double minLongitude,
            @RequestParam(required = false) Double maxLongitude,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {

        Page<BookListingDto> results = bookListingService.searchBookListings(
                titleKeyword, authorKeyword, isbn,
                listingType, listingStatus, condition,
                minLatitude, maxLatitude, minLongitude, maxLongitude,
                minPrice, maxPrice,
                pageable);
        return ResponseEntity.ok(results);
    }

}