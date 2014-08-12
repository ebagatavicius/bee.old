package com.butent.bee.client.widget;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.HasInputHandlers;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.edit.HasCharacterFilter;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.html.Autocomplete;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasAutocomplete;
import com.butent.bee.shared.ui.HasCapsLock;
import com.butent.bee.shared.ui.HasMaxLength;
import com.butent.bee.shared.ui.HasSuggestionSource;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;

import elemental.html.InputElement;

/**
 * Implements a text box that allows a single line of text to be entered.
 */

public class InputText extends CustomWidget implements Editor, TextBox, HasCharacterFilter,
    HasInputHandlers, HasKeyDownHandlers, HasKeyPressHandlers, HasTextBox, HasCapsLock,
    HasMaxLength, HasAutocomplete, HasSuggestionSource, HasChangeHandlers,
    HasValueChangeHandlers<String> {

  private CharMatcher charMatcher;

  private boolean nullable = true;

  private boolean editing;

  private boolean upperCase;

  private String options;

  private boolean handlesTabulation;

  private boolean valueChangeHandlerInitialized;

  private String suggestionSource;

  public InputText() {
    super(Document.get().createTextInputElement());
  }

  public InputText(Element element) {
    super(element);
  }

  @Override
  public boolean acceptChar(char charCode) {
    if (getCharMatcher() == null) {
      return true;
    } else {
      return getCharMatcher().matches(charCode);
    }
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
  public HandlerRegistration addInputHandler(InputHandler handler) {
    return Binder.addInputHandler(this, handler);
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    if (!valueChangeHandlerInitialized) {
      valueChangeHandlerInitialized = true;

      addChangeHandler(new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
          ValueChangeEvent.fire(InputText.this, getValue());
        }
      });
    }

    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void clearValue() {
    setValue(BeeConst.STRING_EMPTY);
  }

  @Override
  public String getAutocomplete() {
    return getInputElement().getAutocomplete();
  }

  public CharMatcher getCharMatcher() {
    return charMatcher;
  }

  @Override
  public int getCursorPos() {
    return getInputElement().getSelectionStart();
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return EditorAction.SELECT;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "txt";
  }

  @Override
  public int getMaxLength() {
    return getInputElement().getMaxLength();
  }

  @Override
  public String getName() {
    return getInputElement().getName();
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

  @Override
  public String getSelectedText() {
    int start = getInputElement().getSelectionStart();
    int end = getInputElement().getSelectionEnd();

    return (start >= 0 && end > start) ? getText().substring(start, end) : BeeConst.STRING_EMPTY;
  }

  @Override
  public int getSelectionLength() {
    return getInputElement().getSelectionEnd() - getInputElement().getSelectionStart();
  }

  @Override
  public String getSuggestionSource() {
    return suggestionSource;
  }

  @Override
  public int getTabIndex() {
    return getInputElement().getTabIndex();
  }

  @Override
  public String getText() {
    return getInputElement().getValue();
  }

  @Override
  public TextBox getTextBox() {
    return this;
  }

  @Override
  public String getValue() {
    return Strings.nullToEmpty(getInputElement().getValue());
  }

  public int getVisibleLength() {
    return getInputElement().getSize();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_TEXT;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return false;
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  public boolean isAllSelected() {
    String text = getText();
    if (BeeUtils.isEmpty(text)) {
      return false;
    } else {
      return BeeUtils.equalsTrim(text, getSelectedText());
    }
  }

  @Override
  public boolean isEditing() {
    return editing;
  }

  public boolean isEmpty() {
    return BeeUtils.isEmpty(getValue());
  }

  @Override
  public boolean isEnabled() {
    return !getInputElement().isDisabled();
  }

  @Override
  public boolean isMultiline() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return getElement().equals(node);
  }

  public boolean isUpperCase() {
    return upperCase;
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.isKeyPress(event.getType())) {
      char charCode = (char) event.getCharCode();

      if (!acceptChar(charCode)) {
        if (event.getCharCode() != 0) {
          event.preventDefault();
        }
      } else if (isUpperCase()) {
        char upper = Character.toUpperCase(charCode);
        if (upper != charCode) {
          event.preventDefault();
          UiHelper.pressKey(this, upper);
        }
      }
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
      getInputElement().select();
    }
  }

  @Override
  public void setAccessKey(char key) {
    getInputElement().setAccessKey(String.valueOf(key));
  }

  @Override
  public void setAutocomplete(Autocomplete autocomplete) {
    setAutocomplete((autocomplete == null) ? null : autocomplete.build());
  }

  @Override
  public void setAutocomplete(String ac) {
    getInputElement().setAutocomplete(ac);
  }

  public void setCharMatcher(CharMatcher charMatcher) {
    this.charMatcher = charMatcher;
  }

  @Override
  public void setCursorPos(int pos) {
    getInputElement().setSelectionRange(pos, pos);
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setEnabled(boolean enabled) {
    getInputElement().setDisabled(!enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getInputElement().focus();
    } else {
      getInputElement().blur();
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
    getInputElement().setMaxLength(maxLength);
  }

  @Override
  public void setName(String name) {
    getInputElement().setName(name);
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
  public void setSuggestionSource(String suggestionSource) {
    this.suggestionSource = suggestionSource;
  }

  @Override
  public void setTabIndex(int index) {
    getInputElement().setTabIndex(index);
  }

  @Override
  public void setText(String text) {
    getInputElement().setValue(Strings.nullToEmpty(text));
  }

  @Override
  public void setUpperCase(boolean upperCase) {
    this.upperCase = upperCase;
  }

  @Override
  public void setValue(String value) {
    String v = (isUpperCase() && !BeeUtils.isEmpty(value)) ? value.toUpperCase()
        : Strings.nullToEmpty(value);
    getInputElement().setValue(v);
  }

  public void setVisibleLength(int length) {
    getInputElement().setSize(length);
  }

  @Override
  public void startEdit(String value, char charCode, EditorAction onEntry, Element sourceElement) {
    EditorAction action = (onEntry == null) ? getDefaultEntryAction() : onEntry;
    EditorAssistant.doEditorAction(this, value, charCode, action);
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }

  protected CharMatcher getDefaultCharMatcher() {
    return CharMatcher.inRange(BeeConst.CHAR_SPACE, Character.MAX_VALUE);
  }

  protected EditorAction getDefaultEntryAction() {
    return EditorAction.REPLACE;
  }

  protected String getDefaultStyleName() {
    return "bee-InputText";
  }

  @Override
  protected void init() {
    super.init();

    if (isTextBox()) {
      addStyleName(StyleUtils.NAME_TEXT_BOX);
    }
    addStyleName(getDefaultStyleName());

    setCharMatcher(getDefaultCharMatcher());
    sinkEvents(Event.ONKEYPRESS);
  }

  protected boolean isTextBox() {
    return true;
  }

  private InputElement getInputElement() {
    return (InputElement) getElement();
  }
}
