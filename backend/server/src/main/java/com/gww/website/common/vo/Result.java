package com.gww.website.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 所有JSON接口使用统一返回结构，文件下载接口除外。 */
@Schema(description = "统一响应")
public record Result<T>(int code, String msg, T data) {
  public static <T> Result<T> ok(T data) {
    return new Result<>(0, "成功", data);
  }

  public static Result<Void> error(int code, String message) {
    return new Result<>(code, message, null);
  }
}
