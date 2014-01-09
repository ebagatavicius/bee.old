package com.butent.bee.server.news;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.news.Channel;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsUtils;
import com.butent.bee.shared.news.Subscription;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class NewsBean {

  private static BeeLogger logger = LogUtils.getLogger(NewsBean.class);

  private static final String TBL_USER_FEEDS = "UserFeeds";

  private static final String COL_UF_USER = "User";
  private static final String COL_UF_FEED = CommonsConstants.COL_FEED;

  private static final String COL_UF_CAPTION = "Caption";
  private static final String COL_UF_SUBSCRIPTION_DATE = "SubscriptionDate";
  private static final String COL_UF_ORDINAL = "Ordinal";

  private static final int ID_CHUNK_SIZE = 3;

  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  public SqlSelect getAccessQuery(long userId, String table) {
    String usageTable = NewsUtils.getUsageTable(table);
    String relationColumn = getUsageRelationColumn(table, usageTable);
    
    return NewsHelper.getAccessQuery(userId, usageTable, relationColumn);
  }

  public ResponseObject getNews() {
    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error("user id not available");
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_USER_FEEDS, COL_UF_FEED, COL_UF_CAPTION, COL_UF_SUBSCRIPTION_DATE)
        .addFrom(TBL_USER_FEEDS)
        .setWhere(SqlUtils.equals(TBL_USER_FEEDS, COL_UF_USER, userId))
        .addOrder(TBL_USER_FEEDS, COL_UF_ORDINAL, sys.getIdName(TBL_USER_FEEDS));

    SimpleRowSet userFeeds = qs.getData(query);
    if (DataUtils.isEmpty(userFeeds)) {
      return ResponseObject.emptyResponse();
    }

    List<Subscription> subscriptions = Lists.newArrayList();
    int countHeadlines = 0;

    for (SimpleRow row : userFeeds) {
      Feed feed = EnumUtils.getEnumByName(Feed.class, row.getValue(COL_UF_FEED));
      if (feed == null) {
        logger.warning("invalid user feed name", row.getValue(COL_UF_FEED));
        continue;
      }

      String caption = row.getValue(COL_UF_CAPTION);
      DateTime date = row.getDateTime(COL_UF_SUBSCRIPTION_DATE);

      Subscription subscription = new Subscription(feed, caption, date);

      List<Headline> headlines = getHeadlines(feed, userId, date);
      if (!headlines.isEmpty()) {
        subscription.getHeadlines().addAll(headlines);
        countHeadlines += headlines.size();
      }

      subscriptions.add(subscription);
    }

    logger.info("user", userId, "subscriptions", subscriptions.size(), "headlines", countHeadlines);

    return ResponseObject.response(subscriptions);
  }

  public IsCondition getUpdatesCondition(String usageTable, long userId, DateTime startDate) {
    return NewsHelper.getUpdatesCondition(usageTable, userId, startDate);
  }
  
  public SqlSelect getUpdatesQuery(long userId, String table, DateTime startDate) {
    String usageTable = NewsUtils.getUsageTable(table);
    String relationColumn = getUsageRelationColumn(table, usageTable);
    
    return NewsHelper.getUpdatesQuery(userId, usageTable, relationColumn, startDate);
  }

  public String getUsageRelationColumn(String table, String usageTable) {
    if (BeeUtils.anyEmpty(table, usageTable)) {
      return null;
    }

    String relationColumn = NewsHelper.getUsageRelationColumn(usageTable);

    if (BeeUtils.isEmpty(relationColumn)) {
      Collection<BeeField> fields = sys.getTableFields(usageTable);

      if (fields != null) {
        for (BeeField field : fields) {
          if (field instanceof BeeRelation && table.equals(((BeeRelation) field).getRelation())) {
            relationColumn = field.getName();
            break;
          }
        }
      }

      if (BeeUtils.isEmpty(relationColumn)) {
        logger.severe("table", table, "usage table", usageTable, "relation column not found");
      } else {
        NewsHelper.putUsageRelationColumn(usageTable, relationColumn);
      }
    }

    return relationColumn;
  }

  public IsCondition joinUsage(String table) {
    String usageTable = NewsUtils.getUsageTable(table);
    String relationColumn = getUsageRelationColumn(table, usageTable);
    
    return sys.joinTables(table, usageTable, relationColumn);
  }

  public ResponseObject onAccess(RequestInfo reqInfo) {
    Assert.notNull(reqInfo);

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error(reqInfo.getService(), "user id not available");
    }

    String table = reqInfo.getParameter(Service.VAR_TABLE);
    if (BeeUtils.isEmpty(table)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_TABLE);
    }

    Long dataId = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_ID));
    if (!DataUtils.isId(dataId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_ID);
    }

    String usageTable = NewsUtils.getUsageTable(table);
    if (BeeUtils.isEmpty(usageTable)) {
      return ResponseObject.error(reqInfo.getService(), "table", table, "has no usage table");
    }

    String relationColumn = getUsageRelationColumn(table, usageTable);
    if (BeeUtils.isEmpty(relationColumn)) {
      return ResponseObject.error(reqInfo.getService(), "relation column not found for",
          table, usageTable);
    }

    IsCondition where = SqlUtils.equals(usageTable, relationColumn, dataId,
        NewsUtils.COL_USAGE_USER, userId);
    
    ResponseObject response;
    if (qs.sqlExists(usageTable, where)) {
      response = qs.updateDataWithResponse(new SqlUpdate(usageTable)
          .addConstant(NewsUtils.COL_USAGE_ACCESS, System.currentTimeMillis())
          .setWhere(where));

    } else {
      response = qs.insertDataWithResponse(new SqlInsert(usageTable)
          .addFields(relationColumn, NewsUtils.COL_USAGE_USER, NewsUtils.COL_USAGE_ACCESS)
          .addValues(dataId, userId, System.currentTimeMillis()));
    }
    
    logger.debug("news on access", userId, table, dataId, usageTable);
    return response;
  }

  public void onUpdate(String table, List<BeeColumn> columns, Long rowId, boolean isNew) {
    String usageTable = NewsUtils.getUsageTable(table);
    if (BeeUtils.isEmpty(usageTable)) {
      return;
    }
    
    if (!isNew && !NewsUtils.anyObserved(table, columns)) {
      return;
    }

    if (!DataUtils.isId(rowId)) {
      return;
    }

    String relationColumn = getUsageRelationColumn(table, usageTable);
    if (BeeUtils.isEmpty(relationColumn)) {
      return;
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return;
    }

    long now = System.currentTimeMillis();

    IsCondition where = SqlUtils.equals(usageTable, relationColumn, rowId,
        NewsUtils.COL_USAGE_USER, userId);

    if (qs.sqlExists(usageTable, where)) {
      qs.updateData(new SqlUpdate(usageTable)
          .addConstant(NewsUtils.COL_USAGE_ACCESS, now)
          .addConstant(NewsUtils.COL_USAGE_UPDATE, now)
          .setWhere(where));

    } else {
      qs.insertData(new SqlInsert(usageTable)
          .addFields(relationColumn, NewsUtils.COL_USAGE_USER, NewsUtils.COL_USAGE_ACCESS,
              NewsUtils.COL_USAGE_UPDATE)
          .addValues(rowId, userId, now, now));
    }
    
    logger.debug("news on update", userId, table, rowId, usageTable);
  }

  public void registerChannel(Feed feed, Channel channel) {
    Assert.notNull(feed);
    Assert.notNull(channel);

    NewsHelper.registerChannel(feed, channel);
  }
  
  public void registerHeadlineProducer(Feed feed, HeadlineProducer headlineProducer) {
    Assert.notNull(feed);
    Assert.notNull(headlineProducer);

    NewsHelper.registerHeadlineProducer(feed, headlineProducer);
  }

  public void registerUsageQueryProvider(Feed feed, UsageQueryProvider usageQueryProvider) {
    Assert.notNull(feed);
    Assert.notNull(usageQueryProvider);

    NewsHelper.registerUsageQueryProvider(feed, usageQueryProvider);
  }

  public ResponseObject subscribe(RequestInfo reqInfo) {
    Assert.notNull(reqInfo);

    Long userId = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_USER));
    if (!DataUtils.isId(userId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_USER);
    }

    String feedList = reqInfo.getParameter(Service.VAR_FEED);
    if (BeeUtils.isEmpty(feedList)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_FEED);
    }

    List<Feed> feeds = NewsUtils.splitFeeds(feedList);
    if (feeds.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), "cannot parse feeds", feedList);
    }

    long time = TimeUtils.nowMinutes().getTime();

    for (Feed feed : feeds) {
      SqlInsert insert = new SqlInsert(TBL_USER_FEEDS)
          .addFields(COL_UF_USER, COL_UF_FEED, COL_UF_SUBSCRIPTION_DATE)
          .addValues(userId, feed.name(), time);

      ResponseObject response = qs.insertDataWithResponse(insert);
      if (response.hasErrors()) {
        return response;
      }
    }

    logger.info(reqInfo.getService(), "user", userId, "subscribed to", feeds);
    return ResponseObject.response(feeds.size());
  }

  private Map<Long, Long> getAccess(Feed feed, String usageTable, String relationColumn,
      long userId, DateTime startDate) {

    Map<Long, Long> access = Maps.newHashMap();

    SqlSelect query = NewsHelper.getQueryForAccess(feed, usageTable, relationColumn, userId,
        startDate);
    if (query != null) {
      SimpleRowSet data = qs.getData(query);
      if (!DataUtils.isEmpty(data)) {
        for (SimpleRow row : data) {
          access.put(row.getLong(0), row.getLong(1));
        }
      }
    }

    return access;
  }

  private List<Headline> getHeadlines(Feed feed, long userId, DateTime startDate) {
    List<Headline> result = Lists.newArrayList();

    if (NewsHelper.hasChannel(feed)) {
      List<Headline> headlines = NewsHelper.getHeadlines(feed, userId, startDate);
      if (!BeeUtils.isEmpty(headlines)) {
        result.addAll(headlines);
      }
      return result;
    }

    String usageTable = feed.getUsageTable();
    String relationColumn = getUsageRelationColumn(feed.getTable(), usageTable);

    Map<Long, Long> updates = getUpdates(feed, usageTable, relationColumn, userId, startDate);
    if (updates.isEmpty()) {
      return result;
    }

    Map<Long, Long> access = getAccess(feed, usageTable, relationColumn, userId, startDate);

    Set<Long> newIds = Sets.newHashSet();
    Set<Long> updIds = Sets.newHashSet();

    if (access.isEmpty()) {
      newIds.addAll(updates.keySet());

    } else {
      for (Map.Entry<Long, Long> updateEntry : updates.entrySet()) {
        Long id = updateEntry.getKey();

        Long accessTime = access.get(id);
        if (accessTime == null) {
          newIds.add(id);
        } else if (accessTime < updateEntry.getValue()) {
          updIds.add(id);
        }
      }
    }

    List<Headline> headlines = produceHeadlines(feed, userId, newIds, updIds);
    if (!headlines.isEmpty()) {
      result.addAll(headlines);
    }

    return result;
  }

  private Map<Long, Long> getUpdates(Feed feed, String usageTable, String relationColumn,
      long userId, DateTime startDate) {

    Map<Long, Long> updates = Maps.newHashMap();

    SqlSelect query = NewsHelper.getQueryForUpdates(feed, usageTable, relationColumn,
        userId, startDate);
    if (query != null) {
      SimpleRowSet data = qs.getData(query);

      if (!DataUtils.isEmpty(data)) {
        for (SimpleRow row : data) {
          updates.put(row.getLong(0), row.getLong(1));
        }
      }
    }

    return updates;
  }

  private List<Headline> produceHeadlines(Feed feed, long userId, Collection<Long> newIds,
      Collection<Long> updIds) {
    List<Headline> headlines = Lists.newArrayList();

    boolean hasNew = !BeeUtils.isEmpty(newIds);
    boolean hasUpd = !BeeUtils.isEmpty(updIds);

    if (!hasNew && !hasUpd) {
      return headlines;
    }

    List<Long> ids = Lists.newArrayList();
    if (hasNew) {
      ids.addAll(newIds);
    }
    if (hasUpd) {
      ids.addAll(updIds);
    }

    boolean hasProducer = NewsHelper.hasHeadlineProducer(feed);

    String viewName = feed.getHeadlineView();
    if (BeeUtils.isEmpty(viewName)) {
      logger.severe("feed", feed.name(), "headline view not specified");
      return headlines;
    }

    List<String> headlineColumns = feed.getHeadlineColumns();
    if (!hasProducer && BeeUtils.isEmpty(headlineColumns)) {
      logger.severe("feed", feed.name(), "headline columns not specified");
      return headlines;
    }

    List<Integer> indexes = Lists.newArrayList();

    for (int pos = 0; pos < ids.size(); pos += ID_CHUNK_SIZE) {
      List<Long> chunk = ids.subList(pos, Math.min(pos + ID_CHUNK_SIZE, ids.size()));

      BeeRowSet rowSet = qs.getViewData(viewName, Filter.idIn(chunk), null, headlineColumns);
      if (DataUtils.isEmpty(rowSet)) {
        logger.warning("feed", feed.name(), "headline view", viewName, "no data for ids", chunk);
        continue;
      }

      if (indexes.isEmpty() && !hasProducer) {
        for (String column : headlineColumns) {
          indexes.add(rowSet.getColumnIndex(column));
        }
      }

      for (BeeRow row : rowSet.getRows()) {
        boolean isNew = hasNew && newIds.contains(row.getId());

        if (hasProducer) {
          Headline headline = NewsHelper.getHeadline(feed, userId, rowSet, row, isNew);
          if (headline != null) {
            headlines.add(headline);
          }

        } else {
          String caption = DataUtils.join(rowSet.getColumns(), row, indexes, Headline.SEPARATOR);
          if (BeeUtils.isEmpty(caption)) {
            caption = BeeUtils.bracket(row.getId());
          }

          headlines.add(Headline.create(row.getId(), caption, isNew));
        }
      }
    }

    return headlines;
  }
}
