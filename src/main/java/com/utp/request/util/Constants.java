package com.utp.request.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Constants {

  public static final Integer ID_STATUS_REGISTERED = 1;
  public static final Integer ID_STATUS_IN_REVISION = 2;
  public static final Integer ID_STATUS_APPROVED = 3;
  public static final Integer ID_STATUS_REJECTED = 4;

  public static final String ROLE_NAME_SAE = "ROLE_SAE";

  public static final String NAME_MICROSERVICE = "business-parking-request";

  public static final String OBSERVATION_REGISTERED = "Solicitud registrada.";
  public static final String OBSERVATION_IN_REVISION = "Solicitud asignada a Personal SAE para revisión.";
  public static final String OBSERVATION_RESUBMITTED_DEFAULT = "Solicitud reenviada para una nueva revisión.";

  public static final String ERROR_VEHICLE_OWNED_BY_ANOTHER_USER =
      "The vehicle with this number plate belongs to another user";
  public static final String ERROR_REQUEST_ALREADY_EXISTS =
      "A request already exists for this vehicle in the current cycle";
  public static final String ERROR_REQUEST_REJECTED_USE_RESUBMIT =
      "This request was rejected, use the resubmit endpoint instead";
  public static final String ERROR_REQUEST_NOT_FOUND = "Request not found";
  public static final String ERROR_REQUEST_NOT_REJECTED =
      "The request is not in a rejected state and cannot be resubmitted";
  public static final String ERROR_REQUEST_WRONG_CYCLE =
      "The request does not belong to the current cycle";
  public static final String ERROR_ACCEPTOR_NOT_FOUND = "Acceptor not found";
  public static final String ERROR_APPLICANT_NOT_FOUND = "Applicant not found";
  public static final String ERROR_ACCEPTOR_NOT_SAE = "The acceptor is not Personal SAE";
  public static final String ERROR_NO_ACCEPTOR_AVAILABLE = "No Personal SAE is available to be assigned";
  public static final String ERROR_USERS_SERVICE_UNAVAILABLE = "business-core-portal is not available";
}
