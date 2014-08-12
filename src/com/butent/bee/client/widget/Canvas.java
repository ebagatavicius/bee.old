package com.butent.bee.client.widget;

import com.google.gwt.canvas.dom.client.Context;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.FocusWidget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.IdentifiableWidget;

public class Canvas extends FocusWidget implements IdentifiableWidget {

  public Canvas() {
    this(Document.get().createCanvasElement());
  }

  public Canvas(Element elem) {
    super(elem);
    init();
  }

  public CanvasElement getCanvasElement() {
    return this.getElement().cast();
  }

  public Context getContext(String contextId) {
    return getCanvasElement().getContext(contextId);
  }

  public Context2d getContext2d() {
    return getCanvasElement().getContext2d();
  }

  public int getCoordinateSpaceHeight() {
    return getCanvasElement().getHeight();
  }

  public int getCoordinateSpaceWidth() {
    return getCanvasElement().getWidth();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "canvas";
  }

  public void setCoordinateSpaceHeight(int height) {
    getCanvasElement().setHeight(height);
  }

  public void setCoordinateSpaceWidth(int width) {
    getCanvasElement().setWidth(width);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public String toDataUrl() {
    return getCanvasElement().toDataUrl();
  }

  public String toDataUrl(String type) {
    return getCanvasElement().toDataUrl(type);
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
  }
}
