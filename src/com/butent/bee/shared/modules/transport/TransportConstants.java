package com.butent.bee.shared.modules.transport;

import com.google.common.collect.Lists;

import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.ui.HasSortingOrder;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public final class TransportConstants {

  public enum AssessmentStatus implements HasLocalizedCaption {
    NEW {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trAssessmentStatusNew();
      }
    },
    ANSWERED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trAssessmentStatusAnswered();
      }
    },
    LOST {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trAssessmentStatusLost();
      }
    },
    APPROVED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trAssessmentStatusApproved();
      }
    };

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public enum ShipmentRequestStatus implements HasLocalizedCaption {
    NEW {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusNew();
      }
    },
    ANSWERED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusAnswered();
      }
    },
    CONTRACT_SENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusContractSent();
      }
    },
    APPROVED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusApproved();
      }
    },
    REJECTED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusRejected();
      }
    },
    CONFIRMED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusConfirmed();
      }
    },
    LOST {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestStatusRejected();
      }
    };

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public enum TextConstant implements HasLocalizedCaption {
    CONTRACT_MAIL_CONTENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trContractMailContent();
      }

      @Override
      public String getDefaultContent() {
        return "Follow the link below to confirm or reject the agreement"
            + " which is attached to this letter<br><br>"
            + "http://127.0.0.1:8080/Bee/{CONTRACT_PATH}<br><br><br>"
            + "This message was created automatically by mail delivery software. "
            + "Thank You for using our services.";
      }
    },
    REGISTRATION_MAIL_CONTENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRegistrationMailContent();
      }

      @Override
      public String getDefaultContent() {
        return "The login which is given below is to connect to Your user account<br><br>"
            + "Address: http://127.0.0.1:8080/Bee<br>"
            + "Login: {LOGIN}<br>"
            + "Password: {PASSWORD}<br><br>"
            + "This message was created automatically by mail delivery software. "
            + "Thank You for using our services.";
      }
    },
    REQUEST_CONFIRMED_MAIL_CONTENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestConfirmedMailContent();
      }

      @Override
      public String getDefaultContent() {
        return "Your order (id:{CONTRACT_ID}) is in progress!<br><br><br>"
            + "This message was created automatically by mail delivery software. "
            + "Thank You for using our services.";
      }
    },
    REQUEST_LOST_MAIL_CONTENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestLostMailContent();
      }

      @Override
      public String getDefaultContent() {
        return "Your inquiry (id:{CONTRACT_ID}) is denied.<br><br><br>"
            + "This message was created automatically by mail delivery software. "
            + "Thank You for using our services.";
      }
    },
    SUMBMITTED_REQUEST_CONTENT {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestSubmittedContent();
      }

      @Override
      public String getDefaultContent() {
        return "Your request has been received. Will contact You soon!";
      }
    },
    REQUEST_COMMON_TERMS {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trRequestCommonTerms();
      }

      @Override
      public String getDefaultContent() {
        return "";
      }
    };

    public String getDefaultContent() {
      return null;
    }
  }

  public enum FuelSeason implements HasLocalizedCaption {
    SUMMER {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.summer();
      }
    },
    WINTER {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.winter();
      }
    };
  }

  public enum OrderStatus implements HasLocalizedCaption {
    REQUEST {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trOrderStatusRequest();
      }
    },
    ACTIVE {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trOrderStatusActive();
      }
    },
    CANCELED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trOrderStatusCanceled();
      }
    },
    COMPLETED {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trOrderStatusCompleted();
      }
    };

    public boolean is(Integer status) {
      return status != null && ordinal() == status;
    }
  }

  public enum TripStatus implements HasCaption, HasSortingOrder {
    NEW(Localized.dictionary().trTripStatusNew(), true, 0),
    ACTIVE(Localized.dictionary().trTripStatusActive(), true, 1),
    CANCELED(Localized.dictionary().trTripStatusCanceled(), false, 2),
    COMPLETED(Localized.dictionary().trTripStatusCompleted(), false, 4),
    ARRANGED(Localized.dictionary().trTripStatusArranged(), true, 3);

    private final String caption;
    private final boolean editable;
    private final int sortingOrder;

    TripStatus(String caption, boolean editable, int sortingOrder) {
      this.caption = caption;
      this.editable = editable;
      this.sortingOrder = sortingOrder;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    @Override
    public int getSortingOrder() {
      return sortingOrder;
    }

    public boolean isEditable() {
      return editable;
    }
  }

  public enum TripConstant implements HasLocalizedCaption {
    AVERAGE_KM_COST {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trAverageKilometerCost();
      }
    },

    AVERAGE_FUEL_COST {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trAverageFuelCost();
      }
    },

    CONSTANT_COSTS {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trConstantCosts();
      }
    },

    ECONOMY_BONUS {
      @Override
      public String getCaption(Dictionary constants) {
        return constants.trEconomyBonus();
      }
    };
  }

  public enum VehicleType {
    TRUCK {
      @Override
      public String getTripVehicleIdColumnName() {
        return COL_VEHICLE;
      }

      @Override
      public String getTripVehicleNumberColumnName() {
        return COL_VEHICLE + COL_VEHICLE_NUMBER;
      }
    },
    TRAILER {
      @Override
      public String getTripVehicleIdColumnName() {
        return COL_TRAILER;
      }

      @Override
      public String getTripVehicleNumberColumnName() {
        return COL_TRAILER + COL_VEHICLE_NUMBER;
      }
    };

    public abstract String getTripVehicleIdColumnName();

    public abstract String getTripVehicleNumberColumnName();
  }

  public static void register() {
    EnumUtils.register(AssessmentStatus.class);
    EnumUtils.register(TripStatus.class);
    EnumUtils.register(OrderStatus.class);

    EnumUtils.register(ShipmentRequestStatus.class);

    EnumUtils.register(FuelSeason.class);
    EnumUtils.register(TripConstant.class);
    EnumUtils.register(TextConstant.class);
  }

  public static final String SVC_GET_BEFORE = "GetBeforeData";
  public static final String SVC_GET_UNASSIGNED_CARGOS = "GetUnassignedCargos";
  public static final String SVC_GET_ROUTE = "GetTripRoute";
  public static final String SVC_GENERATE_ROUTE = "GenerateTripRoute";
  public static final String SVC_GENERATE_DAILY_COSTS = "GenerateDailyCosts";
  public static final String SVC_GET_FX_DATA = "GetFxData";
  public static final String SVC_GET_SS_DATA = "GetSsData";
  public static final String SVC_GET_DTB_DATA = "GetDtbData";
  public static final String SVC_GET_TRUCK_TB_DATA = "GetTruckTbData";
  public static final String SVC_GET_TRAILER_TB_DATA = "GetTrailerTbData";
  public static final String SVC_GET_COLORS = "GetColors";
  public static final String SVC_GET_CARGO_USAGE = "GetCargoUsage";
  public static final String SVC_GET_CARGO_PLACES = "GetCargoPlaces";
  public static final String SVC_GET_ASSESSMENT_QUANTITY_REPORT = "GetAssessmentQuantityReport";
  public static final String SVC_GET_ASSESSMENT_TURNOVER_REPORT = "GetAssessmentTurnoverReport";
  public static final String SVC_UPDATE_PERCENT = "UpdatePercent";
  public static final String SVC_CREATE_INVOICE_ITEMS = "CreateInvoiceItems";
  public static final String SVC_GET_CREDIT_INFO = "GetCreditInfo";
  public static final String SVC_GET_TRIP_INFO = "GetTripInfo";
  public static final String SVC_GET_VEHICLE_BUSY_DATES = "GetVehicleBusyDates";
  public static final String SVC_GET_DRIVER_BUSY_DATES = "GetDriverBusyDates";
  public static final String SVC_GET_TEXT_CONSTANT = "GetTextConstant";

  public static final String SVC_SEND_MESSAGE = "SendMessage";

  public static final String SVC_TRIP_PROFIT_REPORT = "TripProfitReport";
  public static final String SVC_FUEL_USAGE_REPORT = "FuelUsageReport";

  public static final String PRM_MESSAGE_TEMPLATE = "MessageTemplate";
  public static final String PRM_TRIP_PREFIX = "DefaultTripPrefix";
  public static final String PRM_INVOICE_PREFIX = "DefaultInvoicePrefix";
  public static final String PRM_CARGO_TYPE = "CargoType";
  public static final String PRM_CARGO_SERVICE = "CargoService";
  public static final String PRM_EXCLUDE_VAT = "ExcludeVAT";
  public static final String PRM_SELF_SERVICE_ROLE = "SelfServiceRole";

  public static final String VAR_INCOME = "Income";
  public static final String VAR_EXPENSE = "Expense";

  public static final String VAR_LOADING = "Loading";
  public static final String VAR_UNLOADING = "Unloading";
  public static final String VAR_UNBOUND = "Unbound";

  public static final String TBL_TRANSPORT_GROUPS = "TransportGroups";

  public static final String TBL_VEHICLES = "Vehicles";
  public static final String TBL_VEHICLE_SERVICES = "VehicleServices";
  public static final String TBL_VEHICLE_TRACKING = "VehicleTracking";
  public static final String TBL_VEHICLE_GROUPS = "VehicleGroups";
  public static final String TBL_VEHICLE_SERVICE_TYPES = "ServiceTypes";
  public static final String TBL_EXPEDITION_TYPES = "ExpeditionTypes";
  public static final String TBL_SHIPPING_TERMS = "ShippingTerms";
  public static final String TBL_FUEL_CONSUMPTIONS = "FuelConsumptions";

  public static final String TBL_TRIPS = "Trips";
  public static final String TBL_TRIP_DRIVERS = "TripDrivers";
  public static final String TBL_DRIVER_ADVANCES = "DriverAdvances";
  public static final String TBL_TRIP_COSTS = "TripCosts";
  public static final String TBL_TRIP_FUEL_COSTS = "TripFuelCosts";
  public static final String TBL_TRIP_ROUTES = "TripRoutes";
  public static final String TBL_TRIP_FUEL_CONSUMPTIONS = "TripFuelConsumptions";
  public static final String TBL_TRIP_USAGE = "TripUsage";

  public static final String TBL_TRIP_CONSTANTS = "TripConstants";
  public static final String TBL_TRANSPORT_SETTINGS = "TransportSettings";

  public static final String TBL_ORDERS = "TransportationOrders";
  public static final String TBL_ORDER_CARGO = "OrderCargo";
  public static final String TBL_CARGO_TRIPS = "CargoTrips";
  public static final String TBL_CARGO_PLACES = "CargoPlaces";
  public static final String TBL_CARGO_INCOMES = "CargoIncomes";
  public static final String TBL_CARGO_INCOMES_USAGE = "CargoIncomesUsage";
  public static final String TBL_CARGO_EXPENSES = "CargoExpenses";
  public static final String TBL_CARGO_EXPENSES_USAGE = "CargoExpensesUsage";
  public static final String TBL_CARGO_LOADING = "CargoLoading";
  public static final String TBL_CARGO_UNLOADING = "CargoUnloading";
  public static final String TBL_CARGO_HANDLING = "CargoHandling";
  public static final String TBL_CARGO_TYPES = "CargoTypes";

  public static final String TBL_SERVICES = "Services";

  public static final String TBL_ASSESSMENTS = "Assessments";
  public static final String TBL_ASSESSMENTS_USAGE = "AssessmentsUsage";
  public static final String TBL_ASSESSMENT_FORWARDERS = "AssessmentForwarders";
  public static final String TBL_SALES_USAGE = "SalesUsage";

  public static final String TBL_DRIVERS = "Drivers";
  public static final String TBL_DRIVER_GROUPS = "DriverGroups";
  public static final String TBL_DRIVER_DAILY_COSTS = "DriverDailyCosts";
  public static final String TBL_DRIVER_ABSENCE = "DriverAbsence";
  public static final String TBL_ABSENCE_TYPES = "AbsenceTypes";

  public static final String TBL_FUEL_TYPES = "FuelTypes";

  public static final String TBL_SHIPMENT_REQUESTS = "ShipmentRequests";

  public static final String TBL_COUNTRY_NORMS = "CountryNorms";
  public static final String TBL_COUNTRY_DAILY_COSTS = "CountryDailyCosts";

  public static final String VIEW_ORDERS = "TransportationOrders";

  public static final String VIEW_ORDER_CARGO = "OrderCargo";
  public static final String VIEW_CARGO_TRIPS = "CargoTrips";

  public static final String VIEW_ALL_CARGO = "AllCargo";
  public static final String VIEW_WAITING_CARGO = "WaitingCargo";
  public static final String VIEW_CARGO_PURCHASES = "CargoPurchases";
  public static final String VIEW_CARGO_SALES = "CargoSales";
  public static final String VIEW_CARGO_CREDIT_SALES = "CargoCreditSales";
  public static final String VIEW_CARGO_INVOICES = "CargoInvoices";
  public static final String VIEW_CARGO_CREDIT_INVOICES = "CargoCreditInvoices";
  public static final String VIEW_CARGO_PURCHASE_INVOICES = "CargoPurchaseInvoices";
  public static final String VIEW_CARGO_INCOMES = "CargoIncomes";
  public static final String VIEW_CARGO_FILES = "CargoFiles";

  public static final String VIEW_CARGO_TYPES = "CargoTypes";
  public static final String VIEW_CARGO_GROUPS = "CargoGroups";

  public static final String VIEW_TRANSPORT_GROUPS = "TransportGroups";

  public static final String VIEW_TRIPS = TBL_TRIPS;
  public static final String VIEW_EXPEDITION_TRIPS = "ExpeditionTrips";
  public static final String VIEW_ACTIVE_TRIPS = "ActiveTrips";

  public static final String VIEW_TRIP_CARGO = "TripCargo";
  public static final String VIEW_TRIP_DRIVERS = "TripDrivers";
  public static final String VIEW_TRIP_COSTS = "TripCosts";
  public static final String VIEW_TRIP_FUEL_COSTS = "TripFuelCosts";
  public static final String VIEW_TRIP_PURCHASES = "TripPurchases";
  public static final String VIEW_TRIP_PURCHASE_INVOICES = "TripPurchaseInvoices";

  public static final String VIEW_VEHICLES = "Vehicles";
  public static final String VIEW_VEHICLE_SERVICES = "VehicleServices";
  public static final String VIEW_FUEL_TEMPERATURES = "FuelTemperatures";

  public static final String VIEW_SPARE_PARTS = "SpareParts";

  public static final String VIEW_TRANSPORT_SETTINGS = "TransportSettings";

  public static final String VIEW_DRIVERS = "Drivers";
  public static final String VIEW_DRIVER_ABSENCE = "DriverAbsence";
  public static final String VIEW_ABSENCE_TYPES = "AbsenceTypes";

  public static final String VIEW_ASSESSMENTS = "Assessments";
  public static final String VIEW_CHILD_ASSESSMENTS = "ChildAssessments";
  public static final String VIEW_ASSESSMENT_TRANSPORTATIONS = "AssessmentTransportations";
  public static final String VIEW_ASSESSMENT_EXECUTORS = "AssessmentExecutors";

  public static final String VIEW_SHIPMENT_REQUESTS = "ShipmentRequests";

  public static final String VIEW_TEXT_CONSTANTS = "TextConstants";

  public static final String VIEW_EXPEDITION_TYPES = "ExpeditionTypes";
  public static final String VIEW_SHIPPING_TERMS = "ShippingTerms";

  public static final String COL_GROUP = "Group";
  public static final String COL_GROUP_NAME = "Name";
  public static final String COL_GROUP_MANAGER = "Manager";

  public static final String COL_TRIP = "Trip";
  public static final String COL_TRIP_ID = "TripID";
  public static final String COL_TRIP_NO = "TripNo";
  public static final String COL_TRIP_DATE = "Date";
  public static final String COL_TRIP_DATE_FROM = "DateFrom";
  public static final String COL_TRIP_DATE_TO = "DateTo";
  public static final String COL_TRIP_PLANNED_END_DATE = "PlannedEndDate";
  public static final String COL_TRIP_PERCENT = "TripPercent";
  public static final String COL_TRIP_ROUTE = "Route";
  public static final String COL_TRIP_NOTES = "Notes";
  public static final String COL_TRIP_STATUS = "Status";
  public static final String COL_TRIP_MANAGER = "Manager";

  public static final String COL_SPEEDOMETER = "Speedometer";
  public static final String COL_SPEEDOMETER_BEFORE = "SpeedometerBefore";
  public static final String COL_SPEEDOMETER_AFTER = "SpeedometerAfter";
  public static final String COL_FUEL_BEFORE = "FuelBefore";
  public static final String COL_FUEL_AFTER = "FuelAfter";

  public static final String COL_TRIP_CONSTANT = "Constant";

  public static final String COL_EXPEDITION = "Expedition";
  public static final String COL_FORWARDER = "Forwarder";
  public static final String COL_FORWARDER_VEHICLE = "ForwarderVehicle";
  public static final String COL_FORWARDER_DRIVER = "ForwarderDriver";

  public static final String COL_CARGO = "Cargo";
  public static final String COL_CARGO_DESCRIPTION = "Description";
  public static final String COL_CARGO_ID = "CargoID";
  public static final String COL_CARGO_INCOME = "Income";
  public static final String COL_CARGO_PERCENT = "CargoPercent";
  public static final String COL_CARGO_MESSAGE = "Message";
  public static final String COL_CARGO_TRIP = "CargoTrip";
  public static final String COL_CARGO_TRIP_ID = "CargoTripID";
  public static final String COL_CARGO_CMR = "Cmr";
  public static final String COL_CARGO_NOTES = "Notes";
  public static final String COL_SHIPPING_TERM = "ShippingTerm";
  public static final String COL_CARGO_QUANTITY = "Quantity";
  public static final String COL_CARGO_WEIGHT = "Weight";
  public static final String COL_CARGO_VOLUME = "Volume";
  public static final String COL_CARGO_LDM = "LDM";
  public static final String COL_CARGO_LENGTH = "Length";
  public static final String COL_CARGO_WIDTH = "Width";
  public static final String COL_CARGO_HEIGHT = "Height";
  public static final String COL_CARGO_OUTSIZED = "Outsized";
  public static final String COL_CARGO_PARTIAL = "Partial";
  public static final String COL_CARGO_PALETTES = "Palettes";
  public static final String COL_CARGO_VALUE = "Value";
  public static final String COL_CARGO_VALUE_CURRENCY = "ValueCurrency";
  public static final String COL_CARGO_TYPE = "CargoType";

  public static final String COL_DAILY_COSTS_ITEM = "DailyCostsItem";
  public static final String COL_ROAD_COSTS_ITEM = "RoadCostsItem";
  public static final String COL_COUNTRY_NORM = "CountryNorm";

  public static final String COL_ASSESSMENT = "Assessment";
  public static final String COL_ASSESSMENT_ID = "AssessmentID";
  public static final String COL_ASSESSMENT_STATUS = "Status";
  public static final String COL_ASSESSMENT_NOTES = "Notes";
  public static final String COL_ASSESSMENT_LOG = "Log";
  public static final String COL_ASSESSMENT_EXPENSES = "ExpensesRegistered";

  public static final String COL_STATUS = "Status";
  public static final String COL_OWNER = "Owner";
  public static final String COL_OWNER_NAME = "OwnerName";
  public static final String COL_MODEL = "Model";
  public static final String COL_MODEL_NAME = "ModelName";
  public static final String COL_PRODUCTION_DATE = "ProductionDate";
  public static final String COL_NUMBER = "Number";
  public static final String COL_TYPE_NAME = "TypeName";
  public static final String COL_EXPORTED = "Exported";

  public static final String COL_COSTS_DATE = "Date";
  public static final String COL_COSTS_ITEM = "Item";
  public static final String COL_COSTS_QUANTITY = "Quantity";
  public static final String COL_COSTS_PRICE = "Price";
  public static final String COL_COSTS_CURRENCY = "Currency";
  public static final String COL_COSTS_VAT = "Vat";
  public static final String COL_COSTS_COUNTRY = "Country";
  public static final String COL_COSTS_SUPPLIER = "Supplier";
  public static final String COL_COSTS_NOTE = "Note";
  public static final String COL_COSTS_EXTERNAL_ID = "ExternalID";

  public static final String COL_ORDER = "Order";
  public static final String COL_ORDER_ID = "OrderID";
  public static final String COL_ORDER_NO = "OrderNo";
  public static final String COL_ORDER_NOTES = "Notes";
  public static final String COL_ORDER_DATE = "Date";
  public static final String COL_ORDER_MANAGER = "Manager";
  public static final String COL_CUSTOMER = "Customer";
  public static final String COL_CUSTOMER_NAME = "CustomerName";
  public static final String COL_PAYER = "Payer";
  public static final String COL_PAYER_NAME = "PayerName";

  public static final String COL_SERVICE = "Service";
  public static final String COL_SERVICE_NAME = "Name";
  public static final String COL_TRANSPORTATION = "Transportation";
  public static final String COL_DATE = "Date";
  public static final String COL_AMOUNT = "Amount";
  public static final String COL_NOTE = "Note";

  public static final String COL_ROUTE_CARGO = "TripCargo";
  public static final String COL_ROUTE_DEPARTURE_DATE = "DepartureDate";
  public static final String COL_ROUTE_DEPARTURE_COUNTRY = "DepartureCountry";
  public static final String COL_ROUTE_DEPARTURE_CITY = "DepartureCity";
  public static final String COL_ROUTE_ARRIVAL_DATE = "ArrivalDate";
  public static final String COL_ROUTE_ARRIVAL_COUNTRY = "ArrivalCountry";
  public static final String COL_ROUTE_ARRIVAL_CITY = "ArrivalCity";
  public static final String COL_ROUTE_KILOMETERS = "Kilometers";
  public static final String COL_ROUTE_WEIGHT = "CargoWeight";
  public static final String COL_ROUTE_SEASON = "Season";
  public static final String COL_ROUTE_CONSUMPTION = "Consumption";

  public static final String COL_LOADED_KILOMETERS = "LoadedKilometers";
  public static final String COL_EMPTY_KILOMETERS = "EmptyKilometers";
  public static final String COL_DESCRIPTION = "Description";
  public static final String COL_CARGO_DIRECTIONS = "Directions";

  public static final String COL_UNLOADING_PLACE = "UnloadingPlace";
  public static final String COL_LOADING_PLACE = "LoadingPlace";

  public static final String COL_PLACE_ORDINAL = "Ordinal";
  public static final String COL_PLACE_DATE = "Date";
  public static final String COL_PLACE_COMPANY = "Company";
  public static final String COL_PLACE_CONTACT = "Contact";
  public static final String COL_PLACE_CITY = "City";
  public static final String COL_PLACE_COUNTRY = "Country";
  public static final String COL_PLACE_ADDRESS = "Address";
  public static final String COL_PLACE_POST_INDEX = "PostIndex";
  public static final String COL_PLACE_NUMBER = "Number";
  public static final String COL_PLACE_NOTE = "Note";

  public static final String COL_VEHICLE_ID = "VehicleID";
  public static final String COL_VEHICLE = "Vehicle";
  public static final String COL_TRAILER = "Trailer";
  public static final String COL_VEHICLE_NUMBER = "Number";
  public static final String COL_FUEL = "Fuel";

  public static final String COL_VEHICLE_START_DATE = "StartDate";
  public static final String COL_VEHICLE_END_DATE = "EndDate";
  public static final String COL_VEHICLE_NOTES = "Notes";

  public static final String COL_DRIVER = "Driver";
  public static final String COL_DRIVER_PERSON = "CompanyPerson";
  public static final String COL_DRIVER_START_DATE = "StartDate";
  public static final String COL_DRIVER_END_DATE = "EndDate";
  public static final String COL_DRIVER_EXPERIENCE = "Experience";
  public static final String COL_DRIVER_NOTES = "Notes";
  public static final String COL_MAIN_DRIVER = "MainDriver";

  public static final String COL_TRIP_DRIVER_FROM = "DateFrom";
  public static final String COL_TRIP_DRIVER_TO = "DateTo";
  public static final String COL_TRIP_DRIVER_NOTE = "Note";

  public static final String COL_VEHICLE_SERVICE_DATE = "Date";
  public static final String COL_VEHICLE_SERVICE_DATE_TO = "DateTo";
  public static final String COL_VEHICLE_SERVICE_TYPE = "Type";
  public static final String COL_VEHICLE_SERVICE_NAME = "Name";
  public static final String COL_VEHICLE_SERVICE_NOTES = "Notes";

  public static final String COL_ABSENCE_NAME = "Name";
  public static final String COL_ABSENCE_LABEL = "Label";
  public static final String COL_ABSENCE_COLOR = "Color";

  public static final String COL_ABSENCE = "Absence";
  public static final String COL_ABSENCE_FROM = "DateFrom";
  public static final String COL_ABSENCE_TO = "DateTo";
  public static final String COL_ABSENCE_NOTES = "Notes";

  public static final String COL_IS_TRUCK = "IsTruck";
  public static final String COL_IS_TRAILER = "IsTrailer";

  public static final String COL_FX_PIXELS_PER_CUSTOMER = "FxPixelsPerCustomer";
  public static final String COL_FX_PIXELS_PER_ORDER = "FxPixelsPerOrder";

  public static final String COL_FX_COUNTRY_FLAGS = "FxCountryFlags";
  public static final String COL_FX_PLACE_INFO = "FxPlaceInfo";

  public static final String COL_FX_PLACE_CITIES = "FxPlaceCities";
  public static final String COL_FX_PLACE_CODES = "FxPlaceCodes";

  public static final String COL_FX_PIXELS_PER_DAY = "FxPixelsPerDay";
  public static final String COL_FX_PIXELS_PER_ROW = "FxPixelsPerRow";

  public static final String COL_FX_HEADER_HEIGHT = "FxHeaderHeight";
  public static final String COL_FX_FOOTER_HEIGHT = "FxFooterHeight";

  public static final String COL_FX_REFRESH_LOCAL_CHANGES = "FxRefreshLocalChanges";
  public static final String COL_FX_REFRESH_REMOTE_CHANGES = "FxRefreshRemoteChanges";

  public static final String COL_FX_THEME = "FxTheme";

  public static final String COL_FX_ITEM_OPACITY = "FxItemOpacity";
  public static final String COL_FX_STRIP_OPACITY = "FxStripOpacity";

  public static final String COL_FX_FILTER_DATA_TYPES = "FxFilterDataTypes";
  public static final String COL_FX_FILTERS = "FxFilters";

  public static final String COL_SS_MIN_DATE = "SsMinDate";
  public static final String COL_SS_MAX_DATE = "SsMaxDate";

  public static final String COL_SS_TRANSPORT_GROUPS = "SsTransportGroups";
  public static final String COL_SS_COMPLETED_TRIPS = "SsCompletedTrips";

  public static final String COL_SS_PIXELS_PER_TRUCK = "SsPixelsPerTruck";
  public static final String COL_SS_PIXELS_PER_TRIP = "SsPixelsPerTrip";

  public static final String COL_SS_SEPARATE_TRIPS = "SsSeparateTrips";
  public static final String COL_SS_SEPARATE_CARGO = "SsSeparateCargo";

  public static final String COL_SS_COUNTRY_FLAGS = "SsCountryFlags";
  public static final String COL_SS_PLACE_INFO = "SsPlaceInfo";
  public static final String COL_SS_PLACE_CITIES = "SsPlaceCities";
  public static final String COL_SS_PLACE_CODES = "SsPlaceCodes";

  public static final String COL_SS_ADDITIONAL_INFO = "SsAdditionalInfo";

  public static final String COL_SS_ORDER_CUSTOMER = "SsOrderCustomer";
  public static final String COL_SS_ORDER_NO = "SsOrderNo";

  public static final String COL_SS_PIXELS_PER_DAY = "SsPixelsPerDay";
  public static final String COL_SS_PIXELS_PER_ROW = "SsPixelsPerRow";

  public static final String COL_SS_HEADER_HEIGHT = "SsHeaderHeight";
  public static final String COL_SS_FOOTER_HEIGHT = "SsFooterHeight";

  public static final String COL_SS_REFRESH_LOCAL_CHANGES = "SsRefreshLocalChanges";
  public static final String COL_SS_REFRESH_REMOTE_CHANGES = "SsRefreshRemoteChanges";

  public static final String COL_SS_THEME = "SsTheme";

  public static final String COL_SS_ITEM_OPACITY = "SsItemOpacity";
  public static final String COL_SS_STRIP_OPACITY = "SsStripOpacity";

  public static final String COL_SS_FILTER_DATA_TYPES = "SsFilterDataTypes";
  public static final String COL_SS_FILTERS = "SsFilters";

  public static final String COL_DTB_MIN_DATE = "DtbMinDate";
  public static final String COL_DTB_MAX_DATE = "DtbMaxDate";

  public static final String COL_DTB_TRANSPORT_GROUPS = "DtbTransportGroups";
  public static final String COL_DTB_COMPLETED_TRIPS = "DtbCompletedTrips";

  public static final String COL_DTB_PIXELS_PER_DRIVER = "DtbPixelsPerDriver";

  public static final String COL_DTB_COUNTRY_FLAGS = "DtbCountryFlags";
  public static final String COL_DTB_PLACE_INFO = "DtbPlaceInfo";

  public static final String COL_DTB_PLACE_CITIES = "DtbPlaceCities";
  public static final String COL_DTB_PLACE_CODES = "DtbPlaceCodes";

  public static final String COL_DTB_ADDITIONAL_INFO = "DtbAdditionalInfo";

  public static final String COL_DTB_PIXELS_PER_DAY = "DtbPixelsPerDay";
  public static final String COL_DTB_PIXELS_PER_ROW = "DtbPixelsPerRow";

  public static final String COL_DTB_HEADER_HEIGHT = "DtbHeaderHeight";
  public static final String COL_DTB_FOOTER_HEIGHT = "DtbFooterHeight";

  public static final String COL_DTB_REFRESH_LOCAL_CHANGES = "DtbRefreshLocalChanges";
  public static final String COL_DTB_REFRESH_REMOTE_CHANGES = "DtbRefreshRemoteChanges";

  public static final String COL_DTB_COLOR = "DtbColor";

  public static final String COL_DTB_ITEM_OPACITY = "DtbItemOpacity";
  public static final String COL_DTB_STRIP_OPACITY = "DtbStripOpacity";

  public static final String COL_DTB_FILTER_DATA_TYPES = "DtbFilterDataTypes";
  public static final String COL_DTB_FILTERS = "DtbFilters";

  public static final String COL_TRUCK_MIN_DATE = "TruckMinDate";
  public static final String COL_TRUCK_MAX_DATE = "TruckMaxDate";

  public static final String COL_TRUCK_TRANSPORT_GROUPS = "TruckTransportGroups";
  public static final String COL_TRUCK_COMPLETED_TRIPS = "TruckCompletedTrips";

  public static final String COL_TRUCK_PIXELS_PER_NUMBER = "TruckPixelsPerNumber";
  public static final String COL_TRUCK_PIXELS_PER_INFO = "TruckPixelsPerInfo";

  public static final String COL_TRUCK_SEPARATE_CARGO = "TruckSeparateCargo";
  public static final String COL_TRUCK_COUNTRY_FLAGS = "TruckCountryFlags";
  public static final String COL_TRUCK_PLACE_INFO = "TruckPlaceInfo";

  public static final String COL_TRUCK_PLACE_CITIES = "TruckPlaceCities";
  public static final String COL_TRUCK_PLACE_CODES = "TruckPlaceCodes";

  public static final String COL_TRUCK_ADDITIONAL_INFO = "TruckAdditionalInfo";

  public static final String COL_TRUCK_PIXELS_PER_DAY = "TruckPixelsPerDay";
  public static final String COL_TRUCK_PIXELS_PER_ROW = "TruckPixelsPerRow";

  public static final String COL_TRUCK_HEADER_HEIGHT = "TruckHeaderHeight";
  public static final String COL_TRUCK_FOOTER_HEIGHT = "TruckFooterHeight";

  public static final String COL_TRUCK_REFRESH_LOCAL_CHANGES = "TruckRefreshLocalChanges";
  public static final String COL_TRUCK_REFRESH_REMOTE_CHANGES = "TruckRefreshRemoteChanges";

  public static final String COL_TRUCK_THEME = "TruckTheme";

  public static final String COL_TRUCK_ITEM_OPACITY = "TruckItemOpacity";
  public static final String COL_TRUCK_STRIP_OPACITY = "TruckStripOpacity";

  public static final String COL_TRUCK_FILTER_DATA_TYPES = "TruckFilterDataTypes";
  public static final String COL_TRUCK_FILTERS = "TruckFilters";

  public static final String COL_TRAILER_MIN_DATE = "TrailerMinDate";
  public static final String COL_TRAILER_MAX_DATE = "TrailerMaxDate";

  public static final String COL_TRAILER_TRANSPORT_GROUPS = "TrailerTransportGroups";
  public static final String COL_TRAILER_COMPLETED_TRIPS = "TrailerCompletedTrips";

  public static final String COL_TRAILER_PIXELS_PER_NUMBER = "TrailerPixelsPerNumber";
  public static final String COL_TRAILER_PIXELS_PER_INFO = "TrailerPixelsPerInfo";

  public static final String COL_TRAILER_SEPARATE_CARGO = "TrailerSeparateCargo";
  public static final String COL_TRAILER_COUNTRY_FLAGS = "TrailerCountryFlags";
  public static final String COL_TRAILER_PLACE_INFO = "TrailerPlaceInfo";
  public static final String COL_TRAILER_PLACE_CITIES = "TrailerPlaceCities";
  public static final String COL_TRAILER_PLACE_CODES = "TrailerPlaceCodes";

  public static final String COL_TRAILER_ADDITIONAL_INFO = "TrailerAdditionalInfo";

  public static final String COL_TRAILER_PIXELS_PER_DAY = "TrailerPixelsPerDay";
  public static final String COL_TRAILER_PIXELS_PER_ROW = "TrailerPixelsPerRow";

  public static final String COL_TRAILER_HEADER_HEIGHT = "TrailerHeaderHeight";
  public static final String COL_TRAILER_FOOTER_HEIGHT = "TrailerFooterHeight";

  public static final String COL_TRAILER_REFRESH_LOCAL_CHANGES = "TrailerRefreshLocalChanges";
  public static final String COL_TRAILER_REFRESH_REMOTE_CHANGES = "TrailerRefreshRemoteChanges";

  public static final String COL_TRAILER_THEME = "TrailerTheme";

  public static final String COL_TRAILER_ITEM_OPACITY = "TrailerItemOpacity";
  public static final String COL_TRAILER_STRIP_OPACITY = "TrailerStripOpacity";

  public static final String COL_TRAILER_FILTER_DATA_TYPES = "TrailerFilterDataTypes";
  public static final String COL_TRAILER_FILTERS = "TrailerFilters";

  public static final String COL_REGISTRATION_DATE = "Date";
  public static final String COL_REGISTRATION_STATUS = "Status";
  public static final String COL_REGISTRATION_COMPANY_NAME = "CompanyName";
  public static final String COL_REGISTRATION_COMPANY_CODE = "CompanyCode";
  public static final String COL_REGISTRATION_VAT_CODE = "VatCode";
  public static final String COL_REGISTRATION_CONTACT = "Contact";
  public static final String COL_REGISTRATION_CONTACT_POSITION = "ContactPosition";
  public static final String COL_REGISTRATION_ADDRESS = "Address";
  public static final String COL_REGISTRATION_CITY = "City";
  public static final String COL_REGISTRATION_COUNTRY = "Country";
  public static final String COL_REGISTRATION_PHONE = "Phone";
  public static final String COL_REGISTRATION_MOBILE = "Mobile";
  public static final String COL_REGISTRATION_FAX = "Fax";
  public static final String COL_REGISTRATION_EMAIL = "Email";
  public static final String COL_REGISTRATION_EXCHANGE_CODE = "ExchangeCode";
  public static final String COL_REGISTRATION_BANK = "Bank";
  public static final String COL_REGISTRATION_BANK_ADDRESS = "BankAddress";
  public static final String COL_REGISTRATION_BANK_ACCOUNT = "BankAccount";
  public static final String COL_REGISTRATION_SWIFT = "Swift";
  public static final String COL_REGISTRATION_NOTES = "Notes";
  public static final String COL_REGISTRATION_REGISTER = "Register";

  public static final String COL_QUERY_DATE = "Date";
  public static final String COL_QUERY_STATUS = "Status";
  public static final String COL_QUERY_CUSTOMER_TYPE = "CustomerType";
  public static final String COL_QUERY_CUSTOMER_NAME = "CustomerName";
  public static final String COL_QUERY_CUSTOMER_CODE = "CustomerCode";
  public static final String COL_QUERY_CUSTOMER_VAT_CODE = "CustomerVatCode";
  public static final String COL_QUERY_CUSTOMER_COUNTRY = "CustomerCountry";
  public static final String COL_QUERY_CUSTOMER_CITY = "CustomerCity";
  public static final String COL_QUERY_CUSTOMER_ADDRESS = "CustomerAddress";
  public static final String COL_QUERY_CUSTOMER_POST_INDEX = "CustomerPostIndex";
  public static final String COL_QUERY_CUSTOMER_PHONE = "CustomerPhone";
  public static final String COL_QUERY_CUSTOMER_FAX = "CustomerFax";
  public static final String COL_QUERY_CUSTOMER_EMAIL = "CustomerEmail";
  public static final String COL_QUERY_CUSTOMER_CONTACT = "CustomerContactPerson";
  public static final String COL_QUERY_CUSTOMER_CONTACT_POSITION = "CustomerPersonPosition";
  public static final String COL_QUERY_CUSTOMER_EXCHANGE_CODE = "CustomerExchangeCode";
  public static final String COL_QUERY_LOADING_EMAIL = "LoadingEmail";
  public static final String COL_QUERY_LOADING_CITY = "LoadingCity";
  public static final String COL_QUERY_UNLOADING_EMAIL = "UnloadingEmail";
  public static final String COL_QUERY_UNLOADING_CITY = "UnloadingCity";
  public static final String COL_QUERY_EXPEDITION = "Expedition";
  public static final String COL_QUERY_DELIVERY_DATE = "DeliveryDate";
  public static final String COL_QUERY_DELIVERY_TIME = "DeliveryTime";
  public static final String COL_QUERY_TERMS_OF_DELIVERY = "TermsOfDelivery";
  public static final String COL_QUERY_CUSTOMS_BROKERAGE = "CustomsBrokerage";
  public static final String COL_QUERY_FREIGHT_INSURANCE = "FreightInsurance";
  public static final String COL_QUERY_CARGO = "Cargo";
  public static final String COL_QUERY_MANAGER = "Manager";
  public static final String COL_QUERY_NOTES = "Notes";
  public static final String COL_QUERY_HOST = "Host";
  public static final String COL_QUERY_AGENT = "Agent";
  public static final String COL_QUERY_REASON = "LossReason";

  public static final String COL_TEXT_CONSTANT = "Constant";
  public static final String COL_TEXT_CONTENT = "Content";

  public static final String COL_EXPEDITION_TYPE_NAME = "Name";
  public static final String COL_EXPEDITION_LOGISTICS = "Logistics";
  public static final String COL_SHIPPING_TERM_NAME = "Name";
  public static final String COL_SELF_SERVICE = "SelfService";
  public static final String COL_SHIPMENT_REQUEST = "ShipmentRequest";

  public static final String COL_CARGO_TYPE_NAME = "CargoTypeName";
  public static final String COL_CARGO_TYPE_COLOR = "Color";

  public static final String COL_TRANSPORTATION_ORDER = "TransportationOrder";

  public static final String FORM_TEXT_CONSTANT = "TextConstant";
  public static final String FORM_NEW_VEHICLE = "NewVehicle";
  public static final String FORM_ORDER = "TransportationOrder";
  public static final String FORM_NEW_SIMPLE_ORDER = "NewSimpleTransportationOrder";
  public static final String FORM_TRIP = "Trip";
  public static final String FORM_EXPEDITION_TRIP = "ExpeditionTrip";
  public static final String FORM_CARGO = "OrderCargo";
  public static final String FORM_ASSESSMENT = "Assessment";
  public static final String FORM_ASSESSMENT_FORWARDER = "AssessmentForwarder";
  public static final String FORM_ASSESSMENT_TRANSPORTATION = "AssessmentTransportation";

  public static final String FORM_NEW_CARGO_CREDIT_INVOICE = "NewCargoCreditInvoice";
  public static final String FORM_CARGO_INVOICE = "CargoInvoice";
  public static final String FORM_CARGO_PURCHASE_INVOICE = "CargoPurchaseInvoice";
  public static final String FORM_TRIP_PURCHASE_INVOICE = "TripPurchaseInvoice";

  public static final String FORM_FX_SETTINGS = "TrFxSettings";
  public static final String FORM_SS_SETTINGS = "TrSsSettings";
  public static final String FORM_DTB_SETTINGS = "TrDtbSettings";
  public static final String FORM_TRUCK_SETTINGS = "TruckTbSettings";
  public static final String FORM_TRAILER_SETTINGS = "TrailerTbSettings";

  public static final String FORM_SHIPMENT_REQUEST = "ShipmentRequest";
  public static final String FORM_CARGO_PLACE = "CargoPlace";
  public static final String FORM_CARGO_PLACE_UNBOUND = FORM_CARGO_PLACE + VAR_UNBOUND;

  public static final String GRID_ASSESSMENT_REQUESTS = "AssessmentRequests";
  public static final String GRID_ASSESSMENT_ORDERS = "AssessmentOrders";
  public static final String GRID_SHIPMENT_REQUESTS = "ShipmentRegisteredRequests";

  public static final String PROP_COLORS = "Colors";
  public static final String PROP_COUNTRIES = "Countries";
  public static final String PROP_CITIES = "Cities";
  public static final String PROP_DRIVERS = "Drivers";
  public static final String PROP_DRIVER_GROUPS = "DriverGroups";
  public static final String PROP_ABSENCE = "Absence";
  public static final String PROP_VEHICLES = "Vehicles";
  public static final String PROP_VEHICLE_SERVICES = "VehicleServices";
  public static final String PROP_ORDER_CARGO = "OrderCargo";
  public static final String PROP_TRIPS = "Trips";
  public static final String PROP_TRIP_DRIVERS = "TripDrivers";
  public static final String PROP_FREIGHTS = "Freights";
  public static final String PROP_CARGO_HANDLING = "CargoHandling";
  public static final String PROP_CARGO_TYPES = "CargoTypes";
  public static final String PROP_TRANSPORT_GROUPS = "TransportGroups";
  public static final String PROP_VEHICLE_GROUPS = "VehicleGroups";
  public static final String PROP_VEHICLE_MANAGER = "VehicleManager";

  public static final String ALS_TRIP_DATE = "TripDate";
  public static final String ALS_ORDER_DATE = "OrderDate";
  public static final String ALS_ORDER_STATUS = "OrderStatus";
  public static final String ALS_ORDER_NOTES = "OrderNotes";

  public static final String ALS_FORWARDER_NAME = "ForwarderName";
  public static final String ALS_EXPEDITION_TYPE = "ExpeditionType";
  public static final String ALS_ASSESSMENT_FORWARDER = "AssessmentForwarder";

  public static final String ALS_VEHICLE_NUMBER = "VehicleNumber";
  public static final String ALS_TRAILER_NUMBER = "TrailerNumber";

  public static final String ALS_TRIP_MANAGER = "TripManager";

  public static final String ALS_TRIP_VERSION = "TripVersion";
  public static final String ALS_CARGO_TRIP_VERSION = "CargoTripVersion";

  public static final String ALS_ABSENCE_NAME = "AbsenceName";
  public static final String ALS_ABSENCE_LABEL = "AbsenceLabel";
  public static final String ALS_CUSTOMER_NAME = "CustomerName";
  public static final String ALS_CUSTOMER_TYPE_NAME = "CustomerTypeName";

  public static final String ALS_CARGO_DESCRIPTION = "CargoDescription";

  public static final String ALS_LOADING_DATE = "LoadingDate";
  public static final String ALS_LOADING_NUMBER = "LoadingNumber";
  public static final String ALS_LOADING_CONTACT = "LoadingContact";
  public static final String ALS_LOADING_COMPANY = "LoadingCompany";
  public static final String ALS_LOADING_ADDRESS = "LoadingAddress";
  public static final String ALS_LOADING_POST_INDEX = "LoadingPostIndex";
  public static final String ALS_LOADING_CITY_NAME = "LoadingCityName";
  public static final String ALS_LOADING_COUNTRY_NAME = "LoadingCountryName";
  public static final String ALS_LOADING_COUNTRY_CODE = "LoadingCountryCode";

  public static final String ALS_UNLOADING_DATE = "UnloadingDate";
  public static final String ALS_UNLOADING_NUMBER = "UnloadingNumber";
  public static final String ALS_UNLOADING_CONTACT = "UnloadingContact";
  public static final String ALS_UNLOADING_COMPANY = "UnloadingCompany";
  public static final String ALS_UNLOADING_ADDRESS = "UnLoadingAddress";
  public static final String ALS_UNLOADING_POST_INDEX = "UnloadingPostIndex";
  public static final String ALS_UNLOADING_CITY_NAME = "UnloadingCityName";
  public static final String ALS_UNLOADING_COUNTRY_NAME = "UnloadingCountryName";
  public static final String ALS_UNLOADING_COUNTRY_CODE = "UnloadingCountryCode";

  public static final String ALS_PAYER_NAME = "PayerName";
  public static final String ALS_PAYER_TYPE_NAME = "PayerTypeName";

  public static final String ALS_REQUEST_CUSTOMER_FIRST_NAME = "CustomerFirstName";
  public static final String ALS_REQUEST_CUSTOMER_LAST_NAME = "CustomerLastName";
  public static final String ALS_REQUEST_CUSTOMER_COMPANY = "CustomerCompany";

  public static final String ALS_CARGO_CMR_NUMBER = "CmrNumber";
  public static final String ALS_CARGO_NOTES = "CargoNotes";

  public static final String ALS_SERVICE_NAME = "ServiceName";

  public static final String DATA_TYPE_ORDER_CARGO = "OrderCargo";
  public static final String DATA_TYPE_TRIP = "Trip";
  public static final String DATA_TYPE_FREIGHT = "Freight";
  public static final String DATA_TYPE_TRUCK = "Truck";
  public static final String DATA_TYPE_TRAILER = "Trailer";
  public static final String DATA_TYPE_DRIVER = "Driver";

  public static final String REP_CONTRACT = "OrderContract";

  public static final String AR_DEPARTMENT = "Department";
  public static final String AR_MANAGER = "Manager";
  public static final String AR_CUSTOMER = "Customer";

  public static final String AR_RECEIVED = "Received";
  public static final String AR_ANSWERED = "Answered";
  public static final String AR_LOST = "Lost";
  public static final String AR_APPROVED = "Approved";

  public static final String AR_SECONDARY = "Secondary";

  public static final String AR_INCOME = "Income";
  public static final String AR_EXPENSE = "Expense";
  public static final String AR_SECONDARY_INCOME = "SecondaryIncome";
  public static final String AR_SECONDARY_EXPENSE = "SecondaryExpense";

  public static final List<String> TRIP_DATE_COLUMNS = Lists.newArrayList(COL_TRIP_DATE,
      COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_TRIP_PLANNED_END_DATE);

  private TransportConstants() {
  }
}
