package com.utp.request.util;

import java.time.LocalDate;

public final class CycleUtil {

  private CycleUtil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static String determineCycle() {
    LocalDate now = LocalDate.now();
    int year = now.getYear();
    int month = now.getMonthValue();
    int day = now.getDayOfMonth();

    if (month == 1 && day <= 20) {
      return year + Constants.ID_CYCLE_0;
    }
    if (month == 3 && day <= 20) {
      return year + Constants.ID_CYCLE_1;
    }
    if (month == 6 && day <= 20) {
      return year + Constants.ID_CYCLE_2;
    }
    throw new IllegalArgumentException("Date is not within the valid range for any cycle");
  }
}
