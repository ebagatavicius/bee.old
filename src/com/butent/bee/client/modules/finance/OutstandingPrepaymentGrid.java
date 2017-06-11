package com.butent.bee.client.modules.finance;

import static com.butent.bee.shared.modules.finance.FinanceConstants.FILTER_OUTSTANDING_PREPAYMENT;

import com.butent.bee.client.data.Provider;
import com.butent.bee.client.event.logical.RowCountChangeEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.modules.trade.TradeKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class OutstandingPrepaymentGrid extends PrepaymentGrid {

  private static final String STYLE_COMMAND_DISCHARGE =
      TradeKeeper.STYLE_PREFIX + "prepayment-command-discharge";

  private BiConsumer<GridView, PrepaymentKind> discharger;

  OutstandingPrepaymentGrid(PrepaymentKind kind) {
    super(kind);
  }

  @Override
  public GridInterceptor getInstance() {
    return new OutstandingPrepaymentGrid(getKind());
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    HeaderView header = (presenter == null) ? null : presenter.getHeader();
    if (header != null) {
      Button discharge = new Button(Localized.dictionary().paymentDischargePrepaymentCommand());
      discharge.addStyleName(STYLE_COMMAND_DISCHARGE);

      discharge.addClickHandler(event -> {
        if (discharger != null) {
          GridView gridView = getGridView();
          if (gridView != null && !gridView.isEmpty()) {
            discharger.accept(gridView, getKind());
          }
        }
      });

      header.addCommandItem(discharge);
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    return Provider.createDefaultParentFilters(Filter.isFalse());
  }

  public void onParentChange(Long company, Long currency) {
    GridPresenter presenter = getGridPresenter();

    if (presenter != null
        && presenter.getDataProvider().setDefaultParentFilter(buildFilter(company, currency))) {

      getGridView().getGrid().clearSelection();
      presenter.handleAction(Action.REFRESH);
    }
  }

  @Override
  public void onLoad(GridView gridView) {
    gridView.getGrid().addMutationHandler(e -> SummaryChangeEvent.maybeFire(gridView));
    gridView.getGrid().addSelectionCountChangeHandler(e -> SummaryChangeEvent.maybeFire(gridView));

    super.onLoad(gridView);
  }

  @Override
  public boolean onRowCountChange(GridView gridView, RowCountChangeEvent event) {
    return false;
  }

  public void setDischarger(BiConsumer<GridView, PrepaymentKind> discharger) {
    this.discharger = discharger;
  }

  private Filter buildFilter(Long company, Long currency) {
    if (DataUtils.isId(company)) {
      List<String> args = Arrays.asList(Codec.pack(getKind()),
          BeeUtils.toStringOrNull(company), BeeUtils.toStringOrNull(currency));

      return Filter.custom(FILTER_OUTSTANDING_PREPAYMENT, args);
    } else {
      return Filter.isFalse();
    }
  }
}
