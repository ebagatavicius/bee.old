package com.butent.bee.shared.report;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.utils.BeeUtils;

public abstract class ResultCalculator<T> {

  private T result;

  public abstract ResultCalculator calculate(ReportFunction fnc, T val);

  public T getResult() {
    return result;
  }

  public String getString() {
    return result.toString();
  }

  public void setResult(T result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return BeeUtils.padLeft(getString(), 15, BeeConst.CHAR_SPACE);
  }
}
