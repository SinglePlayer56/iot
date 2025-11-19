package com.viktor.device_service.exception;

import com.viktor.device.dto.ErrorResponseDto;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler({NotFoundException.class})
    public @ResponseBody ErrorResponseDto handleNotFoundException(HttpServletResponse response, Exception e) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        String traceId = Span.current().getSpanContext().getTraceId();

        var errorResponseDto = new ErrorResponseDto();
        errorResponseDto.setCode(HttpServletResponse.SC_NOT_FOUND);
        errorResponseDto.setMessage(e.getMessage());
        errorResponseDto.setTimestamp(System.currentTimeMillis());
        errorResponseDto.setTraceId(traceId);

        return errorResponseDto;
    }

    @ExceptionHandler({AlreadyExistsException.class})
    public @ResponseBody ErrorResponseDto handleAlreadyExistsException(HttpServletResponse response, Exception e) {
        response.setStatus(HttpServletResponse.SC_CONFLICT);

        String traceId = Span.current().getSpanContext().getTraceId();

        var errorResponseDto = new ErrorResponseDto();
        errorResponseDto.setCode(HttpServletResponse.SC_CONFLICT);
        errorResponseDto.setMessage(e.getMessage());
        errorResponseDto.setTimestamp(System.currentTimeMillis());
        errorResponseDto.setTraceId(traceId);

        return errorResponseDto;
    }

    @ExceptionHandler({DevicePublishException.class})
    public @ResponseBody ErrorResponseDto handleDevicePublishException(HttpServletResponse response, Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        String traceId = Span.current().getSpanContext().getTraceId();

        var errorResponseDto = new ErrorResponseDto();
        errorResponseDto.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        errorResponseDto.setMessage(e.getMessage());
        errorResponseDto.setTimestamp(System.currentTimeMillis());
        errorResponseDto.setTraceId(traceId);

        return errorResponseDto;
    }
}
