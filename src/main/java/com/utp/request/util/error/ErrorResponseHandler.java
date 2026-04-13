package com.utp.request.util.error;

import com.utp.request.generated.model.ParkingRequestResponse;
import com.utp.request.generated.model.ModelApiException;
import com.utp.request.generated.model.ApiExceptionDetail;
import com.utp.request.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.HashMap;

@Component
@RestControllerAdvice
@Slf4j
public class ErrorResponseHandler {

  private static final String ERROR_TYPE_FUNCTIONAL = "FUNCTIONAL";
  private static final String ERROR_TYPE_TECHNICAL = "TECHNICAL";

  public Mono<ResponseEntity<ParkingRequestResponse>> buildValidationErrorResponse(IllegalArgumentException e) {
    log.error("Validation error: {}", e.getMessage());
    ModelApiException errorResponse = new ModelApiException();
    errorResponse.description(HttpStatus.BAD_REQUEST.name());
    errorResponse.errorType(ERROR_TYPE_FUNCTIONAL);

    buildApiExceptionDetail(e, errorResponse);

    @SuppressWarnings("unchecked")
    ResponseEntity<ParkingRequestResponse> response = (ResponseEntity<ParkingRequestResponse>)
        (ResponseEntity<?>) ResponseEntity.badRequest().body(errorResponse);
    return Mono.just(response);
  }

  public Mono<ResponseEntity<ParkingRequestResponse>> buildUnexpectedErrorResponse(Exception e) {
    log.error("Unexpected error: {}", e.getMessage());
    ModelApiException errorResponse = new ModelApiException();
    errorResponse.description("Ocurrió un error inesperado");
    errorResponse.errorType(ERROR_TYPE_TECHNICAL);

    buildApiExceptionDetail(e, errorResponse);

    @SuppressWarnings("unchecked")
    ResponseEntity<ParkingRequestResponse> response = (ResponseEntity<ParkingRequestResponse>)
        (ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    return Mono.just(response);
  }

  @ExceptionHandler(MissingRequestValueException.class)
  public Mono<ResponseEntity<ModelApiException>> handleMissingRequestValue(MissingRequestValueException e) {
    log.error("Missing request value: {}", e.getMessage());

    String headerName = extractHeaderName(e.getMessage());

    ModelApiException errorResponse = new ModelApiException();
    errorResponse.description("Header requerido faltante: " + headerName);
    errorResponse.errorType(ERROR_TYPE_FUNCTIONAL);

    ArrayList<ApiExceptionDetail> details = new ArrayList<>();
    ApiExceptionDetail detail = new ApiExceptionDetail();
    detail.component(Constants.NAME_MICROSERVICE);
    detail.description("El header '" + headerName + "' es requerido pero no fue proporcionado");
    details.add(detail);

    errorResponse.setExceptionDetails(details);
    errorResponse.setProperties(new HashMap<>());

    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
  }

  private void buildApiExceptionDetail(Exception e, ModelApiException errorResponse) {
    ArrayList<ApiExceptionDetail> details = new ArrayList<>();
    ApiExceptionDetail detail = new ApiExceptionDetail();
    detail.component(Constants.NAME_MICROSERVICE);
    detail.description(e.getMessage());
    details.add(detail);

    errorResponse.setExceptionDetails(details);
    errorResponse.setProperties(new HashMap<>());
  }

  private String extractHeaderName(String message) {
    int startIndex = message.indexOf("'") + 1;
    int endIndex = message.lastIndexOf("'");
    if (startIndex > 0 && endIndex > startIndex) {
      return message.substring(startIndex, endIndex);
    }
    return "Unknown header";
  }
}
