package com.butent.bee.client.modules.classifiers;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.orders.OrderForm;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.TreeGridInterceptor;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.orders.OrdersConstants.OrdersStatus;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ItemsGrid extends TreeGridInterceptor {

  private static final String STYLE_RES_ORDER_PREFIX = "bee-reserved-orders-";
  private static final String STYLE_RES_ORDER_TABLE = STYLE_RES_ORDER_PREFIX + "table";
  private static final String STYLE_RES_ORDER_HEADER = STYLE_RES_ORDER_PREFIX + "header";
  private static final String STYLE_RES_ORDER_INFO = STYLE_RES_ORDER_PREFIX + "info";
  private static final String STYLE_LABEL_SUFFIX = "label";
  private static final String STYLE_CELL_SUFFIX = "cell";

  private static final String STYLE_RES_ORDER_ID_PREFIX = STYLE_RES_ORDER_PREFIX + "id-";
  private static final String STYLE_RES_ORDER_DATE_PREFIX = STYLE_RES_ORDER_PREFIX + "date-";
  private static final String STYLE_RES_ORDER_COMPANY_PREFIX = STYLE_RES_ORDER_PREFIX + "company-";
  private static final String STYLE_RES_ORDER_MANAGER_PREFIX = STYLE_RES_ORDER_PREFIX
      + "manager-";
  private static final String STYLE_RES_ORDER_WAREHOUSE_PREFIX = STYLE_RES_ORDER_PREFIX
      + "warehouse-";
  private static final String STYLE_RES_ORDER_REMAINDER_PREFIX = STYLE_RES_ORDER_PREFIX
      + "remainderQty-";
  private static final String STYLE_RES_ORDER_INVOICE_QTY_PREFIX = STYLE_RES_ORDER_PREFIX
      + "invoiceQty-";

  static String getSupplierKey(boolean services) {
    return BeeUtils.join(BeeConst.STRING_UNDER, GRID_ITEMS, services ? "services" : "goods");
  }

  private final boolean services;

  ItemsGrid(boolean services) {
    this.services = services;
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView,
      ColumnDescription columnDescription) {

    String id = columnDescription.getId();

    if (showServices()) {
      switch (id) {
        case COL_ITEM_WEIGHT:
        case COL_ITEM_AREA:
        case COL_RESERVED_REMAINDER:
        case PRP_FREE_REMAINDER:
        case COL_WAREHOUSE_REMAINDER:
        case TradeConstants.PROP_STOCK:
          return null;
      }

    } else {
      switch (id) {
        case COL_TIME_UNIT:
        case COL_ITEM_DPW:
        case COL_ITEM_MIN_TERM:
          return null;
      }
    }

    return super.beforeCreateColumn(gridView, columnDescription);
  }

  @Override
  public String getCaption() {
    if (showServices()) {
      return Localized.dictionary().services();
    } else {
      return Localized.dictionary().goods();
    }
  }

  @Override
  public GridInterceptor getInstance() {
    return new ItemsGrid(services);
  }

  @Override
  public List<String> getParentLabels() {
    if (getSelectedTreeItem() == null || getTreeView() == null) {
      return super.getParentLabels();
    } else {
      return getTreeView().getPathLabels(getSelectedTreeItem().getId(), COL_CATEGORY_NAME);
    }
  }

  @Override
  public boolean initDescription(GridDescription gridDescription) {
    gridDescription.setCaption(null);

    Filter filter;
    if (showServices()) {
      filter = Filter.notNull(COL_ITEM_IS_SERVICE);
    } else {
      filter = Filter.isNull(COL_ITEM_IS_SERVICE);
    }

    gridDescription.setFilter(filter);
    return true;
  }

  @Override
  public void onEditStart(EditStartEvent event) {

    if (BeeUtils.same(event.getColumnId(), COL_RESERVED_REMAINDER)) {
      event.consume();

      IsRow row = event.getRowValue();

      Filter filter = Filter.and(Filter.equals(COL_ORDERS_STATUS, OrdersStatus.APPROVED),
          Filter.in("OrderID", VIEW_ORDER_ITEMS, COL_ORDER, Filter.equals(COL_ITEM, row.getId())));

      Queries.getRowSet(VIEW_ORDERS, Arrays.asList(ProjectConstants.COL_DATES_START_DATE,
          ALS_COMPANY_NAME, ALS_MANAGER_FIRST_NAME, ALS_MANAGER_LAST_NAME, ALS_WAREHOUSE_CODE),
          filter, new RowSetCallback() {

            @Override
            public void onSuccess(BeeRowSet orders) {

              if (orders.getNumberOfRows() > 0) {
                ParameterList params = ClassifierKeeper.createArgs(SVC_FILTER_ORDERS);
                params.addDataItem(TBL_ORDERS, Codec.beeSerialize(orders.getRowIds()));
                params.addDataItem(COL_ITEM, row.getId());

                BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    if (!response.hasErrors()) {
                      Map<String, Pair<String, String>> remainders = new HashMap<>();
                      for (Map.Entry<String, String> entry : Codec.deserializeHashMap(response
                          .getResponseAsString()).entrySet()) {
                        remainders.put(entry.getKey(), Pair.restore(entry.getValue()));
                      }

                      Global.showModalWidget(renderOrders(orders, remainders));
                    }
                  }
                });
              }
            }
          });
    } else {
      super.onEditStart(event);
    }

  }

  public boolean showServices() {
    return services;
  }

  @Override
  protected Filter getFilter(Long category) {
    if (category == null) {
      return null;
    } else {
      return Filter.or(Filter.equals(COL_ITEM_TYPE, category),
          Filter.equals(COL_ITEM_GROUP, category),
          Filter.in(Data.getIdColumn(VIEW_ITEMS),
              VIEW_ITEM_CATEGORIES, COL_ITEM, Filter.equals(COL_CATEGORY, category)));
    }
  }

  IsRow getSelectedCategory() {
    return getSelectedTreeItem();
  }

  private static HtmlTable renderOrders(BeeRowSet rowSet,
      Map<String, Pair<String, String>> remainderMap) {

    HtmlTable table = new HtmlTable(STYLE_RES_ORDER_TABLE);
    Dictionary lc = Localized.dictionary();

    table.setText(0, 0, lc.order(), STYLE_RES_ORDER_ID_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(0, 1, lc.orderDate(), STYLE_RES_ORDER_DATE_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(0, 2, lc.company(), STYLE_RES_ORDER_COMPANY_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(0, 3, lc.manager(), STYLE_RES_ORDER_MANAGER_PREFIX
        + STYLE_LABEL_SUFFIX);
    table.setText(0, 4, lc.warehouse(), STYLE_RES_ORDER_WAREHOUSE_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(0, 5, lc.ordResQty(), STYLE_RES_ORDER_REMAINDER_PREFIX + STYLE_LABEL_SUFFIX);
    table.setText(0, 6, lc.ordInvoiceQty(), STYLE_RES_ORDER_INVOICE_QTY_PREFIX
        + STYLE_LABEL_SUFFIX);
    table.getRowFormatter().addStyleName(0, STYLE_RES_ORDER_HEADER);

    for (int i = 0; i < rowSet.getNumberOfRows(); i++) {
      table.setText(i + 1, 0, String.valueOf(rowSet.getRow(i).getId()), STYLE_RES_ORDER_ID_PREFIX
          + STYLE_CELL_SUFFIX);
      table.setText(i + 1, 1, Format.renderDate(rowSet.getRow(i).getDateTime(0).getDate()),
          STYLE_RES_ORDER_DATE_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(i + 1, 2, rowSet.getRow(i).getString(1), STYLE_RES_ORDER_COMPANY_PREFIX
          + STYLE_CELL_SUFFIX);
      table.setText(i + 1, 3, BeeUtils.join(" ", rowSet.getRow(i).getString(2), rowSet.getRow(i)
          .getString(3)), STYLE_RES_ORDER_MANAGER_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(i + 1, 4, rowSet.getRow(i).getString(4), STYLE_RES_ORDER_WAREHOUSE_PREFIX
          + STYLE_CELL_SUFFIX);
      table.setText(i + 1, 5, remainderMap.get(String.valueOf(rowSet.getRow(i).getId())).getA(),
          STYLE_RES_ORDER_REMAINDER_PREFIX + STYLE_CELL_SUFFIX);
      table.setText(i + 1, 6, remainderMap.get(String.valueOf(rowSet.getRow(i).getId())).getB(),
          STYLE_RES_ORDER_INVOICE_QTY_PREFIX + STYLE_CELL_SUFFIX);
      table.getRowFormatter().addStyleName(i + 1, STYLE_RES_ORDER_INFO);
    }

    table.addClickHandler(event -> {
      Element target = EventUtils.getEventTargetElement(event);
      TableCellElement cell = DomUtils.getParentCell(target, true);

      if (cell != null && cell.hasClassName(STYLE_RES_ORDER_ID_PREFIX + STYLE_CELL_SUFFIX)) {

        long id = Long.valueOf(cell.getInnerText());
        if (DataUtils.isId(id)) {
          RowEditor.openForm(COL_ORDER, Data.getDataInfo(VIEW_ORDERS), Filter.compareId(id),
              Opener.MODAL, null, new OrderForm());
        }
      }
    });

    return table;
  }
}
