package com.butent.bee.shared.modules.finance;

import com.butent.bee.shared.modules.finance.analysis.TurnoverOrBalance;
import com.butent.bee.shared.modules.finance.analysis.IndicatorKind;
import com.butent.bee.shared.modules.finance.analysis.IndicatorSource;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

public final class FinanceConstants {

  public static final String SVC_POST_TRADE_DOCUMENT = "postTradeDocument";

  public static final String SVC_VERIFY_ANALYSIS_FORM = "verifyAnalysisForm";
  public static final String SVC_CALCULATE_ANALYSIS_FORM = "calculateAnalysisForm";
  public static final String SVC_SAVE_ANALYSIS_RESULTS = "saveAnalysisResults";
  public static final String SVC_GET_ANALYSIS_RESULTS = "getAnalysisResults";

  public static final String TBL_FINANCIAL_RECORDS = "FinancialRecords";

  public static final String TBL_FINANCE_CONFIGURATION = "FinanceConfiguration";
  public static final String TBL_FINANCE_CONTENTS = "FinanceContents";
  public static final String TBL_FINANCE_DISTRIBUTION = "FinanceDistribution";

  public static final String TBL_FINANCIAL_INDICATORS = "FinancialIndicators";
  public static final String TBL_INDICATOR_ACCOUNTS = "IndicatorAccounts";
  public static final String TBL_INDICATOR_FILTERS = "IndicatorFilters";

  public static final String TBL_BUDGET_TYPES = "BudgetTypes";
  public static final String TBL_BUDGET_HEADERS = "BudgetHeaders";
  public static final String TBL_BUDGET_ENTRIES = "BudgetEntries";

  public static final String TBL_ANALYSIS_HEADERS = "AnalysisHeaders";
  public static final String TBL_ANALYSIS_COLUMNS = "AnalysisColumns";
  public static final String TBL_ANALYSIS_ROWS = "AnalysisRows";
  public static final String TBL_ANALYSIS_FILTERS = "AnalysisFilters";
  public static final String TBL_ANALYSIS_RESULTS = "AnalysisResults";

  public static final String VIEW_FINANCIAL_RECORDS = "FinancialRecords";
  public static final String VIEW_FINANCE_PREPAYMENTS = "FinancePrepayments";

  public static final String VIEW_FINANCE_CONFIGURATION = "FinanceConfiguration";
  public static final String VIEW_FINANCE_CONTENTS = "FinanceContents";

  public static final String VIEW_FINANCE_DISTRIBUTION = "FinanceDistribution";
  public static final String VIEW_FINANCE_DISTRIBUTION_OF_ITEMS = "FinanceDistributionOfItems";
  public static final String VIEW_FINANCE_DISTRIBUTION_OF_TRADE_OPERATIONS =
      "FinanceDistributionOfTradeOperations";
  public static final String VIEW_FINANCE_DISTRIBUTION_OF_TRADE_DOCUMENTS =
      "FinanceDistributionOfTradeDocuments";

  public static final String VIEW_FINANCIAL_INDICATORS = "FinancialIndicators";
  public static final String VIEW_INDICATOR_ACCOUNTS = "IndicatorAccounts";

  public static final String VIEW_BUDGET_HEADERS = "BudgetHeaders";
  public static final String VIEW_BUDGET_ENTRIES = "BudgetEntries";

  public static final String VIEW_ANALYSIS_HEADERS = "AnalysisHeaders";
  public static final String VIEW_ANALYSIS_COLUMNS = "AnalysisColumns";
  public static final String VIEW_ANALYSIS_ROWS = "AnalysisRows";
  public static final String VIEW_ANALYSIS_FILTERS = "AnalysisFilters";
  public static final String VIEW_ANALYSIS_RESULTS = "AnalysisResults";

  public static final String COL_FIN_JOURNAL = "Journal";
  public static final String COL_FIN_DATE = "Date";
  public static final String COL_FIN_COMPANY = "Company";
  public static final String COL_FIN_CONTENT = "Content";
  public static final String COL_FIN_DEBIT = "Debit";
  public static final String COL_FIN_DEBIT_SERIES = "DebitSeries";
  public static final String COL_FIN_DEBIT_DOCUMENT = "DebitDocument";
  public static final String COL_FIN_CREDIT = "Credit";
  public static final String COL_FIN_CREDIT_SERIES = "CreditSeries";
  public static final String COL_FIN_CREDIT_DOCUMENT = "CreditDocument";
  public static final String COL_FIN_AMOUNT = "Amount";
  public static final String COL_FIN_CURRENCY = "Currency";
  public static final String COL_FIN_QUANTITY = "Quantity";
  public static final String COL_FIN_TRADE_DOCUMENT = "TradeDocument";
  public static final String COL_FIN_TRADE_PAYMENT = "TradePayment";
  public static final String COL_FIN_PREPAYMENT_KIND = "PrepaymentKind";
  public static final String COL_FIN_PREPAYMENT_PARENT = "PrepaymentParent";
  public static final String COL_FIN_EMPLOYEE = "Employee";

  public static final String COL_DEFAULT_JOURNAL = "DefaultJournal";
  public static final String COL_PETTY_CASH = "PettyCash";
  public static final String COL_CASH_IN_BANK = "CashInBank";
  public static final String COL_RECEIVABLES_FROM_EMPLOYEES = "ReceivablesFromEmployees";
  public static final String COL_LIABILITIES_TO_EMPLOYEES = "LiabilitiesToEmployees";
  public static final String COL_REVENUE_AND_EXPENSE_SUMMARY = "RevenueAndExpenseSummary";
  public static final String COL_TRANSITORY_ACCOUNT = "TransitoryAccount";
  public static final String COL_FOREIGN_EXCHANGE_GAIN = "ForeignExchangeGain";
  public static final String COL_FOREIGN_EXCHANGE_LOSS = "ForeignExchangeLoss";
  public static final String COL_ADVANCE_PAYMENTS_GIVEN = "AdvancePaymentsGiven";
  public static final String COL_ADVANCE_PAYMENTS_RECEIVED = "AdvancePaymentsReceived";
  public static final String COL_COST_OF_MERCHANDISE = "CostOfMerchandise";

  public static final String COL_TRADE_ACCOUNTS_PRECEDENCE = "TradeAccountsPrecedence";
  public static final String COL_TRADE_DIMENSIONS_PRECEDENCE = "TradeDimensionsPrecedence";

  public static final String COL_FIN_DISTR_DATE_FROM = "DateFrom";
  public static final String COL_FIN_DISTR_DATE_TO = "DateTo";
  public static final String COL_FIN_DISTR_DEBIT = "Debit";
  public static final String COL_FIN_DISTR_CREDIT = "Credit";
  public static final String COL_FIN_DISTR_PERCENT = "Percent";
  public static final String COL_FIN_DISTR_DEBIT_REPLACEMENT = "DebitReplacement";
  public static final String COL_FIN_DISTR_CREDIT_REPLACEMENT = "CreditReplacement";
  public static final String COL_FIN_DISTR_ITEM = "Item";
  public static final String COL_FIN_DISTR_TRADE_OPERATION = "Operation";
  public static final String COL_FIN_DISTR_TRADE_DOCUMENT = "TradeDocument";

  public static final String COL_FIN_INDICATOR_KIND = "IndicatorKind";
  public static final String COL_FIN_INDICATOR_NAME = "IndicatorName";
  public static final String COL_FIN_INDICATOR_ABBREVIATION = "IndicatorAbbreviation";
  public static final String COL_FIN_INDICATOR_SOURCE = "IndicatorSource";
  public static final String COL_FIN_INDICATOR_SCRIPT = "IndicatorScript";
  public static final String COL_FIN_INDICATOR_TURNOVER_OR_BALANCE = "IndicatorTurnoverOrBalance";
  public static final String COL_FIN_INDICATOR_NORMAL_BALANCE = "IndicatorNormalBalance";
  public static final String COL_FIN_INDICATOR_CLOSING_ENTRIES = "IndicatorClosingEntries";
  public static final String COL_FIN_INDICATOR_RATIO = "IndicatorRatio";
  public static final String COL_FIN_INDICATOR_SCALE = "IndicatorScale";

  public static final String COL_FIN_INDICATOR = "Indicator";
  public static final String COL_INDICATOR_ACCOUNT = "Account";

  public static final String COL_INDICATOR_FILTER_EMPLOYEE = "Employee";
  public static final String COL_INDICATOR_FILTER_EXTRA = "ExtraFilter";
  public static final String COL_INDICATOR_FILTER_INCLUDE = "Include";

  public static final String COL_BUDGET_TYPE_NAME = "BudgetTypeName";

  public static final String COL_BUDGET_NAME = "BudgetName";
  public static final String COL_BUDGET_HEADER = "BudgetHeader";

  public static final String COL_BUDGET_HEADER_ORDINAL = "Ordinal";
  public static final String COL_BUDGET_HEADER_EMPLOYEE = "Employee";
  public static final String COL_BUDGET_HEADER_INDICATOR = "Indicator";
  public static final String COL_BUDGET_HEADER_TURNOVER_OR_BALANCE = "TurnoverOrBalance";
  public static final String COL_BUDGET_HEADER_TYPE = "BudgetType";
  public static final String COL_BUDGET_HEADER_CURRENCY = "Currency";
  public static final String COL_BUDGET_HEADER_YEAR = "Year";
  public static final String COL_BUDGET_HEADER_BACKGROUND = "Background";
  public static final String COL_BUDGET_HEADER_FOREGROUND = "Foreground";

  private static final String[] COL_BUDGET_SHOW_ENTRY_DIMENSIONS = new String[] {
      "EntryDim01", "EntryDim02", "EntryDim03", "EntryDim04", "EntryDim05",
      "EntryDim06", "EntryDim07", "EntryDim08", "EntryDim09", "EntryDim10"
  };

  public static final String COL_BUDGET_SHOW_ENTRY_EMPLOYEE = "EntryEmployee";

  public static final String COL_BUDGET_ENTRY_ORDINAL = "Ordinal";
  public static final String COL_BUDGET_ENTRY_EMPLOYEE = "Employee";
  public static final String COL_BUDGET_ENTRY_INDICATOR = "Indicator";
  public static final String COL_BUDGET_ENTRY_TURNOVER_OR_BALANCE = "TurnoverOrBalance";
  public static final String COL_BUDGET_ENTRY_TYPE = "BudgetType";
  public static final String COL_BUDGET_ENTRY_YEAR = "Year";

  public static final String[] COL_BUDGET_ENTRY_VALUES = new String[] {
      "Month01", "Month02", "Month03", "Month04", "Month05", "Month06",
      "Month07", "Month08", "Month09", "Month10", "Month11", "Month12"
  };

  public static final String COL_ACCOUNT_NORMAL_BALANCE = "NormalBalance";

  public static final String COL_ANALYSIS_NAME = "AnalysisName";
  public static final String COL_ANALYSIS_HEADER_ORDINAL = "Ordinal";
  public static final String COL_ANALYSIS_HEADER_EMPLOYEE = "Employee";
  public static final String COL_ANALYSIS_HEADER_BUDGET_TYPE = "BudgetType";
  public static final String COL_ANALYSIS_HEADER_YEAR_FROM = "YearFrom";
  public static final String COL_ANALYSIS_HEADER_MONTH_FROM = "MonthFrom";
  public static final String COL_ANALYSIS_HEADER_YEAR_UNTIL = "YearUntil";
  public static final String COL_ANALYSIS_HEADER_MONTH_UNTIL = "MonthUntil";
  public static final String COL_ANALYSIS_HEADER_CURRENCY = "Currency";
  public static final String COL_ANALYSIS_HEADER_BACKGROUND = "Background";
  public static final String COL_ANALYSIS_HEADER_FOREGROUND = "Foreground";

  private static final String[] COL_ANALYSIS_SHOW_COLUMN_DIMENSIONS = new String[] {
      "ColumnDim01", "ColumnDim02", "ColumnDim03", "ColumnDim04", "ColumnDim05",
      "ColumnDim06", "ColumnDim07", "ColumnDim08", "ColumnDim09", "ColumnDim10"
  };

  public static final String COL_ANALYSIS_SHOW_COLUMN_EMPLOYEE = "ColumnEmployee";
  public static final String COL_ANALYSIS_COLUMN_SPLIT_LEVELS = "ColumnSplitLevels";
  public static final String COL_ANALYSIS_COLUMN_FILTERS = "ColumnFilters";

  private static final String[] COL_ANALYSIS_SHOW_ROW_DIMENSIONS = new String[] {
      "RowDim01", "RowDim02", "RowDim03", "RowDim04", "RowDim05",
      "RowDim06", "RowDim07", "RowDim08", "RowDim09", "RowDim10"
  };

  public static final String COL_ANALYSIS_SHOW_ROW_EMPLOYEE = "RowEmployee";
  public static final String COL_ANALYSIS_ROW_SPLIT_LEVELS = "RowSplitLevels";
  public static final String COL_ANALYSIS_ROW_FILTERS = "RowFilters";

  public static final String COL_ANALYSIS_COLUMN_SELECTED = "Selected";
  public static final String COL_ANALYSIS_COLUMN_ORDINAL = "Ordinal";
  public static final String COL_ANALYSIS_COLUMN_NAME = "ColumnName";
  public static final String COL_ANALYSIS_COLUMN_ABBREVIATION = "ColumnAbbreviation";
  public static final String COL_ANALYSIS_COLUMN_VALUES = "ColumnValues";
  public static final String COL_ANALYSIS_COLUMN_INDICATOR = "Indicator";
  public static final String COL_ANALYSIS_COLUMN_TURNOVER_OR_BALANCE = "TurnoverOrBalance";
  public static final String COL_ANALYSIS_COLUMN_BUDGET_TYPE = "BudgetType";
  public static final String COL_ANALYSIS_COLUMN_EMPLOYEE = "Employee";
  public static final String COL_ANALYSIS_COLUMN_YEAR_FROM = "YearFrom";
  public static final String COL_ANALYSIS_COLUMN_MONTH_FROM = "MonthFrom";
  public static final String COL_ANALYSIS_COLUMN_YEAR_UNTIL = "YearUntil";
  public static final String COL_ANALYSIS_COLUMN_MONTH_UNTIL = "MonthUntil";
  public static final String COL_ANALYSIS_COLUMN_SCRIPT = "ColumnScript";
  public static final String COL_ANALYSIS_COLUMN_SCALE = "ColumnScale";
  public static final String COL_ANALYSIS_COLUMN_BACKGROUND = "Background";
  public static final String COL_ANALYSIS_COLUMN_FOREGROUND = "Foreground";
  public static final String COL_ANALYSIS_COLUMN_STYLE = "ColumnStyle";

  public static final String[] COL_ANALYSIS_COLUMN_SPLIT = new String[] {
      "ColumnSplit01", "ColumnSplit02", "ColumnSplit03", "ColumnSplit04", "ColumnSplit05",
      "ColumnSplit06", "ColumnSplit07", "ColumnSplit08", "ColumnSplit09", "ColumnSplit10"
  };

  public static final String COL_ANALYSIS_ROW_SELECTED = "Selected";
  public static final String COL_ANALYSIS_ROW_ORDINAL = "Ordinal";
  public static final String COL_ANALYSIS_ROW_NAME = "RowName";
  public static final String COL_ANALYSIS_ROW_ABBREVIATION = "RowAbbreviation";
  public static final String COL_ANALYSIS_ROW_VALUES = "RowValues";
  public static final String COL_ANALYSIS_ROW_INDICATOR = "Indicator";
  public static final String COL_ANALYSIS_ROW_TURNOVER_OR_BALANCE = "TurnoverOrBalance";
  public static final String COL_ANALYSIS_ROW_BUDGET_TYPE = "BudgetType";
  public static final String COL_ANALYSIS_ROW_EMPLOYEE = "Employee";
  public static final String COL_ANALYSIS_ROW_YEAR_FROM = "YearFrom";
  public static final String COL_ANALYSIS_ROW_MONTH_FROM = "MonthFrom";
  public static final String COL_ANALYSIS_ROW_YEAR_UNTIL = "YearUntil";
  public static final String COL_ANALYSIS_ROW_MONTH_UNTIL = "MonthUntil";
  public static final String COL_ANALYSIS_ROW_SCRIPT = "RowScript";
  public static final String COL_ANALYSIS_ROW_SCALE = "RowScale";
  public static final String COL_ANALYSIS_ROW_BACKGROUND = "Background";
  public static final String COL_ANALYSIS_ROW_FOREGROUND = "Foreground";
  public static final String COL_ANALYSIS_ROW_STYLE = "RowStyle";

  public static final String[] COL_ANALYSIS_ROW_SPLIT = new String[] {
      "RowSplit01", "RowSplit02", "RowSplit03", "RowSplit04", "RowSplit05",
      "RowSplit06", "RowSplit07", "RowSplit08", "RowSplit09", "RowSplit10"
  };

  public static final String COL_ANALYSIS_HEADER = "AnalysisHeader";
  public static final String COL_ANALYSIS_COLUMN = "AnalysisColumn";
  public static final String COL_ANALYSIS_ROW = "AnalysisRow";

  public static final String COL_ANALYSIS_FILTER_EMPLOYEE = "Employee";
  public static final String COL_ANALYSIS_FILTER_EXTRA = "ExtraFilter";
  public static final String COL_ANALYSIS_FILTER_INCLUDE = "Include";

  public static final String COL_ANALYSIS_RESULT_DATE = "Date";
  public static final String COL_ANALYSIS_RESULT_CAPTION = "Caption";
  public static final String COL_ANALYSIS_RESULT_SIZE = "Size";
  public static final String COL_ANALYSIS_RESULTS = "Results";

  public static final String ALS_JOURNAL_BACKGROUND = "JournalBackground";
  public static final String ALS_JOURNAL_FOREGROUND = "JournalForeground";

  public static final String ALS_DEBIT_CODE = "DebitCode";
  public static final String ALS_DEBIT_BACKGROUND = "DebitBackground";
  public static final String ALS_DEBIT_FOREGROUND = "DebitForeground";

  public static final String ALS_CREDIT_CODE = "CreditCode";
  public static final String ALS_CREDIT_BACKGROUND = "CreditBackground";
  public static final String ALS_CREDIT_FOREGROUND = "CreditForeground";

  public static final String ALS_DEBIT_REPLACEMENT_BACKGROUND = "DebitReplacementBackground";
  public static final String ALS_DEBIT_REPLACEMENT_FOREGROUND = "DebitReplacementForeground";

  public static final String ALS_CREDIT_REPLACEMENT_BACKGROUND = "CreditReplacementBackground";
  public static final String ALS_CREDIT_REPLACEMENT_FOREGROUND = "CreditReplacementForeground";

  public static final String ALS_INDICATOR_BACKGROUND = "IndicatorBackground";
  public static final String ALS_INDICATOR_FOREGROUND = "IndicatorForeground";

  public static final String ALS_BUDGET_TYPE_BACKGROUND = "BudgetTypeBackground";
  public static final String ALS_BUDGET_TYPE_FOREGROUND = "BudgetTypeForeground";

  public static final String ALS_EMPLOYEE_FIRST_NAME = "EmployeeFirstName";
  public static final String ALS_EMPLOYEE_LAST_NAME = "EmployeeLastName";

  public static final String PROP_PREPAYMENT_USED = "PrepaymentUsed";

  public static final String GRID_FINANCIAL_RECORDS = "FinancialRecords";
  public static final String GRID_TRADE_DOCUMENT_FINANCIAL_RECORDS =
      "TradeDocumentFinancialRecords";

  public static final String GRID_ITEM_FINANCE_DISTRIBUTION = "ItemFinanceDistribution";
  public static final String GRID_TRADE_OPERATION_FINANCE_DISTRIBUTION =
      "OperationFinanceDistribution";
  public static final String GRID_TRADE_DOCUMENT_FINANCE_DISTRIBUTION =
      "TradeDocumentFinanceDistribution";

  public static final String GRID_FINANCE_DISTRIBUTION_OF_ITEMS = "FinanceDistributionOfItems";
  public static final String GRID_FINANCE_DISTRIBUTION_OF_TRADE_OPERATIONS =
      "FinanceDistributionOfTradeOperations";
  public static final String GRID_FINANCE_DISTRIBUTION_OF_TRADE_DOCUMENTS =
      "FinanceDistributionOfTradeDocuments";

  public static final String GRID_FINANCE_CONTENTS = "FinanceContents";

  public static final String GRID_FINANCIAL_INDICATORS_PRIMARY = "FinancialIndicatorsPrimary";
  public static final String GRID_FINANCIAL_INDICATORS_SECONDARY = "FinancialIndicatorsSecondary";

  public static final String GRID_INDICATOR_ACCOUNTS = "IndicatorAccounts";

  public static final String GRID_BUDGET_HEADERS = "BudgetHeaders";
  public static final String GRID_BUDGET_ENTRIES = "BudgetEntries";

  public static final String GRID_ANALYSIS_HEADERS = "AnalysisHeaders";
  public static final String GRID_ANALYSIS_COLUMNS = "AnalysisColumns";
  public static final String GRID_ANALYSIS_ROWS = "AnalysisRows";
  public static final String GRID_ANALYSIS_RESULTS = "AnalysisResults";

  public static final String GRID_PREPAYMENT_SUPPLIERS = "PrepaymentSuppliers";
  public static final String GRID_PREPAYMENT_CUSTOMERS = "PrepaymentCustomers";
  public static final String GRID_PREPAYMENT_USE = "PrepaymentUse";
  public static final String GRID_OUTSTANDING_PREPAYMENT_GIVEN = "OutstandingPrepaymentGiven";
  public static final String GRID_OUTSTANDING_PREPAYMENT_RECEIVED = "OutstandingPrepaymentReceived";

  public static final String FORM_FINANCE_DEFAULT_ACCOUNTS = "FinanceDefaultAccounts";
  public static final String FORM_FINANCE_POSTING_PRECEDENCE = "FinancePostingPrecedence";

  public static final String FORM_FINANCIAL_INDICATOR_PRIMARY = "FinancialIndicatorPrimary";
  public static final String FORM_FINANCIAL_INDICATOR_SECONDARY = "FinancialIndicatorSecondary";

  public static final String FORM_SIMPLE_BUDGET = "SimpleBudget";
  public static final String FORM_SIMPLE_ANALYSIS = "SimpleAnalysis";

  public static final String FILTER_OUTSTANDING_PREPAYMENT = "outstanding_prepayment";

  public static String colBudgetEntryValue(int month) {
    return COL_BUDGET_ENTRY_VALUES[month - 1];
  }

  public static String colBudgetShowEntryDimension(int dimension) {
    return COL_BUDGET_SHOW_ENTRY_DIMENSIONS[dimension - 1];
  }

  public static String colAnalysisShowColumnDimension(int dimension) {
    return COL_ANALYSIS_SHOW_COLUMN_DIMENSIONS[dimension - 1];
  }

  public static String colAnalysisShowRowDimension(int dimension) {
    return COL_ANALYSIS_SHOW_ROW_DIMENSIONS[dimension - 1];
  }

  public static Integer getBudgetEntryMonth(String colName) {
    int index = ArrayUtils.indexOf(COL_BUDGET_ENTRY_VALUES, colName);
    return (index >= 0) ? index + 1 : null;
  }

  public static Integer getBudgetShowEntryDimension(String colName) {
    int index = ArrayUtils.indexOf(COL_BUDGET_SHOW_ENTRY_DIMENSIONS, colName);
    return (index >= 0) ? index + 1 : null;
  }

  public static Integer getAnalysisShowColumnDimension(String colName) {
    int index = ArrayUtils.indexOf(COL_ANALYSIS_SHOW_COLUMN_DIMENSIONS, colName);
    return (index >= 0) ? index + 1 : null;
  }

  public static Integer getAnalysisShowRowDimension(String colName) {
    int index = ArrayUtils.indexOf(COL_ANALYSIS_SHOW_ROW_DIMENSIONS, colName);
    return (index >= 0) ? index + 1 : null;
  }

  public static boolean normalBalanceIsCredit(Boolean value) {
    return BeeUtils.isTrue(value);
  }

  public static final int ANALYSIS_MIN_SCALE = 0;
  public static final int ANALYSIS_MAX_SCALE = 5;

  public static final int ANALYSIS_MIN_YEAR = 2000;
  public static final int ANALYSIS_MAX_YEAR = 2099;

  public static final YearMonth ANALYSIS_MIN_YEAR_MONTH = new YearMonth(ANALYSIS_MIN_YEAR, 1);
  public static final YearMonth ANALYSIS_MAX_YEAR_MONTH = new YearMonth(ANALYSIS_MAX_YEAR, 12);

  public static void register() {
    EnumUtils.register(NormalBalance.class);

    EnumUtils.register(IndicatorKind.class);
    EnumUtils.register(IndicatorSource.class);
    EnumUtils.register(TurnoverOrBalance.class);

    EnumUtils.register(PrepaymentKind.class);
  }

  private FinanceConstants() {
  }
}
