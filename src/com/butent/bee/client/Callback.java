package com.butent.bee.client;

public abstract class Callback<T> {

  public void onFailure(String... reason) {
    BeeKeeper.getScreen().notifySevere(reason);
  }

  public abstract void onSuccess(T result);
}
