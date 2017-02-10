package com.butent.bee.client.view.edit;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.HasRelatedRow;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.ActiveWidgetChangeEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.validation.CellValidationBus;
import com.butent.bee.client.validation.EditorValidation;
import com.butent.bee.client.validation.HasCellValidationHandlers;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.client.validation.ValidationOrigin;
import com.butent.bee.client.view.form.DisplayWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.HasPercentageTag;
import com.butent.bee.shared.data.HasRelatedCurrency;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.RefreshType;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditableWidget implements EditChangeHandler, FocusHandler, BlurHandler,
    EditStopEvent.Handler, HasCellValidationHandlers {

  private static final BeeLogger logger = LogUtils.getLogger(EditableWidget.class);

  private final int dataIndex;
  private final BeeColumn dataColumn;

  private final WidgetDescription widgetDescription;
  private final DisplayWidget displayWidget;

  private final Evaluator editable;
  private final Evaluator validation;
  private final Evaluator carry;

  private final boolean required;
  private final boolean readOnly;

  private final CellValidationBus cellValidationBus = new CellValidationBus();
  private HasCellValidationHandlers validationDelegate;

  private boolean initialized;
  private boolean dirty;

  private Editor editor;
  private FormView form;

  public EditableWidget(List<BeeColumn> dataColumns, int dataIndex,
      WidgetDescription widgetDescription, DisplayWidget displayWidget) {
    Assert.notNull(widgetDescription);

    this.dataIndex = dataIndex;

    this.widgetDescription = widgetDescription;
    this.displayWidget = displayWidget;

    if (dataIndex >= 0) {
      this.dataColumn = dataColumns.get(dataIndex);

      String source = dataColumn.getId();
      this.editable = Evaluator.create(widgetDescription.getEditable(), source, dataColumns);
      this.validation = Evaluator.create(widgetDescription.getValidation(), source, dataColumns);
      this.carry = Evaluator.create(widgetDescription.getCarry(), source, dataColumns);

    } else {
      this.dataColumn = null;

      this.editable = null;
      this.validation = null;
      this.carry = null;
    }

    this.required = BeeUtils.isTrue(widgetDescription.getRequired());
    this.readOnly = BeeUtils.isTrue(widgetDescription.getReadOnly());
  }

  @Override
  public HandlerRegistration addCellValidationHandler(CellValidateEvent.Handler handler) {
    return cellValidationBus.addCellValidationHandler(handler);
  }

  public void bind(FormView formView) {
    if (isInitialized()) {
      return;
    }

    Assert.notNull(formView);
    setForm(formView);

    Widget widget = DomUtils.getChildQuietly(formView.asWidget(), getWidgetId());
    if (widget instanceof Editor) {
      setEditor((Editor) widget);
      getEditor().setNullable(isNullable());

      getEditor().addFocusHandler(this);
      getEditor().addBlurHandler(this);

      getEditor().addEditChangeHandler(this);
      getEditor().addEditStopHandler(this);

      setInitialized(true);
    } else {
      logger.warning("editable widget: no editor", getCaption(), getWidgetId());
    }
  }

  public boolean checkForUpdate(boolean normalize) {
    return update(null, false, normalize);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof EditableWidget)) {
      return false;
    }
    return getWidgetDescription().equals(((EditableWidget) obj).getWidgetDescription());
  }

  @Override
  public Boolean fireCellValidation(CellValidateEvent event) {
    if (getValidationDelegate() == null) {
      return cellValidationBus.fireCellValidation(event);
    } else {
      return getValidationDelegate().fireCellValidation(event);
    }
  }

  public String getCaption() {
    if (!BeeUtils.isEmpty(getWidgetDescription().getCaption())) {
      return getWidgetDescription().getCaption();
    } else if (hasColumn()) {
      return Localized.getLabel(getDataColumn());
    } else {
      return null;
    }
  }

  public String getCarryValue(IsRow row) {
    if (row == null || getCarry() == null || !hasColumn()) {
      return null;
    }

    getCarry().update(row, BeeConst.UNDEF, getDataIndex(), getDataType(),
        row.getString(getDataIndex()));
    String value = getCarry().evaluate();
    if (BeeUtils.isEmpty(value)) {
      return null;
    }

    switch (getDataType()) {
      case BOOLEAN:
        return BooleanValue.pack(BeeUtils.toBooleanOrNull(value));

      case DATE:
        if (BeeUtils.isLong(value)) {
          return BeeUtils.toString(new JustDate(TimeUtils.toDateTimeOrNull(value)).getDays());
        } else {
          return null;
        }

      case DATE_TIME:
        return BeeUtils.isLong(value) ? value.trim() : null;

      case DECIMAL:
        return BeeUtils.isDecimal(value) ? value.trim() : null;

      case INTEGER:
        return BeeUtils.isInt(value) ? value.trim() : null;

      case LONG:
        return BeeUtils.isLong(value) ? value.trim() : null;

      case NUMBER:
        return BeeUtils.isDouble(value) ? value.trim() : null;

      case TEXT:
      case BLOB:
      case TIME_OF_DAY:
        return BeeUtils.trimRight(value);
    }
    return null;
  }

  public String getColumnId() {
    return hasColumn() ? getDataColumn().getId() : null;
  }

  public BeeColumn getDataColumn() {
    return dataColumn;
  }

  public int getDataIndex() {
    return dataIndex;
  }

  public Editor getEditor() {
    return editor;
  }

  public Relation getRelation() {
    return getWidgetDescription().getRelation();
  }

  public boolean getRowModeForUpdate() {
    if (getUpdateMode() == null) {
      return hasRelation();
    } else {
      return RefreshType.ROW.equals(getUpdateMode());
    }
  }

  public String getRowPropertyName() {
    return widgetDescription.getRowProperty();
  }

  public Boolean getUserMode() {
    return widgetDescription.getUserMode();
  }

  public HasCellValidationHandlers getValidationDelegate() {
    return validationDelegate;
  }

  public String getWidgetId() {
    return getWidgetDescription().getWidgetId();
  }

  public String getWidgetName() {
    return getWidgetDescription().getWidgetName();
  }

  public boolean hasCarry() {
    return getCarry() != null;
  }

  public boolean hasColumn() {
    return dataColumn != null;
  }

  public boolean hasDefaults() {
    return hasColumn() && getDataColumn().hasDefaults();
  }

  @Override
  public int hashCode() {
    return getWidgetDescription().hashCode();
  }

  public boolean hasRelation() {
    return getRelation() != null;
  }

  public boolean hasRowProperty() {
    return !BeeUtils.isEmpty(getRowPropertyName());
  }

  public boolean hasSource(String source) {
    if (BeeUtils.isEmpty(source)) {
      return false;
    } else if (hasColumn()) {
      return BeeUtils.same(source, getColumnId());
    } else {
      return BeeUtils.same(source, getRowPropertyName());
    }
  }

  public boolean isDirty() {
    return dirty;
  }

  public boolean isDisplay() {
    return getDisplayWidget() != null;
  }

  public boolean isEditable(IsRow row) {
    if (row == null) {
      return false;
    }
    if (getEditable() == null || !hasColumn()) {
      return true;
    }

    getEditable().update(row, BeeConst.UNDEF, getDataIndex(), getDataType(),
        row.getString(getDataIndex()));
    return BeeUtils.toBoolean(getEditable().evaluate());
  }

  public boolean isFocusable() {
    if (getWidgetDescription().getWidgetType() != null) {
      return getWidgetDescription().getWidgetType().isFocusable();
    }
    return false;
  }

  public boolean isNullable() {
    if (isRequired()) {
      return false;
    } else if (hasColumn()) {
      return getDataColumn().isNullable();
    } else {
      return true;
    }
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public Collection<String> maybeUpdateRelation(String viewName, IsRow row) {
    Set<String> result = new HashSet<>();

    if (!BeeUtils.isEmpty(viewName) && row != null) {
      if (getEditor() instanceof HasRelatedRow && getRelation() != null && hasColumn()
          && getRelation().renderTarget()) {

        result.addAll(RelationUtils.updateRow(Data.getDataInfo(viewName), getColumnId(), row,
            Data.getDataInfo(getRelation().getViewName()),
            ((HasRelatedRow) getEditor()).getRelatedRow(), false));
      }

      if (getEditor() instanceof HasRelatedCurrency) {
        String currencySource = ((HasRelatedCurrency) getEditor()).getCurrencySource();

        if (!BeeUtils.isEmpty(currencySource)) {
          result.addAll(RelationUtils.maybeUpdateCurrency(Data.getDataInfo(viewName), row,
              currencySource, getEditor().getNormalizedValue() != null));
        }
      }

      if (getEditor() instanceof HasPercentageTag) {
        String percentageTag = ((HasPercentageTag) getEditor()).getPercentageTag();

        if (!BeeUtils.isEmpty(percentageTag)
            && HasPercentageTag.maybeUpdate(Data.getDataInfo(viewName), row,
            BeeUtils.toDoubleOrNull(getEditor().getNormalizedValue()), percentageTag)) {

          result.add(percentageTag);
        }
      }
    }

    return result;
  }

  @Override
  public void onBlur(BlurEvent event) {
    getEditor().setEditing(false);
    getForm().onActiveWidgetChange(new ActiveWidgetChangeEvent(getWidgetId(), false));
  }

  @Override
  public void onEditStop(EditStopEvent event) {
    if (event.isChanged()) {
      update(KeyCodes.KEY_TAB, false, true);

    } else if (event.isError()) {
      if (getForm() != null) {
        getForm().notifySevere(event.getMessage());
      }

    } else if (event.isCanceled()) {
      reset();

    } else if (event.isClosed()) {
      reset();
      end(event.getKeyCode(), event.hasModifiers());

    } else if (event.isEdited() && getForm() != null) {
      Collection<String> updated =
          maybeUpdateRelation(getForm().getViewName(), getForm().getActiveRow());

      if (!BeeUtils.isEmpty(updated)) {
        getForm().refresh(false, true);
      }
    }
  }

  @Override
  public void onFocus(FocusEvent event) {
    getEditor().setEditing(true);
    getForm().onActiveWidgetChange(new ActiveWidgetChangeEvent(getWidgetId(), true));

    if (event.getSource() instanceof HasTextBox) {
      TextBox widget = ((HasTextBox) event.getSource()).getTextBox();
      String value = widget.getText();
      if (BeeUtils.isEmpty(value)) {
        return;
      }

      EditorAction action = getWidgetDescription().getOnFocus();
      if (action == null) {
        action = ((Editor) widget).getDefaultFocusAction();
        if (action == null) {
          return;
        }
      }

      switch (action) {
        case END:
          widget.setCursorPos(value.length());
          break;
        case HOME:
          widget.setCursorPos(0);
          break;
        case SELECT:
          UiHelper.selectDeferred(widget);
          break;
        default:
      }
    }
  }

  @Override
  public void onKeyDown(KeyDownEvent event) {
    int keyCode = event.getNativeKeyCode();
    if (getEditor() == null || getEditor().handlesKey(keyCode)) {
      return;
    }

    switch (keyCode) {
      case KeyCodes.KEY_ENTER:
      case KeyCodes.KEY_TAB:
        event.preventDefault();
        update(keyCode, EventUtils.hasModifierKey(event.getNativeEvent()), true);
        break;
    }
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    update(null, false, true);
  }

  public void refresh(IsRow row) {
    if (getEditor() != null) {
      String value;

      if (hasColumn()) {
        if (row == null) {
          value = BeeConst.STRING_EMPTY;
        } else {
          value = BeeUtils.trimRight(row.getString(getDataIndex()));
        }

        getEditor().render(value);

      } else if (hasRowProperty() && !isDisplay()) {
        if (row == null) {
          value = BeeConst.STRING_EMPTY;
        } else {
          Long userId = BeeKeeper.getUser().idOrNull(getUserMode());
          value = BeeUtils.trimRight(row.getProperty(getRowPropertyName(), userId));
        }

        getEditor().render(value);
      }

      if (isDisplay()) {
        getDisplayWidget().refresh((Widget) getEditor(), row);
      }

      maybeSummarize();
    }

    setDirty(false);
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  public void setValidationDelegate(HasCellValidationHandlers validationDelegate) {
    this.validationDelegate = validationDelegate;
  }

  public boolean validate(ValidationOrigin origin) {
    if (isReadOnly() || !getEditor().isEnabled()) {
      return true;

    } else {
      String oldValue = getOldValue();
      String newValue = getEditor().getNormalizedValue();

      return validate(oldValue, newValue, origin);
    }
  }

  private void end(Integer keyCode, boolean hasModifiers) {
    if (getForm() != null) {
      getForm().onEditEnd(new EditEndEvent(keyCode, hasModifiers, getWidgetId()), this);
    }
  }

  private Evaluator getCarry() {
    return carry;
  }

  private ValueType getDataType() {
    return hasColumn() ? getDataColumn().getType() : null;
  }

  private DisplayWidget getDisplayWidget() {
    return displayWidget;
  }

  private Evaluator getEditable() {
    return editable;
  }

  private FormView getForm() {
    return form;
  }

  private String getOldValue() {
    if (getRowValue() == null) {
      return null;

    } else if (hasColumn()) {
      return getRowValue().getString(getDataIndex());

    } else if (hasRowProperty()) {
      Long userId = BeeKeeper.getUser().idOrNull(getUserMode());
      return getRowValue().getProperty(getRowPropertyName(), userId);

    } else {
      return null;
    }
  }

  private IsRow getRowValue() {
    if (getForm() == null) {
      return null;
    }
    return getForm().getActiveRow();
  }

  private RefreshType getUpdateMode() {
    return getWidgetDescription().getUpdateMode();
  }

  private Evaluator getValidation() {
    return validation;
  }

  private WidgetDescription getWidgetDescription() {
    return widgetDescription;
  }

  private boolean isInitialized() {
    return initialized;
  }

  private boolean isRequired() {
    return required;
  }

  private void maybeSummarize() {
    if (getEditor() != null && getEditor().summarize()) {
      SummaryChangeEvent.fire(getEditor());
    }
  }

  private void reset() {
    if (hasColumn() || hasRowProperty() || isDisplay()) {
      refresh(getRowValue());
    } else if (getEditor() != null) {
      getEditor().normalizeDisplay(getEditor().getNormalizedValue());
    }
  }

  private void setEditor(Editor editor) {
    this.editor = editor;
  }

  private void setForm(FormView form) {
    this.form = form;
  }

  private void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  private boolean update(Integer keyCode, boolean hasModifiers, boolean normalize) {
    String oldValue = getOldValue();
    String newValue = getEditor().getNormalizedValue();

    if (BeeUtils.equalsTrimRight(oldValue, newValue)) {
      if (normalize) {
        reset();
      }

      if (keyCode == null) {
        if (getRowValue() == null) {
          maybeSummarize();
        }
        return true;
      }

    } else if (validate(oldValue, newValue, ValidationOrigin.CELL)) {
      if (normalize) {
        getEditor().normalizeDisplay(newValue);
      }
      setDirty(true);

    } else {
      if (normalize) {
        if (hasColumn() || hasRowProperty() || isDisplay()) {
          reset();
        } else {
          getEditor().clearValue();
          maybeSummarize();
        }
      }
      return false;
    }

    if (getForm() != null) {
      getForm().onEditEnd(new EditEndEvent(getRowValue(), getDataColumn(),
          oldValue, newValue, getRowModeForUpdate(), hasRelation(), keyCode, hasModifiers,
          getWidgetId()), this);
    }

    maybeSummarize();

    return true;
  }

  private boolean validate(String oldValue, String newValue, ValidationOrigin origin) {
    if (hasColumn()) {
      CellValidation cellValidation = new CellValidation(oldValue, newValue, getEditor(),
          EditorValidation.INPUT, getValidation(), getRowValue(), getDataColumn(), getDataIndex(),
          getDataType(), isNullable(), getCaption(), getForm());
      return BeeUtils.isTrue(ValidationHelper.validateCell(cellValidation, this, origin));

    } else if (getEditor() != null && getForm() != null) {
      List<String> messages = getEditor().validate(true);

      if (BeeUtils.isEmpty(messages)) {
        return true;
      } else {
        ValidationHelper.showError(getForm(), getCaption(), messages);
        return false;
      }

    } else {
      return true;
    }
  }
}
