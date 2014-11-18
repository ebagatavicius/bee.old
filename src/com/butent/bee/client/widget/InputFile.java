package com.butent.bee.client.widget;

import com.google.common.net.MediaType;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasName;

import elemental.html.FileList;
import elemental.html.InputElement;
import elemental.js.html.JsInputElement;

public class InputFile extends Widget implements HasName, HasChangeHandlers, EnablableWidget,
    IdentifiableWidget {

  public InputFile() {
    super();
    setElement(Document.get().createFileInputElement());
    init();
  }

  public InputFile(boolean multiple) {
    this();
    if (multiple) {
      getInputElement().setMultiple(true);
    }
  }

  @Override
  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    return addDomHandler(handler, ChangeEvent.getType());
  }

  public void clear() {
    if (!isEmpty()) {
      getInputElement().setValue(BeeConst.STRING_EMPTY);
    }
  }

  public void click() {
    getInputElement().click();
  }

  public FileList getFiles() {
    return getInputElement().getFiles();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "file";
  }

  @Override
  public String getName() {
    return getInputElement().getName();
  }

  public boolean isEmpty() {
    return getFiles().length() <= 0;
  }

  @Override
  public boolean isEnabled() {
    return !getInputElement().isDisabled();
  }

  public void setAccept(MediaType mediaType) {
    if (mediaType != null) {
      setAccept(mediaType.toString());
    }
  }

  public void setAccept(String accept) {
    getInputElement().setAccept(accept);
  }

  @Override
  public void setEnabled(boolean enabled) {
    getInputElement().setDisabled(!enabled);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setName(String name) {
    getInputElement().setName(name);
  }

  protected void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "InputFile");
  }

  private InputElement getInputElement() {
    return (JsInputElement) getElement().cast();
  }
}
