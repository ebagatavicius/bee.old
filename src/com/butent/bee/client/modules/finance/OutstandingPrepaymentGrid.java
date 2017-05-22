package com.butent.bee.client.modules.finance;

import static com.butent.bee.shared.modules.finance.FinanceConstants.FILTER_OUTSTANDING_PREPAYMENT;

import com.butent.bee.client.data.Provider;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.modules.finance.PrepaymentKind;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OutstandingPrepaymentGrid extends PrepaymentGrid {

  OutstandingPrepaymentGrid(PrepaymentKind kind) {
    super(kind);
  }

  @Override
  public GridInterceptor getInstance() {
    return new OutstandingPrepaymentGrid(getKind());
  }

  @Override
  public Map<String, Filter> getInitialParentFilters(Collection<UiOption> uiOptions) {
    return Provider.createDefaultParentFilters(Filter.isFalse());
  }

  public void onParentChange(Long company, Long currency) {
    GridPresenter presenter = getGridPresenter();

    if (presenter != null
        && presenter.getDataProvider().setDefaultParentFilter(buildFilter(company, currency))) {

      presenter.handleAction(Action.REFRESH);
    }
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
