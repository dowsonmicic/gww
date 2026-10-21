package com.gww.website.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 可向客户端公开的业务错误，不携带磁盘路径或底层异常信息。 */
@Getter
public class BusinessException extends RuntimeException {
  private final HttpStatus status;

  public BusinessException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }
}
