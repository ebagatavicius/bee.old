package com.butent.bee.client.modules.classifiers;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.presenter.GridFormPresenter;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.RowPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridFormKind;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontSize;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

class CompanyForm extends AbstractFormInterceptor {

  private FaLabel switchAction;

  CompanyForm() {
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid && (BeeUtils.same(name, GRID_COMPANY_BANK_ACCOUNTS)
        || BeeUtils.same(name, GRID_COMPANY_USERS))) {
      ChildGrid grid = (ChildGrid) widget;

      grid.setGridInterceptor(new AbstractGridInterceptor() {

        @Override
        public void afterCreatePresenter(GridPresenter presenter) {
          HeaderView header = presenter.getHeader();
          header.clearCommandPanel();

          FaLabel setDefault = new FaLabel(FontAwesome.CHECK);
          setDefault.setTitle(Localized.dictionary().setAsPrimary());
          setDefault.addClickHandler(event -> {
            GridView gridView = getGridPresenter().getGridView();

            IsRow selectedRow = gridView.getActiveRow();
            if (selectedRow == null) {
              gridView.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
            } else {
              setAsPrimary(selectedRow.getId());
            }
          });

          header.addCommandItem(setDefault);
        }

        @Override
        public void afterInsertRow(IsRow row) {
          setAsPrimary(row.getId(), true);
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }

        private void setAsPrimary(Long gridRowId) {
          setAsPrimary(gridRowId, false);
        }

        private void setAsPrimary(Long gridRowId, boolean checkDefault) {
          GridView gridView = getGridPresenter().getGridView();
          String gridName = gridView.getGridName();

          if (BeeUtils.same(gridName, GRID_COMPANY_BANK_ACCOUNTS)) {
            setAsPrimaryAccount(gridRowId, checkDefault);
          }

          if (BeeUtils.same(gridName, GRID_COMPANY_USERS)) {
            setAsPrimaryCompanyUser(gridRowId, checkDefault);
          }
        }

        private void setAsPrimaryAccount(final Long companyBankAccount, boolean checkDefault) {
          final IsRow companyRow = getFormView().getActiveRow();
          final IsRow companyRowOld = getFormView().getOldRow();
          final int defBankAccFieldId = Data.getColumnIndex(VIEW_COMPANIES,
              COL_DEFAULT_BANK_ACCOUNT);

          boolean hasDefault =
              DataUtils.isId(companyRow.getLong(defBankAccFieldId));

          boolean canChange = !hasDefault || !checkDefault;

          if (canChange) {
            Queries.update(getFormView().getViewName(), Filter.compareId(companyRow.getId()),
                COL_DEFAULT_BANK_ACCOUNT, Value.getValue(companyBankAccount), new IntCallback() {

                  @Override
                  public void onSuccess(Integer result) {
                    companyRow.setValue(defBankAccFieldId, companyBankAccount);
                    companyRowOld.setValue(defBankAccFieldId, companyBankAccount);
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getGridView().getViewName());
                  }
                });
          }
        }

        private void setAsPrimaryCompanyUser(final Long companyUser, boolean checkDefault) {
          final IsRow companyRow = getFormView().getActiveRow();
          final IsRow companyRowOld = getFormView().getOldRow();
          final int idxDefComanyUser = Data.getColumnIndex(VIEW_COMPANIES,
              COL_DEFAULT_COMPANY_USER);

          boolean hasDefault =
              DataUtils.isId(companyRow.getLong(idxDefComanyUser));
          boolean canChange = !hasDefault || !checkDefault;

          if (canChange) {
            Queries.update(getFormView().getViewName(), Filter.compareId(companyRow.getId()),
                COL_DEFAULT_COMPANY_USER, Value.getValue(companyUser), new IntCallback() {

                  @Override
                  public void onSuccess(Integer result) {
                    companyRow.setValue(idxDefComanyUser, companyUser);
                    companyRowOld.setValue(idxDefComanyUser, companyUser);
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getGridView().getViewName());
                  }
                });
          }
        }

      });
    } else if (BeeUtils.same(name, TBL_COMPANY_PERSONS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public ColumnDescription beforeCreateColumn(GridView gridView, ColumnDescription descr) {
          if (BeeUtils.same(descr.getId(), COL_COMPANY)) {
            return null;
          }
          return super.beforeCreateColumn(gridView, descr);
        }

        @Override
        public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
          presenter.getGridView().ensureRelId(new IdCallback() {
            @Override
            public void onSuccess(Long id) {
              final String viewName = presenter.getViewName();
              DataInfo dataInfo = Data.getDataInfo(viewName);
              BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);
              Data.setValue(viewName, newRow, COL_COMPANY, id);

              RowFactory.createRow(dataInfo.getNewRowForm(),
                  Localized.dictionary().newCompanyPerson(), dataInfo, newRow, Modality.ENABLED,
                  null, new AbstractFormInterceptor() {
                    @Override
                    public boolean beforeCreateWidget(String widgetName, Element description) {
                      if (BeeUtils.startsWith(widgetName, COL_COMPANY)) {
                        return false;
                      }
                      return super.beforeCreateWidget(widgetName, description);
                    }

                    @Override
                    public FormInterceptor getInstance() {
                      return null;
                    }
                  }, null,
                  new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow result) {
                      Data.onViewChange(viewName, DataChangeEvent.RESET_REFRESH);
                    }
                  });
            }
          });
          return false;
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }
      });
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {
    if (BeeUtils.isTrue(result.getBoolean(getDataIndex(COL_COMPANY_TYPE_PERSON)))) {
      Map<String, String> personInfo = new HashMap<>();
      personInfo.put(COL_COMPANY, BeeUtils.toString(result.getId()));
      String contact = result.getString(getDataIndex(COL_COMPANY_NAME));

      if (!BeeUtils.isEmpty(contact)) {
        String[] arr = contact.split(BeeConst.STRING_SPACE, 2);
        personInfo.put(COL_FIRST_NAME, ArrayUtils.getQuietly(arr, 0));
        personInfo.put(COL_LAST_NAME, ArrayUtils.getQuietly(arr, 1));
      }

      Stream.of(COL_PHONE, COL_MOBILE, COL_FAX, COL_ADDRESS, COL_POST_INDEX, COL_CITY, COL_COUNTRY,
          COL_WEBSITE, ALS_EMAIL_ID).forEach(column ->
          personInfo.put(column, result.getString(getDataIndex(column))));

      ClassifierUtils.createCompanyPerson(personInfo, person ->
          BeeKeeper.getScreen().notifyInfo(Localized.dictionary().newCompanyPersonMessage()));
    }
    super.afterInsertRow(result, forced);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (DataUtils.hasId(row)) {
      refreshCreditInfo();
      createQrButton(form, row);

      Presenter presenter = form.getViewPresenter();

      if (switchAction == null) {
        switchAction = getFormIcon();
        switchAction.setTitle(getFormIconTitle());
        switchAction.addClickHandler(event -> switchForm());
        presenter.getHeader().addCommandItem(switchAction);
      }
      switchAction.setVisible(!form.isAdding()
          && (presenter instanceof GridFormPresenter || presenter instanceof RowPresenter));
    }
    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyForm();
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    FormView form = getFormView();
    IsRow row = form.getActiveRow();
    if (!BeeUtils.isEmpty(event.getColumns())) {
      if (BeeUtils.isEmpty(row.getString(form.getDataIndex(COL_COMPANY_TYPE)))) {
        event.consume();
        BeeKeeper.getScreen().notifySevere(Localized.dictionary()
            .fieldRequired(Localized.dictionary().companyStatus()));
      }
    }
  }

  private static void createQrButton(FormView form, IsRow row) {
    Widget widget = form.getWidgetByName(QR_FLOW_PANEL, false);

    if (widget instanceof FlowPanel) {
      FlowPanel qrFlowPanel = (FlowPanel) widget;
      qrFlowPanel.clear();

      FaLabel qrCodeLabel = new FaLabel(FontAwesome.QRCODE);
      qrCodeLabel.setTitle(Localized.dictionary().qrCode());
      qrCodeLabel.addStyleName(StyleUtils.className(FontSize.X_LARGE));

      qrCodeLabel.addClickHandler(event -> ClassifierKeeper.generateQrCode(form, row));

      qrFlowPanel.add(qrCodeLabel);
    }
  }

  private FaLabel getFormIcon() {
    if (isFormSimple()) {
      return new FaLabel(FontAwesome.TOGGLE_OFF);
    } else {
      return new FaLabel(FontAwesome.TOGGLE_ON);
    }
  }

  private String getFormIconTitle() {
    if (isFormSimple()) {
      return Localized.dictionary().editMode();
    } else {
      return Localized.dictionary().previewMode();
    }
  }

  private boolean isFormSimple() {
    return FORM_NEW_COMPANY.equals(getFormView().getFormName());
  }

  private void refreshCreditInfo() {
    final FormView form = getFormView();
    final Widget widget = form.getWidgetByName(SVC_CREDIT_INFO, false);

    if (widget != null) {
      widget.getElement().setInnerText(null);

      ParameterList args = TradeKeeper.createArgs(SVC_CREDIT_INFO);
      args.addDataItem(COL_COMPANY, getActiveRow().getId());

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(form);

          if (response.hasErrors()) {
            return;
          }
          Map<String, String> result =
              Codec.deserializeLinkedHashMap(response.getResponseAsString());

          if (!BeeUtils.isEmpty(result)) {
            HtmlTable table = new HtmlTable();
            table.setColumnCellStyles(1, "text-align:right; font-weight:bold;color:red;");
            int c = 0;

            String amount = result.get(VAR_DEBT);

            if (BeeUtils.isPositiveDouble(amount)) {
              table.setHtml(c, 0, Localized.dictionary().trdDebt());
              table.setHtml(c, 1, amount);
              double limit = BeeUtils.toDouble(result.get(COL_COMPANY_CREDIT_LIMIT));

              if (BeeUtils.toDouble(amount) <= limit) {
                StyleUtils.setColor(table.getCellFormatter().getElement(c, 1), "black");
              }
              c++;
            }
            amount = result.get(VAR_OVERDUE);

            if (BeeUtils.isPositiveDouble(amount)) {
              table.setHtml(c, 0, Localized.dictionary().trdOverdue());
              table.setHtml(c, 1, amount);
            }
            widget.getElement().setInnerHTML(table.getElement().getString());
          }
        }
      });
    }
  }

  private void switchForm() {
    if (getFormView().getViewPresenter() instanceof GridFormPresenter) {
      final GridFormPresenter presenter = (GridFormPresenter) getFormView().getViewPresenter();

      presenter.save(row -> {
        GridView gridView = presenter.getGridView();

        int index = gridView.getFormIndex(GridFormKind.EDIT);
        gridView.selectForm(GridFormKind.EDIT, 1 - index);

        EditStartEvent event = new EditStartEvent(row, gridView.isReadOnly());
        gridView.onEditStart(event);
      });

    } else if (getFormView().getViewPresenter() instanceof RowPresenter) {
      String switchTo = isFormSimple() ? FORM_COMPANY : FORM_NEW_COMPANY;
      String viewName = getViewName();

      IsRow oldRow = getFormView().getOldRow();
      IsRow newRow = getActiveRow();

      boolean modal = UiHelper.isModal(getFormView().asWidget());

      getFormView().getViewPresenter().handleAction(Action.CANCEL);

      Consumer<FormView> onOpen = form -> form.setOldRow(oldRow);
      Opener opener = modal ? Opener.modal(onOpen) : Opener.newTab(onOpen);

      RowEditor.openForm(switchTo, viewName, newRow, opener, null);
    }
  }
}
