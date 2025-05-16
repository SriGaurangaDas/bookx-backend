package com.bookx.bookx_backend.service;

import com.bookx.bookx_backend.dto.BookDto;
import com.bookx.bookx_backend.model.Book;
import com.bookx.bookx_backend.repository.BookRepository;
import com.bookx.bookx_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {


    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // --- Public API Methods ---

    @Transactional
    public BookDto findOrCreateBook(BookDto bookDto) {
        // Try to find an existing book, e.g., by ISBN if it's a reliable unique identifier
        Optional<Book> existingBookOpt = Optional.empty();
        if (bookDto.getIsbn() != null && !bookDto.getIsbn().trim().isEmpty()) {
            // Assuming BookRepository might have findByIsbn(String isbn) or we add it.
            // For now, let's simulate, or if not, creation will just happen.
            existingBookOpt = bookRepository.findByIsbn(bookDto.getIsbn());
        }

        if (existingBookOpt.isPresent()) {
            return mapEntityToDto(existingBookOpt.get());
        } else {
            // Create a new canonical book entry
            Book book = Book.builder()
                    .title(bookDto.getTitle())
                    .author(bookDto.getAuthor())
                    .isbn(bookDto.getIsbn())
                    .description(bookDto.getDescription())
                    .coverImageUrl(bookDto.getCoverImageUrl())
                    .build();
            Book savedBook = bookRepository.save(book);
            return mapEntityToDto(savedBook);
        }
    }
    @Transactional(readOnly = true)
    public BookDto getBookDtoById(Long id) {
        Book book = findBookById(id);
        return mapEntityToDto(book);
    }

    @Transactional(readOnly = true)
    public List<BookDto> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDto updateBookDetails(Long bookId, BookDto bookDto) {
        Book existingBook = findBookById(bookId);
        // TODO: Add authorization logic (e.g., admin only)
        // Authorization: Who can update canonical book details? Admins?
        // For now, let's assume authorized if the method is reached.
        // The previous owner check is no longer valid.

        // Update core fields from DTO
        existingBook.setTitle(bookDto.getTitle());
        existingBook.setAuthor(bookDto.getAuthor());
        existingBook.setIsbn(bookDto.getIsbn());
        existingBook.setDescription(bookDto.getDescription());
        existingBook.setCoverImageUrl(bookDto.getCoverImageUrl());
        // Note: JPA's @UpdateTimestamp on Book entity would handle its own 'updatedAt' if present.

        Book updatedBook = bookRepository.save(existingBook);
        return mapEntityToDto(updatedBook);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        Book book = findBookById(bookId);
        // TODO: Add authorization logic (e.g., admin only)
        // Authorization: Who can delete canonical book details? Admins?
        // The previous owner check is no longer valid.

        // Check if there are any listings associated with this book
        if (book.getListings() != null && !book.getListings().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete book with ID " + bookId + ". It has active listings. Please delete listings first.");
        }
        bookRepository.delete(book);
    }

    // --- Search and Filter Methods ---



    @Transactional(readOnly = true)
    public List<BookDto> searchBooksByTitle(String titleKeyword) {
        return bookRepository.findByTitleContainingIgnoreCase(titleKeyword).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    // --- Helper / Private Methods ---

    private Book findBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found with id: " + id));
    }

    private BookDto mapEntityToDto(Book book) {
        if (book == null) return null;
        return BookDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .coverImageUrl(book.getCoverImageUrl())
                // Removed listing-specific fields
                .build();
    }

    // mapDtoToEntity is effectively inlined in findOrCreateBook for creation
    // and updateBookDetails for updates. A separate one isn't strictly needed now.


}