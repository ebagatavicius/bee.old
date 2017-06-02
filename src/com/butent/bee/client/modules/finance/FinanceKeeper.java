package com.butent.bee.client.modules.finance;

import com.google.common.collect.ImmutableList;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.finance.analysis.AnalysisColumnsGrid;
import com.butent.bee.client.modules.finance.analysis.AnalysisHeadersGrid;
import com.butent.bee.client.modules.finance.analysis.AnalysisResultsGrid;
import com.butent.bee.client.modules.finance.analysis.AnalysisRowsGrid;
import com.butent.bee.client.modules.finance.analysis.BudgetEntriesGrid;
import com.butent.bee.client.modules.finance.analysis.BudgetHeadersGrid;
import com.butent.bee.client.modules.finance.analysis.FinancialIndicatorsGrid;
import com.butent.bee.client.modules.finance.analysis.SimpleAnalysisForm;
import com.butent.bee.client.modules.finance.analysis.SimpleBudgetForm;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.grid.interceptor.UniqueChildInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.modules.finance.analysis.IndicatorKind;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class FinanceKeeper {

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "fin-";

  private static final BeeLogger logger = LogUtils.getLogger(FinanceKeeper.class);

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.FINANCE, method);
  }

  public static void postTradeDocuments(final Collection<Long> docIds,
      final Consumer<Boolean> callback) {

    if (BeeUtils.isEmpty(docIds)) {
      if (callback != null) {
        callback.accept(false);
      }

    } else {
      ParameterList parameters = createArgs(SVC_POST_TRADE_DOCUMENTS);
      parameters.addDataItem(Service.VAR_LIST, DataUtils.buildIdList(docIds));

      BeeKeeper.getRpc().makeRequest(parameters, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasErrors()) {
            if (callback != null) {
              callback.accept(false);
            }

          } else {
            if (response.hasResponse()) {
              logger.debug(SVC_POST_TRADE_DOCUMENTS, response.getResponse());
            }
            if (callback != null) {
              callback.accept(true);
            }
          }
        }
      });
    }
  }

  public static void register() {
    MenuService.FINANCE_DEFAULT_ACCOUNTS.setHandler(parameters ->
        openConfiguration(FORM_FINANCE_DEFAULT_ACCOUNTS));
    MenuService.FINANCE_POSTING_PRECEDENCE.setHandler(parameters ->
        openConfiguration(FORM_FINANCE_POSTING_PRECEDENCE));

    List<String> gridNames = ImmutableList.of(
        GRID_FINANCIAL_RECORDS, GRID_TRADE_DOCUMENT_FINANCIAL_RECORDS,
        GRID_PREPAYMENT_SUPPLIERS, GRID_PREPAYMENT_CUSTOMERS,
        GRID_OUTSTANDING_PREPAYMENT_GIVEN, GRID_OUTSTANDING_PREPAYMENT_RECEIVED);
    String viewName = VIEW_FINANCIAL_RECORDS;

    ConditionalStyle.registerGridColumnColorProvider(gridNames,
        Collections.singleton(COL_FIN_JOURNAL),
        viewName, ALS_JOURNAL_BACKGROUND, ALS_JOURNAL_FOREGROUND);

    registerDebitCreditColor(gridNames, COL_FIN_DEBIT, COL_FIN_CREDIT, viewName);

    registerDebitCreditColor(Collections.singleton(GRID_FINANCE_CONTENTS),
        COL_FIN_DEBIT, COL_FIN_CREDIT, VIEW_FINANCE_CONTENTS);

    gridNames = ImmutableList.of(GRID_ITEM_FINANCE_DISTRIBUTION,
        GRID_TRADE_OPERATION_FINANCE_DISTRIBUTION, GRID_TRADE_DOCUMENT_FINANCE_DISTRIBUTION);
    viewName = VIEW_FINANCE_DISTRIBUTION;

    registerDebitCreditColor(gridNames, COL_FIN_DISTR_DEBIT, COL_FIN_DISTR_CREDIT, viewName);
    registerDebitCreditReplacementColor(gridNames, viewName);

    gridNames = ImmutableList.of(GRID_FINANCE_DISTRIBUTION_OF_ITEMS);
    viewName = VIEW_FINANCE_DISTRIBUTION_OF_ITEMS;

    registerDebitCreditColor(gridNames, COL_FIN_DISTR_DEBIT, COL_FIN_DISTR_CREDIT, viewName);
    registerDebitCreditReplacementColor(gridNames, viewName);

    gridNames = ImmutableList.of(GRID_FINANCE_DISTRIBUTION_OF_TRADE_OPERATIONS);
    viewName = VIEW_FINANCE_DISTRIBUTION_OF_TRADE_OPERATIONS;

    ConditionalStyle.registerGridColumnColorProvider(gridNames,
        Collections.singleton(COL_FIN_DISTR_TRADE_OPERATION),
        viewName, ALS_OPERATION_BACKGROUND, ALS_OPERATION_FOREGROUND);

    registerDebitCreditColor(gridNames, COL_FIN_DISTR_DEBIT, COL_FIN_DISTR_CREDIT, viewName);
    registerDebitCreditReplacementColor(gridNames, viewName);

    gridNames = ImmutableList.of(GRID_FINANCE_DISTRIBUTION_OF_TRADE_DOCUMENTS);
    viewName = VIEW_FINANCE_DISTRIBUTION_OF_TRADE_DOCUMENTS;

    registerDebitCreditColor(gridNames, COL_FIN_DISTR_DEBIT, COL_FIN_DISTR_CREDIT, viewName);
    registerDebitCreditReplacementColor(gridNames, viewName);

    GridFactory.registerGridInterceptor(GRID_FINANCIAL_RECORDS, new FinancialRecordsGrid());
    GridFactory.registerGridInterceptor(GRID_TRADE_DOCUMENT_FINANCIAL_RECORDS,
        new TradeDocumentFinancialRecordsGrid());

    GridFactory.registerGridInterceptor(GRID_PREPAYMENT_SUPPLIERS,
        new PrepaymentGrid(PrepaymentKind.SUPPLIERS));
    GridFactory.registerGridInterceptor(GRID_PREPAYMENT_CUSTOMERS,
        new PrepaymentGrid(PrepaymentKind.CUSTOMERS));

    GridFactory.registerGridInterceptor(GRID_OUTSTANDING_PREPAYMENT_GIVEN,
        new OutstandingPrepaymentGrid(PrepaymentKind.SUPPLIERS));
    GridFactory.registerGridInterceptor(GRID_OUTSTANDING_PREPAYMENT_RECEIVED,
        new OutstandingPrepaymentGrid(PrepaymentKind.CUSTOMERS));

    GridFactory.registerGridInterceptor(ClassifierConstants.GRID_CHART_OF_ACCOUNTS,
        new ChartOfAccountsGrid());

    FormFactory.registerFormInterceptor(FORM_FINANCE_POSTING_PRECEDENCE,
        new FinancePostingPrecedenceForm());

    registerAnalysis();
  }

  private static void registerAnalysis() {
    for (int dimension = 1; dimension <= Dimensions.SPACETIME; dimension++) {
      String label = Dimensions.singular(dimension);

      Localized.setColumnLabel(colBudgetShowEntryDimension(dimension),
          Localized.dictionary().finBudgetShowDimension(label));

      Localized.setColumnLabel(colAnalysisShowColumnDimension(dimension),
          Localized.dictionary().finAnalysisShowColumnDimension(label));
      Localized.setColumnLabel(colAnalysisShowRowDimension(dimension),
          Localized.dictionary().finAnalysisShowRowDimension(label));
    }

    for (int month = 1; month <= 12; month++) {
      Localized.setColumnLabel(colBudgetEntryValue(month), Format.properMonthFull(month));
    }

    for (int i = 0; i < COL_ANALYSIS_COLUMN_SPLIT.length; i++) {
      Localized.setColumnLabel(COL_ANALYSIS_COLUMN_SPLIT[i],
          Localized.dictionary().finAnalysisSplit(i + 1));
    }
    for (int i = 0; i < COL_ANALYSIS_ROW_SPLIT.length; i++) {
      Localized.setColumnLabel(COL_ANALYSIS_ROW_SPLIT[i],
          Localized.dictionary().finAnalysisSplit(i + 1));
    }

    GridFactory.registerGridInterceptor(GRID_FINANCIAL_INDICATORS_PRIMARY,
        new FinancialIndicatorsGrid(IndicatorKind.PRIMARY));
    GridFactory.registerGridInterceptor(GRID_FINANCIAL_INDICATORS_SECONDARY,
        new FinancialIndicatorsGrid(IndicatorKind.SECONDARY));

    GridFactory.registerGridInterceptor(GRID_INDICATOR_ACCOUNTS,
        new UniqueChildInterceptor(Localized.dictionary().finIndicatorAccounts(),
            COL_FIN_INDICATOR, COL_INDICATOR_ACCOUNT, VIEW_CHART_OF_ACCOUNTS,
            Collections.singletonList(COL_ACCOUNT_CODE),
            Arrays.asList(COL_ACCOUNT_CODE, COL_ACCOUNT_NAME)));

    GridFactory.registerGridInterceptor(GRID_BUDGET_HEADERS, new BudgetHeadersGrid());
    GridFactory.registerGridInterceptor(GRID_BUDGET_ENTRIES, new BudgetEntriesGrid());

    GridFactory.registerGridInterceptor(GRID_ANALYSIS_HEADERS, new AnalysisHeadersGrid());
    GridFactory.registerGridInterceptor(GRID_ANALYSIS_COLUMNS, new AnalysisColumnsGrid());
    GridFactory.registerGridInterceptor(GRID_ANALYSIS_ROWS, new AnalysisRowsGrid());
    GridFactory.registerGridInterceptor(GRID_ANALYSIS_RESULTS, new AnalysisResultsGrid());

    FormFactory.registerFormInterceptor(FORM_SIMPLE_BUDGET, new SimpleBudgetForm());
    FormFactory.registerFormInterceptor(FORM_SIMPLE_ANALYSIS, new SimpleAnalysisForm());

    RowEditor.registerFormNameProvider(VIEW_FINANCIAL_INDICATORS, (dataInfo, row) -> {
      IndicatorKind kind = row.getEnum(dataInfo.getColumnIndex(COL_FIN_INDICATOR_KIND),
          IndicatorKind.class);

      return (kind == null) ? null : kind.getEditForm();
    });
  }

  private static void registerDebitCreditColor(Collection<String> gridNames,
      String debitColumn, String creditColumn, String viewName) {

    ConditionalStyle.registerGridColumnColorProvider(gridNames, Collections.singleton(debitColumn),
        viewName, ALS_DEBIT_BACKGROUND, ALS_DEBIT_FOREGROUND);

    ConditionalStyle.registerGridColumnColorProvider(gridNames, Collections.singleton(creditColumn),
        viewName, ALS_CREDIT_BACKGROUND, ALS_CREDIT_FOREGROUND);
  }

  private static void registerDebitCreditReplacementColor(Collection<String> gridNames,
      String viewName) {

    ConditionalStyle.registerGridColumnColorProvider(gridNames,
        Collections.singleton(COL_FIN_DISTR_DEBIT_REPLACEMENT),
        viewName, ALS_DEBIT_REPLACEMENT_BACKGROUND, ALS_DEBIT_REPLACEMENT_FOREGROUND);

    ConditionalStyle.registerGridColumnColorProvider(gridNames,
        Collections.singleton(COL_FIN_DISTR_CREDIT_REPLACEMENT),
        viewName, ALS_CREDIT_REPLACEMENT_BACKGROUND, ALS_CREDIT_REPLACEMENT_FOREGROUND);
  }

  private static void openConfiguration(final String formName) {
    Queries.getRowSet(VIEW_FINANCE_CONFIGURATION, null, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          RowFactory.createRowUsingForm(VIEW_FINANCE_CONFIGURATION, formName, null);
        } else {
          RowEditor.openForm(formName, VIEW_FINANCE_CONFIGURATION, result.getRow(0),
              Opener.modeless(), null);
        }
      }
    });
  }

  private FinanceKeeper() {
  }
}
