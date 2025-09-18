package com.utp.request.util;

public final class Constants {

  private Constants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static final Integer ID_STATUS_REGISTERED = 1;
  public static final Integer ID_STATUS_IN_REVISION = 2;
  public static final Integer ID_STATUS_NOT_APPROVED = 0;

  public static final String ID_CYCLE_0 = "-0";
  public static final String ID_CYCLE_1 = "-1";
  public static final String ID_CYCLE_2 = "-2";

  public static final String PLATE_VALID = "VALID";
  public static final String ERROR_NO_RESPONSE_YET = "The number plate doesn't have a response yet";
  public static final String ERROR_ALREADY_APPROVED = "The number plate has already been approved";
  public static final String ERROR_MAX_REQUESTS_REACHED = "This user has already made 2 requests for this cycle";
}
