package com.zjgsu.lkz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 全局异常处理：参数无效 400，资源不存在 404，业务规则冲突 409，不一律转 500。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail handleRemote(org.springframework.web.server.ResponseStatusException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
    }

    /** 请求体校验失败（@Valid）。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "请求参数无效: " + firstFieldError(ex));
        problem.setTitle("参数无效");
        return problem;
    }

    /** 资源不存在（商品/订单 id 错误或路径不存在）。 */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("资源不存在");
        return problem;
    }

    /** 业务规则冲突（已售出、买自己的商品等）。 */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleConflict(BusinessRuleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("业务规则冲突");
        return problem;
    }

    /** 404 兜底：路径不存在时也返回结构化 ProblemDetail 而不是默认错误页。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "请求路径不存在: " + ex.getResourcePath());
        problem.setTitle("资源不存在");
        return problem;
    }

    private String firstFieldError(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("unknown");
    }
}
