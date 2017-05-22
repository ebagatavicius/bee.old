package com.butent.bee.client.modules.finance.analysis;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.finance.FinanceKeeper;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.view.edit.AdjustmentListener;
import com.butent.bee.client.view.edit.EditChangeHandler;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.finance.analysis.AnalysisCellType;
import com.butent.bee.shared.modules.finance.analysis.AnalysisValueType;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

class AnalysisValueTypesEditor extends Flow implements Editor, AdjustmentListener {

  static final class Renderer extends AbstractCellRenderer {

    Renderer(CellSource cellSource) {
      super(cellSource);
    }

    @Override
    public String render(IsRow row) {
      String values = getString(row);
      if (BeeUtils.isEmpty(values)) {
        return null;
      }

      List<AnalysisCellType> cellTypes = AnalysisCellType.decode(values);
      if (BeeUtils.isEmpty(cellTypes)) {
        return null;
      }

      StringBuilder sb = new StringBuilder();

      for (AnalysisCellType cellType : cellTypes) {
        if (sb.length() > 0) {
          sb.append(BeeConst.STRING_SPACE);
        }

        sb.append(cellType.getAnalysisValueType().getAbbreviation());
        if (!BeeConst.isUndef(cellType.getScale())) {
          sb.append(BeeConst.STRING_POINT).append(cellType.getScale());
        }
      }

      return sb.toString();
    }
  }

  private static final String STYLE_PREFIX =
      FinanceKeeper.STYLE_PREFIX + "AnalysisValueTypesEditor-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";

  private static final String STYLE_HEADER = STYLE_PREFIX + "header";
  private static final String STYLE_SAVE = STYLE_PREFIX + "save";
  private static final String STYLE_CANCEL = STYLE_PREFIX + "cancel";

  private static final String STYLE_VISIBILITY_TOGGLE = STYLE_PREFIX + "toggle";
  private static final String STYLE_SCALE_INPUT = STYLE_PREFIX + "scale";

  private final EnumMap<AnalysisValueType, CheckBox> visibilityWidgets =
      new EnumMap<>(AnalysisValueType.class);
  private final EnumMap<AnalysisValueType, InputSpinner> scaleWidgets =
      new EnumMap<>(AnalysisValueType.class);

  private boolean nullable = true;

  private boolean editing;
  private boolean enabled = true;

  private boolean handlesTabulation;

  private boolean summarize;

  private String value;

  AnalysisValueTypesEditor(boolean embedded) {
    super(STYLE_CONTAINER);
    DomUtils.makeFocusable(this);

    createWidgets();
    renderContent(embedded);

    sinkEvents(Event.ONBLUR);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler blurHandler) {
    return addDomHandler(blurHandler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addEditChangeHandler(EditChangeHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
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
  public HandlerRegistration addSummaryChangeHandler(SummaryChangeEvent.Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public void adjust(Element source) {
    if (source != null) {
      StyleUtils.copyProperties(source, getElement(), StyleUtils.STYLE_LEFT, StyleUtils.STYLE_TOP);
      StyleUtils.setWidth(this, 150);
      StyleUtils.setHeight(this, 120);
    }
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
  public String getIdPrefix() {
    return "ave";
  }

  @Override
  public String getNormalizedValue() {
    return getValue();
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
    return value;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.CUSTOM_EDITABLE;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return false;
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
    if (EventUtils.isBlur(event.getType())) {
      Element relatedElement = EventUtils.getRelatedEventTargetElement(event);

      if (relatedElement != null && getElement().isOrHasChild(relatedElement)) {
        return;
      }
    }

    super.onBrowserEvent(event);
  }

  @Override
  public void render(String v) {
    setValue(v);
    updateDisplay();
  }

  @Override
  public void setAccessKey(char key) {
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
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
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
    this.value = value;
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {

    StyleUtils.clearWidth(this);
    StyleUtils.clearHeight(this);

    setValue(oldValue);
    updateDisplay();
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return BeeConst.EMPTY_IMMUTABLE_STRING_LIST;
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return BeeConst.EMPTY_IMMUTABLE_STRING_LIST;
  }

  private void createWidgets() {
    if (!visibilityWidgets.isEmpty()) {
      visibilityWidgets.clear();
    }
    if (!scaleWidgets.isEmpty()) {
      scaleWidgets.clear();
    }

    for (AnalysisValueType avt : AnalysisValueType.values()) {
      CheckBox checkBox = new CheckBox(avt.getCaption());

      checkBox.addValueChangeHandler(event -> updateValue());
      checkBox.addBlurHandler(this::maybeClose);

      visibilityWidgets.put(avt, checkBox);

      if (avt.hasScale()) {
        InputSpinner spinner = new InputSpinner(ANALYSIS_MIN_SCALE, ANALYSIS_MAX_SCALE, 1);
        spinner.setValue(avt.getDefaultScale());
        spinner.setTitle(Localized.dictionary().finAnalysisScale());

        spinner.addInputHandler(event -> updateValue());
        spinner.addBlurHandler(this::maybeClose);

        scaleWidgets.put(avt, spinner);
      }
    }
  }

  private static AnalysisCellType findCellType(Collection<AnalysisCellType> cellTypes,
      AnalysisValueType avt) {

    for (AnalysisCellType cellType : cellTypes) {
      if (cellType.getAnalysisValueType() == avt) {
        return cellType;
      }
    }

    return null;
  }

  private void maybeClose(DomEvent<?> event) {
    Element relatedElement = EventUtils.getRelatedEventTargetElement(event.getNativeEvent());
    if (relatedElement == null || !getElement().isOrHasChild(relatedElement)) {
      DomEvent.fireNativeEvent(Document.get().createBlurEvent(), this);
    }
  }

  private void renderContent(boolean embedded) {
    HtmlTable table = new HtmlTable(STYLE_TABLE);
    int r = 0;

    if (!embedded) {
      Flow header = new Flow();

      FaLabel save = new FaLabel(Action.SAVE.getIcon());
      save.setTitle(Action.SAVE.getCaption());
      save.addStyleName(STYLE_SAVE);

      save.addClickHandler(event -> fireEvent(new EditStopEvent(State.CHANGED)));
      header.add(save);

      FaLabel cancel = new FaLabel(Action.CANCEL.getIcon());
      cancel.setTitle(Action.CANCEL.getCaption());
      cancel.addStyleName(STYLE_CANCEL);

      cancel.addClickHandler(event -> fireEvent(new EditStopEvent(State.CANCELED)));
      header.add(cancel);

      table.setWidgetAndStyle(r, 0, header, STYLE_HEADER);
      if (!scaleWidgets.isEmpty()) {
        table.getCellFormatter().setColSpan(r, 0, 2);
      }

      r++;
    }

    for (AnalysisValueType avt : AnalysisValueType.values()) {
      if (visibilityWidgets.containsKey(avt)) {
        table.setWidgetAndStyle(r, 0, visibilityWidgets.get(avt), STYLE_VISIBILITY_TOGGLE);

        if (scaleWidgets.containsKey(avt)) {
          table.setWidgetAndStyle(r, 1, scaleWidgets.get(avt), STYLE_SCALE_INPUT);
        }

        r++;
      }
    }

    if (!isEmpty()) {
      clear();
    }
    add(table);
  }

  private void updateDisplay() {
    List<AnalysisCellType> cellTypes = AnalysisCellType.decode(getValue());

    visibilityWidgets.forEach((avt, checkBox) -> {
      AnalysisCellType cellType = findCellType(cellTypes, avt);
      checkBox.setChecked(cellType != null);

      if (scaleWidgets.containsKey(avt)) {
        int scale = (cellType == null || BeeConst.isUndef(cellType.getScale()))
            ? avt.getDefaultScale() : cellType.getScale();
        scaleWidgets.get(avt).setValue(scale);
      }
    });
  }

  private void updateValue() {
    List<AnalysisCellType> cellTypes = new ArrayList<>();

    visibilityWidgets.forEach((avt, checkBox) -> {
      if (checkBox.isChecked()) {
        if (scaleWidgets.containsKey(avt)) {
          int scale = BeeUtils.clamp(scaleWidgets.get(avt).getIntValue(),
              ANALYSIS_MIN_SCALE, ANALYSIS_MAX_SCALE);
          cellTypes.add(new AnalysisCellType(avt, scale));

        } else {
          cellTypes.add(new AnalysisCellType(avt));
        }
      }
    });

    setValue(AnalysisCellType.encode(cellTypes));
  }
}
