package com.bookx.bookx_backend.repository;

import com.bookx.bookx_backend.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // Basic CRUD operations (save, findById, findAll, deleteById, etc.)
    // are automatically provided by JpaRepository.
    List<Book> findByTitleContainingIgnoreCase(String titleKeyword);
    Optional<Book> findByIsbn(String isbn); // New method
}
