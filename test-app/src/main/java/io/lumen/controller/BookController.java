package io.lumen.controller;

import io.lumen.data.BookDto;
import io.lumen.web.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @GetMapping("/{id}")
    public String getBook(@PathVariable("id") int id) {
        return "Book #" + id;
    }

    @PostMapping("/body")
    public String createBookJson(@RequestBody BookDto book) {
        return "Created book: " + book.getTitle() + " by " + book.getAuthor();
    }
}
