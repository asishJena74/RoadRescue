package app.roadrescue.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<Map<String, Object>> api(ApiException error) {
    return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException error) {
    var fields = new LinkedHashMap<String, Object>();
    for (FieldError fieldError : error.getBindingResult().getFieldErrors()) {
      fields.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity.badRequest().body(Map.of("message", "Validation failed.", "issues", Map.of("fieldErrors", fields)));
  }

  @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class})
  ResponseEntity<Map<String, Object>> badRequest(Exception error) {
    return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> fallback(Exception error) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Unexpected server error."));
  }
}
