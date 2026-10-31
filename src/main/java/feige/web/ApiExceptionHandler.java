package feige.web;

import feige.service.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handle(ApiException exception) {
        return body(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooBig() {
        return body(HttpStatus.BAD_REQUEST, "文件不能超过 100MB");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        Map<String, String> payload = new LinkedHashMap<String, String>();
        payload.put("message", message);
        return new ResponseEntity<Map<String, String>>(payload, status);
    }
}
