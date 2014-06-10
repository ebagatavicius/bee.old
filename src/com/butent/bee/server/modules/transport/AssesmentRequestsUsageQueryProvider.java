package com.butent.bee.server.modules.transport;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.modules.transport.TransportConstants.OrderStatus;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.time.DateTime;

public class AssesmentRequestsUsageQueryProvider implements UsageQueryProvider {

  private boolean userMode;
  private boolean orderMode;

  @Override
  public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
      DateTime startDate) {

    SqlSelect select = new SqlSelect()
        .addFields(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT)
        .addMax(NewsConstants.getUsageTable(TBL_ASSESSMENTS), NewsConstants.COL_USAGE_ACCESS)
        .addFrom(NewsConstants.getUsageTable(TBL_ASSESSMENTS))
        .addFromInner(TBL_ASSESSMENTS,
            SqlUtils.join(TBL_ASSESSMENTS, COL_ASSESSMENT_ID,
                NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT))
        .addFromInner(TBL_ORDER_CARGO,
            SqlUtils.join(TBL_ORDER_CARGO, COL_CARGO_ID, TBL_ASSESSMENTS, COL_CARGO))
        .addFromInner(TBL_ORDERS,
            SqlUtils.join(TBL_ORDERS, COL_ORDER_ID, TBL_ORDER_CARGO, COL_ORDER))
        .addGroup(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT)
        .setWhere(SqlUtils.and(
            SqlUtils.equals(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_USER, userId),
            SqlUtils.notNull(NewsConstants.getUsageTable(TBL_ASSESSMENTS),
                NewsConstants.COL_USAGE_ACCESS)
            ));

    if (userMode) {
      select.setWhere(SqlUtils.and(select.getWhere(),
          SqlUtils.equals(TBL_ORDERS, COL_ORDER_MANAGER, userId)));
    }

    if (orderMode) {
      select.setWhere(SqlUtils.and(select.getWhere(),
          SqlUtils.notEqual(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST.ordinal())));
    } else {
      select.setWhere(SqlUtils.and(select.getWhere(),
          SqlUtils.equals(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST.ordinal())));
    }

    return select;
  }

  @Override
  public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
      DateTime startDate) {
    SqlSelect selectFromChild = new SqlSelect()
        .addFields(TBL_ASSESSMENTS, COL_ASSESSMENT)
        .addMax(NewsConstants.getUsageTable(TBL_ASSESSMENTS), NewsConstants.COL_USAGE_UPDATE)
        .addFrom(TBL_ASSESSMENTS)
        .addFromInner(NewsConstants.getUsageTable(TBL_ASSESSMENTS),
            SqlUtils.join(TBL_ASSESSMENTS, COL_ASSESSMENT_ID,
                NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT))
        .addFromInner(TBL_ORDER_CARGO,
            SqlUtils.join(TBL_ORDER_CARGO, COL_CARGO_ID, TBL_ASSESSMENTS, COL_CARGO))
        .addFromInner(TBL_ORDERS,
            SqlUtils.join(TBL_ORDERS, COL_ORDER_ID, TBL_ORDER_CARGO, COL_ORDER))
        .addGroup(TBL_ASSESSMENTS, COL_ASSESSMENT)
        .setWhere(SqlUtils.and(
            SqlUtils.notEqual(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_USER, userId),
            SqlUtils.more(NewsConstants.getUsageTable(TBL_ASSESSMENTS),
                NewsConstants.COL_USAGE_UPDATE, NewsHelper.getStartTime(startDate)),
            SqlUtils.notNull(TBL_ASSESSMENTS, COL_ASSESSMENT)
            ));

    if (userMode) {
      selectFromChild.setWhere(SqlUtils.and(selectFromChild.getWhere(),
          SqlUtils.equals(TBL_ORDERS, COL_ORDER_MANAGER, userId)));
    }
    
    if (orderMode) {
      selectFromChild.setWhere(SqlUtils.and(selectFromChild.getWhere(),
          SqlUtils.notEqual(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST.ordinal())));
    } else {
      selectFromChild.setWhere(SqlUtils.and(selectFromChild.getWhere(),
          SqlUtils.equals(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST.ordinal())));
    }

    SqlSelect selectGeneral = new SqlSelect()
        .addFields(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT)
    .addMax(NewsConstants.getUsageTable(TBL_ASSESSMENTS), NewsConstants.COL_USAGE_UPDATE)
        .addFrom(NewsConstants.getUsageTable(TBL_ASSESSMENTS))
        .addFromInner(TBL_ASSESSMENTS,
            SqlUtils.join(TBL_ASSESSMENTS, COL_ASSESSMENT_ID,
                NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT))
    .addFromInner(TBL_ORDER_CARGO,
        SqlUtils.join(TBL_ORDER_CARGO, COL_CARGO_ID, TBL_ASSESSMENTS, COL_CARGO))
    .addFromInner(TBL_ORDERS,
        SqlUtils.join(TBL_ORDERS, COL_ORDER_ID, TBL_ORDER_CARGO, COL_ORDER))
        .addGroup(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_ASSESSMENT)
    .setWhere(SqlUtils.and(
        SqlUtils.notEqual(NewsConstants.getUsageTable(TBL_ASSESSMENTS), COL_USER, userId),
        SqlUtils.more(NewsConstants.getUsageTable(TBL_ASSESSMENTS),
                NewsConstants.COL_USAGE_UPDATE, NewsHelper.getStartTime(startDate))
        ));

    if (userMode) {
      selectGeneral.setWhere(SqlUtils.and(selectGeneral.getWhere(),
          SqlUtils.equals(TBL_ORDERS, COL_ORDER_MANAGER, userId)));
    }

    if (orderMode) {
      selectGeneral.setWhere(SqlUtils.and(selectGeneral.getWhere(),
          SqlUtils.notEqual(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST.ordinal())));
    } else {
      selectGeneral.setWhere(SqlUtils.and(selectGeneral.getWhere(),
          SqlUtils.equals(TBL_ORDERS, COL_STATUS, OrderStatus.REQUEST.ordinal())));
    }

    selectGeneral.setUnionAllMode(true).addUnion(selectFromChild);

    String aliasName = SqlUtils.uniqueName();

    SqlSelect select = new SqlSelect()
        .addFields(aliasName, COL_ASSESSMENT)
        .addMax(aliasName, NewsConstants.COL_USAGE_UPDATE)
        .addFrom(selectGeneral, aliasName)
        .addGroup(aliasName, COL_ASSESSMENT);

    return select;
  }

  public AssesmentRequestsUsageQueryProvider(boolean userMode, boolean orderMode) {
    this.userMode = userMode;
    this.orderMode = orderMode;
  }

  public AssesmentRequestsUsageQueryProvider(boolean userMode) {
    this(userMode, false);
  }

}
