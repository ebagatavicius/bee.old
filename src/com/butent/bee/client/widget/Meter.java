package com.butent.bee.client.widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.Tags;

/**
 * Implements a meter user interface component for gauging actual values against the target ones.
 */

public class Meter extends CustomWidget {

  public Meter() {
    super(DomUtils.createElement(Tags.METER));
  }

  public Meter(double min, double max, double value) {
    this();
    setMin(min);
    setMax(max);
    setValue(value);
  }

  public Meter(double min, double max, double value, double low, double high, double optimum) {
    this();
    setMin(min);
    setMax(max);
    setValue(value);
    setLow(low);
    setHigh(high);
    setOptimum(optimum);
  }

  public double getHigh() {
    return getElement().getPropertyDouble(Attributes.HIGH);
  }

  @Override
  public String getIdPrefix() {
    return "meter";
  }

  public double getLow() {
    return getElement().getPropertyDouble(Attributes.LOW);
  }

  public double getMax() {
    return getElement().getPropertyDouble(Attributes.MAX);
  }

  public double getMin() {
    return getElement().getPropertyDouble(Attributes.MIN);
  }

  public double getOptimum() {
    return getElement().getPropertyDouble(Attributes.OPTIMUM);
  }

  public double getValue() {
    return getElement().getPropertyDouble(Attributes.VALUE);
  }

  public void setHigh(double high) {
    getElement().setPropertyDouble(Attributes.HIGH, high);
  }

  public void setLow(double low) {
    getElement().setPropertyDouble(Attributes.LOW, low);
  }

  public void setMax(double max) {
    getElement().setPropertyDouble(Attributes.MAX, max);
  }

  public void setMin(double min) {
    getElement().setPropertyDouble(Attributes.MIN, min);
  }

  public void setOptimum(double optimum) {
    getElement().setPropertyDouble(Attributes.OPTIMUM, optimum);
  }

  public void setValue(double value) {
    getElement().setPropertyDouble(Attributes.VALUE, value);
  }

  @Override
  protected void init() {
    super.init();
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "Meter");
  }
}
