package com.butent.bee.client.composite;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.InputEvent;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.client.widget.InputColor;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Color;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class ColorEditor extends Flow implements Editor, HasTextBox, HasKeyDownHandlers,
    PreviewHandler {

  private static final String STYLE_PREFIX = "bee-ColorEditor-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_TEXT_BOX = STYLE_PREFIX + "textBox";
  private static final String STYLE_PICKER = STYLE_PREFIX + "picker";

  private final InputText textBox;
  private final InputColor picker;

  private final List<BlurHandler> blurHandlers = Lists.newArrayList();

  private State pickerState = State.CLOSED;

  public ColorEditor() {
    super();
    addStyleName(STYLE_CONTAINER);

    this.textBox = new InputText();
    textBox.addStyleName(STYLE_TEXT_BOX);

    this.picker = new InputColor();
    picker.addStyleName(STYLE_PICKER);

    textBox.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        doBlur(event);
      }
    });

    textBox.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == BeeConst.CHAR_ASTERISK) {
          event.preventDefault();
          event.stopPropagation();

          openPicker();
        }
      }
    });

    picker.addInputHandler(new InputHandler() {
      @Override
      public void onInput(InputEvent event) {
        closePicker();

        textBox.setValue(picker.getValue());
        fireEvent(new EditStopEvent(State.CHANGED));
      }
    });

    picker.addMouseDownHandler(new MouseDownHandler() {
      @Override
      public void onMouseDown(MouseDownEvent event) {
        if (EventUtils.isLeftButton(event.getNativeButton())) {
          setPickerState(State.PENDING);
        }
      }
    });

    picker.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Previewer.ensureRegistered(ColorEditor.this);
      }
    });

    add(textBox);
    add(picker);
  }

  @Override
  public HandlerRegistration addBlurHandler(final BlurHandler handler) {
    Assert.notNull(handler);
    blurHandlers.add(handler);

    return new HandlerRegistration() {
      @Override
      public void removeHandler() {
        blurHandlers.remove(handler);
      }
    };
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addKeyDownHandler(handler);
  }

  @Override
  public HandlerRegistration addEditStopHandler(EditStopEvent.Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return textBox.addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return textBox.addDomHandler(handler, KeyDownEvent.getType());
  }

  @Override
  public void clearValue() {
    setValue(null);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return EditorAction.SELECT;
  }

  @Override
  public String getIdPrefix() {
    return "color-edit";
  }

  @Override
  public String getNormalizedValue() {
    return textBox.getNormalizedValue();
  }

  @Override
  public String getOptions() {
    return textBox.getOptions();
  }

  @Override
  public int getTabIndex() {
    return textBox.getTabIndex();
  }

  @Override
  public TextBox getTextBox() {
    return textBox;
  }

  @Override
  public String getValue() {
    return textBox.getValue();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.COLOR_EDITOR;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return textBox.handlesKey(keyCode);
  }

  @Override
  public boolean handlesTabulation() {
    return textBox.handlesTabulation();
  }

  @Override
  public boolean isEditing() {
    return textBox.isEditing();
  }

  @Override
  public boolean isEnabled() {
    return textBox.isEnabled();
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return textBox.isNullable();
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return textBox.isOrHasPartner(node) || picker.getElement().isOrHasChild(node);
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    picker.setColor(normalizedValue);
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    if (isPickerPending()) {
      String type = event.getNativeEvent().getType();

      if (EventUtils.isKeyEvent(type) || EventUtils.isMouseButtonEvent(type)) {
        boolean isTarget = (targetNode != null) && getElement().isOrHasChild(targetNode);

        closePicker();
        if (!isTarget) {
          DomEvent.fireNativeEvent(Document.get().createBlurEvent(), textBox);
        }
      }
    }
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    textBox.setAccessKey(key);
  }

  @Override
  public void setEditing(boolean editing) {
    textBox.setEditing(editing);
  }

  @Override
  public void setEnabled(boolean enabled) {
    textBox.setEnabled(enabled);
    picker.setEnabled(enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    textBox.setFocus(focused);
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    textBox.setHandlesTabulation(handlesTabulation);
  }

  @Override
  public void setNullable(boolean nullable) {
    textBox.setNullable(nullable);
  }

  @Override
  public void setOptions(String options) {
    textBox.setOptions(options);
  }

  @Override
  public void setTabIndex(int index) {
    textBox.setTabIndex(index);
  }

  @Override
  public void setValue(String value) {
    textBox.setValue(value);
    picker.setColor(value);
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {

    if (charCode == BeeConst.CHAR_ASTERISK) {
      setValue(oldValue);
      openPicker();
    } else {
      picker.setColor(oldValue);
      textBox.startEdit(oldValue, charCode, onEntry, sourceElement);
    }
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return validate(getValue(), checkForNull);
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    List<String> messages = Lists.newArrayList();

    if (BeeUtils.isEmpty(normalizedValue)) {
      if (checkForNull && !isNullable()) {
        messages.add(Localized.getConstants().enterColor());
      }
      return messages;
    }

    if (!Color.validate(normalizedValue)) {
      messages.add(BeeUtils.joinWords(Localized.getConstants().colorIsInvalid(), normalizedValue));
    }
    return messages;
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    Previewer.ensureUnregistered(this);
  }

  private void closePicker() {
    if (!isPickerClosed()) {
      setPickerState(State.CLOSED);
      Previewer.ensureUnregistered(this);
    }
  }

  private void doBlur(BlurEvent event) {
    if (isPickerClosed()) {
      for (BlurHandler handler : blurHandlers) {
        handler.onBlur(event);
      }
    }
  }

  private State getPickerState() {
    return pickerState;
  }

  private boolean isPickerClosed() {
    return State.CLOSED.equals(getPickerState());
  }

  private boolean isPickerPending() {
    return State.PENDING.equals(getPickerState());
  }

  private void openPicker() {
    if (!isPickerPending()) {
      setPickerState(State.PENDING);
      picker.click();
      Previewer.ensureRegistered(this);
    }
  }

  private void setPickerState(State pickerState) {
    this.pickerState = pickerState;
  }
}