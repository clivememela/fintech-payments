package za.co.titandynamix.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "za.co.titandynamix.controller")
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(error("BAD_REQUEST", ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage(),
                        "rejectedValue", fe.getRejectedValue()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "Validation failed", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> details = ex.getConstraintViolations()
                .stream()
                .map(this::toViolationDetail)
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "Validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "An unexpected error occurred", null));
    }

    private Map<String, Object> error(String code, String message, List<Map<String, Object>> details) {
        String msg = message == null ? "" : (message.length() > 500 ? message.substring(0, 500) : message);
        if (details == null || details.isEmpty()) {
            return Map.of(
                    "timestamp", Instant.now().toString(),
                    "code", code,
                    "message", msg
            );
        }
        return Map.of(
                "timestamp", Instant.now().toString(),
                "code", code,
                "message", msg,
                "details", details
        );
    }

    private Map<String, Object> toViolationDetail(ConstraintViolation<?> v) {
        return Map.of(
                "property", v.getPropertyPath() == null ? "" : v.getPropertyPath().toString(),
                "message", v.getMessage(),
                "invalidValue", v.getInvalidValue()
        );
    }
}

