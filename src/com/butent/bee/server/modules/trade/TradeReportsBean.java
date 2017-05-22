package com.butent.bee.server.modules.trade;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.AdministrationModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.OperationType;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeMovementColumn;
import com.butent.bee.shared.modules.trade.TradeMovementGroup;
import com.butent.bee.shared.modules.trade.TradeReportGroup;
import com.butent.bee.shared.report.ReportParameters;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.NullOrdering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class TradeReportsBean {

  private static BeeLogger logger = LogUtils.getLogger(TradeReportsBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  ParamHolderBean prm;
  @EJB
  AdministrationModuleBean adm;

  public ResponseObject doService(String service, RequestInfo reqInfo) {
    ResponseObject response;

    String svc = BeeUtils.trim(service);
    switch (svc) {
      case SVC_TRADE_STOCK_REPORT:
        response = doStockReport(reqInfo, false);
        break;

      case SVC_TRADE_MOVEMENT_OF_GOODS_REPORT:
        response = doStockReport(reqInfo, true);
        break;

      default:
        String msg = BeeUtils.joinWords("service not recognized:", svc);
        logger.warning(msg);
        response = ResponseObject.error(msg);
    }

    return response;
  }

  private ResponseObject doStockReport(RequestInfo reqInfo, boolean movement) {
    if (!reqInfo.hasParameter(Service.VAR_REPORT_PARAMETERS)) {
      return ResponseObject.parameterNotFound(reqInfo.getLabel(), Service.VAR_REPORT_PARAMETERS);
    }

    ReportParameters parameters =
        ReportParameters.restore(reqInfo.getParameter(Service.VAR_REPORT_PARAMETERS));

    DateTime startDate = null;
    DateTime endDate;

    if (movement) {
      startDate = parameters.getDateTime(RP_START_DATE);
      endDate = parameters.getDateTime(RP_END_DATE);

      if (startDate == null) {
        return ResponseObject.parameterNotFound(reqInfo.getLabel(), RP_START_DATE);
      }
      if (endDate == null) {
        return ResponseObject.parameterNotFound(reqInfo.getLabel(), RP_END_DATE);
      }

    } else {
      endDate = parameters.getDateTime(RP_DATE);
    }

    boolean showQuantity = parameters.getBoolean(RP_SHOW_QUANTITY);
    boolean showAmount = parameters.getBoolean(RP_SHOW_AMOUNT);

    if (!showQuantity && !showAmount) {
      showQuantity = true;
      showAmount = true;

      parameters.add(RP_SHOW_QUANTITY, showQuantity);
      parameters.add(RP_SHOW_AMOUNT, showAmount);
    }

    ItemPrice itemPrice = parameters.getEnum(RP_ITEM_PRICE, ItemPrice.class);
    if (itemPrice == ItemPrice.COST) {
      itemPrice = null;
      parameters.remove(RP_ITEM_PRICE);
    }

    Long currency = parameters.getLong(RP_CURRENCY);
    if (currency == null && showAmount) {
      currency = prm.getRelation(AdministrationConstants.PRM_CURRENCY);
      parameters.add(RP_CURRENCY, currency);
    }

    Set<Long> warehouses = parameters.getIds(RP_WAREHOUSES);
    Set<Long> suppliers = parameters.getIds(RP_SUPPLIERS);
    Set<Long> manufacturers = parameters.getIds(RP_MANUFACTURERS);
    Set<Long> documents = parameters.getIds(RP_DOCUMENTS);

    Set<Long> itemTypes = parameters.getIds(RP_ITEM_TYPES);
    Set<Long> itemGroups = parameters.getIds(RP_ITEM_GROUPS);
    Set<Long> itemCategories = parameters.getIds(RP_ITEM_CATEGORIES);
    Set<Long> items = parameters.getIds(RP_ITEMS);

    DateTime receivedFrom = parameters.getDateTime(RP_RECEIVED_FROM);
    DateTime receivedTo = parameters.getDateTime(RP_RECEIVED_TO);

    String itemFilter = parameters.getText(RP_ITEM_FILTER);

    if (!items.isEmpty()) {
      manufacturers.clear();
      parameters.remove(RP_MANUFACTURERS);

      itemTypes.clear();
      parameters.remove(RP_ITEM_TYPES);

      itemGroups.clear();
      parameters.remove(RP_ITEM_GROUPS);

      itemCategories.clear();
      parameters.remove(RP_ITEM_CATEGORIES);

      itemFilter = null;
      parameters.remove(RP_ITEM_FILTER);
    }

    List<TradeReportGroup> rowGroups = TradeReportGroup.parseList(parameters, 10);
    TradeReportGroup stockGroup = TradeReportGroup.parse(parameters.getText(RP_STOCK_COLUMNS));

    List<TradeMovementGroup> movementGroups;
    if (movement) {
      movementGroups = TradeMovementGroup.parseList(parameters.getText(RP_MOVEMENT_COLUMNS));
    } else {
      movementGroups = new ArrayList<>();
    }

    if (rowGroups.isEmpty()) {
      if (!TradeReportGroup.WAREHOUSE.equals(stockGroup)) {
        rowGroups.add(TradeReportGroup.WAREHOUSE);
      }

      if (!TradeReportGroup.ITEM.equals(stockGroup)) {
        rowGroups.add(TradeReportGroup.ITEM);
      }
      if (!EnumUtils.in(stockGroup, TradeReportGroup.ITEM, TradeReportGroup.ARTICLE)) {
        rowGroups.add(TradeReportGroup.ARTICLE);
      }

      if (showQuantity && !TradeReportGroup.UNIT.equals(stockGroup)) {
        rowGroups.add(TradeReportGroup.UNIT);
      }

    } else if (stockGroup != null && rowGroups.contains(stockGroup)) {
      rowGroups.remove(stockGroup);
    }

    boolean summary = parameters.getBoolean(RP_SUMMARY);

    Map<TradeReportGroup, String> groupValueAliases = new EnumMap<>(TradeReportGroup.class);
    rowGroups.forEach(trg -> groupValueAliases.put(trg, trg.getValueAlias()));
    if (stockGroup != null) {
      groupValueAliases.put(stockGroup, stockGroup.getValueAlias());
    }

    boolean needsYear = groupValueAliases.containsKey(TradeReportGroup.YEAR_RECEIVED);
    boolean needsMonth = groupValueAliases.containsKey(TradeReportGroup.MONTH_RECEIVED);

    String aliasStock = TBL_TRADE_STOCK;
    IsCondition stockCondition = getStockCondition(aliasStock, warehouses);

    String aliasPrimaryDocumentItems = SqlUtils.uniqueName("pdi");
    IsCondition primaryDocumentItemCondition = getPrimaryDocumentItemCondition(
        aliasPrimaryDocumentItems, items, itemCategories, manufacturers);

    String aliasPrimaryDocuments = SqlUtils.uniqueName("pdo");
    IsCondition primaryDocumentCondition =
        getPrimaryDocumentCondition(aliasPrimaryDocuments, suppliers, receivedFrom, receivedTo);

    String aliasDocumentItems = SqlUtils.uniqueName("dit");
    IsCondition documentItemCondition = getDocumentItemCondition(aliasDocumentItems, documents);

    String aliasItems = TBL_ITEMS;
    IsCondition itemTypeCondition = getItemTypeCondition(aliasItems, itemTypes);
    IsCondition itemGroupCondition = getItemGroupCondition(aliasItems, itemGroups);

    IsCondition itemCondition = parseItemFilter(itemFilter);

    boolean needsCost = showAmount && itemPrice == null;

    boolean needsItems = itemTypeCondition != null || itemGroupCondition != null
        || itemCondition != null || showAmount && itemPrice != null
        || TradeReportGroup.needsItem(stockGroup) || TradeReportGroup.needsItem(rowGroups);

    boolean needsPrimaryDocuments = primaryDocumentCondition != null
        || TradeReportGroup.needsPrimaryDocument(stockGroup)
        || TradeReportGroup.needsPrimaryDocument(rowGroups);

    boolean needsPrimaryDocumentItems = needsItems || needsPrimaryDocuments
        || primaryDocumentItemCondition != null
        || TradeReportGroup.needsPrimaryDocumentItem(stockGroup)
        || TradeReportGroup.needsPrimaryDocumentItem(rowGroups);

    String documentItemId = sys.getIdName(TBL_TRADE_DOCUMENT_ITEMS);
    String documentId = sys.getIdName(TBL_TRADE_DOCUMENTS);

    String aliasQuantity = COL_STOCK_QUANTITY;
    String aliasStartQuantity = PREFIX_START_STOCK + aliasQuantity;
    String aliasEndQuantity = PREFIX_END_STOCK + aliasQuantity;

    String aliasPrice = COL_TRADE_ITEM_PRICE;
    String aliasCurrency = COL_TRADE_CURRENCY;

    String aliasAmount = COL_TRADE_AMOUNT;
    String aliasStartAmount = PREFIX_START_STOCK + aliasAmount;
    String aliasEndAmount = PREFIX_END_STOCK + aliasAmount;

    String aliasYear = BeeConst.YEAR;
    String aliasMonth = BeeConst.MONTH;

    int quantityPrecision = sys.getFieldPrecision(TBL_TRADE_STOCK, COL_STOCK_QUANTITY);
    int quantityScale = sys.getFieldScale(TBL_TRADE_STOCK, COL_STOCK_QUANTITY);

    int amountPrecision = sys.getFieldPrecision(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST);
    int amountScale = sys.getFieldScale(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST);

    List<String> stockColumnGroupLabels = new ArrayList<>();
    List<String> stockColumnGroupValues = new ArrayList<>();

    List<String> stockStartLabels = new ArrayList<>();
    List<String> stockStartValues = new ArrayList<>();

    List<String> stockEndLabels = new ArrayList<>();
    List<String> stockEndValues = new ArrayList<>();

    List<TradeMovementColumn> movementInColumns = new ArrayList<>();
    List<TradeMovementColumn> movementOutColumns = new ArrayList<>();

    List<String> rowGroupValueColumns = new ArrayList<>();
    List<String> rowGroupLabelColumns = new ArrayList<>();

    List<String> quantityColumns = new ArrayList<>();
    List<String> amountColumns = new ArrayList<>();

    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRADE_STOCK, COL_TRADE_DOCUMENT_ITEM);

    groupValueAliases.forEach((tgr, alias) -> {
      String source;

      switch (tgr.valueSource()) {
        case TBL_TRADE_DOCUMENTS:
          source = aliasPrimaryDocuments;
          break;
        case TBL_TRADE_DOCUMENT_ITEMS:
          source = tgr.primaryDocument() ? aliasPrimaryDocumentItems : aliasDocumentItems;
          break;
        default:
          source = tgr.valueSource();
      }

      query.addField(source, tgr.valueColumn(), alias);
    });

    if (needsYear || needsMonth) {
      query.addEmptyNumeric(aliasYear, 4, 0);
      query.addEmptyNumeric(aliasMonth, 6, 0);
    }

    if (movement) {
      query.addExpr(zero(quantityPrecision, quantityScale), aliasStartQuantity);
      query.addExpr(zero(quantityPrecision, quantityScale), aliasEndQuantity);

    } else if (endDate == null) {
      query.addField(TBL_TRADE_STOCK, COL_STOCK_QUANTITY, aliasQuantity);
    } else {
      query.addExpr(zero(quantityPrecision, quantityScale), aliasQuantity);
    }

    if (showAmount) {
      if (needsCost) {
        query.addField(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST, aliasPrice);
        query.addField(TBL_TRADE_ITEM_COST, COL_TRADE_ITEM_COST_CURRENCY, aliasCurrency);
      } else {
        query.addField(TBL_ITEMS, itemPrice.getPriceColumn(), aliasPrice);
        query.addField(TBL_ITEMS, itemPrice.getCurrencyColumn(), aliasCurrency);
      }

      if (movement) {
        query.addExpr(zero(amountPrecision, amountScale), aliasStartAmount);
        query.addExpr(zero(amountPrecision, amountScale), aliasEndAmount);
      } else {
        query.addExpr(zero(amountPrecision, amountScale), aliasAmount);
      }
    }

    query.addFrom(TBL_TRADE_STOCK);

    if (needsPrimaryDocumentItems) {
      query.addFromLeft(TBL_TRADE_DOCUMENT_ITEMS, aliasPrimaryDocumentItems,
          SqlUtils.join(aliasPrimaryDocumentItems, documentItemId,
              aliasStock, COL_PRIMARY_DOCUMENT_ITEM));
    }
    if (needsPrimaryDocuments) {
      query.addFromLeft(TBL_TRADE_DOCUMENTS, aliasPrimaryDocuments,
          SqlUtils.join(aliasPrimaryDocuments, documentId,
              aliasPrimaryDocumentItems, COL_TRADE_DOCUMENT));
    }

    if (documentItemCondition != null) {
      query.addFromLeft(TBL_TRADE_DOCUMENT_ITEMS, aliasDocumentItems,
          SqlUtils.join(aliasDocumentItems, documentItemId,
              aliasStock, COL_TRADE_DOCUMENT_ITEM));
    }

    if (needsItems) {
      query.addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, aliasPrimaryDocumentItems, COL_ITEM));
    }

    if (needsCost) {
      query.addFromLeft(TBL_TRADE_ITEM_COST,
          SqlUtils.join(TBL_TRADE_ITEM_COST, COL_TRADE_DOCUMENT_ITEM,
              TBL_TRADE_STOCK, COL_PRIMARY_DOCUMENT_ITEM));
    }

    HasConditions where = SqlUtils.and(stockCondition, primaryDocumentItemCondition,
        primaryDocumentCondition, documentItemCondition, itemTypeCondition, itemGroupCondition,
        itemCondition);

    if (endDate == null && !movement) {
      where.add(SqlUtils.nonZero(TBL_TRADE_STOCK, COL_STOCK_QUANTITY));
    }

    query.setWhere(normalize(where));

    String tmp = qs.sqlCreateTemp(query);
    if (qs.isEmpty(tmp)) {
      qs.sqlDropTemp(tmp);
      return ResponseObject.emptyResponse();
    }

    qs.sqlIndex(tmp, COL_TRADE_DOCUMENT_ITEM);

    if (movement) {
      ResponseObject response = calculateStock(tmp, aliasStartQuantity, startDate);
      if (!response.hasErrors()) {
        response = calculateStock(tmp, aliasEndQuantity, endDate);
      }

      if (response.hasErrors()) {
        qs.sqlDropTemp(tmp);
        return response;
      }

    } else if (endDate != null) {
      ResponseObject response = calculateStock(tmp, aliasQuantity, endDate);
      if (response.hasErrors()) {
        qs.sqlDropTemp(tmp);
        return response;
      }

      SqlDelete delete = new SqlDelete(tmp)
          .setWhere(SqlUtils.or(SqlUtils.isNull(tmp, aliasQuantity),
              SqlUtils.equals(tmp, aliasQuantity, 0)));

      response = qs.updateDataWithResponse(delete);
      if (response.hasErrors()) {
        qs.sqlDropTemp(tmp);
        return response;
      }

      if (qs.isEmpty(tmp)) {
        qs.sqlDropTemp(tmp);
        return ResponseObject.emptyResponse();
      }
    }

    if (showAmount) {
      qs.sqlIndex(tmp, aliasCurrency);

      ResponseObject response = maybeExchange(tmp, aliasPrice, aliasCurrency, endDate, currency);

      if (!showQuantity && !response.hasErrors()) {
        SqlDelete delete = new SqlDelete(tmp)
            .setWhere(SqlUtils.or(SqlUtils.isNull(tmp, aliasPrice),
                SqlUtils.equals(tmp, aliasPrice, 0)));

        response = qs.updateDataWithResponse(delete);

        if (!response.hasErrors() && qs.isEmpty(tmp)) {
          qs.sqlDropTemp(tmp);
          return ResponseObject.emptyResponse();
        }
      }

      if (!response.hasErrors()) {
        if (movement) {
          response = calculateAmount(tmp, aliasStartQuantity, aliasPrice, aliasStartAmount);
          if (!response.hasErrors()) {
            response = calculateAmount(tmp, aliasEndQuantity, aliasPrice, aliasEndAmount);
          }

        } else {
          response = calculateAmount(tmp, aliasQuantity, aliasPrice, aliasAmount);
        }
      }

      if (response.hasErrors()) {
        qs.sqlDropTemp(tmp);
        return response;
      }
    }

    if (needsYear || needsMonth) {
      String dateAlias = BeeUtils.notEmpty(
          groupValueAliases.get(TradeReportGroup.YEAR_RECEIVED),
          groupValueAliases.get(TradeReportGroup.MONTH_RECEIVED));

      qs.setYearMonth(tmp, dateAlias, aliasYear, aliasMonth);

      if (needsYear) {
        groupValueAliases.put(TradeReportGroup.YEAR_RECEIVED, aliasYear);

      } else {
        SqlUpdate update = new SqlUpdate(tmp)
            .addExpression(aliasMonth,
                SqlUtils.plus(SqlUtils.multiply(SqlUtils.field(tmp, aliasYear), 100),
                    SqlUtils.field(tmp, aliasMonth)));

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          qs.sqlDropTemp(tmp);
          return response;
        }
      }

      if (needsMonth) {
        groupValueAliases.put(TradeReportGroup.MONTH_RECEIVED, aliasMonth);
      }
    }

    if (stockGroup == null) {
      if (showQuantity) {
        if (movement) {
          quantityColumns.add(aliasStartQuantity);
          quantityColumns.add(aliasEndQuantity);
        } else {
          quantityColumns.add(aliasQuantity);
        }
      }

      if (showAmount) {
        if (movement) {
          amountColumns.add(aliasStartAmount);
          amountColumns.add(aliasEndAmount);
        } else {
          amountColumns.add(aliasAmount);
        }
      }

    } else {
      String valueColumn = groupValueAliases.get(stockGroup);
      qs.sqlIndex(tmp, valueColumn);

      Multimap<String, Object> labelToValue = getGroupLabels(stockGroup, tmp, valueColumn,
          needsYear);

      if (!labelToValue.isEmpty()) {
        stockColumnGroupLabels.addAll(labelToValue.keySet());
        stockColumnGroupLabels.sort(null);

        if (stockGroup.isEditable()) {
          for (String label : stockColumnGroupLabels) {
            stockColumnGroupValues.add(BeeUtils.joinItems(labelToValue.get(label)));
          }
        }
      }

      boolean hasEmptyValue = qs.sqlExists(tmp, SqlUtils.isNull(tmp, valueColumn));

      List<BeeColumn> pivotColumns = new ArrayList<>();

      if (showQuantity) {
        if (movement) {
          pivotColumns.add(new BeeColumn(ValueType.DECIMAL, aliasStartQuantity,
              quantityPrecision, quantityScale));
          pivotColumns.add(new BeeColumn(ValueType.DECIMAL, aliasEndQuantity,
              quantityPrecision, quantityScale));

          if (hasEmptyValue) {
            quantityColumns.add(aliasForEmptyValue(aliasStartQuantity));
            quantityColumns.add(aliasForEmptyValue(aliasEndQuantity));
          }

          if (!stockColumnGroupLabels.isEmpty()) {
            for (int i = 0; i < stockColumnGroupLabels.size(); i++) {
              quantityColumns.add(aliasForGroupValue(aliasStartQuantity, i));
              quantityColumns.add(aliasForGroupValue(aliasEndQuantity, i));
            }
          }

        } else {
          pivotColumns.add(new BeeColumn(ValueType.DECIMAL, aliasQuantity,
              quantityPrecision, quantityScale));

          if (hasEmptyValue) {
            quantityColumns.add(aliasForEmptyValue(aliasQuantity));
          }

          if (!stockColumnGroupLabels.isEmpty()) {
            for (int i = 0; i < stockColumnGroupLabels.size(); i++) {
              quantityColumns.add(aliasForGroupValue(aliasQuantity, i));
            }
          }
        }
      }

      if (showAmount) {
        if (movement) {
          pivotColumns.add(new BeeColumn(ValueType.DECIMAL, aliasStartAmount,
              amountPrecision, amountScale));
          pivotColumns.add(new BeeColumn(ValueType.DECIMAL, aliasEndAmount,
              amountPrecision, amountScale));

          if (hasEmptyValue) {
            amountColumns.add(aliasForEmptyValue(aliasStartAmount));
            amountColumns.add(aliasForEmptyValue(aliasEndAmount));
          }

          if (!stockColumnGroupLabels.isEmpty()) {
            for (int i = 0; i < stockColumnGroupLabels.size(); i++) {
              amountColumns.add(aliasForGroupValue(aliasStartAmount, i));
              amountColumns.add(aliasForGroupValue(aliasEndAmount, i));
            }
          }

        } else {
          pivotColumns.add(new BeeColumn(ValueType.DECIMAL, aliasAmount,
              amountPrecision, amountScale));

          if (hasEmptyValue) {
            amountColumns.add(aliasForEmptyValue(aliasAmount));
          }

          if (!stockColumnGroupLabels.isEmpty()) {
            for (int i = 0; i < stockColumnGroupLabels.size(); i++) {
              amountColumns.add(aliasForGroupValue(aliasAmount, i));
            }
          }
        }
      }

      ResponseObject response = pivot(tmp, valueColumn, labelToValue, hasEmptyValue, pivotColumns);
      qs.sqlDropTemp(tmp);

      if (response.hasErrors()) {
        return response;
      }

      tmp = response.getResponseAsString();
      qs.sqlIndex(tmp, COL_TRADE_DOCUMENT_ITEM);
    }

    if (!rowGroups.isEmpty()) {
      for (TradeReportGroup group : rowGroups) {
        qs.sqlIndex(tmp, groupValueAliases.get(group));
      }

      ResponseObject response = addRowGroupLabels(tmp, rowGroups, groupValueAliases, needsYear);
      qs.sqlDropTemp(tmp);

      if (response.hasErrors()) {
        return response;
      }

      tmp = response.getResponseAsString();
      qs.sqlIndex(tmp, COL_TRADE_DOCUMENT_ITEM);

      for (TradeReportGroup group : rowGroups) {
        rowGroupValueColumns.add(groupValueAliases.get(group));
        rowGroupLabelColumns.add(group.getLabelAlias());
      }
    }

    if (movement) {
      ResponseObject response = addMovement(tmp, startDate, endDate, movementGroups,
          showQuantity, quantityPrecision, quantityScale,
          showAmount, amountPrecision, amountScale, aliasPrice);

      if (response.hasErrors()) {
        qs.sqlDropTemp(tmp);
        return response;
      }

      if (response.hasResponse()) {
        Map<String, String> map = response.getResponseAsStringMap();

        if (map.containsKey(Service.VAR_TABLE)) {
          qs.sqlDropTemp(tmp);
          tmp = map.get(Service.VAR_TABLE);
        }

        String value = map.get(RP_MOVEMENT_IN_COLUMNS);
        if (!BeeUtils.isEmpty(value)) {
          movementInColumns.addAll(TradeMovementColumn.restoreList(value));
        }

        value = map.get(RP_MOVEMENT_OUT_COLUMNS);
        if (!BeeUtils.isEmpty(value)) {
          movementOutColumns.addAll(TradeMovementColumn.restoreList(value));
        }

        if (showQuantity) {
          movementInColumns.forEach(c -> quantityColumns.add(c.getQuantityColumn()));
          movementOutColumns.forEach(c -> quantityColumns.add(c.getQuantityColumn()));
        }
        if (showAmount) {
          movementInColumns.forEach(c -> amountColumns.add(c.getAmountColumn()));
          movementOutColumns.forEach(c -> amountColumns.add(c.getAmountColumn()));
        }
      }
    }

    if (movement) {
      List<String> qcs = new ArrayList<>(quantityColumns);
      List<String> acs = new ArrayList<>(amountColumns);

      int size = Math.max(qcs.size(), acs.size());

      quantityColumns.clear();
      amountColumns.clear();

      int startIndex = -1;
      int endIndex = -1;

      boolean ok;

      for (int i = 0; i < size; i++) {
        String qc = showQuantity ? qcs.get(i) : null;
        String ac = showAmount ? acs.get(i) : null;

        String column = showQuantity ? qc : ac;

        boolean start = column.startsWith(PREFIX_START_STOCK);
        boolean end = !start && column.startsWith(PREFIX_END_STOCK);

        if (start || end) {
          ok = qs.sqlExists(tmp, SqlUtils.nonZero(tmp, column));

          if (!stockColumnGroupLabels.isEmpty() && !column.endsWith(EMPTY_VALUE_SUFFIX)) {
            if (start) {
              startIndex++;

              if (ok) {
                if (BeeUtils.isIndex(stockColumnGroupLabels, startIndex)) {
                  stockStartLabels.add(stockColumnGroupLabels.get(startIndex));
                }
                if (BeeUtils.isIndex(stockColumnGroupValues, startIndex)) {
                  stockStartValues.add(stockColumnGroupValues.get(startIndex));
                }
              }

            } else if (end) {
              endIndex++;

              if (ok) {
                if (BeeUtils.isIndex(stockColumnGroupLabels, endIndex)) {
                  stockEndLabels.add(stockColumnGroupLabels.get(endIndex));
                }
                if (BeeUtils.isIndex(stockColumnGroupValues, endIndex)) {
                  stockEndValues.add(stockColumnGroupValues.get(endIndex));
                }
              }
            }
          }

        } else {
          ok = true;
        }

        if (ok) {
          if (showQuantity) {
            quantityColumns.add(qc);
          }
          if (showAmount) {
            amountColumns.add(ac);
          }
        }
      }

      if (showQuantity && quantityColumns.isEmpty() || showAmount && amountColumns.isEmpty()) {
        qs.sqlDropTemp(tmp);
        return ResponseObject.emptyResponse();
      }

    } else if (stockGroup != null) {
      stockEndLabels.addAll(stockColumnGroupLabels);
      stockEndValues.addAll(stockColumnGroupValues);
    }

    boolean showPrice = rowGroups.contains(TradeReportGroup.ITEM) && showAmount && !summary;

    SqlSelect select = new SqlSelect().addFrom(tmp);

    if (rowGroups.isEmpty()) {
      select.addSum(tmp, quantityColumns).addSum(tmp, amountColumns);

    } else {
      select.addFields(tmp, rowGroupValueColumns).addFields(tmp, rowGroupLabelColumns)
          .addSum(tmp, quantityColumns).addSum(tmp, amountColumns)
          .addGroup(tmp, rowGroupValueColumns).addGroup(tmp, rowGroupLabelColumns);

      for (int i = 0; i < rowGroups.size(); i++) {
        select.addOrder(tmp, rowGroupLabelColumns.get(i), NullOrdering.NULLS_FIRST);
        select.addOrder(tmp, rowGroupValueColumns.get(i), NullOrdering.NULLS_FIRST);
      }

      if (showPrice) {
        select.addFields(tmp, aliasPrice).addGroup(tmp, aliasPrice).addOrder(tmp, aliasPrice);
      }

      if (movement) {
        HasConditions having = SqlUtils.or();

        for (String column : showQuantity ? quantityColumns : amountColumns) {
          having.add(SqlUtils.nonZero(SqlUtils.aggregate(SqlConstants.SqlFunction.SUM,
              SqlUtils.field(tmp, column))));
        }

        select.setHaving(having);
      }
    }

    SimpleRowSet data = qs.getData(select);
    qs.sqlDropTemp(tmp);

    if (DataUtils.isEmpty(data)) {
      return ResponseObject.emptyResponse();
    }

    Map<String, String> result = new HashMap<>();
    result.put(Service.VAR_REPORT_PARAMETERS, parameters.serialize());
    result.put(Service.VAR_DATA, data.serialize());

    if (!rowGroups.isEmpty()) {
      result.put(RP_ROW_GROUPS, EnumUtils.joinIndexes(rowGroups));

      result.put(RP_ROW_GROUP_VALUE_COLUMNS, NameUtils.join(rowGroupValueColumns));
      result.put(RP_ROW_GROUP_LABEL_COLUMNS, NameUtils.join(rowGroupLabelColumns));
    }

    if (stockGroup != null) {
      result.put(RP_STOCK_COLUMN_GROUPS, BeeUtils.toString(stockGroup.ordinal()));

      if (!stockStartLabels.isEmpty()) {
        result.put(RP_STOCK_START_COLUMN_LABELS, Codec.beeSerialize(stockStartLabels));
      }
      if (!stockStartValues.isEmpty()) {
        result.put(RP_STOCK_START_COLUMN_VALUES, Codec.beeSerialize(stockStartValues));
      }

      if (!stockEndLabels.isEmpty()) {
        result.put(RP_STOCK_END_COLUMN_LABELS, Codec.beeSerialize(stockEndLabels));
      }
      if (!stockEndValues.isEmpty()) {
        result.put(RP_STOCK_END_COLUMN_VALUES, Codec.beeSerialize(stockEndValues));
      }
    }

    if (movement) {
      if (!movementGroups.isEmpty()) {
        result.put(RP_MOVEMENT_COLUMN_GROUPS, EnumUtils.joinIndexes(movementGroups));
      }

      if (!movementInColumns.isEmpty()) {
        result.put(RP_MOVEMENT_IN_COLUMNS, Codec.beeSerialize(movementInColumns));
      }
      if (!movementOutColumns.isEmpty()) {
        result.put(RP_MOVEMENT_OUT_COLUMNS, Codec.beeSerialize(movementOutColumns));
      }
    }

    if (!quantityColumns.isEmpty()) {
      result.put(RP_QUANTITY_COLUMNS, NameUtils.join(quantityColumns));
    }
    if (!amountColumns.isEmpty()) {
      result.put(RP_AMOUNT_COLUMNS, NameUtils.join(amountColumns));
    }

    if (showPrice) {
      result.put(RP_PRICE_COLUMN, aliasPrice);
    }

    return ResponseObject.response(result);
  }

  private static IsCondition getItemTypeCondition(String alias, Collection<Long> itemTypes) {
    return BeeUtils.isEmpty(itemTypes) ? null : SqlUtils.inList(alias, COL_ITEM_TYPE, itemTypes);
  }

  private static IsCondition getItemGroupCondition(String alias, Collection<Long> itemGroups) {
    return BeeUtils.isEmpty(itemGroups) ? null : SqlUtils.inList(alias, COL_ITEM_GROUP, itemGroups);
  }

  private static IsCondition getPrimaryDocumentItemCondition(String alias,
      Collection<Long> items, Collection<Long> categories, Collection<Long> manufacturers) {

    HasConditions conditions = SqlUtils.and();

    if (!BeeUtils.isEmpty(items)) {
      conditions.add(SqlUtils.inList(alias, COL_ITEM, items));
    }
    if (!BeeUtils.isEmpty(categories)) {
      conditions.add(SqlUtils.in(alias, COL_ITEM, TBL_ITEM_CATEGORIES, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_CATEGORIES, COL_CATEGORY, categories)));
    }
    if (!BeeUtils.isEmpty(manufacturers)) {
      conditions.add(SqlUtils.in(alias, COL_ITEM, TBL_ITEM_MANUFACTURERS, COL_ITEM,
          SqlUtils.inList(TBL_ITEM_MANUFACTURERS, COL_ITEM_MANUFACTURER, manufacturers)));
    }

    return normalize(conditions);
  }

  private static IsCondition getPrimaryDocumentCondition(String alias, Collection<Long> suppliers,
      DateTime receivedFrom, DateTime receivedTo) {

    HasConditions conditions = SqlUtils.and();

    if (!BeeUtils.isEmpty(suppliers)) {
      conditions.add(SqlUtils.inList(alias, COL_TRADE_SUPPLIER, suppliers));
    }

    if (receivedFrom != null) {
      conditions.add(SqlUtils.moreEqual(alias, COL_TRADE_DATE, receivedFrom));
    }
    if (receivedTo != null) {
      conditions.add(SqlUtils.less(alias, COL_TRADE_DATE, receivedTo));
    }

    return normalize(conditions);
  }

  private static IsCondition getDocumentItemCondition(String alias, Collection<Long> documents) {
    return BeeUtils.isEmpty(documents)
        ? null : SqlUtils.inList(alias, COL_TRADE_DOCUMENT, documents);
  }

  private static IsCondition getStockCondition(String alias, Collection<Long> warehouses) {
    return BeeUtils.isEmpty(warehouses)
        ? null : SqlUtils.inList(alias, COL_STOCK_WAREHOUSE, warehouses);
  }

  private static IsCondition normalize(HasConditions conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return null;
    } else if (conditions.size() == 1) {
      return conditions.peek();
    } else {
      return conditions;
    }
  }

  private IsCondition parseItemFilter(String input) {
    if (BeeUtils.isEmpty(input)) {
      return null;

    } else {
      BeeView view = sys.getView(VIEW_ITEMS);
      Long userId = usr.getCurrentUserId();

      Filter filter = view.parseFilter(input, userId);
      return (filter == null) ? null : view.getCondition(filter);
    }
  }

  private static IsExpression zero(int precision, int scale) {
    return SqlUtils.cast(SqlUtils.constant(0), SqlConstants.SqlDataType.DECIMAL, precision, scale);
  }

  private ResponseObject calculateStock(String tbl, String fld, DateTime date) {
    IsCondition dateCondition = (date == null)
        ? null : SqlUtils.less(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE, date);

    IsCondition producerCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        OperationType.getStockProducers());
    IsCondition consumerCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        OperationType.getStockConsumers());

    IsCondition phaseCondition = SqlUtils.inList(TBL_TRADE_DOCUMENTS, COL_TRADE_DOCUMENT_PHASE,
        TradeDocumentPhase.getStockPhases());

    SqlSelect producerQuery = new SqlSelect()
        .addFields(tbl, COL_TRADE_DOCUMENT_ITEM)
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(tbl)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            tbl, COL_TRADE_DOCUMENT_ITEM))
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(SqlUtils.and(dateCondition, producerCondition, phaseCondition));

    String producers = qs.sqlCreateTemp(producerQuery);

    if (qs.isEmpty(producers)) {
      qs.sqlDropTemp(producers);

    } else {
      qs.sqlIndex(producers, COL_TRADE_DOCUMENT_ITEM);

      SqlUpdate update = new SqlUpdate(tbl)
          .setFrom(producers, SqlUtils.join(producers, COL_TRADE_DOCUMENT_ITEM,
              tbl, COL_TRADE_DOCUMENT_ITEM))
          .addExpression(fld, SqlUtils.field(producers, COL_TRADE_ITEM_QUANTITY));

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      qs.sqlDropTemp(producers);

      if (updResponse.hasErrors()) {
        return updResponse;
      }
    }

    SqlSelect consumerQuery = new SqlSelect()
        .addFields(tbl, COL_TRADE_DOCUMENT_ITEM)
        .addSum(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(tbl)
        .addFromInner(TBL_TRADE_DOCUMENT_ITEMS, SqlUtils.join(tbl, COL_TRADE_DOCUMENT_ITEM,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT))
        .addFromInner(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromInner(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(SqlUtils.and(dateCondition, consumerCondition, phaseCondition))
        .addGroup(tbl, COL_TRADE_DOCUMENT_ITEM);

    String consumers = qs.sqlCreateTemp(consumerQuery);

    if (qs.isEmpty(consumers)) {
      qs.sqlDropTemp(consumers);

    } else {
      qs.sqlIndex(consumers, COL_TRADE_DOCUMENT_ITEM);

      SqlUpdate update = new SqlUpdate(tbl)
          .setFrom(consumers, SqlUtils.join(consumers, COL_TRADE_DOCUMENT_ITEM,
              tbl, COL_TRADE_DOCUMENT_ITEM))
          .addExpression(fld,
              SqlUtils.minus(SqlUtils.field(tbl, fld),
                  SqlUtils.field(consumers, COL_TRADE_ITEM_QUANTITY)));

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      qs.sqlDropTemp(consumers);

      if (updResponse.hasErrors()) {
        return updResponse;
      }
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject addMovement(String tbl, DateTime startDate, DateTime endDate,
      List<TradeMovementGroup> movementGroups,
      boolean showQuantity, int quantityPrecision, int quantityScale,
      boolean showAmount, int amountPrecision, int amountScale, String fldPrice) {

    IsCondition dateCondition = SqlUtils.and(
        SqlUtils.moreEqual(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE, startDate),
        SqlUtils.less(TBL_TRADE_DOCUMENTS, COL_TRADE_DATE, endDate));

    IsCondition producerCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        OperationType.getStockProducers());
    IsCondition consumerCondition = SqlUtils.inList(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE,
        OperationType.getStockConsumers());

    IsCondition phaseCondition = SqlUtils.inList(TBL_TRADE_DOCUMENTS, COL_TRADE_DOCUMENT_PHASE,
        TradeDocumentPhase.getStockPhases());

    SqlSelect producerQuery = new SqlSelect()
        .addFields(tbl, COL_TRADE_DOCUMENT_ITEM)
        .addFields(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(tbl)
        .addFromLeft(TBL_TRADE_DOCUMENT_ITEMS, sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS,
            tbl, COL_TRADE_DOCUMENT_ITEM))
        .addFromLeft(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromLeft(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(SqlUtils.and(dateCondition, producerCondition, phaseCondition));

    SqlSelect consumerQuery = new SqlSelect()
        .addFields(tbl, COL_TRADE_DOCUMENT_ITEM)
        .addSum(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_QUANTITY)
        .addFrom(tbl)
        .addFromLeft(TBL_TRADE_DOCUMENT_ITEMS, SqlUtils.join(tbl, COL_TRADE_DOCUMENT_ITEM,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT))
        .addFromLeft(TBL_TRADE_DOCUMENTS, sys.joinTables(TBL_TRADE_DOCUMENTS,
            TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_DOCUMENT))
        .addFromLeft(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
            TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION))
        .setWhere(SqlUtils.and(dateCondition, consumerCondition, phaseCondition,
            SqlUtils.notNull(TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT)))
        .addGroup(tbl, COL_TRADE_DOCUMENT_ITEM);

    for (TradeMovementGroup group : movementGroups) {
      switch (group) {
        case OPERATION_TYPE:
          producerQuery.addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE);

          consumerQuery.addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE);
          consumerQuery.addGroup(TBL_TRADE_OPERATIONS, COL_OPERATION_TYPE);
          break;

        case OPERATION:
          producerQuery.addFields(TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION);

          consumerQuery.addFields(TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION);
          consumerQuery.addGroup(TBL_TRADE_DOCUMENTS, COL_TRADE_OPERATION);
          break;

        case WAREHOUSE:
          String aliasStockFrom = SqlUtils.uniqueName("from");
          String aliasStockTo = SqlUtils.uniqueName("to");

          producerQuery.addField(aliasStockFrom, COL_STOCK_WAREHOUSE, COL_TRADE_WAREHOUSE_FROM);
          producerQuery.addFromLeft(TBL_TRADE_STOCK, aliasStockFrom,
              SqlUtils.join(aliasStockFrom, COL_TRADE_DOCUMENT_ITEM,
                  TBL_TRADE_DOCUMENT_ITEMS, COL_TRADE_ITEM_PARENT));

          producerQuery.addField(aliasStockTo, COL_STOCK_WAREHOUSE, COL_TRADE_WAREHOUSE_TO);
          producerQuery.addFromLeft(TBL_TRADE_STOCK, aliasStockTo,
              SqlUtils.join(aliasStockTo, COL_TRADE_DOCUMENT_ITEM, tbl, COL_TRADE_DOCUMENT_ITEM));

          consumerQuery.addField(aliasStockFrom, COL_STOCK_WAREHOUSE, COL_TRADE_WAREHOUSE_FROM);
          consumerQuery.addFromLeft(TBL_TRADE_STOCK, aliasStockFrom,
              SqlUtils.join(aliasStockFrom, COL_TRADE_DOCUMENT_ITEM, tbl, COL_TRADE_DOCUMENT_ITEM));
          consumerQuery.addGroup(aliasStockFrom, COL_STOCK_WAREHOUSE);

          consumerQuery.addField(aliasStockTo, COL_STOCK_WAREHOUSE, COL_TRADE_WAREHOUSE_TO);
          consumerQuery.addFromLeft(TBL_TRADE_STOCK, aliasStockTo,
              sys.joinTables(TBL_TRADE_DOCUMENT_ITEMS, aliasStockTo, COL_TRADE_DOCUMENT_ITEM));
          consumerQuery.addGroup(aliasStockTo, COL_STOCK_WAREHOUSE);
          break;
      }
    }

    String producers = qs.sqlCreateTemp(producerQuery);
    String consumers = qs.sqlCreateTemp(consumerQuery);

    List<TradeMovementColumn> inColumns = new ArrayList<>();
    List<TradeMovementColumn> outColumns = new ArrayList<>();

    if (!qs.isEmpty(producers)) {
      inColumns.addAll(getMovementColumns(producers, PREFIX_MOVEMENT_IN, movementGroups));
    }
    if (!qs.isEmpty(consumers)) {
      outColumns.addAll(getMovementColumns(consumers, PREFIX_MOVEMENT_OUT, movementGroups));
    }

    if (inColumns.isEmpty() && outColumns.isEmpty()) {
      qs.sqlDropTemp(producers);
      qs.sqlDropTemp(consumers);

      return ResponseObject.emptyResponse();
    }

    SqlSelect query = new SqlSelect().addAllFields(tbl).addFrom(tbl);

    Stream.concat(inColumns.stream(), outColumns.stream()).forEach(column -> {
      if (showQuantity) {
        query.addExpr(zero(quantityPrecision, quantityScale), column.getQuantityColumn());
      }
      if (showAmount) {
        query.addExpr(zero(amountPrecision, amountScale), column.getAmountColumn());
      }
    });

    String tmp = qs.sqlCreateTemp(query);
    qs.sqlIndex(tmp, COL_TRADE_DOCUMENT_ITEM);

    if (!inColumns.isEmpty()) {
      indexMovement(producers, movementGroups);

      ResponseObject response = updateMovement(tmp, producers, inColumns, movementGroups,
          showQuantity, showAmount, fldPrice);

      if (response.hasErrors()) {
        qs.sqlDropTemp(producers);
        qs.sqlDropTemp(consumers);

        qs.sqlDropTemp(tmp);
        return response;
      }
    }

    if (!outColumns.isEmpty()) {
      indexMovement(consumers, movementGroups);

      ResponseObject response = updateMovement(tmp, consumers, outColumns, movementGroups,
          showQuantity, showAmount, fldPrice);

      if (response.hasErrors()) {
        qs.sqlDropTemp(producers);
        qs.sqlDropTemp(consumers);

        qs.sqlDropTemp(tmp);
        return response;
      }
    }

    qs.sqlDropTemp(producers);
    qs.sqlDropTemp(consumers);

    Map<String, String> result = new HashMap<>();
    result.put(Service.VAR_TABLE, tmp);

    if (!inColumns.isEmpty()) {
      result.put(RP_MOVEMENT_IN_COLUMNS, Codec.beeSerialize(inColumns));
    }
    if (!outColumns.isEmpty()) {
      result.put(RP_MOVEMENT_OUT_COLUMNS, Codec.beeSerialize(outColumns));
    }

    return ResponseObject.response(result);
  }

  private ResponseObject updateMovement(String dst, String src, List<TradeMovementColumn> columns,
      List<TradeMovementGroup> groups, boolean updateQuantity, boolean updateAmount,
      String fldPrice) {

    IsCondition join = SqlUtils.join(src, COL_TRADE_DOCUMENT_ITEM, dst, COL_TRADE_DOCUMENT_ITEM);

    for (TradeMovementColumn column : columns) {
      HasConditions where = SqlUtils.and();

      if (column.getOperationType() != null) {
        where.add(SqlUtils.equals(src, COL_OPERATION_TYPE, column.getOperationType()));
      }
      if (DataUtils.isId(column.getOperation())) {
        where.add(SqlUtils.equals(src, COL_TRADE_OPERATION, column.getOperation()));
      }

      if (groups.contains(TradeMovementGroup.WAREHOUSE)) {
        where.add(SqlUtils.equals(src, COL_TRADE_WAREHOUSE_FROM, column.getWarehouseFrom()));
        where.add(SqlUtils.equals(src, COL_TRADE_WAREHOUSE_TO, column.getWarehouseTo()));
      }

      if (updateQuantity) {
        SqlUpdate update = new SqlUpdate(dst)
            .setFrom(src, join)
            .setWhere(where)
            .addExpression(column.getQuantityColumn(),
                SqlUtils.field(src, COL_TRADE_ITEM_QUANTITY));

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          return response;
        }
      }

      if (updateAmount) {
        SqlUpdate update = new SqlUpdate(dst)
            .setFrom(src, join)
            .setWhere(SqlUtils.and(where, SqlUtils.nonZero(dst, fldPrice)))
            .addExpression(column.getAmountColumn(),
                SqlUtils.multiply(SqlUtils.field(src, COL_TRADE_ITEM_QUANTITY),
                    SqlUtils.field(dst, fldPrice)));

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          return response;
        }
      }
    }

    return ResponseObject.emptyResponse();
  }

  private void indexMovement(String tbl, List<TradeMovementGroup> groups) {
    qs.sqlIndex(tbl, COL_TRADE_DOCUMENT_ITEM);

    for (TradeMovementGroup group : groups) {
      switch (group) {
        case OPERATION_TYPE:
          qs.sqlIndex(tbl, COL_OPERATION_TYPE);
          break;

        case OPERATION:
          qs.sqlIndex(tbl, COL_TRADE_OPERATION);
          break;

        case WAREHOUSE:
          qs.sqlIndex(tbl, COL_TRADE_WAREHOUSE_FROM);
          qs.sqlIndex(tbl, COL_TRADE_WAREHOUSE_TO);
          break;
      }
    }
  }

  private List<TradeMovementColumn> getMovementColumns(String src, String prefix,
      List<TradeMovementGroup> groups) {

    List<TradeMovementColumn> columns = new ArrayList<>();

    if (groups.isEmpty()) {
      columns.add(new TradeMovementColumn(prefix + BeeConst.STRING_ZERO));

    } else {
      SqlSelect query = new SqlSelect().setDistinctMode(true).addFrom(src);

      for (TradeMovementGroup group : groups) {
        switch (group) {
          case OPERATION_TYPE:
            query.addFields(src, COL_OPERATION_TYPE)
                .addOrder(src, COL_OPERATION_TYPE);
            break;

          case OPERATION:
            query.addFields(TBL_TRADE_OPERATIONS, COL_OPERATION_ORDINAL, COL_OPERATION_NAME)
                .addFields(src, COL_TRADE_OPERATION)
                .addFromLeft(TBL_TRADE_OPERATIONS, sys.joinTables(TBL_TRADE_OPERATIONS,
                    src, COL_TRADE_OPERATION))
                .addOrder(TBL_TRADE_OPERATIONS, COL_OPERATION_ORDINAL, COL_OPERATION_NAME);
            break;

          case WAREHOUSE:
            String aliasFrom = SqlUtils.uniqueName("from");
            String aliasTo = SqlUtils.uniqueName("to");

            String idName = sys.getIdName(TBL_WAREHOUSES);

            query.addField(aliasFrom, COL_WAREHOUSE_CODE, ALS_WAREHOUSE_FROM_CODE)
                .addField(aliasTo, COL_WAREHOUSE_CODE, ALS_WAREHOUSE_TO_CODE)
                .addFields(src, COL_TRADE_WAREHOUSE_FROM, COL_TRADE_WAREHOUSE_TO)
                .addFromLeft(TBL_WAREHOUSES, aliasFrom,
                    SqlUtils.join(aliasFrom, idName, src, COL_TRADE_WAREHOUSE_FROM))
                .addFromLeft(TBL_WAREHOUSES, aliasTo,
                    SqlUtils.join(aliasTo, idName, src, COL_TRADE_WAREHOUSE_TO))
                .addOrder(aliasFrom, COL_WAREHOUSE_CODE)
                .addOrder(aliasTo, COL_WAREHOUSE_CODE);
            break;
        }
      }

      SimpleRowSet data = qs.getData(query);

      int index = 1;
      for (SimpleRowSet.SimpleRow row : data) {
        TradeMovementColumn column = new TradeMovementColumn(prefix + Integer.toString(index));

        if (groups.contains(TradeMovementGroup.OPERATION_TYPE)) {
          column.setOperationType(row.getEnum(COL_OPERATION_TYPE, OperationType.class));
        }

        if (groups.contains(TradeMovementGroup.OPERATION)) {
          column.setOperation(row.getLong(COL_TRADE_OPERATION));
          column.setOperationName(row.getValue(COL_OPERATION_NAME));
        }

        if (groups.contains(TradeMovementGroup.WAREHOUSE)) {
          column.setWarehouseFrom(row.getLong(COL_TRADE_WAREHOUSE_FROM));
          column.setWarehouseFromCode(row.getValue(ALS_WAREHOUSE_FROM_CODE));

          column.setWarehouseTo(row.getLong(COL_TRADE_WAREHOUSE_TO));
          column.setWarehouseToCode(row.getValue(ALS_WAREHOUSE_TO_CODE));
        }

        columns.add(column);
        index++;
      }
    }

    return columns;
  }

  private ResponseObject maybeExchange(String tbl, String fldPrice, String fldCurrency,
      DateTime date, Long currency) {

    if (DataUtils.isId(currency)) {
      SqlSelect currencyQuery = new SqlSelect().setDistinctMode(true)
          .addFields(tbl, fldCurrency)
          .addFrom(tbl)
          .setWhere(SqlUtils.and(SqlUtils.notNull(tbl, fldCurrency),
              SqlUtils.notEqual(tbl, fldCurrency, currency)));

      Set<Long> currencies = qs.getLongSet(currencyQuery);

      if (!BeeUtils.isEmpty(currencies)) {
        long time = (date == null) ? System.currentTimeMillis() : date.getTime();
        double toRate = adm.getRate(currency, time);

        for (Long from : currencies) {
          double fromRate = adm.getRate(from, time);

          if (Double.compare(toRate, fromRate) != BeeConst.COMPARE_EQUAL) {
            double rate = fromRate / toRate;

            SqlUpdate exchange = new SqlUpdate(tbl)
                .addExpression(fldPrice, SqlUtils.multiply(SqlUtils.field(tbl, fldPrice), rate))
                .setWhere(SqlUtils.and(SqlUtils.equals(tbl, fldCurrency, from),
                    SqlUtils.notNull(tbl, fldPrice)));

            ResponseObject response = qs.updateDataWithResponse(exchange);
            if (response.hasErrors()) {
              return response;
            }
          }
        }
      }
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject calculateAmount(String tbl, String fldQuantity, String fldPrice,
      String fldAmount) {

    SqlUpdate update = new SqlUpdate(tbl)
        .addExpression(fldAmount, SqlUtils.multiply(SqlUtils.field(tbl, fldQuantity),
            SqlUtils.field(tbl, fldPrice)))
        .setWhere(SqlUtils.notNull(tbl, fldQuantity, fldPrice));

    ResponseObject response = qs.updateDataWithResponse(update);
    if (response.hasErrors()) {
      return response;
    }

    return ResponseObject.emptyResponse();
  }

  private Multimap<String, Object> getGroupLabels(TradeReportGroup group, String tbl, String fld,
      boolean hasYear) {

    Multimap<String, Object> result = ArrayListMultimap.create();

    switch (group.getType()) {
      case DATE_TIME:
        if (group == TradeReportGroup.YEAR_RECEIVED) {
          Set<Integer> years = qs.getDistinctInts(tbl, fld, SqlUtils.notNull(tbl, fld));
          years.forEach(y -> result.put(TimeUtils.yearToString(y), y));

        } else if (group == TradeReportGroup.MONTH_RECEIVED) {
          Set<Integer> months = qs.getDistinctInts(tbl, fld, SqlUtils.notNull(tbl, fld));
          months.forEach(m -> {
            String label = hasYear ? TimeUtils.monthToString(m) : Integer.toString(m);
            result.put(label, m);
          });
        }
        break;

      case LONG:
        if (BeeUtils.allNotEmpty(group.labelSource(), group.labelColumn())) {
          SqlSelect query = new SqlSelect().setDistinctMode(true)
              .addFields(tbl, fld)
              .addFields(group.labelSource(), group.labelColumn())
              .addFrom(tbl)
              .addFromInner(group.labelSource(), sys.joinTables(group.labelSource(), tbl, fld));

          SimpleRowSet data = qs.getData(query);
          if (!DataUtils.isEmpty(data)) {
            data.forEach(row -> result.put(row.getValue(1), row.getLong(0)));
          }
        }
        break;

      default:
        Set<String> values = qs.getDistinctValues(tbl, fld, SqlUtils.notNull(tbl, fld));
        values.forEach(v -> result.put(v, v));
    }

    return result;
  }

  private ResponseObject pivot(String tbl, String fld,
      Multimap<String, Object> labelToValue, boolean hasEmptyValue, List<BeeColumn> pivotColumns) {

    List<String> labels = new ArrayList<>(labelToValue.keySet());
    labels.sort(null);

    SqlSelect query = new SqlSelect()
        .addAllFields(tbl)
        .addFrom(tbl);

    if (hasEmptyValue) {
      pivotColumns.forEach(column -> query.addEmptyNumeric(aliasForEmptyValue(column.getId()),
          column.getPrecision(), column.getScale()));
    }

    if (!labels.isEmpty()) {
      for (int i = 0; i < labels.size(); i++) {
        for (BeeColumn column : pivotColumns) {
          query.addEmptyNumeric(aliasForGroupValue(column.getId(), i),
              column.getPrecision(), column.getScale());
        }
      }
    }

    String tmp = qs.sqlCreateTemp(query);

    if (hasEmptyValue) {
      SqlUpdate update = new SqlUpdate(tmp);

      pivotColumns.forEach(column -> update.addExpression(aliasForEmptyValue(column.getId()),
          SqlUtils.field(tmp, column.getId())));

      update.setWhere(SqlUtils.isNull(tmp, fld));

      ResponseObject response = qs.updateDataWithResponse(update);
      if (response.hasErrors()) {
        qs.sqlDropTemp(tmp);
        return response;
      }
    }

    if (!labels.isEmpty()) {
      for (int i = 0; i < labels.size(); i++) {
        SqlUpdate update = new SqlUpdate(tmp);

        for (BeeColumn column : pivotColumns) {
          update.addExpression(aliasForGroupValue(column.getId(), i),
              SqlUtils.field(tmp, column.getId()));
        }

        update.setWhere(SqlUtils.inList(tmp, fld, labelToValue.get(labels.get(i))));

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          qs.sqlDropTemp(tmp);
          return response;
        }
      }
    }

    return ResponseObject.response(tmp);
  }

  private static String aliasForEmptyValue(String prefix) {
    return prefix + EMPTY_VALUE_SUFFIX;
  }

  private static String aliasForGroupValue(String prefix, int index) {
    return prefix + "_" + Integer.toString(index + 1);
  }

  private ResponseObject addRowGroupLabels(String tbl, Collection<TradeReportGroup> groups,
      Map<TradeReportGroup, String> valueColumns, boolean hasYear) {

    Map<TradeReportGroup, Multimap<String, Object>> groupLabels = new LinkedHashMap<>();

    groups.forEach(group -> groupLabels.put(group,
        getGroupLabels(group, tbl, valueColumns.get(group), hasYear)));

    SqlSelect query = new SqlSelect()
        .addAllFields(tbl)
        .addFrom(tbl);

    for (TradeReportGroup group : groups) {
      Multimap<String, Object> labels = groupLabels.get(group);

      int precision;
      if (labels.isEmpty()) {
        precision = 1;
      } else {
        precision = labels.keySet().stream().mapToInt(String::length).max().getAsInt();
      }

      query.addEmptyString(group.getLabelAlias(), precision);
    }

    String tmp = qs.sqlCreateTemp(query);

    for (TradeReportGroup group : groups) {
      String valueColumn = valueColumns.get(group);
      String labelColumn = group.getLabelAlias();

      Multimap<String, Object> labels = groupLabels.get(group);

      if (group.getType() == ValueType.TEXT) {
        SqlUpdate update = new SqlUpdate(tmp)
            .addExpression(labelColumn, SqlUtils.field(tmp, valueColumn));

        ResponseObject response = qs.updateDataWithResponse(update);
        if (response.hasErrors()) {
          qs.sqlDropTemp(tmp);
          return response;
        }

      } else if (!labels.isEmpty()) {
        for (String label : labels.keySet()) {
          SqlUpdate update = new SqlUpdate(tmp)
              .addConstant(labelColumn, label)
              .setWhere(SqlUtils.inList(tmp, valueColumn, labels.get(label)));

          ResponseObject response = qs.updateDataWithResponse(update);
          if (response.hasErrors()) {
            qs.sqlDropTemp(tmp);
            return response;
          }
        }
      }
    }

    return ResponseObject.response(tmp);
  }
}
