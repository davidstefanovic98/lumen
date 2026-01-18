package io.lumen.controller;

import io.lumen.context.annotation.Controller;
import io.lumen.data.BookDto;
import io.lumen.web.annotation.*;

@Controller
public class BookController {

    @GetMapping("/books/{id}")
    public String getBook(@PathVariable("id") int id) {
        return "Book #" + id;
    }

    @PostMapping("/books")
    public String createBook(@RequestParam("title") String title,
                             @RequestParam("author") String author) {
        return "Created book: " + title + " by " + author;
    }

    @PostMapping("/books/body")
    public String createBookJson(@RequestBody BookDto book) {
        return "Created book: " + book.getTitle() + " by " + book.getAuthor();
    }
}
