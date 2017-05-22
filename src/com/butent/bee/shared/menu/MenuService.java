package com.butent.bee.shared.menu;

import com.google.common.collect.Sets;

import static com.butent.bee.shared.modules.cars.CarsConstants.TBL_SERVICE_EVENTS;

import com.butent.bee.shared.data.DataNameProvider;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.finance.FinanceConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.rights.RightsState;

import java.util.EnumSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlEnumValue;

public enum MenuService {

  @XmlEnumValue("form")
  FORM(RightsState.VIEW),
  @XmlEnumValue("grid")
  GRID(RightsState.VIEW),
  @XmlEnumValue("new")
  NEW(EnumSet.of(RightsState.VIEW, RightsState.EDIT, RightsState.CREATE), true),
  @XmlEnumValue("parameters")
  PARAMETERS(RightsState.VIEW),
  @XmlEnumValue("report")
  REPORT(RightsState.VIEW),
  @XmlEnumValue("stages")
  STAGES(EnumSet.of(RightsState.VIEW, RightsState.EDIT, RightsState.CREATE), true),

  @XmlEnumValue("items")
  ITEMS(RightsState.VIEW, ClassifierConstants.VIEW_ITEMS),
  @XmlEnumValue("update_exchange_rates")
  UPDATE_EXCHANGE_RATES(EnumSet.of(RightsState.VIEW, RightsState.CREATE),
      AdministrationConstants.VIEW_CURRENCY_RATES),

  @XmlEnumValue("calendar_reports")
  CALENDAR_REPORTS(RightsState.VIEW,
      Sets.newHashSet(CalendarConstants.VIEW_REPORT_OPTIONS, CalendarConstants.VIEW_APPOINTMENTS)),

  @XmlEnumValue("task_list")
  TASK_LIST(RightsState.VIEW, TaskConstants.VIEW_TASKS),

  @XmlEnumValue("discuss_list")
  DISCUSS_LIST(RightsState.VIEW, DiscussionsConstants.VIEW_DISCUSSIONS),

  @XmlEnumValue("documents")
  DOCUMENTS(RightsState.VIEW, DocumentConstants.VIEW_DOCUMENTS),

  @XmlEnumValue("open_mail")
  OPEN_MAIL(EnumSet.noneOf(RightsState.class)),

  @XmlEnumValue("ensure_categories_and_open_grid")
  ENSURE_CATEGORIES_AND_OPEN_GRID(RightsState.VIEW, true),
  @XmlEnumValue("edit_terms_of_delivery")
  EDIT_TERMS_OF_DELIVERY(EnumSet.of(RightsState.VIEW, RightsState.EDIT),
      EcConstants.VIEW_CONFIGURATION),
  @XmlEnumValue("edit_ec_contacts")
  EDIT_EC_CONTACTS(EnumSet.of(RightsState.VIEW, RightsState.EDIT),
      EcConstants.VIEW_CONFIGURATION),

  @XmlEnumValue("freight_exchange")
  FREIGHT_EXCHANGE(RightsState.VIEW,
      Sets.newHashSet(TransportConstants.VIEW_ORDERS, TransportConstants.VIEW_ORDER_CARGO)),
  @XmlEnumValue("shipping_schedule")
  SHIPPING_SCHEDULE(RightsState.VIEW,
      Sets.newHashSet(TransportConstants.VIEW_VEHICLES, TransportConstants.VIEW_TRIPS)),
  @XmlEnumValue("driver_time_board")
  DRIVER_TIME_BOARD(RightsState.VIEW,
      Sets.newHashSet(TransportConstants.VIEW_DRIVERS, TransportConstants.VIEW_TRIPS)),
  @XmlEnumValue("truck_time_board")
  TRUCK_TIME_BOARD(RightsState.VIEW,
      Sets.newHashSet(TransportConstants.VIEW_VEHICLES, TransportConstants.VIEW_TRIPS)),
  @XmlEnumValue("trailer_time_board")
  TRAILER_TIME_BOARD(RightsState.VIEW,
      Sets.newHashSet(TransportConstants.VIEW_VEHICLES, TransportConstants.VIEW_TRIPS)),

  @XmlEnumValue("assessments_grid")
  ASSESSMENTS_GRID(RightsState.VIEW, TransportConstants.TBL_ASSESSMENTS),

  @XmlEnumValue("service_calendar")
  SERVICE_CALENDAR(RightsState.VIEW,
      Sets.newHashSet(ServiceConstants.VIEW_SERVICE_OBJECTS, TaskConstants.VIEW_TASKS)),

  @XmlEnumValue("car_service_calendar")
  CAR_SERVICE_CALENDAR(RightsState.VIEW, TBL_SERVICE_EVENTS),

  @XmlEnumValue("trade_act_new")
  TRADE_ACT_NEW(RightsState.CREATE, TradeActConstants.VIEW_TRADE_ACTS),
  @XmlEnumValue("trade_act_list")
  TRADE_ACT_LIST(RightsState.VIEW, TradeActConstants.VIEW_TRADE_ACTS),

  @XmlEnumValue("trade_documents")
  TRADE_DOCUMENTS(RightsState.VIEW, TradeConstants.VIEW_TRADE_DOCUMENTS),
  @XmlEnumValue("rebuild_trade_stock")
  REBUILD_TRADE_STOCK(EnumSet.of(RightsState.CREATE, RightsState.EDIT, RightsState.DELETE),
      TradeConstants.VIEW_TRADE_STOCK),

  @XmlEnumValue("finance_default_accounts")
  FINANCE_DEFAULT_ACCOUNTS(RightsState.EDIT, FinanceConstants.VIEW_FINANCE_CONFIGURATION),
  @XmlEnumValue("finance_posting_precedence")
  FINANCE_POSTING_PRECEDENCE(RightsState.EDIT, FinanceConstants.VIEW_FINANCE_CONFIGURATION),

  @XmlEnumValue("extra_dimensions")
  EXTRA_DIMENSIONS(RightsState.VIEW),

  @XmlEnumValue("service_maintenance_list")
  SERVICE_MAINTENANCE_LIST(RightsState.VIEW, ServiceConstants.TBL_SERVICE_MAINTENANCE);

  private final Set<RightsState> dataRightsStates;
  private DataNameProvider dataNameProvider;

  private MenuTransformer transformer;

  private MenuHandler handler;

  MenuService(RightsState dataRightsState) {
    this.dataRightsStates = EnumSet.of(dataRightsState);
  }

  MenuService(RightsState dataRightsState, String dataName) {
    this(dataRightsState);
    setDataName(dataName);
  }

  MenuService(RightsState dataRightsState, Set<String> dataNames) {
    this(dataRightsState);
    setDataNames(dataNames);
  }

  MenuService(RightsState dataRightsState, boolean dataIsParameter) {
    this(dataRightsState);
    if (dataIsParameter) {
      setDataIsParameter();
    }
  }

  MenuService(EnumSet<RightsState> dataRightsStates) {
    this.dataRightsStates = dataRightsStates;
  }

  MenuService(EnumSet<RightsState> dataRightsStates, String dataName) {
    this(dataRightsStates);
    setDataName(dataName);
  }

  MenuService(EnumSet<RightsState> dataRightsStates, boolean dataIsParameter) {
    this(dataRightsStates);
    if (dataIsParameter) {
      setDataIsParameter();
    }
  }

  public Set<RightsState> getDataRightsStates() {
    return dataRightsStates;
  }

  public MenuHandler getHandler() {
    return handler;
  }

  public Set<String> getDataNames(String parameter) {
    return dataNameProvider == null ? null : dataNameProvider.apply(parameter);
  }

  public MenuTransformer getTransformer() {
    return transformer;
  }

  public void setDataName(final String dataName) {
    setDataNames(Sets.newHashSet(dataName));
  }

  public void setDataNames(final Set<String> dataNames) {
    this.dataNameProvider = input -> dataNames;
  }

  public void setDataIsParameter() {
    this.dataNameProvider = Sets::newHashSet;
  }

  public void setDataNameProvider(DataNameProvider dataNameProvider) {
    this.dataNameProvider = dataNameProvider;
  }

  public void setHandler(MenuHandler handler) {
    this.handler = handler;
  }

  public void setTransformer(MenuTransformer transformer) {
    this.transformer = transformer;
  }
}
