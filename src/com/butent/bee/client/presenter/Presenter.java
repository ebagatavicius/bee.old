package com.butent.bee.client.presenter;

import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public interface Presenter extends HandlesActions, HasCaption {

  String getEventSource();

  HeaderView getHeader();

  View getMainView();

  default String getRowMessage() {
    HeaderView header = getHeader();
    return (header == null) ? null : header.getRowMessage();
  }

  default String getRowMessageOrCaption() {
    String rowMessage = getRowMessage();
    return BeeUtils.isEmpty(rowMessage) ? getCaption() : rowMessage;
  }

  String getViewKey();

  void onViewUnload();

  void setEventSource(String eventSource);
}
