package com.avalon.jpcap.common.exceptions;

import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.result.HttpResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolationException;
import java.util.List;

/**
 * @project: fastbe-order-ext
 * @description: Controller层异常捕获处理类
 * @author: DingHaoLun
 * @create: 2021-09-10 15:24
 **/

@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {

    /**
     *
     */
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    /**
     * 任意异常兜底
     */
    @ExceptionHandler(Exception.class)
    public HttpResult<?> exceptionHandler(Exception e) {
        log.info("controller任意异常Exception兜底", e);
        return HttpResult.failure("-1", "系统错误");
    }
//
//    /**
//     * 自定义参数异常类的处理
//     */
//    @ExceptionHandler(ParameterException.class)
//    public Result<?> parameterExceptionHandler(ParameterException e) {
//        log.info("controller入参异常{}", e);
//        return Result.failure(e.getCode(), e.getMessage());
//    }

    /**
     * 自定义业务异常类的处理
     */
    @ExceptionHandler(BusinessException.class)
    public HttpResult<?> businessExceptionHandler(BusinessException e) {
        log.info("controller业务异常", e);
        return HttpResult.failure(e.getErrorCode(), e.getMessage());
    }

    /**
     * 处理Get请求中 使用@Valid 验证路径中请求@RequestBody实体校验失败后抛出的异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public HttpResult BindExceptionHandler(BindException e) {
        log.info("controller入参异常", e);
        return HttpResult.failure("-1", e.getMessage());
    }

    /**
     * @RequestParam 修饰的字段 Controller层JRS303 @Valid参数校验处理失败后的异常处理
     * @see javax.validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public HttpResult<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        log.info("controller入参异常", e);
        return HttpResult.failure("-1", e.getMessage());
    }

    /**
     * @RequestBody 修饰的实体 Controller参数@Valid校验处理失败后的异常处理
     * @see javax.validation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public HttpResult<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.info("controller入参异常", e);
        BindingResult result = e.getBindingResult();
        if (result != null && result.hasErrors()) {
            List<ObjectError> errors = result.getAllErrors();
            StringBuilder defaultMessage = new StringBuilder("参数非法:");
            for (ObjectError eachError : errors) {
                FieldError fieldError = (FieldError) eachError;
                //返回自定义错误格式（返回@Valid定义的实体中的 message=xxx 的内容
                defaultMessage.append(fieldError.getDefaultMessage()).append(';');
            }
            return HttpResult.failure("-1", defaultMessage.toString());
        }
        //若异常至少返回参数非法的提示
        return HttpResult.failure("-1", "参数非法");
    }

    /**
     * http请求的@RequestBody JSON进行反序列化为 Controller的某个实体对象时无法正确反序列的异常 处理
     *
     * @see HttpMessageNotReadableException
     *///当不允许post、get请求什么都不传入参数的时候，加入 HttpMessageNotReadableException.class,
    @ExceptionHandler(value = {MissingServletRequestParameterException.class, HttpRequestMethodNotSupportedException.class})
    public HttpResult<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.info("controller入参异常", e);
        return HttpResult.failure("-1", "参数非法");
    }

    /**
     * 表单上传文件，文件大小超过springboot设置的大小限制
     * 1、springBoot默认Multipart文件上传大小限制为1MB, 超出大小捕获异常MaxUploadSizeExceededException
     * 2、springBoot2.0之后版本，可以在properties中设置
     * # 单个文件最大为20M
     * spring.servlet.multipart.max-file-size = 20MB
     * # 单次请求文件总数大小为20M
     * spring.servlet.multipart.max-request-size = 110MB
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public HttpResult<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.info("controller入参异常", e);
        return HttpResult.failure("-1", "单个上传文件大小不能超出{}",maxFileSize);
    }

}