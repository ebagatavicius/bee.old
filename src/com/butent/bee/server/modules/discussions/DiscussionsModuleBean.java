package com.butent.bee.server.modules.discussions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.ParameterEvent;
import com.butent.bee.server.modules.ParameterEventHandler;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.RowChildren;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.H2;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionEvent;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.DiscussionStatus;
import com.butent.bee.shared.modules.discussions.DiscussionsUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Stateless
@LocalBean
public class DiscussionsModuleBean implements BeeModule {

  private static final int MAX_NUMBERS_OF_ROWS = 100;
  private static final String IMG_LINK = "link";

  private static final String LOG_CREATE_DISCUSSION_LABEL =
      "Create discussion: ";

  private static final String LOG_CREATE_ANNOUNCEMENT_LABEL =
      "Create annoucement: ";

  private static final String LOG_MAIL_NEW_DISCUSSION_LABEL =
      "Mail new discussion";

  private static final String LOG_MAIL_NEW_ANNOUNCEMENT_LABEL =
      "Mail new announcment";

  private static BeeLogger logger = LogUtils.getLogger(DiscussionsModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  DataEditorBean deb;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  ParamHolderBean prm;
  @EJB
  NewsBean news;
  @EJB
  MailModuleBean mail;

  @Resource
  EJBContext ctx;
  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    return qs.getSearchResults(VIEW_DISCUSSIONS,
        Filter.anyContains(Sets.newHashSet(COL_SUBJECT, COL_DESCRIPTION, ALS_OWNER_FIRST_NAME,
            ALS_OWNER_LAST_NAME), query));
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.isPrefix(svc, DISCUSSIONS_PREFIX)) {
      response = doDiscussionEvent(BeeUtils.removePrefix(svc, DISCUSSIONS_PREFIX), reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_DISCUSSION_DATA)) {
      response = getDiscussionData(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_ANNOUNCEMENTS_DATA)) {
      response = getAnnouncements();
    } else if (BeeUtils.same(svc, SVC_GET_BIRTHDAYS)) {
      response = getBirthdays();
    } else {
      String message = BeeUtils.joinWords("Discussion service not recognized:", svc);
      logger.warning(message);
      response = ResponseObject.error(message);
    }

    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createText(module, PRM_DISCUSS_ADMIN, false, ""),
        BeeParameter.createBoolean(module, PRM_ALLOW_DELETE_OWN_COMMENTS, false, null),
        BeeParameter.createNumber(module, PRM_DISCUSS_INACTIVE_TIME_IN_DAYS, false, null),
        BeeParameter.createText(module, PRM_FORBIDDEN_FILES_EXTENTIONS, false, ""),
        BeeParameter.createNumber(module, PRM_MAX_UPLOAD_FILE_SIZE, false, null),
        BeeParameter.createRelation(module, PRM_DISCUSS_BIRTHDAYS, false, TBL_ADS_TOPICS, COL_NAME)
        );

    return params;
  }

  @Override
  public Module getModule() {
    return Module.DISCUSSIONS;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    initTimer();

    prm.registerParameterEventHandler(new ParameterEventHandler() {
      @Subscribe
      public void initTimers(ParameterEvent event) {
        if (BeeUtils.same(event.getParameter(), PRM_DISCUSS_INACTIVE_TIME_IN_DAYS)) {
          DiscussionsModuleBean bean = Invocation.locateRemoteBean(DiscussionsModuleBean.class);

          if (bean != null) {
            bean.initTimer();
          }
        }
      }
    });

    sys.registerDataEventHandler(new DataEventHandler() {

      @Subscribe
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isBefore()) {
          return;
        }

        if (BeeUtils.same(event.getTargetName(), VIEW_DISCUSSIONS)) {
          BeeRowSet rowSet = event.getRowset();

          if (!rowSet.isEmpty()) {
            Set<Long> discussionsIds = new HashSet<>();

            if (rowSet.getNumberOfRows() < MAX_NUMBERS_OF_ROWS) {
              for (BeeRow row : rowSet.getRows()) {
                discussionsIds.add(row.getId());
              }
            }

            SqlSelect discussUsers = new SqlSelect()
                .addFields(TBL_DISCUSSIONS_USERS, COL_DISCUSSION, COL_LAST_ACCESS, COL_STAR)
                .addFrom(TBL_DISCUSSIONS_USERS);

            IsCondition usersWhere =
                SqlUtils.equals(TBL_DISCUSSIONS_USERS, AdministrationConstants.COL_USER, usr
                    .getCurrentUserId());

            discussUsers.setWhere(usersWhere);

            if (!discussionsIds.isEmpty()) {
              discussUsers.setWhere(SqlUtils.and(usersWhere, SqlUtils.inList(TBL_DISCUSSIONS_USERS,
                  COL_DISCUSSION, discussionsIds)));
            }

            SimpleRowSet discussUsersData = qs.getData(discussUsers);

            int discussIndex = discussUsersData.getColumnIndex(COL_DISCUSSION);
            int accessIndex = discussUsersData.getColumnIndex(COL_LAST_ACCESS);
            int starIndex = discussUsersData.getColumnIndex(COL_STAR);

            for (SimpleRow discussUserRow : discussUsersData) {
              long discussionId = discussUserRow.getLong(discussIndex);
              BeeRow row = rowSet.getRowById(discussionId);

              if (row == null) {
                continue;
              }

              row.setProperty(PROP_USER, BeeConst.STRING_PLUS);

              if (discussUserRow.getValue(accessIndex) != null) {
                row.setProperty(PROP_LAST_ACCESS, discussUserRow.getValue(starIndex));
              }
              if (discussUserRow.getValue(starIndex) != null) {
                row.setProperty(PROP_STAR, discussUserRow.getValue(starIndex));
              }
            }

            SqlSelect discussionsEvents = new SqlSelect()
                .addFields(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION)
                .addMax(TBL_DISCUSSIONS_COMMENTS, COL_PUBLISH_TIME)
                .addFrom(TBL_DISCUSSIONS_COMMENTS)
                .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION);

            if (!discussionsIds.isEmpty()) {
              discussionsEvents.setWhere(SqlUtils.inList(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION,
                  discussionsIds));
            }

            SimpleRowSet discussEventsData = qs.getData(discussionsEvents);
            discussIndex = discussEventsData.getColumnIndex(COL_DISCUSSION);
            int publishIndex = discussEventsData.getColumnIndex(COL_PUBLISH_TIME);

            for (SimpleRow discussEventRow : discussEventsData) {
              long discussionId = discussEventRow.getLong(discussIndex);
              BeeRow row = rowSet.getRowById(discussionId);

              if (discussEventRow.getValue(publishIndex) != null) {
                row.setProperty(PROP_LAST_PUBLISH, discussEventRow.getValue(publishIndex));
              }
            }

            Map<Long, Integer> markCounts = getDiscussionsMarksCount(discussionsIds);
            Map<Long, String> lastCommentData = getDisscussionsLastComment(discussionsIds);
            for (BeeRow row : rowSet.getRows()) {
              String markValue =
                  markCounts.get(row.getId()) != null ? BeeUtils
                      .toString(markCounts.get(row.getId())) : "0";
              row.setProperty(PROP_MARK_COUNT, markValue);

              String lastCommentVal = lastCommentData.get(row.getId());

              row.setProperty(PROP_LAST_COMMENT_DATA, lastCommentVal);

              String filesCountVal = "";
              if (BeeUtils.isPositive(DataUtils.getColumnIndex(ALS_FILES_COUNT,
                  rowSet.getColumns(), false))) {
                filesCountVal = row.getString(rowSet.getColumnIndex(ALS_FILES_COUNT));
              }

              row.setProperty(PROP_FILES_COUNT, filesCountVal);

              String relValue = "";

              if (BeeUtils.isPositive(DataUtils.getColumnIndex(ALS_RELATIONS_COUNT, rowSet
                  .getColumns(), false))) {
                int rc =
                    BeeUtils.unbox(row.getInteger(rowSet.getColumnIndex(ALS_RELATIONS_COUNT)));

                if (rc > 0) {
                  relValue += IMG_LINK;
                }
              }

              row.setProperty(PROP_RELATIONS_COUNT, relValue);

              if (BeeUtils.isEmpty(row.getProperty(PROP_USER))) {
                row.setProperty(PROP_USER, BeeConst.STRING_PLUS);
                createDiscussionUser(row.getId(), usr.getCurrentUserId(), null, false);
              }
            }
          }
        }
      }

      @Subscribe
      public void setMarkTypesRowProperties(ViewQueryEvent event) {
        if (event.isBefore()) {
          return;
        }

        if (BeeUtils.same(event.getTargetName(), VIEW_DISCUSSIONS_MARK_TYPES)) {
          BeeRowSet rowSet = event.getRowset();

          if (!rowSet.isEmpty()) {

            if (rowSet.getNumberOfRows() < MAX_NUMBERS_OF_ROWS) {
              for (BeeRow row : rowSet.getRows()) {
                row.setProperty(PROP_PREVIEW_IMAGE,
                    row.getString(rowSet.getColumnIndex(COL_IMAGE_RESOURCE_NAME)));
              }
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.DISCUSSIONS, new DiscussionsUsageQueryProvider());
    news.registerUsageQueryProvider(Feed.ANNOUNCEMENTS, new AnnouncementsUsageQueryProvider());
  }

  public void initTimer() {
    Timer discussTimer = null;

    for (Timer timer : timerService.getTimers()) {
      if (Objects.equals(timer.getInfo(), PRM_DISCUSS_INACTIVE_TIME_IN_DAYS)) {
        discussTimer = timer;
        break;
      }
    }
    if (discussTimer != null) {
      discussTimer.cancel();
    }
    Integer days = prm.getInteger(PRM_DISCUSS_INACTIVE_TIME_IN_DAYS);

    if (BeeUtils.isPositive(days)) {
      discussTimer = timerService.createIntervalTimer(DEFAUT_DISCCUSS_TIMER_TIMEOUT,
          DEFAUT_DISCCUSS_TIMER_TIMEOUT, new TimerConfig(PRM_DISCUSS_INACTIVE_TIME_IN_DAYS, false));

      logger.info("Created DISCUSSION refresh timer starting at", discussTimer.getNextTimeout());
    } else {
      if (discussTimer != null) {
        logger.info("Removed DISCUSSION timer");
      }
    }

  }

  private void addDiscussionProperties(BeeRow row, List<BeeColumn> columns,
      Collection<Long> discussionUsers, Collection<Long> discussionMarks, Long commentId) {
    long discussionId = row.getId();

    if (!BeeUtils.isEmpty(discussionUsers)) {
      discussionUsers.remove(row.getLong(DataUtils.getColumnIndex(COL_OWNER, columns)));

      if (!discussionUsers.isEmpty()) {
        row.setProperty(PROP_MEMBERS, DataUtils.buildIdList(discussionUsers));
      }
    }

    if (!BeeUtils.isEmpty(discussionMarks)) {
      row.setProperty(PROP_MARKS, DataUtils.buildIdList(discussionMarks));
    }

    SimpleRowSet markData = getDiscussionMarksData((List<Long>) discussionMarks);
    row.setProperty(PROP_MARK_DATA, markData.serialize());

    Multimap<String, Long> discussionRelations = getDiscussionRelations(discussionId);
    for (String property : discussionRelations.keySet()) {
      row.setProperty(property, DataUtils.buildIdList(discussionRelations.get(property)));
    }

    List<FileInfo> files = getDiscussionFiles(discussionId);
    if (!files.isEmpty()) {
      row.setProperty(PROP_FILES, Codec.beeSerialize(files));
    }

    BeeRowSet comments =
        qs.getViewData(VIEW_DISCUSSIONS_COMMENTS, Filter.equals(COL_DISCUSSION, discussionId));
    if (!DataUtils.isEmpty(comments)) {
      row.setProperty(PROP_COMMENTS, comments.serialize());
    }

    if (commentId != null) {
      row.setProperty(PROP_LAST_COMMENT, BeeUtils.toString(commentId));
    }

    BeeRowSet rs = getDiscussionMarkTypeData();
    if (!rs.isEmpty()) {
      row.setProperty(PROP_MARK_TYPES, rs.serialize());
    }

    Collection<BeeParameter> discussModuleParams = prm.getParameters(getModule().getName());
    if (!discussModuleParams.isEmpty()) {
      Map<String, String> paramsMap = new HashMap<>();

      for (BeeParameter p : discussModuleParams) {
        paramsMap.put(p.getName(), p.getValue());
      }

      row.setProperty(PROP_PARAMETERS, Codec.beeSerialize(paramsMap));
    }
  }

  private ResponseObject createDiscussionRelations(long discussionId,
      Map<String, String> properties) {
    int count = 0;

    if (BeeUtils.isEmpty(properties)) {
      return ResponseObject.response(count);
    }

    ResponseObject response = new ResponseObject();
    List<RowChildren> children = new ArrayList<>();

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      String relation = DiscussionsUtils.translateDiscussionPropertyToRelation(entry.getKey());

      if (BeeUtils.allNotEmpty(relation, entry.getValue())) {
        children.add(RowChildren.create(AdministrationConstants.TBL_RELATIONS, COL_DISCUSSION,
            null,
            relation, entry.getValue()));
      }
    }

    if (!BeeUtils.isEmpty(children)) {
      count = deb.commitChildren(discussionId, children, response);
    }

    return response.setResponse(count);
  }

  private ResponseObject createDiscussionUser(long discussionId, long userId, Long time,
      boolean isMember) {

    SqlUpdate update = new SqlUpdate(TBL_DISCUSSIONS_USERS).addConstant(COL_MEMBER, isMember)
        .setWhere(
            SqlUtils.and(SqlUtils.equals(TBL_DISCUSSIONS_USERS, AdministrationConstants.COL_USER,
                userId), SqlUtils.equals(TBL_DISCUSSIONS_USERS, COL_DISCUSSION, discussionId)));

    if (time != null) {
      update.addConstant(COL_LAST_ACCESS, time);
    }

    ResponseObject resp = qs.updateDataWithResponse(update);

    if (!resp.hasResponse() || BeeUtils.unbox((Integer) resp.getResponse()) <= 0) {

      SqlInsert insert = new SqlInsert(TBL_DISCUSSIONS_USERS)
          .addConstant(COL_DISCUSSION, discussionId)
          .addConstant(AdministrationConstants.COL_USER, userId)
          .addConstant(COL_MEMBER, isMember);

      if (time != null) {
        insert.addConstant(COL_LAST_ACCESS, time);
      }
      return qs.insertDataWithResponse(insert);
    }

    return resp;
  }

  private ResponseObject commitDiscussionComment(long discussionId, long userId,
      Long parentCommentId,
      String comment, long mills) {
    SqlInsert si = new SqlInsert(TBL_DISCUSSIONS_COMMENTS)
        .addConstant(COL_DISCUSSION, discussionId)
        .addConstant(COL_PUBLISHER, userId)
        .addConstant(COL_PARENT_COMMENT, parentCommentId)
        .addConstant(COL_PUBLISH_TIME, mills);

    if (!BeeUtils.isEmpty(comment)) {
      si.addConstant(COL_COMMENT_TEXT, comment);
    }

    return qs.insertDataWithResponse(si);
  }

  private ResponseObject commitDiscussionData(BeeRowSet data, Collection<Long> oldUsers,
      boolean checkUsers, Set<String> updatedRelations, Long commentId) {

    ResponseObject response;
    BeeRow row = data.getRow(0);

    List<Long> newUsers;

    if (checkUsers) {
      newUsers = DiscussionsUtils.getDiscussionMembers(row, data.getColumns());

      if (!BeeUtils.sameElements(oldUsers, newUsers)) {
        updateDiscussionUsers(row.getId(), oldUsers, newUsers);
      }
    } else {
      newUsers = new ArrayList<>(oldUsers);
    }

    if (!BeeUtils.isEmpty(updatedRelations)) {
      updateDiscussionRelations(row.getId(), updatedRelations, row);
    }

    Map<Integer, String> shadow = row.getShadow();
    if (shadow != null && !shadow.isEmpty()) {
      List<BeeColumn> columns = new ArrayList<>();
      List<String> oldValues = new ArrayList<>();
      List<String> newValues = new ArrayList<>();

      for (Map.Entry<Integer, String> entry : shadow.entrySet()) {
        columns.add(data.getColumn(entry.getKey()));

        oldValues.add(entry.getValue());
        newValues.add(row.getString(entry.getKey()));
      }

      BeeRow updRow = new BeeRow(row.getId(), row.getVersion(), oldValues);
      for (int i = 0; i < columns.size(); i++) {
        updRow.preliminaryUpdate(i, newValues.get(i));
      }

      BeeRowSet updated = new BeeRowSet(data.getViewName(), columns);
      updated.addRow(updRow);

      response = deb.commitRow(updated);
      if (!response.hasErrors() && response.hasResponse(BeeRow.class)) {
        addDiscussionProperties((BeeRow) response.getResponse(), data.getColumns(), newUsers, null,
            commentId);
      }
    } else {
      response = getDiscussionData(row.getId(), commentId);
    }

    return response;
  }

  private ResponseObject deleteDiscussionComment(long discussionId, long commentId) {

    String reasonText = BeeUtils.joinWords("<i style=\"font-size: smaller; color:red\">(", usr
        .getLocalizableConstants().discussEventCommentDeleted()
        + " )</i>:", new DateTime().toString() + ",", usr.getCurrentUserSign());

    SqlUpdate update =
        new SqlUpdate(TBL_DISCUSSIONS_COMMENTS)
            .addConstant(COL_REASON, reasonText)
            .addConstant(COL_DELETED, true)
            .setWhere(SqlUtils.and(SqlUtils.equals(
                TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, discussionId), SqlUtils.equals(
                TBL_DISCUSSIONS_COMMENTS, sys.getIdName(TBL_DISCUSSIONS_COMMENTS), commentId)));

    return qs.updateDataWithResponse(update);
  }

  private ResponseObject doDiscussionEvent(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;
    DiscussionEvent event = EnumUtils.getEnumByName(DiscussionEvent.class, svc);

    if (event == null) {
      String message = BeeUtils.joinWords("Discussion service not recognized:", svc);
      logger.warning(message);
      response = ResponseObject.error(message);
      return response;
    }

    String dataParam = reqInfo.getParameter(VAR_DISCUSSION_DATA);

    if (BeeUtils.isEmpty(dataParam)) {
      String msg = BeeUtils.joinWords("Discussion data not received:", svc, event);
      logger.warning(msg);
      response = ResponseObject.error(msg);
      return response;
    }

    BeeRowSet discussData = BeeRowSet.restore(dataParam);
    BeeRow discussRow = discussData.getRow(0);

    long discussionId = discussRow.getId();

    long currentUser = usr.getCurrentUserId();
    long now = System.currentTimeMillis();

    Long commentId = null;

    String commentText = reqInfo.getParameter(VAR_DISCUSSION_COMMENT);
    Long parentComment = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_DISCUSSION_PARENT_COMMENT));
    Long deleteComment =
        BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_DISCUSSION_DELETED_COMMENT));
    Long markId = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_DISCUSSION_MARK));
    Long markedComment = BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_DISCUSSION_MARKED_COMMENT));

    Set<Long> oldMembers = DataUtils.parseIdSet(reqInfo.getParameter(VAR_DISCUSSION_USERS));
    Set<String> updatedRelations = NameUtils.toSet(reqInfo.getParameter(VAR_DISCUSSION_USERS));
    switch (event) {
      case CREATE:
        Map<String, String> properties = discussRow.getProperties();

        if (properties == null) {
          properties = new HashMap<>();
        }

        List<Long> members = DataUtils.parseIdList(properties.get(PROP_MEMBERS));
        List<Long> discussions = new ArrayList<>();

        BeeRow newRow = DataUtils.cloneRow(discussRow);
        DiscussionStatus status = DiscussionStatus.ACTIVE;

        if (DiscussionStatus.in(discussRow.getInteger(discussData.getColumnIndex(COL_STATUS)),
            DiscussionStatus.CLOSED)) {
          status = DiscussionStatus.CLOSED;
        }

        newRow.setValue(discussData.getColumnIndex(COL_STATUS), status.ordinal());
        discussData.clearRows();
        discussData.addRow(newRow);
        response = deb.commitRow(discussData);

        if (response.hasErrors()) {
          break;
        }

        discussionId = ((BeeRow) response.getResponse()).getId();

        if (!response.hasErrors()) {
          response = createDiscussionUser(discussionId, currentUser, now, true);
        }

        if (!response.hasErrors()) {
          for (long memberId : members) {
            if (memberId != currentUser) {
              response = createDiscussionUser(discussionId, memberId, now, true);
              if (response.hasErrors()) {
                break;
              }
            }
          }
        }

        if (!response.hasErrors()) {
          response = createDiscussionRelations(discussionId, properties);
        }

        if (!response.hasErrors()) {
          discussions.add(discussionId);
        }

        if (response.hasErrors()) {
          break;
        }

        if (!response.hasErrors()) {
          if (discussions.isEmpty()) {
            response = ResponseObject.error(usr.getLocalizableConstants().discussNotCreated());
          } else {
            response = ResponseObject.response(DataUtils.buildIdList(discussions));
          }
        }
        break;

      case CREATE_MAIL:
        String discussIdData = reqInfo.getParameter(VAR_DISCUSSION_ID);

        if (DataUtils.isId(BeeUtils.toLongOrNull(discussIdData))) {
          discussionId = BeeUtils.toLong(discussIdData);
        } else {
          return ResponseObject.error("Incorrect discussion id:", discussIdData);
        }

        boolean notifyEmail = BeeConst.isTrue(discussRow.getProperty(PROP_MAIL));
        boolean announcement = false;
        String announcementTopic = "";

        if (!(discussData.getColumnIndex(COL_TOPIC) < 0)) {
          announcement =
              DataUtils.isId(discussRow.getLong(discussData.getColumnIndex(COL_TOPIC)));
        }

        if (announcement) {
          BeeRowSet topics =
              qs.getViewData(VIEW_ADS_TOPICS, Filter.compareId(discussRow.getLong(discussData
                  .getColumnIndex(COL_TOPIC))));

          announcementTopic = topics.getRow(0).getString(topics.getColumnIndex(COL_NAME));
        }

        boolean sendAll =
            BeeUtils
                .unbox(discussRow.getBoolean(discussData.getColumnIndex(COL_ACCESSIBILITY)));

        ResponseObject mailResponse =
            sendNewDiscussionMail(discussionId, announcement, announcementTopic, notifyEmail,
                sendAll);
        response = ResponseObject.emptyResponse().addMessagesFrom(mailResponse);
        break;
      case DEACTIVATE:
        break;
      case VISIT:
        if (oldMembers.contains(currentUser)) {
          response = registerDiscussionVisit(discussionId, currentUser, now);
        }

        if (response == null || !response.hasErrors()) {
          response =
              commitDiscussionData(discussData, oldMembers, false, updatedRelations, commentId);
        }

        break;
      case COMMENT_DELETE:
      case ACTIVATE:
      case CLOSE:
      case COMMENT:
      case MARK:
      case MODIFY:
      case REPLY:
        if (!BeeUtils.isEmpty(commentText)) {
          response =
              commitDiscussionComment(discussionId, currentUser, parentComment, commentText, now);
        }

        if (response != null) {
          if (response.hasResponse(Long.class)) {
            commentId = (Long) response.getResponse();
          }
        }

        if ((response == null || !response.hasErrors()) && DataUtils.isId(deleteComment)) {
          response = deleteDiscussionComment(discussionId, deleteComment);
        }

        if ((response == null || !response.hasErrors()) && DataUtils.isId(markId)) {
          response = doMark(discussionId, markedComment, markId, currentUser);
        }

        if (response == null || !response.hasErrors()) {
          response =
              commitDiscussionData(discussData, oldMembers, true, updatedRelations, commentId);
        }

        break;
      default:
        break;
    }

    if (response.hasErrors()) {
      ctx.setRollbackOnly();
    }

    return response;
  }

  @Timeout
  private void doInactiveDiscussions() {
    if (!Config.isInitialized()) {
      return;
    }
    Long days = prm.getLong(PRM_DISCUSS_INACTIVE_TIME_IN_DAYS);

    if (!BeeUtils.isPositive(days)) {
      logger.info("No value set for discussion deactyvation");
      return;
    }
    SqlSelect select =
        new SqlSelect().addField(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS), sys
            .getIdName(TBL_DISCUSSIONS))
            .addMax(TBL_DISCUSSIONS_COMMENTS, COL_PUBLISH_TIME, ALS_LAST_COMMET)
            .addFrom(TBL_DISCUSSIONS)
            .addFromLeft(TBL_DISCUSSIONS_COMMENTS,
                sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION))
            .setWhere(SqlUtils.and(
                SqlUtils.equals(TBL_DISCUSSIONS, COL_STATUS, DiscussionStatus.ACTIVE.ordinal()),
                SqlUtils.notNull(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION)))
            .addGroup(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS));

    SimpleRowSet activeDiscussions = qs.getData(select);

    if (activeDiscussions.isEmpty()) {
      logger.info("No value set for discussion deactyvation");
      return;
    }

    JustDate dacyvationTime =
        new JustDate(System.currentTimeMillis() - (days * TimeUtils.MILLIS_PER_DAY));

    int count = 0;
    for (int i = 0; i < activeDiscussions.getNumberOfRows(); i++) {

      if (activeDiscussions.getDate(i, ALS_LAST_COMMET).compareTo(dacyvationTime) < 0) {
        SqlUpdate update =
            new SqlUpdate(TBL_DISCUSSIONS).addConstant(COL_STATUS,
                DiscussionStatus.INACTIVE.ordinal())
                .setWhere(SqlUtils.equals(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS),
                    activeDiscussions.getLong(i, sys.getIdName(TBL_DISCUSSIONS))));

        count += qs.updateData(update);
      }
    }

    logger.info("Setting inactive discussions", count);
  }

  private ResponseObject doMark(Long discussionId, Long commentId, Long markId, Long userId) {
    SqlInsert insert =
        new SqlInsert(TBL_DISCUSSIONS_COMMENTS_MARKS).addConstant(COL_DISCUSSION, discussionId)
            .addConstant(COL_COMMENT, commentId)
            .addConstant(COL_MARK, markId)
            .addConstant(AdministrationConstants.COL_USER, userId);

    return qs.insertDataWithResponse(insert);
  }

  private ResponseObject getDiscussionData(RequestInfo reqInfo) {
    long discussionId = BeeUtils.toLong(reqInfo.getParameter(VAR_DISCUSSION_ID));
    if (!DataUtils.isId(discussionId)) {
      String msg = BeeUtils.joinWords(SVC_GET_DISCUSSION_DATA, "discussion id not received");
      logger.warning(msg);
      return ResponseObject.error(msg);
    }

    return getDiscussionData(discussionId, null);
  }

  private ResponseObject getAnnouncements() {
    Long birthTopic = prm.getRelation(PRM_DISCUSS_BIRTHDAYS);
    DateTime nowStart = new DateTime();
    nowStart.setHour(0);
    nowStart.setMinute(0);
    nowStart.setSecond(0);
    nowStart.setMillis(0);

    DateTime nowFinish = new DateTime();
    nowFinish.setHour(23);
    nowFinish.setMinute(59);
    nowFinish.setSecond(59);
    nowFinish.setMillis(999);

    SqlSelect select = new SqlSelect()
        .addField(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS), COL_DISCUSSION)
        .addField(TBL_ADS_TOPICS, COL_NAME, ALS_TOPIC_NAME)
        .addField(TBL_ADS_TOPICS, COL_ORDINAL, COL_ORDINAL)
        .addField(TBL_DISCUSSIONS, COL_CREATED, COL_CREATED)
        .addField(TBL_DISCUSSIONS, COL_SUBJECT, COL_SUBJECT)
        .addField(TBL_DISCUSSIONS, COL_IMPORTANT, COL_IMPORTANT)
        .addField(TBL_DISCUSSIONS, COL_DESCRIPTION, COL_DESCRIPTION)
        .addField(TBL_PERSONS, COL_FIRST_NAME,
            COL_FIRST_NAME)
        .addField(TBL_PERSONS, COL_LAST_NAME,
            COL_LAST_NAME)
        .addField(TBL_PERSONS, COL_PHOTO,
            COL_PHOTO)
        .addCount(TBL_DISCUSSIONS_FILES, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE)
        .addCount(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION,
                COL_DISCUSSION_COMMENTS)
        .addExpr(
            SqlUtils.sqlIf(
                SqlUtils.isNull(TBL_DISCUSSIONS_USAGE, NewsConstants.COL_USAGE_ACCESS),
                true, null), ALS_NEW_ANNOUCEMENT)
        .addEmptyField(ALS_BIRTHDAY, SqlDataType.BOOLEAN, 1, 0, false)
        .addFrom(TBL_DISCUSSIONS)
        .addFromInner(TBL_ADS_TOPICS, sys.joinTables(TBL_ADS_TOPICS, TBL_DISCUSSIONS, COL_TOPIC))
        .addFromLeft(TBL_DISCUSSIONS_USAGE, SqlUtils.and(SqlUtils.equals(TBL_DISCUSSIONS_USAGE,
            COL_DISCUSSION, SqlUtils.field(TBL_DISCUSSIONS, sys
                .getIdName(TBL_DISCUSSIONS))),
            SqlUtils.equals(TBL_DISCUSSIONS_USAGE, AdministrationConstants.COL_USER, usr
                .getCurrentUserId())))
        .addFromLeft(AdministrationConstants.TBL_USERS,
            sys.joinTables(AdministrationConstants.TBL_USERS, TBL_DISCUSSIONS, COL_OWNER))
        .addFromLeft(
            TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, AdministrationConstants.TBL_USERS,
                COL_COMPANY_PERSON))
        .addFromLeft(
            TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS,
                COL_PERSON))
        .addFromLeft(TBL_DISCUSSIONS_FILES,
            sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_FILES, COL_DISCUSSION))
        .addFromLeft(TBL_DISCUSSIONS_COMMENTS,
                sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION))
        .addFromLeft(TBL_DISCUSSIONS_USERS,
            sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_USERS, COL_DISCUSSION))
        .setWhere(SqlUtils.and(
            SqlUtils.notNull(TBL_ADS_TOPICS, COL_VISIBLE),
            SqlUtils.notNull(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS)),
            SqlUtils.or(
                SqlUtils.and(
                    SqlUtils.moreEqual(TBL_DISCUSSIONS, COL_VISIBLE_TO, System
                        .currentTimeMillis()),
                    SqlUtils.lessEqual(TBL_DISCUSSIONS, COL_VISIBLE_FROM, System
                        .currentTimeMillis())
                    ),
                SqlUtils.or(
                    SqlUtils.equals(TBL_DISCUSSIONS, COL_VISIBLE_TO, nowStart),
                    SqlUtils.equals(TBL_DISCUSSIONS, COL_VISIBLE_FROM, nowStart)
                    ),
                SqlUtils.and(
                    SqlUtils.lessEqual(TBL_DISCUSSIONS, COL_VISIBLE_FROM, System
                        .currentTimeMillis()),
                    SqlUtils.isNull(TBL_DISCUSSIONS, COL_VISIBLE_TO)
                    ),
                SqlUtils.and(
                    SqlUtils.isNull(TBL_DISCUSSIONS, COL_VISIBLE_FROM),
                    SqlUtils.moreEqual(TBL_DISCUSSIONS, COL_VISIBLE_TO, nowFinish)
                    )
                ),
            SqlUtils.or(SqlUtils.or(SqlUtils.and(SqlUtils.equals(TBL_DISCUSSIONS_USERS,
                AdministrationConstants.COL_USER,
                usr.getCurrentUserId()), SqlUtils.notNull(TBL_DISCUSSIONS_USERS, COL_MEMBER)),
                SqlUtils.equals(TBL_DISCUSSIONS, COL_OWNER, usr.getCurrentUserId())
                ),
                SqlUtils.notNull(TBL_DISCUSSIONS, COL_ACCESSIBILITY)
                ))
        )
        .addOrder(TBL_ADS_TOPICS, COL_ORDINAL)
        .addOrderDesc(TBL_DISCUSSIONS, COL_CREATED)
        .addGroup(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS))
        .addGroup(TBL_ADS_TOPICS, COL_NAME, COL_ORDINAL)
        .addGroup(TBL_DISCUSSIONS_USAGE, NewsConstants.COL_USAGE_ACCESS)
        .addGroup(TBL_PERSONS, COL_FIRST_NAME,
            COL_LAST_NAME, COL_PHOTO);
    // logger.info(select.getQuery());
    SimpleRowSet rs = qs.getData(select);

    if (DataUtils.isId(birthTopic)) {
      BeeRowSet topicRows =
          qs.getViewData(VIEW_ADS_TOPICS, Filter.and(Filter.compareId(birthTopic), Filter
              .isNot(Filter.isNull(COL_VISIBLE))));

      if (!topicRows.isEmpty()) {
        BeeRow tRow = topicRows.getRow(topicRows.getNumberOfRows() - 1);
        Integer ordinal =
            tRow.getInteger(topicRows.getColumnIndex(COL_ORDINAL));

        String[] topicData = new String[rs.getNumberOfColumns()];

        topicData[rs.getColumnIndex(ALS_TOPIC_NAME)] =
            tRow.getString(topicRows.getColumnIndex(COL_NAME));
        topicData[rs.getColumnIndex(COL_ORDINAL)] =
            tRow.getString(topicRows.getColumnIndex(COL_ORDINAL));
        topicData[rs.getColumnIndex(ALS_BIRTHDAY)] = BeeUtils.toString(true);

        int placeId = rs.getNumberOfRows() - 1;
        for (int i = 0; i < rs.getNumberOfRows(); i++) {
          Integer ord = BeeUtils.toIntOrNull(rs.getValue(i, rs.getColumnIndex(COL_ORDINAL)));

          if (BeeUtils.compareNullsLast(ord, ordinal) >= 0) {
            if (placeId > i) {
              placeId = i;
            }
          }
        }

        if (placeId >= 0) {
          rs.getRows().add(placeId, topicData);
        } else {
          rs.getRows().add(topicData);
        }
      }
    }

    if (!rs.isEmpty()) {
      ResponseObject resp = ResponseObject.response(rs);
      return resp;
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject getBirthdays() {
    JustDate now = new JustDate(System.currentTimeMillis());

    List<Integer> availableDays = new ArrayList<>();

    now.setDom(now.getDom() - ((DEFAULT_BIRTHDAYS_DAYS_RANGE / 2) + 1));

    for (int i = 0; i < DEFAULT_BIRTHDAYS_DAYS_RANGE + 1; i++) {
      now.increment();
      availableDays.add(now.getDoy());
    }

    List<Long> activeUsers = new ArrayList<>();

    for (UserData userData : usr.getAllUserData()) {
      if (!usr.isBlocked(userData.getLogin())) {
        activeUsers.add(userData.getPerson());
      }
    }

    SqlSelect select = new SqlSelect()
        .addField(TBL_PERSONS, COL_FIRST_NAME,
            COL_FIRST_NAME)
        .addField(TBL_PERSONS, COL_LAST_NAME,
            COL_LAST_NAME)
        .addField(TBL_PERSONS, COL_DATE_OF_BIRTH,
            COL_DATE_OF_BIRTH)
        .addFrom(TBL_PERSONS)
        .setWhere(SqlUtils.and(
            SqlUtils.inList(TBL_PERSONS, sys
                .getIdName(TBL_PERSONS), activeUsers),
            SqlUtils.notNull(TBL_PERSONS, COL_DATE_OF_BIRTH)))
        .addOrder(TBL_PERSONS, COL_DATE_OF_BIRTH);

    SimpleRowSet up = qs.getData(select);
    SimpleRowSet birthdays =
        new SimpleRowSet(new String[] {COL_NAME, COL_DATE_OF_BIRTH, COL_ORDINAL});

    for (String[] upRow : up.getRows()) {
      if (!BeeUtils.isLong(upRow[up.getColumnIndex(COL_DATE_OF_BIRTH)])) {
        continue;
      }

      JustDate date =
          new JustDate(BeeUtils
              .toLong(upRow[up.getColumnIndex(COL_DATE_OF_BIRTH)]));

      if (availableDays.contains(Integer.valueOf(date.getDoy()))) {
        String[] birthdaysRow = new String[] {
            BeeUtils.joinWords(upRow[up.getColumnIndex(COL_FIRST_NAME)],
                upRow[up.getColumnIndex(COL_LAST_NAME)]),
            upRow[up.getColumnIndex(COL_DATE_OF_BIRTH)],
            BeeUtils.toString(date.getDoy())
        };
        birthdays.addRow(birthdaysRow);
      }

    }

    if (!birthdays.isEmpty()) {
      return ResponseObject.response(sortBirhdaysList(birthdays));
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject getDiscussionData(long discussionId, Long commentId) {
    BeeRowSet rowSet = qs.getViewData(VIEW_DISCUSSIONS, Filter.compareId(discussionId));
    if (DataUtils.isEmpty(rowSet)) {
      return ResponseObject.emptyResponse();
    }

    BeeRow data = rowSet.getRow(0);
    addDiscussionProperties(data, rowSet.getColumns(), getDiscussionMembers(discussionId),
        getDiscussionMarks(discussionId),
        commentId);

    return ResponseObject.response(data);
  }

  private List<FileInfo> getDiscussionFiles(long discussionId) {
    List<FileInfo> result = new ArrayList<>();

    BeeRowSet rowSet =
        qs.getViewData(VIEW_DISCUSSIONS_FILES, Filter.equals(COL_DISCUSSION, discussionId));

    if (rowSet == null || rowSet.isEmpty()) {
      return result;
    }

    for (BeeRow row : rowSet.getRows()) {
      FileInfo sf =
          new FileInfo(DataUtils.getLong(rowSet, row, AdministrationConstants.COL_FILE),
              DataUtils.getString(rowSet, row, ALS_FILE_NAME),
              DataUtils.getLong(rowSet, row, ALS_FILE_SIZE),
              DataUtils.getString(rowSet, row, ALS_FILE_TYPE));

      Long commentId = DataUtils.getLong(rowSet, row, COL_COMMENT);

      if (commentId != null) {
        sf.setRelatedId(commentId);
      }

      String caption = DataUtils.getString(rowSet, row, COL_CAPTION);

      if (!BeeUtils.isEmpty(caption)) {
        sf.setCaption(caption);
      }

      sf.setIcon(ExtensionIcons.getIcon(sf.getName()));
      result.add(sf);
    }

    return result;
  }

  private BeeRowSet getDiscussionMarkTypeData() {
    return qs.getViewData(VIEW_DISCUSSIONS_MARK_TYPES);
  }

  private List<Long> getDiscussionMarks(long discussionId) {
    if (!DataUtils.isId(discussionId)) {
      return new ArrayList<>();
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_DISCUSSIONS_COMMENTS_MARKS, sys.getIdName(TBL_DISCUSSIONS_COMMENTS_MARKS))
        .addFrom(TBL_DISCUSSIONS_COMMENTS_MARKS)
        .setWhere(
            SqlUtils.equals(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION,
                discussionId)).addOrder(
            TBL_DISCUSSIONS_COMMENTS_MARKS, sys.getIdName(TBL_DISCUSSIONS_COMMENTS_MARKS));

    Long[] result = qs.getLongColumn(query);
    List<Long> markList = new ArrayList<>();

    if (BeeUtils.isPositive(result.length)) {
      markList = Lists.newArrayList(result);
    }

    return markList;
  }

  private Map<Long, String> getDisscussionsLastComment(Set<Long> discussionIds) {
    Map<Long, String> ls = new HashMap<>();

    SqlSelect select =
        new SqlSelect()
            .addField(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, COL_DISCUSSION)
            .addMax(TBL_DISCUSSIONS_COMMENTS, sys.getIdName(TBL_DISCUSSIONS_COMMENTS),
                ALS_LAST_COMMET)

            .addFrom(TBL_DISCUSSIONS)
            .addFromLeft(TBL_DISCUSSIONS_COMMENTS,
                sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION))
            .setWhere(
                SqlUtils
                    .and(SqlUtils.notNull(TBL_DISCUSSIONS_COMMENTS, sys
                        .getIdName(TBL_DISCUSSIONS_COMMENTS)),
                        SqlUtils.inList(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS),
                            discussionIds)))
            .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION);

    SimpleRowSet maxComments = qs.getData(select);

    if (maxComments.isEmpty()) {
      return ls;
    }

    select =
        new SqlSelect()
            .addField(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, COL_DISCUSSION)
            .addField(TBL_DISCUSSIONS_COMMENTS, COL_PUBLISH_TIME, COL_PUBLISH_TIME)
            .addField(TBL_PERSONS, COL_FIRST_NAME,
                COL_FIRST_NAME)
            .addField(TBL_PERSONS, COL_LAST_NAME,
                COL_LAST_NAME)
            .addFrom(TBL_DISCUSSIONS)
            .addFromLeft(TBL_DISCUSSIONS_COMMENTS,
                sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION))
            .addFromLeft(
                AdministrationConstants.TBL_USERS,
                sys.joinTables(AdministrationConstants.TBL_USERS, TBL_DISCUSSIONS_COMMENTS,
                    COL_PUBLISHER))
            .addFromLeft(
                TBL_COMPANY_PERSONS,
                sys.joinTables(TBL_COMPANY_PERSONS, AdministrationConstants.TBL_USERS,
                    COL_COMPANY_PERSON))
            .addFromLeft(
                TBL_PERSONS,
                sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS,
                    COL_PERSON))
            .setWhere(
                SqlUtils
                    .and(SqlUtils.notNull(TBL_DISCUSSIONS_COMMENTS, sys
                        .getIdName(TBL_DISCUSSIONS_COMMENTS)),
                        SqlUtils.inList(TBL_DISCUSSIONS_COMMENTS,
                            sys.getIdName(TBL_DISCUSSIONS_COMMENTS),
                            (Object[]) maxComments.getColumn(maxComments
                                .getColumnIndex(ALS_LAST_COMMET))),
                        SqlUtils.inList(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS),
                            (Object[]) maxComments.getColumn(maxComments
                                .getColumnIndex(COL_DISCUSSION)))));
    /* .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION); */

    SimpleRowSet rs = qs.getData(select);

    for (String[] row : rs.getRows()) {
      ls.put(BeeUtils.toLong(row[rs.getColumnIndex(COL_DISCUSSION)]),
          BeeUtils.joinWords(TimeUtils.renderDateTime(BeeUtils.unbox(BeeUtils.toLong(row[rs
              .getColumnIndex(COL_PUBLISH_TIME)])))
              + ",", row[rs
              .getColumnIndex(COL_FIRST_NAME)], row[rs
              .getColumnIndex(COL_LAST_NAME)]));
    }

    return ls;
  }

  private Map<Long, Integer> getDiscussionsMarksCount(Set<Long> discussionIds) {
    SqlSelect select = new SqlSelect()
        .addField(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION, COL_DISCUSSION)
        .addCount(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_MARK, PROP_MARK_COUNT)
        .addFrom(TBL_DISCUSSIONS_COMMENTS_MARKS)
        .setWhere(SqlUtils.and(SqlUtils.isNull(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_COMMENT),
            SqlUtils.inList(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION, discussionIds)))
        .addGroup(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION);

    SimpleRowSet rs = qs.getData(select);
    Map<Long, Integer> ls = new HashMap<>();

    for (String[] row : rs.getRows()) {
      ls.put(BeeUtils.toLong(row[rs.getColumnIndex(COL_DISCUSSION)]),
          BeeUtils.toInt(row[rs.getColumnIndex(PROP_MARK_COUNT)]));
    }

    return ls;
  }

  private SimpleRowSet getDiscussionMarksData(List<Long> filter) {
    SqlSelect select = new SqlSelect()
        .addField(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION, COL_DISCUSSION)
        .addField(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_COMMENT, COL_COMMENT)
        .addField(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_MARK, COL_MARK)
        .addField(TBL_DISCUSSIONS_COMMENTS_MARKS, AdministrationConstants.COL_USER,
            AdministrationConstants.COL_USER)
        .addField(TBL_PERSONS, COL_FIRST_NAME,
            COL_FIRST_NAME)
        .addField(TBL_PERSONS, COL_LAST_NAME,
            COL_LAST_NAME)
        .addFrom(TBL_DISCUSSIONS_COMMENTS_MARKS)
        .addFromLeft(AdministrationConstants.TBL_USERS,
            sys.joinTables(AdministrationConstants.TBL_USERS, TBL_DISCUSSIONS_COMMENTS_MARKS,
                AdministrationConstants.COL_USER))
        .addField(TBL_COMMENTS_MARK_TYPES, COL_MARK_NAME, COL_MARK_NAME)
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, AdministrationConstants.TBL_USERS,
                COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS,
                COL_PERSON))
        .addFromLeft(TBL_COMMENTS_MARK_TYPES,
            sys.joinTables(TBL_COMMENTS_MARK_TYPES, TBL_DISCUSSIONS_COMMENTS_MARKS, COL_MARK));

    select.setWhere(SqlUtils.sqlTrue());

    if (!BeeUtils.isEmpty(filter)) {
      select.setWhere(SqlUtils.and(select.getWhere(), SqlUtils.inList(
          TBL_DISCUSSIONS_COMMENTS_MARKS, sys
              .getIdName(TBL_DISCUSSIONS_COMMENTS_MARKS),
          filter)));
    } else {
      select.setWhere(SqlUtils.sqlFalse());
    }

    SimpleRowSet rs = qs.getData(select);

    return rs;
  }

  private List<Long> getDiscussionMembers(long discussionId) {
    if (!DataUtils.isId(discussionId)) {
      return new ArrayList<>();
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_DISCUSSIONS_USERS, AdministrationConstants.COL_USER)
        .addFrom(TBL_DISCUSSIONS_USERS)
        .setWhere(
            SqlUtils.and(SqlUtils.equals(TBL_DISCUSSIONS_USERS, COL_DISCUSSION, discussionId),
                SqlUtils.equals(TBL_DISCUSSIONS_USERS, COL_MEMBER, VALUE_MEMBER))).addOrder(
            TBL_DISCUSSIONS_USERS, sys.getIdName(TBL_DISCUSSIONS_USERS));
    return Lists.newArrayList(qs.getLongColumn(query));
  }

  private Multimap<String, Long> getDiscussionRelations(long discussionId) {
    Multimap<String, Long> res = HashMultimap.create();

    for (String relation : DiscussionsUtils.getRelations()) {
      Long[] ids =
          qs.getRelatedValues(AdministrationConstants.TBL_RELATIONS, COL_DISCUSSION, discussionId,
              relation);

      if (ids != null) {
        String property = DiscussionsUtils.translateRelationToDiscussionProperty(relation);

        for (Long id : ids) {
          res.put(property, id);
        }
      }
    }

    return res;
  }

  private Document renderDiscussionDocument(long discussionId, boolean typeAnnoucement,
      String anouncmentTopic, SimpleRow discussMailRow, LocalizableConstants constants,
      boolean isPublic) {

    String discussSubject = BeeUtils.joinWords(
        (typeAnnoucement ? constants.announcement()
            : constants.discussion()) + BeeConst.STRING_COLON, discussMailRow
            .getValue(COL_SUBJECT));

    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8(), title().text(discussSubject));

    Div panel = div();
    doc.getBody().append(panel);

    boolean important = BeeUtils.unbox(discussMailRow.getBoolean(COL_IMPORTANT));
    String subjectColor = important ? Colors.RED : Colors.BLACK;

    H2 subjectElement = h2().text(discussSubject);
    subjectElement.setColor(subjectColor);
    panel.append(subjectElement);

    Tbody tableFields = tbody().append(
        tr().append(td().text(constants.date()),
            td().text(TimeUtils.renderCompact(discussMailRow.getDate(COL_CREATED)))));

    if (typeAnnoucement) {
      tableFields.append(
          tr().append(td().text(constants.adTopic()),
              td().text(anouncmentTopic))
          );
    }

    Div discussDescriptionContent = div().text(discussMailRow.getValue(COL_DESCRIPTION));
    // discussDescriptionContent.setMaxHeight(4, CssUnit.EM);
    // discussDescriptionContent.setOverflow(Overflow.HIDDEN);

    tableFields.append(
        tr().append(td().text(constants.discussOwner()),
            td().text(usr.getUserSign(discussMailRow.getLong(COL_OWNER)))),
        tr().append(td().text(constants.discussDescription()),
            td().append(discussDescriptionContent))
        );

    if (isPublic && !typeAnnoucement) {
      tableFields.append(
          tr().append(td().text(constants.discussMembers()),
              td().text(constants.systemAllUsers()))
          );
    } else if (!isPublic) {

      List<Long> discussMemberIds = getDiscussionMembers(discussionId);

      String memberList = "";

      for (Long userId : discussMemberIds) {

        memberList = BeeUtils.joinNoDuplicates(BeeConst.STRING_COMMA + BeeConst.STRING_SPACE,
            memberList, usr.getUserSign(userId));
      }

      tableFields.append(
          tr().append(td().text(constants.discussMembers()),
              td().text(memberList))
          );
    }

    List<Element> cells = tableFields.queryTag(Tags.TD);
    for (Element cell : cells) {
      if (cell.index() == 0) {
        cell.setPaddingRight(1, CssUnit.EM);
        cell.setFontWeight(FontWeight.BOLDER);
        cell.setVerticalAlign(VerticalAlign.TEXT_TOP);
      }
    }

    panel.append(table().append(tableFields));

    return doc;
  }

  private ResponseObject registerDiscussionVisit(long discussionId, long userId, long mills) {
    IsCondition where =
        SqlUtils.and(
            SqlUtils.equals(TBL_DISCUSSIONS_USERS, COL_DISCUSSION, discussionId,
                AdministrationConstants.COL_USER, userId), SqlUtils.equals(TBL_DISCUSSIONS_USERS,
                COL_MEMBER, new Integer(1)));

    return qs.updateDataWithResponse(new SqlUpdate(TBL_DISCUSSIONS_USERS).addConstant(
        COL_LAST_ACCESS, mills)
        .setWhere(where));
  }

  private static SimpleRowSet sortBirhdaysList(SimpleRowSet birthdays) {
    List<Integer> ordinals = Lists.newArrayList(birthdays.getIntColumn(COL_ORDINAL));
    Collections.sort(ordinals);
    SimpleRowSet sorted = new SimpleRowSet(birthdays.getColumnNames());

    for (Integer ordinal : ordinals) {
      for (String[] row : birthdays.getRows()) {
        Integer doy = BeeUtils.toIntOrNull(row[birthdays.getColumnIndex(COL_ORDINAL)]);

        if (doy.compareTo(ordinal) == 0) {
          sorted.addRow(row);
          birthdays.getRows().remove(row);
          break;
        }
      }
    }

    return sorted;
  }

  private ResponseObject sendNewDiscussionMail(long discussionId, boolean typeAnnoucement,
      String annoucementTopic, boolean notifyEmailPreference, boolean sendAll) {

    Long senderAccountId = mail.getSenderAccountId(typeAnnoucement
        ? LOG_CREATE_ANNOUNCEMENT_LABEL
        : LOG_CREATE_DISCUSSION_LABEL);

    ResponseObject response = ResponseObject.emptyResponse();
    String label = typeAnnoucement ? LOG_MAIL_NEW_ANNOUNCEMENT_LABEL
        : LOG_MAIL_NEW_DISCUSSION_LABEL;

    if (senderAccountId == null) {
      return response;
    }

    SqlSelect discussMailList =
        new SqlSelect()
            .addFields(TBL_DISCUSSIONS, COL_SUBJECT, COL_DESCRIPTION, COL_OWNER,
                COL_CREATED, COL_TOPIC, COL_IMPORTANT)
            .addFrom(TBL_DISCUSSIONS)
            .setDistinctMode(true);

    HasConditions where =
        SqlUtils.and(SqlUtils.equals(TBL_DISCUSSIONS, COL_DISCUSSION_ID, discussionId));

    if (notifyEmailPreference && sendAll) {
      discussMailList.addField(TBL_USERS, sys.getIdName(TBL_USERS), DiscussionsConstants.COL_USER);

      /* Using Cartesian product force sending all mails */
      discussMailList.addFrom(TBL_USERS);

      where.add(SqlUtils.notEqual(TBL_DISCUSSIONS, COL_OWNER,
          SqlUtils.field(TBL_USERS, sys.getIdName(TBL_USERS))));

    } else if (!notifyEmailPreference && sendAll) {
      discussMailList.addField(TBL_USER_SETTINGS, AdministrationConstants.COL_USER,
          DiscussionsConstants.COL_USER);

      /* Using Cartesian product for sending all mails by settings */
      discussMailList.addFrom(TBL_USER_SETTINGS);

      where.add(SqlUtils.notEqual(TBL_DISCUSSIONS, COL_OWNER,
          SqlUtils.field(TBL_USER_SETTINGS, AdministrationConstants.COL_USER)));

      where.add(typeAnnoucement ? SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_NEW_ANNOUNCEMENTS)
          : SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_NEW_DISCUSSIONS));

    } else if (notifyEmailPreference && !sendAll) {
      discussMailList.addField(TBL_USERS, sys.getIdName(TBL_USERS), DiscussionsConstants.COL_USER);

      discussMailList
          .addFromInner(TBL_DISCUSSIONS_USERS,
              sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_USERS, COL_DISCUSSION))
          .addFromInner(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_DISCUSSIONS_USERS, DiscussionsConstants.COL_USER));

      where.add(SqlUtils.notEqual(TBL_DISCUSSIONS, COL_OWNER,
          SqlUtils.field(TBL_DISCUSSIONS_USERS, DiscussionsConstants.COL_USER)));

    } else if (!notifyEmailPreference && !sendAll) {
      discussMailList.addField(TBL_USERS, sys.getIdName(TBL_USERS), DiscussionsConstants.COL_USER);

      discussMailList
          .addFromInner(TBL_DISCUSSIONS_USERS,
              sys.joinTables(TBL_DISCUSSIONS, TBL_DISCUSSIONS_USERS, COL_DISCUSSION))
          .addFromInner(TBL_USERS,
              sys.joinTables(TBL_USERS, TBL_DISCUSSIONS_USERS, DiscussionsConstants.COL_USER))
          .addFromInner(TBL_USER_SETTINGS,
              sys.joinTables(TBL_USERS, TBL_USER_SETTINGS,
                  AdministrationConstants.COL_USER));

      where.add(SqlUtils.notEqual(TBL_DISCUSSIONS, COL_OWNER,
          SqlUtils.field(TBL_DISCUSSIONS_USERS, DiscussionsConstants.COL_USER)));

      where.add(typeAnnoucement ? SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_NEW_ANNOUNCEMENTS)
          : SqlUtils.notNull(TBL_USER_SETTINGS, COL_MAIL_NEW_DISCUSSIONS));

    }

    discussMailList.setWhere(where);

    logger.warning(typeAnnoucement ? LOG_CREATE_ANNOUNCEMENT_LABEL : LOG_CREATE_DISCUSSION_LABEL,
        "query:",
        discussMailList.getQuery());

    SimpleRowSet discussMailListRowSet = qs.getData(discussMailList);

    if (DataUtils.isEmpty(discussMailListRowSet)) {
      return response;
    }

    for (SimpleRow discussMailRow : discussMailListRowSet) {
      Long member = discussMailRow.getLong(DiscussionsConstants.COL_USER);

      if (usr.isBlocked(usr.getUserName(member))) {
        logger.warning(label, discussionId, "member", member, "is blocked");
        continue;
      }

      String memberEmail = usr.getUserEmail(member, false);

      if (BeeUtils.isEmpty(memberEmail)) {
        logger.warning(label, discussionId, "member", member, "email not available");
        continue;
      }

      LocalizableConstants constants = usr.getLocalizableConstants(member);

      if (constants == null) {
        logger.warning(label, discussionId, "member", member, "localization not available");
        continue;
      }

      Document discussMailDocument =
          renderDiscussionDocument(discussionId, typeAnnoucement, annoucementTopic, discussMailRow,
              constants, sendAll);

      String htmlDiscussMailContent = discussMailDocument.buildLines();

      logger.info(label, discussionId, "mail to", member, memberEmail);

      String subject = typeAnnoucement ? constants.discussMailNewAnnouncementSubject()
          : constants.discussMailNewDiscussionSubject();

      ResponseObject mailResponse = mail.sendMail(senderAccountId, memberEmail, subject,
          htmlDiscussMailContent);

      if (mailResponse.hasErrors()) {
        response.addWarning("Send mail failed");
      }

    }

    return response;
  }

  private ResponseObject updateDiscussionRelations(long discussionId, Set<String> updatedRelations,
      BeeRow row) {
    ResponseObject response = new ResponseObject();
    List<RowChildren> children = new ArrayList<>();

    for (String property : updatedRelations) {
      String relation = DiscussionsUtils.translateDiscussionPropertyToRelation(property);

      if (!BeeUtils.isEmpty(relation)) {
        children.add(RowChildren.create(AdministrationConstants.TBL_RELATIONS, COL_DISCUSSION,
            discussionId, relation, row.getProperty(property)));
      }
    }

    int count = 0;

    if (!BeeUtils.isEmpty(children)) {
      count = deb.commitChildren(discussionId, children, response);
    }

    return response.setResponse(count);
  }

  private void updateDiscussionUsers(long discussionId, Collection<Long> oldUsers,
      Collection<Long> newUsers) {
    List<Long> insert = new ArrayList<>(newUsers);
    insert.removeAll(oldUsers);

    List<Long> delete = new ArrayList<>(oldUsers);
    delete.removeAll(newUsers);

    for (Long user : insert) {
      createDiscussionUser(discussionId, user, null, true);
    }

    for (Long user : delete) {
      IsCondition condition =
          SqlUtils.equals(TBL_DISCUSSIONS_USERS, COL_DISCUSSION, discussionId,
              AdministrationConstants.COL_USER, user);
      qs.updateData(new SqlUpdate(TBL_DISCUSSIONS_USERS).addConstant(COL_MEMBER, false).setWhere(
          condition));
    }
  }

}
