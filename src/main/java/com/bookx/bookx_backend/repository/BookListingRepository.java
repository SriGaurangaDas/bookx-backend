package com.bookx.bookx_backend.repository;

import com.bookx.bookx_backend.model.BookListing;
import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.model.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal; // For price queries later if needed

@Repository
public interface BookListingRepository extends JpaRepository<BookListing, Long>, JpaSpecificationExecutor<BookListing> {
    List<BookListing> findByListerId(Long userId);
    List<BookListing> findByBookId(Long bookId);
    long countByBookId(Long bookId); // Method to count listings by book ID

    // New methods based on BookListing attributes:
    List<BookListing> findByListingType(ListingType listingType);
    List<BookListing> findByListingStatus(ListingCurrentStatus listingStatus);
    List<BookListing> findByCondition(BookCondition condition);

    List<BookListing> findByListingTypeAndListingStatus(ListingType listingType, ListingCurrentStatus listingStatus);

    // Location-based search for listings
    List<BookListing> findByLocationLatitudeBetweenAndLocationLongitudeBetween(
            Double minLatitude, Double maxLatitude,
            Double minLongitude, Double maxLongitude
    );

    // Example for price-based search (primarily for FOR_SALE listings)
    List<BookListing> findByListingTypeAndPriceBetween(ListingType listingType, BigDecimal minPrice, BigDecimal maxPrice);

    // You can add more complex queries as your application's search needs evolve, for example:
    // Combining book title (from Book entity via join) with listing status:
    // @Query("SELECT bl FROM BookListing bl JOIN bl.book b WHERE b.title LIKE %:titleKeyword% AND bl.listingStatus = :status")
    // List<BookListing> findByBookTitleContainingAndListingStatus(String titleKeyword, ListingCurrentStatus status);
}