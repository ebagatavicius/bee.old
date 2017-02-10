package com.butent.bee.server.modules.finance;

import com.google.common.base.Stopwatch;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.finance.FinanceConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.finance.Dimensions;
import com.butent.bee.shared.modules.finance.FinanceUtils;
import com.butent.bee.shared.modules.finance.TradeAccounts;
import com.butent.bee.shared.modules.finance.TradeAccountsPrecedence;
import com.butent.bee.shared.modules.finance.TradeDimensionsPrecedence;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDocumentSums;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class FinancePostingBean {

  private static BeeLogger logger = LogUtils.getLogger(FinancePostingBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  DataEditorBean deb;

  public ResponseObject postTradeDocument(long docId) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    BeeRowSet docData = qs.getViewDataById(VIEW_TRADE_DOCUMENTS, docId);

    if (DataUtils.isEmpty(docData)) {
      Dictionary dictionary = usr.getDictionary();
      return ResponseObject.warning(dictionary.trdDocument(), docId, dictionary.nothingFound());
    }

    BeeRowSet docLines = qs.getViewData(VIEW_TRADE_DOCUMENT_ITEMS,
        Filter.and(Filter.equals(COL_TRADE_DOCUMENT, docId),
            Filter.notEquals(COL_TRADE_ITEM_QUANTITY, BeeConst.DOUBLE_ZERO)));

    if (DataUtils.isEmpty(docLines)) {
      Dictionary dictionary = usr.getDictionary();
      return ResponseObject.warning(dictionary.trdDocumentItems(), docId,
          dictionary.nothingFound());
    }

    BeeRowSet config = qs.getViewData(VIEW_FINANCE_CONFIGURATION);
    if (DataUtils.isEmpty(config)) {
      Dictionary dictionary = usr.getDictionary();
      return ResponseObject.warning(dictionary.dataNotAvailable(dictionary.finDefaultAccounts()));
    }

    BeeRowSet docPayments = qs.getViewData(VIEW_TRADE_PAYMENTS,
        Filter.and(Filter.equals(COL_TRADE_DOCUMENT, docId),
            Filter.or(Filter.notNull(COL_TRADE_PAYMENT_ACCOUNT),
                Filter.notNull(COL_TRADE_PAYMENT_TYPE_ACCOUNT)),
            Filter.notEquals(COL_TRADE_PAYMENT_AMOUNT, BeeConst.DOUBLE_ZERO)));

    int rowIndex = 0;
    TradeAccounts defaultAccounts = TradeAccounts.createAvailable(config, config.getRow(rowIndex));

    Long defaultJournal = config.getLong(rowIndex, COL_DEFAULT_JOURNAL);
    Long costOfMerchandise = config.getLong(rowIndex, COL_COST_OF_MERCHANDISE);

    List<TradeDimensionsPrecedence> dimensionsPrecedence = TradeDimensionsPrecedence
        .parse(config.getString(rowIndex, COL_TRADE_DIMENSIONS_PRECEDENCE));
    List<TradeAccountsPrecedence> accountsPrecedence =
        TradeAccountsPrecedence.parse(config.getString(rowIndex, COL_TRADE_ACCOUNTS_PRECEDENCE));

    rowIndex = 0;
    DateTime date = docData.getDateTime(rowIndex, COL_TRADE_DOCUMENT_RECEIVED_DATE);
    if (date == null) {
      date = docData.getDateTime(rowIndex, COL_TRADE_DATE);
    }

    String series = docData.getString(rowIndex, COL_SERIES);
    String number = docData.getString(rowIndex, COL_TRADE_NUMBER);
    if (BeeUtils.isEmpty(number)) {
      number = docData.getString(rowIndex, COL_TRADE_DOCUMENT_NUMBER_1);
    }
    if (BeeUtils.isEmpty(number)) {
      number = docData.getString(rowIndex, COL_TRADE_DOCUMENT_NUMBER_2);
    }

    Long operation = docData.getLong(rowIndex, COL_TRADE_OPERATION);

    OperationType operationType = docData.getEnum(rowIndex, COL_OPERATION_TYPE,
        OperationType.class);
    if (operationType == null) {
      Dictionary dictionary = usr.getDictionary();
      return ResponseObject.error(dictionary.valueEmpty(dictionary.trdOperationType()));
    }

    Long supplier = docData.getLong(rowIndex, COL_TRADE_SUPPLIER);
    Long customer = docData.getLong(rowIndex, COL_TRADE_CUSTOMER);

    Long currency = docData.getLong(rowIndex, COL_TRADE_CURRENCY);
    Long payer = docData.getLong(rowIndex, COL_TRADE_PAYER);

    Long company;
    if (DataUtils.isId(payer)) {
      company = payer;
    } else if (operationType.consumesStock()) {
      company = BeeUtils.nvl(customer, supplier);
    } else {
      company = BeeUtils.nvl(supplier, customer);
    }

    Long warehouseFrom = docData.getLong(rowIndex, COL_TRADE_WAREHOUSE_FROM);
    Long warehouseTo = docData.getLong(rowIndex, COL_TRADE_WAREHOUSE_TO);

    Long manager = docData.getLong(rowIndex, COL_TRADE_MANAGER);

    EnumMap<TradeDimensionsPrecedence, Dimensions> dimensions =
        new EnumMap<>(TradeDimensionsPrecedence.class);
    EnumMap<TradeAccountsPrecedence, TradeAccounts> accounts =
        new EnumMap<>(TradeAccountsPrecedence.class);

    dimensions.put(TradeDimensionsPrecedence.DOCUMENT,
        Dimensions.create(docData, docData.getRow(rowIndex)));
    accounts.put(TradeAccountsPrecedence.DOCUMENT,
        TradeAccounts.create(docData, docData.getRow(rowIndex)));

    dimensions.put(TradeDimensionsPrecedence.OPERATION,
        getDimensions(VIEW_TRADE_OPERATIONS, operation));
    accounts.put(TradeAccountsPrecedence.OPERATION,
        getTradeAccounts(VIEW_TRADE_OPERATIONS, operation));

    dimensions.put(TradeDimensionsPrecedence.COMPANY, getDimensions(VIEW_COMPANIES, company));
    accounts.put(TradeAccountsPrecedence.COMPANY, getTradeAccounts(VIEW_COMPANIES, company));

    Dimensions warehouseFromDimensions = getDimensions(VIEW_WAREHOUSES, warehouseFrom);
    TradeAccounts warehouseFromAccounts = getTradeAccounts(VIEW_WAREHOUSES, warehouseFrom);

    Dimensions warehouseToDimensions = getDimensions(VIEW_WAREHOUSES, warehouseTo);
    TradeAccounts warehouseToAccounts = getTradeAccounts(VIEW_WAREHOUSES, warehouseTo);

    int itemIndex = docLines.getColumnIndex(COL_ITEM);
    int isServiceIndex = docLines.getColumnIndex(COL_ITEM_IS_SERVICE);

    int quantityIndex = docLines.getColumnIndex(COL_TRADE_ITEM_QUANTITY);

    int itemWarehouseFromIndex = docLines.getColumnIndex(COL_TRADE_ITEM_WAREHOUSE_FROM);
    int itemWarehouseToIndex = docLines.getColumnIndex(COL_TRADE_ITEM_WAREHOUSE_TO);

    int employeeIndex = docLines.getColumnIndex(COL_TRADE_ITEM_EMPLOYEE);

    int parentIndex = docLines.getColumnIndex(COL_TRADE_ITEM_PARENT);

    int costIndex = docLines.getColumnIndex(COL_TRADE_ITEM_COST);
    int costCurrencyIndex = docLines.getColumnIndex(ALS_COST_CURRENCY);

    int parentCostIndex = docLines.getColumnIndex(ALS_PARENT_COST);
    int parentCostCurrencyIndex = docLines.getColumnIndex(ALS_PARENT_COST_CURRENCY);

    TradeDocumentSums tdSums = TradeDocumentSums.of(docData, docLines);
    double docTotal = tdSums.getTotal();

    Map<Long, Long> itemCategoryTree = getItemCategoryTree();

    List<BeeColumn> columns = sys.getView(VIEW_FINANCIAL_RECORDS).getRowSetColumns();
    BeeRowSet buffer = new BeeRowSet(VIEW_FINANCIAL_RECORDS, columns);

    for (BeeRow row : docLines) {
      Long item = row.getLong(itemIndex);
      boolean isService = BeeUtils.isTrue(row.getBoolean(isServiceIndex));

      Double quantity = row.getDouble(quantityIndex);

      Long employee = row.getLong(employeeIndex);
      if (!DataUtils.isId(employee)) {
        employee = manager;
      }

      Long parent = row.getLong(parentIndex);

      Double cost = row.getDouble(costIndex);
      Long costCurrency = row.getLong(costCurrencyIndex);

      Double parentCost = row.getDouble(parentCostIndex);
      Long parentCostCurrency = row.getLong(parentCostCurrencyIndex);

      Long itemWarehouseFrom = getParentWarehouse(parent);
      if (!DataUtils.isId(itemWarehouseFrom)) {
        itemWarehouseFrom = row.getLong(itemWarehouseFromIndex);
      }
      Long itemWarehouseTo = row.getLong(itemWarehouseToIndex);

      dimensions.put(TradeDimensionsPrecedence.DOCUMENT_LINE, Dimensions.create(docLines, row));
      accounts.put(TradeAccountsPrecedence.DOCUMENT_LINE, TradeAccounts.create(docLines, row));

      dimensions.put(TradeDimensionsPrecedence.ITEM, getDimensions(VIEW_ITEMS, item));
      accounts.put(TradeAccountsPrecedence.ITEM, getTradeAccounts(VIEW_ITEMS, item));

      Long itemGroup = qs.getLongById(TBL_ITEMS, item, COL_ITEM_GROUP);
      Long itemType = qs.getLongById(TBL_ITEMS, item, COL_ITEM_TYPE);
      Set<Long> itemCategories = getItemCategories(item);

      dimensions.put(TradeDimensionsPrecedence.ITEM_GROUP,
          getItemCategoryDimensions(itemGroup, itemCategoryTree));
      accounts.put(TradeAccountsPrecedence.ITEM_GROUP,
          getItemCategoryTradeAccounts(itemGroup, itemCategoryTree));

      dimensions.put(TradeDimensionsPrecedence.ITEM_TYPE,
          getItemCategoryDimensions(itemType, itemCategoryTree));
      accounts.put(TradeAccountsPrecedence.ITEM_TYPE,
          getItemCategoryTradeAccounts(itemType, itemCategoryTree));

      dimensions.put(TradeDimensionsPrecedence.ITEM_CATEGORY,
          getItemCategoriesDimensions(itemCategories, itemCategoryTree));
      accounts.put(TradeAccountsPrecedence.ITEM_CATEGORY,
          getItemCategoriesTradeAccounts(itemCategories, itemCategoryTree));

      dimensions.put(TradeDimensionsPrecedence.WAREHOUSE,
          getWarehouseDimensions(operationType,
              warehouseFrom, warehouseFromDimensions, itemWarehouseFrom,
              warehouseTo, warehouseToDimensions, itemWarehouseTo));
      accounts.put(TradeAccountsPrecedence.WAREHOUSE,
          getWarehouseTradeAccounts(operationType,
              warehouseFrom, warehouseFromAccounts, itemWarehouseFrom,
              warehouseTo, warehouseToAccounts, itemWarehouseTo));

      Dimensions dim = computeTradeDimensions(dimensionsPrecedence, dimensions);
      TradeAccounts acc = computeTradeAccounts(accountsPrecedence, accounts, defaultAccounts);

      Double costAmount = BeeUtils.nonZero(cost) && DataUtils.isId(costCurrency)
          ? cost * quantity : null;
      Double parentCostAmount = DataUtils.isId(parent) && BeeUtils.nonZero(parentCost)
          && DataUtils.isId(parentCostCurrency) ? parentCost * quantity : null;

      double lineVat = tdSums.getItemVat(row.getId());
      double lineTotal = tdSums.getItemTotal(row.getId());

      List<BeeRow> lineRows = new ArrayList<>();

      if (BeeUtils.nonZero(lineTotal - lineVat)) {
        BeeUtils.addNotNull(lineRows,
            post(columns, date,
                operationType.getAmountDebit(acc), operationType.getAmountCredit(acc),
                lineTotal - lineVat, currency, quantity, employee, dim));
      }

      if (BeeUtils.nonZero(parentCostAmount)) {
        BeeUtils.addNotNull(lineRows,
            post(columns, date, acc.getCostOfGoodsSold(), costOfMerchandise,
                parentCostAmount, parentCostCurrency, quantity, employee, dim));
      }

      if (BeeUtils.nonZero(lineVat)) {
        BeeUtils.addNotNull(lineRows,
            post(columns, date, operationType.getVatDebit(acc), operationType.getVatCredit(acc),
                lineVat, currency, quantity, employee, dim));
      }

      if (!DataUtils.isEmpty(docPayments)
          && BeeUtils.nonZero(lineTotal) && BeeUtils.nonZero(docTotal)) {

        lineRows.addAll(postPayments(columns, docPayments, operationType.consumesStock(),
            operationType.getDebtAccount(acc), lineTotal, docTotal, currency, quantity,
            employee, dim));
      }

      if (!lineRows.isEmpty()) {
        buffer.addRows(
            maybeDistribute(lineRows, columns, docId, operation, item, dimensionsPrecedence));
      }
    }

    BeeRowSet output = aggregate(buffer);

    updateJournal(output, defaultJournal);
    updateCompany(output, company);
    updateDocumentNumbers(output, series, number);
    updateContents(output, operation);

    ResponseObject response = commitTradeDocument(docId, output);
    stopwatch.stop();

    if (!response.hasErrors()) {
      logger.info(SVC_POST_TRADE_DOCUMENT, docId,
          buffer.getNumberOfRows(), output.getNumberOfRows(), response.getResponse(),
          BeeUtils.bracket(stopwatch.toString()));
    }

    return response;
  }

  private Dimensions getDimensions(String viewName, Long id) {
    if (DataUtils.isId(id)) {
      BeeRowSet rowSet = qs.getViewDataById(viewName, id);

      if (!DataUtils.isEmpty(rowSet)) {
        return Dimensions.create(rowSet, rowSet.getRow(0));
      }
    }
    return null;
  }

  private Set<Long> getItemCategories(Long item) {
    return qs.getDistinctLongs(TBL_ITEM_CATEGORIES, COL_CATEGORY,
        SqlUtils.equals(TBL_ITEM_CATEGORIES, COL_ITEM, item));
  }

  private Map<Long, Long> getItemCategoryTree() {
    Map<Long, Long> result = new HashMap<>();

    String idName = sys.getIdName(TBL_ITEM_CATEGORY_TREE);
    SqlSelect treeQuery = new SqlSelect()
        .addFields(TBL_ITEM_CATEGORY_TREE, idName, COL_CATEGORY_PARENT)
        .addFrom(TBL_ITEM_CATEGORY_TREE)
        .setWhere(SqlUtils.notNull(TBL_ITEM_CATEGORY_TREE, COL_CATEGORY_PARENT));

    SimpleRowSet treeData = qs.getData(treeQuery);
    if (!DataUtils.isEmpty(treeData)) {
      for (SimpleRowSet.SimpleRow row : treeData) {
        result.put(row.getLong(idName), row.getLong(COL_CATEGORY_PARENT));
      }
    }

    return result;
  }

  private Dimensions getItemCategoryDimensions(Long id, Map<Long, Long> parents) {
    if (!DataUtils.isId(id)) {
      return null;
    }

    Dimensions dimensions = getDimensions(VIEW_ITEM_CATEGORY_TREE, id);

    Long parent = parents.get(id);
    if (!DataUtils.isId(parent)) {
      return dimensions;
    }

    List<Dimensions> list = new ArrayList<>();
    list.add(dimensions);
    list.add(getItemCategoryDimensions(parent, parents));

    return Dimensions.merge(list);
  }

  private Dimensions getItemCategoriesDimensions(Collection<Long> ids, Map<Long, Long> parents) {
    if (BeeUtils.isEmpty(ids)) {
      return null;
    } else {
      return Dimensions.merge(ids.stream()
          .map(id -> getItemCategoryDimensions(id, parents))
          .collect(Collectors.toList()));
    }
  }

  private TradeAccounts getItemCategoryTradeAccounts(Long id, Map<Long, Long> parents) {
    if (!DataUtils.isId(id)) {
      return null;
    }

    TradeAccounts tradeAccounts = getTradeAccounts(VIEW_ITEM_CATEGORY_TREE, id);

    Long parent = parents.get(id);
    if (!DataUtils.isId(parent)) {
      return tradeAccounts;
    }

    List<TradeAccounts> list = new ArrayList<>();
    list.add(tradeAccounts);
    list.add(getItemCategoryTradeAccounts(parent, parents));

    return TradeAccounts.merge(list);
  }

  private TradeAccounts getItemCategoriesTradeAccounts(Collection<Long> ids,
      Map<Long, Long> parents) {

    if (BeeUtils.isEmpty(ids)) {
      return null;
    } else {
      return TradeAccounts.merge(ids.stream()
          .map(id -> getItemCategoryTradeAccounts(id, parents))
          .collect(Collectors.toList()));
    }
  }

  private TradeAccounts getTradeAccounts(String viewName, Long id) {
    if (DataUtils.isId(id)) {
      BeeRowSet rowSet = qs.getViewDataById(viewName, id);

      if (!DataUtils.isEmpty(rowSet)) {
        return TradeAccounts.create(rowSet, rowSet.getRow(0));
      }
    }
    return null;
  }

  private Long getParentWarehouse(Long parent) {
    if (DataUtils.isId(parent)) {
      return qs.getLong(TBL_TRADE_STOCK, COL_STOCK_WAREHOUSE, COL_TRADE_DOCUMENT_ITEM, parent);
    } else {
      return null;
    }
  }

  private Dimensions getWarehouseDimensions(OperationType operationType,
      Long warehouseFrom, Dimensions warehouseFromDimensions, Long itemWarehouseFrom,
      Long warehouseTo, Dimensions warehouseToDimensions, Long itemWarehouseTo) {

    if (operationType.consumesStock()) {
      if (!DataUtils.isId(itemWarehouseFrom) || Objects.equals(itemWarehouseFrom, warehouseFrom)) {
        return warehouseFromDimensions;
      } else {
        return getDimensions(VIEW_WAREHOUSES, itemWarehouseFrom);
      }

    } else {
      if (!DataUtils.isId(itemWarehouseTo) || Objects.equals(itemWarehouseTo, warehouseTo)) {
        return warehouseToDimensions;
      } else {
        return getDimensions(VIEW_WAREHOUSES, itemWarehouseTo);
      }
    }
  }

  private TradeAccounts getWarehouseTradeAccounts(OperationType operationType,
      Long warehouseFrom, TradeAccounts warehouseFromAccounts, Long itemWarehouseFrom,
      Long warehouseTo, TradeAccounts warehouseToAccounts, Long itemWarehouseTo) {

    if (operationType.consumesStock()) {
      if (!DataUtils.isId(itemWarehouseFrom) || Objects.equals(itemWarehouseFrom, warehouseFrom)) {
        return warehouseFromAccounts;
      } else {
        return getTradeAccounts(VIEW_WAREHOUSES, itemWarehouseFrom);
      }

    } else {
      if (!DataUtils.isId(itemWarehouseTo) || Objects.equals(itemWarehouseTo, warehouseTo)) {
        return warehouseToAccounts;
      } else {
        return getTradeAccounts(VIEW_WAREHOUSES, itemWarehouseTo);
      }
    }
  }

  private static Dimensions computeTradeDimensions(List<TradeDimensionsPrecedence> precedence,
      Map<TradeDimensionsPrecedence, Dimensions> input) {

    return Dimensions.merge(precedence.stream().map(input::get).collect(Collectors.toList()));
  }

  private static TradeAccounts computeTradeAccounts(List<TradeAccountsPrecedence> precedence,
      Map<TradeAccountsPrecedence, TradeAccounts> input, TradeAccounts defaultAccounts) {

    List<TradeAccounts> list = precedence.stream().map(input::get).collect(Collectors.toList());
    list.add(defaultAccounts);

    return TradeAccounts.merge(list);
  }

  private static List<BeeRow> postPayments(List<BeeColumn> columns, BeeRowSet payments,
      boolean asDebit, Long debtAccount, double lineTotal, double docTotal,
      Long currency, Double quantity, Long employee, Dimensions dimensions) {

    List<BeeRow> result = new ArrayList<>();

    int dateIndex = payments.getColumnIndex(COL_TRADE_PAYMENT_DATE);
    int amountIndex = payments.getColumnIndex(COL_TRADE_PAYMENT_AMOUNT);

    int accountIndex = payments.getColumnIndex(COL_TRADE_PAYMENT_ACCOUNT);
    int typeAccountIndex = payments.getColumnIndex(COL_TRADE_PAYMENT_TYPE_ACCOUNT);

    int seriesIndex = payments.getColumnIndex(COL_TRADE_PAYMENT_SERIES);
    int numberIndex = payments.getColumnIndex(COL_TRADE_PAYMENT_NUMBER);

    int outputPaymentIndex = DataUtils.getColumnIndex(COL_FIN_TRADE_PAYMENT, columns);

    int outputSeriesIndex = DataUtils.getColumnIndex(asDebit
        ? COL_FIN_DEBIT_SERIES : COL_FIN_CREDIT_SERIES, columns);
    int outputDocumentIndex = DataUtils.getColumnIndex(asDebit
        ? COL_FIN_DEBIT_DOCUMENT : COL_FIN_CREDIT_DOCUMENT, columns);

    for (BeeRow row : payments) {
      DateTime date = row.getDateTime(dateIndex);

      Double amount = row.getDouble(amountIndex);
      Double qty = null;

      if (BeeUtils.nonZero(amount)) {
        if (BeeUtils.nonZero(quantity)) {
          qty = quantity * amount / docTotal;
        }
        amount *= lineTotal / docTotal;
      }

      Long account = row.getLong(accountIndex);
      if (account == null) {
        account = row.getLong(typeAccountIndex);
      }

      String series = row.getString(seriesIndex);
      String number = row.getString(numberIndex);

      Long debit = asDebit ? account : debtAccount;
      Long credit = asDebit ? debtAccount : account;

      BeeRow output = post(columns, date, debit, credit, amount, currency, qty,
          employee, dimensions);

      if (output != null) {
        output.setValue(outputPaymentIndex, row.getId());

        if (!BeeUtils.isEmpty(number)) {
          output.setValue(outputDocumentIndex, number.trim());

          if (!BeeUtils.isEmpty(series)) {
            output.setValue(outputSeriesIndex, series.trim());
          }
        }

        result.add(output);
      }
    }

    return result;
  }

  private List<BeeRow> maybeDistribute(List<BeeRow> input, List<BeeColumn> columns,
      long document, long operation, long item, List<TradeDimensionsPrecedence> precedence) {

    List<BeeRow> result = new ArrayList<>();

    int dateIndex = DataUtils.getColumnIndex(COL_FIN_DATE, columns);

    int debitIndex = DataUtils.getColumnIndex(COL_FIN_DEBIT, columns);
    int creditIndex = DataUtils.getColumnIndex(COL_FIN_CREDIT, columns);

    for (BeeRow row : input) {
      DateTime date = row.getDateTime(dateIndex);

      Long debit = row.getLong(debitIndex);
      Long credit = row.getLong(creditIndex);

      String debitCode = qs.getValueById(TBL_CHART_OF_ACCOUNTS, debit, COL_ACCOUNT_CODE);
      String creditCode = qs.getValueById(TBL_CHART_OF_ACCOUNTS, credit, COL_ACCOUNT_CODE);

      CompoundFilter filter = Filter.and();

      filter.add(
          Filter.or(
              Filter.equals(COL_FIN_DISTR_TRADE_DOCUMENT, document),
              Filter.equals(COL_FIN_DISTR_TRADE_OPERATION, operation),
              Filter.equals(COL_FIN_DISTR_ITEM, item)),
          FinanceUtils.getPeriodFilter(date, COL_FIN_DISTR_DATE_FROM, COL_FIN_DISTR_DATE_TO),
          Filter.or(Filter.isNull(COL_FIN_DISTR_DEBIT),
              FinanceUtils.getStartFilter(debitCode, ALS_DEBIT_CODE)),
          Filter.or(Filter.isNull(COL_FIN_DISTR_CREDIT),
              FinanceUtils.getStartFilter(creditCode, ALS_CREDIT_CODE)),
          Filter.nonNegative(COL_FIN_DISTR_PERCENT));

      BeeRowSet distribution = qs.getViewData(VIEW_FINANCE_DISTRIBUTION, filter);

      if (DataUtils.isEmpty(distribution)) {
        result.add(row);
      } else {
        result.addAll(distribute(row, columns, distribution, precedence));
      }
    }

    return result;
  }

  private static List<BeeRow> distribute(BeeRow input, List<BeeColumn> columns,
      BeeRowSet distribution, List<TradeDimensionsPrecedence> precedence) {

    List<BeeRow> result = new ArrayList<>();

    List<List<BeeRow>> rowsByPrecedence = distributionByPrecedence(distribution, precedence);

    int debitIndex = DataUtils.getColumnIndex(COL_FIN_DEBIT, columns);
    int creditIndex = DataUtils.getColumnIndex(COL_FIN_CREDIT, columns);

    int amountIndex = DataUtils.getColumnIndex(COL_FIN_AMOUNT, columns);
    int quantityIndex = DataUtils.getColumnIndex(COL_FIN_QUANTITY, columns);

    int percentIndex = distribution.getColumnIndex(COL_FIN_DISTR_PERCENT);

    int debitReplacementIndex = distribution.getColumnIndex(COL_FIN_DISTR_DEBIT_REPLACEMENT);
    int creditReplacementIndex = distribution.getColumnIndex(COL_FIN_DISTR_CREDIT_REPLACEMENT);

    List<BeeRow> inputRows = new ArrayList<>();
    inputRows.add(input);

    List<BeeRow> outputRows = new ArrayList<>();

    for (List<BeeRow> distributionRows : rowsByPrecedence) {
      List<Double> factors = normalizeDistributionFactors(distributionRows.stream()
          .map(row -> BeeUtils.unbox(row.getDouble(percentIndex))).collect(Collectors.toList()));

      outputRows.clear();

      for (BeeRow inputRow : inputRows) {
        double inputAmount = inputRow.getDouble(amountIndex);
        Double inputQuantity = inputRow.getDouble(quantityIndex);

        for (int i = 0; i < distributionRows.size(); i++) {
          BeeRow distributionRow = distributionRows.get(i);
          double factor = factors.get(i);

          BeeRow outputRow = DataUtils.cloneRow(inputRow);
          outputRow.setValue(amountIndex, inputAmount * factor);

          if (BeeUtils.nonZero(inputQuantity)) {
            outputRow.setValue(quantityIndex, inputQuantity * factor);
          }

          Long debitReplacement = distributionRow.getLong(debitReplacementIndex);
          if (DataUtils.isId(debitReplacement)) {
            outputRow.setValue(debitIndex, debitReplacement);
          }

          Long creditReplacement = distributionRow.getLong(creditReplacementIndex);
          if (DataUtils.isId(creditReplacement)) {
            outputRow.setValue(creditIndex, creditReplacement);
          }

          Dimensions.create(distribution, distributionRow).applyTo(columns, outputRow);

          outputRows.add(outputRow);
        }

        double remainingFactor = BeeConst.DOUBLE_ONE - BeeUtils.sum(factors);

        if (remainingFactor >= BeeConst.DOUBLE_ONE / BeeConst.DOUBLE_ONE_HUNDRED) {
          BeeRow outputRow = DataUtils.cloneRow(inputRow);
          outputRow.setValue(amountIndex, inputAmount * remainingFactor);

          if (BeeUtils.nonZero(inputQuantity)) {
            outputRow.setValue(quantityIndex, inputQuantity * remainingFactor);
          }

          outputRows.add(outputRow);
        }
      }

      inputRows.clear();
      inputRows.addAll(outputRows);
    }

    if (!outputRows.isEmpty()) {
      result.addAll(outputRows.stream()
          .filter(
              row -> FinanceUtils.isValidEntry(row.getLong(debitIndex), row.getLong(creditIndex))
                  && BeeUtils.isPositive(row.getDouble(amountIndex)))
          .collect(Collectors.toList()));
    }

    if (result.isEmpty()) {
      result.add(input);
    }

    return result;
  }

  private static List<Double> normalizeDistributionFactors(List<Double> percentages) {
    List<Double> factors = new ArrayList<>();

    double total = BeeUtils.sum(percentages);
    long countEmpty = percentages.stream().filter(p -> !BeeUtils.isPositive(p)).count();

    double factor;
    for (Double p : percentages) {
      if (BeeUtils.isPositive(p)) {
        factor = p / BeeConst.DOUBLE_ONE_HUNDRED;

      } else if (total < BeeConst.DOUBLE_ONE_HUNDRED && countEmpty > 0) {
        factor = (1 - total / BeeConst.DOUBLE_ONE_HUNDRED) / countEmpty;

      } else {
        factor = BeeConst.DOUBLE_ZERO;
      }

      factors.add(factor);
    }

    return factors;
  }

  private static List<List<BeeRow>> distributionByPrecedence(BeeRowSet distribution,
      List<TradeDimensionsPrecedence> precedence) {

    List<List<BeeRow>> result = new ArrayList<>();

    List<BeeRow> documentDistribution = new ArrayList<>();
    List<BeeRow> operationDistribution = new ArrayList<>();
    List<BeeRow> itemDistribution = new ArrayList<>();

    int documentIndex = distribution.getColumnIndex(COL_FIN_DISTR_TRADE_DOCUMENT);
    int operationIndex = distribution.getColumnIndex(COL_FIN_DISTR_TRADE_OPERATION);
    int itemIndex = distribution.getColumnIndex(COL_FIN_DISTR_ITEM);

    for (BeeRow row : distribution) {
      if (DataUtils.isId(row.getLong(documentIndex))) {
        documentDistribution.add(row);

      } else if (DataUtils.isId(row.getLong(operationIndex))) {
        operationDistribution.add(row);

      } else if (DataUtils.isId(row.getLong(itemIndex))) {
        itemDistribution.add(row);
      }
    }

    for (int i = precedence.size() - 1; i >= 0; i--) {
      TradeDimensionsPrecedence tdp = precedence.get(i);

      if (tdp == TradeDimensionsPrecedence.DOCUMENT) {
        if (!documentDistribution.isEmpty()) {
          result.add(documentDistribution);
        }

      } else if (tdp == TradeDimensionsPrecedence.OPERATION) {
        if (!operationDistribution.isEmpty()) {
          result.add(operationDistribution);
        }

      } else if (tdp == TradeDimensionsPrecedence.ITEM) {
        if (!itemDistribution.isEmpty()) {
          result.add(itemDistribution);
        }
      }
    }

    return result;
  }

  private void updateContents(BeeRowSet rowSet, Long operation) {
    if (!DataUtils.isEmpty(rowSet)) {
      int contentIndex = rowSet.getColumnIndex(COL_FIN_CONTENT);

      int debitIndex = rowSet.getColumnIndex(COL_FIN_DEBIT);
      int creditIndex = rowSet.getColumnIndex(COL_FIN_CREDIT);

      Map<String, String> contentTranslationColumns =
          sys.getView(rowSet.getViewName()).getTranslationColumns(COL_FIN_CONTENT);

      int precision = rowSet.getColumn(contentIndex).getPrecision();

      int updated = 0;

      for (BeeRow row : rowSet) {
        Long debit = row.getLong(debitIndex);
        Long credit = row.getLong(creditIndex);

        if (BeeUtils.isEmpty(row.getString(contentIndex))
            && FinanceUtils.isValidEntry(debit, credit)) {

          BeeRowSet contents = getContents(debit, credit, Dimensions.create(rowSet, row));
          if (!DataUtils.isEmpty(contents)) {
            int rowIndex = 0;
            row.setValue(contentIndex,
                clampContent(contents.getString(rowIndex, COL_FIN_CONTENT), precision));

            if (!BeeUtils.isEmpty(contentTranslationColumns)) {
              for (String colName : contentTranslationColumns.values()) {
                int sourceIndex = contents.getColumnIndex(colName);
                int targetIndex = rowSet.getColumnIndex(colName);

                if (sourceIndex >= 0 && targetIndex >= 0) {
                  row.setValue(targetIndex,
                      clampContent(contents.getString(rowIndex, sourceIndex), precision));
                }
              }
            }

            updated++;
          }
        }
      }

      if (updated < rowSet.getNumberOfRows() && DataUtils.isId(operation)) {
        BeeRowSet operationData = qs.getViewDataById(VIEW_TRADE_OPERATIONS, operation);

        if (!DataUtils.isEmpty(operationData)) {
          int operationNameIndex = operationData.getColumnIndex(COL_OPERATION_NAME);
          Map<String, String> operationNameTranslationColumns =
              sys.getView(operationData.getViewName()).getTranslationColumns(COL_OPERATION_NAME);

          int rowIndex = 0;

          Map<Integer, String> values = new HashMap<>();
          values.put(contentIndex,
              clampContent(operationData.getString(rowIndex, operationNameIndex), precision));

          if (!BeeUtils.isEmpty(operationNameTranslationColumns)
              && !BeeUtils.isEmpty(contentTranslationColumns)) {

            operationNameTranslationColumns.forEach((locale, colName) -> {
              if (contentTranslationColumns.containsKey(locale)) {
                int sourceIndex = operationData.getColumnIndex(colName);
                int targetIndex = rowSet.getColumnIndex(contentTranslationColumns.get(locale));

                if (sourceIndex >= 0 && targetIndex >= 0) {
                  String value = operationData.getString(rowIndex, sourceIndex);
                  if (!BeeUtils.isEmpty(value)) {
                    values.put(targetIndex, clampContent(value, precision));
                  }
                }
              }
            });
          }

          for (BeeRow row : rowSet) {
            if (BeeUtils.isEmpty(row.getString(contentIndex))) {
              values.forEach(row::setValue);
            }
          }
        }
      }
    }
  }

  private BeeRowSet getContents(Long debit, Long credit, Dimensions dimensions) {
    String debitCode = qs.getValueById(TBL_CHART_OF_ACCOUNTS, debit, COL_ACCOUNT_CODE);
    String creditCode = qs.getValueById(TBL_CHART_OF_ACCOUNTS, credit, COL_ACCOUNT_CODE);

    CompoundFilter filter = Filter.and();
    filter.add(FinanceUtils.getStartFilter(debitCode, ALS_DEBIT_CODE));
    filter.add(FinanceUtils.getStartFilter(creditCode, ALS_CREDIT_CODE));

    if (dimensions != null) {
      filter.add(dimensions.getFilter());
    }

    return qs.getViewData(VIEW_FINANCE_CONTENTS, filter);
  }

  private static String clampContent(String value, int precision) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    } else if (precision > 0) {
      return BeeUtils.left(value.trim(), precision);
    } else {
      return value.trim();
    }
  }

  private static void updateDocumentNumbers(BeeRowSet rowSet, String series, String number) {
    if (!DataUtils.isEmpty(rowSet) && !BeeUtils.isEmpty(number)) {
      int debitSeriesIndex = rowSet.getColumnIndex(COL_FIN_DEBIT_SERIES);
      int debitDocumentIndex = rowSet.getColumnIndex(COL_FIN_DEBIT_DOCUMENT);

      int creditSeriesIndex = rowSet.getColumnIndex(COL_FIN_CREDIT_SERIES);
      int creditDocumentIndex = rowSet.getColumnIndex(COL_FIN_CREDIT_DOCUMENT);

      for (BeeRow row : rowSet) {
        if (BeeUtils.allEmpty(row.getString(debitSeriesIndex),
            row.getString(debitDocumentIndex))) {

          if (!BeeUtils.isEmpty(series)) {
            row.setValue(debitSeriesIndex, series);
          }
          row.setValue(debitDocumentIndex, number);
        }

        if (BeeUtils.allEmpty(row.getString(creditSeriesIndex),
            row.getString(creditDocumentIndex))) {

          if (!BeeUtils.isEmpty(series)) {
            row.setValue(creditSeriesIndex, series);
          }
          row.setValue(creditDocumentIndex, number);
        }
      }
    }
  }

  private static void updateJournal(BeeRowSet rowSet, Long journal) {
    if (!DataUtils.isEmpty(rowSet) && DataUtils.isId(journal)) {
      int index = rowSet.getColumnIndex(COL_FIN_JOURNAL);

      for (BeeRow row : rowSet) {
        if (!DataUtils.isId(row.getLong(index))) {
          row.setValue(index, journal);
        }
      }
    }
  }

  private static void updateCompany(BeeRowSet rowSet, Long company) {
    if (!DataUtils.isEmpty(rowSet) && DataUtils.isId(company)) {
      int index = rowSet.getColumnIndex(COL_FIN_COMPANY);

      for (BeeRow row : rowSet) {
        if (!DataUtils.isId(row.getLong(index))) {
          row.setValue(index, company);
        }
      }
    }
  }

  private static BeeRow post(List<BeeColumn> columns, DateTime date,
      Long debit, Long credit, Double amount, Long currency, Double quantity,
      Long employee, Dimensions dimensions) {

    if (FinanceUtils.isValidEntry(date, debit, credit, amount, currency)) {
      BeeRow row = DataUtils.createEmptyRow(columns.size());

      row.setValue(DataUtils.getColumnIndex(COL_FIN_DATE, columns), date);

      row.setValue(DataUtils.getColumnIndex(COL_FIN_DEBIT, columns), debit);
      row.setValue(DataUtils.getColumnIndex(COL_FIN_CREDIT, columns), credit);

      row.setValue(DataUtils.getColumnIndex(COL_FIN_AMOUNT, columns), amount);
      row.setValue(DataUtils.getColumnIndex(COL_FIN_CURRENCY, columns), currency);

      if (BeeUtils.nonZero(quantity)) {
        row.setValue(DataUtils.getColumnIndex(COL_FIN_QUANTITY, columns), quantity);
      }

      if (DataUtils.isId(employee)) {
        row.setValue(DataUtils.getColumnIndex(COL_FIN_EMPLOYEE, columns), employee);
      }

      if (dimensions != null && !dimensions.isEmpty()) {
        dimensions.applyTo(columns, row);
      }

      return row;

    } else {
      return null;
    }
  }

  private static BeeRowSet aggregate(BeeRowSet input) {
    if (DataUtils.getNumberOfRows(input) <= 1) {
      return input;
    }

    int amountIndex = input.getColumnIndex(COL_FIN_AMOUNT);
    int quantityIndex = input.getColumnIndex(COL_FIN_QUANTITY);

    Map<List<String>, Double> amounts = input.getRows().stream().collect(
        Collectors.groupingBy(row -> FinanceUtils.groupingFunction(row, amountIndex, quantityIndex),
            Collectors.summingDouble(row -> BeeUtils.unbox(row.getDouble(amountIndex)))));

    if (amounts.size() == input.getNumberOfRows()) {
      return input;
    }

    Map<List<String>, Double> quantities = input.getRows().stream().collect(
        Collectors.groupingBy(row -> FinanceUtils.groupingFunction(row, amountIndex, quantityIndex),
            Collectors.summingDouble(row -> BeeUtils.unbox(row.getDouble(quantityIndex)))));

    BeeRowSet result = new BeeRowSet(input.getViewName(), input.getColumns());

    int amountScale = input.getColumn(amountIndex).getScale();
    int quantityScale = input.getColumn(quantityIndex).getScale();

    amounts.forEach((values, amount) -> {
      List<String> list = new ArrayList<>(values);

      list.set(amountIndex, BeeUtils.toString(amount, amountScale));

      Double quantity = quantities.get(values);
      if (BeeUtils.nonZero(quantity)) {
        list.set(quantityIndex, BeeUtils.toString(quantity, quantityScale));
      }

      result.addRow(DataUtils.NEW_ROW_ID, DataUtils.NEW_ROW_VERSION, list);
    });

    return result;
  }

  private ResponseObject commitTradeDocument(long docId, BeeRowSet rowSet) {
    SqlDelete delete = new SqlDelete(TBL_FINANCIAL_RECORDS)
        .setWhere(SqlUtils.equals(TBL_FINANCIAL_RECORDS, COL_FIN_TRADE_DOCUMENT, docId));

    ResponseObject deleteResponse = qs.updateDataWithResponse(delete);
    if (deleteResponse.hasErrors() || DataUtils.isEmpty(rowSet)) {
      return deleteResponse;
    }

    int index = rowSet.getColumnIndex(COL_FIN_TRADE_DOCUMENT);
    for (BeeRow row : rowSet) {
      row.setValue(index, docId);
    }

    BeeRowSet insert = DataUtils.createRowSetForInsert(rowSet);

    for (int i = 0; i < insert.getNumberOfRows(); i++) {
      ResponseObject insertResponse = deb.commitRow(insert, i, RowInfo.class);
      if (insertResponse.hasErrors()) {
        return insertResponse;
      }
    }

    return ResponseObject.response(insert.getNumberOfRows());
  }
}
