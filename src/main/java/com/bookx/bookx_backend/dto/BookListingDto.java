package com.bookx.bookx_backend.dto;

import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.model.enums.ListingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookListingDto {
    private Long id;
    private BookDto book; // Nested slim DTO for basic book info
    private UserSummaryDto lister; // Nested DTO for lister info

    private ListingType listingType;
    private ListingCurrentStatus listingStatus;
    private BookCondition condition;
    private BigDecimal price;
    private String listerNotes;
    private Double locationLatitude;
    private Double locationLongitude;
    private Instant createdAt;
    private Instant updatedAt;
}