package com.butent.bee.client.presenter;

import com.butent.bee.client.BeeKeeper;

public interface PresenterCallback {

  PresenterCallback SHOW_IN_ACTIVE_PANEL = new PresenterCallback() {
    @Override
    public void onCreate(Presenter presenter) {
      if (presenter != null) {
        BeeKeeper.getScreen().updateActivePanel(presenter.getWidget());
      }
    }
  };

  PresenterCallback SHOW_IN_NEW_TAB = new PresenterCallback() {
    @Override
    public void onCreate(Presenter presenter) {
      if (presenter != null) {
        BeeKeeper.getScreen().showInNewPlace(presenter.getWidget());
      }
    }
  };

  void onCreate(Presenter presenter);
}
