package com.butent.bee.shared.news;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Feed implements HasLocalizedCaption {
  TASKS_ASSIGNED(ModuleAndSub.of(Module.TASKS), TaskConstants.TBL_TASKS,
      TaskConstants.VIEW_TASKS) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTasksAssigned();
    }
  },

  TASKS_DELEGATED(ModuleAndSub.of(Module.TASKS), TaskConstants.TBL_TASKS,
      TaskConstants.VIEW_TASKS) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTasksDelegated();
    }
  },

  TASKS_OBSERVED(ModuleAndSub.of(Module.TASKS), TaskConstants.TBL_TASKS,
      TaskConstants.VIEW_TASKS) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTasksObserved();
    }
  },

  TASKS_ALL(ModuleAndSub.of(Module.TASKS), TaskConstants.TBL_TASKS, TaskConstants.VIEW_TASKS) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTasksAll();
    }
  },

  REQUESTS_ASSIGNED(
      ModuleAndSub.of(Module.TASKS),
      TaskConstants.TBL_REQUESTS,
      TaskConstants.VIEW_REQUESTS,
      Lists.newArrayList(TaskConstants.COL_REQUEST_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedRequestsAssigned();
    }
  },

  REQUESTS_ALL(
      ModuleAndSub.of(Module.TASKS),
      TaskConstants.TBL_REQUESTS,
      TaskConstants.VIEW_REQUESTS,
      Lists.newArrayList(TaskConstants.COL_REQUEST_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedRequestsAll();
    }
  },

  COMPANIES_MY(ModuleAndSub.of(Module.CLASSIFIERS, SubModule.CONTACTS),
      TBL_COMPANY_USERS, VIEW_COMPANIES,
      COL_COMPANY_NAME) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedCompaniesMy();
    }
  },

  COMPANIES_ALL(ModuleAndSub.of(Module.CLASSIFIERS, SubModule.CONTACTS),
      TBL_COMPANIES, VIEW_COMPANIES,
      COL_COMPANY_NAME) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedCompaniesAll();
    }
  },

  PERSONS(ModuleAndSub.of(Module.CLASSIFIERS, SubModule.CONTACTS), TBL_PERSONS,
      VIEW_PERSONS,
      Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedPersons();
    }
  },

  GOODS(ModuleAndSub.of(Module.TRADE), TBL_ITEMS, VIEW_ITEMS,
      Lists.newArrayList(COL_ITEM_NAME),
      Lists.newArrayList(COL_ITEM_ARTICLE)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedGoods();
    }
  },

  DOCUMENTS(ModuleAndSub.of(Module.DOCUMENTS), DocumentConstants.TBL_DOCUMENTS,
      DocumentConstants.VIEW_DOCUMENTS, DocumentConstants.COL_DOCUMENT_NAME) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedDocuments();
    }
  },

  APPOINTMENTS_MY(ModuleAndSub.of(Module.CALENDAR), CalendarConstants.TBL_APPOINTMENT_ATTENDEES,
      CalendarConstants.VIEW_APPOINTMENTS) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedAppointmentsMy();
    }
  },

  APPOINTMENTS_ALL(ModuleAndSub.of(Module.CALENDAR), CalendarConstants.TBL_APPOINTMENTS,
      CalendarConstants.VIEW_APPOINTMENTS) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedAppointmentsAll();
    }
  },

  EC_CLIENTS_MY(ModuleAndSub.of(Module.ECOMMERCE), EcConstants.TBL_CLIENTS,
      EcConstants.VIEW_CLIENTS, Lists.newArrayList(ALS_COMPANY_NAME),
      Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedEcClientsMy();
    }
  },

  EC_CLIENTS_ALL(ModuleAndSub.of(Module.ECOMMERCE), EcConstants.TBL_CLIENTS,
      EcConstants.VIEW_CLIENTS, Lists.newArrayList(ALS_COMPANY_NAME),
      Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedEcClientsAll();
    }
  },

  EC_ORDERS_MY(ModuleAndSub.of(Module.ECOMMERCE), EcConstants.TBL_ORDERS, EcConstants.VIEW_ORDERS,
      Lists.newArrayList(EcConstants.ALS_ORDER_CLIENT_COMPANY_NAME),
      Lists.newArrayList(EcConstants.COL_ORDER_DATE, EcConstants.COL_ORDER_STATUS)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedEcOrdersMy();
    }
  },

  EC_ORDERS_ALL(ModuleAndSub.of(Module.ECOMMERCE), EcConstants.TBL_ORDERS, EcConstants.VIEW_ORDERS,
      Lists.newArrayList(EcConstants.ALS_ORDER_CLIENT_COMPANY_NAME),
      Lists.newArrayList(EcConstants.COL_ORDER_DATE, EcConstants.COL_ORDER_STATUS)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedEcOrdersAll();
    }
  },

  EC_REGISTRATIONS(ModuleAndSub.of(Module.ECOMMERCE), EcConstants.TBL_REGISTRATIONS,
      EcConstants.VIEW_REGISTRATIONS, Lists.newArrayList(EcConstants.COL_REGISTRATION_FIRST_NAME,
      EcConstants.COL_REGISTRATION_LAST_NAME),
      Lists.newArrayList(EcConstants.COL_REGISTRATION_COMPANY_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedEcRegistrations();
    }
  },

  ORDER_CARGO(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_ORDER_CARGO,
      TransportConstants.VIEW_ORDER_CARGO,
      Lists.newArrayList(TransportConstants.COL_CARGO_DESCRIPTION)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrCargo();
    }
  },

  TRANSPORTATION_ORDERS_MY(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_ORDERS,
      TransportConstants.VIEW_ORDERS,
      Lists.newArrayList(TransportConstants.COL_ORDER_DATE, TransportConstants.COL_ORDER_NO),
      Lists.newArrayList(TransportConstants.COL_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrOrdersMy();
    }
  },

  TRANSPORTATION_ORDERS_ALL(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_ORDERS,
      TransportConstants.VIEW_ORDERS,
      Lists.newArrayList(TransportConstants.COL_ORDER_DATE, TransportConstants.COL_ORDER_NO),
      Lists.newArrayList(TransportConstants.COL_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrOrdersAll();
    }
  },

  TRIPS_MY(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_TRIPS,
      TransportConstants.VIEW_TRIPS,
      Lists.newArrayList(TransportConstants.COL_TRIP_DATE, TransportConstants.COL_TRIP_NO,
          TransportConstants.ALS_VEHICLE_NUMBER)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrTripsMy();
    }
  },

  TRIPS(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_TRIPS,
      TransportConstants.VIEW_TRIPS,
      Lists.newArrayList(TransportConstants.COL_TRIP_DATE, TransportConstants.COL_TRIP_NO,
          TransportConstants.ALS_VEHICLE_NUMBER)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrTripsAll();
    }
  },

  SHIPMENT_REQUESTS_UNREGISTERED_MY(ModuleAndSub.of(Module.TRANSPORT, SubModule.SELFSERVICE),
      TransportConstants.TBL_SHIPMENT_REQUESTS, TransportConstants.VIEW_SHIPMENT_REQUESTS,
      Arrays.asList(TransportConstants.COL_QUERY_STATUS, TransportConstants.COL_QUERY_DATE,
          TransportConstants.COL_QUERY_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrRequestsUnregisteredMy();
    }
  },

  SHIPMENT_REQUESTS_UNREGISTERED_ALL(ModuleAndSub.of(Module.TRANSPORT, SubModule.SELFSERVICE),
      TransportConstants.TBL_SHIPMENT_REQUESTS, TransportConstants.VIEW_SHIPMENT_REQUESTS,
      Arrays.asList(TransportConstants.COL_QUERY_STATUS, TransportConstants.COL_QUERY_DATE,
          TransportConstants.COL_QUERY_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrRequestsUnregisteredAll();
    }
  },

  SHIPMENT_REQUESTS_MY(ModuleAndSub.of(Module.TRANSPORT, SubModule.SELFSERVICE),
      TransportConstants.TBL_SHIPMENT_REQUESTS, TransportConstants.VIEW_SHIPMENT_REQUESTS,
      Arrays.asList(TransportConstants.COL_QUERY_STATUS, TransportConstants.COL_QUERY_DATE,
          ALS_COMPANY_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrRequestsMy();
    }
  },

  SHIPMENT_REQUESTS_ALL(ModuleAndSub.of(Module.TRANSPORT, SubModule.SELFSERVICE),
      TransportConstants.TBL_SHIPMENT_REQUESTS, TransportConstants.VIEW_SHIPMENT_REQUESTS,
      Arrays.asList(TransportConstants.COL_QUERY_STATUS, TransportConstants.COL_QUERY_DATE,
          ALS_COMPANY_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrRequestsAll();
    }
  },

  VEHICLES(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_VEHICLES,
      TransportConstants.VIEW_VEHICLES,
      Lists.newArrayList(TransportConstants.COL_VEHICLE_NUMBER, TransportConstants.COL_TYPE_NAME),
      Lists.newArrayList(COL_VEHICLE_BRAND_NAME, TransportConstants.COL_MODEL_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrVehicles();
    }
  },

  DRIVERS(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_DRIVERS,
      TransportConstants.VIEW_DRIVERS,
      Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrDrivers();
    }
  },

  ASSESSMENT_REQUESTS_ALL(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS),
      TransportConstants.TBL_ASSESSMENTS, TransportConstants.VIEW_ASSESSMENTS,
      Lists.newArrayList(TransportConstants.COL_ASSESSMENT, DataUtils.ID_TAG,
          TransportConstants.COL_STATUS,
          TransportConstants.ALS_ORDER_NOTES, TransportConstants.ALS_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrAssessmentAllRequests();
    }

  },

  ASSESSMENT_REQUESTS_MY(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS),
      TransportConstants.TBL_ASSESSMENTS, TransportConstants.VIEW_ASSESSMENTS,
      Lists.newArrayList(TransportConstants.COL_ASSESSMENT, DataUtils.ID_TAG,
          TransportConstants.COL_STATUS,
          TransportConstants.ALS_ORDER_NOTES, TransportConstants.ALS_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrAssessmentMyRequests();
    }

  },

  ASSESSMENT_ORDERS_ALL(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS),
      TransportConstants.TBL_ASSESSMENTS, TransportConstants.VIEW_ASSESSMENTS,
      Lists.newArrayList(TransportConstants.COL_ASSESSMENT, DataUtils.ID_TAG,
          TransportConstants.ALS_ORDER_NOTES,
          TransportConstants.COL_STATUS, TransportConstants.ALS_CUSTOMER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrAssessmentAllOrders();
    }

  },

  ASSESSMENT_ORDERS_MY(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS),
      TransportConstants.TBL_ASSESSMENTS, TransportConstants.VIEW_ASSESSMENTS,
      Lists.newArrayList(TransportConstants.COL_ASSESSMENT, DataUtils.ID_TAG,
          TransportConstants.ALS_ORDER_NOTES, TransportConstants.ALS_CUSTOMER_NAME,
          TransportConstants.COL_STATUS)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrAssessmentMyOrders();
    }

  },

  ASSESSMENT_TRANSPORTATIONS(ModuleAndSub.of(Module.TRANSPORT, SubModule.LOGISTICS),
      TransportConstants.TBL_TRIPS,
      TransportConstants.VIEW_ASSESSMENT_TRANSPORTATIONS,
      Lists.newArrayList(DataUtils.ID_TAG, TransportConstants.COL_TRIP_DATE,
          TransportConstants.ALS_FORWARDER_NAME, TransportConstants.ALS_EXPEDITION_TYPE)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrAssessmentTransportations();
    }

  },

  CARGO_SALES(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_CARGO_INCOMES,
      TransportConstants.VIEW_CARGO_SALES, Lists.newArrayList(TransportConstants.ALS_ORDER_DATE,
      TransportConstants.COL_ORDER_NO, TransportConstants.ALS_PAYER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrOrderCargoSales();
    }

  },

  CARGO_CREDIT_SALES(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_CARGO_INCOMES,
      TransportConstants.VIEW_CARGO_CREDIT_SALES, Lists.newArrayList(
      TransportConstants.ALS_ORDER_DATE, TransportConstants.COL_ORDER_NO,
      TransportConstants.ALS_PAYER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrOrderCargoCreditSales();
    }

  },

  CARGO_PURCHASES(ModuleAndSub.of(Module.TRANSPORT), TransportConstants.TBL_CARGO_EXPENSES,
      TransportConstants.VIEW_CARGO_PURCHASES, Lists.newArrayList(
      TransportConstants.ALS_ORDER_DATE, TransportConstants.COL_ORDER_NO,
      TransportConstants.ALS_SERVICE_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {

      return constants.feedTrTripCosts();
    }
  },

  CARGO_INVOICES(ModuleAndSub.of(Module.TRANSPORT), TradeConstants.TBL_SALES,
      TransportConstants.VIEW_CARGO_INVOICES, Lists.newArrayList(TransportConstants.COL_DATE,
      TransportConstants.COL_NUMBER, TransportConstants.ALS_PAYER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrOrderCargoInvoices();
    }
  },

  CARGO_PROFORMA_INVOICES(ModuleAndSub.of(Module.TRANSPORT), TradeConstants.TBL_SALES,
      TransportConstants.VIEW_CARGO_INVOICES, Lists.newArrayList(TransportConstants.COL_DATE,
      TransportConstants.COL_NUMBER, TransportConstants.ALS_PAYER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrCargoProformaInvoices();
    }
  },

  CARGO_CREDIT_INVOICES(ModuleAndSub.of(Module.TRANSPORT), TradeConstants.TBL_PURCHASES,
      TransportConstants.VIEW_CARGO_PURCHASE_INVOICES, Lists.newArrayList(
      TransportConstants.COL_DATE, TransportConstants.COL_NUMBER,
      TradeConstants.ALS_SUPPLIER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrCargoCreditInvoices();
    }
  },

  CARGO_PURCHASE_INVOICES(ModuleAndSub.of(Module.TRANSPORT), TradeConstants.TBL_PURCHASES,
      TransportConstants.VIEW_CARGO_PURCHASE_INVOICES, Lists.newArrayList(
      TransportConstants.COL_DATE, TransportConstants.COL_NUMBER,
      TradeConstants.ALS_SUPPLIER_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.feedTrCargoPurchaseInvoices();
    }
  },

  DISCUSSIONS(ModuleAndSub.of(Module.DISCUSSIONS), DiscussionsConstants.TBL_DISCUSSIONS,
      DiscussionsConstants.VIEW_DISCUSSIONS, Lists.newArrayList(DiscussionsConstants.COL_SUBJECT)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.discussions();
    }
  },

  ANNOUNCEMENTS(ModuleAndSub.of(Module.DISCUSSIONS), DiscussionsConstants.TBL_DISCUSSIONS,
      DiscussionsConstants.VIEW_DISCUSSIONS, Lists.newArrayList(DiscussionsConstants.COL_SUBJECT)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.announcements();
    }
  },

  MAIL(ModuleAndSub.of(Module.MAIL), MailConstants.TBL_PLACES, MailConstants.TBL_PLACES,
      Lists.newArrayList(MailConstants.COL_DATE, "SenderEmail", MailConstants.COL_SUBJECT)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.mail();
    }
  },

  PROJECT(ModuleAndSub.of(Module.PROJECTS), ProjectConstants.TBL_PROJECTS,
      ProjectConstants.VIEW_PROJECTS,
      Lists.newArrayList(ProjectConstants.COL_PROJECT_NAME)) {
    @Override
    public String getCaption(Dictionary constants) {
      return constants.projects();
    }

  };

  public static final Collection<Feed> ALL = EnumSet.allOf(Feed.class);

  private static final String SEPARATOR = BeeConst.STRING_COMMA;
  private static final Splitter splitter = Splitter.on(SEPARATOR).omitEmptyStrings().trimResults();

  public static String join(Collection<Feed> feeds) {
    if (BeeUtils.isEmpty(feeds)) {
      return BeeConst.STRING_EMPTY;
    }

    Set<Integer> ordinals = new HashSet<>();
    for (Feed feed : feeds) {
      if (feed != null) {
        ordinals.add(feed.ordinal());
      }
    }

    return BeeUtils.join(SEPARATOR, ordinals);
  }

  public static List<Feed> split(String input) {
    List<Feed> feeds = new ArrayList<>();
    if (BeeUtils.isEmpty(input)) {
      return feeds;
    }

    for (String s : splitter.split(input)) {
      Feed feed = EnumUtils.getEnumByIndex(Feed.class, s);
      if (feed != null) {
        feeds.add(feed);
      }
    }
    return feeds;
  }

  private final ModuleAndSub moduleAndSub;
  private final String table;

  private final String headlineView;

  private final List<String> labelColumns;
  private final List<String> titleColumns;

  Feed(ModuleAndSub moduleAndSub, String table, String headlineView) {
    this(moduleAndSub, table, headlineView, BeeConst.EMPTY_IMMUTABLE_STRING_LIST,
        BeeConst.EMPTY_IMMUTABLE_STRING_LIST);
  }

  Feed(ModuleAndSub moduleAndSub, String table, String headlineView, String labelColumn) {
    this(moduleAndSub, table, headlineView, Lists.newArrayList(labelColumn),
        BeeConst.EMPTY_IMMUTABLE_STRING_LIST);
  }

  Feed(ModuleAndSub moduleAndSub, String table, String headlineView, List<String> labelColumns) {
    this(moduleAndSub, table, headlineView, labelColumns, BeeConst.EMPTY_IMMUTABLE_STRING_LIST);
  }

  Feed(ModuleAndSub moduleAndSub, String table, String headlineView,
      List<String> labelColumns, List<String> titleColumns) {

    this.moduleAndSub = moduleAndSub;
    this.table = table;

    this.headlineView = headlineView;

    this.labelColumns = labelColumns;
    this.titleColumns = titleColumns;
  }

  public String getHeadlineView() {
    return headlineView;
  }

  public List<String> getLabelColumns() {
    return labelColumns;
  }

  public ModuleAndSub getModuleAndSub() {
    return moduleAndSub;
  }

  public String getTable() {
    return table;
  }

  public List<String> getTitleColumns() {
    return titleColumns;
  }

  public String getUsageTable() {
    return (table == null) ? null : NewsConstants.getUsageTable(table);
  }

  public boolean in(Feed... feeds) {
    if (feeds != null) {
      for (Feed feed : feeds) {
        if (feed == this) {
          return true;
        }
      }
    }
    return false;
  }
}
