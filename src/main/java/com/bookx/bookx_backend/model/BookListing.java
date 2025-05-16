package com.bookx.bookx_backend.model;

import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.model.enums.ListingType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "book_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booklisting_book"))
    private Book book; // Reference to the canonical Book entity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lister_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booklisting_lister_user"))
    private User lister; // The user who created this listing

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType listingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingCurrentStatus listingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition") // Reusing your existing BookCondition enum
    private BookCondition condition;

    @Column(precision = 10, scale = 2) // For monetary values
    private BigDecimal price; // Applicable if listingType is FOR_SALE

    @Lob
    @Column(columnDefinition = "TEXT")
    private String listerNotes; // Optional notes from the lister about this specific copy/listing

    // Location where the book is available from this lister
    private Double locationLatitude;
    private Double locationLongitude;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Example: A method to check if the listing is for sale and available
    public boolean isAvailableForSale() {
        return ListingType.FOR_SALE.equals(this.listingType) && ListingCurrentStatus.AVAILABLE.equals(this.listingStatus);
    }
}