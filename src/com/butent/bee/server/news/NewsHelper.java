package com.butent.bee.server.news;

import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.news.Channel;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.Headline;
import com.butent.bee.shared.news.HeadlineProducer;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NewsHelper {

  private static BeeLogger logger = LogUtils.getLogger(NewsHelper.class);

  private static final Map<String, String> usageRelationColumns = new ConcurrentHashMap<>();

  private static final Map<Feed, Channel> registeredChannels = new ConcurrentHashMap<>();
  private static final Map<Feed, UsageQueryProvider> registeredQueryProviders =
      new ConcurrentHashMap<>();
  private static final Map<Feed, HeadlineProducer> registeredHeadlineProducers =
      new ConcurrentHashMap<>();

  public static List<IsCondition> buildConditions(IsCondition... conditions) {
    Assert.notNull(conditions);

    List<IsCondition> result = new ArrayList<>();
    for (IsCondition condition : conditions) {
      if (condition != null) {
        result.add(condition);
      }
    }

    return result;
  }

  public static List<Pair<String, IsCondition>> buildJoin(String source, IsCondition condition) {
    Assert.notEmpty(source);
    Assert.notNull(condition);

    List<Pair<String, IsCondition>> result = new ArrayList<>();
    result.add(Pair.of(source, condition));

    return result;
  }

  public static List<Pair<String, IsCondition>> buildJoins(String s1, IsCondition c1,
      String s2, IsCondition c2) {
    Assert.notEmpty(s1);
    Assert.notNull(c1);
    Assert.notEmpty(s2);
    Assert.notNull(c2);

    List<Pair<String, IsCondition>> result = new ArrayList<>();
    result.add(Pair.of(s1, c1));
    result.add(Pair.of(s2, c2));

    return result;
  }

  public static SqlSelect getAccessQuery(String idTable, String idColumn,
      List<Pair<String, IsCondition>> joins, Collection<IsCondition> conditions, long userId) {
    return getAccessQuery(idTable, idColumn, idTable, idTable, joins, conditions, userId);
  }

  public static SqlSelect getAccessQuery(String idTable, String idColumn, String usageTable,
      List<Pair<String, IsCondition>> joins, Collection<IsCondition> conditions, long userId) {
    return getAccessQuery(idTable, idColumn, usageTable, idTable, joins, conditions, userId);
  }

  public static SqlSelect getAccessQuery(String idTable, String idColumn, String usageTable,
      String from, List<Pair<String, IsCondition>> joins, Collection<IsCondition> conditions,
      long userId) {

    SqlSelect query = new SqlSelect()
        .addFields(idTable, idColumn)
        .addMax(usageTable, NewsConstants.COL_USAGE_ACCESS)
        .addFrom(from);

    if (!BeeUtils.isEmpty(joins)) {
      for (Pair<String, IsCondition> join : joins) {
        query.addFromInner(join.getA(), join.getB());
      }
    }

    IsCondition accessCondition = SqlUtils.equals(usageTable, NewsConstants.COL_USAGE_USER, userId);
    if (BeeUtils.isEmpty(conditions)) {
      query.setWhere(accessCondition);

    } else {
      HasConditions where = SqlUtils.and(accessCondition);
      for (IsCondition condition : conditions) {
        if (condition != null) {
          where.add(condition);
        }
      }

      query.setWhere(where);
    }

    query.addGroup(idTable, idColumn);

    return query;
  }

  public static long getStartTime(DateTime startDate) {
    return (startDate == null) ? 0L : startDate.getTime();
  }

  public static IsCondition getUpdatesCondition(String usageTable, long userId,
      DateTime startDate) {
    return SqlUtils.and(SqlUtils.or(SqlUtils.isNull(usageTable, NewsConstants.COL_USAGE_USER),
            SqlUtils.notEqual(usageTable, NewsConstants.COL_USAGE_USER, userId)),
        SqlUtils.more(usageTable, NewsConstants.COL_USAGE_UPDATE, getStartTime(startDate)));
  }

  public static SqlSelect getUpdatesQuery(String idTable, String idColumn,
      List<Pair<String, IsCondition>> joins, Collection<IsCondition> conditions,
      long userId, DateTime startDate) {
    return getUpdatesQuery(idTable, idColumn, idTable, idTable, joins, conditions,
        userId, startDate);
  }

  public static SqlSelect getUpdatesQuery(String idTable, String idColumn, String usageTable,
      List<Pair<String, IsCondition>> joins, Collection<IsCondition> conditions,
      long userId, DateTime startDate) {
    return getUpdatesQuery(idTable, idColumn, usageTable, idTable, joins, conditions,
        userId, startDate);
  }

  public static SqlSelect getUpdatesQuery(String idTable, String idColumn, String usageTable,
      String from, List<Pair<String, IsCondition>> joins, Collection<IsCondition> conditions,
      long userId, DateTime startDate) {

    SqlSelect query = new SqlSelect()
        .addFields(idTable, idColumn)
        .addMax(usageTable, NewsConstants.COL_USAGE_UPDATE)
        .addFrom(from);

    if (!BeeUtils.isEmpty(joins)) {
      for (Pair<String, IsCondition> join : joins) {
        query.addFromInner(join.getA(), join.getB());
      }
    }

    IsCondition updatesCondition = getUpdatesCondition(usageTable, userId, startDate);
    if (BeeUtils.isEmpty(conditions)) {
      query.setWhere(updatesCondition);

    } else {
      HasConditions where = SqlUtils.and(updatesCondition);
      for (IsCondition condition : conditions) {
        if (condition != null) {
          where.add(condition);
        }
      }

      query.setWhere(where);
    }

    query.addGroup(idTable, idColumn);

    return query;
  }

  static Headline getHeadline(Feed feed, long userId, BeeRowSet rowSet, IsRow row, boolean isNew,
      Dictionary constants, DateTimeFormatInfo dtfInfo) {

    return registeredHeadlineProducers.get(feed).produce(feed, userId, rowSet, row, isNew,
        constants, dtfInfo);
  }

  static List<Headline> getHeadlines(Feed feed, long userId, DateTime startDate) {
    return registeredChannels.get(feed).getHeadlines(feed, userId, startDate);
  }

  static SqlSelect getQueryForAccess(Feed feed, String usageTable, String relationColumn,
      long userId, DateTime startDate) {

    if (hasUsageQueryProvider(feed)) {
      return registeredQueryProviders.get(feed).getQueryForAccess(feed, relationColumn, userId,
          startDate);
    } else {
      return getAccessQuery(userId, usageTable, relationColumn);
    }
  }

  static SqlSelect getQueryForUpdates(Feed feed, String usageTable, String relationColumn,
      long userId, DateTime startDate) {

    if (hasUsageQueryProvider(feed)) {
      return registeredQueryProviders.get(feed).getQueryForUpdates(feed, relationColumn, userId,
          startDate);
    } else {
      return getUpdatesQuery(userId, usageTable, relationColumn, startDate);
    }
  }

  static String getUsageRelationColumn(String usageTable) {
    return usageRelationColumns.get(usageTable);
  }

  static boolean hasChannel(Feed feed) {
    return feed != null && registeredChannels.containsKey(feed);
  }

  static boolean hasHeadlineProducer(Feed feed) {
    return feed != null && registeredHeadlineProducers.containsKey(feed);
  }

  static boolean hasUsageQueryProvider(Feed feed) {
    return feed != null && registeredQueryProviders.containsKey(feed);
  }

  static void putUsageRelationColumn(String usageTable, String relationColumn) {
    usageRelationColumns.put(usageTable, relationColumn);
  }

  static void registerChannel(Feed feed, Channel channel) {
    registeredChannels.put(feed, channel);
    logger.debug("registered channel for feed", feed);
  }

  static void registerHeadlineProducer(Feed feed, HeadlineProducer headlineProducer) {
    registeredHeadlineProducers.put(feed, headlineProducer);
    logger.debug("registered headline producer for feed", feed);
  }

  static void registerUsageQueryProvider(Feed feed, UsageQueryProvider usageQueryProvider) {
    registeredQueryProviders.put(feed, usageQueryProvider);
    logger.debug("registered usage query provider for feed", feed);
  }

  private static SqlSelect getAccessQuery(long userId, String usageTable, String relationColumn) {
    if (BeeUtils.anyEmpty(usageTable, relationColumn)) {
      return null;
    } else {
      return getAccessQuery(usageTable, relationColumn, usageTable, usageTable, null, null, userId);
    }
  }

  private static SqlSelect getUpdatesQuery(long userId, String usageTable, String relationColumn,
      DateTime startDate) {
    if (BeeUtils.anyEmpty(usageTable, relationColumn)) {
      return null;
    } else {
      return getUpdatesQuery(usageTable, relationColumn, usageTable, usageTable, null, null,
          userId, startDate);
    }
  }

  private NewsHelper() {
  }
}
