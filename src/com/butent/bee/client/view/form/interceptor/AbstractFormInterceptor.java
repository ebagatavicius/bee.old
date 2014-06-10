package com.butent.bee.client.view.form.interceptor;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;

import java.util.List;

public abstract class AbstractFormInterceptor implements FormInterceptor {

  private FormView formView;

  @Override
  public void afterAction(Action action, Presenter presenter) {
  }

  @Override
  public void afterCreate(FormView form) {
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
  }

  @Override
  public void afterStateChange(State state, boolean modal) {
  }
  
  @Override
  public void afterUpdateRow(IsRow result) {
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    return true;
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    return true;
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
  }

  @Override
  public void beforeStateChange(State state, boolean modal) {
  }
  
  @Override
  public IdentifiableWidget createCustomWidget(String name, Element description) {
    return null;
  }

  @Override
  public boolean focusSource(String source) {
    if (getFormView() == null) {
      return false;
    } else {
      Widget widget = getFormView().getWidgetBySource(source);
      return (widget == null) ? null : UiHelper.focus(widget);
    }
  }

  @Override
  public IsRow getActiveRow() {
    return (getFormView() == null) ? null : getFormView().getActiveRow();
  }

  @Override
  public long getActiveRowId() {
    return (getFormView() == null) ? BeeConst.UNDEF : getFormView().getActiveRowId();
  }

  @Override
  public int getDataIndex(String source) {
    return (getFormView() == null) ? BeeConst.UNDEF : getFormView().getDataIndex(source);
  }

  @Override
  public DateTime getDateTimeValue(String source) {
    return (getFormView() == null) ? null : getFormView().getDateTimeValue(source);
  }

  @Override
  public JustDate getDateValue(String source) {
    return (getFormView() == null) ? null : getFormView().getDateValue(source);
  }

  @Override
  public Domain getDomain() {
    return null;
  }

  @Override
  public FormView getFormView() {
    return formView;
  }

  @Override
  public GridView getGridView() {
    if (getFormView() != null && getFormView().getViewPresenter() instanceof HasGridView) {
      return ((HasGridView) getFormView().getViewPresenter()).getGridView();
    } else {
      return null;
    }
  }

  @Override
  public HeaderView getHeaderView() {
    return (getFormView() == null || getFormView().getViewPresenter() == null) ? null
        : getFormView().getViewPresenter().getHeader();
  }

  @Override
  public Integer getIntegerValue(String source) {
    return (getFormView() == null) ? null : getFormView().getIntegerValue(source);
  }

  @Override
  public Long getLongValue(String source) {
    return (getFormView() == null) ? null : getFormView().getLongValue(source);
  }

  @Override
  public AbstractCellRenderer getRenderer(WidgetDescription widgetDescription) {
    return null;
  }

  @Override
  public BeeRowSet getRowSet() {
    return null;
  }

  @Override
  public String getStringValue(String source) {
    return (getFormView() == null) ? null : getFormView().getStringValue(source);
  }

  @Override
  public String getViewName() {
    return (getFormView() == null) ? null : getFormView().getViewName();
  }

  @Override
  public boolean hasFooter(int rowCount) {
    return true;
  }

  @Override
  public boolean isRowEditable(IsRow row) {
    return row != null && row.isEditable();
  }
  
  @Override
  public void notifyRequired(String message) {
    if (getFormView() != null) {
      getFormView().notifyWarning(message, Localized.getConstants().valueRequired());
    }
  }

  @Override
  public void onClose(List<String> messages, IsRow oldRow, IsRow newRow) {
  }

  @Override
  public void onLoad(FormView form) {
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
  }

  @Override
  public void onSetActiveRow(IsRow row) {
  }

  @Override
  public void onShow(Presenter presenter) {
  }

  @Override
  public void onStart(FormView form) {
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, Scheduler.ScheduledCommand focusCommand) {
    return true;
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
  }

  @Override
  public void onStateChange(State state) {
  }

  @Override
  public void onUnload(FormView form) {
  }

  @Override
  public void setFormView(FormView formView) {
    this.formView = formView;
  }
}
