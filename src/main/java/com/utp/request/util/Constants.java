package com.utp.request.util;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public final class Constants {

  public static final Integer ID_VEHICLE_TYPE_CAR = 1;
  public static final Integer ID_VEHICLE_TYPE_MOTORCYCLE = 2;
  public static final Integer ID_VEHICLE_TYPE_PICKUP = 3;

  public static final Set<Integer> ID_VEHICLE_TYPES_MOTORCYCLE = Set.of(ID_VEHICLE_TYPE_MOTORCYCLE);

  public static final String PATTERN_NUMBER_PLATE_CAR = "^[A-Z0-9]{3}-[A-Z0-9]{3}$";
  public static final String PATTERN_NUMBER_PLATE_MOTORCYCLE = "^[A-Z0-9]{2}-[A-Z0-9]{4}$";

  public static final Integer ID_STATUS_REGISTERED = 1;
  public static final Integer ID_STATUS_IN_REVISION = 2;
  public static final Integer ID_STATUS_APPROVED = 3;
  public static final Integer ID_STATUS_REJECTED = 4;
  public static final Integer ID_STATUS_RESUBMITTED = 5;

  public static final Integer ID_VEHICLE_STATUS_ASSIGNED = 1;
  public static final Integer ID_VEHICLE_STATUS_UNASSIGNED = 2;

  public static final Integer MAX_ASSIGNED_VEHICLES_PER_USER = 5;
  public static final Integer MAX_REQUESTS_PER_CYCLE = 2;

  public static final String ROLE_NAME_SAE = "ROLE_SAE";
  public static final String ROLE_NAME_SECURITY = "ROLE_SECURITY";

  public static final String NAME_MICROSERVICE = "business-parking-request";

  public static final String OBSERVATION_REGISTERED = "Solicitud registrada.";
  public static final String OBSERVATION_IN_REVISION = "Solicitud asignada a Personal SAE para revisión.";
  public static final String OBSERVATION_RESUBMITTED_DEFAULT = "Solicitud reenviada para una nueva revisión.";

  public static final String ERROR_VEHICLE_OWNED_BY_ANOTHER_USER =
      "El vehículo con esta placa pertenece a otro usuario";
  public static final String ERROR_VEHICLE_TYPE_REQUIRED =
      "El tipo de vehículo es requerido para registrar una placa nueva";
  public static final String ERROR_INVALID_NUMBER_PLATE_CAR =
      "La placa de un automóvil o camioneta debe tener el formato ABC-123 (3-3 caracteres)";
  public static final String ERROR_INVALID_NUMBER_PLATE_MOTORCYCLE =
      "La placa de una motocicleta debe tener el formato AB-1234 (2-4 caracteres)";
  public static final String ERROR_MAX_ASSIGNED_VEHICLES_REACHED =
      "Has alcanzado el máximo de " + MAX_ASSIGNED_VEHICLES_PER_USER + " vehículos asignados";
  public static final String ERROR_MAX_REQUESTS_PER_CYCLE_REACHED =
      "Has alcanzado el máximo de " + MAX_REQUESTS_PER_CYCLE + " solicitudes para este ciclo";
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
  public static final String ERROR_NOT_ACCEPTOR =
      "El usuario autenticado no es el aceptante consultado";
  public static final String ERROR_NOT_APPLICANT =
      "El usuario autenticado no es el solicitante consultado";
  public static final String ERROR_REQUEST_NOT_OWNED =
      "El usuario autenticado no es el solicitante ni el aceptante de esta solicitud";
  public static final String ERROR_MISSING_USER_ID =
      "El token no contiene el identificador del usuario autenticado";
  public static final String ERROR_ACCEPTOR_NOT_SAE = "El aceptante no es Personal SAE";
  public static final String ERROR_NO_ACCEPTOR_AVAILABLE = "No hay Personal SAE disponible para ser asignado";
  public static final String ERROR_PLATE_REGISTERED_UNASSIGNED =
      "Esta placa ya está registrada y fue desasignada de su propietario anterior. "
          + "Comunícate con Personal SAE para reasignarla";
  public static final String ERROR_VEHICLE_ALREADY_UNASSIGNED =
      "El vehículo ya no está asignado a ningún usuario";
  public static final String ERROR_USERS_SERVICE_UNAVAILABLE = "business-core-portal no se encuentra disponible";
}
