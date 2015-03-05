package com.butent.bee.client.composite;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.Global;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditStopEvent.Handler;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorFactory;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.client.view.edit.TextBox;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasTextDimensions;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

/**
 * Implements a component used for making changes to multiple lines of text.
 */

public class TextEditor extends Absolute implements Editor, HasTextDimensions, HasTextBox,
    HasKeyDownHandlers {

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "TextEditor";

  private final InputArea area;
  private final String acceptId;
  private final String noesId;

  private String options;

  private boolean handlesTabulation;

  private boolean summarize;

  public TextEditor() {
    super();

    this.area = new InputArea();
    area.addStyleName(STYLE_NAME + "-area");

    Simple wrapper = new Simple(area);
    wrapper.addStyleName(STYLE_NAME + "-wrapper");

    Image accept = new Image(Global.getImages().accept(), new EditorFactory.Accept(area));
    accept.addStyleName(STYLE_NAME + "-accept");
    this.acceptId = accept.getId();

    Image noes = new Image(Global.getImages().noes(), new EditorFactory.Cancel(area));
    noes.addStyleName(STYLE_NAME + "-cancel");
    this.noesId = noes.getId();

    add(wrapper);
    add(accept);
    add(noes);

    addStyleName(STYLE_NAME);
    sinkEvents(Event.ONMOUSEDOWN);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return getArea().addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addKeyDownHandler(handler);
  }

  @Override
  public HandlerRegistration addEditStopHandler(Handler handler) {
    return getArea().addEditStopHandler(handler);
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return getArea().addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return getArea().addKeyDownHandler(handler);
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
  public int getCharacterWidth() {
    return getArea().getCharacterWidth();
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return getArea().getDefaultFocusAction();
  }

  @Override
  public String getIdPrefix() {
    return "text-editor";
  }

  @Override
  public String getNormalizedValue() {
    return getArea().getNormalizedValue();
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public Value getSummary() {
    return getArea().getSummary();
  }

  @Override
  public int getTabIndex() {
    return getArea().getTabIndex();
  }

  @Override
  public TextBox getTextBox() {
    return getArea();
  }

  @Override
  public String getValue() {
    return getArea().getValue();
  }

  @Override
  public int getVisibleLines() {
    return getArea().getVisibleLines();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_AREA;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return getArea().handlesKey(keyCode);
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  @Override
  public boolean isEditing() {
    return getArea().isEditing();
  }

  @Override
  public boolean isEnabled() {
    return getArea().isEnabled();
  }

  @Override
  public boolean isNullable() {
    return getArea().isNullable();
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && getElement().isOrHasChild(node);
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
    getArea().normalizeDisplay(normalizedValue);
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (EventUtils.isMouseDown(event.getType())) {
      String id = EventUtils.getTargetId(event.getEventTarget());
      if (BeeUtils.same(id, getId())) {
        event.preventDefault();
        return;
      }
      if (BeeUtils.inListSame(id, getAcceptId(), getNoesId())) {
        event.preventDefault();
        State state = BeeUtils.same(id, getAcceptId()) ? State.CHANGED : State.CANCELED;
        getArea().fireEvent(new EditStopEvent(state));
        return;
      }
    }
    super.onBrowserEvent(event);
  }

  @Override
  public void render(String value) {
    setValue(value);
  }

  @Override
  public void setAccessKey(char key) {
    getArea().setAccessKey(key);
  }

  @Override
  public void setCharacterWidth(int width) {
    getArea().setCharacterWidth(width);
  }

  @Override
  public void setEditing(boolean editing) {
    getArea().setEditing(editing);
  }

  @Override
  public void setEnabled(boolean enabled) {
    UiHelper.enableChildren(this, enabled);
  }

  @Override
  public void setFocus(boolean focused) {
    getArea().setFocus(focused);
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  @Override
  public void setNullable(boolean nullable) {
    getArea().setNullable(nullable);
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public void setTabIndex(int index) {
    getArea().setTabIndex(index);
  }

  @Override
  public void setValue(String value) {
    getArea().setValue(value);
  }

  @Override
  public void setVisibleLines(int lines) {
    getArea().setVisibleLines(lines);
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    getArea().startEdit(oldValue, charCode, onEntry, sourceElement);
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return getArea().validate(checkForNull);
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return getArea().validate(normalizedValue, checkForNull);
  }

  private String getAcceptId() {
    return acceptId;
  }

  private InputArea getArea() {
    return area;
  }

  private String getNoesId() {
    return noesId;
  }
}
