package com.butent.bee.client.modules.finance;

import com.google.common.collect.ImmutableList;

import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.rights.Module;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FinanceKeeper {

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.FINANCE, method);
  }

  public static void register() {
    MenuService.FINANCE_CONFIGURATION.setHandler(parameters -> openConfiguration());

    List<String> gridNames = ImmutableList.of(GRID_FINANCIAL_RECORDS,
        GRID_TRADE_DOCUMENT_FINANCIAL_RECORDS);
    String viewName = VIEW_FINANCIAL_RECORDS;

    ConditionalStyle.registerGridColumnColorProvider(gridNames,
        Collections.singleton(COL_FIN_JOURNAL),
        viewName, ALS_JOURNAL_BACKGROUND, ALS_JOURNAL_FOREGROUND);

    registerDebitCreditColor(gridNames, COL_FIN_DEBIT, COL_FIN_CREDIT, viewName);

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

  private static void openConfiguration() {
    Queries.getRowSet(VIEW_FINANCE_CONFIGURATION, null, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          RowFactory.createRow(VIEW_FINANCE_CONFIGURATION);
        } else {
          RowEditor.open(VIEW_FINANCE_CONFIGURATION, result.getRow(0), Opener.modeless());
        }
      }
    });
  }

  private FinanceKeeper() {
  }
}