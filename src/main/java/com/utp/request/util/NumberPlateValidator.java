package com.utp.request.util;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public final class NumberPlateValidator {

  private static final Pattern CAR_PATTERN = Pattern.compile(Constants.PATTERN_NUMBER_PLATE_CAR);
  private static final Pattern MOTORCYCLE_PATTERN = Pattern.compile(Constants.PATTERN_NUMBER_PLATE_MOTORCYCLE);

  public static boolean isMotorcycle(Integer idVehicleType) {
    return Constants.ID_VEHICLE_TYPES_MOTORCYCLE.contains(idVehicleType);
  }

  public static boolean matchesCarPlate(String numberPlate) {
    return matches(CAR_PATTERN, numberPlate);
  }

  public static boolean matchesMotorcyclePlate(String numberPlate) {
    return matches(MOTORCYCLE_PATTERN, numberPlate);
  }

  private static boolean matches(Pattern pattern, String numberPlate) {
    if (numberPlate == null) {
      return false;
    }
    return pattern.matcher(numberPlate.trim().toUpperCase(Locale.ROOT)).matches();
  }
}
