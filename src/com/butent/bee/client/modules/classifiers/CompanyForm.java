package com.butent.bee.client.modules.classifiers;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Map;

public class CompanyForm extends AbstractFormInterceptor {

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
          setDefault.setTitle(Localized.getConstants().setAsPrimary());
          setDefault.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
              GridView gridView = getGridPresenter().getGridView();

              IsRow selectedRow = gridView.getActiveRow();
              if (selectedRow == null) {
                gridView.notifyWarning(Localized.getConstants().selectAtLeastOneRow());
                return;
              } else {
                setAsPrimary(selectedRow.getId());
              }
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
                  Localized.getConstants().newCompanyPerson(), dataInfo, newRow, null,
                  new AbstractFormInterceptor() {
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
                  },
                  new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow result) {
                      Data.onViewChange(viewName, DataChangeEvent.CANCEL_RESET_REFRESH);
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
  public void afterRefresh(FormView form, IsRow row) {
    refreshCreditInfo();
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyForm();
  }

  private void refreshCreditInfo() {
    final FormView form = getFormView();
    final Widget widget = form.getWidgetByName(SVC_CREDIT_INFO);

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
          Map<String, String> result = Codec.deserializeMap(response.getResponseAsString());

          if (!BeeUtils.isEmpty(result)) {
            HtmlTable table = new HtmlTable();
            table.setColumnCellStyles(1, "text-align:right; font-weight:bold;color:red;");
            int c = 0;

            String amount = result.get(VAR_DEBT);

            if (BeeUtils.isPositiveDouble(amount)) {
              table.setHtml(c, 0, Localized.getConstants().trdDebt());
              table.setHtml(c, 1, amount);
              double limit = BeeUtils.toDouble(result.get(COL_COMPANY_CREDIT_LIMIT));

              if (BeeUtils.toDouble(amount) <= limit) {
                StyleUtils.setColor(table.getCellFormatter().getElement(c, 1), "black");
              }
              c++;
            }
            amount = result.get(VAR_OVERDUE);

            if (BeeUtils.isPositiveDouble(amount)) {
              table.setHtml(c, 0, Localized.getConstants().trdOverdue());
              table.setHtml(c, 1, amount);
            }
            widget.getElement().setInnerHTML(table.getElement().getString());
          }
        }
      });
    }
  }
}
