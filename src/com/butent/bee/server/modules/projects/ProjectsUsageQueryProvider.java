package com.butent.bee.server.modules.projects;

import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;
import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.time.DateTime;

public class ProjectsUsageQueryProvider implements UsageQueryProvider {

  @Override
  public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
      DateTime startDate) {

    SqlSelect select = new SqlSelect()
        .addFields(TBL_PROJECT_USAGE, COL_PROJECT)
        .addMax(TBL_PROJECT_USAGE, NewsConstants.COL_USAGE_ACCESS)
        .addFrom(TBL_PROJECT_USAGE)
        .addFromInner(
            TBL_PROJECTS,
            SqlUtils.join(TBL_PROJECTS, COL_PROJECT_ID, TBL_PROJECT_USAGE,
                COL_PROJECT))
        .addFromInner(
            TBL_PROJECT_USERS,
            SqlUtils.join(TBL_PROJECTS, COL_PROJECT_ID, TBL_PROJECT_USERS,
                COL_PROJECT))
        .addGroup(TBL_PROJECT_USAGE, COL_PROJECT)
        .setWhere(SqlUtils.and(
            SqlUtils.equals(TBL_PROJECT_USAGE, COL_USER, userId),
            SqlUtils.notNull(TBL_PROJECT_USAGE, NewsConstants.COL_USAGE_ACCESS)
            ));

    return select;
  }

  @Override
  public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
      DateTime startDate) {

    SqlSelect select =
        new SqlSelect()
            .addFields(TBL_PROJECT_EVENTS, COL_PROJECT)
            .addMax(TBL_PROJECT_EVENTS, ProjectConstants.COL_PUBLISH_TIME,
                NewsConstants.COL_USAGE_UPDATE)
            .addFrom(TBL_PROJECT_EVENTS)
            .addFromInner(
                TBL_PROJECTS,
                SqlUtils.join(TBL_PROJECTS, COL_PROJECT_ID, TBL_PROJECT_EVENTS,
                    COL_PROJECT))
            .addFromInner(
                TBL_PROJECT_USERS,
                SqlUtils.join(TBL_PROJECTS, COL_PROJECT_ID, TBL_PROJECT_USERS,
                    COL_PROJECT))
            .addGroup(TBL_PROJECT_EVENTS, COL_PROJECT)
            .setWhere(SqlUtils.and(
                SqlUtils.notEqual(TBL_PROJECT_EVENTS, ProjectConstants.COL_PUBLISHER, userId),
                SqlUtils.more(TBL_PROJECT_EVENTS, ProjectConstants.COL_PUBLISH_TIME,
                    NewsHelper.getStartTime(startDate)),
                SqlUtils.or(
                    SqlUtils.equals(TBL_PROJECT_USERS, COL_USER, userId)
                    )));

    SqlSelect select2 = new SqlSelect()
        .addFields(TBL_PROJECT_USERS, COL_PROJECT)
        .addMax(TBL_PROJECTS, COL_CREATED, NewsConstants.COL_USAGE_UPDATE)
        .addFrom(TBL_PROJECTS)
        .addFromInner(
            TBL_PROJECT_USERS,
            SqlUtils.join(TBL_PROJECTS, COL_PROJECT_ID, TBL_PROJECT_USERS,
                COL_PROJECT))
        .addGroup(TBL_PROJECT_USERS, COL_PROJECT)
        .setWhere(SqlUtils.and(
            SqlUtils.more(TBL_PROJECTS, COL_CREATED,
                NewsHelper.getStartTime(startDate)),
            SqlUtils.or(
                SqlUtils.equals(TBL_PROJECT_USERS, COL_USER, userId))));

    select.setUnionAllMode(true).addUnion(select2);

    String alias = SqlUtils.uniqueName();

    return new SqlSelect()
        .addFields(alias, COL_PROJECT)
        .addMax(alias, NewsConstants.COL_USAGE_UPDATE)
        .addFrom(select, alias)
        .addGroup(alias, COL_PROJECT);
  }
}
