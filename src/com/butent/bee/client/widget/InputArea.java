package com.butent.bee.client.widget;

import com.google.common.base.Strings;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.HasInputHandlers;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesAfterSave;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Resource;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.html.Autocomplete;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasAutocomplete;
import com.butent.bee.shared.ui.HasMaxLength;
import com.butent.bee.shared.ui.HasTextDimensions;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collections;
import java.util.List;

import elemental.html.TextAreaElement;

/**
 * Implements a text area that allows multiple lines of text to be entered.
 */

public class InputArea extends CustomWidget implements Editor, TextBox, HandlesAfterSave,
    HasTextDimensions, HasInputHandlers, HasTextBox, HasMaxLength, HasAutocomplete,
    HasKeyDownHandlers {

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "InputArea";

  private Resource resource;

  private String digest;

  private boolean nullable = true;

  private boolean editing;

  private boolean editorInitialized;

  private String options;

  private boolean handlesTabulation;

  private boolean summarize;

  public InputArea() {
    super(Document.get().createTextAreaElement());
  }

  public InputArea(Element element) {
    super(element);
  }

  public InputArea(Resource resource) {
    this();
    this.resource = resource;

    setValue(resource.getContent());
    if (resource.isReadOnly()) {
      getTextAreaElement().setReadOnly(true);
    }
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
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
  public HandlerRegistration addInputHandler(InputHandler handler) {
    return Binder.addInputHandler(this, handler);
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  @Override
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    setValue(BeeConst.STRING_EMPTY);
  }

  @Override
  public String getAutocomplete() {
    if (Features.supportsAutocompleteTextArea()) {
      return getElement().getPropertyString(Attributes.AUTOCOMPLETE);
    } else {
      return null;
    }
  }

  @Override
  public int getCharacterWidth() {
    return getTextAreaElement().getCols();
  }

  @Override
  public int getCursorPos() {
    return getTextAreaElement().getSelectionStart();
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  public String getDigest() {
    return digest;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "area";
  }

  @Override
  public int getMaxLength() {
    return getTextAreaElement().getMaxLength();
  }

  @Override
  public String getName() {
    return getTextAreaElement().getName();
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

  @Override
  public String getOptions() {
    return options;
  }

  public Resource getResource() {
    return resource;
  }

  @Override
  public String getSelectedText() {
    int start = getTextAreaElement().getSelectionStart();
    int end = getTextAreaElement().getSelectionEnd();

    return (start >= 0 && end > start) ? getText().substring(start, end) : BeeConst.STRING_EMPTY;
  }

  @Override
  public int getSelectionLength() {
    return getTextAreaElement().getSelectionEnd() - getTextAreaElement().getSelectionStart();
  }

  @Override
  public Value getSummary() {
    return new IntegerValue(BeeUtils.countLines(getValue()));
  }

  @Override
  public int getTabIndex() {
    return getTextAreaElement().getTabIndex();
  }

  @Override
  public String getText() {
    return getTextAreaElement().getValue();
  }

  @Override
  public TextBox getTextBox() {
    return this;
  }

  @Override
  public String getValue() {
    return Strings.nullToEmpty(getTextAreaElement().getValue());
  }

  @Override
  public int getVisibleLines() {
    return getTextAreaElement().getRows();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_AREA;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return !BeeUtils.inList(keyCode, KeyCodes.KEY_ESCAPE, KeyCodes.KEY_TAB);
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
  public boolean isEnabled() {
    return !getTextAreaElement().isDisabled();
  }

  @Override
  public boolean isMultiline() {
    return true;
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return getElement().equals(node);
  }

  public boolean isValueChanged() {
    String v = getValue();
    String d = getDigest();

    if (BeeUtils.isEmpty(v)) {
      return !BeeUtils.isEmpty(d);
    } else if (BeeUtils.isEmpty(d)) {
      return true;
    } else {
      return !d.equals(Codec.md5(v));
    }
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onAfterSave(String opt) {
    if (BeeUtils.isEmpty(opt)) {
      updateDigest();
    } else {
      setDigest(opt);
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (isEditing() && UiHelper.isSave(event)) {
      event.preventDefault();
      fireEvent(new EditStopEvent(State.CHANGED));
      return;
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void selectAll() {
    if (BeeUtils.hasLength(getText(), 1)) {
      getTextAreaElement().select();
    }
  }

  @Override
  public void setAccessKey(char key) {
    getTextAreaElement().setAccessKey(String.valueOf(key));
  }

  @Override
  public void setAutocomplete(Autocomplete autocomplete) {
    setAutocomplete((autocomplete == null) ? null : autocomplete.build());
  }

  @Override
  public void setAutocomplete(String ac) {
    if (Features.supportsAutocompleteTextArea()) {
      getElement().setPropertyString(Attributes.AUTOCOMPLETE, ac);
    }
  }

  @Override
  public void setCharacterWidth(int width) {
    getTextAreaElement().setCols(width);
  }

  @Override
  public void setCursorPos(int pos) {
    getTextAreaElement().setSelectionRange(pos, pos);
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    getTextAreaElement().setDisabled(!enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getTextAreaElement().focus();
    } else {
      getTextAreaElement().blur();
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
  public void setMaxLength(int maxLength) {
    getTextAreaElement().setMaxLength(maxLength);
  }

  @Override
  public void setName(String name) {
    getTextAreaElement().setName(name);
  }

  @Override
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public void setTabIndex(int index) {
    getTextAreaElement().setTabIndex(index);
  }

  @Override
  public void setText(String text) {
    getTextAreaElement().setValue(Strings.nullToEmpty(text));
  }

  @Override
  public void setValue(String value) {
    getTextAreaElement().setValue(Strings.nullToEmpty(value));
    updateDigest(getValue());
  }

  @Override
  public void setVisibleLines(int lines) {
    getTextAreaElement().setRows(lines);
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    if (!isEditorInitialized()) {
      initEditor();
      setEditorInitialized(true);
    }

    EditorAction action = (onEntry == null) ? EditorAction.ADD_LAST : onEntry;
    EditorAssistant.doEditorAction(this, oldValue, charCode, action);
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  public String updateDigest() {
    return updateDigest(getValue());
  }

  public String updateDigest(String value) {
    if (BeeUtils.isEmpty(value)) {
      setDigest(null);
    } else {
      setDigest(Codec.md5(value));
    }
    return getDigest();
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
  }

  private TextAreaElement getTextAreaElement() {
    return (TextAreaElement) getElement();
  }

  private void initEditor() {
    sinkEvents(Event.ONKEYDOWN);
  }

  private boolean isEditorInitialized() {
    return editorInitialized;
  }

  private void setEditorInitialized(boolean editorInitialized) {
    this.editorInitialized = editorInitialized;
  }
}
