package com.butent.bee.server.modules.documents;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.html.builder.Factory.td;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.COL_CATEGORY_NAME;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.communication.ChatBean;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.classifiers.TimerBuilder;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsFrom;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

@Singleton
@Lock(LockType.READ)
@LocalBean
public class DocumentsModuleBean extends TimerBuilder implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(DocumentsModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  QueryServiceBean qs;
  @EJB
  DataEditorBean deb;
  @EJB
  NewsBean news;
  @EJB
  MailModuleBean mail;
  @EJB
  ChatBean chat;

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();

    result.addAll(qs.getSearchResults(VIEW_DOCUMENTS,
        Filter.anyContains(Sets.newHashSet(COL_DOCUMENT_NUMBER, COL_REGISTRATION_NUMBER,
            COL_DOCUMENT_NAME, ALS_CATEGORY_NAME, ALS_TYPE_NAME,
            ALS_PLACE_NAME, ALS_STATUS_NAME, ALS_DOCUMENT_COMPANY_NAME), query)));

    result.addAll(qs.getSearchResults(VIEW_DOCUMENT_FILES,
        Filter.anyContains(Sets.newHashSet(AdministrationConstants.ALS_FILE_NAME,
            COL_FILE_CAPTION, COL_FILE_DESCRIPTION, COL_FILE_COMMENT), query)));

    return result;

  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_COPY_DOCUMENT_DATA)) {
      response = copyDocumentData(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DOCUMENT_DATA)));

    } else if (BeeUtils.same(svc, SVC_SET_CATEGORY_STATE)) {
      response = setCategoryState(BeeUtils.toLongOrNull(reqInfo.getParameter("id")),
          BeeUtils.toLongOrNull(reqInfo.getParameter(AdministrationConstants.COL_ROLE)),
          EnumUtils.getEnumByIndex(RightsState.class,
              reqInfo.getParameter(AdministrationConstants.COL_STATE)),
          Codec.unpack(reqInfo.getParameter("on")));

    } else {
      String msg = BeeUtils.joinWords(getModule().getName(), "service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    return Arrays.asList(BeeParameter.createBoolean(module, PRM_PRINT_AS_PDF, true, null),
        BeeParameter.createText(module, PRM_PRINT_SIZE, true, "A4 portrait"),
        BeeParameter.createRelation(module, PRM_PRINT_HEADER, true, TBL_EDITOR_TEMPLATES,
            COL_EDITOR_TEMPLATE_NAME),
        BeeParameter.createRelation(module, PRM_PRINT_FOOTER, true, TBL_EDITOR_TEMPLATES,
            COL_EDITOR_TEMPLATE_NAME),
        BeeParameter.createText(module, PRM_PRINT_MARGINS, true, "2em 1em"));
  }

  @Override
  public Module getModule() {
    return Module.DOCUMENTS;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public TimerService getTimerService() {
    return timerService;
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void applyDocumentRights(ViewQueryEvent event) {
        if (event.isTarget(TBL_DOCUMENTS, VIEW_RELATED_DOCUMENTS) && !usr.isAdministrator()) {
          if (event.isBefore()) {
            SqlSelect query = event.getQuery();
            String tableAlias = null;

            for (IsFrom from : query.getFrom()) {
              if (from.getSource() instanceof String
                  && BeeUtils.same((String) from.getSource(), TBL_DOCUMENT_TREE)) {
                tableAlias = BeeUtils.notEmpty(from.getAlias(), TBL_DOCUMENT_TREE);
                break;
              }
            }
            if (!BeeUtils.isEmpty(tableAlias)) {
              sys.filterVisibleState(query, TBL_DOCUMENT_TREE, tableAlias);
            }
          } else {
            BeeRowSet rs = event.getRowset();
            int categoryIdx = rs.getColumnIndex(COL_DOCUMENT_CATEGORY);
            List<Long> categories = new ArrayList<>();

            if (BeeUtils.isNonNegative(categoryIdx)) {
              for (Long category : rs.getDistinctLongs(categoryIdx)) {
                categories.add(category);
              }
            }
            if (!BeeUtils.isEmpty(categories)) {
              BeeRowSet catRs = qs.getViewData(TBL_DOCUMENT_TREE, Filter.idIn(categories), null,
                  Lists.newArrayList(COL_DOCUMENT_CATEGORY + COL_CATEGORY_NAME));

              for (BeeRow row : rs) {
                IsRow catRow = catRs.getRowById(row.getLong(categoryIdx));
                row.setEditable(catRow.isEditable());
                row.setRemovable(catRow.isRemovable());
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void fillDocumentNumber(ViewInsertEvent event) {
        if (event.isBefore(TBL_DOCUMENTS)) {
          List<BeeColumn> cols = event.getColumns();

          if (DataUtils.contains(cols, COL_DOCUMENT_NUMBER)
              || !DataUtils.contains(cols, COL_DOCUMENT_CATEGORY)) {
            return;
          }
          IsRow row = event.getRow();
          HasConditions or = SqlUtils.or(SqlUtils.isNull(TBL_TREE_PREFIXES, COL_DOCUMENT_TYPE));

          SqlSelect query = new SqlSelect()
              .addFields(TBL_TREE_PREFIXES, COL_NUMBER_PREFIX)
              .addFrom(TBL_TREE_PREFIXES)
              .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TREE_PREFIXES, COL_DOCUMENT_CATEGORY,
                  row.getLong(DataUtils.getColumnIndex(COL_DOCUMENT_CATEGORY, cols))), or))
              .addOrder(TBL_TREE_PREFIXES, COL_DOCUMENT_TYPE);

          Long type;
          int typeIdx = DataUtils.getColumnIndex(COL_DOCUMENT_TYPE, cols);

          if (!BeeConst.isUndef(typeIdx)) {
            type = row.getLong(typeIdx);

            if (DataUtils.isId(type)) {
              or.add(SqlUtils.equals(TBL_TREE_PREFIXES, COL_DOCUMENT_TYPE, type));
            }
          }
          SimpleRowSet rs = qs.getData(query);
          String prefix = DataUtils.isEmpty(rs) ? null : rs.getValue(0, COL_NUMBER_PREFIX);

          if (!BeeUtils.isEmpty(prefix)) {
            JustDate date = TimeUtils.today();
            cols.add(new BeeColumn(COL_DOCUMENT_NUMBER));
            row.addValue(Value.getValue(qs.getNextNumber(event.getTargetName(),
                COL_DOCUMENT_NUMBER,
                prefix.replace("{year}", TimeUtils.yearToString(date.getYear()))
                    .replace("{month}", TimeUtils.monthToString(date.getMonth()))
                    .replace("{day}", TimeUtils.dayOfMonthToString(date.getDom())), null)));
          }
        }
      }

      /**
       * Fills sent/received document numbers.
       *
       * Data modify handler checks document sent/received date field changes. If sent/received date
       * was modified then sent/received number field will be changed. New sent/received number
       * value is latest numbers of documents max value. Changes of sent/received number not
       * applying when sent/received date field is empty or sent/received number has own input
       * value.
       *
       * @param event listener of Data modify handler
       */
      @Subscribe
      @AllowConcurrentEvents
      public void fillDocumentSentReceivedNumber(DataEvent.ViewModifyEvent event) {
        if (event.isBefore(TBL_DOCUMENTS)) {
          final IsRow row;
          final List<BeeColumn> columns;

          if (event instanceof ViewInsertEvent) {
            row = ((ViewInsertEvent) event).getRow();
            columns = ((ViewInsertEvent) event).getColumns();

          } else if (event instanceof DataEvent.ViewUpdateEvent) {
            row = ((DataEvent.ViewUpdateEvent) event).getRow();
            columns = ((DataEvent.ViewUpdateEvent) event).getColumns();

          } else {
            return;
          }
          for (Pair<String, String> pair : Arrays.asList(Pair.of(COL_DOCUMENT_SENT_NUMBER,
              COL_DOCUMENT_SENT), Pair.of(COL_DOCUMENT_RECEIVED_NUMBER, COL_DOCUMENT_RECEIVED))) {

            String number = pair.getA();
            String date = pair.getB();

            if (!DataUtils.contains(columns, number) && DataUtils.contains(columns, date)) {
              int idxDate = DataUtils.getColumnIndex(date, columns);

              if (!BeeUtils.isEmpty(row.getString(idxDate))) {
                /** Write values in derived references of instances */
                columns.add(sys.getView(TBL_DOCUMENTS).getBeeColumn(number));
                row.addValue(new TextValue(qs.getNextNumber(event.getTargetName(), number, null,
                    null)));
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_DOCUMENT_FILES)) {
          ExtensionIcons.setIcons(event.getRowset(), AdministrationConstants.ALS_FILE_NAME,
              AdministrationConstants.PROP_ICON);

        } else if (event.isAfter(VIEW_DOCUMENT_TEMPLATES)) {
          Map<Long, IsRow> indexedRows = new HashMap<>();
          BeeRowSet rowSet = event.getRowset();
          int idx = rowSet.getColumnIndex(COL_DOCUMENT_DATA);

          for (BeeRow row : rowSet.getRows()) {
            Long id = row.getLong(idx);

            if (DataUtils.isId(id)) {
              indexedRows.put(id, row);
            }
          }
          if (!indexedRows.isEmpty()) {
            BeeView view = sys.getView(VIEW_DATA_CRITERIA);
            SqlSelect query = view.getQuery(usr.getCurrentUserId());

            query.setWhere(SqlUtils.and(query.getWhere(),
                SqlUtils.isNull(view.getSourceAlias(), COL_CRITERIA_GROUP_NAME),
                SqlUtils.inList(view.getSourceAlias(), COL_DOCUMENT_DATA, indexedRows.keySet())));

            for (SimpleRow row : qs.getData(query)) {
              IsRow r = indexedRows.get(row.getLong(COL_DOCUMENT_DATA));

              if (r != null) {
                r.setProperty(COL_CRITERION_NAME + row.getValue(COL_CRITERION_NAME),
                    row.getValue(COL_CRITERION_VALUE));
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void setRightsProperties(ViewQueryEvent event) {
        if (event.isAfter(TBL_DOCUMENT_TREE)
            && usr.isWidgetVisible(RegulatedWidget.DOCUMENT_TREE)) {

          String tableName = event.getTargetName();
          String idName = sys.getIdName(tableName);

          SqlSelect query = event.getQuery().resetFields().addFields(tableName, idName);

          BeeTable table = sys.getTable(tableName);
          Map<RightsState, String> states = new LinkedHashMap<>();
          boolean stateExists = false;
          Map<String, Long> roles = new TreeMap<>();
          roles.put("", 0L);

          for (Long role : usr.getRoles()) {
            roles.put(usr.getRoleName(role), role);
          }
          for (RightsState state : table.getStates()) {
            states.put(state, table.joinState(query, tableName, state));

            if (!BeeUtils.isEmpty(states.get(state))) {
              for (Long role : roles.values()) {
                IsExpression xpr = SqlUtils.sqlIf(table.checkState(states.get(state), state, role),
                    true, false);

                if (!BeeUtils.isEmpty(query.getGroupBy())) {
                  query.addMax(xpr, state.name() + role);
                } else {
                  query.addExpr(xpr, state.name() + role);
                }
              }
              stateExists = true;
            }
          }
          SimpleRowSet rs = null;

          if (stateExists) {
            rs = qs.getData(query);
          }
          for (BeeRow row : event.getRowset()) {
            for (RightsState state : states.keySet()) {
              for (Long role : roles.values()) {
                String value;

                if (!BeeUtils.isEmpty(states.get(state))) {
                  value = rs.getValueByKey(idName, BeeUtils.toString(row.getId()),
                      state.name() + role);
                } else {
                  value = Codec.pack(state.isChecked());
                }
                row.setProperty(BeeUtils.join("_", role, state.ordinal()), value);
              }
            }
          }
          event.getRowset().setTableProperty(AdministrationConstants.TBL_ROLES,
              Codec.beeSerialize(roles));
          event.getRowset().setTableProperty(AdministrationConstants.TBL_RIGHTS,
              Codec.beeSerialize(states.keySet()));
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void updateTimers(DataEvent.ViewModifyEvent event) {
        if (event.isAfter(TBL_DOCUMENTS)) {
          if (event instanceof DataEvent.ViewUpdateEvent) {
            DataEvent.ViewUpdateEvent ev = (DataEvent.ViewUpdateEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DOCUMENT_EXPIRES)) {
              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, TBL_DOCUMENTS, ev.getRow().getId());
            }
          } else if (event instanceof DataEvent.ViewDeleteEvent) {
            for (long id : ((DataEvent.ViewDeleteEvent) event).getIds()) {
              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, TBL_DOCUMENTS, id);
            }
          } else if (event instanceof DataEvent.ViewInsertEvent) {
            DataEvent.ViewInsertEvent ev = (DataEvent.ViewInsertEvent) event;
            if (DataUtils.contains(ev.getColumns(), COL_DOCUMENT_EXPIRES)) {
              createOrUpdateTimers(TIMER_REMIND_DOCUMENT_END, TBL_DOCUMENTS,
                  ((DataEvent.ViewInsertEvent) event).getRow().getId());
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.DOCUMENTS, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return visibility(NewsHelper.getAccessQuery(feed.getUsageTable(), relationColumn, null,
            null, userId));
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {
        return visibility(NewsHelper.getUpdatesQuery(feed.getUsageTable(), relationColumn, null,
            null, userId, startDate));
      }

      private SqlSelect visibility(SqlSelect query) {
        if (!Invocation.locateRemoteBean(UserServiceBean.class).isAdministrator()) {
          SystemBean sysBean = Invocation.locateRemoteBean(SystemBean.class);

          query.addFromInner(TBL_DOCUMENTS,
              Invocation.locateRemoteBean(NewsBean.class).joinUsage(TBL_DOCUMENTS))
              .addFromInner(TBL_DOCUMENT_TREE,
                  sysBean.joinTables(TBL_DOCUMENT_TREE, TBL_DOCUMENTS, COL_DOCUMENT_CATEGORY));

          sysBean.filterVisibleState(query, TBL_DOCUMENT_TREE);
        }
        return query;
      }
    });

    buildTimers(TIMER_REMIND_DOCUMENT_END);

  }

  @Override
  public void onTimeout(String timerInfo) {
    if (BeeUtils.isPrefix(timerInfo, TIMER_REMIND_DOCUMENT_END)) {
      logger.info("expired document reminder timeout", timerInfo);
      Long documentId =
          BeeUtils.toLong(BeeUtils.removePrefix(timerInfo, TIMER_REMIND_DOCUMENT_END));

      if (DataUtils.isId(documentId)
          && DataUtils.isId(mail.getSenderAccountId(TIMER_REMIND_DOCUMENT_END))) {
        sendExpiredDocumentsReminders(documentId);
      }
    }
  }

  @Override
  protected List<Timer> createTimers(String timerIdentifier, IsCondition wh) {
    List<Timer> timersList = new ArrayList<>();
    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_DOCUMENT_END)) {
      String docIdColumn = sys.getIdName(TBL_DOCUMENTS);

      SimpleRowSet data = qs.getData(new SqlSelect()
      .addFields(TBL_DOCUMENTS, sys.getIdName(TBL_DOCUMENTS), COL_DOCUMENT_EXPIRES)
      .addFrom(TBL_DOCUMENTS)
      .setWhere(SqlUtils.and(wh,
          SqlUtils.notNull(TBL_DOCUMENTS, COL_DOCUMENT_EXPIRES),
          SqlUtils.moreEqual(TBL_DOCUMENTS,
              COL_DOCUMENT_EXPIRES, TimeUtils.today(DOCUMENT_EXPIRATION_MIN_DAYS)))));

      for (SimpleRowSet.SimpleRow row : data) {
        Long timerId = row.getLong(docIdColumn);

        DateTime expDate = TimeUtils.toDateTimeOrNull(row.getValue(COL_DOCUMENT_EXPIRES));
        DateTime timerTime;

        if (TimeUtils.isMeq(
                        TimeUtils.goMonth(expDate, -1), new DateTime(System.currentTimeMillis()))) {
          timerTime = TimeUtils.goMonth(expDate, -1);

        } else {
          TimeUtils.addDay(expDate, -DOCUMENT_EXPIRATION_MIN_DAYS);
          timerTime = expDate;
        }

        if (timerTime == null) {
          continue;
        }

        if (timerTime.getTime() > System.currentTimeMillis()) {
          Timer timer = getTimerService().createSingleActionTimer(timerTime.getJava(),
              new TimerConfig(timerIdentifier + timerId, false));

          logger.info("Created timer:", timerTime, timer.getInfo());

          if (timer != null) {
            timersList.add(timer);
          }
        }
      }
    }

    return timersList;
  }

  @Override
  protected Pair<IsCondition, List<String>> getConditionAndTimerIdForUpdate(String timerIdentifier,
                                                                String viewName, Long relationId) {
    if (BeeUtils.same(timerIdentifier, TIMER_REMIND_DOCUMENT_END)
        && BeeUtils.same(viewName, TBL_DOCUMENTS)) {
      IsCondition wh = SqlUtils.equals(TBL_DOCUMENTS,  sys.getIdName(TBL_DOCUMENTS), relationId);
      List<String> timerIdentifiersIds = new ArrayList<>();
      timerIdentifiersIds.add(timerIdentifier + relationId);
      return Pair.of(wh, timerIdentifiersIds);

    }

    return null;
  }

  private void sendExpiredDocumentsReminders(Long documentId) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_DOCUMENTS, sys.getIdName(TBL_DOCUMENTS), COL_DOCUMENT_USER,
            COL_DOCUMENT_DATE, COL_DOCUMENT_EXPIRES,
            COL_DOCUMENT_NAME, COL_DOCUMENT_NUMBER, COL_REGISTRATION_NUMBER)
        .addField(ClassifierConstants.TBL_COMPANIES,
            ClassifierConstants.COL_COMPANY_NAME, ALS_DOCUMENT_COMPANY_NAME)
        .addField(ClassifierConstants.TBL_COMPANY_TYPES,
            ClassifierConstants.COL_COMPANY_TYPE_NAME, ALS_TYPE_NAME)
        .addFrom(TBL_DOCUMENTS)
        .addFromLeft(ClassifierConstants.TBL_COMPANIES,
            sys.joinTables(ClassifierConstants.TBL_COMPANIES, TBL_DOCUMENTS, COL_DOCUMENT_COMPANY))
        .addFromLeft(ClassifierConstants.TBL_COMPANY_TYPES,
            sys.joinTables(ClassifierConstants.TBL_COMPANY_TYPES, ClassifierConstants.TBL_COMPANIES,
                ClassifierConstants.COL_COMPANY_TYPE))
        .setWhere(sys.idEquals(TBL_DOCUMENTS, documentId));

    Long senderAccountId = mail.getSenderAccountId(TIMER_REMIND_DOCUMENT_END);

    SimpleRowSet data = qs.getData(query);
    if (data.getNumberOfRows() > 0 && DataUtils.isId(senderAccountId)) {
      formAndSendExpiredDocument(data.getRow(0), senderAccountId);
    }
  }

  private void formAndSendExpiredDocument(SimpleRow dRow, Long senderAccountId) {
    Document doc = new Document();

    Long userId = dRow.getLong(COL_DOCUMENT_USER);
    Dictionary dic = usr.getDictionary(userId);
    DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo(userId);

    doc.getHead().append(meta().encodingDeclarationUtf8());

    Div panel = div();
    doc.getBody().append(panel);

    Tbody fields = tbody().append(
        tr().append(
            td().text(dic.documentName()),
            td().text(dRow.getValue(COL_DOCUMENT_NAME))),
        tr().append(
            td().text(dic.company()),
            td().text(BeeUtils.joinWords(dRow.getValue(ALS_DOCUMENT_COMPANY_NAME),
                dRow.getValue(ALS_TYPE_NAME)))),
        tr().append(
            td().text(dic.documentDate()),
            td().text(Formatter.renderDateTime(dtfInfo, dRow.getDateTime(COL_DOCUMENT_DATE)))),
        tr().append(
            td().text(dic.documentExpires()),
            td().text(Formatter.renderDateTime(dtfInfo, dRow.getDateTime(COL_DOCUMENT_EXPIRES)))),
        tr().append(
            td().text(dic.documentNumber()),
            td().text(dRow.getValue(COL_DOCUMENT_NUMBER))),
        tr().append(
            td().text(dic.documentRegistrationNumberShort()),
            td().text(dRow.getValue(COL_REGISTRATION_NUMBER))));

    List<Element> cells = fields.queryTag(Tags.TD);
    for (Element cell : cells) {
      if (cell.index() == 0) {
        cell.setPaddingRight(1, CssUnit.EM);
        cell.setFontWeight(FontWeight.BOLDER);
      }
    }

    panel.append(table().append(fields));

    String content = doc.buildLines();
    String headerCaption = BeeUtils.joinWords(dic.document(),
        dRow.getValue(COL_DOCUMENT_NAME));

    String recipientEmail;
    if (userId != null && usr.isActive(userId)) {
      recipientEmail = usr.getUserEmail(userId, false);
    } else {
      recipientEmail = mail.getSenderAccountEmail(senderAccountId);
    }

    ResponseObject mailResponse = mail.sendStyledMail(senderAccountId, recipientEmail,
        dic.documentExpireReminderMailSubject(), content, headerCaption);

    if (mailResponse.hasErrors()) {
      logger.severe(TIMER_REMIND_DOCUMENT_END, "mail error - canceled");
    }

    Map<String, String> linkData = new HashMap<>();
    linkData.put(VIEW_DOCUMENTS, dRow.getValue(sys.getIdName(TBL_DOCUMENTS)));
    chat.putMessage(mail.styleMailHeader(headerCaption), userId, linkData);
  }

  private ResponseObject copyDocumentData(Long data) {
    Assert.state(DataUtils.isId(data));

    Long dataId = qs.insertData(new SqlInsert(TBL_DOCUMENT_DATA)
        .addConstant(COL_DOCUMENT_CONTENT, qs.getValue(new SqlSelect()
            .addFields(TBL_DOCUMENT_DATA, COL_DOCUMENT_CONTENT)
            .addFrom(TBL_DOCUMENT_DATA)
            .setWhere(sys.idEquals(TBL_DOCUMENT_DATA, data)))));

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addField(TBL_CRITERIA_GROUPS, sys.getIdName(TBL_CRITERIA_GROUPS), COL_CRITERIA_GROUP)
        .addField(TBL_CRITERIA_GROUPS, COL_CRITERIA_ORDINAL,
            COL_CRITERIA_GROUP + COL_CRITERIA_ORDINAL)
        .addFields(TBL_CRITERIA_GROUPS, COL_CRITERIA_GROUP_NAME)
        .addFields(TBL_CRITERIA, COL_CRITERIA_ORDINAL, COL_CRITERION_NAME, COL_CRITERION_VALUE)
        .addFrom(TBL_CRITERIA_GROUPS)
        .addFromLeft(TBL_CRITERIA,
            sys.joinTables(TBL_CRITERIA_GROUPS, TBL_CRITERIA, COL_CRITERIA_GROUP))
        .setWhere(SqlUtils.equals(TBL_CRITERIA_GROUPS, COL_DOCUMENT_DATA, data)));

    Map<Long, Long> groups = new HashMap<>();

    for (SimpleRow row : rs) {
      long groupId = row.getLong(COL_CRITERIA_GROUP);
      String criterion = row.getValue(COL_CRITERION_NAME);

      if (!groups.containsKey(groupId)) {
        groups.put(groupId, qs.insertData(new SqlInsert(TBL_CRITERIA_GROUPS)
            .addConstant(COL_DOCUMENT_DATA, dataId)
            .addConstant(COL_CRITERIA_ORDINAL,
                row.getValue(COL_CRITERIA_GROUP + COL_CRITERIA_ORDINAL))
            .addConstant(COL_CRITERIA_GROUP_NAME, row.getValue(COL_CRITERIA_GROUP_NAME))));
      }
      if (!BeeUtils.isEmpty(criterion)) {
        qs.insertData(new SqlInsert(TBL_CRITERIA)
            .addConstant(COL_CRITERIA_GROUP, groups.get(groupId))
            .addConstant(COL_CRITERIA_ORDINAL, row.getValue(COL_CRITERIA_ORDINAL))
            .addConstant(COL_CRITERION_NAME, criterion)
            .addConstant(COL_CRITERION_VALUE, row.getValue(COL_CRITERION_VALUE)));
      }
    }
    return ResponseObject.response(dataId);
  }

  private ResponseObject setCategoryState(Long id, Long roleId, RightsState state, boolean on) {
    Assert.noNulls(id, roleId, state);

    deb.setState(TBL_DOCUMENT_TREE, state, id, roleId, on);
    return ResponseObject.emptyResponse();
  }
}
