package com.butent.bee.server.modules.finance;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class FinanceModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(FinanceModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;

  @EJB
  FinancePostingBean posting;
  @EJB
  AnalysisBean analysis;

  @Override
  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;
    String svc = BeeUtils.trim(service);

    switch (svc) {
      case SVC_POST_TRADE_DOCUMENT:
      case SVC_VERIFY_ANALYSIS_FORM:
      case SVC_CALCULATE_ANALYSIS_FORM:
      case SVC_GET_ANALYSIS_RESULTS:
        Long id = reqInfo.getParameterLong(Service.VAR_ID);

        if (DataUtils.isId(id)) {
          switch (svc) {
            case SVC_POST_TRADE_DOCUMENT:
              response = posting.postTradeDocument(id);
              break;
            case SVC_VERIFY_ANALYSIS_FORM:
              response = analysis.verifyForm(id);
              break;
            case SVC_CALCULATE_ANALYSIS_FORM:
              response = analysis.calculateForm(id);
              break;
            case SVC_GET_ANALYSIS_RESULTS:
              response = analysis.getResults(id);
              break;
            default:
              Assert.untouchable();
              response = null;
          }

        } else {
          response = ResponseObject.parameterNotFound(svc, Service.VAR_ID);
        }
        break;

      case SVC_POST_TRADE_DOCUMENTS:
        response = postTradeDocuments(reqInfo);
        break;

      case SVC_SAVE_ANALYSIS_RESULTS:
        response = analysis.saveResults(reqInfo);
        break;

      default:
        String msg = BeeUtils.joinWords("service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  @Override
  public Module getModule() {
    return Module.FINANCE;
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setPrepaymentUsage(DataEvent.ViewQueryEvent event) {
        if (event.isAfter(VIEW_FINANCE_PREPAYMENTS) && event.hasData()) {
          SqlSelect query = new SqlSelect()
              .addFields(TBL_FINANCIAL_RECORDS, COL_FIN_PREPAYMENT_PARENT)
              .addSum(TBL_FINANCIAL_RECORDS, COL_FIN_AMOUNT)
              .addFrom(TBL_FINANCIAL_RECORDS)
              .setWhere(SqlUtils.inList(TBL_FINANCIAL_RECORDS, COL_FIN_PREPAYMENT_PARENT,
                  event.getRowset().getRowIds()))
              .addGroup(TBL_FINANCIAL_RECORDS, COL_FIN_PREPAYMENT_PARENT);

          SimpleRowSet data = qs.getData(query);

          if (!DataUtils.isEmpty(data)) {
            for (SimpleRowSet.SimpleRow row : data) {
              Long id = row.getLong(0);
              Double used = row.getDouble(1);

              if (DataUtils.isId(id)) {
                BeeRow target = event.getRowset().getRowById(id);

                if (target != null) {
                  target.setProperty(PROP_PREPAYMENT_USED, used);
                  target.setRemovable(false);
                }
              }
            }
          }
        }
      }

      @Subscribe
      @AllowConcurrentEvents
      public void onDeleteTradePayment(DataEvent.ViewDeleteEvent event) {
        if (event.isBefore(TradeConstants.VIEW_TRADE_PAYMENTS)) {
          Set<Long> ids = event.getIds();

          if (!BeeUtils.isEmpty(ids)) {
            SqlSelect query = new SqlSelect()
                .addFields(TBL_FINANCIAL_RECORDS, sys.getIdName(TBL_FINANCIAL_RECORDS))
                .addFrom(TBL_FINANCIAL_RECORDS)
                .setWhere(SqlUtils.inList(TBL_FINANCIAL_RECORDS, COL_FIN_TRADE_PAYMENT, ids));

            Set<Long> finIds = qs.getLongSet(query);
            if (!BeeUtils.isEmpty(finIds)) {
              event.setAttribute(COL_FIN_TRADE_PAYMENT, finIds);
            }
          }

        } else if (event.isAfter(TradeConstants.VIEW_TRADE_PAYMENTS)
            && event.hasAttribute(COL_FIN_TRADE_PAYMENT)) {

          Set<Long> finIds = DataUtils.asIdSet(event.getAttribute(COL_FIN_TRADE_PAYMENT));
          if (!BeeUtils.isEmpty(finIds)) {
            Endpoint.fireDelete(VIEW_FINANCIAL_RECORDS, finIds);
          }
        }
      }
    });

    BeeView.registerConditionProvider(FILTER_OUTSTANDING_PREPAYMENT, (view, args) -> {
      PrepaymentKind kind = Codec.unpack(PrepaymentKind.class, BeeUtils.getQuietly(args, 0));

      Long company = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, 1));
      Long currency = BeeUtils.toLongOrNull(BeeUtils.getQuietly(args, 2));

      if (kind != null) {
        String source = TBL_FINANCIAL_RECORDS;

        HasConditions where = SqlUtils.and();
        where.add(SqlUtils.equals(source, COL_FIN_PREPAYMENT_KIND, kind));

        if (DataUtils.isId(company)) {
          where.add(SqlUtils.equals(source, COL_FIN_COMPANY, company));
        }
        if (DataUtils.isId(currency)) {
          where.add(SqlUtils.equals(source, COL_FIN_CURRENCY, currency));
        }

        String idName = sys.getIdName(source);
        String useSource = SqlUtils.uniqueName("us");
        String useAmount = SqlUtils.uniqueName("ua");

        IsCondition join = SqlUtils.and(
            SqlUtils.join(source, idName, useSource, COL_FIN_PREPAYMENT_PARENT),
            SqlUtils.join(source, COL_FIN_CURRENCY, useSource, COL_FIN_CURRENCY));

        SqlSelect query = new SqlSelect()
            .addFields(source, idName, COL_FIN_AMOUNT)
            .addSum(useSource, COL_FIN_AMOUNT, useAmount)
            .addFrom(source)
            .addFromLeft(source, useSource, join)
            .setWhere(where)
            .addGroup(source, idName, COL_FIN_AMOUNT);

        SimpleRowSet data = qs.getData(query);

        if (!DataUtils.isEmpty(data)) {
          Set<Long> ids = new HashSet<>();

          for (SimpleRowSet.SimpleRow row : data) {
            double diff = BeeUtils.unbox(row.getDouble(COL_FIN_AMOUNT))
                - BeeUtils.unbox(row.getDouble(useAmount));

            if (BeeUtils.nonZero(Localized.normalizeMoney(diff))) {
              ids.add(row.getLong(idName));
            }
          }

          if (!ids.isEmpty()) {
            return SqlUtils.inList(source, idName, ids);
          }
        }
      }

      return SqlUtils.sqlFalse();
    });
  }

  public ResponseObject addPrepayment(PrepaymentKind kind, DateTime date, Long company,
      Long paymentAccount, String series, String document, double amount, Long currency) {

    ResponseObject response = ResponseObject.emptyResponse();

    Dictionary dictionary = usr.getDictionary();
    List<String> messages = new ArrayList<>();

    if (kind == null) {
      messages.add(dictionary.parameterNotFound(NameUtils.getClassName(PrepaymentKind.class)));
    }
    if (date == null) {
      messages.add(dictionary.fieldRequired(COL_FIN_DATE));
    }
    if (company == null) {
      messages.add(dictionary.fieldRequired(COL_FIN_COMPANY));
    }
    if (paymentAccount == null) {
      messages.add(dictionary.parameterNotFound(dictionary.account()));
    }
    if (!BeeUtils.isPositive(amount)) {
      messages.add(dictionary.fieldRequired(COL_FIN_AMOUNT));
    }
    if (currency == null) {
      messages.add(dictionary.fieldRequired(COL_FIN_CURRENCY));
    }

    Long journal = null;
    Long advanceAccount = null;

    if (messages.isEmpty()) {
      BeeRowSet config = qs.getViewData(VIEW_FINANCE_CONFIGURATION);

      if (DataUtils.isEmpty(config)) {
        messages.add(dictionary.dataNotAvailable(dictionary.finDefaultAccounts()));

      } else {
        int rowIndex = 0;

        journal = config.getLong(rowIndex, COL_DEFAULT_JOURNAL);
        advanceAccount = config.getLong(rowIndex, kind.defaultAccountColumn());

        if (!DataUtils.isId(advanceAccount)) {
          messages.add(BeeUtils.joinWords("advance account", kind.defaultAccountColumn(),
              "not available"));
        } else if (Objects.equals(paymentAccount, advanceAccount)) {
          messages.add("payment account equals advance account");
        }
      }
    }

    if (messages.isEmpty()) {
      SqlInsert insert = new SqlInsert(TBL_FINANCIAL_RECORDS)
          .addNotNull(COL_FIN_JOURNAL, journal)
          .addConstant(COL_FIN_DATE, date)
          .addConstant(COL_FIN_COMPANY, company)
          .addConstant(COL_FIN_CONTENT, dictionary.prepayment());

      switch (kind.normalBalance()) {
        case DEBIT:
          insert.addConstant(COL_FIN_DEBIT, advanceAccount)
              .addConstant(COL_FIN_CREDIT, paymentAccount)
              .addNotEmpty(COL_FIN_CREDIT_SERIES, series)
              .addNotEmpty(COL_FIN_CREDIT_DOCUMENT, document);
          break;

        case CREDIT:
          insert.addConstant(COL_FIN_CREDIT, advanceAccount)
              .addConstant(COL_FIN_DEBIT, paymentAccount)
              .addNotEmpty(COL_FIN_DEBIT_SERIES, series)
              .addNotEmpty(COL_FIN_DEBIT_DOCUMENT, document);
          break;
      }

      insert.addConstant(COL_FIN_AMOUNT, Localized.normalizeMoney(amount))
          .addConstant(COL_FIN_CURRENCY, currency)
          .addConstant(COL_FIN_PREPAYMENT_KIND, kind);

      ResponseObject insertResponse = qs.insertDataWithResponse(insert);
      if (insertResponse.hasErrors()) {
        return insertResponse;
      }

    } else {
      response.addWarning("cannot add prepayment");
      messages.forEach(response::addWarning);
    }

    return response;
  }

  public Long getDefaultAccount(String colName) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_FINANCE_CONFIGURATION, colName)
        .addFrom(TBL_FINANCE_CONFIGURATION)
        .setWhere(SqlUtils.notNull(TBL_FINANCE_CONFIGURATION, colName));

    Set<Long> set = qs.getLongSet(query);
    if (set.size() == 1) {
      return set.stream().findFirst().get();
    } else {
      return null;
    }
  }

  private ResponseObject postTradeDocuments(RequestInfo reqInfo) {
    Set<Long> docIds = DataUtils.parseIdSet(reqInfo.getParameter(Service.VAR_LIST));
    if (BeeUtils.isEmpty(docIds)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), Service.VAR_LIST);
    }

    ResponseObject response = ResponseObject.emptyResponse();

    int count = 0;
    Stopwatch stopwatch = Stopwatch.createStarted();

    for (Long docId : docIds) {
      ResponseObject postResponse = posting.postTradeDocument(docId);
      if (postResponse.hasErrors()) {
        return postResponse;
      }

      response.addMessagesFrom(postResponse);
      if (postResponse.hasResponse(Integer.class)) {
        count += BeeUtils.unbox(postResponse.getResponseAsInt());
      }
    }

    response.setResponse(BeeUtils.joinWords(docIds.size(), count,
        BeeUtils.bracket(stopwatch.toString())));
    return response;
  }
}
