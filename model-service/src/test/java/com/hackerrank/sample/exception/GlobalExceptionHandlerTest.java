package com.hackerrank.sample.exception;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackerrank.sample.model.Model;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

public class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void handleBadRequestReturnsExpectedResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/model");
        MDC.put("traceId", "trace-123");

        try {
            ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                    new BadResourceRequestException("id is required."),
                    request
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(400, response.getBody().status());
            assertEquals("Bad Request", response.getBody().error());
            assertEquals("BAD_REQUEST", response.getBody().code());
            assertEquals("id is required.", response.getBody().message());
            assertEquals("/model", response.getBody().path());
            assertEquals("trace-123", response.getBody().traceId());
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    public void handleNotFoundReturnsExpectedResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/model/1");
        MDC.put("traceId", "trace-456");

        try {
            ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                    new NoSuchResourceFoundException("No model with given id found."),
                    request
            );

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(404, response.getBody().status());
            assertEquals("Not Found", response.getBody().error());
            assertEquals("NOT_FOUND", response.getBody().code());
            assertEquals("No model with given id found.", response.getBody().message());
            assertEquals("/model/1", response.getBody().path());
            assertEquals("trace-456", response.getBody().traceId());
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    public void handleValidationReturnsConciseMessage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/model");

        Method method = TestController.class.getDeclaredMethod("handle", Model.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Model(), "model");
        bindingResult.addError(new FieldError("model", "id", "id is required"));
        bindingResult.addError(new FieldError("model", "name", "name is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "id: id is required; name: name is required",
                response.getBody().message()
        );
        assertEquals("VALIDATION_ERROR", response.getBody().code());
    }

    @Test
    public void handleUnreadableBodyReturnsExpectedMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/model");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Invalid JSON body.",
                (HttpInputMessage) null
        );

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid JSON body.", response.getBody().message());
        assertEquals("INVALID_JSON", response.getBody().code());
    }

    @Test
    public void handleUnexpectedReturnsExpectedMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/model");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"),
                request
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error.", response.getBody().message());
        assertEquals("UNEXPECTED_ERROR", response.getBody().code());
    }

    private static class TestController {
        void handle(Model model) {
        }
    }
}
