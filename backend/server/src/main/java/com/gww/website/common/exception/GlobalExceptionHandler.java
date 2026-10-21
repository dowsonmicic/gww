package com.gww.website.common.exception;

import com.gww.website.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Result<Void>> business(BusinessException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(Result.error(ex.getStatus().value(), ex.getMessage()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HandlerMethodValidationException.class,
    HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Result<Void>> invalid(Exception ex) {
    return ResponseEntity.badRequest().body(Result.error(400, "参数格式或校验不正确"));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Result<Void>> large(Exception ex) {
    return ResponseEntity.status(413).body(Result.error(413, "文件超过允许大小"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> unexpected(Exception ex) {
    log.error("请求处理失败，异常类型={}", ex.getClass().getSimpleName(), ex);
    return ResponseEntity.status(500).body(Result.error(500, "服务暂不可用"));
  }
}
