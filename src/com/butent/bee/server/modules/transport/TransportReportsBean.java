package com.butent.bee.server.modules.transport;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_VAT;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class TransportReportsBean {

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;

  /**
   * Return SqlSelect query, calculating cargo incomes from CargoServices table.
   *
   * @param cargos query filter with <b>unique</b> "Cargo" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return query with columns: "Cargo", "CargoIncome", "ServicesIncome"
   */
  public SqlSelect getCargoIncomeQuery(SqlSelect cargos, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromInner(cargos, alias, sys.joinTables(TBL_ORDER_CARGO, alias, COL_CARGO))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_INCOMES,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromLeft(TBL_SERVICES, sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO));

    IsExpression amountExpr = SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_CARGO_INCOMES, amountExpr);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES, amountExpr);
    }
    IsExpression currencyExpr = SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY);
    IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
        SqlUtils.field(TBL_ORDERS, COL_DATE));

    IsExpression cargoIncome = SqlUtils.sqlIf(SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION),
        null, amountExpr);
    IsExpression servicesIncome = SqlUtils.sqlIf(SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION),
        amountExpr, null);

    if (DataUtils.isId(currency)) {
      cargoIncome = ExchangeUtils.exchangeFieldTo(ss, cargoIncome, currencyExpr, dateExpr,
          SqlUtils.constant(currency));

      servicesIncome = ExchangeUtils.exchangeFieldTo(ss, servicesIncome, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      cargoIncome = ExchangeUtils.exchangeField(ss, cargoIncome, currencyExpr, dateExpr);

      servicesIncome = ExchangeUtils.exchangeField(ss, servicesIncome, currencyExpr, dateExpr);
    }
    ss.addSum(cargoIncome, "CargoIncome")
        .addSum(servicesIncome, "ServicesIncome");

    return ss;
  }

  public ResponseObject getCargoProfit(SqlSelect flt) {
    SqlSelect ss = getCargoIncomeQuery(flt, null, false)
        .addEmptyDouble("ServicesCost")
        .addEmptyDouble("TripCosts");

    String crsTotals = qs.sqlCreateTemp(ss);
    String alias = SqlUtils.uniqueName();

    qs.updateData(new SqlUpdate(crsTotals)
        .setFrom(getCargoCostQuery(flt, null, false), alias,
            SqlUtils.joinUsing(crsTotals, alias, COL_CARGO))
        .addExpression("ServicesCost", SqlUtils.field(alias, VAR_EXPENSE)));

    ss = new SqlSelect()
        .setDistinctMode(true)
        .addFields(TBL_CARGO_TRIPS, COL_TRIP)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(crsTotals, SqlUtils.joinUsing(TBL_CARGO_TRIPS, crsTotals, COL_CARGO));

    String crsIncomes = getTripIncomes(ss, null, false);
    String crsCosts = getTripCosts(ss, null, false);

    ss = new SqlSelect()
        .addFields(crsIncomes, COL_TRIP)
        .addSum(TBL_CARGO_TRIPS, "CargoPercent", "TotalPercent")
        .addSum(SqlUtils.sqlIf(SqlUtils.isNull(TBL_CARGO_TRIPS, "CargoPercent"),
            SqlUtils.field(crsIncomes, "TripIncome"), 0), "TotalIncome")
        .addFrom(crsIncomes)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.joinUsing(crsIncomes, TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO))
        .addGroup(crsIncomes, COL_TRIP);

    String tmp = qs.sqlCreateTemp(ss);

    IsExpression xpr = SqlUtils.multiply(
        SqlUtils.divide(
            SqlUtils.plus(
                SqlUtils.nvl(SqlUtils.field(crsCosts, "DailyCosts"), 0),
                SqlUtils.nvl(SqlUtils.field(crsCosts, "RoadCosts"), 0),
                SqlUtils.nvl(SqlUtils.field(crsCosts, "OtherCosts"), 0),
                SqlUtils.nvl(SqlUtils.field(crsCosts, "FuelCosts"), 0)),
            100),
        SqlUtils.sqlIf(SqlUtils.isNull(TBL_CARGO_TRIPS, "CargoPercent"),
            SqlUtils.multiply(
                SqlUtils.minus(100, SqlUtils.nvl(SqlUtils.field(tmp, "TotalPercent"), 0)),
                SqlUtils.divide(SqlUtils.field(crsIncomes, "TripIncome"),
                    SqlUtils.field(tmp, "TotalIncome"))),
            SqlUtils.field(TBL_CARGO_TRIPS, "CargoPercent")));

    ss = new SqlSelect()
        .addFields(crsIncomes, COL_CARGO)
        .addSum(xpr, "Cost")
        .addFrom(crsIncomes)
        .addFromInner(TBL_CARGO_TRIPS,
            SqlUtils.joinUsing(crsIncomes, TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO))
        .addFromInner(crsCosts, SqlUtils.joinUsing(crsIncomes, crsCosts, COL_TRIP))
        .addFromInner(tmp, SqlUtils.joinUsing(crsIncomes, tmp, COL_TRIP))
        .addGroup(crsIncomes, COL_CARGO);

    String crsTripCosts = qs.sqlCreateTemp(ss);

    qs.sqlDropTemp(tmp);
    qs.sqlDropTemp(crsCosts);
    qs.sqlDropTemp(crsIncomes);

    SqlUpdate su = new SqlUpdate(crsTotals)
        .setFrom(crsTripCosts, SqlUtils.joinUsing(crsTotals, crsTripCosts, COL_CARGO))
        .addExpression("TripCosts", SqlUtils.field(crsTripCosts, "Cost"));

    qs.updateData(su);

    qs.sqlDropTemp(crsTripCosts);

    ss = new SqlSelect()
        .addSum(crsTotals, "CargoIncome")
        .addSum(crsTotals, "TripCosts")
        .addSum(crsTotals, "ServicesIncome")
        .addSum(crsTotals, "ServicesCost")
        .addFrom(crsTotals);

    SimpleRowSet.SimpleRow res = qs.getRow(ss);

    qs.sqlDropTemp(crsTotals);

    return ResponseObject.response(new String[] {
        "CargoIncome:", res.getValue("CargoIncome"),
        "TripCosts:", res.getValue("TripCosts"), "ServicesIncome:", res.getValue("ServicesIncome"),
        "ServicesCost:", res.getValue("ServicesCost")});
  }

  /**
   * Return SqlSelect query, calculating trip fuel consumptions from TripRoutes table.
   *
   * @param routes query filter with <b>unique</b> route values.
   * @param routeMode if true, returns results, grouped by RouteID, else grouped by Trip
   * @return query with two columns: (RouteID or "Trip") and "Quantity"
   */
  public SqlSelect getFuelConsumptionsQuery(SqlSelect routes, boolean routeMode) {
    String trips = TBL_TRIPS;
    String fuel = TBL_FUEL_CONSUMPTIONS;
    String routeId = sys.getIdName(TBL_TRIP_ROUTES);

    IsExpression xpr = SqlUtils.round(
        SqlUtils.sqlIf(SqlUtils.isNull(TBL_TRIP_ROUTES, "Consumption"),
            SqlUtils.plus(
                SqlUtils.divide(
                    SqlUtils.plus(
                        SqlUtils.nvl(
                            SqlUtils.multiply(
                                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS),
                                SqlUtils.sqlCase(SqlUtils.field(TBL_TRIP_ROUTES, "Season"), 0,
                                    SqlUtils.field(fuel, "Summer"),
                                    SqlUtils.field(fuel, "Winter")),
                                SqlUtils.plus(1,
                                    SqlUtils.divide(
                                        SqlUtils.nvl(
                                            SqlUtils.field(VIEW_FUEL_TEMPERATURES, "Rate"),
                                            0),
                                        100))),
                            0),
                        SqlUtils.nvl(
                            SqlUtils.multiply(
                                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS),
                                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_WEIGHT),
                                SqlUtils.field(fuel, "TonneKilometer")),
                            0)),
                    100),
                SqlUtils.nvl(SqlUtils.multiply(SqlUtils.field(TBL_TRIP_ROUTES, "MotoHours"),
                    SqlUtils.field(fuel, "MotoHour")), 0)),
            SqlUtils.field(TBL_TRIP_ROUTES, "Consumption")), 2);

    String alias = SqlUtils.uniqueName();

    return new SqlSelect()
        .addFields(TBL_TRIP_ROUTES, routeMode ? routeId : COL_TRIP)
        .addSum(xpr, "Quantity")
        .addFrom(TBL_TRIP_ROUTES)
        .addFromInner(routes, alias, SqlUtils.joinUsing(TBL_TRIP_ROUTES, alias, routeId))
        .addFromInner(trips, sys.joinTables(trips, TBL_TRIP_ROUTES, COL_TRIP))
        .addFromInner(fuel, SqlUtils.joinUsing(trips, fuel, COL_VEHICLE))
        .addFromLeft(VIEW_FUEL_TEMPERATURES,
            SqlUtils.and(sys.joinTables(fuel, VIEW_FUEL_TEMPERATURES, "Consumption"),
                SqlUtils.joinUsing(VIEW_FUEL_TEMPERATURES, TBL_TRIP_ROUTES, "Season"),
                SqlUtils.or(SqlUtils.isNull(VIEW_FUEL_TEMPERATURES, "TempFrom"),
                    SqlUtils.lessEqual(SqlUtils.field(VIEW_FUEL_TEMPERATURES, "TempFrom"),
                        SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, "Temperature"), 0))),
                SqlUtils.or(SqlUtils.isNull(VIEW_FUEL_TEMPERATURES, "TempTo"),
                    SqlUtils.more(SqlUtils.field(VIEW_FUEL_TEMPERATURES, "TempTo"),
                        SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, "Temperature"), 0)))))
        .setWhere(SqlUtils.and(
            SqlUtils.or(SqlUtils.isNull(fuel, "DateFrom"),
                SqlUtils.joinLessEqual(fuel, "DateFrom", TBL_TRIP_ROUTES,
                    COL_ROUTE_DEPARTURE_DATE)),
            SqlUtils.or(SqlUtils.isNull(fuel, "DateTo"),
                SqlUtils.joinMore(fuel, "DateTo", TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE))))
        .addGroup(TBL_TRIP_ROUTES, routeMode ? routeId : COL_TRIP);
  }

  /**
   * Returns Temporary table name with calculated trip incomes by each cargo.
   *
   * @param trips query filter with <b>unique</b> "Trip" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return Temporary table name with following structure: <br>
   *         "Trip" - trip ID <br>
   *         "Cargo" - cargo ID <br>
   *         "TripIncome" - total trip income <br>
   */
  public String getTripIncomes(SqlSelect trips, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();
    String subq = SqlUtils.uniqueName();

    String tmpCargoTrip = getCargoTripPercents(COL_CARGO,
        new SqlSelect().setDistinctMode(true)
            .addFields(TBL_CARGO_TRIPS, COL_CARGO)
            .addFrom(TBL_CARGO_TRIPS)
            .addFromInner(trips, alias, SqlUtils.joinUsing(TBL_CARGO_TRIPS, alias, COL_TRIP)));

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tmpCargoTrip, COL_CARGO, COL_TRIP)
        .addExpr(SqlUtils.multiply(SqlUtils.divide(SqlUtils.field(subq, "CargoIncome"), 100),
            SqlUtils.field(tmpCargoTrip, COL_TRIP_PERCENT)), "TripIncome")
        .addFrom(tmpCargoTrip)
        .addFromInner(getCargoIncomeQuery(new SqlSelect().setDistinctMode(true)
            .addFields(tmpCargoTrip, COL_CARGO)
            .addFrom(tmpCargoTrip), currency, woVat),
            subq, SqlUtils.joinUsing(tmpCargoTrip, subq, COL_CARGO))
        .addFromInner(trips, alias, SqlUtils.joinUsing(tmpCargoTrip, alias, COL_TRIP)));

    qs.sqlDropTemp(tmpCargoTrip);
    return tmp;
  }

  public ResponseObject getTripProfit(long tripId) {
    String crs = getTripCosts(new SqlSelect().addConstant(tripId, "Trip"), null, false);

    SimpleRowSet.SimpleRow res = qs.getRow(new SqlSelect().addAllFields(crs).addFrom(crs));

    qs.sqlDropTemp(crs);

    crs = getTripIncomes(new SqlSelect().addConstant(tripId, "Trip"), null, false);

    SqlSelect ss = new SqlSelect()
        .addSum(crs, "TripIncome")
        .addFrom(crs);

    String tripIncome = qs.getValue(ss);

    qs.sqlDropTemp(crs);

    return ResponseObject.response(new String[] {
        "DailyCosts:", res.getValue("DailyCosts"),
        "RoadCosts:", res.getValue("RoadCosts"), "OtherCosts:", res.getValue("OtherCosts"),
        "FuelCosts:", res.getValue("FuelCosts"), "TripIncome:", tripIncome});
  }

  public ResponseObject getTripProfitReport(RequestInfo reqInfo) {
    Long currency = reqInfo.getParameterLong(COL_CURRENCY);
    boolean woVat = BeeUtils.toBoolean(reqInfo.getParameter(COL_TRADE_VAT));

    HasConditions clause = SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION));

    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));

    String trucks = SqlUtils.uniqueName();
    String trailers = SqlUtils.uniqueName();

    String tripIncome = "TripIncome";
    String kilometers = COL_ROUTE_KILOMETERS;
    String fuelCosts = "FuelCosts";
    String dailyCosts = "DailyCosts";
    String roadCosts = "RoadCosts";
    String constantCosts = "ConstantCosts";
    String otherCosts = "OtherCosts";
    String plannedKilometers = "Planned" + kilometers;
    String plannedFuelCosts = "Planned" + fuelCosts;
    String plannedDailyCosts = "Planned" + dailyCosts;
    String plannedRoadCosts = "Planned" + roadCosts;

    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_TRIPS,
        sys.getIdName(TBL_TRIPS)), SqlConstants.SqlDataType.STRING, 20, 0), COL_TRIP));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_NO));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_STATUS));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE_FROM));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE_TO));
    clause.add(report.getCondition(SqlUtils.field(trucks, COL_VEHICLE_NUMBER), COL_VEHICLE));
    clause.add(report.getCondition(SqlUtils.field(trailers, COL_VEHICLE_NUMBER), COL_TRAILER));

    SqlSelect query = new SqlSelect()
        .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
        .addFields(TBL_TRIPS, COL_TRIP_STATUS, COL_TRIP_DATE, COL_TRIP_NO,
            COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_TRIP_ROUTE)
        .addField(trucks, COL_VEHICLE_NUMBER, COL_VEHICLE)
        .addField(trailers, COL_VEHICLE_NUMBER, COL_TRAILER)
        .addEmptyDouble(plannedKilometers)
        .addEmptyDouble(kilometers)
        .addEmptyDouble(plannedFuelCosts)
        .addEmptyDouble(fuelCosts)
        .addEmptyDouble(plannedDailyCosts)
        .addEmptyDouble(dailyCosts)
        .addEmptyDouble(plannedRoadCosts)
        .addEmptyDouble(roadCosts)
        .addEmptyDouble(constantCosts)
        .addEmptyDouble(otherCosts)
        .addEmptyDouble(tripIncome)
        .addFrom(TBL_TRIPS)
        .addFromLeft(TBL_VEHICLES, trucks,
            sys.joinTables(TBL_VEHICLES, trucks, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailers,
            sys.joinTables(TBL_VEHICLES, trailers, TBL_TRIPS, COL_TRAILER))
        .setWhere(clause);

    String tmp = qs.sqlCreateTemp(query);

    // Routes
    if (report.requiresField(COL_TRIP_ROUTE)) {
      String rTmp = getTripRoutes(new SqlSelect()
          .addFields(tmp, COL_TRIP)
          .addFrom(tmp)
          .setWhere(SqlUtils.isNull(tmp, COL_TRIP_ROUTE)));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(COL_TRIP_ROUTE, SqlUtils.field(rTmp, COL_TRIP_ROUTE))
          .setFrom(rTmp, SqlUtils.joinUsing(tmp, rTmp, COL_TRIP)));

      qs.sqlDropTemp(rTmp);
    }
    // Kilometers
    if (report.requiresField(kilometers)) {
      String als = SqlUtils.uniqueName();

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(kilometers, SqlUtils.field(als, kilometers))
          .setFrom(new SqlSelect()
              .addFields(TBL_TRIP_ROUTES, COL_TRIP)
              .addSum(TBL_TRIP_ROUTES, kilometers)
              .addFrom(TBL_TRIP_ROUTES)
              .addFromInner(tmp, SqlUtils.joinUsing(TBL_TRIP_ROUTES, tmp, COL_TRIP))
              .addGroup(TBL_TRIP_ROUTES, COL_TRIP), als,
              SqlUtils.joinUsing(tmp, als, COL_TRIP)));
    }
    // Planned kilometers
    if (report.requiresField(plannedKilometers) || report.requiresField(plannedFuelCosts)
        || report.requiresField(plannedDailyCosts) || report.requiresField(plannedRoadCosts)) {

      String tmpCargo = qs.sqlCreateTemp(new SqlSelect().setDistinctMode(true)
          .addFields(TBL_CARGO_TRIPS, COL_CARGO)
          .addEmptyDouble(kilometers)
          .addExpr(SqlUtils.plus(
              SqlUtils.nvl(SqlUtils.field(TBL_ORDER_CARGO, COL_LOADED_KILOMETERS), 0),
              SqlUtils.nvl(SqlUtils.field(TBL_ORDER_CARGO, COL_EMPTY_KILOMETERS), 0)),
              plannedKilometers)
          .addFrom(TBL_CARGO_TRIPS)
          .addFromInner(tmp, SqlUtils.joinUsing(TBL_CARGO_TRIPS, tmp, COL_TRIP))
          .addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO)));

      String als = SqlUtils.uniqueName();

      qs.updateData(new SqlUpdate(tmpCargo)
          .addExpression(plannedKilometers,
              SqlUtils.plus(SqlUtils.field(tmpCargo, plannedKilometers),
                  SqlUtils.nvl(SqlUtils.field(als, COL_LOADED_KILOMETERS), 0),
                  SqlUtils.nvl(SqlUtils.field(als, COL_EMPTY_KILOMETERS), 0)))
          .setFrom(new SqlSelect()
              .addFields(TBL_CARGO_HANDLING, COL_CARGO)
              .addSum(TBL_CARGO_HANDLING, COL_LOADED_KILOMETERS)
              .addSum(TBL_CARGO_HANDLING, COL_EMPTY_KILOMETERS)
              .addFrom(TBL_CARGO_HANDLING)
              .addFromInner(tmpCargo,
                  SqlUtils.joinUsing(TBL_CARGO_HANDLING, tmpCargo, COL_CARGO))
              .addGroup(TBL_CARGO_HANDLING, COL_CARGO),
              als, SqlUtils.joinUsing(tmpCargo, als, COL_CARGO)));

      String tmpTripCargo = qs.sqlCreateTemp(new SqlSelect()
          .addFields(TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO)
          .addSum(TBL_TRIP_ROUTES, kilometers)
          .addEmptyDouble(plannedKilometers)
          .addFrom(TBL_TRIP_ROUTES)
          .addFromInner(TBL_CARGO_TRIPS,
              sys.joinTables(TBL_CARGO_TRIPS, TBL_TRIP_ROUTES, COL_ROUTE_CARGO))
          .addFromInner(tmpCargo, SqlUtils.joinUsing(TBL_CARGO_TRIPS, tmpCargo, COL_CARGO))
          .addGroup(TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO));

      qs.updateData(new SqlUpdate(tmpCargo)
          .addExpression(kilometers, SqlUtils.field(als, kilometers))
          .setFrom(new SqlSelect()
              .addFields(tmpTripCargo, COL_CARGO)
              .addSum(tmpTripCargo, kilometers)
              .addFrom(tmpTripCargo)
              .addGroup(tmpTripCargo, COL_CARGO),
              als, SqlUtils.joinUsing(tmpCargo, als, COL_CARGO)));

      qs.updateData(new SqlUpdate(tmpTripCargo)
          .addExpression(plannedKilometers,
              SqlUtils.multiply(SqlUtils.nvl(SqlUtils.field(tmpCargo, plannedKilometers), 0),
                  SqlUtils.divide(SqlUtils.nvl(SqlUtils.field(tmpTripCargo, kilometers), 0),
                      SqlUtils.nvl(SqlUtils.field(tmpCargo, kilometers), 0))))
          .setFrom(tmpCargo, SqlUtils.joinUsing(tmpTripCargo, tmpCargo, COL_CARGO))
          .setWhere(SqlUtils.positive(SqlUtils.nvl(SqlUtils.field(tmpCargo, kilometers), 0))));

      qs.sqlDropTemp(tmpCargo);

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(plannedKilometers, SqlUtils.field(als, plannedKilometers))
          .setFrom(new SqlSelect()
              .addFields(tmpTripCargo, COL_TRIP)
              .addSum(tmpTripCargo, plannedKilometers)
              .addFrom(tmpTripCargo)
              .addGroup(tmpTripCargo, COL_TRIP),
              als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));

      qs.sqlDropTemp(tmpTripCargo);
    }
    // Planned fuel costs
    if (report.requiresField(plannedFuelCosts)) {
      String als = SqlUtils.uniqueName();

      String tt = qs.sqlCreateTemp(new SqlSelect()
          .addFields(tmp, COL_TRIP, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_TRIP_DATE,
              plannedKilometers)
          .addFields(TBL_TRIPS, COL_VEHICLE)
          .addFields(TBL_TRIP_ROUTES, COL_ROUTE_SEASON)
          .addEmptyNumeric(fuelCosts, 6, 2)
          .addFrom(tmp)
          .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, tmp, COL_TRIP))
          .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(tmp, TBL_TRIP_ROUTES, COL_TRIP))
          .addFromInner(new SqlSelect()
              .addFields(TBL_TRIP_ROUTES, COL_TRIP)
              .addMin(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE)
              .addFrom(tmp)
              .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(tmp, TBL_TRIP_ROUTES, COL_TRIP))
              .addGroup(TBL_TRIP_ROUTES, COL_TRIP),
              als, SqlUtils.joinUsing(TBL_TRIP_ROUTES, als, COL_TRIP, COL_ROUTE_DEPARTURE_DATE)));

      qs.updateData(new SqlUpdate(tt)
          .addExpression(fuelCosts, SqlUtils.sqlCase(SqlUtils.field(tt, COL_ROUTE_SEASON), 0,
              SqlUtils.field(TBL_FUEL_CONSUMPTIONS, "Summer"),
              SqlUtils.field(TBL_FUEL_CONSUMPTIONS, "Winter")))
          .setFrom(TBL_FUEL_CONSUMPTIONS, SqlUtils.and(
              SqlUtils.joinUsing(tt, TBL_FUEL_CONSUMPTIONS, COL_VEHICLE),
              SqlUtils.or(SqlUtils.isNull(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_FROM),
                  SqlUtils.joinLessEqual(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_FROM, tt,
                      COL_TRIP_DATE_FROM)),
              SqlUtils.or(SqlUtils.isNull(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_TO),
                  SqlUtils.joinMore(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_TO, tt,
                      COL_TRIP_DATE_FROM)))));

      IsExpression xpr = SqlUtils.divide(
          SqlUtils.multiply(SqlUtils.field(tt, plannedKilometers), SqlUtils.field(tt, fuelCosts)),
          100,
          SqlUtils.divide(SqlUtils.minus(SqlUtils.field(tt, COL_TRIP_DATE_TO),
              SqlUtils.field(tt, COL_TRIP_DATE_FROM)),
              TimeUtils.MILLIS_PER_DAY));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(plannedFuelCosts, SqlUtils.field(als, plannedFuelCosts))
          .setFrom(getConstantsQuery(tt, TBL_TRIP_CONSTANTS,
              SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
                  TripConstant.AVERAGE_FUEL_COST.ordinal()), plannedFuelCosts, xpr, currency),
              als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));

      qs.sqlDropTemp(tt);
    }
    // Planned daily costs
    if (report.requiresField(plannedDailyCosts)) {
      String tt = qs.sqlCreateTemp(new SqlSelect()
          .addFields(tmp, COL_TRIP, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_TRIP_DATE,
              plannedKilometers)
          .addFields(TBL_TRIP_DRIVERS, COL_DRIVER)
          .addFrom(tmp)
          .addFromInner(TBL_TRIP_DRIVERS, SqlUtils.joinUsing(tmp, TBL_TRIP_DRIVERS, COL_TRIP)));

      String als = SqlUtils.uniqueName();

      IsExpression xpr = SqlUtils.divide(SqlUtils.field(tt, plannedKilometers),
          SqlUtils.divide(SqlUtils.minus(SqlUtils.field(tt, COL_TRIP_DATE_TO),
              SqlUtils.field(tt, COL_TRIP_DATE_FROM)), TimeUtils.MILLIS_PER_DAY));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(plannedDailyCosts, SqlUtils.field(als, plannedDailyCosts))
          .setFrom(getConstantsQuery(tt, TBL_DRIVER_DAILY_COSTS,
              SqlUtils.joinUsing(tt, TBL_DRIVER_DAILY_COSTS, COL_DRIVER),
              plannedDailyCosts, xpr, currency),
              als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));

      qs.sqlDropTemp(tt);
    }
    // Planned road costs
    if (report.requiresField(plannedRoadCosts)) {
      String als = SqlUtils.uniqueName();

      IsExpression xpr = SqlUtils.divide(SqlUtils.field(tmp, plannedKilometers),
          SqlUtils.divide(SqlUtils.minus(SqlUtils.field(tmp, COL_TRIP_DATE_TO),
              SqlUtils.field(tmp, COL_TRIP_DATE_FROM)), TimeUtils.MILLIS_PER_DAY));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(plannedRoadCosts, SqlUtils.field(als, plannedRoadCosts))
          .setFrom(getConstantsQuery(tmp, TBL_TRIP_CONSTANTS,
              SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
                  TripConstant.AVERAGE_KM_COST.ordinal()), plannedRoadCosts, xpr, currency),
              als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));
    }
    // Costs
    if (report.requiresField(dailyCosts) || report.requiresField(roadCosts)
        || report.requiresField(otherCosts) || report.requiresField(fuelCosts)) {
      String costs = getTripCosts(new SqlSelect()
          .addFields(tmp, COL_TRIP)
          .addFrom(tmp), currency, woVat);

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(dailyCosts, SqlUtils.field(costs, dailyCosts))
          .addExpression(roadCosts, SqlUtils.field(costs, roadCosts))
          .addExpression(otherCosts, SqlUtils.field(costs, otherCosts))
          .addExpression(fuelCosts, SqlUtils.field(costs, fuelCosts))
          .setFrom(costs, SqlUtils.joinUsing(tmp, costs, COL_TRIP)));

      qs.sqlDropTemp(costs);
    }
    // Constant costs
    if (report.requiresField(constantCosts)) {
      String als = SqlUtils.uniqueName();

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(constantCosts, SqlUtils.field(als, constantCosts))
          .setFrom(getConstantsQuery(tmp, TBL_TRIP_CONSTANTS,
              SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
                  TripConstant.CONSTANT_COSTS.ordinal()), constantCosts, null, currency),
              als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));
    }
    // Cargo info
    String orderDate = COL_ORDER + COL_ORDER_DATE;

    boolean cargoRequired = report.requiresField(COL_ORDER_NO)
        || report.requiresField(orderDate) || report.requiresField(COL_CUSTOMER)
        || report.requiresField(COL_ORDER_MANAGER) || report.requiresField(COL_CARGO)
        || report.requiresField(COL_CARGO_PARTIAL);

    if (cargoRequired) {
      String tmpTripCargo = getCargoTripPercents(COL_TRIP,
          new SqlSelect()
              .addFields(tmp, COL_TRIP)
              .addFrom(tmp));

      clause.clear();
      clause.add(report.getCondition(TBL_ORDERS, COL_ORDER_NO));
      clause.add(report.getCondition(SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE), orderDate));
      clause.add(report.getCondition(SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME),
          COL_CUSTOMER));
      clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(tmpTripCargo, COL_CARGO),
          SqlConstants.SqlDataType.STRING, 20, 0), COL_CARGO));
      clause.add(report.getCondition(TBL_ORDER_CARGO, COL_CARGO_PARTIAL));

      query = new SqlSelect()
          .addFields(TBL_ORDERS, COL_ORDER_NO)
          .addField(TBL_ORDERS, COL_ORDER_DATE, orderDate)
          .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER)
          .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
              SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")),
              COL_ORDER_MANAGER)
          .addFields(tmpTripCargo, COL_CARGO)
          .addFields(TBL_ORDER_CARGO, COL_CARGO_PARTIAL)
          .addFrom(tmp)
          .addFromInner(tmpTripCargo, SqlUtils.joinUsing(tmp, tmpTripCargo, COL_TRIP))
          .addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, tmpTripCargo, COL_CARGO))
          .addFromInner(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
          .addFromInner(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
          .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER))
          .addFromLeft(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
          .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
          .setWhere(clause);

      for (String column : qs.getData(new SqlSelect()
          .addAllFields(tmp)
          .addFrom(tmp)
          .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

        if (BeeUtils.inList(column, kilometers, fuelCosts, dailyCosts, roadCosts, constantCosts,
            otherCosts, plannedKilometers, plannedFuelCosts, plannedDailyCosts, plannedRoadCosts)) {

          query.addExpr(SqlUtils.multiply(SqlUtils.divide(SqlUtils.field(tmp, column), 100),
              SqlUtils.field(tmpTripCargo, COL_CARGO_PERCENT)), column);
        } else {
          query.addFields(tmp, column);
        }
      }
      String tt = qs.sqlCreateTemp(query);
      qs.sqlDropTemp(tmp);
      qs.sqlDropTemp(tmpTripCargo);
      tmp = tt;
    }
    // Incomes
    if (report.requiresField(tripIncome)) {
      String als = SqlUtils.uniqueName();

      String tripIncomes = getTripIncomes(new SqlSelect().setDistinctMode(true)
          .addFields(tmp, COL_TRIP)
          .addFrom(tmp), currency, woVat);

      if (cargoRequired) {
        qs.updateData(new SqlUpdate(tmp)
            .addExpression(tripIncome, SqlUtils.field(tripIncomes, tripIncome))
            .setFrom(tripIncomes, SqlUtils.joinUsing(tmp, tripIncomes, COL_CARGO, COL_TRIP)));
      } else {
        qs.updateData(new SqlUpdate(tmp)
            .addExpression(tripIncome, SqlUtils.field(als, tripIncome))
            .setFrom(new SqlSelect()
                .addFields(tripIncomes, COL_TRIP)
                .addSum(tripIncomes, tripIncome)
                .addFrom(tripIncomes)
                .addFromInner(tmp, SqlUtils.joinUsing(tripIncomes, tmp, COL_TRIP))
                .addGroup(tripIncomes, COL_TRIP), als,
                SqlUtils.joinUsing(tmp, als, COL_TRIP)));
      }
      qs.sqlDropTemp(tripIncomes);
    }
    query = new SqlSelect()
        .addFrom(tmp)
        .setWhere(SqlUtils.and(report.getCondition(tmp, COL_TRIP_ROUTE),
            report.getCondition(tmp, COL_ORDER_MANAGER)));

    for (String column : qs.getData(new SqlSelect()
        .addAllFields(tmp)
        .addFrom(tmp)
        .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

      if (report.requiresField(column)) {
        query.addFields(tmp, column);
      }
    }
    SimpleRowSet data = qs.getData(query);
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(data);
  }

  /**
   * Return Temporary table name with calculated trip routes.
   *
   * @param trips query filter with <b>unique</b> "Trip" values.
   * @return Temporary table name with following structure: <br>
   *         "Trip" - trip ID <br>
   *         "Routes" - trip route <br>
   */
  public String getTripRoutes(SqlSelect trips) {
    String als = SqlUtils.uniqueName();
    String lastRoute = SqlUtils.uniqueName();
    String cnt = SqlUtils.uniqueName();

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addFields(als, COL_TRIP)
        .addEmptyString(COL_TRIP_ROUTE, sys.getFieldPrecision(TBL_TRIPS, COL_TRIP_ROUTE))
        .addEmptyString(lastRoute, sys.getFieldPrecision(TBL_COUNTRIES, COL_COUNTRY_NAME))
        .addFrom(trips, als));

    String rTmp = qs.sqlCreateTemp(new SqlSelect().setUnionAllMode(false)
        .addFields(TBL_TRIP_ROUTES, COL_TRIP)
        .addField(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE, COL_DATE)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_CODE),
            SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_NAME)), COL_COUNTRY_CODE)
        .addFrom(tmp)
        .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(tmp, TBL_TRIP_ROUTES, COL_TRIP))
        .addFromInner(TBL_COUNTRIES,
            sys.joinTables(TBL_COUNTRIES, TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_COUNTRY))
        .addUnion(new SqlSelect()
            .addFields(TBL_TRIP_ROUTES, COL_TRIP)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE),
                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE)), COL_DATE)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_CODE),
                SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_NAME)), COL_COUNTRY_CODE)
            .addFrom(tmp)
            .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(tmp, TBL_TRIP_ROUTES, COL_TRIP))
            .addFromInner(TBL_COUNTRIES,
                sys.joinTables(TBL_COUNTRIES, TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_COUNTRY))
        ));
    String routes = qs.sqlCreateTemp(new SqlSelect()
        .addFields(rTmp, COL_TRIP, COL_COUNTRY_CODE)
        .addCount(cnt)
        .addFrom(rTmp)
        .addFromInner(rTmp, als, SqlUtils.and(SqlUtils.joinUsing(rTmp, als, COL_TRIP),
            SqlUtils.or(SqlUtils.joinMore(rTmp, COL_DATE, als, COL_DATE),
                SqlUtils.and(SqlUtils.joinUsing(rTmp, als, COL_DATE),
                    SqlUtils.joinMoreEqual(rTmp, COL_COUNTRY_CODE, als, COL_COUNTRY_CODE)))))
        .addGroup(rTmp, COL_TRIP, COL_DATE, COL_COUNTRY_CODE));

    qs.sqlDropTemp(rTmp);

    int c = BeeUtils.unbox(qs.getInt(new SqlSelect().addMax(routes, cnt).addFrom(routes)));

    if (c > 0) {
      qs.updateData(new SqlUpdate(tmp)
          .addExpression(COL_TRIP_ROUTE, SqlUtils.field(routes, COL_COUNTRY_CODE))
          .addExpression(lastRoute, SqlUtils.field(routes, COL_COUNTRY_CODE))
          .setFrom(routes, SqlUtils.and(SqlUtils.joinUsing(tmp, routes, COL_TRIP),
              SqlUtils.equals(routes, cnt, 1))));

      for (int i = 2; i <= c; i++) {
        qs.updateData(new SqlUpdate(tmp)
            .addExpression(COL_TRIP_ROUTE,
                SqlUtils.concat(SqlUtils.field(tmp, COL_TRIP_ROUTE), "'-'",
                    SqlUtils.field(routes, COL_COUNTRY_CODE)))
            .addExpression(lastRoute, SqlUtils.field(routes, COL_COUNTRY_CODE))
            .setFrom(routes, SqlUtils.and(SqlUtils.joinUsing(tmp, routes, COL_TRIP),
                SqlUtils.equals(routes, cnt, i),
                SqlUtils.notEqual(tmp, lastRoute, SqlUtils.field(routes, COL_COUNTRY_CODE)))));
      }
    }
    qs.sqlDropTemp(routes);
    return tmp;
  }

  /**
   * Return SqlSelect query, calculating cargo costs from CargoServices table.
   *
   * @param cargos query filter with <b>unique</b> "Cargo" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return query with columns: "Cargo", "Expense"
   */
  private SqlSelect getCargoCostQuery(SqlSelect cargos, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromInner(cargos, alias, sys.joinTables(TBL_ORDER_CARGO, alias, COL_CARGO))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_EXPENSES,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_EXPENSES, COL_CARGO))
        .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO));

    IsExpression amountExpr = SqlUtils.field(TBL_CARGO_EXPENSES, COL_AMOUNT);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_CARGO_EXPENSES, amountExpr);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_CARGO_EXPENSES, amountExpr);
    }
    IsExpression currencyExpr = SqlUtils.field(TBL_CARGO_EXPENSES, COL_CURRENCY);
    IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_CARGO_EXPENSES, COL_DATE),
        SqlUtils.field(TBL_ORDERS, COL_DATE));

    if (DataUtils.isId(currency)) {
      amountExpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      amountExpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    ss.addSum(amountExpr, VAR_EXPENSE);

    return ss;
  }

  /**
   * Returns Temporary table name with calculated trip income or cargo cost percents.
   *
   * @param key "Cargo" or "Trip"
   * @param filter query filter with <b>unique</b> key values.
   * @return Temporary table name with following structure: <br>
   *         "Cargo" - cargo ID <br>
   *         "Trip" - trip ID <br>
   *         key == "Cargo" ? "TripPercent" : "CargoPercent"
   */
  private String getCargoTripPercents(String key, SqlSelect filter) {
    String percent = null;

    switch (key) {
      case COL_CARGO:
        percent = COL_TRIP_PERCENT;
        break;
      case COL_TRIP:
        percent = COL_CARGO_PERCENT;
        break;
      default:
        Assert.unsupported(key + ": only " + COL_CARGO + " and " + COL_TRIP + " values supported");
    }
    String alias = SqlUtils.uniqueName();

    String tmpCargoTrip = qs.sqlCreateTemp(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, COL_CARGO, COL_TRIP, percent)
        .addSum(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(filter, alias, SqlUtils.joinUsing(TBL_CARGO_TRIPS, alias, key))
        .addFromLeft(TBL_TRIP_ROUTES,
            sys.joinTables(TBL_CARGO_TRIPS, TBL_TRIP_ROUTES, COL_ROUTE_CARGO))
        .addGroup(TBL_CARGO_TRIPS, COL_CARGO, COL_TRIP, percent));

    // Too big percent correction
    qs.updateData(new SqlUpdate(tmpCargoTrip)
        .addExpression(percent, SqlUtils.multiply(
            SqlUtils.divide(SqlUtils.field(tmpCargoTrip, percent),
                SqlUtils.field(alias, percent)), 100))
        .setFrom(new SqlSelect()
            .addFields(tmpCargoTrip, key)
            .addSum(tmpCargoTrip, percent)
            .addFrom(tmpCargoTrip)
            .addGroup(tmpCargoTrip, key), alias,
            SqlUtils.and(SqlUtils.joinUsing(tmpCargoTrip, alias, key),
                SqlUtils.notNull(alias, percent), SqlUtils.more(alias, percent, 100))));

    // Percent correction by runned kilometers
    qs.updateData(new SqlUpdate(tmpCargoTrip)
        .addExpression(percent, SqlUtils.multiply(
            SqlUtils.divide(SqlUtils.field(tmpCargoTrip, COL_ROUTE_KILOMETERS),
                SqlUtils.field(alias, COL_ROUTE_KILOMETERS)),
            SqlUtils.minus(100, SqlUtils.nvl(SqlUtils.field(alias, percent), 0))))
        .setFrom(new SqlSelect()
            .addFields(tmpCargoTrip, key)
            .addSum(SqlUtils.sqlIf(SqlUtils.isNull(tmpCargoTrip, percent),
                SqlUtils.field(tmpCargoTrip, COL_ROUTE_KILOMETERS), null), COL_ROUTE_KILOMETERS)
            .addSum(tmpCargoTrip, percent)
            .addFrom(tmpCargoTrip)
            .addGroup(tmpCargoTrip, key), alias,
            SqlUtils.and(SqlUtils.joinUsing(alias, tmpCargoTrip, key),
                SqlUtils.notNull(alias, COL_ROUTE_KILOMETERS),
                SqlUtils.positive(alias, COL_ROUTE_KILOMETERS)))
        .setWhere(SqlUtils.isNull(tmpCargoTrip, percent)));

    String cntEmpty = SqlUtils.uniqueName();

    // Percent correction proportionaly for empty percents
    qs.updateData(new SqlUpdate(tmpCargoTrip)
        .addExpression(percent, SqlUtils.divide(
            SqlUtils.minus(100, SqlUtils.nvl(SqlUtils.field(alias, percent), 0)),
            SqlUtils.field(alias, cntEmpty)))
        .setFrom(new SqlSelect()
            .addFields(tmpCargoTrip, key)
            .addSum(tmpCargoTrip, percent)
            .addSum(SqlUtils.sqlIf(SqlUtils.isNull(tmpCargoTrip, percent), 1, 0), cntEmpty)
            .addFrom(tmpCargoTrip)
            .addGroup(tmpCargoTrip, key), alias, SqlUtils.joinUsing(tmpCargoTrip, alias, key))
        .setWhere(SqlUtils.isNull(tmpCargoTrip, percent)));

    return tmpCargoTrip;
  }

  private static SqlSelect getConstantsQuery(String tmp, String src, IsCondition join, String col,
      IsExpression expression, Long currency) {

    SqlSelect ss = new SqlSelect()
        .addFields(tmp, COL_TRIP)
        .addFrom(tmp)
        .addFromInner(src, SqlUtils.and(join,
            SqlUtils.notNull(tmp, COL_TRIP_DATE_FROM),
            SqlUtils.notNull(tmp, COL_TRIP_DATE_TO),
            SqlUtils.joinMore(tmp, COL_TRIP_DATE_TO, tmp, COL_TRIP_DATE_FROM),
            SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_FROM),
                SqlUtils.joinLess(src, COL_TRIP_DATE_FROM, tmp, COL_TRIP_DATE_TO)),
            SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(src, COL_TRIP_DATE_TO, tmp, COL_TRIP_DATE_FROM))))
        .addGroup(tmp, COL_TRIP);

    IsExpression xpr = SqlUtils.multiply(SqlUtils.divide(SqlUtils.minus(
        SqlUtils.sqlIf(SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_TO),
            SqlUtils.joinMore(src, COL_TRIP_DATE_TO, tmp, COL_TRIP_DATE_TO)),
            SqlUtils.field(tmp, COL_TRIP_DATE_TO),
            SqlUtils.field(src, COL_TRIP_DATE_TO)),
        SqlUtils.sqlIf(SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_FROM),
            SqlUtils.joinLess(src, COL_TRIP_DATE_FROM, tmp, COL_TRIP_DATE_FROM)),
            SqlUtils.field(tmp, COL_TRIP_DATE_FROM),
            SqlUtils.field(src, COL_TRIP_DATE_FROM))),
        TimeUtils.MILLIS_PER_DAY), SqlUtils.field(src, COL_CARGO_VALUE));

    if (expression != null) {
      xpr = SqlUtils.multiply(expression, xpr);
    }
    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, xpr,
          SqlUtils.field(src, COL_CURRENCY), SqlUtils.field(tmp, COL_TRIP_DATE),
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, xpr,
          SqlUtils.field(src, COL_CURRENCY), SqlUtils.field(tmp, COL_TRIP_DATE));
    }
    return ss.addSum(xpr, col);
  }

  /**
   * Return Temporary table name with calculated trip costs.
   *
   * @param trips query filter with <b>unique</b> "Trip" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return Temporary table name with following structure: <br>
   *         "Trip" - trip ID <br>
   *         "DailyCosts" - total trip daily costs <br>
   *         "RoadCosts" - total trip road costs <br>
   *         "OtherCosts" - total trip other costs <br>
   *         "FuelCosts" - total trip fuel costs considering remainder corrections
   */
  private String getTripCosts(SqlSelect trips, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();

    // Trip costs
    SqlSelect ss = new SqlSelect()
        .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
        .addField(TBL_TRIPS, "Date", "TripDate")
        .addFields(TBL_TRIPS, COL_VEHICLE, "FuelBefore", "FuelAfter")
        .addEmptyDouble("FuelCosts")
        .addFrom(TBL_TRIPS)
        .addFromInner(trips, alias, sys.joinTables(TBL_TRIPS, alias, COL_TRIP))
        .addFromLeft(TBL_TRIP_COSTS, sys.joinTables(TBL_TRIPS, TBL_TRIP_COSTS, COL_TRIP))
        .addGroup(TBL_TRIPS, sys.getIdName(TBL_TRIPS), "Date", COL_VEHICLE, "FuelBefore",
            "FuelAfter");

    IsExpression amountExpr;

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_COSTS);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_COSTS);
    }
    IsExpression currencyExpr = SqlUtils.field(TBL_TRIP_COSTS, COL_CURRENCY);
    IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_DATE),
        SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE));

    IsExpression xpr;

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    IsCondition dailyCond = SqlUtils.inList(TBL_TRIP_COSTS, COL_ITEM,
        qs.getLongList(new SqlSelect().setDistinctMode(true)
            .addFields(TBL_COUNTRY_NORMS, COL_DAILY_COSTS_ITEM)
            .addFrom(TBL_COUNTRY_NORMS)));

    if (dailyCond != null) {
      ss.addSum(SqlUtils.sqlIf(dailyCond, xpr, null), "DailyCosts");
    } else {
      ss.addEmptyDouble("DailyCosts");
    }
    IsCondition roadCond = SqlUtils.inList(TBL_TRIP_COSTS, COL_ITEM,
        qs.getLongList(new SqlSelect().setDistinctMode(true)
            .addFields(TBL_COUNTRY_NORMS, COL_ROAD_COSTS_ITEM)
            .addFrom(TBL_COUNTRY_NORMS)));

    if (roadCond != null) {
      ss.addSum(SqlUtils.sqlIf(roadCond, xpr, null), "RoadCosts");
    } else {
      ss.addEmptyDouble("RoadCosts");
    }
    if (BeeUtils.anyNotNull(dailyCond, roadCond)) {
      ss.addSum(SqlUtils.sqlIf(SqlUtils.or(dailyCond, roadCond), null, xpr), "OtherCosts");
    } else {
      ss.addSum(xpr, "OtherCosts");
    }
    String tmpCosts = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmpCosts, COL_TRIP);

    // Fuel costs
    ss = new SqlSelect()
        .addFields(tmpCosts, COL_TRIP)
        .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
        .addFrom(tmpCosts)
        .addFromLeft(TBL_TRIP_FUEL_COSTS,
            SqlUtils.joinUsing(tmpCosts, TBL_TRIP_FUEL_COSTS, COL_TRIP))
        .addGroup(tmpCosts, COL_TRIP);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_FUEL_COSTS);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_FUEL_COSTS);
    }
    currencyExpr = SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_CURRENCY);
    dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE),
        SqlUtils.field(tmpCosts, "TripDate"));

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    ss.addSum(xpr, "FuelCosts");

    String tmp = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmp, COL_TRIP);

    qs.updateData(new SqlUpdate(tmpCosts)
        .setFrom(tmp, SqlUtils.joinUsing(tmpCosts, tmp, COL_TRIP))
        .addExpression("FuelCosts", SqlUtils.field(tmp, "FuelCosts")));

    // Fuel consumptions
    if (qs.sqlExists(tmpCosts, SqlUtils.isNull(tmpCosts, "FuelAfter"))) {
      String tmpRoutes = qs.sqlCreateTemp(getFuelConsumptionsQuery(new SqlSelect()
          .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
          .addFrom(TBL_TRIP_ROUTES)
          .addFromInner(tmpCosts, SqlUtils.joinUsing(TBL_TRIP_ROUTES, tmpCosts, COL_TRIP)), false));
      qs.sqlIndex(tmpRoutes, COL_TRIP);

      String tmpConsumptions = qs.sqlCreateTemp(new SqlSelect()
          .addFields(TBL_TRIP_FUEL_CONSUMPTIONS, COL_TRIP)
          .addSum(TBL_TRIP_FUEL_CONSUMPTIONS, "Quantity")
          .addFrom(TBL_TRIP_FUEL_CONSUMPTIONS)
          .addFromInner(tmpCosts,
              SqlUtils.joinUsing(TBL_TRIP_FUEL_CONSUMPTIONS, tmpCosts, COL_TRIP))
          .addGroup(TBL_TRIP_FUEL_CONSUMPTIONS, COL_TRIP));

      qs.sqlIndex(tmpConsumptions, COL_TRIP);

      qs.updateData(new SqlUpdate(tmpCosts)
          .setFrom(new SqlSelect()
              .addFields(tmp, COL_TRIP, "Quantity")
              .addField(tmpRoutes, "Quantity", "routeQuantity")
              .addField(tmpConsumptions, "Quantity", "consumeQuantity")
              .addFrom(tmp)
              .addFromLeft(tmpRoutes, SqlUtils.joinUsing(tmp, tmpRoutes, COL_TRIP))
              .addFromLeft(tmpConsumptions, SqlUtils.joinUsing(tmp, tmpConsumptions, COL_TRIP)),
              "sub", SqlUtils.joinUsing(tmpCosts, "sub", COL_TRIP))
          .addExpression("FuelAfter", SqlUtils.minus(
              SqlUtils.plus(
                  SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelBefore"), 0),
                  SqlUtils.nvl(SqlUtils.field("sub", "Quantity"), 0)),
              SqlUtils.nvl(SqlUtils.field("sub", "routeQuantity"), 0),
              SqlUtils.nvl(SqlUtils.field("sub", "consumeQuantity"), 0)))
          .setWhere(SqlUtils.isNull(tmpCosts, "FuelAfter")));

      qs.sqlDropTemp(tmpRoutes);
      qs.sqlDropTemp(tmpConsumptions);
    }
    qs.sqlDropTemp(tmp);

    // Fuel cost correction
    ss =
        new SqlSelect()
            .addFields(TBL_TRIPS, COL_VEHICLE)
            .addField(TBL_TRIPS, "Date", "TripDate")
            .addFields(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE)
            .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
            .addFrom(TBL_TRIPS)
            .addFromInner(TBL_TRIP_FUEL_COSTS,
                sys.joinTables(TBL_TRIPS, TBL_TRIP_FUEL_COSTS, COL_TRIP))
            .addFromInner(new SqlSelect()
                .addFields(TBL_TRIPS, COL_VEHICLE)
                .addMax(TBL_TRIPS, "Date", "MaxDate")
                .addFrom(TBL_TRIPS)
                .addFromInner(tmpCosts, sys.joinTables(TBL_TRIPS, tmpCosts, COL_TRIP))
                .addGroup(TBL_TRIPS, COL_VEHICLE), "sub",
                SqlUtils.and(SqlUtils.joinUsing(TBL_TRIPS, "sub", COL_VEHICLE),
                    SqlUtils.joinLessEqual(TBL_TRIPS, "Date", "sub", "MaxDate"),
                    SqlUtils.and(SqlUtils.positive(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY),
                        SqlUtils.positive(TBL_TRIP_FUEL_COSTS, COL_COSTS_PRICE))))
            .addGroup(TBL_TRIPS, COL_VEHICLE, "Date")
            .addGroup(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_FUEL_COSTS);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_FUEL_COSTS);
    }
    currencyExpr = SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_CURRENCY);
    dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE),
        SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE));

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    ss.addSum(xpr, "Sum");

    String tmpFuels = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmpFuels, COL_VEHICLE);

    for (int i = 0; i < 2; i++) {
      boolean plusMode = i == 0;
      String fld = plusMode ? "FuelBefore" : "FuelAfter";

      tmp = qs.sqlCreateTemp(new SqlSelect()
          .addFields(tmpCosts, COL_TRIP, "TripDate", COL_VEHICLE)
          .addField(tmpCosts, fld, "Remainder")
          .addEmptyDate("Date")
          .addEmptyDouble("Cost")
          .addFrom(tmpCosts)
          .setWhere(SqlUtils.positive(tmpCosts, fld)));

      qs.sqlIndex(tmp, COL_TRIP, COL_VEHICLE);
      int c = 0;

      IsCondition cond = plusMode
          ? SqlUtils.joinLess(tmpFuels, "TripDate", tmp, "TripDate")
          : SqlUtils.joinLessEqual(tmpFuels, "TripDate", tmp, "TripDate");

      do {
        String tmp2 = qs.sqlCreateTemp(new SqlSelect()
            .addFields(tmp, COL_VEHICLE, "TripDate")
            .addMax(tmpFuels, "Date")
            .addFrom(tmp)
            .addFromInner(tmpFuels, SqlUtils.joinUsing(tmp, tmpFuels, COL_VEHICLE))
            .setWhere(SqlUtils.and(cond,
                SqlUtils.or(SqlUtils.isNull(tmp, "Date"),
                    SqlUtils.joinLess(tmpFuels, "Date", tmp, "Date")),
                SqlUtils.positive(tmp, "Remainder")))
            .addGroup(tmp, COL_VEHICLE, "TripDate"));

        qs.sqlIndex(tmp2, COL_VEHICLE);

        c = qs.updateData(new SqlUpdate(tmp)
            .setFrom(new SqlSelect()
                .addFields(tmp2, COL_VEHICLE, "TripDate", "Date")
                .addFields(tmpFuels, "Quantity", "Sum")
                .addFrom(tmp2)
                .addFromInner(tmpFuels,
                    SqlUtils.joinUsing(tmp2, tmpFuels, COL_VEHICLE, "Date")),
                "sub", SqlUtils.joinUsing(tmp, "sub", COL_VEHICLE, "TripDate"))
            .addExpression("Date", SqlUtils.field("sub", "Date"))
            .addExpression("Cost", SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0),
                SqlUtils.sqlIf(SqlUtils.joinLess(tmp, "Remainder", "sub", "Quantity"),
                    SqlUtils.multiply(SqlUtils.field(tmp, "Remainder"),
                        SqlUtils.divide((Object[]) SqlUtils.fields("sub", "Sum", "Quantity"))),
                    SqlUtils.field("sub", "Sum"))))
            .addExpression("Remainder", SqlUtils.minus(SqlUtils.field(tmp, "Remainder"),
                SqlUtils.field("sub", "Quantity")))
            .setWhere(SqlUtils.positive(tmp, "Remainder")));

        qs.sqlDropTemp(tmp2);

      } while (BeeUtils.isPositive(c));

      IsExpression expr = plusMode
          ? SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelCosts"), 0),
              SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0))
          : SqlUtils.minus(SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelCosts"), 0),
              SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0));

      qs.updateData(new SqlUpdate(tmpCosts)
          .setFrom(tmp, SqlUtils.joinUsing(tmpCosts, tmp, COL_TRIP))
          .addExpression("FuelCosts", expr));

      qs.sqlDropTemp(tmp);
    }
    qs.sqlDropTemp(tmpFuels);

    return tmpCosts;
  }
}
