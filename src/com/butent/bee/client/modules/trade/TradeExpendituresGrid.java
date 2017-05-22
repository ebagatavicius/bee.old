package com.butent.bee.client.modules.trade;

import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.DataReceivedEvent;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasRowValue;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowToDouble;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.value.DecimalValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.trade.TradeDocumentPhase;
import com.butent.bee.shared.modules.trade.TradeVatMode;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradeExpendituresGrid extends AbstractGridInterceptor {

  private final class SumRenderer extends AbstractCellRenderer implements HasRowValue {

    private final RowToDouble valueFunction;

    private SumRenderer(RowToDouble valueFunction) {
      super(null);
      this.valueFunction = valueFunction;
    }

    @Override
    public boolean dependsOnSource(String source) {
      return BeeUtils.inList(source, COL_EXPENDITURE_TYPE, COL_EXPENDITURE_AMOUNT,
          COL_EXPENDITURE_VAT, COL_EXPENDITURE_VAT_IS_PERCENT);
    }

    @Override
    public Value getRowValue(IsRow row) {
      return DecimalValue.of(evaluate(row));
    }

    @Override
    public String render(IsRow row) {
      double x = evaluate(row);
      return (x == BeeConst.DOUBLE_ZERO) ? null : BeeUtils.toString(x);
    }

    private double evaluate(IsRow row) {
      if (row == null) {
        return BeeConst.DOUBLE_ZERO;
      } else {
        Double value = valueFunction.apply(row);
        return BeeUtils.isDouble(value) ? value : BeeConst.DOUBLE_ZERO;
      }
    }
  }

  private final class GenRenderer extends AbstractCellRenderer {

    private final ButtonElement button;
    private final DivElement link;

    private GenRenderer(CellSource cellSource) {
      super(cellSource);

      this.button = Document.get().createPushButtonElement();
      button.setInnerText(Localized.dictionary().trdGenerateDocument());
      button.addClassName(GEN_STYLE_BUILD);

      this.link = Document.get().createDivElement();
      link.addClassName(GEN_STYLE_LINK);
    }

    @Override
    public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
      Long value = getLong(row);

      if (DataUtils.isId(value)) {
        return new XCell(cellIndex, value, styleRef);
      } else {
        return null;
      }
    }

    @Override
    public String render(IsRow row) {
      Long value = getLong(row);

      if (DataUtils.isId(value)) {
        link.setInnerText(BeeUtils.toString(value));
        return link.getString();

      } else if (canGenerate(row)) {
        return button.getString();

      } else {
        return null;
      }
    }
  }

  private static final String GEN_STYLE_PREFIX =
      BeeConst.CSS_CLASS_PREFIX + "trade-expenditures-gen-";

  private static final String GEN_STYLE_BUILD = GEN_STYLE_PREFIX + "build";
  private static final String GEN_STYLE_LINK = GEN_STYLE_PREFIX + "link";

  TradeExpendituresGrid() {
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeExpendituresGrid();
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (COL_EXPENDITURE_GENERATED_DOCUMENT.equals(columnName)) {
      column.getCell().addClickHandler(event -> {
        Element target = EventUtils.getEventTargetElement(event);
        IsRow row = AbstractCell.getEventRow(event);

        if (target != null && row != null) {
          if (target.hasClassName(GEN_STYLE_BUILD) && canGenerate(row)) {
            tryGenerate(row);

          } else if (target.hasClassName(GEN_STYLE_LINK)) {
            Long genId = row.getLong(getDataIndex(COL_EXPENDITURE_GENERATED_DOCUMENT));
            if (DataUtils.isId(genId)) {
              RowEditor.open(VIEW_TRADE_DOCUMENTS, genId, Opener.MODAL);
            }
          }
        }
      });
    }

    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public AbstractCellRenderer getRenderer(String columnName, List<? extends IsColumn> dataColumns,
      ColumnDescription columnDescription, CellSource cellSource) {

    if (!BeeUtils.isEmpty(columnName)) {
      switch (columnName) {
        case "VatAmount":
          return new SumRenderer(this::getVatAmount);

        case "Total":
          return new SumRenderer(this::getTotal);

        case COL_EXPENDITURE_GENERATED_DOCUMENT:
          return new GenRenderer(cellSource);
      }
    }

    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }

  @Override
  public void onDataReceived(DataReceivedEvent event) {
    TradeUtils.configureCostCalculation(getGridView());

    super.onDataReceived(event);
  }

  private boolean canGenerate(IsRow row) {
    if (row == null) {
      return false;
    }
    if (!row.isNull(getDataIndex(COL_EXPENDITURE_GENERATED_DOCUMENT))) {
      return false;
    }

    if (row.isNull(getDataIndex(COL_EXPENDITURE_DATE))) {
      return false;
    }
    if (!isValid(getAmount(row))) {
      return false;
    }
    if (row.isNull(getDataIndex(COL_EXPENDITURE_CURRENCY))) {
      return false;
    }

    if (BeeUtils.isEmpty(row.getString(getDataIndex(COL_EXPENDITURE_NUMBER)))) {
      return false;
    }

    if (row.isNull(getDataIndex(COL_EXPENDITURE_TYPE_OPERATION))) {
      return false;
    }
    if (row.isNull(getDataIndex(COL_EXPENDITURE_TYPE_ITEM))) {
      return false;
    }

    return true;
  }

  private void tryGenerate(final IsRow row) {
    String typeName = row.getString(getDataIndex(COL_EXPENDITURE_TYPE_NAME));

    Global.confirm(typeName, Icon.QUESTION,
        Collections.singletonList(Localized.dictionary().trdGenerateDocument()),
        Localized.dictionary().actionCreate(), Localized.dictionary().cancel(), () -> {

          List<String> tdColumns = new ArrayList<>();
          List<String> tdValues = new ArrayList<>();

          tdColumns.add(COL_TRADE_DATE);
          tdValues.add(row.getString(getDataIndex(COL_EXPENDITURE_DATE)));

          tdColumns.add(COL_TRADE_CURRENCY);
          tdValues.add(row.getString(getDataIndex(COL_EXPENDITURE_CURRENCY)));

          tdColumns.add(COL_TRADE_OPERATION);
          tdValues.add(row.getString(getDataIndex(COL_EXPENDITURE_TYPE_OPERATION)));

          String series = row.getString(getDataIndex(COL_EXPENDITURE_SERIES));
          if (!BeeUtils.isEmpty(series)) {
            tdColumns.add(COL_TRADE_SERIES);
            tdValues.add(series.trim());
          }

          String number = row.getString(getDataIndex(COL_EXPENDITURE_NUMBER));
          if (!BeeUtils.isEmpty(number)) {
            tdColumns.add(COL_TRADE_NUMBER);
            tdValues.add(number.trim());
          }

          String warehouse = row.getString(getDataIndex(COL_EXPENDITURE_TYPE_WAREHOUSE));
          if (DataUtils.isId(warehouse)) {
            tdColumns.add(COL_TRADE_WAREHOUSE_TO);
            tdValues.add(warehouse);
          }

          String supplier = row.getString(getDataIndex(COL_EXPENDITURE_SUPPLIER));
          if (!DataUtils.isId(supplier)) {
            supplier = row.getString(getDataIndex(ALS_EXPENDITURE_TYPE_SUPPLIER));
          }
          if (DataUtils.isId(supplier)) {
            tdColumns.add(COL_TRADE_SUPPLIER);
            tdValues.add(supplier);
          }

          tdColumns.add(COL_TRADE_DOCUMENT_PHASE);
          tdValues.add(BeeUtils.toString(TradeDocumentPhase.PENDING.ordinal()));

          TradeVatMode vatMode = getVatMode(row);
          if (vatMode != null) {
            tdColumns.add(COL_TRADE_DOCUMENT_VAT_MODE);
            tdValues.add(BeeUtils.toString(vatMode.ordinal()));
          }

          Queries.insert(VIEW_TRADE_DOCUMENTS, Data.getColumns(VIEW_TRADE_DOCUMENTS, tdColumns),
              tdValues, null, new RowCallback() {
                @Override
                public void onSuccess(final BeeRow tdRow) {
                  Queries.updateCellAndFire(getViewName(), row.getId(), row.getVersion(),
                      COL_EXPENDITURE_GENERATED_DOCUMENT, null, BeeUtils.toString(tdRow.getId()));

                  RowInsertEvent.fire(BeeKeeper.getBus(), VIEW_TRADE_DOCUMENTS, tdRow, null);

                  List<String> itemColumns = new ArrayList<>();
                  List<String> itemValues = new ArrayList<>();

                  itemColumns.add(COL_TRADE_DOCUMENT);
                  itemValues.add(BeeUtils.toString(tdRow.getId()));

                  itemColumns.add(COL_ITEM);
                  itemValues.add(row.getString(getDataIndex(COL_EXPENDITURE_TYPE_ITEM)));

                  itemColumns.add(COL_TRADE_ITEM_QUANTITY);
                  itemValues.add(BeeConst.STRING_ONE);

                  itemColumns.add(COL_TRADE_ITEM_PRICE);
                  itemValues.add(row.getString(getDataIndex(COL_EXPENDITURE_AMOUNT)));

                  String vat = row.getString(getDataIndex(COL_EXPENDITURE_VAT));
                  if (!BeeUtils.isEmpty(vat)) {
                    itemColumns.add(COL_TRADE_DOCUMENT_ITEM_VAT);
                    itemValues.add(vat);
                  }

                  String vip = row.getString(getDataIndex(COL_EXPENDITURE_VAT_IS_PERCENT));
                  if (!BeeUtils.isEmpty(vip)) {
                    itemColumns.add(COL_TRADE_DOCUMENT_ITEM_VAT_IS_PERCENT);
                    itemValues.add(vip);
                  }

                  Queries.insert(VIEW_TRADE_DOCUMENT_ITEMS,
                      Data.getColumns(VIEW_TRADE_DOCUMENT_ITEMS, itemColumns), itemValues,
                      null, new RowCallback() {
                        @Override
                        public void onSuccess(BeeRow itemRow) {
                          RowInsertEvent.fire(BeeKeeper.getBus(), VIEW_TRADE_DOCUMENT_ITEMS,
                              itemRow, null);

                          RowEditor.open(VIEW_TRADE_DOCUMENTS, tdRow.getId(), Opener.MODAL);
                        }
                      });
                }
              });
        });
  }

  private Double getVatAmount(IsRow row) {
    TradeVatMode vatMode = getVatMode(row);
    if (vatMode == null) {
      return null;
    }

    Double amount = getAmount(row);
    if (!isValid(amount)) {
      return null;
    }

    Double vat = row.getDouble(getDataIndex(COL_EXPENDITURE_VAT));

    if (isValid(vat)) {
      if (row.isNull(getDataIndex(COL_EXPENDITURE_VAT_IS_PERCENT))) {
        return round(vat);
      } else {
        return round(vatMode.computePercent(amount, vat));
      }

    } else {
      return null;
    }
  }

  private Double getTotal(IsRow row) {
    Double amount = getAmount(row);
    if (!isValid(amount)) {
      return null;
    }

    TradeVatMode vatMode = getVatMode(row);
    if (vatMode == TradeVatMode.PLUS) {
      Double vat = getVatAmount(row);
      if (isValid(vat)) {
        return round(amount) + round(vat);
      }
    }

    return round(amount);
  }

  private Double getAmount(IsRow row) {
    return round(row.getDouble(getDataIndex(COL_EXPENDITURE_AMOUNT)));
  }

  private TradeVatMode getVatMode(IsRow row) {
    return EnumUtils.getEnumByIndex(TradeVatMode.class,
        row.getInteger(getDataIndex(COL_OPERATION_VAT_MODE)));
  }

  private static boolean isValid(Double x) {
    return BeeUtils.nonZero(x);
  }

  private static Double round(Double x) {
    return isValid(x) ? BeeUtils.round(x, 2) : null;
  }
}
