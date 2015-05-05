package com.butent.bee.client.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.HasInputHandlers;
import com.butent.bee.client.event.InputEvent;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.utils.BeeUtils;

import elemental.client.Browser;
import elemental.html.InputElement;

public class InputColor extends Widget implements EnablableWidget, IdentifiableWidget,
    HasInputHandlers, HasMouseDownHandlers, HasClickHandlers, HasValueChangeHandlers<String> {

  public InputColor() {
    super();

    InputElement inputElement = Browser.getDocument().createInputElement();
    if (Features.supportsInputColor()) {
      inputElement.setType("color");
    }

    setElement((Element) inputElement);
    init();
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return Binder.addClickHandler(this, handler);
  }

  public HandlerRegistration addColorChangeHandler(final Scheduler.ScheduledCommand command) {
    Assert.notNull(command);

    if (Features.supportsInputColor()) {
      return addInputHandler(new InputHandler() {
        @Override
        public void onInput(InputEvent event) {
          command.execute();
        }
      });

    } else {
      return addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          command.execute();
        }
      });
    }
  }

  @Override
  public HandlerRegistration addInputHandler(InputHandler handler) {
    return Binder.addInputHandler(this, handler);
  }

  @Override
  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return Binder.addMouseDownHandler(this, handler);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public void click() {
    if (Features.supportsInputColor()) {
      getInputElement().click();
    } else {
      Global.inputString(Localized.getConstants().color(), null, new StringCallback(false) {
        @Override
        public String getMessage(String value) {
          return validate(value) ? null : Localized.getConstants().colorIsInvalid();
        }

        @Override
        public void onSuccess(String value) {
          String newValue = Color.normalize(value);
          if (!BeeUtils.equalsTrim(newValue, getValue())) {
            setValue(newValue);
            ValueChangeEvent.fire(InputColor.this, newValue);
          }
        }

        @Override
        public boolean validate(String value) {
          return BeeUtils.isEmpty(value) || Color.validate(value);
        }
      }, null, getValue(), 20);
    }
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "color-pick";
  }

  public String getValue() {
    return getInputElement().getValue();
  }

  @Override
  public boolean isEnabled() {
    return !getInputElement().isDisabled();
  }

  public void setColor(String color) {
    setValue(Color.normalize(color));
  }

  @Override
  public void setEnabled(boolean enabled) {
    getInputElement().setDisabled(!enabled);
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setValue(String value) {
    getInputElement().setValue(BeeUtils.trim(value));
  }

  protected void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName(BeeConst.CSS_CLASS_PREFIX + "InputColor");
  }

  private InputElement getInputElement() {
    return (InputElement) getElement();
  }
}
