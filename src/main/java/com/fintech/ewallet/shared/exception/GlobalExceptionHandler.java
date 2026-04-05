package com.fintech.ewallet.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.RequiredArgsConstructor;

/**
 * Global exception handler for all REST controllers.
 * Converts exceptions into standardized {@link ApiErrorResponse} format.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

        private final MessageSource messageSource;

        /**
         * Handle domain/business-rule exceptions.
         */
        @ExceptionHandler(DomainException.class)
        public ResponseEntity<ApiErrorResponse> handleDomainException(
                        DomainException ex, HttpServletRequest request) {

                String localizedMessage = messageSource.getMessage(
                        ex.getErrorCode(), null, ex.getMessage(), LocaleContextHolder.getLocale());

                log.warn("Domain error: [{}] {}", ex.getErrorCode(), localizedMessage);

                ApiErrorResponse response = ApiErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code(ex.getErrorCode())
                                .message(localizedMessage)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Handle @Valid / @Validated bean validation failures.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {

                List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                                .toList();

                log.warn("Validation failed on {} fields at {}", fieldErrors.size(), request.getRequestURI());

                ApiErrorResponse response = ApiErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code("VALIDATION_FAILED")
                                .message(messageSource.getMessage("error.validation.invalid", null, "Request validation failed", LocaleContextHolder.getLocale()))
                                .details(fieldErrors)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Handle IllegalArgumentException (e.g., bad enum values, invalid inputs).
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex, HttpServletRequest request) {

                log.warn("Illegal argument: {}", ex.getMessage());

                ApiErrorResponse response = ApiErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code("INVALID_ARGUMENT")
                                .message(ex.getMessage()) // IllegalArgument usually holds dev-message, not user-safe
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Handle business rule violations (e.g., daily limit exceeded, insufficient
         * funds).
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ApiErrorResponse> handleIllegalState(
                        IllegalStateException ex, HttpServletRequest request) {

                log.warn("Business rule violation: {}", ex.getMessage());

                ApiErrorResponse response = ApiErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .code("BUSINESS_RULE_VIOLATION")
                                .message(ex.getMessage()) // Can map similarly if keys are passed
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.badRequest().body(response);
        }

        /**
         * Catch-all for unexpected server errors.
         * Do NOT expose stack traces in production.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleGenericException(
                        Exception ex, HttpServletRequest request) {

                log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

                String localizedMessage = messageSource.getMessage(
                        "error.internal", null, "An unexpected error occurred", LocaleContextHolder.getLocale());

                ApiErrorResponse response = ApiErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .code("INTERNAL_ERROR")
                                .message(localizedMessage)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}
