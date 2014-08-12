package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditStopEvent.Handler;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasCheckedness;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import elemental.js.dom.JsElement;

public class Toggle extends CustomWidget implements Editor, HasValueChangeHandlers<String>,
    HasCheckedness {

  private static final String STYLE_SUFFIX_CHECKED = "checked";
  private static final String STYLE_SUFFIX_UNCHECKED = "unchecked";

  private static final String BALLOT = "\u2717";
  private static final String HEAVY_CHECK_MARK = "\u2714";

  private final String upFace;
  private final String downFace;

  private boolean checked;

  private boolean enabled = true;
  private boolean nullable = true;

  private boolean editing;

  private String options;

  private boolean handlesTabulation;

  public Toggle() {
    this(BALLOT, HEAVY_CHECK_MARK);
  }

  public Toggle(String upFace, String downFace) {
    this(upFace, downFace, null, false);
  }

  public Toggle(String upFace, String downFace, String styleName, boolean checked) {
    this(Document.get().createDivElement(), upFace, downFace, styleName, checked);
  }

  public Toggle(Element element, String upFace, String downFace, String styleName,
      boolean checked) {

    super(element, BeeUtils.notEmpty(styleName, "bee-Toggle"));
    addStyleDependentName(STYLE_SUFFIX_UNCHECKED);

    this.upFace = upFace;
    this.downFace = downFace;

    this.checked = checked;

    getElement().setInnerHTML(checked ? downFace : upFace);
    sinkEvents(Event.ONCLICK);
  }

  public Toggle(FontAwesome up, FontAwesome down, String styleName, boolean checked) {
    this(Document.get().createDivElement(), up, down, styleName, checked);
  }

  public Toggle(Element element, FontAwesome up, FontAwesome down, String styleName,
      boolean checked) {
    this(element, String.valueOf(up.getCode()), String.valueOf(down.getCode()), styleName, checked);
    StyleUtils.setFontFamily(this, FontAwesome.FAMILY);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addValueChangeHandler(handler);
  }

  @Override
  public HandlerRegistration addEditStopHandler(Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    setValue(null);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "toggle";
  }

  @Override
  public String getNormalizedValue() {
    Boolean v = isChecked();
    if (!v && isNullable()) {
      v = null;
    }
    return BooleanValue.pack(v);
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public int getTabIndex() {
    return getElement().getTabIndex();
  }

  @Override
  public String getValue() {
    return BooleanValue.pack(isChecked());
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.TOGGLE;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return false;
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  public void invert() {
    setChecked(!isChecked());
  }

  @Override
  public boolean isChecked() {
    return checked;
  }

  @Override
  public boolean isEditing() {
    return editing;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && getElement().isOrHasChild(node);
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (!isEnabled()) {
      return;
    }

    if (EventUtils.isClick(event)) {
      invert();
      fire();
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    ((JsElement) getElement().cast()).setAccessKey(String.valueOf(key));
  }

  @Override
  public void setChecked(boolean checked) {
    if (checked != isChecked()) {
      this.checked = checked;

      if (!Objects.equals(downFace, upFace)) {
        getElement().setInnerHTML(checked ? downFace : upFace);
      }

      setStyleDependentName(STYLE_SUFFIX_CHECKED, checked);
      setStyleDependentName(STYLE_SUFFIX_UNCHECKED, !checked);
    }
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getElement().focus();
    } else {
      getElement().blur();
    }
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  @Override
  public void setTabIndex(int index) {
    getElement().setTabIndex(index);
  }

  @Override
  public void setValue(String value) {
    setChecked(BeeUtils.toBoolean(value));
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    setValue(oldValue);
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }

  private void fire() {
    if (isEditing()) {
      fireEvent(new EditStopEvent(State.CHANGED));
    } else {
      ValueChangeEvent.fire(this, BooleanValue.pack(isChecked()));
    }
  }
}
