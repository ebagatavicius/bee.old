package com.butent.bee.server.modules.finance;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashSet;
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
      public void setTradeDocumentSums(DataEvent.ViewQueryEvent event) {
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
}
