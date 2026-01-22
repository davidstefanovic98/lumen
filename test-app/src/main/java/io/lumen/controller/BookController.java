package io.lumen.controller;

import io.lumen.data.BookDto;
import io.lumen.web.annotation.*;
import io.lumen.web.http.ResponseEntity;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @GetMapping(value = "/{id}")
    public String getBook(@PathVariable("id") int id) {
        return "Book #" + id;
    }

    @PostMapping("/body")
    public String createBookJson(@RequestBody BookDto book) {
        return "Created book: " + book.getTitle() + " by " + book.getAuthor();
    }

    @GetMapping("/not-required")
    public ResponseEntity<String> getNotRequired(@RequestParam(required = false) Integer id) {
        return ResponseEntity.ok("Book #" + id);
    }

    @GetMapping
    public ResponseEntity<BookDto> getBookDto() {
        return ResponseEntity.ok(new BookDto("1984", "George Orwell"));
    }
}
