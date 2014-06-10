package com.butent.bee.server.modules.classifiers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.ViewDeleteEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewUpdateEvent;
import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.I18nUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.calendar.CalendarConstants;
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
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
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

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> search = Lists.newArrayList();

    if (usr.isModuleVisible(ModuleAndSub.of(getModule(), SubModule.CONTACTS))) {
      List<SearchResult> companiesSr = qs.getSearchResults(VIEW_COMPANIES,
          Filter.anyContains(Sets.newHashSet(COL_COMPANY_NAME, COL_COMPANY_CODE, COL_PHONE,
              COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(companiesSr);

      List<SearchResult> personsSr = qs.getSearchResults(VIEW_PERSONS,
          Filter.anyContains(Sets.newHashSet(COL_FIRST_NAME, COL_LAST_NAME, COL_PHONE,
              COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));
      search.addAll(personsSr);
    }

    if (usr.isModuleVisible(Module.TRADE.getName())) {
      List<SearchResult> itemsSr = qs.getSearchResults(VIEW_ITEMS,
          Filter.anyContains(Sets.newHashSet(COL_ITEM_NAME, COL_ITEM_ARTICLE, COL_ITEM_BARCODE),
              query));
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
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void storeEmail(ViewModifyEvent event) {
        if (BeeUtils.same(event.getTargetName(), TBL_EMAILS) && event.isBefore()
            && !(event instanceof ViewDeleteEvent)) {

          List<BeeColumn> cols;
          BeeRow row;

          if (event instanceof ViewInsertEvent) {
            cols = ((ViewInsertEvent) event).getColumns();
            row = ((ViewInsertEvent) event).getRow();
          } else {
            cols = ((ViewUpdateEvent) event).getColumns();
            row = ((ViewUpdateEvent) event).getRow();
          }
          int idx = DataUtils.getColumnIndex(COL_EMAIL_ADDRESS, cols);

          if (idx != BeeConst.UNDEF) {
            String email = BeeUtils.normalize(row.getString(idx));

            try {
              new InternetAddress(email, true).validate();
              row.setValue(idx, email);
            } catch (AddressException ex) {
              event.addErrorMessage(BeeUtils.joinWords("Wrong address:", ex.getMessage()));
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
              .addConstant(COL_EMAIL_ADDRESS, address)
              .addNotEmpty(COL_EMAIL_LABEL, companyName)));
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
        .addFrom(TBL_COMPANIES)
        .addFromLeft(TBL_CONTACTS, sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
        .addFromLeft(TBL_EMAILS, sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
        .addFromLeft(TBL_CITIES, sys.joinTables(TBL_CITIES, TBL_CONTACTS, COL_CITY))
        .addFromLeft(TBL_COUNTRIES, sys.joinTables(TBL_COUNTRIES, TBL_CONTACTS, COL_COUNTRY))
        .setWhere(sys.idEquals(TBL_COMPANIES, companyId)));

    Locale loc = I18nUtils.toLocale(locale);
    LocalizableConstants constants = (loc == null)
        ? Localized.getConstants() : Localizations.getConstants(loc);

    Map<String, String> translations = Maps.newHashMap();
    translations.put(COL_COMPANY_NAME, constants.company());
    translations.put(COL_COMPANY_CODE, constants.companyCode());
    translations.put(COL_COMPANY_VAT_CODE, constants.companyVATCode());
    translations.put(COL_ADDRESS, constants.address());
    translations.put(COL_POST_INDEX, constants.postIndex());
    translations.put(COL_PHONE, constants.phone());
    translations.put(COL_MOBILE, constants.mobile());
    translations.put(COL_FAX, constants.fax());
    translations.put(COL_EMAIL_ADDRESS, constants.address());
    translations.put(COL_CITY, constants.city());
    translations.put(COL_COUNTRY, constants.country());

    Map<String, Pair<String, String>> info = Maps.newHashMap();

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
}
