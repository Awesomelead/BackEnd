package kr.co.awesomelead.groupware_backend.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // MethodArgumentNotValidException 예외를 특별히 처리하는 메서드
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {

        // 에러 메시지를 담을 Map 생성
        Map<String, String> errors = new HashMap<>();

        // 예외 결과(BindingResult)에서 모든 필드 에러(FieldError)를 가져옴
        BindingResult bindingResult = ex.getBindingResult();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            // 어떤 필드에서 에러가 났는지(field)와 DTO에 설정한 메시지(message)를 Map에 담음
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // 400 Bad Request 상태 코드와 함께 에러 메시지를 담은 Map을 응답으로 보냄
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
