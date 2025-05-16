package com.bookx.bookx_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {

    private Long id; // For responses

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Size(max = 255, message = "Author name cannot exceed 255 characters")
    private String author;

    @Size(max = 20, message = "ISBN cannot exceed 20 characters")
    private String isbn;

    private String description; // No specific validation for now, can be long

    @Size(max = 512, message = "Cover image URL cannot exceed 512 characters")
    private String coverImageUrl;
}