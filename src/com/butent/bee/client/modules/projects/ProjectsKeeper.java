package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.modules.trade.InvoicesGrid;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

/**
 * Client-side projects module handler.
 */
public final class ProjectsKeeper {

  public static final String STYLE_PREFIX = "bee-prj-";

  /**
   * Creates rpc parameters of projects module.
   * 
   * @param method name of method.
   * @return rpc parameters to call queries of server-side.
   */
  public static ParameterList createSvcArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.PROJECTS, method);
  }

  /**
   * Register projects client-side module handler.
   */
  public static void register() {
    /* Register grid handlers */
    GridFactory.registerGridInterceptor(GRID_ALL_PROJECTS, new AllProjectsGrid());
    GridFactory.registerGridInterceptor(GRID_PROJECTS, new ProjectsGrid());
    GridFactory.registerGridInterceptor(GRID_PROJECT_USERS, new ProjectUsersGrid());
    GridFactory.registerGridInterceptor(GRID_PROJECT_STAGES, new ProjectStagesGrid());
    GridFactory.registerGridInterceptor(GRID_PROJECT_INCOMES, new ProjectIncomesGrid());
    GridFactory.registerGridInterceptor(GRID_PROJECT_INVOICES, new InvoicesGrid() {

      private boolean erpConnectionActive;

      @Override
      public GridInterceptor getInstance() {
        return this;
      }

      @Override
      public void afterCreatePresenter(GridPresenter presenter) {
        if (isErpConnectionActive()) {
          super.afterCreatePresenter(presenter);
        }
      }

      @Override
      public void beforeCreateColumns(List<? extends IsColumn> dataColumns,
          final List<ColumnDescription> columnDescriptions) {

        Global.getParameter(AdministrationConstants.PRM_ERP_ADDRESS, new Consumer<String>() {

          @Override
          public void accept(String erpAddress) {
            setErpConnectionActive(!BeeUtils.isEmpty(erpAddress));

            for (ColumnDescription columnDescription : columnDescriptions) {
              if (BeeUtils.inListSame(columnDescription.getId(),
                  TradeConstants.COL_TRADE_PAYMENT_TIME,
                  TradeConstants.COL_TRADE_PAID)) {
                columnDescription.setEditInPlace(!isErpConnectionActive());
              }
            }
          }
        });
      }

      @Override
      public ColumnDescription beforeCreateColumn(GridView gridView,
          ColumnDescription columnDescription) {
        if (BeeUtils.inListSame(columnDescription.getId(),
            TradeConstants.COL_TRADE_PAYMENT_TIME,
            TradeConstants.COL_TRADE_PAID)) {
          columnDescription.setEditInPlace(!isErpConnectionActive());
        }
        return super.beforeCreateColumn(gridView, columnDescription);
      }

      private boolean isErpConnectionActive() {
        return erpConnectionActive;
      }

      private void setErpConnectionActive(boolean active) {
        this.erpConnectionActive = active;
      }

    });

    /* Register form handlers */
    FormFactory.registerFormInterceptor(FORM_PROJECT, new ProjectForm());
    FormFactory.registerFormInterceptor(FORM_PROJECT_STAGE, new ProjectStageForm());
  }

  private ProjectsKeeper() {

  }
}
