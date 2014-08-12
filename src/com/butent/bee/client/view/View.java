package com.butent.bee.client.view;

import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.ui.Action;

public interface View extends IdentifiableWidget, EnablableWidget, ReadyEvent.HasReadyHandlers {

  Presenter getViewPresenter();

  String getWidgetId();

  boolean reactsTo(Action action);

  void setViewPresenter(Presenter viewPresenter);
}
