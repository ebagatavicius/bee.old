package com.butent.bee.client.event.logical;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.Autocomplete;
import com.butent.bee.shared.Consumable;
import com.butent.bee.shared.State;

import java.util.Collection;

public final class AutocompleteEvent extends GwtEvent<AutocompleteEvent.Handler>
    implements Consumable {

  public interface Handler extends EventHandler {
    void onDataSelector(AutocompleteEvent event);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fire(Autocomplete selector, State state) {
    fireEvent(selector, new AutocompleteEvent(state));
  }

  public static AutocompleteEvent fireExclusions(Autocomplete selector,
      Collection<String> exclusions) {
    AutocompleteEvent event = new AutocompleteEvent(State.UPDATING);
    event.setExclusions(exclusions);
    fireEvent(selector, event);
    return event;
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  public static HandlerRegistration register(Autocomplete selector, Handler handler) {
    return BeeKeeper.getBus().addHandlerToSource(getType(), selector, handler, false);
  }

  public static HandlerRegistration register(Handler handler) {
    return BeeKeeper.getBus().addHandler(getType(), handler, false);
  }

  private static void fireEvent(Autocomplete selector, AutocompleteEvent event) {
    selector.fireEvent(event);

    if (!event.isConsumed()) {
      event.revive();
      BeeKeeper.getBus().fireEventFromSource(event, selector);
    }
  }

  private final State state;
  private Collection<String> exclusions;
  private boolean consumed;

  private AutocompleteEvent(State state) {
    super();
    this.state = state;
  }

  @Override
  public void consume() {
    setConsumed(true);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public Collection<String> getExclusions() {
    return exclusions;
  }

  public Autocomplete getSelector() {
    if (getSource() instanceof Autocomplete) {
      return (Autocomplete) getSource();
    } else {
      return null;
    }
  }

  public State getState() {
    return state;
  }

  public String getValue() {
    return getSelector().getNormalizedValue();
  }

  public String getViewName() {
    return (getSelector() == null) ? null : getSelector().getOracle().getViewName();
  }

  public boolean isCanceled() {
    return State.CANCELED.equals(getState());
  }

  public boolean isChanged() {
    return State.CHANGED.equals(getState());
  }

  public boolean isClosed() {
    return State.CLOSED.equals(getState());
  }

  @Override
  public boolean isConsumed() {
    return consumed;
  }

  public boolean isDataLoaded() {
    return State.LOADED.equals(getState());
  }

  public boolean isExclusions() {
    return State.UPDATING.equals(getState());
  }

  public boolean isOpened() {
    return State.OPEN.equals(getState());
  }

  public boolean isUnloading() {
    return State.UNLOADING.equals(getState());
  }

  @Override
  public void setConsumed(boolean consumed) {
    this.consumed = consumed;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onDataSelector(this);
  }

  private void setExclusions(Collection<String> exclusions) {
    this.exclusions = exclusions;
  }
}
