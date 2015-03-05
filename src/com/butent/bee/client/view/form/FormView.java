package com.butent.bee.client.view.form;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.event.DndWidget;
import com.butent.bee.client.event.logical.ActiveWidgetChangeEvent;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormDescription;
import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.add.HasAddEndHandlers;
import com.butent.bee.client.view.add.HasAddStartHandlers;
import com.butent.bee.client.view.add.HasReadyForInsertHandlers;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.edit.HasReadyForUpdateHandlers;
import com.butent.bee.client.view.edit.HasSaveChangesHandlers;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.shared.HasState;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Contains necessary methods for form implementing classes.
 */

public interface FormView extends DataView, HasDataTable, ActiveWidgetChangeEvent.Handler,
    HasAddStartHandlers, HasAddEndHandlers, HasReadyForInsertHandlers, HasReadyForUpdateHandlers,
    HasDimensions, HasState, DndWidget, EditEndEvent.Handler, RequiresResize, Printable,
    HasSaveChangesHandlers {

  void addDynamicStyle(String widgetId, CellSource cellSource, ConditionalStyle conditionalStyle);

  void applyOptions(String options);

  boolean checkOnClose(NativePreviewEvent event);

  boolean checkOnSave(NativePreviewEvent event);

  void create(FormDescription formDescription, String viewName, List<BeeColumn> dataColumns,
      boolean addStyle, FormInterceptor interceptor);

  void editRow(IsRow rowValue, Scheduler.ScheduledCommand focusCommand);

  boolean focus(String source);

  int flush();

  long getActiveRowId();

  Collection<RowChildren> getChildrenForInsert();

  Collection<RowChildren> getChildrenForUpdate();

  DateTime getDateTimeValue(String source);

  HasDataTable getDisplay();

  Double getDoubleValue(String source);

  List<EditableWidget> getEditableWidgets();

  FormInterceptor getFormInterceptor();

  String getFormName();

  Integer getIntegerValue(String source);

  JustDate getDateValue(String source);

  Long getLongValue(String source);

  Map<String, Widget> getNamedWidgets();

  IsRow getOldRow();

  IdentifiableWidget getRootWidget();

  JavaScriptObject getRowJso();

  String getStringValue(String source);

  Widget getWidgetByName(String name);

  Widget getWidgetBySource(String source);

  boolean isInteractive();

  boolean isRowEditable(IsRow rowValue, boolean warn);

  boolean isRowEnabled(IsRow rowValue);

  void observeData();

  boolean observesData();

  void onClose(CloseCallback closeCallback);

  void prepareForInsert();

  boolean printFooter();

  boolean printHeader();

  void refreshChildWidgets(IsRow row);

  void setCaption(String caption);

  void start(Integer rowCount);

  void updateCell(String columnId, String newValue);

  void updateRow(IsRow row, boolean refreshChildren);

  boolean validate(NotificationListener notificationListener, boolean focusOnError);
}