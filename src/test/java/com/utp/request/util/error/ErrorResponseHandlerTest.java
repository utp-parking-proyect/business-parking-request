package com.utp.request.util.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ErrorResponseHandlerTest {

  @InjectMocks
  private ErrorResponseHandler errorResponseHandler;

  @Test
  void testBuildValidationErrorResponse() {
    // Arrange
    IllegalArgumentException exception = new IllegalArgumentException("Invalid data");

    // Act & Assert
    StepVerifier.create(errorResponseHandler.buildValidationErrorResponse(exception))
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert response.getBody() != null;
        })
        .verifyComplete();
  }

  @Test
  void testBuildUnexpectedErrorResponse() {
    // Arrange
    Exception exception = new Exception("Unexpected error occurred");

    // Act & Assert
    StepVerifier.create(errorResponseHandler.buildUnexpectedErrorResponse(exception))
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
          assert response.getBody() != null;
        })
        .verifyComplete();
  }
}
