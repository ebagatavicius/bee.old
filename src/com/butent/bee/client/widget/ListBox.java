package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.ui.AcceptsCaptions;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasValueStartIndex;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import elemental.js.dom.JsElement;

/**
 * Implements a list box user interface component that presents a list of choices to the user.
 */

public class ListBox extends CustomWidget implements Editor, HasItems, HasValueStartIndex,
    AcceptsCaptions, HasChangeHandlers, HasKeyDownHandlers {

  public static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "ListBox";

  private boolean nullable = true;

  private boolean editing;

  private boolean valueNumeric;
  private int valueStartIndex;

  private int minSize = BeeConst.UNDEF;
  private int maxSize = BeeConst.UNDEF;

  private boolean changePending;
  private boolean deselectPending;

  private String options;

  private boolean handlesTabulation;

  private boolean summarize;

  public ListBox() {
    this(false);
  }

  public ListBox(boolean multiple) {
    super(Document.get().createSelectElement());
    if (multiple) {
      getSelectElement().setMultiple(multiple);
    }
    init();
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    return addDomHandler(handler, ChangeEvent.getType());
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
    return addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public void addItem(String item) {
    addItem(item, item);
  }

  public void addItem(String text, String value) {
    OptionElement option = Document.get().createOptionElement();
    option.setText(text);
    option.setValue(value);

    getSelectElement().add(option, null);
    updateSize();
  }

  @Override
  public void addItems(Collection<String> items) {
    Assert.notNull(items);
    for (String it : items) {
      addItem(it);
    }
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  public void clear() {
    getSelectElement().clear();
    updateSize();
  }

  @Override
  public void clearValue() {
    deselect();
  }

  public void deselect() {
    setSelectedIndex(BeeConst.UNDEF);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "list";
  }

  public int getIndex(String text) {
    int index = BeeConst.UNDEF;
    if (BeeUtils.isEmpty(text)) {
      return index;
    }

    if (isValueNumeric()) {
      if (BeeUtils.isDigit(BeeUtils.trim(text))) {
        int z = BeeUtils.toInt(text) - getValueStartIndex();
        if (isIndex(z)) {
          index = z;
        }
      }
    } else {
      for (int i = 0; i < getItemCount(); i++) {
        if (BeeUtils.same(getValue(i), text)) {
          index = i;
          break;
        }
      }
    }
    return index;
  }

  @Override
  public int getItemCount() {
    return getSelectElement().getOptions().getLength();
  }

  @Override
  public List<String> getItems() {
    List<String> items = new ArrayList<>();
    for (int i = 0; i < getItemCount(); i++) {
      items.add(getItemText(i));
    }
    return items;
  }

  public String getItemText(int index) {
    checkIndex(index);
    return getSelectElement().getOptions().getItem(index).getText();
  }

  public int getMaxSize() {
    return maxSize;
  }

  public int getMinSize() {
    return minSize;
  }

  @Override
  public String getNormalizedValue() {
    String v = getValue();
    if (BeeUtils.isEmpty(v) && isNullable()) {
      return null;
    } else {
      return BeeUtils.trimRight(v);
    }
  }

  public OptionElement getOptionElement(int index) {
    if (isIndex(index)) {
      return getSelectElement().getOptions().getItem(index);
    } else {
      return null;
    }
  }

  @Override
  public String getOptions() {
    return options;
  }

  public int getSelectedIndex() {
    return getSelectElement().getSelectedIndex();
  }

  @Override
  public Value getSummary() {
    return BooleanValue.of(!BeeUtils.isEmpty(getValue()));
  }

  @Override
  public int getTabIndex() {
    return getElement().getTabIndex();
  }

  @Override
  public String getValue() {
    int index = getSelectedIndex();
    if (!isIndex(index)) {
      return null;
    } else if (isValueNumeric()) {
      return BeeUtils.toString(index + getValueStartIndex());
    } else {
      return getValue(index);
    }
  }

  public String getValue(int index) {
    checkIndex(index);
    return getSelectElement().getOptions().getItem(index).getValue();
  }

  @Override
  public int getValueStartIndex() {
    return valueStartIndex;
  }

  public int getVisibleItemCount() {
    return getSelectElement().getSize();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.LIST_BOX;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return BeeUtils.inList(keyCode, KeyCodes.KEY_UP, KeyCodes.KEY_DOWN);
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  @Override
  public boolean isEditing() {
    return editing;
  }

  @Override
  public boolean isEmpty() {
    return getItemCount() <= 0;
  }

  @Override
  public boolean isEnabled() {
    return !getSelectElement().isDisabled();
  }

  @Override
  public boolean isIndex(int index) {
    return index >= 0 && index < getItemCount();
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && getElement().isOrHasChild(node);
  }

  public boolean isValueNumeric() {
    return valueNumeric;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onBrowserEvent(Event event) {
    String type = event.getType();

    if (EventUtils.isChange(type)) {
      setChangePending(true);

    } else if (EventUtils.isMouseDown(type)) {
      setChangePending(false);

    } else if (EventUtils.isMouseUp(type)) {
      if (isChangePending() && isEditing()) {
        setChangePending(false);
        fireEvent(new EditStopEvent(State.CHANGED));
      }

    } else if (EventUtils.isKeyDown(type)) {
      if (isNullable() && event.getKeyCode() == KeyCodes.KEY_DELETE
          && !BeeConst.isUndef(getSelectedIndex())) {

        clearValue();
        fireEvent(new EditStopEvent(State.CHANGED));
      }
    }

    super.onBrowserEvent(event);
  }

  public void removeItem(int index) {
    checkIndex(index);
    getSelectElement().remove(index);
    updateSize();
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    ((JsElement) getElement().cast()).setAccessKey(String.valueOf(key));
  }

  public void setAllVisible() {
    int cnt = getItemCount();
    if (cnt > 0) {
      setVisibleItemCount(cnt);
    }
  }

  @Override
  public void setCaptions(Class<? extends Enum<?>> clazz) {
    if (!isEmpty()) {
      clear();
    }
    for (Pair<Integer, String> pair : EnumUtils.getSortedCaptions(clazz)) {
      addItem(pair.getB(), BeeUtils.toString(pair.getA()));
    }
    setValueNumeric(false);
  }

  @Override
  public void setCaptions(String captionKey) {
    setCaptions(EnumUtils.getClassByKey(captionKey));
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    getSelectElement().setDisabled(!enabled);
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
  public void setItems(Collection<String> items) {
    if (getItemCount() > 0) {
      clear();
    }
    if (items != null) {
      addItems(items);
    }
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  public void setMinSize(int minSize) {
    this.minSize = minSize;
  }

  @Override
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setSelectedIndex(int index) {
    getSelectElement().setSelectedIndex(index);
    if (!isAttached()) {
      setDeselectPending(BeeConst.isUndef(index));
    }
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public void setTabIndex(int index) {
    getElement().setTabIndex(index);
  }

  @Override
  public void setValue(String value) {
    setSelectedIndex(getIndex(value));
  }

  public void setValueNumeric(boolean valueNumeric) {
    this.valueNumeric = valueNumeric;
  }

  @Override
  public void setValueStartIndex(int valueStartIndex) {
    this.valueStartIndex = valueStartIndex;
  }

  public void setVisibleItemCount(int visibleItems) {
    getSelectElement().setSize(visibleItems);
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    if (charCode > BeeConst.CHAR_SPACE) {
      for (int i = 0; i < getItemCount(); i++) {
        if (BeeUtils.startsWith(getValue(i), charCode)) {
          setSelectedIndex(i);
          return;
        }
      }
    }
    setSelectedIndex(Math.max(getIndex(oldValue), 0));
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  public void updateSize() {
    int size = Math.max(getMinSize(), 1);
    if (getMaxSize() > 0) {
      size = Math.max(size, getItemCount());
      size = Math.min(size, getMaxSize());
    }

    if (size != getVisibleItemCount()) {
      setVisibleItemCount(size);
    }
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  protected void init() {
    super.init();
    addStyleName(STYLE_NAME);
    sinkEvents(Event.ONCHANGE | Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONKEYDOWN);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    if (isDeselectPending()) {
      setDeselectPending(false);
      deselect();
    }
  }

  private void checkIndex(int index) {
    Assert.isIndex(index, getItemCount());
  }

  private SelectElement getSelectElement() {
    return getElement().cast();
  }

  private boolean isChangePending() {
    return changePending;
  }

  private boolean isDeselectPending() {
    return deselectPending;
  }

  private void setChangePending(boolean changePending) {
    this.changePending = changePending;
  }

  private void setDeselectPending(boolean deselectPending) {
    this.deselectPending = deselectPending;
  }
}
