package com.butent.bee.client.richtext;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.PreviewHandler;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.AdjustmentListener;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.EditStopEvent.Handler;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;

import elemental.js.dom.JsElement;

/**
 * Enables usage of formatted text editor user interface component.
 */

public class RichTextEditor extends Flow implements Editor, AdjustmentListener, PreviewHandler,
    HasKeyDownHandlers {

  private static final String STYLE_CONTAINER = BeeConst.CSS_CLASS_PREFIX + "RichTextEditor";
  private static final String STYLE_CONTAINER_EMBEDDED = BeeConst.CSS_CLASS_PREFIX
      + "RichTextEditor-embedded";
  private static final String STYLE_TOOLBAR = BeeConst.CSS_CLASS_PREFIX + "RichTextToolbar";
  private static final String STYLE_PANEL = BeeConst.CSS_CLASS_PREFIX + "RichTextPanel";
  private static final String STYLE_AREA = BeeConst.CSS_CLASS_PREFIX + "RichTextArea";

  private final RichTextToolbar toolbar;
  private final RichTextArea area;

  private final boolean embedded;

  private boolean nullable = true;

  private boolean editing;

  private String options;

  private boolean handlesTabulation;

  private boolean summarize;

  public RichTextEditor(boolean embedded) {
    super();

    this.embedded = embedded;

    this.area = new RichTextArea();
    this.area.setStyleName(STYLE_AREA);
    DomUtils.createId(this.area, "rt-area");

    this.toolbar = new RichTextToolbar(this, this.area, embedded);
    this.toolbar.addStyleName(STYLE_TOOLBAR);

    Simple panel = new Simple(this.area);
    panel.addStyleName(STYLE_PANEL);

    add(this.toolbar);
    add(panel);

    addStyleName(STYLE_CONTAINER);
    if (embedded) {
      addStyleName(STYLE_CONTAINER_EMBEDDED);
    }
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    if (isEmbedded()) {
      return getArea().addBlurHandler(handler);
    } else {
      return addDomHandler(handler, BlurEvent.getType());
    }
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addKeyDownHandler(handler);
  }

  @Override
  public HandlerRegistration addEditStopHandler(Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    if (isEmbedded()) {
      return getArea().addFocusHandler(handler);
    } else {
      return addDomHandler(handler, FocusEvent.getType());
    }
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
  public void adjust(Element source) {
    if (source != null) {
      StyleUtils.copyProperties(source, getElement(), StyleUtils.STYLE_LEFT, StyleUtils.STYLE_TOP);
      StyleUtils.setWidth(this, source.getOffsetWidth());
      StyleUtils.setHeight(this, source.getOffsetHeight());
    }
  }

  @Override
  public void clearValue() {
    setValue(BeeConst.STRING_EMPTY);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  @Override
  public String getIdPrefix() {
    return "rt-editor";
  }

  @Override
  public String getNormalizedValue() {
    if (getValue() == null) {
      return null;
    }
    return BeeUtils.trimRight(getValue());
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public Value getSummary() {
    return new IntegerValue(BeeUtils.countLines(getValue()));
  }

  @Override
  public int getTabIndex() {
    return getArea().getTabIndex();
  }

  @Override
  public String getValue() {
    return getArea().getHtml();
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.RICH_TEXT_EDITOR;
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
    return getArea().isEnabled();
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return node != null && (getElement().isOrHasChild(node) || getArea().contains(node));
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onEventPreview(NativePreviewEvent event, Node targetNode) {
    if (isEditing() && !isEmbedded() && !toolbar.isWaiting()
        && EventUtils.isMouseDown(event.getNativeEvent().getType())
        && !isOrHasPartner(targetNode)) {
      fireEvent(new EditStopEvent(State.CANCELED));
    }
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
  public void setEditing(boolean editing) {
    this.editing = editing;

    if (!isEmbedded()) {
      if (editing) {
        Previewer.ensureRegistered(this);
      } else {
        closePreview();
        setFocus(false);
      }
    }
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
    this.nullable = nullable;
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
    getArea().setHtml(value);
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    EditorAction action = (onEntry == null) ? EditorAction.ADD_LAST : onEntry;
    EditorAssistant.doEditorAction(this, oldValue, charCode, action);

    if (!isEmbedded()) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          StyleUtils.setSize(getArea(), getArea().getParent().getOffsetWidth(),
              getArea().getParent().getOffsetHeight());
          getToolbar().updateStatus();
        }
      });
    }
  }

  @Override
  public boolean summarize() {
    return summarize;
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
  protected void onUnload() {
    closePreview();
    super.onUnload();
  }

  private void closePreview() {
    Previewer.ensureUnregistered(this);
  }

  private RichTextArea getArea() {
    return area;
  }

  private RichTextToolbar getToolbar() {
    return toolbar;
  }

  private boolean isEmbedded() {
    return embedded;
  }
}
