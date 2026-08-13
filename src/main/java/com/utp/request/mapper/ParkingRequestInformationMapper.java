package com.utp.request.mapper;

import com.utp.request.generated.client.users.model.CycleResponse;
import com.utp.request.generated.client.users.model.UserResponse;
import com.utp.request.generated.model.ApplicantInformation;
import com.utp.request.generated.model.ParkingRequestInformation;
import com.utp.request.generated.model.VehicleInformation;
import com.utp.request.generated.model.WorkflowEntry;
import com.utp.request.model.entity.Request;
import com.utp.request.model.entity.Status;
import com.utp.request.model.entity.Vehicle;
import com.utp.request.model.entity.VehicleType;
import com.utp.request.model.entity.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParkingRequestInformationMapper {

  @Mapping(target = "numberPlate", source = "vehicle.numberPlate")
  @Mapping(target = "vehicleType", source = "vehicleType.nameVehicleType")
  VehicleInformation toVehicleInformation(Vehicle vehicle, VehicleType vehicleType);

  @Mapping(target = "idApplicant", source = "applicant.idUser")
  @Mapping(target = "nameApplicant", source = "applicant.name")
  @Mapping(target = "lastNameApplicant", source = "applicant.lastname")
  @Mapping(target = "usernameApplicant", source = "applicant.username")
  @Mapping(target = "numberCycle", source = "cycle.nameCycle")
  ApplicantInformation toApplicantInformation(UserResponse applicant, CycleResponse cycle);

  @Mapping(target = "status", source = "status.nameStatus")
  @Mapping(target = "dateStatusChange", source = "workflow.dateStatusChange")
  @Mapping(target = "observation", source = "workflow.observation")
  WorkflowEntry toWorkflowEntry(Workflow workflow, Status status);

  @Mapping(target = "idRequest", source = "request.idRequest")
  @Mapping(target = "applicant", source = "applicantInformation")
  @Mapping(target = "vehicle", source = "vehicleInformation")
  @Mapping(target = "dateRequest", source = "request.dateRequest")
  @Mapping(target = "dateResponse", source = "request.dateResponse")
  @Mapping(target = "status", source = "status.nameStatus")
  ParkingRequestInformation toParkingRequestInformation(Request request, Status status,
      ApplicantInformation applicantInformation, VehicleInformation vehicleInformation);
}
