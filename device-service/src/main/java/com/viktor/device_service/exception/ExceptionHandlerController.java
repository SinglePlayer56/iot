package com.viktor.device_service.exception;

import com.viktor.device.dto.ErrorResponseDto;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(NotFoundException.class)
    public @ResponseBody ErrorResponseDto handleNotFound(NotFoundException e, HttpServletResponse response) {
        return buildError(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public @ResponseBody ErrorResponseDto handleAlreadyExists(AlreadyExistsException e, HttpServletResponse response) {
        return buildError(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DevicePublishException.class)
    public @ResponseBody ErrorResponseDto handleDevicePublish(DevicePublishException e, HttpServletResponse response) {
        return buildError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public @ResponseBody ErrorResponseDto handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletResponse response
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(ef -> ef.getField() + ": " + ef.getDefaultMessage())
                .orElse("Validation error");

        return buildError(response, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public @ResponseBody ErrorResponseDto handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletResponse response
    ) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("Validation error");

        return buildError(response, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    private ErrorResponseDto buildError(HttpServletResponse response, int code, String message) {
        response.setStatus(code);

        var dto = new ErrorResponseDto();
        dto.setCode(code);
        dto.setMessage(message);
        dto.setTimestamp(System.currentTimeMillis());
        dto.setTraceId(Span.current().getSpanContext().getTraceId());

        return dto;
    }
}
