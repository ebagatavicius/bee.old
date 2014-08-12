package com.butent.bee.server.news;

import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.time.DateTime;

import java.util.List;

public abstract class ExtendedUsageQueryProvider implements UsageQueryProvider {

  @Override
  public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
      DateTime startDate) {
    return NewsHelper.getAccessQuery(feed.getUsageTable(), relationColumn,
        getJoins(), getConditions(userId), userId);
  }

  @Override
  public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
      DateTime startDate) {
    return NewsHelper.getUpdatesQuery(feed.getUsageTable(), relationColumn,
        getJoins(), getConditions(userId), userId, startDate);
  }

  protected abstract List<IsCondition> getConditions(long userId);

  protected abstract List<Pair<String, IsCondition>> getJoins();
}
