package com.butent.bee.client.modules.tasks;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.shared.data.filter.ColumnNotNullFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

public class RequestsGridInterceptor extends AbstractGridInterceptor {

  private static final String REGISTRED_WIDGET_NAME = "Registred";
  private static final String FINISHED_WIDGET_NAME = "Finished";

  private InputBoolean registred;
  private InputBoolean finished;

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    presenter.handleAction(Action.REFRESH);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof InputBoolean) {
      InputBoolean w = (InputBoolean) widget;

      if (BeeUtils.same(name, REGISTRED_WIDGET_NAME)) {
        registred = w;
      } else if (BeeUtils.same(name, FINISHED_WIDGET_NAME)) {
        finished = w;
      } else {
        w = null;
      }
      if (w != null) {
        w.addValueChangeHandler(new ValueChangeHandler<String>() {
          @Override
          public void onValueChange(ValueChangeEvent<String> event) {
            getGridPresenter().handleAction(Action.REFRESH);
          }
        });
      }
    }
  }

  @Override
  public void beforeRefresh(GridPresenter presenter) {
    presenter.getDataProvider().setParentFilter("CustomFilter", getFilter());
  }

  @Override
  public GridInterceptor getInstance() {
    return new RequestsGridInterceptor();
  }

  private Filter getFilter() {
    Filter filter = null;

    if (registred != null && BooleanValue.unpack(registred.getValue())) {
      filter = ColumnNotNullFilter.notNull(TaskConstants.COL_REQUEST_DATE);
    }
    if (finished == null || !BooleanValue.unpack(finished.getValue())) {
      filter = Filter.and(filter, Filter.isNull(TaskConstants.COL_REQUEST_FINISHED));
    }
    if (finished != null && BooleanValue.unpack(finished.getValue())) {
      filter = Filter.or(filter, Filter.isNot(Filter.isNull(TaskConstants.COL_REQUEST_FINISHED)));
    }
    return filter;
  }
}
