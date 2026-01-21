package com.example.IncidentPulse.Exception;

import com.example.IncidentPulse.DTO.Response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<ErrorCode>> handleAppException(AppException appException){
        ErrorCode errorCode = appException.getErrorCode();
        return ResponseEntity.badRequest().body(
                ApiResponse.<ErrorCode>builder()
                        .code(errorCode.getCode())
                        .data(errorCode)
                        .now(LocalDateTime.now())
                        .message("Please read a error!!!")
                        .build());
    }

}
