package com.butent.bee.client.widget;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.shared.ui.Orientation;

public class Mover extends CustomDiv implements MoveEvent.HasMoveHandlers {

  private final Orientation orientation;

  private boolean mouseDown;

  private int startX;
  private int startY;

  private int currentX;
  private int currentY;

  public Mover() {
    this(null, null);
  }

  public Mover(String styleName) {
    this(styleName, null);
  }

  public Mover(String styleName, Orientation orientation) {
    super(styleName);
    this.orientation = orientation;
  }

  @Override
  public HandlerRegistration addMoveHandler(MoveEvent.Handler handler) {
    sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE);
    return addHandler(handler, MoveEvent.getType());
  }

  public int getCurrentX() {
    return currentX;
  }

  public int getCurrentY() {
    return currentY;
  }

  @Override
  public String getIdPrefix() {
    return "mover";
  }

  public int getStartX() {
    return startX;
  }

  public int getStartY() {
    return startY;
  }

  @Override
  public void onBrowserEvent(Event event) {
    int x = event.getClientX();
    int y = event.getClientY();

    switch (event.getTypeInt()) {
      case Event.ONMOUSEDOWN:
        setMouseDown(true);

        setStartX(x);
        setStartY(y);

        setCurrentX(x);
        setCurrentY(y);

        Event.setCapture(getElement());

        event.preventDefault();
        event.stopPropagation();
        break;

      case Event.ONMOUSEUP:
        if (isMouseDown()) {
          setMouseDown(false);

          int dx = x - getCurrentX();
          int dy = y - getCurrentY();

          setCurrentX(x);
          setCurrentY(y);

          Event.releaseCapture(getElement());

          event.preventDefault();
          event.stopPropagation();

          MoveEvent.fireFinish(this, dx, dy);
        }
        break;

      case Event.ONMOUSEMOVE:
        if (isMouseDown()) {
          int dx = x - getCurrentX();
          int dy = y - getCurrentY();

          setCurrentX(x);
          setCurrentY(y);

          event.preventDefault();
          event.stopPropagation();

          if (shouldFire(dx, dy)) {
            MoveEvent.fireMove(this, dx, dy);
          }
        }
        break;
    }
  }

  private boolean isMouseDown() {
    return mouseDown;
  }

  private void setCurrentX(int currentX) {
    this.currentX = currentX;
  }

  private void setCurrentY(int currentY) {
    this.currentY = currentY;
  }

  private void setMouseDown(boolean mouseDown) {
    this.mouseDown = mouseDown;
  }

  private void setStartX(int startX) {
    this.startX = startX;
  }

  private void setStartY(int startY) {
    this.startY = startY;
  }

  private boolean shouldFire(int dx, int dy) {
    if (orientation == null) {
      return dx != 0 || dy != 0;
    } else if (Orientation.HORIZONTAL.equals(orientation)) {
      return dx != 0;
    } else {
      return dy != 0;
    }
  }
}
