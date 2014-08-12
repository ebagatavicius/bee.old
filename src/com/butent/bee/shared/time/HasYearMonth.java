package com.butent.bee.shared.time;

public interface HasYearMonth {

  JustDate getDate();

  int getMonth();

  int getYear();

  void setMonth(int month);

  void setYear(int year);
}
