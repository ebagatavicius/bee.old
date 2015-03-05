package com.butent.bee.client.view.form.interceptor;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.client.data.HasActiveRow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.WidgetDescription;
import com.butent.bee.client.ui.WidgetInterceptor;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Action;

import java.util.List;
import java.util.Set;

public interface FormInterceptor extends WidgetInterceptor, HasGridView, HandlesStateChange,
    HasDomain, HasActiveRow, HasViewName, EditEndEvent.Handler {

  void afterAction(Action action, Presenter presenter);

  void afterCreate(FormView form);

  void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget);

  void afterInsertRow(IsRow result, boolean forced);

  void afterRefresh(FormView form, IsRow row);

  void afterStateChange(State state, boolean modal);

  void afterUpdateRow(IsRow result);

  boolean beforeAction(Action action, Presenter presenter);

  void beforeRefresh(FormView form, IsRow row);

  void beforeStateChange(State state, boolean modal);

  boolean focusSource(String source);

  long getActiveRowId();

  int getDataIndex(String source);

  DateTime getDateTimeValue(String source);

  Set<Action> getDisabledActions(Set<Action> defaultActions);

  Set<Action> getEnabledActions(Set<Action> defaultActions);

  FormView getFormView();

  HeaderView getHeaderView();

  FormInterceptor getInstance();

  Integer getIntegerValue(String source);

  JustDate getDateValue(String source);

  Long getLongValue(String source);

  AbstractCellRenderer getRenderer(WidgetDescription widgetDescription);

  BeeRowSet getRowSet();

  String getStringValue(String source);

  boolean hasFooter(int rowCount);

  boolean isRowEditable(IsRow row);

  boolean isWidgetEditable(EditableWidget editableWidget, IsRow row);

  void notifyRequired(String message);

  void onClose(List<String> messages, IsRow oldRow, IsRow newRow);

  void onLoad(FormView form);

  void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event);

  void onSaveChanges(HasHandlers listener, SaveChangesEvent event);

  void onSetActiveRow(IsRow row);

  void onShow(Presenter presenter);

  void onStart(FormView form);

  boolean onStartEdit(FormView form, IsRow row, Scheduler.ScheduledCommand focusCommand);

  void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow);

  void onUnload(FormView form);

  void setFormView(FormView form);
}