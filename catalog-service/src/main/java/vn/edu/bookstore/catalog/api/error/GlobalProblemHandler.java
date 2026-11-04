package vn.edu.bookstore.catalog.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import vn.edu.bookstore.catalog.service.BookNotFoundException;
import vn.edu.bookstore.catalog.service.DuplicateIsbnException;
import vn.edu.bookstore.catalog.service.PreconditionFailedException;
import vn.edu.bookstore.catalog.service.PreconditionRequiredException;

@RestControllerAdvice
public class GlobalProblemHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    ProblemDetail notFound(BookNotFoundException e, HttpServletRequest req) {
        ProblemDetail p = problem(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy sách",
                e.getMessage(),
                req);
        p.setType(ProblemTypes.BOOK_NOT_FOUND);
        return p;
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    ProblemDetail duplicateIsbn(DuplicateIsbnException e, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT,
                "ISBN đã tồn tại", e.getMessage(), req);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    ProblemDetail preconditionFailed(
            PreconditionFailedException e, HttpServletRequest req) {
        return problem(HttpStatus.PRECONDITION_FAILED,
                "Phiên bản sách đã thay đổi", e.getMessage(), req);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    ProblemDetail preconditionRequired(
            PreconditionRequiredException e, HttpServletRequest req) {
        return problem(HttpStatus.PRECONDITION_REQUIRED,
                "Thiếu If-Match", e.getMessage(), req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(
            ConstraintViolationException e, HttpServletRequest req) {
        ProblemDetail p = problem(HttpStatus.BAD_REQUEST,
                "Dữ liệu không hợp lệ",
                "Tham số yêu cầu không hợp lệ",
                req);

        p.setProperty("errors", e.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", v.getPropertyPath().toString(),
                        "message", v.getMessage()))
                .toList());
        return p;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Thân yêu cầu không hợp lệ");
        p.setType(ProblemTypes.VALIDATION_FAILED);
        p.setTitle("Dữ liệu không hợp lệ");
        p.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null
                                ? "Giá trị không hợp lệ"
                                : error.getDefaultMessage()))
                .toList());

        return handleExceptionInternal(
                e, p, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "JSON không hợp lệ");
        p.setType(ProblemTypes.VALIDATION_FAILED);
        p.setTitle("Dữ liệu không hợp lệ");
        p.setProperty("errors", List.of(Map.of(
                "field", "body",
                "message", "Không thể đọc JSON trong yêu cầu")));

        return handleExceptionInternal(
                e, p, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setInstance(URI.create(req.getRequestURI()));
        return p;
    }
}
