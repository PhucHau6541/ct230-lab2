package vn.edu.bookstore.catalog.api;

import java.net.URI;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import vn.edu.bookstore.catalog.api.generated.BooksApi;
import vn.edu.bookstore.catalog.api.model.Book;
import vn.edu.bookstore.catalog.api.model.BookCreateRequest;
import vn.edu.bookstore.catalog.api.model.BookPage;
import vn.edu.bookstore.catalog.api.model.BookUpdateRequest;
import vn.edu.bookstore.catalog.domain.BookEntity;
import vn.edu.bookstore.catalog.service.BookService;
import vn.edu.bookstore.catalog.service.PreconditionRequiredException;

@RestController
public class BookController implements BooksApi {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<BookPage> listBooks(
            String q, Integer page, Integer size, String sort) {

        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;

        return ResponseEntity.ok(
                BookMapper.toPage(
                        service.list(q, PageRequest.of(
                                pageNumber,
                                pageSize,
                                toSort(sort)))));
    }

    static Sort toSort(String sort) {
        String[] parts = (sort == null || sort.isBlank())
                ? new String[]{"title", "asc"}
                : sort.split(",", 2);

        String property = switch (parts[0]) {
            case "price" -> "priceAmount";
            case "publishedYear" -> "publishedYear";
            default -> "title";
        };

        Sort.Direction direction = parts.length > 1
                && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, property)
                .and(Sort.by("isbn"));
    }

    @Override
    public ResponseEntity<Book> getBook(String isbn, String ifNoneMatch) {
        BookEntity e = service.get(isbn);
        String etag = BookMapper.etag(e);

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Book> createBook(BookCreateRequest request) {
        BookEntity e = service.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{isbn}")
                .buildAndExpand(e.getIsbn())
                .toUri();

        return ResponseEntity.created(location)
                .eTag(BookMapper.etag(e))
                .body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Book> updateBook(
            String isbn, BookUpdateRequest request, String ifMatch) {

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException("Thiếu header If-Match");
        }

        String versionText = ifMatch.replace("\"", "").trim();
        long expectedVersion = Long.parseLong(versionText);

        BookEntity e = service.update(isbn, request, expectedVersion);

        return ResponseEntity.ok()
                .eTag(BookMapper.etag(e))
                .body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Void> deleteBook(String isbn) {
        service.delete(isbn);
        return ResponseEntity.noContent().build();
    }
}
