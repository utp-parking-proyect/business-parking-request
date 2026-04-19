package com.utp.request.util.error;

import com.utp.request.generated.model.ModelApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorResponseHandlerTest {

  @InjectMocks
  private ErrorResponseHandler errorResponseHandler;

  @Mock
  private MissingRequestValueException mockException;

  @Test
  void testHandleValidationError() {
    // Arrange
    IllegalArgumentException exception = new IllegalArgumentException("Invalid data");

    // Act & Assert
    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleValidationError(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert response.getBody() != null;
          assert response.getBody().getDescription().equals("Invalid data");
          assert response.getBody().getErrorType().equals("FUNCTIONAL");
        })
        .verifyComplete();
  }

  @Test
  void testHandleUnexpectedError() {
    // Arrange
    Exception exception = new Exception("Unexpected error occurred");

    // Act & Assert
    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleUnexpectedError(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
          assert response.getBody() != null;
          assert response.getBody().getDescription().equals("Ocurrió un error inesperado");
          assert response.getBody().getErrorType().equals("TECHNICAL");
        })
        .verifyComplete();
  }

  @Test
  void testHandleMissingRequestValue() {
    // Arrange
    when(mockException.getMessage()).thenReturn("Required header 'caller-name' is not present.");

    // Act & Assert
    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleMissingRequestValue(mockException);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert response.getBody() != null;
          assert response.getBody().getErrorType().equals("FUNCTIONAL");
        })
        .verifyComplete();
  }
}
