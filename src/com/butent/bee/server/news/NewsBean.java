package com.butent.bee.server.news;

import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Locality;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.FiresModificationEvents;
import com.butent.bee.shared.data.event.ModificationEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.news.Channel;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.news.Subscription;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.websocket.messages.ModificationMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

  private static final int ID_CHUNK_SIZE = 50;

  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  public ResponseObject getNews(Collection<Feed> feeds) {
    if (BeeUtils.isEmpty(feeds)) {
      return ResponseObject.error("feeds not specified");
    }

    Long userId = usr.getCurrentUserId();
    if (!DataUtils.isId(userId)) {
      return ResponseObject.error("user id not available");
    }

    String idName = sys.getIdName(NewsConstants.TBL_USER_FEEDS);

    SqlSelect query = new SqlSelect()
        .addFields(NewsConstants.TBL_USER_FEEDS, idName, NewsConstants.COL_UF_FEED,
            NewsConstants.COL_UF_CAPTION, NewsConstants.COL_UF_SUBSCRIPTION_DATE)
        .addFrom(NewsConstants.TBL_USER_FEEDS)
        .setWhere(SqlUtils.equals(NewsConstants.TBL_USER_FEEDS, NewsConstants.COL_UF_USER, userId))
        .addOrder(NewsConstants.TBL_USER_FEEDS, NewsConstants.COL_UF_ORDINAL, idName);

    SimpleRowSet userFeeds = qs.getData(query);
    if (DataUtils.isEmpty(userFeeds)) {
      return ResponseObject.emptyResponse();
    }

    String s = BeeUtils.sameElements(feeds, Feed.ALL) ? BeeConst.ALL : feeds.toString();
    logger.info("user", userId, "feeds", s);

    Dictionary constants = usr.getDictionary(userId);

    List<Subscription> subscriptions = new ArrayList<>();
    int countHeadlines = 0;

    for (SimpleRow row : userFeeds) {
      Long rowId = row.getLong(idName);
      if (!DataUtils.isId(rowId)) {
        logger.severe("invalid user feed row id", rowId);
        continue;
      }

      Feed feed = EnumUtils.getEnumByName(Feed.class, row.getValue(NewsConstants.COL_UF_FEED));
      if (feed == null) {
        logger.severe("invalid user feed name", row.getValue(NewsConstants.COL_UF_FEED));
        continue;
      }

      if (!feeds.contains(feed)) {
        continue;
      }

      if (!usr.isModuleVisible(feed.getModuleAndSub())) {
        logger.warning("user", userId, "is subscribed to invisible feed", feed);
        continue;
      }

      String caption = row.getValue(NewsConstants.COL_UF_CAPTION);
      DateTime date = row.getDateTime(NewsConstants.COL_UF_SUBSCRIPTION_DATE);

      Subscription subscription = new Subscription(rowId, feed, caption, date);

      List<Headline> headlines = getHeadlines(feed, userId, date, constants);
      if (!headlines.isEmpty()) {
        subscription.getHeadlines().addAll(headlines);
        countHeadlines += headlines.size();
      }

      subscriptions.add(subscription);
    }

    logger.info("user", userId, "subscriptions", subscriptions.size(), "headlines", countHeadlines);

    if (subscriptions.isEmpty()) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(subscriptions);
    }
  }

  public String getUsageRelationColumn(String table) {
    return getUsageRelationColumn(table, NewsConstants.getUsageTable(table));
  }

  public IsCondition joinUsage(String table) {
    String usageTable = NewsConstants.getUsageTable(table);
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

    String usageTable = NewsConstants.getUsageTable(table);
    if (BeeUtils.isEmpty(usageTable)) {
      return ResponseObject.error(reqInfo.getService(), "table", table, "has no usage table");
    }

    String relationColumn = getUsageRelationColumn(table, usageTable);
    if (BeeUtils.isEmpty(relationColumn)) {
      return ResponseObject.error(reqInfo.getService(), "relation column not found for",
          table, usageTable);
    }

    IsCondition where = SqlUtils.equals(usageTable, relationColumn, dataId,
        NewsConstants.COL_USAGE_USER, userId);

    ResponseObject response;
    if (qs.sqlExists(usageTable, where)) {
      response = qs.updateDataWithResponse(new SqlUpdate(usageTable)
          .addConstant(NewsConstants.COL_USAGE_ACCESS, System.currentTimeMillis())
          .setWhere(where));

    } else {
      response = qs.insertDataWithResponse(new SqlInsert(usageTable)
          .addFields(relationColumn, NewsConstants.COL_USAGE_USER, NewsConstants.COL_USAGE_ACCESS)
          .addValues(dataId, userId, System.currentTimeMillis()));
    }

    logger.debug("news on access", userId, table, dataId, usageTable);
    return response;
  }

  public void maybeRecordUpdate(String viewName, Long rowId) {
    maybeRecordUpdate(viewName, rowId, null);
  }

  public void maybeRecordUpdate(String viewName, Long rowId, List<BeeColumn> checkColumns) {
    if (!sys.isView(viewName)) {
      return;
    }

    BeeView view = sys.getView(viewName);
    String table = view.getSourceName();

    String usageTable = NewsConstants.getUsageTable(table);
    if (BeeUtils.isEmpty(usageTable)) {
      return;
    }

    if (!BeeUtils.isEmpty(checkColumns) && !NewsConstants.anyObserved(table, checkColumns)) {
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
    long now = System.currentTimeMillis();

    IsCondition where = SqlUtils.equals(usageTable, relationColumn, rowId,
        NewsConstants.COL_USAGE_USER, userId);

    int cnt = qs.updateData(new SqlUpdate(usageTable)
        .addConstant(NewsConstants.COL_USAGE_ACCESS, now)
        .addConstant(NewsConstants.COL_USAGE_UPDATE, now)
        .setWhere(where));

    if (!BeeUtils.isPositive(cnt)) {
      qs.insertData(new SqlInsert(usageTable)
          .addFields(relationColumn, NewsConstants.COL_USAGE_USER, NewsConstants.COL_USAGE_ACCESS,
              NewsConstants.COL_USAGE_UPDATE)
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

    final Long userId = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_USER));
    if (!DataUtils.isId(userId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_USER);
    }

    String feedList = reqInfo.getParameter(Service.VAR_FEED);
    if (BeeUtils.isEmpty(feedList)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), Service.VAR_FEED);
    }

    List<Feed> feeds = Feed.split(feedList);
    if (feeds.isEmpty()) {
      return ResponseObject.error(reqInfo.getService(), "cannot parse feeds", feedList);
    }

    long time = TimeUtils.nowMinutes().getTime();

    for (Feed feed : feeds) {
      SqlInsert insert = new SqlInsert(NewsConstants.TBL_USER_FEEDS)
          .addFields(NewsConstants.COL_UF_USER, NewsConstants.COL_UF_FEED,
              NewsConstants.COL_UF_SUBSCRIPTION_DATE)
          .addValues(userId, feed.name(), time);

      ResponseObject response = qs.insertDataWithResponse(insert);
      if (response.hasErrors()) {
        return response;
      }
    }

    FiresModificationEvents commando = new FiresModificationEvents() {
      @Override
      public void fireModificationEvent(ModificationEvent<?> event, Locality locality) {
        Endpoint.sendToUser(userId, new ModificationMessage(event));
      }
    };

    DataChangeEvent.fireRefresh(commando, NewsConstants.VIEW_USER_FEEDS);

    logger.info(reqInfo.getService(), "user", userId, "subscribed to", feeds);
    return ResponseObject.response(feeds.size());
  }

  private Map<Long, Long> getAccess(Feed feed, String usageTable, String relationColumn,
      long userId, DateTime startDate) {

    Map<Long, Long> access = new HashMap<>();

    SqlSelect query = NewsHelper.getQueryForAccess(feed, usageTable, relationColumn, userId,
        startDate);

    if (query == null) {
      logger.warning("news access query is null for feed", feed);

    } else {
      SimpleRowSet data = qs.getData(query);
      if (!DataUtils.isEmpty(data)) {
        for (SimpleRow row : data) {
          access.put(row.getLong(0), row.getLong(1));
        }
      }
    }

    return access;
  }

  private List<Headline> getHeadlines(Feed feed, long userId, DateTime startDate,
      Dictionary constants) {

    List<Headline> result = new ArrayList<>();

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

    Set<Long> newIds = new HashSet<>();
    Set<Long> updIds = new HashSet<>();

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

    List<Headline> headlines = produceHeadlines(feed, userId, newIds, updIds, constants);
    if (!headlines.isEmpty()) {
      result.addAll(headlines);
    }

    return result;
  }

  private Map<Long, Long> getUpdates(Feed feed, String usageTable, String relationColumn,
      long userId, DateTime startDate) {

    Map<Long, Long> updates = new HashMap<>();

    SqlSelect query = NewsHelper.getQueryForUpdates(feed, usageTable, relationColumn,
        userId, startDate);

    if (query == null) {
      logger.warning("news updates query is null for feed", feed);

    } else {
      SimpleRowSet data = qs.getData(query);

      if (!DataUtils.isEmpty(data)) {
        for (SimpleRow row : data) {
          updates.put(row.getLong(0), row.getLong(1));
        }
      }
    }

    return updates;
  }

  private String getUsageRelationColumn(String table, String usageTable) {
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

  private List<Headline> produceHeadlines(Feed feed, long userId, Collection<Long> newIds,
      Collection<Long> updIds, Dictionary constants) {

    List<Headline> headlines = new ArrayList<>();

    boolean hasNew = !BeeUtils.isEmpty(newIds);
    boolean hasUpd = !BeeUtils.isEmpty(updIds);

    if (!hasNew && !hasUpd) {
      return headlines;
    }

    List<Long> ids = new ArrayList<>();
    if (hasNew) {
      ids.addAll(newIds);
    }
    if (hasUpd) {
      ids.addAll(updIds);
    }

    boolean hasProducer = NewsHelper.hasHeadlineProducer(feed);

    String viewName = feed.getHeadlineView();
    if (BeeUtils.isEmpty(viewName)) {
      logger.severe("feed", feed, "headline view not specified");
      return headlines;
    }

    List<String> labelColumns = feed.getLabelColumns();
    List<String> titleColumns = feed.getTitleColumns();

    if (!hasProducer && BeeUtils.isEmpty(labelColumns)) {
      logger.severe("feed", feed, "label columns not specified");
      return headlines;
    }

    List<String> headlineColumns;
    if (BeeUtils.isEmpty(labelColumns) && BeeUtils.isEmpty(titleColumns)) {
      headlineColumns = null;

    } else {
      headlineColumns = new ArrayList<>();

      if (!BeeUtils.isEmpty(labelColumns)) {
        headlineColumns.addAll(labelColumns);
      }
      if (!BeeUtils.isEmpty(titleColumns)) {
        headlineColumns.addAll(titleColumns);
      }
    }

    List<Integer> labelIndexes = new ArrayList<>();
    List<Integer> titleIndexes = new ArrayList<>();

    for (int pos = 0; pos < ids.size(); pos += ID_CHUNK_SIZE) {
      List<Long> chunk = ids.subList(pos, Math.min(pos + ID_CHUNK_SIZE, ids.size()));

      BeeRowSet rowSet = qs.getViewData(viewName, Filter.idIn(chunk), null, headlineColumns);
      if (DataUtils.isEmpty(rowSet)) {
        logger.warning("feed", feed, "headline view", viewName, "no data for ids", chunk);
        continue;
      }

      if (labelIndexes.isEmpty() && !hasProducer) {
        for (String column : labelColumns) {
          labelIndexes.add(rowSet.getColumnIndex(column));
        }

        if (!BeeUtils.isEmpty(titleColumns)) {
          for (String column : titleColumns) {
            titleIndexes.add(rowSet.getColumnIndex(column));
          }
        }
      }

      DateTimeFormatInfo dateTimeFormatInfo = usr.getDateTimeFormatInfo();

      for (BeeRow row : rowSet.getRows()) {
        boolean isNew = hasNew && newIds.contains(row.getId());

        if (hasProducer) {
          Headline headline = NewsHelper.getHeadline(feed, userId, rowSet, row, isNew, constants,
              dateTimeFormatInfo);
          if (headline != null) {
            headlines.add(headline);
          }

        } else {
          String caption = DataUtils.join(rowSet.getColumns(), row, labelIndexes,
              Headline.SEPARATOR, Formatter.getDateRenderer(dateTimeFormatInfo),
              Formatter.getDateTimeRenderer(dateTimeFormatInfo));

          String title;
          if (titleIndexes.isEmpty()) {
            title = null;
          } else {
            title = DataUtils.join(rowSet.getColumns(), row, titleIndexes, Headline.SEPARATOR,
                Formatter.getDateRenderer(dateTimeFormatInfo),
                Formatter.getDateTimeRenderer(dateTimeFormatInfo));
          }

          if (BeeUtils.isEmpty(caption)) {
            if (BeeUtils.isEmpty(title)) {
              caption = BeeUtils.bracket(row.getId());
            } else {
              caption = title;
              title = null;
            }
          }

          headlines.add(Headline.create(row.getId(), caption, title, isNew));
        }
      }
    }

    return headlines;
  }
}
