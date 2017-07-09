package com.butent.bee.client.widget;

import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.font.FontAwesome;

import java.util.function.Consumer;

public class CustomAction extends FaLabel {
  private static final String STYLE_NAME_RUNNING = "bee-rotate";

  private final FontAwesome idle;
  private FontAwesome running = FontAwesome.SPINNER;
  private Consumer<ResponseObject> callback;

  public CustomAction(FontAwesome fa, ClickHandler action) {
    super(Assert.notNull(fa));
    idle = fa;
    addClickHandler(action);
  }

  @Override
  public void enableAnimation(int duration) {
  }

  public Consumer<ResponseObject> getCallback() {
    return callback;
  }

  public void idle() {
    setChar(idle);
    removeStyleName(STYLE_NAME_RUNNING);
    setEnabled(true);
  }

  public void running() {
    setChar(running);
    addStyleName(STYLE_NAME_RUNNING);
    setEnabled(false);
  }

  public void setCallback(Consumer<ResponseObject> callback) {
    this.callback = callback;
  }
}
