package com.butent.bee.client.widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Tags;

import elemental.html.ProgressElement;
import elemental.js.html.JsProgressElement;

/**
 * Implements a progress bar user interface component for gauging actual stage of processes against
 * 100% of the process.
 */

public class Progress extends CustomWidget {

  public Progress() {
    super(DomUtils.createElement(Tags.PROGRESS));
  }

  public Progress(double max) {
    this();
    setMax(max);
  }

  public Progress(double max, double value) {
    this();
    setMax(max);
    setValue(value);
  }

  @Override
  public String getIdPrefix() {
    return "progress";
  }

  public double getMax() {
    return getProgressElement().getMax();
  }

  public double getPosition() {
    return getProgressElement().getPosition();
  }

  public double getValue() {
    return getProgressElement().getValue();
  }

  public void setMax(double max) {
    getProgressElement().setMax(max);
  }

  public void setValue(double value) {
    getProgressElement().setValue(value);
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(BeeConst.CSS_CLASS_PREFIX + "Progress");
  }

  private ProgressElement getProgressElement() {
    return (JsProgressElement) getElement().cast();
  }
}
