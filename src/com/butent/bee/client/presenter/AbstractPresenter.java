package com.butent.bee.client.presenter;

public abstract class AbstractPresenter implements Presenter {

  private String eventSource;

  @Override
  public String getEventSource() {
    return eventSource;
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void setEventSource(String eventSource) {
    this.eventSource = eventSource;
  }
}
