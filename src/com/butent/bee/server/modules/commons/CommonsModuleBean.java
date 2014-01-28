package com.butent.bee.server.modules.commons;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.commons.CommonsConstants.*;

import com.butent.bee.server.data.BeeTable;
import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.DataEvent.TableModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewDeleteEvent;
import com.butent.bee.server.data.DataEvent.ViewInsertEvent;
import com.butent.bee.server.data.DataEvent.ViewModifyEvent;
import com.butent.bee.server.data.DataEvent.ViewUpdateEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.i18n.I18nUtils;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.news.NewsBean;
import com.butent.bee.server.news.NewsHelper;
import com.butent.bee.server.news.UsageQueryProvider;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.news.Feed;
import com.butent.bee.shared.news.NewsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.ibm.icu.text.RuleBasedNumberFormat;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import lt.lb.webservices.exchangerates.ExchangeRatesWS;

@Stateless
@LocalBean
public class CommonsModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(CommonsModuleBean.class);

  private static Collection<? extends BeeParameter> getSqlEngineParameters() {
    List<BeeParameter> params = Lists.newArrayList();

    for (SqlEngine engine : SqlEngine.values()) {
      Map<String, String> value = null;

      switch (engine) {
        case GENERIC:
        case MSSQL:
        case ORACLE:
          break;
        case POSTGRESQL:
          value = ImmutableMap
              .of(".+duplicate key value violates unique constraint.+(\\(.+=.+\\)).+",
                  "Tokia reikšmė jau egzistuoja: $1",
                  ".+violates foreign key constraint.+from table \"(.+)\"\\.",
                  "Įrašas naudojamas lentelėje \"$1\"");
          break;
      }
      params.add(BeeParameter.createMap(COMMONS_MODULE,
          BeeUtils.join(BeeConst.STRING_EMPTY, PRM_SQL_MESSAGES, engine), false, value));
    }
    return params;
  }

  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;
  @EJB
  DataEditorBean deb;
  @EJB
  QueryServiceBean qs;
  @EJB
  ParamHolderBean prm;
  @EJB
  NewsBean news;

  @Resource
  EJBContext ctx;

  @Override
  public Collection<String> dependsOn() {
    return null;
  }

  @Override
  public List<SearchResult> doSearch(String query) {

    List<SearchResult> companiesSr =
        qs.getSearchResults(VIEW_COMPANIES,
            Filter.anyContains(Sets.newHashSet(COL_NAME, COL_COMPANY_CODE, COL_PHONE,
                COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));

    List<SearchResult> personsSr = qs.getSearchResults(VIEW_PERSONS,
        Filter.anyContains(Sets.newHashSet(COL_FIRST_NAME, COL_LAST_NAME, COL_PHONE,
            COL_EMAIL_ADDRESS, COL_ADDRESS, ALS_CITY_NAME, ALS_COUNTRY_NAME), query));

    List<SearchResult> usersSr = qs.getSearchResults(VIEW_USERS,
        Filter.anyContains(Sets.newHashSet(COL_LOGIN, COL_FIRST_NAME, COL_LAST_NAME), query));

    List<SearchResult> itemsSr = qs.getSearchResults(VIEW_ITEMS,
        Filter.anyContains(Sets.newHashSet(COL_NAME, COL_ITEM_ARTICLE, COL_ITEM_BARCODE), query));

    List<SearchResult> commonsSr = Lists.newArrayList();
    commonsSr.addAll(companiesSr);
    commonsSr.addAll(personsSr);
    commonsSr.addAll(usersSr);
    commonsSr.addAll(itemsSr);

    return commonsSr;
  }

  @Override
  public ResponseObject doService(RequestInfo reqInfo) {
    ResponseObject response = null;
    String svc = reqInfo.getParameter(COMMONS_METHOD);

    if (BeeUtils.isPrefix(svc, COMMONS_PARAMETERS_PREFIX)) {
      response = prm.doService(svc, reqInfo);

    } else if (BeeUtils.same(svc, SVC_COMPANY_INFO)) {
      response = getCompanyInfo(BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMPANY)),
          reqInfo.getParameter("locale"));

    } else if (BeeUtils.same(svc, SVC_GET_HISTORY)) {
      response = getHistory(reqInfo.getParameter(VAR_HISTORY_VIEW),
          DataUtils.parseIdSet(reqInfo.getParameter(VAR_HISTORY_IDS)));

    } else if (BeeUtils.same(svc, SVC_UPDATE_EXCHANGE_RATES)) {
      response = updateExchangeRates(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_LIST_OF_CURRENCIES)) {
      response = getListOfCurrencies();
    } else if (BeeUtils.same(svc, SVC_GET_CURRENT_EXCHANGE_RATE)) {
      response = getCurrentExchangeRate(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_EXCHANGE_RATE)) {
      response = getExchangeRate(reqInfo);
    } else if (BeeUtils.same(svc, SVC_GET_EXCHANGE_RATES_BY_CURRENCY)) {
      response = getExchangeRatesByCurrency(reqInfo);

    } else if (BeeUtils.same(svc, SVC_CREATE_COMPANY)) {
      response = createCompany(reqInfo);
    } else if (BeeUtils.same(svc, SVC_CREATE_USER)) {
      response = createUser(reqInfo);
    } else if (BeeUtils.same(svc, SVC_BLOCK_HOST)) {
      response = blockHost(reqInfo);

    } else if (BeeUtils.same(svc, SVC_NUMBER_TO_WORDS)) {
      response = getNumberInWords(BeeUtils.toLongOrNull(reqInfo.getParameter(VAR_AMOUNT)),
          reqInfo.getParameter(VAR_LOCALE));

    } else {
      String msg = BeeUtils.joinWords("Commons service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createText(COMMONS_MODULE, "ProgramTitle", false, "B-NOVO"),
        BeeParameter.createRelation(COMMONS_MODULE, PRM_COMPANY_NAME, false,
            TBL_COMPANIES, COL_COMPANY_NAME),
        BeeParameter.createNumber(COMMONS_MODULE, PRM_VAT_PERCENT, false, 21),
        BeeParameter.createBoolean(COMMONS_MODULE, PRM_AUDIT_OFF, false, null),
        BeeParameter.createText(COMMONS_MODULE, PRM_ERP_ADDRESS, false, null),
        BeeParameter.createText(COMMONS_MODULE, PRM_ERP_LOGIN, false, null),
        BeeParameter.createText(COMMONS_MODULE, PRM_ERP_PASSWORD, false, null),
        BeeParameter.createText(COMMONS_MODULE, "ERPOperation", false, null),
        BeeParameter.createText(COMMONS_MODULE, "ERPWarehouse", false, null),
        BeeParameter.createSet(COMMONS_MODULE, "PRMcollection", false, null),
        BeeParameter.createDate(COMMONS_MODULE, "PRMdate", false, null),
        BeeParameter.createDateTime(COMMONS_MODULE, "PRMdatetime", false, null),
        BeeParameter.createNumber(COMMONS_MODULE, "PRMnumber", false, null),
        BeeParameter.createTime(COMMONS_MODULE, "PRMtime", false, null),
        BeeParameter.createText(COMMONS_MODULE, PRM_URL, false, null));

    params.addAll(getSqlEngineParameters());
    return params;
  }

  @Override
  public String getName() {
    return COMMONS_MODULE;
  }

  @Override
  public String getResourcePath() {
    return getName();
  }

  @Override
  public void init() {
    prm.init();

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      public void refreshIpFilterCache(TableModifyEvent event) {
        if (BeeUtils.same(event.getTargetName(), TBL_IP_FILTERS) && event.isAfter()) {
          sys.initIpFilters();
        }
      }

      @Subscribe
      public void refreshRightsCache(TableModifyEvent event) {
        if (usr.isRightsTable(event.getTargetName()) && event.isAfter()) {
          usr.initRights();
          Endpoint.updateUserData(usr.getAllUserData());
        }
      }

      @Subscribe
      public void refreshUsersCache(TableModifyEvent event) {
        if ((usr.isRoleTable(event.getTargetName()) || usr.isUserTable(event.getTargetName()))
            && event.isAfter()) {
          usr.initUsers();
          Endpoint.updateUserData(usr.getAllUserData());
        }
      }

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
  }

  private ResponseObject blockHost(RequestInfo reqInfo) {
    String host = BeeUtils.trim(reqInfo.getParameter(COL_IP_FILTER_HOST));
    if (BeeUtils.isEmpty(host)) {
      return ResponseObject.parameterNotFound(SVC_BLOCK_HOST, COL_IP_FILTER_HOST);
    }

    if (qs.sqlExists(TBL_IP_FILTERS,
        SqlUtils.and(SqlUtils.equals(TBL_IP_FILTERS, COL_IP_FILTER_HOST, host),
            SqlUtils.isNull(TBL_IP_FILTERS, COL_IP_FILTER_BLOCK_AFTER),
            SqlUtils.isNull(TBL_IP_FILTERS, COL_IP_FILTER_BLOCK_BEFORE)))) {
      return ResponseObject.response(host);
    }

    SqlInsert insert = new SqlInsert(TBL_IP_FILTERS).addConstant(COL_IP_FILTER_HOST, host);
    ResponseObject response = qs.insertDataWithResponse(insert);

    if (response.hasErrors()) {
      return response;
    } else {
      return ResponseObject.response(host);
    }
  }

  private ResponseObject createCity(String name, Long country) {
    Long city;
    if (country == null) {
      city = qs.getId(TBL_CITIES, COL_CITY_NAME, name);
    } else {
      city = qs.getId(TBL_CITIES, COL_CITY_NAME, name, COL_COUNTRY, country);
    }

    if (city == null && country != null) {
      SqlInsert insert = new SqlInsert(TBL_CITIES)
          .addConstant(COL_CITY_NAME, name)
          .addConstant(COL_COUNTRY, country);
      return qs.insertDataWithResponse(insert);
    } else {
      return ResponseObject.response(city);
    }
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
      response = createEmail(email, companyName);
      if (response.hasErrors()) {
        return response;
      }
      row.setValue(DataUtils.getColumnIndex(ALS_EMAIL_ID, columns), response.getResponseAsLong());
    }

    Long country;
    if (!BeeUtils.isEmpty(countryName)) {
      response = createCountry(countryName);
      if (response.hasErrors()) {
        return response;
      }

      country = response.getResponseAsLong();
      if (country != null) {
        row.setValue(DataUtils.getColumnIndex(COL_COUNTRY, columns), country);
      }
    } else {
      country = null;
    }

    if (!BeeUtils.isEmpty(cityName)) {
      response = createCity(cityName, country);
      if (response.hasErrors()) {
        return response;
      }

      Long city = response.getResponseAsLong();
      if (city != null) {
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

  private ResponseObject createCountry(String name) {
    Long country = qs.getId(TBL_COUNTRIES, COL_COUNTRY_NAME, name);

    if (country == null) {
      SqlInsert insert = new SqlInsert(TBL_COUNTRIES).addConstant(COL_COUNTRY_NAME, name);
      return qs.insertDataWithResponse(insert);
    } else {
      return ResponseObject.response(country);
    }
  }

  private ResponseObject createEmail(String address, String label) {
    SqlInsert insert = new SqlInsert(TBL_EMAILS)
        .addConstant(COL_EMAIL_ADDRESS, address)
        .addNotEmpty(COL_EMAIL_LABEL, label);

    return qs.insertDataWithResponse(insert);
  }

  private ResponseObject createUser(RequestInfo reqInfo) {
    String login = reqInfo.getParameter(COL_LOGIN);
    if (BeeUtils.isEmpty(login)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, COL_LOGIN);
    }

    String password = reqInfo.getParameter(COL_PASSWORD);
    if (BeeUtils.isEmpty(password)) {
      return ResponseObject.parameterNotFound(SVC_CREATE_USER, COL_PASSWORD);
    }

    if (usr.isUser(login)) {
      return ResponseObject.warning(usr.getLocalizableMesssages()
          .valueExists(BeeUtils.joinWords(usr.getLocalizableConstants().user(), login)));
    }

    String email = reqInfo.getParameter(COL_EMAIL);
    if (!BeeUtils.isEmpty(email) && qs.sqlExists(TBL_EMAILS, COL_EMAIL_ADDRESS, email)) {
      return ResponseObject.warning(usr.getLocalizableMesssages()
          .valueExists(BeeUtils.joinWords(usr.getLocalizableConstants().email(), email)));
    }

    UserInterface userInterface = EnumUtils.getEnumByIndex(UserInterface.class,
        BeeUtils.toIntOrNull(reqInfo.getParameter(COL_USER_INTERFACE)));

    String companyName = BeeUtils.notEmpty(reqInfo.getParameter(ALS_COMPANY_NAME), login);
    String companyCode = reqInfo.getParameter(ALS_COMPANY_CODE);
    String vatCode = reqInfo.getParameter(COL_COMPANY_VAT_CODE);
    String exchangeCode = reqInfo.getParameter(COL_COMPANY_EXCHANGE_CODE);

    String firstName = BeeUtils.notEmpty(reqInfo.getParameter(COL_FIRST_NAME), login);
    String lastName = reqInfo.getParameter(COL_LAST_NAME);

    String positionName = reqInfo.getParameter(COL_POSITION);

    String address = reqInfo.getParameter(COL_ADDRESS);
    String postIndex = reqInfo.getParameter(COL_POST_INDEX);

    String cityName = reqInfo.getParameter(COL_CITY);
    String countryName = reqInfo.getParameter(COL_COUNTRY);

    String phone = reqInfo.getParameter(COL_PHONE);
    String mobile = reqInfo.getParameter(COL_MOBILE);
    String fax = reqInfo.getParameter(COL_FAX);

    ResponseObject response;

    Long company = qs.getId(TBL_COMPANIES, COL_COMPANY_NAME, companyName);
    if (company == null && !BeeUtils.isEmpty(companyCode)) {
      company = qs.getId(TBL_COMPANIES, COL_COMPANY_CODE, companyCode);
    }

    if (company == null) {
      SqlInsert insCompany = new SqlInsert(TBL_COMPANIES)
          .addConstant(COL_COMPANY_NAME, companyName)
          .addNotEmpty(COL_COMPANY_CODE, companyCode)
          .addNotEmpty(COL_COMPANY_VAT_CODE, vatCode)
          .addNotEmpty(COL_COMPANY_EXCHANGE_CODE, exchangeCode);

      response = qs.insertDataWithResponse(insCompany);
      if (response.hasErrors()) {
        return response;
      }
      company = response.getResponseAsLong();
    }

    SqlInsert insPerson = new SqlInsert(TBL_PERSONS)
        .addConstant(COL_FIRST_NAME, firstName)
        .addNotEmpty(COL_LAST_NAME, lastName);

    response = qs.insertDataWithResponse(insPerson);
    if (response.hasErrors()) {
      return response;
    }
    Long person = response.getResponseAsLong();

    List<BeeColumn> cpColumns = sys.getView(VIEW_COMPANY_PERSONS).getRowSetColumns();
    BeeRow cpRow = DataUtils.createEmptyRow(cpColumns.size());

    cpRow.setValue(DataUtils.getColumnIndex(COL_COMPANY, cpColumns), company);
    cpRow.setValue(DataUtils.getColumnIndex(COL_PERSON, cpColumns), person);

    if (!BeeUtils.isEmpty(email)) {
      response = createEmail(email, BeeUtils.joinWords(firstName, lastName));
      if (response.hasErrors()) {
        return response;
      }
      cpRow.setValue(DataUtils.getColumnIndex(ALS_EMAIL_ID, cpColumns),
          response.getResponseAsLong());
    }

    if (!BeeUtils.isEmpty(positionName)) {
      Long position = qs.getId(TBL_POSITIONS, COL_POSITION_NAME, positionName);

      if (position == null) {
        SqlInsert insPosition = new SqlInsert(TBL_POSITIONS)
            .addConstant(COL_POSITION_NAME, positionName);

        response = qs.insertDataWithResponse(insPosition);
        if (response.hasErrors()) {
          return response;
        }
        position = response.getResponseAsLong();
      }

      if (position != null) {
        cpRow.setValue(DataUtils.getColumnIndex(COL_POSITION, cpColumns), position);
      }
    }

    Long country;
    if (!BeeUtils.isEmpty(countryName)) {
      response = createCountry(countryName);
      if (response.hasErrors()) {
        return response;
      }

      country = response.getResponseAsLong();
      if (country != null) {
        cpRow.setValue(DataUtils.getColumnIndex(COL_COUNTRY, cpColumns), country);
      }
    } else {
      country = null;
    }

    if (!BeeUtils.isEmpty(cityName)) {
      response = createCity(cityName, country);
      if (response.hasErrors()) {
        return response;
      }

      Long city = response.getResponseAsLong();
      if (city != null) {
        cpRow.setValue(DataUtils.getColumnIndex(COL_CITY, cpColumns), city);
      }
    }

    if (!BeeUtils.isEmpty(address)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_ADDRESS, cpColumns), address);
    }
    if (!BeeUtils.isEmpty(postIndex)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_POST_INDEX, cpColumns), postIndex);
    }

    if (!BeeUtils.isEmpty(phone)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_PHONE, cpColumns), phone);
    }
    if (!BeeUtils.isEmpty(mobile)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_MOBILE, cpColumns), mobile);
    }
    if (!BeeUtils.isEmpty(fax)) {
      cpRow.setValue(DataUtils.getColumnIndex(COL_FAX, cpColumns), fax);
    }

    BeeRowSet cpRowSet = DataUtils.createRowSetForInsert(VIEW_COMPANY_PERSONS, cpColumns, cpRow);
    response = deb.commitRow(cpRowSet);
    if (response.hasErrors()) {
      return response;
    }

    Long companyPerson = ((BeeRow) response.getResponse()).getId();

    SqlInsert insUser = new SqlInsert(TBL_USERS)
        .addConstant(COL_LOGIN, login)
        .addConstant(COL_PASSWORD, password)
        .addConstant(COL_COMPANY_PERSON, companyPerson);
    if (userInterface != null) {
      insUser.addConstant(COL_USER_INTERFACE, userInterface.ordinal());
    }

    return qs.insertDataWithResponse(insUser);
  }

  private ResponseObject getCompanyInfo(Long companyId, String locale) {
    if (!DataUtils.isId(companyId)) {
      return ResponseObject.error("Wrong company ID");
    }
    SimpleRow row = qs.getRow(new SqlSelect()
        .addFields(TBL_COMPANIES, COL_NAME, COL_COMPANY_CODE, COL_COMPANY_VAT_CODE)
        .addFields(TBL_CONTACTS, COL_ADDRESS, COL_POST_INDEX, COL_PHONE, COL_MOBILE, COL_FAX)
        .addFields(TBL_EMAILS, COL_EMAIL_ADDRESS)
        .addField(TBL_CITIES, COL_NAME, COL_CITY)
        .addField(TBL_COUNTRIES, COL_NAME, COL_COUNTRY)
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
    translations.put(COL_NAME, constants.company());
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

  private ResponseObject getCurrentExchangeRate(RequestInfo reqInfo) {
    String currency = reqInfo.getParameter(COL_CURRENCY_NAME);
    if (BeeUtils.isEmpty(currency)) {
      return ResponseObject.parameterNotFound(SVC_GET_CURRENT_EXCHANGE_RATE, COL_CURRENCY_NAME);
    }

    String address = getExchangeRatesRemoteAddress();

    if (BeeUtils.isEmpty(address)) {
      return ExchangeRatesWS.getCurrentExchangeRate(currency);
    } else {
      return ExchangeRatesWS.getCurrentExchangeRate(address, currency);
    }
  }

  private ResponseObject getExchangeRate(RequestInfo reqInfo) {
    String currency = reqInfo.getParameter(COL_CURRENCY_NAME);
    if (BeeUtils.isEmpty(currency)) {
      return ResponseObject.parameterNotFound(SVC_GET_EXCHANGE_RATE, COL_CURRENCY_NAME);
    }

    JustDate date = TimeUtils.parseDate(reqInfo.getParameter(COL_CURRENCY_RATE_DATE));
    if (date == null) {
      return ResponseObject.parameterNotFound(SVC_GET_EXCHANGE_RATE, COL_CURRENCY_RATE_DATE);
    }

    String address = getExchangeRatesRemoteAddress();

    if (BeeUtils.isEmpty(address)) {
      return ExchangeRatesWS.getExchangeRate(currency, date);
    } else {
      return ExchangeRatesWS.getExchangeRate(address, currency, date);
    }
  }

  private ResponseObject getExchangeRatesByCurrency(RequestInfo reqInfo) {
    String currency = reqInfo.getParameter(COL_CURRENCY_NAME);
    if (BeeUtils.isEmpty(currency)) {
      return ResponseObject.parameterNotFound(SVC_GET_EXCHANGE_RATES_BY_CURRENCY,
          COL_CURRENCY_NAME);
    }

    JustDate dateLow = TimeUtils.parseDate(reqInfo.getParameter(VAR_DATE_LOW));
    if (dateLow == null) {
      return ResponseObject.parameterNotFound(SVC_GET_EXCHANGE_RATES_BY_CURRENCY, VAR_DATE_LOW);
    }
    JustDate dateHigh = TimeUtils.parseDate(reqInfo.getParameter(VAR_DATE_HIGH));
    if (dateHigh == null) {
      return ResponseObject.parameterNotFound(SVC_GET_EXCHANGE_RATES_BY_CURRENCY, VAR_DATE_HIGH);
    }

    if (TimeUtils.isMore(dateLow, dateHigh)) {
      return ResponseObject.error(usr.getLocalizableConstants().invalidRange(), dateLow, dateHigh);
    }

    String address = getExchangeRatesRemoteAddress();

    if (BeeUtils.isEmpty(address)) {
      return ExchangeRatesWS.getExchangeRatesByCurrency(currency, dateLow, dateHigh);
    } else {
      return ExchangeRatesWS.getExchangeRatesByCurrency(address, currency, dateLow, dateHigh);
    }
  }

  private String getExchangeRatesRemoteAddress() {
    if (prm.hasParameter(PRM_WS_LB_EXCHANGE_RATES_ADDRESS)) {
      return prm.getText(PRM_WS_LB_EXCHANGE_RATES_ADDRESS);
    } else {
      return null;
    }
  }

  private ResponseObject getHistory(String viewName, Collection<Long> idList) {
    LocalizableConstants loc = usr.getLocalizableConstants();

    if (BeeUtils.isEmpty(idList)) {
      return ResponseObject.error(loc.selectAtLeastOneRow());
    }
    BeeView view = sys.getView(viewName);

    SqlSelect query = view.getQuery(ComparisonFilter.idIn(idList), null)
        .resetFields().resetOrder();

    Multimap<String, ViewColumn> columnMap = HashMultimap.create();
    Map<String, Pair<String, String>> idMap = Maps.newHashMap();

    for (ViewColumn col : view.getViewColumns()) {
      if (!col.isHidden() && !col.isReadOnly()
          && (col.getLevel() == 0 || BeeUtils.unbox(col.getEditable()))) {

        String als = view.getColumnSource(col.getName());
        columnMap.put(als, col);

        if (!idMap.containsKey(als)) {
          String parent = col.getParent();

          if (!BeeUtils.isEmpty(parent)) {
            String src = view.getColumnSource(parent);
            String fld = view.getColumnField(parent);
            query.addField(src, fld, parent);

            if (!BeeUtils.isEmpty(query.getGroupBy())) {
              query.addGroup(src, fld);
            }
          } else {
            parent = view.getSourceIdName();
            query.addFields(view.getSourceAlias(), parent);
          }
          idMap.put(als, Pair.of(col.getTable(), parent));
        }
      }
    }
    SimpleRowSet ids = qs.getData(query);
    query = null;

    for (String als : columnMap.keySet()) {
      BeeTable table = sys.getTable(idMap.get(als).getA());

      Set<Long> auditIds = Sets.newHashSet(ids.getLongColumn(idMap.get(als).getB()));
      auditIds.remove(null);

      if (BeeUtils.isEmpty(auditIds) || !table.isAuditable()) {
        continue;
      }
      String src = sys.getAuditSource(table.getName());
      SqlSelect subq = new SqlSelect();

      List<String> fields = Lists.newArrayList();
      List<Object> pairs = Lists.newArrayList();

      for (ViewColumn col : columnMap.get(als)) {
        fields.add(col.getField());

        if (!BeeUtils.same(col.getField(), col.getName())) {
          pairs.add(col.getField());
          pairs.add(col.getName());
        }
      }
      if (!BeeUtils.isEmpty(pairs)) {
        pairs.add(SqlUtils.field(src, AUDIT_FLD_FIELD));

        subq.addExpr(SqlUtils.sqlCase(SqlUtils.field(src, AUDIT_FLD_FIELD),
            pairs.toArray()), AUDIT_FLD_FIELD);
      } else {
        subq.addFields(src, AUDIT_FLD_FIELD);
      }
      subq.addFields(src, AUDIT_FLD_TIME, AUDIT_FLD_TX, AUDIT_FLD_MODE, AUDIT_FLD_ID,
          AUDIT_FLD_VALUE)
          .addConstant(table.getName(), COL_OBJECT)
          .addField(TBL_USERS, COL_LOGIN, COL_USER)
          .addFrom(src)
          .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, src, AUDIT_FLD_USER))
          .setWhere(SqlUtils.and(SqlUtils.inList(src, AUDIT_FLD_ID, auditIds),
              SqlUtils.or(SqlUtils.inList(src, AUDIT_FLD_FIELD, fields),
                  SqlUtils.isNull(src, AUDIT_FLD_FIELD))));

      if (query == null) {
        query = subq.addOrder(src, AUDIT_FLD_TIME, AUDIT_FLD_ID).addOrder(null, COL_OBJECT);
      } else {
        query.setUnionAllMode(true).addUnion(subq);
      }
    }
    BeeRowSet rs = new BeeRowSet(HISTORY_COLUMNS);

    if (query != null) {
      int fldIdx = rs.getColumnIndex(AUDIT_FLD_FIELD);
      int valIdx = rs.getColumnIndex(AUDIT_FLD_VALUE);
      int relIdx = rs.getColumnIndex(COL_RELATION);
      Map<String, String> dict = usr.getLocalizableDictionary();

      for (SimpleRow row : qs.getData(query)) {
        String[] values = new String[rs.getNumberOfColumns()];
        String fld = row.getValue(AUDIT_FLD_FIELD);

        for (int i = 0; i < values.length; i++) {
          String value;

          if (i == relIdx) {
            value = BeeUtils.isEmpty(fld) ? null : view.getColumnRelation(fld);
          } else {
            value = row.getValue(rs.getColumnId(i));
          }
          if (value != null) {
            if (i == fldIdx) {
              value = BeeUtils.notEmpty(Localized.maybeTranslate(view.getColumnLabel(fld), dict),
                  value);

            } else if (i == valIdx) {
              switch (view.getColumnType(fld)) {
                case BOOLEAN:
                  value = BeeUtils.toBoolean(value) ? loc.yes() : loc.no();
                  break;
                case DATE:
                  value = TimeUtils.toDateTimeOrNull(BeeUtils.toLong(value)).toDateString();
                  break;
                case DATETIME:
                  value = TimeUtils.toDateTimeOrNull(BeeUtils.toLong(value)).toCompactString();
                  break;
                case DECIMAL:
                  String enumKey = view.getColumnEnumKey(fld);

                  if (!BeeUtils.isEmpty(enumKey)) {
                    value = BeeUtils.notEmpty(EnumUtils.getLocalizedCaption(enumKey,
                        BeeUtils.toInt(value), loc), value);
                  }
                  break;
                default:
                  break;
              }
            }
          }
          values[i] = value;
        }
        rs.addRow(new BeeRow(0L, values));
      }
    }
    return ResponseObject.response(rs);
  }

  private ResponseObject getListOfCurrencies() {
    String address = getExchangeRatesRemoteAddress();

    if (BeeUtils.isEmpty(address)) {
      return ExchangeRatesWS.getListOfCurrencies();
    } else {
      return ExchangeRatesWS.getListOfCurrencies(address);
    }
  }

  private ResponseObject getNumberInWords(Long number, String locale) {
    Assert.notNull(number);

    Locale loc = I18nUtils.toLocale(locale);

    if (loc == null) {
      loc = usr.getLocale();
    }
    return ResponseObject.response(new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.SPELLOUT)
        .format(number));
  }

  private ResponseObject updateExchangeRates(RequestInfo reqInfo) {
    String low = reqInfo.getParameter(VAR_DATE_LOW);
    if (!BeeUtils.isPositiveInt(low)) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_EXCHANGE_RATES, VAR_DATE_LOW);
    }
    JustDate dateLow = new JustDate(BeeUtils.toInt(low));

    String high = reqInfo.getParameter(VAR_DATE_HIGH);
    if (!BeeUtils.isPositiveInt(high)) {
      return ResponseObject.parameterNotFound(SVC_UPDATE_EXCHANGE_RATES, VAR_DATE_HIGH);
    }
    JustDate dateHigh = new JustDate(BeeUtils.toInt(high));

    if (TimeUtils.isMore(dateLow, dateHigh)) {
      return ResponseObject.error(usr.getLocalizableConstants().invalidRange(), dateLow, dateHigh);
    }

    String currencyIdName = sys.getIdName(TBL_CURRENCIES);

    SqlSelect currencyQuery = new SqlSelect()
        .addFields(TBL_CURRENCIES, currencyIdName, COL_CURRENCY_NAME)
        .addFrom(TBL_CURRENCIES)
        .setWhere(SqlUtils.notNull(TBL_CURRENCIES, COL_CURRENCY_UPDATE_TAG))
        .addOrder(TBL_CURRENCIES, COL_CURRENCY_NAME);

    SimpleRowSet currencies = qs.getData(currencyQuery);
    if (DataUtils.isEmpty(currencies)) {
      return ResponseObject
          .warning(usr.getLocalizableConstants().updateExchangeRatesNoCurrencies());
    }

    String address = getExchangeRatesRemoteAddress();

    ResponseObject response = ResponseObject.emptyResponse();

    for (SimpleRow currencyRow : currencies) {
      Long currencyId = currencyRow.getLong(currencyIdName);
      String currencyName = BeeUtils.trim(currencyRow.getValue(COL_CURRENCY_NAME));

      ResponseObject currencyResponse;
      if (BeeUtils.isEmpty(address)) {
        currencyResponse = ExchangeRatesWS.getExchangeRatesByCurrency(currencyName, dateLow,
            dateHigh);
      } else {
        currencyResponse = ExchangeRatesWS.getExchangeRatesByCurrency(address, currencyName,
            dateLow, dateHigh);
      }

      if (currencyResponse.hasErrors()) {
        response.addErrorsFrom(currencyResponse);
        break;
      }

      SimpleRowSet rates;
      if (currencyResponse.hasResponse(SimpleRowSet.class)) {
        rates = (SimpleRowSet) currencyResponse.getResponse();
      } else {
        rates = null;
      }

      if (DataUtils.isEmpty(rates)) {
        response.addInfo(currencyName, usr.getLocalizableConstants().noData());
        continue;
      }

      String value = rates.getValue(0, COL_CURRENCY_RATE_DATE);
      JustDate min = TimeUtils.parseDate(value);
      if (min == null) {
        response.addWarning(currencyName, usr.getLocalizableConstants().invalidDate(), value);
        continue;
      }

      JustDate max = JustDate.copyOf(min);

      if (rates.getNumberOfRows() > 1) {
        for (int i = 1; i < rates.getNumberOfRows(); i++) {
          JustDate date = TimeUtils.parseDate(rates.getValue(i, COL_CURRENCY_RATE_DATE));
          if (date != null) {
            min = TimeUtils.min(min, date);
            max = TimeUtils.max(max, date);
          }
        }
      }

      SqlDelete delete = new SqlDelete(TBL_CURRENCY_RATES).setWhere(SqlUtils.and(
          SqlUtils.equals(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_CURRENCY, currencyId),
          SqlUtils.moreEqual(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE,
              min.getDateTime().getTime()),
          SqlUtils.less(TBL_CURRENCY_RATES, COL_CURRENCY_RATE_DATE,
              TimeUtils.nextDay(max).getDateTime().getTime())));

      ResponseObject deleteResponse = qs.updateDataWithResponse(delete);
      if (deleteResponse.hasErrors()) {
        response.addErrorsFrom(deleteResponse);
        break;
      }

      int deleteCount = (int) deleteResponse.getResponse();
      int insertCount = 0;

      for (SimpleRow rateRow : rates) {
        DateTime date = TimeUtils.parseDateTime(rateRow.getValue(COL_CURRENCY_RATE_DATE));
        Integer quantity = rateRow.getInt(COL_CURRENCY_RATE_QUANTITY);
        BigDecimal rate = rateRow.getDecimal(COL_CURRENCY_RATE);

        if (date != null && rate != null) {
          SqlInsert insert = new SqlInsert(TBL_CURRENCY_RATES)
              .addConstant(COL_CURRENCY_RATE_CURRENCY, currencyId)
              .addConstant(COL_CURRENCY_RATE_DATE, date.getTime())
              .addNotNull(COL_CURRENCY_RATE_QUANTITY, quantity)
              .addConstant(COL_CURRENCY_RATE, rate);

          ResponseObject insertResponse = qs.insertDataWithResponse(insert);
          if (insertResponse.hasErrors()) {
            response.addErrorsFrom(insertResponse);
            break;
          }

          insertCount++;
        }

        if (response.hasErrors()) {
          break;
        }
      }

      if (response.hasErrors()) {
        break;
      }

      String delMsg = (deleteCount > 0) ? BeeUtils.toString(-deleteCount) : null;
      String insMsg = BeeConst.STRING_PLUS + insertCount;

      response.addInfo(currencyName, delMsg, insMsg);
    }

    return response;
  }
}
