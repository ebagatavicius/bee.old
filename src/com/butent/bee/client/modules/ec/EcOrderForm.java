package com.butent.bee.client.modules.ec;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.SimpleCheckBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants.EcOrderStatus;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class EcOrderForm extends AbstractFormInterceptor {

  private static final String STYLE_UNSUPPLIED = EcStyles.name("Order-UnsuppliedItems-");

  private static final String STYLE_UNSUPPLIED_SELECTION = STYLE_UNSUPPLIED + "selection";
  private static final String STYLE_UNSUPPLIED_DATE = STYLE_UNSUPPLIED + "date";
  private static final String STYLE_UNSUPPLIED_NAME = STYLE_UNSUPPLIED + "name";
  private static final String STYLE_UNSUPPLIED_BRAND = STYLE_UNSUPPLIED + "brand";
  private static final String STYLE_UNSUPPLIED_CODE = STYLE_UNSUPPLIED + "code";
  private static final String STYLE_UNSUPPLIED_QUANTITY = STYLE_UNSUPPLIED + "quantity";
  private static final String STYLE_UNSUPPLIED_PRICE = STYLE_UNSUPPLIED + "price";

  private static final String STYLE_UNSUPPLIED_ROW = STYLE_UNSUPPLIED + "row";
  private static final String STYLE_UNSUPPLIED_SELECTED = STYLE_UNSUPPLIED + "selected";

  private static final String STYLE_SUFFIX_LABEL = "-label";
  private static final String STYLE_SUFFIX_INPUT = "-input";

  private static final String STYLE_COMMAND = EcStyles.name("Order-Command-");

  private static void addToUnsuppliedItems(long orderId) {
    ParameterList params = EcKeeper.createArgs(SVC_ADD_TO_UNSUPPLIED_ITEMS);
    params.addQueryItem(VAR_ORDER, orderId);

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_UNSUPPLIED_ITEMS);
      }
    });
  }

  private Button unsupplied;
  private Button mail;
  private Button erp;
  private Button reject;
  private Button finish;

  EcOrderForm() {
    super();
  }

  @Override
  public FormInterceptor getInstance() {
    return new EcOrderForm();
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    EcOrderStatus status = EcOrderStatus.get(Data.getInteger(getViewName(), row, COL_ORDER_STATUS));

    updateCommands(status);
    reenable(status);

    return true;
  }

  private void appendUnsuppliedItems(BeeRowSet rowSet, final Set<Long> selectedIds) {
    long orderId = getActiveRowId();

    int articleIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_ARTICLE);
    int qtyIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_QUANTITY);
    int priceIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_PRICE);
    int noteIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_NOTE);

    List<String> colNames = Lists.newArrayList(COL_ORDER_ITEM_ORDER, COL_ORDER_ITEM_ARTICLE,
        COL_ORDER_ITEM_QUANTITY_ORDERED, COL_ORDER_ITEM_QUANTITY_SUBMIT,
        COL_ORDER_ITEM_PRICE, COL_ORDER_ITEM_NOTE);
    List<BeeColumn> columns = Data.getColumns(VIEW_ORDER_ITEMS, colNames);

    final Holder<Integer> latch = Holder.of(0);
    RowInsertCallback insertCallback = new RowInsertCallback(VIEW_ORDER_ITEMS, null) {
      @Override
      public void onSuccess(BeeRow result) {
        super.onSuccess(result);

        latch.set(latch.get() + 1);
        if (BeeUtils.unbox(latch.get()) == selectedIds.size()) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDER_ITEMS);
        }
      }
    };

    final Set<RowInfo> delete = new HashSet<>();

    for (Long id : selectedIds) {
      BeeRow row = rowSet.getRowById(id);

      List<String> values = Queries.asList(orderId, row.getString(articleIndex),
          0, row.getString(qtyIndex), row.getString(priceIndex), row.getString(noteIndex));
      Queries.insert(VIEW_ORDER_ITEMS, columns, values, null, insertCallback);

      delete.add(new RowInfo(row, true));
    }

    if (!delete.isEmpty()) {
      Queries.deleteRows(VIEW_UNSUPPLIED_ITEMS, delete, new Queries.IntCallback() {
        @Override
        public void onSuccess(Integer result) {
          DataChangeEvent.fireReset(BeeKeeper.getBus(), VIEW_UNSUPPLIED_ITEMS);
        }
      });
    }
  }

  private void deliverTheLetter() {
    ParameterList args = EcKeeper.createArgs(SVC_MAIL_ORDER);
    args.addQueryItem(VAR_ORDER, getActiveRowId());

    BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());
      }
    });
  }

  private void endRequest() {
    EcOrderStatus status = getOrderStatus();

    updateCommands(status);
    reenable(status);
  }

  private void finishOrder() {
    List<String> messages =
        Collections.singletonList(Localized.dictionary().ecOrderFinishConfirm());

    Global.confirm(null, Icon.QUESTION, messages, new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        updateStatus(EcOrderStatus.FINISHED);
      }
    });
  }

  private EcOrderStatus getOrderStatus() {
    return EcOrderStatus.get(Data.getInteger(getViewName(), getActiveRow(), COL_ORDER_STATUS));
  }

  private void getUnsuppliedItems() {
    int index = getDataIndex(COL_ORDER_CLIENT);
    Long client = getActiveRow().getLong(index);
    Assert.notNull(client);

    Filter filter = Filter.equals(COL_UNSUPPLIED_ITEM_CLIENT, client);
    Queries.getRowSet(VIEW_UNSUPPLIED_ITEMS, null, filter, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (DataUtils.isEmpty(result)) {
          Global.showInfo(Localized.dictionary().ecUnsuppliedItemsNotFound());
        } else {
          showUnsuppliedItems(result);
        }
      }
    });
  }

  private void pleaseMisterPostman() {
    IsRow row = getActiveRow();
    String caption = EcUtils.formatPerson(row.getString(getDataIndex(ALS_ORDER_CLIENT_FIRST_NAME)),
        row.getString(getDataIndex(ALS_ORDER_CLIENT_LAST_NAME)),
        row.getString(getDataIndex(ALS_ORDER_CLIENT_COMPANY_NAME)));

    List<String> messages =
        Collections.singletonList(Localized.dictionary().ecOrderMailConfirm());

    Global.confirm(caption, Icon.QUESTION, messages, new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        deliverTheLetter();
      }
    });
  }

  private void reenable(EcOrderStatus status) {
    boolean enabled = status == EcOrderStatus.NEW;
    if (getFormView() != null && getFormView().isEnabled() != enabled) {
      getFormView().setEnabled(enabled);
    }
  }

  private void registerOrderEvent(Long orderId, EcOrderStatus status) {
    ParameterList args = EcKeeper.createArgs(SVC_REGISTER_ORDER_EVENT);
    args.addQueryItem(VAR_ORDER, orderId);
    args.addQueryItem(VAR_STATUS, status.ordinal());

    BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(getFormView());
        if (!response.hasErrors()) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDER_EVENTS);
        }
      }
    });
  }

  private void rejectOrder() {
    String stylePrefix = EcStyles.name("OrderRejection-");

    HtmlTable table = new HtmlTable();
    table.addStyleName(stylePrefix + "table");

    int row = 0;

    String styleName = stylePrefix + "reason";
    Label label = new Label(Localized.dictionary().ecRejectionReason());
    label.addStyleName(StyleUtils.NAME_REQUIRED);
    table.setWidgetAndStyle(row, 0, label, styleName + STYLE_SUFFIX_LABEL);

    final UnboundSelector selector = UnboundSelector.create(VIEW_REJECTION_REASONS,
        Collections.singletonList(COL_REJECTION_REASON_NAME));
    table.setWidgetAndStyle(row, 1, selector, styleName + STYLE_SUFFIX_INPUT);
    row++;

    styleName = stylePrefix + "comment";
    label = new Label(Localized.dictionary().comment());
    table.setWidgetAndStyle(row, 0, label, styleName + STYLE_SUFFIX_LABEL);

    final InputArea inputArea = new InputArea();

    Widget widget = getFormView().getWidgetBySource(COL_ORDER_MANAGER_COMMENT);
    if (widget instanceof InputArea) {
      String value = ((InputArea) widget).getValue();
      if (!BeeUtils.isEmpty(value)) {
        inputArea.setValue(value.trim());
      }
    }

    table.setWidgetAndStyle(row, 1, inputArea, styleName + STYLE_SUFFIX_INPUT);
    row++;

    int col = 0;
    Button confirm = new Button(Localized.dictionary().ecOrderRejectConfirm());
    table.setWidgetAndStyle(row, col, confirm, stylePrefix + "confirm");

    table.getCellFormatter().setHorizontalAlignment(row, col, TextAlign.CENTER);
    table.getCellFormatter().setColSpan(row, col, 2);

    final DialogBox dialog = DialogBox.create(Localized.dictionary().ecOrderRejectCaption(),
        stylePrefix + "dialog");
    dialog.setWidget(table);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.center();
    selector.setFocus(true);

    confirm.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Long rrId = selector.getRelatedId();

        if (DataUtils.isId(rrId)) {
          dialog.close();
          updateStatus(EcOrderStatus.REJECTED, rrId, inputArea.getValue(), true);
        } else {
          getFormView().notifySevere(Localized.dictionary().ecRejectionReasonRequired());
          selector.setFocus(true);
        }
      }
    });
  }

  private void sendToErp() {
    List<String> messages =
        Collections.singletonList(Localized.dictionary().ecOrderSendToERPConfirm());

    Global.confirm(null, Icon.QUESTION, messages, new ConfirmationCallback() {
      @Override
      public void onConfirm() {
        final long rowId = getActiveRowId();
        startRequest();

        ParameterList args = EcKeeper.createArgs(SVC_SEND_TO_ERP);
        args.addQueryItem(VAR_ORDER, getActiveRowId());
        if (waitingALongLongTime()) {
          args.addQueryItem(VAR_MAIL, 1);
        }

        BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            response.notify(getFormView());

            if (response.hasErrors()) {
              endRequest();

            } else {
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_UNSUPPLIED_ITEMS);
              DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_ORDER_EVENTS);

              Queries.getRow(getViewName(), rowId, new RowCallback() {
                @Override
                public void onFailure(String... reason) {
                  endRequest();
                  getFormView().notifySevere(reason);
                }

                @Override
                public void onSuccess(BeeRow result) {
                  if (rowId == getActiveRowId() && !getFormView().observesData()) {
                    getFormView().updateRow(result, false);
                  }
                  RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);

                  endRequest();
                }
              });
            }
          }
        });
      }
    });
  }

  private void showUnsuppliedItems(final BeeRowSet rowSet) {
    int dateIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_DATE);

    int nameIndex = rowSet.getColumnIndex(COL_TCD_ARTICLE_NAME);
    int brandIndex = rowSet.getColumnIndex(COL_TCD_BRAND_NAME);
    int codeIndex = rowSet.getColumnIndex(COL_TCD_ARTICLE_NR);

    int qtyIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_QUANTITY);
    int priceIndex = rowSet.getColumnIndex(COL_UNSUPPLIED_ITEM_PRICE);

    final Set<Long> selectedIds = new HashSet<>();

    Flow container = new Flow(STYLE_UNSUPPLIED + "container");

    final HtmlTable table = new HtmlTable();
    table.addStyleName(STYLE_UNSUPPLIED + "table");

    int row = 0;
    int col = 0;

    SimpleCheckBox selectAll = new SimpleCheckBox();
    selectAll.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        boolean select = BeeUtils.isTrue(event.getValue());

        selectedIds.clear();
        if (select) {
          for (BeeRow dataRow : rowSet.getRows()) {
            selectedIds.add(dataRow.getId());
          }
        }

        Collection<Widget> children = UiHelper.getChildrenByStyleName(table,
            Collections.singleton(STYLE_UNSUPPLIED_SELECTION));

        for (Widget child : children) {
          if (child instanceof SimpleCheckBox) {
            ((SimpleCheckBox) child).setValue(select, true);
          }
        }
      }
    });
    table.setWidgetAndStyle(row, col++, selectAll, STYLE_UNSUPPLIED + "selectAll");

    Label dateLabel = new Label(Localized.dictionary().ecOrderDate());
    table.setWidgetAndStyle(row, col++, dateLabel, STYLE_UNSUPPLIED_DATE + STYLE_SUFFIX_LABEL);

    Label nameLabel = new Label(Localized.dictionary().ecItemName());
    table.setWidgetAndStyle(row, col++, nameLabel, STYLE_UNSUPPLIED_NAME + STYLE_SUFFIX_LABEL);

    Label brandLabel = new Label(Localized.dictionary().ecItemBrand());
    table.setWidgetAndStyle(row, col++, brandLabel, STYLE_UNSUPPLIED_BRAND + STYLE_SUFFIX_LABEL);

    Label codeLabel = new Label(Localized.dictionary().ecItemCode());
    table.setWidgetAndStyle(row, col++, codeLabel, STYLE_UNSUPPLIED_CODE + STYLE_SUFFIX_LABEL);

    Label qtyLabel = new Label(Localized.dictionary().ecItemQuantity());
    table.setWidgetAndStyle(row, col++, qtyLabel, STYLE_UNSUPPLIED_QUANTITY + STYLE_SUFFIX_LABEL);

    Label priceLabel = new Label(Localized.dictionary().ecItemPrice());
    table.setWidgetAndStyle(row, col++, priceLabel, STYLE_UNSUPPLIED_PRICE + STYLE_SUFFIX_LABEL);

    table.getRowFormatter().addStyleName(row, STYLE_UNSUPPLIED + "header");
    row++;

    for (BeeRow dataRow : rowSet.getRows()) {
      final long id = dataRow.getId();

      col = 0;

      final SimpleCheckBox selection = new SimpleCheckBox();
      selection.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
          TableRowElement element = DomUtils.getParentRow(selection.getElement(), false);

          if (BeeUtils.isTrue(event.getValue())) {
            selectedIds.add(id);
            element.addClassName(STYLE_UNSUPPLIED_SELECTED);
          } else {
            selectedIds.remove(id);
            element.removeClassName(STYLE_UNSUPPLIED_SELECTED);
          }
        }
      });

      table.setWidgetAndStyle(row, col++, selection, STYLE_UNSUPPLIED_SELECTION);

      Label dateWidget = new Label(Format.renderDateTime(dataRow.getDateTime(dateIndex)));
      table.setWidgetAndStyle(row, col++, dateWidget, STYLE_UNSUPPLIED_DATE);

      Label nameWidget = new Label(dataRow.getString(nameIndex));
      table.setWidgetAndStyle(row, col++, nameWidget, STYLE_UNSUPPLIED_NAME);

      Label brandWidget = new Label(dataRow.getString(brandIndex));
      table.setWidgetAndStyle(row, col++, brandWidget, STYLE_UNSUPPLIED_BRAND);

      Label codeWidget = new Label(dataRow.getString(codeIndex));
      table.setWidgetAndStyle(row, col++, codeWidget, STYLE_UNSUPPLIED_CODE);

      Label qtyWidget = new Label(dataRow.getString(qtyIndex));
      table.setWidgetAndStyle(row, col++, qtyWidget, STYLE_UNSUPPLIED_QUANTITY);

      int cents = EcUtils.toCents(dataRow.getDouble(priceIndex));
      Label priceWidget = new Label(EcUtils.formatCents(cents));
      table.setWidgetAndStyle(row, col++, priceWidget, STYLE_UNSUPPLIED_PRICE);

      table.getRowFormatter().addStyleName(row, STYLE_UNSUPPLIED_ROW);

      row++;
    }

    Simple wrapper = new Simple(table);
    wrapper.addStyleName(STYLE_UNSUPPLIED + "wrapper");

    container.add(wrapper);

    Button append = new Button(Localized.dictionary().ecUnsuppliedItemsAppend());
    append.addStyleName(STYLE_UNSUPPLIED + "append");
    container.add(append);

    final DialogBox dialog = DialogBox.create(Localized.dictionary().ecUnsuppliedItems(),
        STYLE_UNSUPPLIED + "dialog");
    dialog.setWidget(container);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.center();

    append.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (selectedIds.isEmpty()) {
          Global.showInfo(Localized.dictionary().selectAtLeastOneRow());
        } else {
          appendUnsuppliedItems(rowSet, selectedIds);
          dialog.close();
        }
      }
    });
  }

  private void startRequest() {
    HeaderView header = getHeaderView();
    if (header != null) {
      header.clearCommandPanel();
      header.addCommandItem(new Image(Global.getImages().loading()));
    }
  }

  private void updateCommands(EcOrderStatus status) {
    HeaderView header = getHeaderView();
    header.clearCommandPanel();

    if (status == EcOrderStatus.NEW) {
      if (this.unsupplied == null) {
        this.unsupplied = new Button(Localized.dictionary().ecOrderCommandUnsuppliedItems(),
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                getUnsuppliedItems();
              }
            });
        this.unsupplied.addStyleName(STYLE_COMMAND + "unsupplied");
      }
      header.addCommandItem(this.unsupplied);
    }

    if (this.mail == null) {
      this.mail = new Button(Localized.dictionary().ecOrderCommandMail(),
          new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              pleaseMisterPostman();
            }
          });
      this.mail.addStyleName(STYLE_COMMAND + "mail");
    }
    header.addCommandItem(this.mail);

    if (status == EcOrderStatus.NEW) {
      if (this.erp == null) {
        this.erp = new Button(Localized.dictionary().ecOrderCommandSendToERP(),
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                sendToErp();
              }
            });
        this.erp.addStyleName(STYLE_COMMAND + "erp");
      }
      header.addCommandItem(this.erp);

      if (this.reject == null) {
        this.reject = new Button(Localized.dictionary().ecOrderCommandReject(),
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                rejectOrder();
              }
            });
        this.reject.addStyleName(STYLE_COMMAND + "reject");
      }
      header.addCommandItem(this.reject);
    }

    if (status == EcOrderStatus.ACTIVE) {
      if (this.finish == null) {
        this.finish = new Button(Localized.dictionary().ecOrderCommandFinish(),
            new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                finishOrder();
              }
            });
        this.finish.addStyleName(STYLE_COMMAND + "finish");
      }
      header.addCommandItem(this.finish);
    }
  }

  private void updateStatus(EcOrderStatus status) {
    updateStatus(status, null, null, false);
  }

  private void updateStatus(final EcOrderStatus status, Long rejectionReason, String comment,
      final boolean addToUnsupplied) {

    startRequest();

    BeeRow orderRow = DataUtils.cloneRow(getActiveRow());
    orderRow.setValue(getDataIndex(COL_ORDER_STATUS), status.ordinal());

    if (rejectionReason != null) {
      orderRow.setValue(getDataIndex(COL_ORDER_REJECTION_REASON), rejectionReason);
    }

    if (comment != null) {
      int commentIndex = getDataIndex(COL_ORDER_MANAGER_COMMENT);
      if (!BeeUtils.equalsTrim(orderRow.getString(commentIndex), comment)) {
        orderRow.setValue(commentIndex, comment.trim());
      }
    }

    Queries.update(getViewName(), getFormView().getDataColumns(), getFormView().getOldRow(),
        orderRow, getFormView().getChildrenForUpdate(), new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            endRequest();
            getFormView().notifySevere(reason);
          }

          @Override
          public void onSuccess(BeeRow result) {
            if (DataUtils.sameId(result, getActiveRow()) && !getFormView().observesData()) {
              getFormView().updateRow(result, false);
            }
            RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), result);

            registerOrderEvent(result.getId(), status);
            if (addToUnsupplied) {
              addToUnsuppliedItems(result.getId());
            }

            if (waitingALongLongTime()) {
              deliverTheLetter();
            }

            endRequest();
          }
        });
  }

  private boolean waitingALongLongTime() {
    IsRow row = getActiveRow();
    return row != null && BeeUtils.isTrue(row.getBoolean(getDataIndex(COL_ORDER_COPY_BY_MAIL)));
  }
}
