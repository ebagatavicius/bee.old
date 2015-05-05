package com.butent.bee.server.modules.classifiers;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.TableModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.I18nUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.BorderStyle;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.elements.Table;
import com.butent.bee.shared.html.builder.elements.Td;
import com.butent.bee.shared.html.builder.elements.Th;
import com.butent.bee.shared.html.builder.elements.Tr;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
import com.butent.bee.shared.modules.calendar.CalendarConstants.AppointmentStatus;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.service.ServiceConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.imageio.ImageIO;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Stateless
@LocalBean
public class ClassifiersModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(ClassifiersModuleBean.class);

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

  @Resource
  TimerService timerService;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> search = new ArrayList<>();

    if (usr.isModuleVisible(ModuleAndSub.of(getModule(), SubModule.CONTACTS))) {
      List<SearchResult> companiesSr = qs.getSearchResults(VIEW_COMPANIES,
          Filter.anyContains(Sets.newHashSet(COL_COMPANY_NAME, COL_COMPANY_CODE, COL_PHONE,
              COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(companiesSr);

      List<SearchResult> personsSr = qs.getSearchResults(VIEW_PERSONS,
          Filter.anyContains(Sets.newHashSet(COL_FIRST_NAME, COL_LAST_NAME, COL_PHONE,
              COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(personsSr);

      List<SearchResult> companiesAndPersons =
          qs.getSearchResults(VIEW_COMPANY_PERSONS, Filter.anyContains(Sets.newHashSet(
              ALS_COMPANY_NAME, ALS_COMPANY_TYPE_NAME, COL_FIRST_NAME, COL_LAST_NAME,
              COL_DEPARTMENT_NAME, ALS_POSITION_NAME, COL_PHONE, COL_MOBILE, COL_FAX, COL_EMAIL,
              COL_ADDRESS, COL_POST_INDEX, COL_WEBSITE, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(companiesAndPersons);
    }

    if (usr.isModuleVisible(ModuleAndSub.of(Module.TRADE))) {
      List<SearchResult> itemsSr =
          qs.getSearchResults(VIEW_ITEMS, Filter.anyContains(Sets.newHashSet(COL_ITEM_NAME,
              COL_ITEM_ARTICLE, COL_ITEM_BARCODE), query));
      search.addAll(itemsSr);
    }
    return search;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.same(svc, SVC_COMPANY_INFO)) {
      response = getCompanyInfo(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMPANY)),
          reqInfo.getParameter(VAR_LOCALE));

    } else if (BeeUtils.same(svc, SVC_CREATE_COMPANY)) {
      response = createCompany(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_COMPANY_TYPE_REPORT)) {
      response = getCompanyTypeReport(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GENERATE_QR_CODE)) {
      try {
        response = generateQrCode(reqInfo);
      } catch (WriterException e) {
        response = ResponseObject.error(e);
        e.printStackTrace();
      } catch (IOException e) {
        response = ResponseObject.error(e);
        e.printStackTrace();
      }
    } else {
      String msg = BeeUtils.joinWords("Commons service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    return null;
  }

  @Override
  public Module getModule() {
    return Module.CLASSIFIERS;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    initActionReminderTimer();

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void setPersonCompanies(ViewQueryEvent event) {
        if (event.isAfter() && event.isTarget(VIEW_PERSONS)
            && !DataUtils.isEmpty(event.getRowset())) {

          SqlSelect query = new SqlSelect()
              .addFields(TBL_COMPANY_PERSONS, COL_PERSON, COL_COMPANY)
              .addFields(TBL_COMPANIES, COL_COMPANY_NAME)
              .addFrom(TBL_COMPANY_PERSONS)
              .addFromInner(TBL_COMPANIES,
                  sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
              .addOrder(TBL_COMPANY_PERSONS, COL_PERSON)
              .addOrder(TBL_COMPANIES, COL_COMPANY_NAME);

          if (event.getRowset().getNumberOfRows() < 100) {
            query.setWhere(SqlUtils.inList(TBL_COMPANY_PERSONS, COL_PERSON,
                event.getRowset().getRowIds()));
          }

          SimpleRowSet data = qs.getData(query);
          if (!DataUtils.isEmpty(data)) {
            Multimap<Long, Long> companyIds = ArrayListMultimap.create();
            Multimap<Long, String> companyNames = ArrayListMultimap.create();

            for (SimpleRow row : data) {
              Long person = row.getLong(COL_PERSON);

              companyIds.put(person, row.getLong(COL_COMPANY));
              companyNames.put(person, row.getValue(COL_COMPANY_NAME));
            }

            for (BeeRow row : event.getRowset()) {
              if (companyIds.containsKey(row.getId())) {
                row.setProperty(PROP_COMPANY_IDS,
                    DataUtils.buildIdList(companyIds.get(row.getId())));
                row.setProperty(PROP_COMPANY_NAMES,
                    BeeUtils.joinItems(companyNames.get(row.getId())));
              }
            }
          }
        }
      }

      @Subscribe
      public void storeEmail(TableModifyEvent event) {
        if (BeeUtils.same(event.getTargetName(), TBL_EMAILS) && event.isBefore()
            && (event.getQuery() instanceof SqlInsert || event.getQuery() instanceof SqlUpdate)) {

          IsQuery query = event.getQuery();
          Object expr = null;

          if (query instanceof SqlInsert) {
            if (!((SqlInsert) query).isMultipleInsert()
                && ((SqlInsert) query).hasField(COL_EMAIL_ADDRESS)) {

              expr = ((SqlInsert) query).getValue(COL_EMAIL_ADDRESS).getValue();
            }
          } else if (((SqlUpdate) query).hasField(COL_EMAIL_ADDRESS)) {
            expr = ((SqlUpdate) query).getValue(COL_EMAIL_ADDRESS);

            if (expr instanceof IsExpression) {
              expr = ((IsExpression) expr).getValue();
            }
          }
          if (expr instanceof Value) {
            String email = BeeUtils.normalize(((Value) expr).getString());

            try {
              new InternetAddress(email, true).validate();

              if (query instanceof SqlInsert) {
                ((SqlInsert) query).updExpression(COL_EMAIL_ADDRESS, SqlUtils.constant(email));
              } else {
                ((SqlUpdate) query).updExpression(COL_EMAIL_ADDRESS, SqlUtils.constant(email));
              }
            } catch (AddressException ex) {
              event.addErrorMessage(BeeUtils.joinWords("Wrong address ", email, ": ",
                  ex.getMessage()));
            }
          }
        }
      }
    });

    news.registerUsageQueryProvider(Feed.COMPANIES_MY, new UsageQueryProvider() {
      @Override
      public SqlSelect getQueryForAccess(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        String usageTable = NewsConstants.getUsageTable(TBL_COMPANIES);
        String urc = news.getUsageRelationColumn(TBL_COMPANIES);

        List<Pair<String, IsCondition>> joins = NewsHelper.buildJoin(TBL_COMPANY_USERS,
            SqlUtils.join(TBL_COMPANY_USERS, COL_COMPANY_USER_COMPANY, usageTable, urc));

        return NewsHelper.getAccessQuery(usageTable, urc, joins, getUserCompanyCondition(userId),
            userId);
      }

      @Override
      public SqlSelect getQueryForUpdates(Feed feed, String relationColumn, long userId,
          DateTime startDate) {

        String usageTable = feed.getUsageTable();

        List<Pair<String, IsCondition>> joins = NewsHelper.buildJoin(usageTable,
            sys.joinTables(TBL_COMPANY_USERS, usageTable, relationColumn));

        return NewsHelper.getUpdatesQuery(TBL_COMPANY_USERS, COL_COMPANY_USER_COMPANY, usageTable,
            joins, getUserCompanyCondition(userId), userId, startDate);
      }

      private List<IsCondition> getUserCompanyCondition(long userId) {
        return Lists.newArrayList(SqlUtils.equals(TBL_COMPANY_USERS, COL_COMPANY_USER_USER,
            userId));
      }
    });

    BeeView.registerConditionProvider(FILTER_COMPANY_CREATION_AND_TYPE,
        new BeeView.ConditionProvider() {
          @Override
          public IsCondition getCondition(BeeView view, List<String> args) {
            Long start = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, 0));
            Long end = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, 1));

            String type = BeeUtils.getQuietly(args, 2);

            String idName = view.getSourceIdName();

            HasConditions conditions = SqlUtils.and();

            if (start != null || end != null) {
              String auditSource = sys.getAuditSource(view.getSourceName());

              IsExpression created = SqlUtils.aggregate(SqlFunction.MIN,
                  SqlUtils.field(auditSource, AUDIT_FLD_TIME));

              SqlSelect auditQuery = new SqlSelect()
                  .addFields(auditSource, AUDIT_FLD_ID)
                  .addExpr(created, AUDIT_FLD_TIME)
                  .addFrom(auditSource)
                  .addGroup(auditSource, AUDIT_FLD_ID);

              HasConditions having = SqlUtils.and();
              if (start != null) {
                having.add(SqlUtils.moreEqual(created, start));
              }
              if (end != null) {
                having.add(SqlUtils.less(created, end));
              }
              auditQuery.setHaving(having);

              String auditAlias = "audit" + SqlUtils.uniqueName();

              SqlSelect subQuery = new SqlSelect()
                  .addFields(auditAlias, AUDIT_FLD_ID)
                  .addFrom(auditQuery, auditAlias);

              conditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));
            }

            if (!BeeUtils.isEmpty(type)) {
              if (type.equals(BeeConst.STRING_ZERO)) {
                conditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName,
                    TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY)));

              } else {
                Set<Long> types = DataUtils.parseIdSet(type);
                if (!types.isEmpty()) {
                  conditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                      TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY,
                      SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE, types)));
                }
              }
            }

            return conditions;
          }
        });

    BeeView.registerConditionProvider(FILTER_COMPANY_USAGE, new BeeView.ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        if (view == null || BeeUtils.size(args) < 2) {
          return null;
        }

        HasConditions conditions = SqlUtils.and();

        String idName = view.getSourceIdName();

        String relation = null;
        Operator operator = null;
        Integer count = null;

        Long start = null;
        Long end = null;

        for (int i = 0; i < args.size() - 1; i += 2) {
          String key = args.get(i);
          String value = args.get(i + 1);

          if (BeeUtils.isEmpty(key) || BeeUtils.isEmpty(value)) {
            continue;
          }

          switch (key) {
            case Service.VAR_VIEW_NAME:
              relation = value;
              break;
            case Service.VAR_OPERATOR:
              operator = Operator.getOperator(value);
              break;
            case Service.VAR_VALUE:
              count = BeeUtils.toIntOrNull(value);
              break;

            case Service.VAR_FROM:
              start = BeeUtils.toLongOrNull(value);
              break;
            case Service.VAR_TO:
              end = BeeUtils.toLongOrNull(value);
              break;

            case COL_COMPANY_TYPE:
            case COL_COMPANY_GROUP:
            case COL_COMPANY_PRIORITY:
            case COL_COMPANY_RELATION_TYPE_STATE:
            case COL_COMPANY_FINANCIAL_STATE:
            case COL_COMPANY_SIZE:
            case COL_COMPANY_INFORMATION_SOURCE:
              conditions.add(SqlUtils.inList(TBL_COMPANIES, key, DataUtils.parseIdSet(value)));
              break;

            case COL_RELATION_TYPE:
              conditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                  TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY,
                  SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE, key,
                      DataUtils.parseIdSet(value))));
              break;

            case COL_ACTIVITY:
              conditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                  TBL_COMPANY_ACTIVITY_STORE, COL_COMPANY,
                  SqlUtils.inList(TBL_COMPANY_ACTIVITY_STORE, key, DataUtils.parseIdSet(value))));
              break;

            case COL_COUNTRY:
            case COL_CITY:
              conditions.add(SqlUtils.inList(view.getColumnSource(key), key,
                  DataUtils.parseIdSet(value)));
              break;
          }
        }

        if (sys.isView(relation) && operator != null) {
          BeeView relView = sys.getView(relation);

          String source = relView.getSourceName();
          String sourceField = null;
          String dtField = null;

          String relationsField = null;

          switch (relation) {
            case TradeConstants.VIEW_SALES:
              sourceField = TradeConstants.COL_TRADE_CUSTOMER;
              dtField = TradeConstants.COL_TRADE_DATE;
              break;

            case TradeConstants.VIEW_PURCHASES:
              sourceField = TradeConstants.COL_TRADE_SUPPLIER;
              dtField = TradeConstants.COL_TRADE_DATE;
              break;

            case DocumentConstants.VIEW_DOCUMENTS:
              dtField = DocumentConstants.COL_DOCUMENT_DATE;
              relationsField = DocumentConstants.COL_DOCUMENT;
              break;

            case TaskConstants.VIEW_TASKS:
              sourceField = COL_COMPANY;
              dtField = TaskConstants.COL_FINISH_TIME;
              relationsField = TaskConstants.COL_TASK;
              break;

            case TaskConstants.VIEW_RECURRING_TASKS:
              sourceField = COL_COMPANY;
              dtField = TaskConstants.COL_RT_SCHEDULE_FROM;
              relationsField = TaskConstants.COL_RECURRING_TASK;
              break;

            case CalendarConstants.VIEW_APPOINTMENTS:
              sourceField = COL_COMPANY;
              dtField = CalendarConstants.COL_START_DATE_TIME;
              break;

            case ServiceConstants.VIEW_SERVICE_OBJECTS:
              sourceField = ServiceConstants.COL_SERVICE_CUSTOMER;
              dtField = relView.getSourceVersionName();
              break;

            case TransportConstants.VIEW_ORDERS:
              sourceField = TransportConstants.COL_CUSTOMER;
              dtField = TransportConstants.COL_ORDER_DATE;
              break;

            case TransportConstants.VIEW_VEHICLES:
              sourceField = TransportConstants.COL_OWNER;
              dtField = relView.getSourceVersionName();
              break;

            case VIEW_COMPANY_CONTACTS:
            case VIEW_COMPANY_DEPARTMENTS:
            case VIEW_COMPANY_PERSONS:
            case VIEW_COMPANY_USERS:
            case VIEW_COMPANY_RELATION_TYPE_STORE:
            case VIEW_COMPANY_ACTIVITY_STORE:
              sourceField = COL_COMPANY;
              dtField = relView.getSourceVersionName();
              break;
          }

          if (BeeUtils.allEmpty(sourceField, relationsField)) {
            logger.severe("filter", FILTER_COMPANY_USAGE, "relation", relation, "not supported");

          } else if (!BeeUtils.isPositive(count) && operator == Operator.LT) {
            conditions.add(SqlUtils.sqlFalse());

          } else if (BeeUtils.isPositive(count) || operator != Operator.GE) {
            if (!BeeUtils.isPositive(count)) {
              count = 0;
            }

            HasConditions dtWhere;
            if (!BeeUtils.isEmpty(dtField) && (start != null || end != null)) {
              dtWhere = SqlUtils.and();
              if (start != null) {
                dtWhere.add(SqlUtils.moreEqual(source, dtField, start));
              }
              if (end != null) {
                dtWhere.add(SqlUtils.less(source, dtField, end));
              }
            } else {
              dtWhere = null;
            }

            HasConditions sourceWhere = BeeUtils.isEmpty(sourceField)
                ? null : SqlUtils.and(SqlUtils.notNull(source, sourceField), dtWhere);
            HasConditions relationsWhere = BeeUtils.isEmpty(relationsField)
                ? null : SqlUtils.and(SqlUtils.notNull(TBL_RELATIONS, COL_COMPANY), dtWhere);

            HasConditions relConditions = SqlUtils.or();

            SqlSelect subQuery;

            if (operator == Operator.LT && count > 0
                || operator == Operator.LE
                || operator == Operator.EQ && count == 0) {

              if (BeeUtils.isEmpty(relationsField)) {
                relConditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName,
                    source, sourceField, sourceWhere)));

              } else if (BeeUtils.isEmpty(sourceField)) {
                subQuery = new SqlSelect().setDistinctMode(true)
                    .addFields(TBL_RELATIONS, COL_COMPANY)
                    .addFrom(TBL_RELATIONS)
                    .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                    .setWhere(relationsWhere);

                relConditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName, subQuery)));

              } else {
                subQuery = new SqlSelect()
                    .addField(source, sourceField, COL_COMPANY)
                    .addFrom(source)
                    .setWhere(sourceWhere);

                subQuery.addUnion(new SqlSelect()
                    .addFields(TBL_RELATIONS, COL_COMPANY)
                    .addFrom(TBL_RELATIONS)
                    .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                    .setWhere(relationsWhere));

                relConditions.add(SqlUtils.not(SqlUtils.in(TBL_COMPANIES, idName, subQuery)));
              }
            }

            if (operator == Operator.GE && count == 1
                || operator == Operator.GT && count == 0) {

              if (BeeUtils.isEmpty(relationsField)) {
                relConditions.add(SqlUtils.in(TBL_COMPANIES, idName,
                    source, sourceField, sourceWhere));

              } else if (BeeUtils.isEmpty(sourceField)) {
                subQuery = new SqlSelect().setDistinctMode(true)
                    .addFields(TBL_RELATIONS, COL_COMPANY)
                    .addFrom(TBL_RELATIONS)
                    .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                    .setWhere(relationsWhere);

                relConditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));

              } else {
                subQuery = new SqlSelect()
                    .addField(source, sourceField, COL_COMPANY)
                    .addFrom(source)
                    .setWhere(sourceWhere);

                subQuery.addUnion(new SqlSelect()
                    .addFields(TBL_RELATIONS, COL_COMPANY)
                    .addFrom(TBL_RELATIONS)
                    .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                    .setWhere(relationsWhere));

                relConditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));
              }
            }

            if (operator == Operator.LT && count > 1
                || operator == Operator.LE && count > 0
                || operator == Operator.EQ && count > 0
                || operator == Operator.GE && count > 1
                || operator == Operator.GT && count > 0) {

              subQuery = new SqlSelect();

              if (BeeUtils.isEmpty(relationsField)) {
                subQuery
                    .addFields(source, sourceField)
                    .addFrom(source)
                    .setWhere(sourceWhere)
                    .addGroup(source, sourceField);

              } else if (BeeUtils.isEmpty(sourceField)) {
                subQuery
                    .addFields(TBL_RELATIONS, COL_COMPANY)
                    .addFrom(TBL_RELATIONS)
                    .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                    .setWhere(relationsWhere)
                    .addGroup(TBL_RELATIONS, COL_COMPANY);

              } else {
                SqlSelect unionQuery = new SqlSelect()
                    .addField(source, sourceField, COL_COMPANY)
                    .addFrom(source)
                    .setWhere(sourceWhere);

                unionQuery.setUnionAllMode(true).addUnion(new SqlSelect()
                    .addFields(TBL_RELATIONS, COL_COMPANY)
                    .addFrom(TBL_RELATIONS)
                    .addFromInner(source, sys.joinTables(source, TBL_RELATIONS, relationsField))
                    .setWhere(relationsWhere));

                String unionAlias = "Un" + SqlUtils.uniqueName();

                subQuery
                    .addFields(unionAlias, COL_COMPANY)
                    .addFrom(unionQuery, unionAlias)
                    .addGroup(unionAlias, COL_COMPANY);
              }

              IsExpression xpr = SqlUtils.aggregate(SqlFunction.COUNT, null);
              subQuery.setHaving(SqlUtils.compare(xpr, operator, SqlUtils.constant(count)));

              relConditions.add(SqlUtils.in(TBL_COMPANIES, idName, subQuery));
            }

            if (!relConditions.isEmpty()) {
              conditions.add(relConditions);
            }
          }
        }

        return conditions;
      }
    });
  }

  public void initActionReminderTimer() {
    Timer actionReminderTimer = null;

    for (Timer timer : timerService.getTimers()) {
      if (Objects.equals(timer.getInfo(), TIMER_REMIND_COMPANY_ACTIONS)) {
        actionReminderTimer = timer;
        break;
      }
    }

    if (actionReminderTimer != null) {
      actionReminderTimer.cancel();
    }

    long startDelay = ((TimeUtils.MINUTES_PER_HOUR - (new DateTime()).getMinute())
        * TimeUtils.MILLIS_PER_MINUTE) + (1 * TimeUtils.MILLIS_PER_SECOND);

    actionReminderTimer = timerService.createIntervalTimer(startDelay,
        DEFAULT_REMIND_ACTIONS_TIMER_TIMEOUT, new TimerConfig(TIMER_REMIND_COMPANY_ACTIONS, false));

    logger.info("Created CALENDAR company actions timer. Timer starts at",
        actionReminderTimer.getNextTimeout());
  }

  private void createActionRemindMail(Long userId, BeeRowSet appointments) {
    Long accountId = mail.getSenderAccountId(TIMER_REMIND_COMPANY_ACTIONS);
    String to = usr.getUserEmail(userId, false);

    if (!DataUtils.isId(accountId) && BeeUtils.isEmpty(to)) {
      return;
    }

    String subject = usr.getLocalizableConstants().calMailPlannedActionSubject();
    String reminderText = usr.getLocalizableConstants().calMailPlannedActionText();

    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8(), title().text(subject));

    Table table = table();
    Tr trHead = tr();

    for (int i = 0; i < appointments.getNumberOfColumns(); i++) {
      String label = BeeConst.STRING_EMPTY;

      if (BeeUtils.same(appointments.getColumnId(i), CalendarConstants.ALS_ATTENDEE_TYPE_NAME)) {
        label = usr.getLocalizableConstants().type();
      } else if (BeeUtils.same(appointments.getColumnId(i), ClassifierConstants.ALS_COMPANY_NAME)) {
        label = usr.getLocalizableConstants().customer();
      } else {
        label = Localized.maybeTranslate(appointments.getColumnLabel(i),
            usr.getLocalizableDictionary());
      }

      Th th = th().text(label);
      th.setBorderWidth("1px");
      th.setBorderStyle(BorderStyle.SOLID);
      th.setBorderColor("black");
      trHead.append(th);
    }

    table.append(trHead);

    Range<Long> maybeTime = Range.closed(
        TimeUtils.startOfYear(TimeUtils.today(), -10).getTime(),
        TimeUtils.startOfYear(TimeUtils.today(), 100).getTime());

    for (IsRow row : appointments) {
      Tr tr = tr();

      for (int i = 0; i < appointments.getNumberOfColumns(); i++) {
        if (row.isNull(i)) {
          tr.append(td());
          continue;
        }

        ValueType type = appointments.getColumnType(i);
        String value = DataUtils.render(appointments.getColumn(i), row, i);

        if (type == ValueType.LONG) {
          Long x = row.getLong(i);
          if (x != null && maybeTime.contains(x)) {
            type = ValueType.DATE_TIME;
            value = new DateTime(x).toCompactString();
          }
        } else if (!BeeUtils.isEmpty(appointments.getColumn(i).getEnumKey())) {
          value = EnumUtils.getLocalizedCaption(appointments.getColumn(i).getEnumKey(),
              row.getInteger(i), usr.getLocalizableConstants(userId));
        }

        Td td = td();
        tr.append(td);
        td.text(value);

        if (ValueType.isNumeric(type) || ValueType.TEXT == type
            && CharMatcher.DIGIT.matchesAnyOf(value) && BeeUtils.isDouble(value)) {
          td.setTextAlign(TextAlign.RIGHT);
        }
      }
      table.append(tr);
    }

    table.setBorderWidth("1px;");
    table.setBorderStyle(BorderStyle.SOLID);
    table.setBorderSpacing("0px;");

    doc.getBody().append(p().text(reminderText));
    doc.getBody().append(table);

    mail.sendMail(accountId, to, subject, doc.buildLines());
    setRemindedAppointments(appointments);
    logger.info(TIMER_REMIND_COMPANY_ACTIONS, "mail send, user id", userId,
        ", company action count", appointments.getRows().size());
  }

  private ResponseObject createCompany(RequestInfo reqInfo) {
    String companyName = reqInfo.getParameter(COL_COMPANY_NAME);
    if (BeeUtils.isEmpty(companyName)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_COMPANY, COL_COMPANY_NAME);
    }

    Long company = qs.getId(TBL_COMPANIES, COL_COMPANY_NAME, companyName);
    if (company != null) {
      return ResponseObject.response(company);
    }

    String companyCode = reqInfo.getParameter(COL_COMPANY_CODE);
    if (!BeeUtils.isEmpty(companyCode)) {
      company = qs.getId(TBL_COMPANIES, COL_COMPANY_CODE, companyCode);
      if (company != null) {
        logger.warning(SVC_CREATE_COMPANY, COL_COMPANY_NAME, companyName, "not found",
            COL_COMPANY_CODE, companyCode, "found id", company);
        return ResponseObject.response(company);
      }
    }

    String vatCode = reqInfo.getParameter(COL_COMPANY_VAT_CODE);
    String exchangeCode = reqInfo.getParameter(COL_COMPANY_EXCHANGE_CODE);

    String email = reqInfo.getParameter(COL_EMAIL);
    if (!BeeUtils.isEmpty(email) && qs.sqlExists(TBL_EMAILS, COL_EMAIL_ADDRESS, email)) {
      logger.warning(usr.getLocalizableMesssages()
          .valueExists(BeeUtils.joinWords(usr.getLocalizableConstants().email(), email)),
          "ignored");
      email = null;
    }

    String address = reqInfo.getParameter(COL_ADDRESS);
    String cityName = reqInfo.getParameter(COL_CITY);
    String countryName = reqInfo.getParameter(COL_COUNTRY);

    String phone = reqInfo.getParameter(COL_PHONE);
    String mobile = reqInfo.getParameter(COL_MOBILE);
    String fax = reqInfo.getParameter(COL_FAX);

    List<BeeColumn> columns = sys.getView(VIEW_COMPANIES).getRowSetColumns();
    BeeRow row = DataUtils.createEmptyRow(columns.size());

    row.setValue(DataUtils.getColumnIndex(COL_COMPANY_NAME, columns), companyName);

    if (!BeeUtils.isEmpty(companyCode)) {
      row.setValue(DataUtils.getColumnIndex(COL_COMPANY_CODE, columns), companyCode);
    }
    if (!BeeUtils.isEmpty(vatCode)) {
      row.setValue(DataUtils.getColumnIndex(COL_COMPANY_VAT_CODE, columns), vatCode);
    }
    if (!BeeUtils.isEmpty(exchangeCode)) {
      row.setValue(DataUtils.getColumnIndex(COL_COMPANY_EXCHANGE_CODE, columns), exchangeCode);
    }

    ResponseObject response;

    if (!BeeUtils.isEmpty(email)) {
      row.setValue(DataUtils.getColumnIndex(ALS_EMAIL_ID, columns),
          qs.insertData(new SqlInsert(TBL_EMAILS)
              .addConstant(COL_EMAIL_ADDRESS, address)));
    }
    Long country = null;

    if (!BeeUtils.isEmpty(countryName)) {
      country = qs.getId(TBL_COUNTRIES, COL_COUNTRY_NAME, countryName);

      if (!DataUtils.isId(country)) {
        country = qs.insertData(new SqlInsert(TBL_COUNTRIES)
            .addConstant(COL_COUNTRY_NAME, countryName));
      }
      row.setValue(DataUtils.getColumnIndex(COL_COUNTRY, columns), country);
    }

    if (!BeeUtils.isEmpty(cityName)) {
      Long city;

      if (DataUtils.isId(country)) {
        city = qs.getId(TBL_CITIES, COL_CITY_NAME, cityName, COL_COUNTRY, country);

        if (!DataUtils.isId(city)) {
          city = qs.insertData(new SqlInsert(TBL_CITIES)
              .addConstant(COL_CITY_NAME, cityName)
              .addConstant(COL_COUNTRY, country));
        }
      } else {
        city = qs.getId(TBL_CITIES, COL_CITY_NAME, cityName);
      }
      if (DataUtils.isId(city)) {
        row.setValue(DataUtils.getColumnIndex(COL_CITY, columns), city);
      }
    }

    if (!BeeUtils.isEmpty(address)) {
      row.setValue(DataUtils.getColumnIndex(COL_ADDRESS, columns), address);
    }

    if (!BeeUtils.isEmpty(phone)) {
      row.setValue(DataUtils.getColumnIndex(COL_PHONE, columns), phone);
    }
    if (!BeeUtils.isEmpty(mobile)) {
      row.setValue(DataUtils.getColumnIndex(COL_MOBILE, columns), mobile);
    }
    if (!BeeUtils.isEmpty(fax)) {
      row.setValue(DataUtils.getColumnIndex(COL_FAX, columns), fax);
    }

    BeeRowSet rowSet = DataUtils.createRowSetForInsert(VIEW_COMPANIES, columns, row);
    response = deb.commitRow(rowSet);
    if (response.hasErrors()) {
      return response;
    }

    company = ((BeeRow) response.getResponse()).getId();
    return ResponseObject.response(company);
  }

  private ResponseObject getCompanyInfo(Long companyId, String locale) {
    if (!DataUtils.isId(companyId)) {
      return ResponseObject.error("Wrong company ID");
    }
    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_COMPANIES, COL_COMPANY_NAME, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE)
        .addFields(TBL_CONTACTS, COL_ADDRESS, COL_POST_INDEX, COL_PHONE, COL_MOBILE, COL_FAX)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addField(TBL_CITIES, COL_CITY_NAME, COL_CITY)
        .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COUNTRY)
        .addFields(TBL_COMPANY_BANK_ACCOUNTS, COL_BANK_ACCOUNT)
        .addField(TBL_BANKS, COL_BANK_NAME, COL_BANK)
        .addFields(TBL_BANKS, COL_BANK_CODE, COL_SWIFT_CODE)
        .addFrom(TBL_COMPANIES)
        .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CONTACTS, COL_CITY))
        .addFromLeft(TBL_COUNTRIES, sys.joinTables(TBL_COUNTRIES, TBL_CONTACTS, COL_COUNTRY))
        .addFromLeft(TBL_COMPANY_BANK_ACCOUNTS,
            sys.joinTables(TBL_COMPANY_BANK_ACCOUNTS, TBL_COMPANIES, COL_DEFAULT_BANK_ACCOUNT))
        .addFromLeft(TBL_BANKS, sys.joinTables(TBL_BANKS, TBL_COMPANY_BANK_ACCOUNTS, COL_BANK))
        .setWhere(sys.idEquals(TBL_COMPANIES, companyId)));

    Locale loc = I18nUtils.toLocale(locale);
    LocalizableConstants constants = (loc == null)
        ? Localized.getConstants() : Localizations.getConstants(loc);

    Map<String, String> translations = new HashMap<>();
    translations.put(COL_COMPANY_NAME, constants.company());
    translations.put(COL_COMPANY_CODE, constants.companyCode());
    translations.put(COL_COMPANY_VAT_CODE, constants.companyVATCode());
    translations.put(COL_ADDRESS, constants.address());
    translations.put(COL_POST_INDEX, constants.postIndex());
    translations.put(COL_PHONE, constants.phone());
    translations.put(COL_MOBILE, constants.mobile());
    translations.put(COL_FAX, constants.fax());
    translations.put(COL_EMAIL_ADDRESS, constants.email());
    translations.put(COL_CITY, constants.city());
    translations.put(COL_COUNTRY, constants.country());
    translations.put(COL_BANK, constants.bank());
    translations.put(COL_BANK_CODE, constants.printBankCode());
    translations.put(COL_SWIFT_CODE, constants.printBankSwift());
    translations.put(COL_BANK_ACCOUNT, constants.printBankAccount());

    Map<String, Pair<String, String>> info = new HashMap<>();

    for (String col : translations.keySet()) {
      info.put(col, Pair.of(translations.get(col), row.getValue(col)));
    }
    return ResponseObject.response(info);
  }

  private ResponseObject getCompanyTypeReport(RequestInfo reqInfo) {
    Long startDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_FROM));
    Long endDate = BeeUtils.toLongOrNull(reqInfo.getParameter(Service.VAR_TO));

    Set<Long> filterTypes = DataUtils.parseIdSet(reqInfo.getParameter(COL_RELATION_TYPE));

    BeeTable table = sys.getTable(TBL_COMPANIES);
    if (!table.isAuditable()) {
      return ResponseObject.warning(TBL_COMPANIES, "is not auditable");
    }

    String auditSource = sys.getAuditSource(table.getName());

    String idName = table.getIdName();

    IsExpression minTime = SqlUtils.aggregate(SqlFunction.MIN,
        SqlUtils.field(auditSource, AUDIT_FLD_TIME));

    SqlSelect companyQuery = new SqlSelect()
        .addFields(TBL_COMPANIES, idName)
        .addExpr(minTime, AUDIT_FLD_TIME)
        .addEmptyNumeric(BeeConst.YEAR, 4, 0)
        .addEmptyNumeric(BeeConst.MONTH, 2, 0)
        .addFrom(TBL_COMPANIES)
        .addFromInner(auditSource,
            SqlUtils.join(auditSource, AUDIT_FLD_ID, TBL_COMPANIES, idName));

    if (!filterTypes.isEmpty()) {
      companyQuery.addFromInner(TBL_COMPANY_RELATION_TYPE_STORE,
          sys.joinTables(TBL_COMPANIES, TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY));
      companyQuery.setWhere(SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE,
          COL_RELATION_TYPE, filterTypes));
    }

    companyQuery.addGroup(TBL_COMPANIES, idName);

    if (startDate != null || endDate != null) {
      HasConditions having = SqlUtils.and();

      if (startDate != null) {
        having.add(SqlUtils.moreEqual(minTime, startDate));
      }
      if (endDate != null) {
        having.add(SqlUtils.less(minTime, endDate));
      }

      companyQuery.setHaving(having);
    }

    String tmp = qs.sqlCreateTemp(companyQuery);

    long count = qs.setYearMonth(tmp, AUDIT_FLD_TIME, BeeConst.YEAR, BeeConst.MONTH);
    if (count <= 0) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    String countName = "Cnt" + SqlUtils.uniqueName();

    SqlSelect query = new SqlSelect()
        .addFields(tmp, BeeConst.YEAR, BeeConst.MONTH)
        .addFields(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE)
        .addFields(TBL_COMPANY_RELATION_TYPES, COL_RELATION_TYPE_NAME)
        .addCount(countName)
        .addFrom(tmp)
        .addFromLeft(TBL_COMPANY_RELATION_TYPE_STORE,
            SqlUtils.join(TBL_COMPANY_RELATION_TYPE_STORE, COL_COMPANY, tmp, idName))
        .addFromLeft(TBL_COMPANY_RELATION_TYPES,
            sys.joinTables(TBL_COMPANY_RELATION_TYPES, TBL_COMPANY_RELATION_TYPE_STORE,
                COL_RELATION_TYPE))
        .addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH)
        .addGroup(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE)
        .addGroup(TBL_COMPANY_RELATION_TYPES, COL_RELATION_TYPE_NAME);

    if (filterTypes.isEmpty()) {
      query.setWhere(SqlUtils.notNull(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE));
    } else {
      query.setWhere(SqlUtils.inList(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE,
          filterTypes));
    }

    SimpleRowSet result = qs.getData(query);

    if (filterTypes.isEmpty()) {
      query.setWhere(SqlUtils.isNull(TBL_COMPANY_RELATION_TYPE_STORE, COL_RELATION_TYPE));

      SimpleRowSet other = qs.getData(query);
      if (!DataUtils.isEmpty(other)) {
        result.append(other);
      }

      SqlSelect totalQuery = new SqlSelect()
          .addFields(tmp, BeeConst.YEAR, BeeConst.MONTH)
          .addCount(countName)
          .addFrom(tmp)
          .addGroup(tmp, BeeConst.YEAR, BeeConst.MONTH);

      SimpleRowSet totals = qs.getData(totalQuery);

      if (!DataUtils.isEmpty(totals)) {
        for (SimpleRow totalRow : totals) {
          SimpleRow row = result.addEmptyRow();

          row.setValue(BeeConst.YEAR, totalRow.getValue(BeeConst.YEAR));
          row.setValue(BeeConst.MONTH, totalRow.getValue(BeeConst.MONTH));

          row.setValue(COL_RELATION_TYPE, BeeConst.STRING_NUMBER_SIGN);

          row.setValue(countName, totalRow.getValue(countName));
        }
      }
    }

    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(result)) {
      return ResponseObject.emptyResponse();
    } else {
      return ResponseObject.response(result);
    }
  }

  private Map<Long, Integer> getRemindActionsUserSettings() {
    Map<Long, Integer> userSettings = Maps.newHashMap();
    Filter isSetReminder = Filter.notNull(COL_REMIND_ACTIONS);
    Filter timeIsSet = Filter.isPositive(COL_REMIND_ACTION_BEFORE);

    BeeRowSet rows = qs.getViewData(VIEW_USER_SETTINGS, Filter.and(isSetReminder, timeIsSet));

    for (IsRow row : rows) {
      Long userId = row.getLong(rows.getColumnIndex(COL_USER));
      Integer parameter = row.getInteger(rows.getColumnIndex(COL_REMIND_ACTION_BEFORE));
      if (!usr.isBlocked(usr.getUserName(userId))
          && !BeeUtils.isEmpty(usr.getUserEmail(userId, false))) {
        userSettings.put(userId, parameter);
      }
    }

    return userSettings;
  }

  @Timeout
  private void notifyCompanyActions(Timer timer) {
    if (!BeeUtils.same((String) timer.getInfo(), TIMER_REMIND_COMPANY_ACTIONS)) {
      return;
    }

    logger.info("Timer", TIMER_REMIND_COMPANY_ACTIONS, "started");

    if (!DataUtils.isId(mail.getSenderAccountId(TIMER_REMIND_COMPANY_ACTIONS))) {
      return;
    }

    Map<Long, Integer> userSettings = getRemindActionsUserSettings();

    for (Long user : userSettings.keySet()) {
      logger.debug("try to remind user", user);
      sendCompanyActionsReminder(user, userSettings.get(user));
    }

    logger.info("Timer", TIMER_REMIND_COMPANY_ACTIONS, "ended, Next start time",
        timer.getNextTimeout());
  }

  private void sendCompanyActionsReminder(Long user, Integer remindBefore) {
    if (!DataUtils.isId(user) || !BeeUtils.isPositive(remindBefore)) {
      return;
    }

    Order sortBy = Order.ascending(CalendarConstants.COL_END_DATE_TIME);
    JustDate today = new JustDate();
    DateTime now = new DateTime();

    Filter hasCompany = Filter.notNull(COL_COMPANY);
    Filter isCompleted = Filter.or(
        Filter.equals(CalendarConstants.COL_STATUS, AppointmentStatus.COMPLETED),
        Filter.equals(CalendarConstants.COL_STATUS, AppointmentStatus.CONFIRMED));
    Filter isActive = Filter.isNot(isCompleted);

    Filter notResult = Filter.isNull(CalendarConstants.COL_ACTION_RESULT);
    Filter notRemind = Filter.isNull(CalendarConstants.COL_ACTION_REMINDED);
    Filter validEnd = Filter.isMore(CalendarConstants.COL_END_DATE_TIME, Value.getValue(today));
    Filter isOwner = Filter.equals(CalendarConstants.COL_CREATOR, user);

    List<String> cols = Lists.newArrayList(CalendarConstants.COL_CREATED,
        CalendarConstants.ALS_ATTENDEE_TYPE_NAME, CalendarConstants.COL_SUMMARY,
        ClassifierConstants.ALS_COMPANY_NAME, CalendarConstants.ALS_PERSON_FIRST_NAME,
        CalendarConstants.ALS_PERSON_LAST_NAME, CalendarConstants.COL_STATUS,
        CalendarConstants.COL_END_DATE_TIME);

    BeeRowSet appointments = qs.getViewData(CalendarConstants.VIEW_APPOINTMENTS,
        Filter.and(hasCompany, Filter.and(isActive, isOwner),
            Filter.and(notResult, notRemind, validEnd)), sortBy, cols);

    List<Long> notRemindIds = Lists.newArrayList();
    long nowInHours = now.getTime() / TimeUtils.MILLIS_PER_HOUR;
    logger.debug("current appointment count", appointments.getNumberOfRows());
    for (IsRow row : appointments) {
      DateTime eventEnd =
          row.getDateTime(appointments.getColumnIndex(CalendarConstants.COL_END_DATE_TIME));

      if (eventEnd == null) {
        notRemindIds.add(row.getId());
        continue;
      }

      long eventEndInHours = eventEnd.getTime() / TimeUtils.MILLIS_PER_HOUR;

      logger.debug("Time values", "event end in hours", row.getId(), eventEndInHours,
          "remind before", remindBefore, "diff", eventEndInHours - (nowInHours + 1));
      if ((eventEndInHours - nowInHours) > remindBefore) {
        notRemindIds.add(row.getId());
      }
    }

    for (long id : notRemindIds) {
      appointments.removeRowById(id);
    }

    if (appointments.getNumberOfRows() > 0) {
      createActionRemindMail(user, appointments);
    } else {
      logger.info("no actions remind for user", user);
    }
  }

  public void setRemindedAppointments(BeeRowSet appointments) {
    SqlUpdate update = new SqlUpdate(CalendarConstants.TBL_APPOINTMENTS);
    update.addConstant(CalendarConstants.COL_ACTION_REMINDED, Boolean.TRUE);
    update.setWhere(SqlUtils.inList(CalendarConstants.TBL_APPOINTMENTS,
        sys.getIdName(CalendarConstants.TBL_APPOINTMENTS), appointments.getRowIds()));
    qs.updateData(update);
  }

  public ResponseObject generateQrCode(RequestInfo reqInfo) throws WriterException, IOException {
    final String qrBase64;

    String qrCodeText = "";
    Map<String, String> values = new HashMap<>();
    String[] qrValues = null;
    String[] keys = null;
    String mobile = reqInfo.getParameter(COL_MOBILE);
    String email = reqInfo.getParameter(COL_EMAIL);
    String address = reqInfo.getParameter(COL_ADDRESS);
    String type = reqInfo.getParameter(QR_TYPE);

    if (type.equals(QR_COMPANY)) {
      qrValues = new String[] {"MECARD:N:", "TEL:", "EMAIL:", "ADR:"};
      keys = new String[] {"name", "mobile", "email", "address"};
      String companyName = reqInfo.getParameter(COL_COMPANY_NAME);
      values.put("name", companyName);
      values.put("mobile", mobile);
      values.put("email", email);
      values.put("address", address);
    } else if (type.equals(QR_PERSON)) {
      qrValues = new String[] {"MECARD:N:", "TEL:", "EMAIL:", "ADR:"};
      keys = new String[] {"userName", "mobile", "email", "address"};
      String userName = reqInfo.getParameter(COL_FIRST_NAME);
      String userLastName = reqInfo.getParameter(COL_LAST_NAME);
      if (!BeeUtils.isEmpty(userLastName)) {
        values.put("userName", userLastName + "," + userName);
      }
      values.put("userName", userName);
      values.put("mobile", mobile);
      values.put("email", email);
      values.put("address", address);
    }

    if (!values.isEmpty()) {
      for (int i = 0; i < values.size(); i++) {
        if (values.get(keys[i]) != null) {
          qrCodeText += qrValues[i] + values.get(keys[i]) + ";";
        }
      }
    }

    qrBase64 = qrCodeGenerator(qrCodeText);
    return ResponseObject.response(qrBase64);
  }

  public String qrCodeGenerator(String qrCodeText) throws WriterException, IOException {
    final String qrBase64;
    int size = 500;
    String fileType = "png";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    hintMap.put(EncodeHintType.CHARACTER_SET, "utf-8");

    ByteMatrix byteMatrix =
        qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
    int matrixWidth = byteMatrix.getWidth();
    BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
    image.createGraphics();

    Graphics2D graphics = (Graphics2D) image.getGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, matrixWidth, matrixWidth);
    graphics.setColor(Color.BLACK);

    for (int i = 0; i < matrixWidth; i++) {
      for (int j = 0; j < matrixWidth; j++) {
        if (byteMatrix.get(i, j) != -1) {
          graphics.fillRect(i, j, 1, 1);
        }
      }
    }

    ImageIO.write(image, fileType, baos);
    baos.flush();
    byte[] imageInByte = baos.toByteArray();
    baos.close();
    qrBase64 = Codec.toBase64(imageInByte);
    return qrBase64;
  }

}
