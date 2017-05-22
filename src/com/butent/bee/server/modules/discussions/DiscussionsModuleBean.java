package com.butent.bee.server.modules.discussions;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;

import com.butent.bee.server.Config;
import com.butent.bee.server.concurrency.ConcurrencyBean;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
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
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants.*;
import com.butent.bee.shared.modules.discussions.DiscussionsUtils;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
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
  @EJB
  ConcurrencyBean cb;
  @EJB
  AdministrationModuleBean adm;

  @Resource
  EJBContext ctx;
  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();
    result.addAll(qs.getSearchResults(VIEW_DISCUSSIONS,
        Filter.anyContains(Sets.newHashSet(COL_SUBJECT, COL_DESCRIPTION, ALS_OWNER_FIRST_NAME,
            ALS_OWNER_LAST_NAME), query)));

    result.addAll(qs.getSearchResults(VIEW_DISCUSSIONS_FILES, Filter.and(
        Filter.anyContains(Sets.newHashSet(AdministrationConstants.COL_FILE_CAPTION,
            AdministrationConstants.ALS_FILE_NAME, COL_COMMENT_TEXT), query),
        Filter.in(COL_COMMENT, VIEW_DISCUSSIONS_COMMENTS, COL_DISCUSSION_COMMENT_ID,
            Filter.or(Filter.isNull(COL_DELETED),
                Filter.isEqual(COL_DELETED, BooleanValue.FALSE))))));

    return result;

  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

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
        BeeParameter.createBoolean(module, PRM_ALLOW_DELETE_OWN_COMMENTS),
        BeeParameter.createNumber(module, PRM_DISCUSS_INACTIVE_TIME_IN_DAYS),
        BeeParameter.createText(module, PRM_FORBIDDEN_FILES_EXTENTIONS, false, ""),
        BeeParameter.createNumber(module, PRM_MAX_UPLOAD_FILE_SIZE),
        BeeParameter.createRelation(module, PRM_DISCUSS_BIRTHDAYS, TBL_ADS_TOPICS, COL_NAME)
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
    sys.registerDataEventHandler(new DataEventHandler() {

      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_DISCUSSIONS) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();

          Set<Long> discussionsIds = new HashSet<>();
          discussionsIds.addAll(rowSet.getRowIds().subList(0, BeeUtils.min(rowSet
              .getNumberOfRows(), MAX_NUMBERS_OF_ROWS)));

          Long userId = usr.getCurrentUserId();
          setRowAccessProperties(discussionsIds, rowSet);

          SimpleRowSet markCounts = getDiscussionsMarksCount(discussionsIds);
          SimpleRowSet commentData = getDiscussionsLastComment(discussionsIds);
          SimpleRowSet fileData = getDiscussionFileData(discussionsIds);
          Set<Long> relDiscussionData = getRelatedDiscussionIds(discussionsIds);

          DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo();

          for (BeeRow row : rowSet.getRows().subList(0,
              BeeUtils.min(rowSet.getNumberOfRows(), MAX_NUMBERS_OF_ROWS))) {

            if (markCounts != null) {
              SimpleRow mark = markCounts.getRowByKey(COL_DISCUSSION,
                  BeeUtils.toString(row.getId()));

              String markValue = mark != null
                  ? BeeUtils.nvl(mark.getValue(PROP_MARK_COUNT), BeeConst.STRING_EMPTY)
                  : BeeConst.STRING_EMPTY;

              row.setProperty(PROP_MARK_COUNT, markValue);
            } else {
              row.setProperty(PROP_MARK_COUNT, BeeConst.STRING_EMPTY);
            }

            if (commentData != null) {
              SimpleRow lastCommentData = commentData.getRowByKey(COL_DISCUSSION,
                  BeeUtils.toString(row.getId()));

              long publishTime = BeeUtils.unbox(lastCommentData.getLong(COL_PUBLISH_TIME));

              String lastCommentVal = lastCommentData != null
                  ? BeeUtils.joinWords(
                  Formatter.renderDateTime(dtfInfo, TimeUtils.dropMillis(publishTime)) + ",",
                  lastCommentData.getValue(COL_FIRST_NAME), lastCommentData.getValue(COL_LAST_NAME))
                  : BeeConst.STRING_EMPTY;

              row.setProperty(PROP_LAST_COMMENT_DATA, lastCommentVal);
              row.setProperty(PROP_COMMENT_COUNT, lastCommentData != null
                  ? BeeUtils.nvl(lastCommentData.getValue(PROP_COMMENT_COUNT),
                  BeeConst.STRING_EMPTY)
                  : BeeConst.STRING_EMPTY);
            } else {
              row.setProperty(PROP_LAST_COMMENT_DATA, BeeConst.STRING_EMPTY);
              row.setProperty(PROP_COMMENT_COUNT, BeeConst.STRING_EMPTY);
            }

            if (fileData != null) {
              SimpleRow fileDataRow = fileData.getRowByKey(COL_DISCUSSION,
                  BeeUtils.toString(row.getId()));

              row.setProperty(PROP_FILES_COUNT, fileDataRow != null
                  ? BeeUtils.nvl(fileDataRow.getValue(PROP_FILES_COUNT), BeeConst.STRING_EMPTY)
                  : BeeConst.STRING_EMPTY);
            } else {
              row.setProperty(PROP_FILES_COUNT, BeeConst.STRING_EMPTY);
            }

            String relValue = relDiscussionData.contains(row.getId()) ? IMG_LINK : "";

            row.setProperty(PROP_RELATIONS_COUNT, relValue);

            if (BeeUtils.isEmpty(row.getProperty(PROP_USER, userId))) {
              row.setProperty(PROP_USER, userId, BeeConst.STRING_PLUS);
              createDiscussionUser(row.getId(), usr.getCurrentUserId(), null, false);
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setMarkTypesRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_DISCUSSIONS_MARK_TYPES) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();

          if (rowSet.getNumberOfRows() < MAX_NUMBERS_OF_ROWS) {
            for (BeeRow row : rowSet.getRows()) {
              row.setProperty(PROP_PREVIEW_IMAGE,
                  row.getString(rowSet.getColumnIndex(COL_IMAGE_RESOURCE_NAME)));
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.DISCUSSIONS, new DiscussionsUsageQueryProvider());
    news.registerUsageQueryProvider(Feed.ANNOUNCEMENTS, new AnnouncementsUsageQueryProvider());
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
      boolean checkUsers, Long commentId) {

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

    String reasonText = BeeUtils.joinWords("<i style=\"font-size: smaller; color:red\">(",
        usr.getDictionary().discussEventCommentDeleted() + " )</i>:",
        Formatter.renderDateTime(usr.getDateTimeFormatInfo(), new DateTime()) + ",",
        usr.getCurrentUserSign());

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
    switch (event) {
      case CREATE:
        Map<String, String> properties = discussRow.getProperties();

        if (properties == null) {
          properties = new HashMap<>();
        }

        List<Long> members = DataUtils.parseIdList(properties.get(PROP_MEMBERS));
        Long[] groupMembers =
            adm.getUserGroupMembers(properties.get(PROP_MEMBER_GROUP)).getLongColumn(COL_UG_USER);

        if (!ArrayUtils.isEmpty(groupMembers)) {
          for (Long member : groupMembers) {
            if (!members.contains(member)) {
              members.add(member);
            }
          }
        }

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

        BeeRow createdDiscussion = (BeeRow) response.getResponse();

        if (!response.hasErrors()) {
          response =
              createDiscussionUser(createdDiscussion.getId(), currentUser, now,
                  !BeeUtils.isEmpty(members));
        }

        if (!response.hasErrors()) {
          for (long memberId : members) {
            if (memberId != currentUser) {
              response = createDiscussionUser(createdDiscussion.getId(), memberId, null, true);
              if (response.hasErrors()) {
                break;
              }
            }
          }
        }

        if (response.hasErrors()) {
          break;
        }

        if (response.hasErrors()) {
          response = ResponseObject.error(usr.getDictionary().discussNotCreated());
        } else {
          response = ResponseObject.response(createdDiscussion);
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

        if (!(DataUtils.getColumnIndex(COL_TOPIC, discussData.getColumns(), false) < 0)) {
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
        response = registerDiscussionVisit(discussionId, currentUser, now);

        if (response == null || !response.hasErrors()) {
          response =
              commitDiscussionData(discussData, oldMembers, false, commentId);
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
          response = registerDiscussionVisit(discussionId, currentUser, now);
        }

        if (response == null || !response.hasErrors()) {
          response =
              commitDiscussionData(discussData, oldMembers, true, commentId);
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

  @Schedule(hour = "*/12", persistent = false)
  private void doInactiveDiscussions() {
    if (!Config.isInitialized()) {
      return;
    }
    Long days = prm.getLong(PRM_DISCUSS_INACTIVE_TIME_IN_DAYS);

    if (!BeeUtils.isPositive(days)) {
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

    String colMaxPublishTime = SqlUtils.uniqueName();
    String colMaxCommentId = SqlUtils.uniqueName();
    String tblMaxComments = SqlUtils.uniqueName();
    String alsDiscussComments = SqlUtils.uniqueName();

    SqlSelect maxCommentQuery = new SqlSelect()
        .addField(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, COL_DISCUSSION)
        .addMax(TBL_DISCUSSIONS_COMMENTS, COL_PUBLISH_TIME, colMaxPublishTime)
        .addMax(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION_COMMENT_ID, colMaxCommentId)
        .addFrom(TBL_DISCUSSIONS_COMMENTS)
        .setWhere(SqlUtils.notEqual(TBL_DISCUSSIONS_COMMENTS, DiscussionsConstants.COL_PUBLISHER,
            usr.getCurrentUserId()))
        .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, COL_PUBLISHER);

    SqlSelect select =
        new SqlSelect()
            .addField(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS), COL_DISCUSSION)
            .addField(TBL_DISCUSSIONS, COL_DISCUSSION_ID, COL_DISCUSSION_ID)
            .addField(TBL_ADS_TOPICS, COL_NAME, ALS_TOPIC_NAME)
            .addField(TBL_ADS_TOPICS, COL_ORDINAL, COL_ORDINAL)
            .addFields(TBL_ADS_TOPICS, COL_BACKGROUND_COLOR)
            .addFields(TBL_ADS_TOPICS, COL_TEXT_COLOR)
            .addField(TBL_DISCUSSIONS, COL_CREATED, COL_CREATED)
            .addField(TBL_DISCUSSIONS, COL_SUBJECT, COL_SUBJECT)
            .addField(TBL_DISCUSSIONS, COL_SUMMARY, COL_SUMMARY)
            .addField(TBL_DISCUSSIONS, COL_STATUS, COL_STATUS)
            .addField(TBL_DISCUSSIONS, COL_IMPORTANT, COL_IMPORTANT)
            .addField(TBL_DISCUSSIONS, COL_DESCRIPTION, COL_DESCRIPTION)
            .addFields(TBL_DISCUSSIONS_USAGE, NewsConstants.COL_USAGE_ACCESS)
            .addField(TBL_PERSONS, COL_FIRST_NAME, COL_FIRST_NAME)
            .addField(TBL_PERSONS, COL_LAST_NAME, COL_LAST_NAME)
            .addField(TBL_FILES, COL_FILE_HASH, COL_PHOTO)
            .addMax(TBL_DISCUSSIONS_COMMENTS, COL_PUBLISH_TIME, COL_PUBLISH_TIME)
            .addMax(tblMaxComments, colMaxPublishTime, ALS_MAX_PUBLISH_TIME)
            .addCount(TBL_DISCUSSIONS_FILES, AdministrationConstants.COL_FILE,
                AdministrationConstants.COL_FILE)
            .addCountDistinct(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION_COMMENT_ID,
                COL_DISCUSSION_COMMENT_ID)
            .addExpr(
                SqlUtils.sqlIf(
                    SqlUtils.isNull(TBL_DISCUSSIONS_USAGE, NewsConstants.COL_USAGE_ACCESS),
                    true, null), ALS_NEW_ANNOUCEMENT)
            .addEmptyText(ALS_BIRTHDAY)
            .addFrom(TBL_DISCUSSIONS)
            .addFromInner(TBL_ADS_TOPICS,
                sys.joinTables(TBL_ADS_TOPICS, TBL_DISCUSSIONS, COL_TOPIC))
            .addFromLeft(maxCommentQuery, tblMaxComments,
                SqlUtils.join(TBL_DISCUSSIONS, COL_DISCUSSION_ID,
                    tblMaxComments, COL_DISCUSSION))
            .addFromLeft(TBL_DISCUSSIONS_COMMENTS, alsDiscussComments,
                SqlUtils.join(tblMaxComments,
                    colMaxCommentId,
                    alsDiscussComments, COL_DISCUSSION_COMMENT_ID))
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
            .addFromLeft(TBL_FILES, sys.joinTables(TBL_FILES, TBL_PERSONS, COL_PHOTO))
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
                            .currentTimeMillis())),
                    SqlUtils.or(
                        SqlUtils.equals(TBL_DISCUSSIONS, COL_VISIBLE_TO, nowStart),
                        SqlUtils.equals(TBL_DISCUSSIONS, COL_VISIBLE_FROM, nowStart)),
                    SqlUtils.and(
                        SqlUtils.lessEqual(TBL_DISCUSSIONS, COL_VISIBLE_FROM, System
                            .currentTimeMillis()),
                        SqlUtils.isNull(TBL_DISCUSSIONS, COL_VISIBLE_TO)),
                    SqlUtils.and(
                        SqlUtils.isNull(TBL_DISCUSSIONS, COL_VISIBLE_FROM),
                        SqlUtils.moreEqual(TBL_DISCUSSIONS, COL_VISIBLE_TO, nowFinish)
                    )
                ),
                SqlUtils.or(SqlUtils.or(SqlUtils.and(SqlUtils.equals(TBL_DISCUSSIONS_USERS,
                    AdministrationConstants.COL_USER,
                    usr.getCurrentUserId()), SqlUtils.notNull(TBL_DISCUSSIONS_USERS, COL_MEMBER)),
                    SqlUtils.equals(TBL_DISCUSSIONS, COL_OWNER, usr.getCurrentUserId())),
                    SqlUtils.notNull(TBL_DISCUSSIONS, COL_ACCESSIBILITY))))
            .addOrder(TBL_DISCUSSIONS, COL_IMPORTANT)
            .addOrderDesc(TBL_DISCUSSIONS, COL_CREATED)
            .addOrder(TBL_ADS_TOPICS, COL_ORDINAL)
            .addGroup(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS))
            .addGroup(TBL_ADS_TOPICS, COL_NAME, COL_ORDINAL, COL_BACKGROUND_COLOR, COL_TEXT_COLOR)
            .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION)
            .addGroup(TBL_DISCUSSIONS_USAGE, NewsConstants.COL_USAGE_ACCESS)
            .addGroup(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
            .addGroup(TBL_FILES, COL_FILE_HASH);

    SimpleRowSet rs = qs.getData(select);

    if (DataUtils.isId(birthTopic)) {
      BeeRowSet topicRows =
          qs.getViewData(VIEW_ADS_TOPICS, Filter.and(Filter.compareId(birthTopic), Filter
              .isNot(Filter.isNull(COL_VISIBLE))));

      ResponseObject birthResp = getBirthdays();

      if (!topicRows.isEmpty() && birthResp.hasResponse(SimpleRowSet.class)) {
        BeeRow tRow = topicRows.getRow(topicRows.getNumberOfRows() - 1);

        String[] topicData = new String[rs.getNumberOfColumns()];

        topicData[rs.getColumnIndex(ALS_TOPIC_NAME)] =
            tRow.getString(topicRows.getColumnIndex(COL_NAME));
        topicData[rs.getColumnIndex(COL_BACKGROUND_COLOR)] =
            tRow.getString(topicRows.getColumnIndex(COL_BACKGROUND_COLOR));
        topicData[rs.getColumnIndex(COL_TEXT_COLOR)] =
            tRow.getString(topicRows.getColumnIndex(COL_TEXT_COLOR));
        topicData[rs.getColumnIndex(COL_ORDINAL)] =
            tRow.getString(topicRows.getColumnIndex(COL_ORDINAL));
        topicData[rs.getColumnIndex(ALS_BIRTHDAY)] = Codec.beeSerialize(birthResp.getResponse());

        int placeId = 0;
        rs.getRows().add(placeId, topicData);

      }
    }

    if (!rs.isEmpty()) {
      return ResponseObject.response(Pair.of(rs, getDiscussionFiles(rs)));
    }

    return ResponseObject.emptyResponse();
  }

  private SimpleRowSet getDiscussionFiles(SimpleRowSet discussionRs) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_DISCUSSIONS_FILES, COL_DISCUSSION, AdministrationConstants.COL_FILE,
            AdministrationConstants.COL_FILE_CAPTION)
        .addFields(TBL_FILES, COL_FILE_HASH, COL_FILE_NAME, COL_FILE_SIZE, COL_FILE_TYPE)
        .addFrom(TBL_DISCUSSIONS_FILES)
        .addFromInner(TBL_FILES,
            sys.joinTables(TBL_FILES, TBL_DISCUSSIONS_FILES, AdministrationConstants.COL_FILE))
        .addFromLeft(TBL_DISCUSSIONS_COMMENTS,
            sys.joinTables(TBL_DISCUSSIONS_COMMENTS, TBL_DISCUSSIONS_FILES, COL_COMMENT))
        .setWhere(SqlUtils.and(SqlUtils.inList(TBL_DISCUSSIONS_FILES, COL_DISCUSSION,
            (Object[]) discussionRs.getColumn(discussionRs
                .getColumnIndex(sys.getIdName(TBL_DISCUSSIONS)))),
            SqlUtils.or(SqlUtils.isNull(TBL_DISCUSSIONS_COMMENTS, COL_DELETED),
                SqlUtils.equals(TBL_DISCUSSIONS_COMMENTS, COL_DELETED, false))));

    return qs.getData(query);
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
      if (usr.isActive(userData.getUserId())) {
        activeUsers.add(userData.getPerson());
      }
    }

    SqlSelect select = new SqlSelect()
        .addField(TBL_PERSONS, COL_FIRST_NAME,
            COL_FIRST_NAME)
        .addField(TBL_PERSONS, COL_LAST_NAME,
            COL_LAST_NAME)
        .addField(TBL_FILES, COL_FILE_HASH, COL_PHOTO)
        .addField(TBL_PERSONS, COL_DATE_OF_BIRTH,
            COL_DATE_OF_BIRTH)
        .addFrom(TBL_PERSONS)
        .addFromLeft(TBL_FILES, sys.joinTables(TBL_FILES, TBL_PERSONS, COL_PHOTO))
        .setWhere(SqlUtils.and(
            SqlUtils.inList(TBL_PERSONS, sys
                .getIdName(TBL_PERSONS), activeUsers),
            SqlUtils.notNull(TBL_PERSONS, COL_DATE_OF_BIRTH)))
        .addOrder(TBL_PERSONS, COL_DATE_OF_BIRTH);

    SimpleRowSet up = qs.getData(select);
    SimpleRowSet birthdays =
        new SimpleRowSet(new String[] {COL_NAME, COL_PHOTO, COL_DATE_OF_BIRTH, COL_ORDINAL});

    for (String[] upRow : up.getRows()) {
      if (!BeeUtils.isLong(upRow[up.getColumnIndex(COL_DATE_OF_BIRTH)])) {
        continue;
      }

      JustDate date =
          new JustDate(BeeUtils
              .toLong(upRow[up.getColumnIndex(COL_DATE_OF_BIRTH)]));

      if (availableDays.contains(date.getDoy())) {
        String[] birthdaysRow = new String[] {
            BeeUtils.joinWords(upRow[up.getColumnIndex(COL_FIRST_NAME)], upRow[up.getColumnIndex(
                COL_LAST_NAME)]),
            upRow[up.getColumnIndex(COL_PHOTO)],
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
              DataUtils.getString(rowSet, row, COL_FILE_HASH),
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

  private SimpleRowSet getDiscussionFileData(Set<Long> discussionIds) {
    if (BeeUtils.isEmpty(discussionIds)) {
      return null;
    }
    SqlSelect query = new SqlSelect()
        .addFields(TBL_DISCUSSIONS_FILES, COL_DISCUSSION)
        .addCount(TBL_DISCUSSIONS_FILES, COL_FILE, PROP_FILES_COUNT)
        .addFrom(TBL_DISCUSSIONS_FILES)
        .addFromLeft(TBL_DISCUSSIONS_COMMENTS, sys.joinTables(TBL_DISCUSSIONS_COMMENTS,
            TBL_DISCUSSIONS_FILES, COL_COMMENT))
        .setWhere(SqlUtils.and(
            SqlUtils.isNull(TBL_DISCUSSIONS_COMMENTS, COL_DELETED),
            SqlUtils.inList(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, discussionIds)
        ))
        .addGroup(TBL_DISCUSSIONS_FILES, COL_DISCUSSION);

    return qs.getData(query);
  }

  private SimpleRowSet getDiscussionsLastComment(Set<Long> discussionIds) {
    //    Map<Long, String> ls = new HashMap<>();

    SqlSelect select = new SqlSelect()
        .addField(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, COL_DISCUSSION)
        .addMax(TBL_DISCUSSIONS_COMMENTS, sys.getIdName(TBL_DISCUSSIONS_COMMENTS), ALS_LAST_COMMET)
        .addCount(TBL_DISCUSSIONS_COMMENTS, sys.getIdName(TBL_DISCUSSIONS_COMMENTS),
            PROP_COMMENT_COUNT)
        .addFrom(TBL_DISCUSSIONS)
        .addFromLeft(TBL_DISCUSSIONS_COMMENTS, sys.joinTables(TBL_DISCUSSIONS,
            TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_DISCUSSIONS_COMMENTS,
            sys.getIdName(TBL_DISCUSSIONS_COMMENTS)), SqlUtils.inList(TBL_DISCUSSIONS,
            sys.getIdName(TBL_DISCUSSIONS), discussionIds)))
        .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION);

    SimpleRowSet maxComments = qs.getData(select);

    if (maxComments.isEmpty()) {
      return null;
    }

    select = new SqlSelect()
        .addField(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION, COL_DISCUSSION)
        .addField(TBL_DISCUSSIONS_COMMENTS, COL_PUBLISH_TIME, COL_PUBLISH_TIME)
        .addField(TBL_PERSONS, COL_FIRST_NAME, COL_FIRST_NAME)
        .addField(TBL_PERSONS, COL_LAST_NAME, COL_LAST_NAME)
        .addEmptyInt(PROP_COMMENT_COUNT)
        .addFrom(TBL_DISCUSSIONS)
        .addFromLeft(TBL_DISCUSSIONS_COMMENTS, sys.joinTables(TBL_DISCUSSIONS,
            TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION))
        .addFromLeft(AdministrationConstants.TBL_USERS,
            sys.joinTables(AdministrationConstants.TBL_USERS, TBL_DISCUSSIONS_COMMENTS,
                COL_PUBLISHER))
        .addFromLeft(TBL_COMPANY_PERSONS, sys.joinTables(TBL_COMPANY_PERSONS,
            AdministrationConstants.TBL_USERS, COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_DISCUSSIONS_COMMENTS,
            sys.getIdName(TBL_DISCUSSIONS_COMMENTS)), SqlUtils.inList(TBL_DISCUSSIONS_COMMENTS,
            sys.getIdName(TBL_DISCUSSIONS_COMMENTS), (Object[])
                maxComments.getColumn(maxComments.getColumnIndex(ALS_LAST_COMMET))),
            SqlUtils.inList(TBL_DISCUSSIONS, sys.getIdName(TBL_DISCUSSIONS),
                (Object[]) maxComments.getColumn(maxComments.getColumnIndex(COL_DISCUSSION)))));
    /* .addGroup(TBL_DISCUSSIONS_COMMENTS, COL_DISCUSSION); */

    SimpleRowSet rs = qs.getData(select);

    String count = maxComments.getValueByKey(ALS_LAST_COMMET, rs.getRow(0).getValue(COL_DISCUSSION),
        PROP_COMMENT_COUNT);
    for (SimpleRow row : rs) {
      row.setValue(PROP_COMMENT_COUNT, maxComments.getValueByKey(COL_DISCUSSION,
          row.getValue(COL_DISCUSSION), PROP_COMMENT_COUNT));
    }

    return rs;
  }

  private SimpleRowSet getDiscussionsMarksCount(Set<Long> discussionIds) {
    if (BeeUtils.isEmpty(discussionIds)) {
      return null;
    }
    SqlSelect select = new SqlSelect()
        .addFields(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION)
        .addCount(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_MARK, PROP_MARK_COUNT)
        .addFrom(TBL_DISCUSSIONS_COMMENTS_MARKS)
        .setWhere(SqlUtils.and(SqlUtils.isNull(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_COMMENT),
            SqlUtils.inList(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION, discussionIds)))
        .addGroup(TBL_DISCUSSIONS_COMMENTS_MARKS, COL_DISCUSSION);

    SimpleRowSet rs = qs.getData(select);

    return rs;
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

    return qs.getData(select);
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

  private Set<Long> getRelatedDiscussionIds(Set<Long> discussions) {
    if (BeeUtils.isEmpty(discussions)) {
      return new HashSet<>();
    }

    SqlSelect query = new SqlSelect()
        .addFields(TBL_RELATIONS, COL_DISCUSSION)
        .addFrom(TBL_RELATIONS)
        .setWhere(SqlUtils.and(
            SqlUtils.inList(TBL_RELATIONS, COL_DISCUSSION, discussions),
            SqlUtils.notNull(TBL_RELATIONS, COL_DISCUSSION)
        ))
        .addGroup(TBL_RELATIONS, COL_DISCUSSION);

    return qs.getLongSet(query);
  }

  private Document renderDiscussionDocument(long discussionId, boolean typeAnnoucement,
      String anouncmentTopic, SimpleRow discussMailRow, Dictionary constants,
      DateTimeFormatInfo dateTimeFormatInfo, boolean isPublic) {

    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8());

    Div panel = div();
    doc.getBody().append(panel);

    Tbody tableFields = tbody().append(
        tr().append(td().text(constants.date()),
            td().text(Formatter.renderDate(dateTimeFormatInfo,
                discussMailRow.getDate(COL_CREATED)))));

    if (typeAnnoucement) {
      tableFields.append(
          tr().append(td().text(constants.adTopic()),
              td().text(anouncmentTopic))
      );
    }

    Div discussDescriptionContent = div().text(discussMailRow.getValue(COL_SUMMARY));
    // discussDescriptionContent.setMaxHeight(4, CssUnit.EM);
    // discussDescriptionContent.setOverflow(Overflow.HIDDEN);

    tableFields.append(
        tr().append(td().text(constants.discussOwner()),
            td().text(usr.getUserSign(discussMailRow.getLong(COL_OWNER)))),
        tr().append(td().text(constants.discussSummary()),
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
                AdministrationConstants.COL_USER, userId));

    int updated = qs.updateData(new SqlUpdate(TBL_DISCUSSIONS_USERS).addConstant(
        COL_LAST_ACCESS, mills)
        .setWhere(where));

    if (updated != 0) {
      return ResponseObject.response(updated);
    }

    SqlInsert insert = new SqlInsert(TBL_DISCUSSIONS_USERS)
        .addConstant(COL_DISCUSSION, discussionId)
        .addConstant(AdministrationConstants.COL_USER, userId)
        .addConstant(COL_MEMBER, null)
        .addConstant(COL_LAST_ACCESS, mills);

    return qs.insertDataWithResponse(insert);
  }

  private void setRowAccessProperties(Set<Long> discussIds, BeeRowSet rowSet) {
    if (BeeUtils.isEmpty(discussIds)) {
      return;
    }

    Long userId = usr.getCurrentUserId();

    SqlSelect discussUsers = new SqlSelect()
        .addFields(TBL_DISCUSSIONS_USERS, COL_DISCUSSION, COL_LAST_ACCESS, COL_STAR)
        .addFrom(TBL_DISCUSSIONS_USERS);

    IsCondition usersWhere =
        SqlUtils.equals(TBL_DISCUSSIONS_USERS, AdministrationConstants.COL_USER, userId);

    discussUsers.setWhere(usersWhere);
    discussUsers.setWhere(SqlUtils.and(usersWhere, SqlUtils.inList(TBL_DISCUSSIONS_USERS,
        COL_DISCUSSION, discussIds)));

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

      row.setProperty(PROP_USER, userId, BeeConst.STRING_PLUS);

      row.setProperty(PROP_LAST_ACCESS, userId, discussUserRow.getValue(accessIndex));
      row.setProperty(PROP_STAR, userId, discussUserRow.getValue(starIndex));
    }
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
            .addFields(TBL_DISCUSSIONS, COL_SUBJECT, COL_SUMMARY, COL_OWNER,
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

      Dictionary constants = usr.getDictionary(member);
      DateTimeFormatInfo dateTimeFormatInfo = usr.getDateTimeFormatInfo(member);

      if (constants == null) {
        logger.warning(label, discussionId, "member", member, "localization not available");
        continue;
      }

      Document discussMailDocument =
          renderDiscussionDocument(discussionId, typeAnnoucement, annoucementTopic, discussMailRow,
              constants, dateTimeFormatInfo, sendAll);

      String htmlDiscussMailContent = discussMailDocument.buildLines();
      String discussSubject = BeeUtils.joinWords(
          (typeAnnoucement ? constants.announcement()
              : constants.discussion()) + BeeConst.STRING_COLON, discussMailRow
              .getValue(COL_SUBJECT));

      Div subjectElement = div().text(discussSubject);
      String content;
      if (BeeUtils.unbox(discussMailRow.getBoolean(COL_IMPORTANT))) {
        content = BeeUtils.join("",
            mail.styleMailHeader(subjectElement.build(), Colors.RED), htmlDiscussMailContent);
      } else {
        content = BeeUtils.join("",
            mail.styleMailHeader(subjectElement.build(), null), htmlDiscussMailContent);
      }

      logger.info(label, discussionId, "mail to", member, memberEmail);

      String subject = typeAnnoucement ? constants.discussMailNewAnnouncementSubject()
          : constants.discussMailNewDiscussionSubject();

      ResponseObject mailResponse = mail.sendMail(senderAccountId, memberEmail, subject, content);

      if (mailResponse.hasErrors()) {
        response.addWarning("Send mail failed");
      }

    }

    return response;
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
