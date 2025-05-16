package com.bookx.bookx_backend.repository.specification;

import com.bookx.bookx_backend.model.Book;
import com.bookx.bookx_backend.model.BookListing;
import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.model.enums.ListingType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;


public class BookListingSpecifications {

    public static Specification<BookListing> titleContains(String titleKeyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(titleKeyword)) {
                return criteriaBuilder.conjunction(); // Always true if no keyword
            }
            Join<BookListing, Book> bookJoin = root.join("book");
            return criteriaBuilder.like(criteriaBuilder.lower(bookJoin.get("title")), "%" + titleKeyword.toLowerCase() + "%");
        };
    }

    public static Specification<BookListing> authorContains(String authorKeyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(authorKeyword)) {
                return criteriaBuilder.conjunction();
            }
            Join<BookListing, Book> bookJoin = root.join("book");
            return criteriaBuilder.like(criteriaBuilder.lower(bookJoin.get("author")), "%" + authorKeyword.toLowerCase() + "%");
        };
    }

    public static Specification<BookListing> isbnEquals(String isbn) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(isbn)) {
                return criteriaBuilder.conjunction();
            }
            Join<BookListing, Book> bookJoin = root.join("book");
            return criteriaBuilder.equal(bookJoin.get("isbn"), isbn);
        };
    }

    public static Specification<BookListing> hasListingType(ListingType listingType) {
        return (root, query, criteriaBuilder) -> {
            if (listingType == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("listingType"), listingType);
        };
    }

    public static Specification<BookListing> hasListingStatus(ListingCurrentStatus listingStatus) {
        return (root, query, criteriaBuilder) -> {
            if (listingStatus == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("listingStatus"), listingStatus);
        };
    }

    public static Specification<BookListing> hasCondition(BookCondition condition) {
        return (root, query, criteriaBuilder) -> {
            if (condition == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("condition"), condition);
        };
    }

    public static Specification<BookListing> inLocationRange(Double minLatitude, Double maxLatitude, Double minLongitude, Double maxLongitude) {
        return (root, query, criteriaBuilder) -> {
            if (minLatitude == null || maxLatitude == null || minLongitude == null || maxLongitude == null) {
                return criteriaBuilder.conjunction();
            }
            // TODO: Consider adding validation: minLat <= maxLat, minLon <= maxLon in service layer or here
            return criteriaBuilder.and(
                    criteriaBuilder.between(root.get("locationLatitude"), minLatitude, maxLatitude),
                    criteriaBuilder.between(root.get("locationLongitude"), minLongitude, maxLongitude)
            );
        };
    }

    public static Specification<BookListing> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            if (minPrice != null && maxPrice != null) {
                // TODO: Consider adding validation: minPrice <= maxPrice in service layer or here
                return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            // maxPrice != null
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
}