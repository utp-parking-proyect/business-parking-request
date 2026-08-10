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
      "El vehículo con esta placa pertenece a otro usuario";
  public static final String ERROR_REQUEST_ALREADY_EXISTS =
      "Ya existe una solicitud para este vehículo en el ciclo actual";
  public static final String ERROR_REQUEST_REJECTED_USE_RESUBMIT =
      "Esta solicitud fue rechazada, utilice el endpoint de reenvío en su lugar";
  public static final String ERROR_REQUEST_NOT_FOUND = "La solicitud no existe";
  public static final String ERROR_REQUEST_NOT_REJECTED =
      "La solicitud no se encuentra en estado rechazado y no puede ser reenviada";
  public static final String ERROR_REQUEST_WRONG_CYCLE =
      "La solicitud no pertenece al ciclo actual";
  public static final String ERROR_ACCEPTOR_NOT_FOUND = "El aceptante no existe";
  public static final String ERROR_APPLICANT_NOT_FOUND = "El solicitante no existe";
  public static final String ERROR_ACCEPTOR_NOT_SAE = "El aceptante no es Personal SAE";
  public static final String ERROR_NO_ACCEPTOR_AVAILABLE = "No hay Personal SAE disponible para ser asignado";
  public static final String ERROR_USERS_SERVICE_UNAVAILABLE = "business-core-portal no se encuentra disponible";
}
