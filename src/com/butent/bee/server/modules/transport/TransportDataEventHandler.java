package com.butent.bee.server.modules.transport;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class TransportDataEventHandler implements DataEventHandler {

  @EJB SystemBean sys;
  @EJB QueryServiceBean qs;

  @Subscribe
  @AllowConcurrentEvents
  public void fillAssessmentProperties(DataEvent.ViewQueryEvent event) {
    if (event.isAfter(TBL_ASSESSMENTS) && event.hasData()) {
      BeeRowSet rowSet = event.getRowset();

      SqlSelect query = new SqlSelect()
          .addFields(TBL_ASSESSMENT_FORWARDERS, COL_ASSESSMENT)
          .addFields(TBL_TRIPS, COL_FORWARDER_VEHICLE)
          .addFrom(TBL_ASSESSMENT_FORWARDERS)
          .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_ASSESSMENT_FORWARDERS, COL_TRIP))
          .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_TRIPS, COL_FORWARDER_VEHICLE),
              SqlUtils.inList(TBL_ASSESSMENT_FORWARDERS, COL_ASSESSMENT, rowSet.getRowIds())));

      Multimap<Long, String> vehicles = HashMultimap.create();
      qs.getData(query).forEach(row -> vehicles.put(row.getLong(0), row.getValue(1)));

      int cargoIdx = rowSet.getColumnIndex(COL_CARGO);
      Set<Long> cargoIds = rowSet.getDistinctLongs(cargoIdx);

      SimpleRowSet minUnloadingDate = qs.getData(new SqlSelect()
          .addFields(TBL_CARGO_UNLOADING, COL_CARGO)
          .addMin(TBL_CARGO_PLACES, COL_PLACE_DATE)
          .addFrom(TBL_CARGO_UNLOADING)
          .addFromInner(TBL_CARGO_PLACES,
              sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE))
          .setWhere(SqlUtils.inList(TBL_CARGO_UNLOADING, COL_CARGO, cargoIds))
          .addGroup(TBL_CARGO_UNLOADING, COL_CARGO));

      SimpleRowSet incomesCommited = qs.getData(new SqlSelect()
          .addFields(TBL_CARGO_INCOMES, COL_CARGO)
          .addFrom(TBL_CARGO_INCOMES)
          .setWhere(SqlUtils.inList(TBL_CARGO_INCOMES, COL_CARGO, cargoIds))
          .addGroup(TBL_CARGO_INCOMES, COL_CARGO)
          .setHaving(SqlUtils.equals(SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              SqlUtils.name(COL_SALE)), SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT, null))));

      SimpleRowSet expensesCommited = qs.getData(new SqlSelect()
          .addFields(TBL_CARGO_EXPENSES, COL_CARGO)
          .addFrom(TBL_CARGO_EXPENSES)
          .setWhere(SqlUtils.inList(TBL_CARGO_EXPENSES, COL_CARGO, cargoIds))
          .addGroup(TBL_CARGO_EXPENSES, COL_CARGO)
          .setHaving(SqlUtils.equals(SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              SqlUtils.name(COL_PURCHASE)), SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              null))));

      for (BeeRow row : rowSet.getRows()) {
        if (vehicles.containsKey(row.getId())) {
          row.setProperty(COL_FORWARDER_VEHICLE, BeeUtils.joinItems(vehicles.get(row.getId())));
        }
        String cargo = row.getString(cargoIdx);

        row.setProperty(PROP_MIN_UNLOADING_DATE,
            minUnloadingDate.getValueByKey(COL_CARGO, cargo, COL_PLACE_DATE));
        row.setProperty(PROP_INCOMES_COMMITED,
            Objects.isNull(incomesCommited.getRowByKey(COL_CARGO, cargo)) ? null : 1);
        row.setProperty(PROP_EXPENSES_COMMITED,
            Objects.isNull(expensesCommited.getRowByKey(COL_CARGO, cargo)) ? null : 1);
      }
    }
  }

  public void initConditions() {
    BeeView.registerConditionProvider(TBL_ASSESSMENTS, (view, args) -> {
      String col = BeeUtils.getQuietly(args, 0);
      String val = BeeUtils.getQuietly(args, 1);

      if (BeeUtils.anyEmpty(col, val)) {
        return null;
      }
      IsCondition clause = null;

      switch (col) {
        case COL_FORWARDER_VEHICLE:
          clause = SqlUtils.in(TBL_ASSESSMENTS, sys.getIdName(TBL_ASSESSMENTS),
              new SqlSelect()
                  .addFields(TBL_ASSESSMENT_FORWARDERS, COL_ASSESSMENT)
                  .addFrom(TBL_ASSESSMENT_FORWARDERS)
                  .addFromInner(TBL_TRIPS,
                      sys.joinTables(TBL_TRIPS, TBL_ASSESSMENT_FORWARDERS, COL_TRIP))
                  .setWhere(SqlUtils.contains(TBL_TRIPS, COL_FORWARDER_VEHICLE, val)));
          break;

        case PROP_MIN_UNLOADING_DATE:
          String[] split = val.split(BeeConst.STRING_COMMA, 2);
          String start = ArrayUtils.getQuietly(split, 0);
          String end = ArrayUtils.getQuietly(split, 1);

          IsCondition dateClause = SqlUtils.notNull(TBL_CARGO_UNLOADING, COL_CARGO);

          if (!BeeUtils.isEmpty(start)) {
            dateClause = SqlUtils.and(dateClause,
                SqlUtils.moreEqual(TBL_CARGO_PLACES, COL_PLACE_DATE, start));
          }
          if (!BeeUtils.isEmpty(end)) {
            dateClause = SqlUtils.and(dateClause,
                SqlUtils.lessEqual(TBL_CARGO_PLACES, COL_PLACE_DATE, end));
          }
          clause = SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO,
              new SqlSelect().setDistinctMode(true)
                  .addFields(TBL_CARGO_UNLOADING, COL_CARGO)
                  .addFrom(TBL_CARGO_UNLOADING)
                  .addFromInner(TBL_CARGO_PLACES,
                      sys.joinTables(TBL_CARGO_PLACES, TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE))
                  .setWhere(dateClause));
          break;

        case PROP_INCOMES_COMMITED:
          IsExpression commitedCount = SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              SqlUtils.field(TBL_CARGO_INCOMES, COL_SALE));
          IsExpression allCount = SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              SqlUtils.field(TBL_ASSESSMENTS, COL_CARGO));

          clause = SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO,
              new SqlSelect()
                  .addFields(TBL_ASSESSMENTS, COL_CARGO)
                  .addFrom(TBL_ASSESSMENTS)
                  .addFromLeft(TBL_CARGO_INCOMES,
                      SqlUtils.joinUsing(TBL_ASSESSMENTS, TBL_CARGO_INCOMES, COL_CARGO))
                  .addGroup(TBL_ASSESSMENTS, COL_CARGO)
                  .setHaving(BeeUtils.toBoolean(val) ? SqlUtils.equals(commitedCount, allCount)
                      : SqlUtils.less(commitedCount, allCount)));
          break;

        case PROP_EXPENSES_COMMITED:
          commitedCount = SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              SqlUtils.field(TBL_CARGO_EXPENSES, COL_PURCHASE));
          allCount = SqlUtils.aggregate(SqlConstants.SqlFunction.COUNT,
              SqlUtils.field(TBL_ASSESSMENTS, COL_CARGO));

          clause = SqlUtils.in(TBL_ASSESSMENTS, COL_CARGO,
              new SqlSelect()
                  .addFields(TBL_ASSESSMENTS, COL_CARGO)
                  .addFrom(TBL_ASSESSMENTS)
                  .addFromLeft(TBL_CARGO_EXPENSES,
                      SqlUtils.joinUsing(TBL_ASSESSMENTS, TBL_CARGO_EXPENSES, COL_CARGO))
                  .addGroup(TBL_ASSESSMENTS, COL_CARGO)
                  .setHaving(BeeUtils.toBoolean(val) ? SqlUtils.equals(commitedCount, allCount)
                      : SqlUtils.less(commitedCount, allCount)));
          break;
      }
      return clause;
    });
  }
}
