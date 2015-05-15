package com.butent.bee.client.modules.ec;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.cell.AbstractCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.Relation.Caching;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.websocket.messages.ProgressMessage;

import java.util.ArrayList;
import java.util.List;

public class EcOrphansGrid extends AbstractGridInterceptor implements ClickHandler {

  Button button = new Button(Localized.getConstants().ecAnalogBinding(), this);
  Image loading = new Image(Global.getImages().loading());

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnName, "Chain")) {
      column.getCell().addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof AbstractCell<?>) {
            createNewItem(((AbstractCell<?>) event.getSource()).getEventContext().getRow());
          }
        }
      });
    }
    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();

    presenter.getHeader().addCommandItem(button);
    presenter.getHeader().addCommandItem(loading);
    setLoading(false);
  }

  private void setLoading(boolean isLoading) {
    button.setVisible(!isLoading);
    loading.setVisible(isLoading);
  }

  @Override
  public GridInterceptor getInstance() {
    return new EcOrphansGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    Global.confirm(Localized.getMessages().ecLocateAnalogs(getGridView().getGrid().getRowCount()),
        new ConfirmationCallback() {
          @Override
          public void onConfirm() {
            setLoading(true);
            final ParameterList args = EcKeeper.createArgs(SVC_ADOPT_ORPHANS);
            Filter filter = getGridPresenter().getDataProvider().getFilter();

            if (filter != null) {
              args.addDataItem("filter", filter.serialize());
            }
            Endpoint.initProgress(Localized.getConstants().ecAnalogBinding(),
                new Consumer<String>() {
                  @Override
                  public void accept(String progress) {
                    if (!BeeUtils.isEmpty(progress)) {
                      args.addDataItem(Service.VAR_PROGRESS, progress);

                      Endpoint.registerProgressHandler(progress,
                          new Function<ProgressMessage, Boolean>() {
                            @Override
                            public Boolean apply(ProgressMessage pm) {
                              if (pm.isClosed() || pm.isCanceled()) {
                                getGridPresenter().refresh(true);
                                setLoading(false);
                              }
                              return null;
                            }
                          });
                    }
                    BeeKeeper.getRpc().makePostRequest(args, (ResponseCallback) null);
                  }
                });
          }
        });
  }

  private void createNewItem(final IsRow orphan) {
    Flow panel = new Flow();

    final TabBar tabs = new TabBar(Orientation.HORIZONTAL);
    tabs.addItem(Localized.getConstants().ecItemName());
    tabs.addItem(Localized.getConstants().ecItemAnalog());
    StyleUtils.setWidth(tabs, 100, CssUnit.PCT);
    tabs.getElement().getStyle().setMarginBottom(5, Unit.PX);
    panel.add(tabs);

    final InputText name = new InputText();
    name.setValue(Data.getString(getViewName(), orphan, COL_TCD_ARTICLE_NAME));
    StyleUtils.setWidth(name, 300);
    panel.add(name);

    Relation relation = Relation.create(TBL_TCD_ARTICLES,
        Lists.newArrayList(COL_TCD_ARTICLE_NR, COL_TCD_ARTICLE_NAME, COL_TCD_BRAND_NAME));
    relation.disableNewRow();
    relation.setInstant(false);
    relation.setCaching(Caching.NONE);

    final UnboundSelector selector = UnboundSelector.create(relation);
    StyleUtils.setWidth(selector, 300);
    panel.add(selector);

    tabs.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> ev) {
        int idx = ev.getSelectedItem();
        StyleUtils.setVisible(name, idx == 0);
        StyleUtils.setVisible(selector, idx == 1);
      }
    });
    tabs.selectTab(0, true);

    Global.inputWidget(Localized.getConstants().ecItemNew(), panel, new InputCallback() {
      @Override
      public String getErrorMessage() {
        int idx = tabs.getSelectedTab();
        BeeRow analog = selector.getRelatedRow();

        if (idx == 0 && BeeUtils.isEmpty(name.getValue()) || idx == 1 && analog == null) {
          return Localized.getConstants().valueRequired();
        }
        return super.getErrorMessage();
      }

      @Override
      public void onSuccess() {
        int idx = tabs.getSelectedTab();

        if (idx == 0) {
          createNewItemWithName(orphan, name.getValue());
        } else {
          createNewItemBasedOnAnalog(orphan, selector.getRelatedRow().getId());
        }
      }
    });
  }

  private void createNewItemBasedOnAnalog(final IsRow orphan, Long article) {
    ParameterList args = EcKeeper.createArgs(SVC_CREATE_ITEM);
    args.addDataItem(COL_TCD_ARTICLE, article);

    for (String col : new String[] {
        COL_TCD_ARTICLE_NR, COL_TCD_BRAND, COL_TCD_ARTICLE_DESCRIPTION, COL_TCD_SUPPLIER,
        COL_TCD_SUPPLIER_ID}) {

      String value = Data.getString(getViewName(), orphan, col);

      if (!BeeUtils.isEmpty(value)) {
        args.addDataItem(col, value);
      }
    }
    BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getGridView());

        if (!response.hasErrors()) {
          Queries.deleteRow(getViewName(), orphan.getId(), new IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              RowDeleteEvent.fire(BeeKeeper.getBus(), getViewName(), orphan.getId());
            }
          });
          RowEditor.open(TBL_TCD_ARTICLES, response.getResponseAsLong(), Opener.NEW_TAB);
        }
      }
    });
  }

  private void createNewItemWithName(final IsRow orphan, String name) {
    final String orphans = getViewName();

    List<BeeColumn> columns = Data.getColumns(orphans, Lists.newArrayList(COL_TCD_ARTICLE_NR,
        COL_TCD_ARTICLE_NAME, COL_TCD_ARTICLE_DESCRIPTION, COL_TCD_BRAND));

    List<String> values = new ArrayList<>();

    for (BeeColumn col : columns) {
      if (BeeUtils.same(col.getId(), COL_TCD_ARTICLE_NAME)) {
        values.add(name);
      } else {
        values.add(Data.getString(orphans, orphan, col.getId()));
      }
    }
    Queries.insert(TBL_TCD_ARTICLES, columns, values, null, new RowCallback() {
      @Override
      public void onSuccess(final BeeRow row) {
        List<BeeColumn> cols = Data.getColumns(TBL_TCD_ARTICLE_SUPPLIERS,
            Lists.newArrayList(COL_TCD_ARTICLE, COL_TCD_SUPPLIER, COL_TCD_SUPPLIER_ID));

        List<String> vals = new ArrayList<>();

        for (BeeColumn col : cols) {
          if (BeeUtils.same(col.getId(), COL_TCD_ARTICLE)) {
            vals.add(BeeUtils.toString(row.getId()));
          } else {
            vals.add(Data.getString(orphans, orphan, col.getId()));
          }
        }
        Queries.insert(TBL_TCD_ARTICLE_SUPPLIERS, cols, vals);

        cols = Data.getColumns(TBL_TCD_ARTICLE_CODES,
            Lists.newArrayList(COL_TCD_ARTICLE, COL_TCD_SEARCH_NR, COL_TCD_CODE_NR, COL_TCD_BRAND));

        vals = Lists.newArrayList(BeeUtils.toString(row.getId()),
            EcUtils.normalizeCode(Data.getString(orphans, orphan, COL_TCD_ARTICLE_NR)),
            Data.getString(orphans, orphan, COL_TCD_ARTICLE_NR),
            Data.getString(orphans, orphan, COL_TCD_BRAND));

        Queries.insert(TBL_TCD_ARTICLE_CODES, cols, vals, null, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            RowEditor.open(TBL_TCD_ARTICLES, row, Opener.NEW_TAB);
          }
        });
        Queries.deleteRow(orphans, orphan.getId(), new IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            RowDeleteEvent.fire(BeeKeeper.getBus(), orphans, orphan.getId());
          }
        });
      }
    });
  }
}
