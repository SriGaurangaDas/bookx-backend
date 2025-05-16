package com.bookx.bookx_backend.service;

import com.bookx.bookx_backend.dto.BookDto;
import com.bookx.bookx_backend.dto.BookListingDto;
import com.bookx.bookx_backend.dto.UserSummaryDto;
import com.bookx.bookx_backend.model.Book;
import com.bookx.bookx_backend.model.BookListing;
import com.bookx.bookx_backend.model.User;
import com.bookx.bookx_backend.model.enums.ListingCurrentStatus;
import com.bookx.bookx_backend.repository.BookListingRepository;
import com.bookx.bookx_backend.repository.BookRepository;
import com.bookx.bookx_backend.repository.UserRepository;
import com.bookx.bookx_backend.repository.specification.BookListingSpecifications;
import com.bookx.bookx_backend.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import com.bookx.bookx_backend.model.enums.BookCondition;
import com.bookx.bookx_backend.model.enums.ListingType;

@Service
@RequiredArgsConstructor
public class BookListingService {

    private final BookListingRepository bookListingRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookService bookService;

    @Transactional
    public BookListingDto createBookListing(BookListingDto bookListingDto, String authenticatedUsername) {
        User lister = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found"));

        BookDto bookDetailsFromListing = getBookDto(bookListingDto);

        BookDto canonicalBookDto = bookService.findOrCreateBook(bookDetailsFromListing);

        Book bookEntity = bookRepository.findById(canonicalBookDto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "Critical error: Canonical book with ID " + canonicalBookDto.getId() + " not found after findOrCreate operation."));

        BookListing bookListing = mapDtoToEntity(bookListingDto);
        bookListing.setLister(lister);
        bookListing.setBook(bookEntity);

        if (bookListing.getListingStatus() == null) {
            bookListing.setListingStatus(ListingCurrentStatus.AVAILABLE);
        }

        BookListing savedListing = bookListingRepository.save(bookListing);
        BookListingDto resultDto = mapEntityToDto(savedListing);
        resultDto.setBook(canonicalBookDto);
        return resultDto;
    }

    private static BookDto getBookDto(BookListingDto bookListingDto) {
        BookDto bookDetailsFromListing = bookListingDto.getBook();
        if (bookDetailsFromListing == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book details (title, author, ISBN) must be provided to create a listing.");
        }
        if (bookDetailsFromListing.getIsbn() == null || bookDetailsFromListing.getIsbn().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book ISBN must be provided in the book details.");
        }
        return bookDetailsFromListing;
    }

    @Transactional(readOnly = true)
    public BookListingDto getBookListingById(Long listingId) {
        BookListing listing = bookListingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book listing with ID " + listingId + " not found."));
        return mapEntityToDto(listing);
    }

    @Transactional(readOnly = true)
    public List<BookListingDto> getBookListingsByLister(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + userId + " not found.");
        }
        return bookListingRepository.findByListerId(userId).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookListingDto> getBookListingsForBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book with ID " + bookId + " not found.");
        }
        return bookListingRepository.findByBookId(bookId).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookListingDto updateBookListing(Long listingId, BookListingDto bookListingDto, String authenticatedUsername) {
        BookListing existingListing = bookListingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book listing with ID " + listingId + " not found."));

        User lister = existingListing.getLister();
        if (!lister.getUsername().equals(authenticatedUsername)) {
            // TODO: Add role-based authorization for admins to update any listing
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this listing.");
        }

        // Update mutable fields
        // Note: The associated Book (existingListing.getBook()) is generally not changed during a listing update.
        // If the book itself needs to change, it's usually a new listing.
        if (bookListingDto.getListingType() != null) {
            existingListing.setListingType(bookListingDto.getListingType());
        }
        if (bookListingDto.getListingStatus() != null) {
            existingListing.setListingStatus(bookListingDto.getListingStatus());
        }
        if (bookListingDto.getCondition() != null) {
            existingListing.setCondition(bookListingDto.getCondition());
        }
        if (bookListingDto.getPrice() != null) {
            existingListing.setPrice(bookListingDto.getPrice());
        }
        // Lister notes can be updated to null or empty if desired by the user
        existingListing.setListerNotes(bookListingDto.getListerNotes());

        // Location can be updated
        existingListing.setLocationLatitude(bookListingDto.getLocationLatitude());
        existingListing.setLocationLongitude(bookListingDto.getLocationLongitude());

        BookListing updatedListing = bookListingRepository.save(existingListing);
        return mapEntityToDto(updatedListing);
    }

    @Transactional
    public void deleteBookListing(Long listingId, String authenticatedUsername) {
        BookListing listing = bookListingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book listing with ID " + listingId + " not found."));

        User lister = listing.getLister();
        if (!lister.getUsername().equals(authenticatedUsername)) {
            // TODO: Add role-based authorization for admins to delete any listing
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this listing.");
        }

        Book book = listing.getBook(); // Get the associated book before deleting the listing

        bookListingRepository.delete(listing);
        bookListingRepository.flush(); // Ensure delete is processed before checking count

        // Check if the book has any remaining listings
        long remainingListingsCount = bookListingRepository.countByBookId(book.getId());
        if (remainingListingsCount == 0) {
            // If no listings remain for this book, delete the canonical book entry itself
            bookRepository.delete(book);
        }
    }

    @Transactional(readOnly = true)
    public Page<BookListingDto> searchBookListings(
            String titleKeyword,
            String authorKeyword,
            String isbn,
            ListingType listingType,
            ListingCurrentStatus listingStatus,
            BookCondition condition,
            Double minLatitude,
            Double maxLatitude,
            Double minLongitude,
            Double maxLongitude,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            Pageable pageable) {

        // Validate range parameters
        if (minLatitude != null && maxLatitude != null && minLatitude > maxLatitude) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLatitude cannot be greater than maxLatitude.");
        }
        if (minLongitude != null && maxLongitude != null && minLongitude > maxLongitude) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minLongitude cannot be greater than maxLongitude.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot be greater than maxPrice.");
        }

        Specification<BookListing> spec = Specification.where(BookListingSpecifications.titleContains(titleKeyword))
                .and(BookListingSpecifications.authorContains(authorKeyword))
                .and(BookListingSpecifications.isbnEquals(isbn))
                .and(BookListingSpecifications.hasListingType(listingType))
                .and(BookListingSpecifications.hasListingStatus(listingStatus))
                .and(BookListingSpecifications.hasCondition(condition))
                .and(BookListingSpecifications.inLocationRange(minLatitude, maxLatitude, minLongitude, maxLongitude))
                .and(BookListingSpecifications.priceBetween(minPrice, maxPrice));

        Page<BookListing> resultsPage = bookListingRepository.findAll(spec, pageable);

        List<BookListingDto> dtoList = resultsPage.getContent().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, resultsPage.getTotalElements());
    }

    public BookListingDto mapEntityToDto(BookListing entity) {
        if (entity == null) return null;
        return BookListingDto.builder()
                .id(entity.getId())
                .book(mapBookEntityToBookDto(entity.getBook()))
                .lister(mapUserEntityToUserSummaryDto(entity.getLister()))
                .listingType(entity.getListingType())
                .listingStatus(entity.getListingStatus())
                .condition(entity.getCondition())
                .price(entity.getPrice())
                .listerNotes(entity.getListerNotes())
                .locationLatitude(entity.getLocationLatitude())
                .locationLongitude(entity.getLocationLongitude())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public BookListing mapDtoToEntity(BookListingDto dto) {
        if (dto == null) return null;
        BookListing entity = BookListing.builder()
                .listingType(dto.getListingType())
                .listingStatus(dto.getListingStatus())
                .condition(dto.getCondition())
                .price(dto.getPrice())
                .listerNotes(dto.getListerNotes())
                .locationLatitude(dto.getLocationLatitude())
                .locationLongitude(dto.getLocationLongitude())
                .build();
        return entity;
    }

    private BookDto mapBookEntityToBookDto(Book bookEntity) {
        if (bookEntity == null) return null;
        return BookDto.builder()
                .id(bookEntity.getId())
                .title(bookEntity.getTitle())
                .author(bookEntity.getAuthor())
                .isbn(bookEntity.getIsbn())
                .description(bookEntity.getDescription())
                .coverImageUrl(bookEntity.getCoverImageUrl())
                .build();
    }

    private UserSummaryDto mapUserEntityToUserSummaryDto(User userEntity) {
        if (userEntity == null) return null;
        return UserSummaryDto.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .build();
    }
}