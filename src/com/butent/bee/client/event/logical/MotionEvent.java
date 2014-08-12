package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;

public class MotionEvent extends Event<MotionEvent.Handler> {

  public interface Handler extends EventHandler {
    void onMotion(MotionEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static HandlerRegistration register(Handler handler) {
    return BeeKeeper.getBus().addHandler(TYPE, handler, false);
  }

  private final String dataType;
  private final String sourceId;

  private final int sourceLeft;
  private final int sourceTop;

  private final int sourceWidth;
  private final int sourceHeight;

  private final int startX;
  private final int startY;

  private int currentX;
  private int currentY;

  private Direction directionX;
  private Direction directionY;

  private int changePositionX;
  private int changePositionY;

  private long changeMillisX;
  private long changeMillisY;

  public MotionEvent(String dataType, IdentifiableWidget widget, int startX, int startY) {
    super();

    Assert.notNull(widget);
    Assert.isTrue(widget.asWidget().isAttached(), "MotionEvent: widget must be attached");

    this.dataType = dataType;
    this.sourceId = widget.getId();

    this.sourceLeft = widget.asWidget().getAbsoluteLeft();
    this.sourceTop = widget.asWidget().getAbsoluteTop();

    this.sourceWidth = widget.asWidget().getOffsetWidth();
    this.sourceHeight = widget.asWidget().getOffsetHeight();

    this.startX = startX;
    this.startY = startY;

    this.currentX = startX;
    this.currentY = startX;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public int getBottom() {
    return getTop() + sourceHeight;
  }

  public long getChangeMillisX() {
    return changeMillisX;
  }

  public long getChangeMillisY() {
    return changeMillisY;
  }

  public int getChangePositionX() {
    return changePositionX;
  }

  public int getChangePositionY() {
    return changePositionY;
  }

  public int getCurrentX() {
    return currentX;
  }

  public int getCurrentY() {
    return currentY;
  }

  public String getDataType() {
    return dataType;
  }

  public Direction getDirectionX() {
    return directionX;
  }

  public Direction getDirectionY() {
    return directionY;
  }

  public int getLeft() {
    return sourceLeft - startX + currentX;
  }

  public int getRight() {
    return getLeft() + sourceWidth;
  }

  public int getSourceHeight() {
    return sourceHeight;
  }

  public String getSourceId() {
    return sourceId;
  }

  public int getSourceLeft() {
    return sourceLeft;
  }

  public int getSourceTop() {
    return sourceTop;
  }

  public int getSourceWidth() {
    return sourceWidth;
  }

  public int getStartX() {
    return startX;
  }

  public int getStartY() {
    return startY;
  }

  public int getTop() {
    return sourceTop - startY + currentY;
  }

  public double getVelocityX() {
    if (getDirectionX() != null && getChangeMillisX() > 0) {
      double diff = Math.abs(getCurrentX() - getChangePositionX());
      return diff / Math.max(System.currentTimeMillis() - getChangeMillisX(), 1);
    } else {
      return BeeConst.DOUBLE_ZERO;
    }
  }

  public double getVelocityY() {
    if (getDirectionY() != null && getChangeMillisY() > 0) {
      double diff = Math.abs(getCurrentY() - getChangePositionY());
      return diff / Math.max(System.currentTimeMillis() - getChangeMillisY(), 1);
    } else {
      return BeeConst.DOUBLE_ZERO;
    }
  }

  public void moveTo(int x, int y) {
    if (x != getCurrentX()) {
      Direction direction = (x < getCurrentX()) ? Direction.WEST : Direction.EAST;

      if (direction != getDirectionX()) {
        setDirectionX(direction);
        setChangePositionX(x);
        setChangeMillisX(System.currentTimeMillis());
      }

      setCurrentX(x);
    }

    if (y != getCurrentY()) {
      Direction direction = (y < getCurrentY()) ? Direction.NORTH : Direction.SOUTH;

      if (direction != getDirectionY()) {
        setDirectionY(direction);
        setChangePositionY(y);
        setChangeMillisY(System.currentTimeMillis());
      }

      setCurrentY(y);
    }
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMotion(this);
  }

  private void setChangeMillisX(long changeMillisX) {
    this.changeMillisX = changeMillisX;
  }

  private void setChangeMillisY(long changeMillisY) {
    this.changeMillisY = changeMillisY;
  }

  private void setChangePositionX(int changePositionX) {
    this.changePositionX = changePositionX;
  }

  private void setChangePositionY(int changePositionY) {
    this.changePositionY = changePositionY;
  }

  private void setCurrentX(int currentX) {
    this.currentX = currentX;
  }

  private void setCurrentY(int currentY) {
    this.currentY = currentY;
  }

  private void setDirectionX(Direction directionX) {
    this.directionX = directionX;
  }

  private void setDirectionY(Direction directionY) {
    this.directionY = directionY;
  }
}
