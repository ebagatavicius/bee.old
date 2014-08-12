package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.HasHtml;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Implements a standard check box user interface component.
 */

public class CheckBox extends FocusWidget implements BooleanWidget, HasHtml {

  private final InputElement inputElem;
  private final LabelElement labelElem;

  private boolean valueChangeHandlerInitialized;

  public CheckBox() {
    this(Document.get().createCheckInputElement());
  }

  public CheckBox(Element elem) {
    super(Document.get().createSpanElement());

    this.inputElem = InputElement.as(elem);
    this.labelElem = Document.get().createLabelElement();

    getElement().appendChild(inputElem);
    getElement().appendChild(labelElem);

    String id = DomUtils.createUniqueId("cbi");
    inputElem.setId(id);
    labelElem.setHtmlFor(id);

    init();
  }

  public CheckBox(String label) {
    this();
    setHtml(label);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
    if (!valueChangeHandlerInitialized) {
      ensureDomEventHandlers();
      valueChangeHandlerInitialized = true;
    }
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public String getFormValue() {
    return inputElem.getValue();
  }

  @Override
  public String getHtml() {
    return labelElem.getInnerHTML();
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "cb";
  }

  @Override
  public int getTabIndex() {
    return inputElem.getTabIndex();
  }

  @Override
  public String getText() {
    return labelElem.getInnerText();
  }

  @Override
  public Boolean getValue() {
    if (isAttached()) {
      return inputElem.isChecked();
    } else {
      return inputElem.isDefaultChecked();
    }
  }

  @Override
  public boolean isChecked() {
    return getValue();
  }

  @Override
  public boolean isEnabled() {
    return !inputElem.isDisabled();
  }

  @Override
  public void setAccessKey(char key) {
    inputElem.setAccessKey(String.valueOf(key));
  }

  @Override
  public void setChecked(boolean checked) {
    setValue(checked);
  }

  @Override
  public void setEnabled(boolean enabled) {
    inputElem.setDisabled(!enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      inputElem.focus();
    } else {
      inputElem.blur();
    }
  }

  public void setFormValue(String value) {
    inputElem.setValue(value);
  }

  @Override
  public void setHtml(String html) {
    labelElem.setInnerHTML(html);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setTabIndex(int index) {
    if (inputElem != null) {
      inputElem.setTabIndex(index);
    }
  }

  @Override
  public void setText(String text) {
    labelElem.setInnerText(text);
  }

  @Override
  public void setValue(Boolean value) {
    setValue(value, false);
  }

  @Override
  public void setValue(Boolean value, boolean fireEvents) {
    boolean b = BeeUtils.unbox(value);
    boolean oldValue = BeeUtils.unbox(getValue());

    inputElem.setChecked(b);
    inputElem.setDefaultChecked(b);

    if (fireEvents && (b != oldValue)) {
      ValueChangeEvent.fire(this, b);
    }
  }

  @Override
  public void sinkEvents(int eventBitsToAdd) {
    if (isOrWasAttached()) {
      Event.sinkEvents(inputElem, eventBitsToAdd | Event.getEventsSunk(inputElem));
    } else {
      super.sinkEvents(eventBitsToAdd);
    }
  }

  protected void ensureDomEventHandlers() {
    addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ValueChangeEvent.fire(CheckBox.this, getValue());
      }
    });
  }

  protected String getDefaultStyleName() {
    return "bee-CheckBox";
  }

  protected InputElement getInputElem() {
    return inputElem;
  }

  protected LabelElement getLabelElem() {
    return labelElem;
  }

  @Override
  protected void onLoad() {
    DOM.setEventListener(inputElem, this);
  }

  @Override
  protected void onUnload() {
    DOM.setEventListener(inputElem, null);
  }

  private void init() {
    DomUtils.createId(this, getIdPrefix());
    addStyleName(getDefaultStyleName());
  }
}
