package com.bookx.bookx_backend.controller;

import com.bookx.bookx_backend.dto.BookDto;
import com.bookx.bookx_backend.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // POST /api/v1/books - Find or Create a new canonical book entry
    @PostMapping
    public ResponseEntity<BookDto> findOrCreateBook(@Valid @RequestBody BookDto bookDto) {
        // Authentication might be required to ensure only valid users can add to the catalog.
        // However, the concept of 'owner' of a canonical book is removed.
        // Authorization for this could be at a higher level (e.g. specific roles can add books).
        BookDto resultingBook = bookService.findOrCreateBook(bookDto);
        // If the book was found, HttpStatus.OK might be more appropriate than CREATED.
        // If it was created, CREATED is correct. Service can be designed to indicate this.
        // For simplicity, let's assume service handles and we return OK or CREATED based on outcome.
        // Or, always return CREATED if it's an idempotent findOrCreate.
        return ResponseEntity.status(HttpStatus.CREATED).body(resultingBook); // Or OK if found
    }

    // GET /api/v1/books/{id} - Get a book by ID
    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable Long id) {
        BookDto bookDto = bookService.getBookDtoById(id);
        return ResponseEntity.ok(bookDto);
    }

    // GET /api/v1/books - Get all books or search by title
    @GetMapping
    public ResponseEntity<List<BookDto>> getAllBooksOrSearchByTitle(
            @RequestParam(required = false) String titleKeyword
    ) {
        List<BookDto> books;
        if (titleKeyword != null && !titleKeyword.trim().isEmpty()) {
            books = bookService.searchBooksByTitle(titleKeyword);
        } else {
            books = bookService.getAllBooks();
        }
        return ResponseEntity.ok(books);
    }

    // PUT /api/v1/books/{id} - Update an existing book's canonical details
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBookDetails(@PathVariable Long id,
                                              @Valid @RequestBody BookDto bookDto) {
        // Authentication/Authorization to update canonical book details should be handled.
        // (e.g., by Spring Security method-level security on the service, or role checks here if simpler)
        // The currentUsername is no longer passed to the service method for ownership check.
        BookDto updatedBook = bookService.updateBookDetails(id, bookDto);
        return ResponseEntity.ok(updatedBook);
    }

    // DELETE /api/v1/books/{id} - Delete a canonical book entry
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        // Authentication/Authorization to delete canonical book details should be handled.
        // The currentUsername is no longer passed.
        bookService.deleteBook(id); // Service handles checks for existing listings
        return ResponseEntity.noContent().build();
    }
}
