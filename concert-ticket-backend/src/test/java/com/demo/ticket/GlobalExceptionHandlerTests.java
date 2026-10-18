package com.demo.ticket;

import com.demo.ticket.Dto.ApiErrorResponse;
import com.demo.ticket.Exception.BookingException;
import com.demo.ticket.Exception.FieldValidationException;
import com.demo.ticket.Exception.GlobalExceptionHandler;
import com.demo.ticket.security.RateLimitException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@NullMarked
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void allHandledErrorsHaveTheSameEnvelopeAndMatchingHttpStatus() {
        List<ResponseEntity<ApiErrorResponse>> responses = List.of(
                handler.handleRateLimit(new RateLimitException(17)),
                handler.handleDataUnavailable(),
                handler.handleBooking(new BookingException("ORDER_NOT_FOUND", "找不到訂單", HttpStatus.NOT_FOUND)),
                handler.handleFieldValidation(new FieldValidationException("phone", "電話格式錯誤")),
                handler.handleDuplicateKey(),
                handler.handleJwtException(),
                handler.handleAccessDenied(),
                handler.handleInvalidOperation(new IllegalArgumentException("操作不合法")));

        for (ResponseEntity<ApiErrorResponse> response : responses) {
            ApiErrorResponse body = responseBody(response);
            assertEquals(response.getStatusCode().value(), body.status());
            assertNotNull(body.code());
            assertNotNull(body.message());
            assertDoesNotThrow(() -> UUID.fromString(body.traceId()));
            assertNotNull(body.timestamp());
            assertEquals(body.message(), body.data().get(0).get("remark"));
            assertInstanceOf(Map.class, body.data().get(1).get("error"));
            assertFalse(body.message().contains("private"));
            assertFalse(body.message().contains("internal"));
        }
    }

    @Test
    void retryHeadersArePreservedForRateLimitAndServiceFailure() {
        assertEquals("17", handler.handleRateLimit(
                new RateLimitException(17)).getHeaders().getFirst("Retry-After")
        );
        assertEquals("5", handler.handleDataUnavailable()
                .getHeaders().getFirst("Retry-After"));
    }

    @Test
    void bookingCodeUsedByTheFrontendIsPreserved() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBooking(new BookingException(
                "PAYMENT_SIMULATION_DISABLED", "模擬付款未開放", HttpStatus.FORBIDDEN));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("PAYMENT_SIMULATION_DISABLED", responseBody(response).code());
    }

    @Test
    void validationPreservesFieldErrorsWithoutReturningRejectedSecrets() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(
                new FieldError(
                        "request",
                        "password",
                        "secret-password",
                        false,
                        new String[0],
                        new Object[0],
                        "密碼格式不正確"
                )
        );
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTests.class.getDeclaredMethod("acceptRequest", Object.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, binding);
        ApiErrorResponse body = responseBody(handler.handleValidationException(exception));
        assertEquals("VALIDATION_ERROR", body.code());
        assertEquals(Map.of("password", "密碼格式不正確"), body.data().get(1).get("error"));
        assertFalse(body.toString().contains("secret-password"));
    }

    @Test
    void operationWithoutAMessageStillHasAValidResponse() {
        assertEquals("操作失敗", responseBody(handler.handleInvalidOperation(new IllegalStateException())).message());
    }

    @Test
    void mvcChoosesDuplicateHandlerAndSerializesTheUnifiedEnvelope() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(handler).build();
        mvc.perform(get("/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_DATA"))
                .andExpect(jsonPath("$.message").value("資料已存在"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.data[0].remark").value("資料已存在"))
                .andExpect(jsonPath("$.data[1].error").isMap());
    }

    @RestController
    static class ErrorController {

        @GetMapping("/duplicate")
        public void duplicate() {
            throw new DuplicateKeyException("private SQL");
        }
    }

    private ApiErrorResponse responseBody(ResponseEntity<ApiErrorResponse> response) {
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        return body;
    }

    // 僅供反射取得參數資訊，建立驗證例外；不會實際呼叫此方法。
    private void acceptRequest(@SuppressWarnings("unused") Object request) {
    }
}
