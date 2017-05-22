package com.butent.bee.client;

import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.output.Printer;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;

import elemental.html.History;
import elemental.js.JsBrowser;

public final class Historian implements HasInfo {

  private enum BrowserHistoryState implements HasCaption {
    FIRST("-") {
      @Override
      int navigate(int pos, int size) {
        return BeeUtils.rotateBackwardExclusive(pos, 0, size);
      }

      @Override
      void reset() {
        getHistory().forward();
      }
    },

    REST("*") {
      @Override
      int navigate(int pos, int size) {
        return pos;
      }

      @Override
      void reset() {
      }
    },

    LAST("+") {
      @Override
      int navigate(int pos, int size) {
        return BeeUtils.rotateForwardExclusive(pos, 0, size);
      }

      @Override
      void reset() {
        getHistory().back();
      }
    };

    private final String caption;

    BrowserHistoryState(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    abstract int navigate(int pos, int size);

    abstract void reset();
  }

  private static final Historian INSTANCE = new Historian();

  private static final BeeLogger logger = LogUtils.getLogger(Historian.class);

  public static void add(Place place) {
    if (place != null) {
      getInstance().addItem(place);
    }
  }

  public static Historian getInstance() {
    return INSTANCE;
  }

  public static int getSize() {
    return getInstance().places.size();
  }

  public static void goTo(String id) {
    if (!BeeUtils.isEmpty(id)) {
      int index = getInstance().getIndex(id);
      if (!BeeConst.isUndef(index) && getInstance().getPosition() != index) {
        getInstance().setPosition(index);
      }
    }
  }

  public static void remove(String id) {
    if (!BeeUtils.isEmpty(id)) {
      getInstance().removeItem(id);
    }
  }

  static void start() {
    getInstance().init();
  }

  private static History getHistory() {
    return JsBrowser.getWindow().getHistory();
  }

  private final int initialHistoryLength;

  private final List<Place> places = new ArrayList<>();

  private int position = BeeConst.UNDEF;

  private Historian() {
    super();
    this.initialHistoryLength = getHistory().getLength();
  }

  public void clear() {
    places.clear();
    setPosition(BeeConst.UNDEF);
  }

  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(
        "Initial History Length", initialHistoryLength,
        "Browser History Length", getHistory().getLength(),
        "Browser History State", getHistory().getState(),
        "Places Size", places.size(),
        "Position", getPosition());

    for (int i = 0; i < places.size(); i++) {
      info.add(new Property("Place " + i, places.get(i).getId()));
    }
    return info;
  }

  public int getPosition() {
    return position;
  }

  public boolean isEmpty() {
    return places.isEmpty();
  }

  private void addItem(Place place) {
    int index = getIndex(place.getId());

    if (!isLast(index)) {
      if (index >= 0) {
        places.remove(index);
      }
      places.add(place);
    }
    setPosition(places.size() - 1);
  }

  private int getIndex(String id) {
    for (int i = 0; i < places.size(); i++) {
      if (BeeUtils.same(places.get(i).getId(), id)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private static BrowserHistoryState getState() {
    Object obj = getHistory().getState();
    if (obj == null) {
      return null;
    }

    String s = obj.toString().trim();
    for (BrowserHistoryState state : BrowserHistoryState.values()) {
      if (state.getCaption().equals(s)) {
        return state;
      }
    }
    return null;
  }

  private void init() {
    reset();

    JsBrowser.getWindow().setOnpopstate(evt -> Historian.this.onPop());

    logger.info("history initialized");
  }

  private boolean isLast(int index) {
    return index >= 0 && index == places.size() - 1;
  }

  private static boolean maybeClosePopup() {
    Popup popup = Popup.getActivePopup();
    if (popup == null) {
      return false;
    }

    if (popup.getOnEscape() == null) {
      popup.close();
    } else {
      if (popup.hideOnEscape()) {
        popup.close();
      }
      popup.getOnEscape().accept(null);
    }
    return true;
  }

  private void onPop() {
    BrowserHistoryState state = getState();
    if (state == null || BrowserHistoryState.REST.equals(state)) {
      return;
    }

    if (Printer.isPrinting()) {
      reset();
      return;
    }

    state.reset();

    if (maybeClosePopup()) {
      return;
    }

    if (BeeUtils.isIndex(places, getPosition())) {
      Place place = places.get(getPosition());
      if (place.onHistory(place, BrowserHistoryState.LAST.equals(state))) {
        return;
      }
    }

    if (places.size() > 1) {
      int size = places.size();
      int oldPos = getPosition();
      int newPos = state.navigate(oldPos, size);

      if (newPos != oldPos) {
        boolean ok = places.get(newPos).activate();
        if (ok) {
          setPosition(newPos);

        } else {
          int p = newPos;
          for (int i = 0; i < places.size() - 1; i++) {
            p = state.navigate(p, size);
            if (p != newPos && p != oldPos) {
              ok = places.get(p).activate();
              if (ok) {
                setPosition(p);
                break;
              }
            }
          }
        }
      }
    }
  }

  private boolean removeItem(String id) {
    int index = getIndex(id);

    if (index >= 0) {
      if (isLast(index)) {
        setPosition(index - 1);
      }
      places.remove(index);
      return true;

    } else {
      return false;
    }
  }

  private static void reset() {
    getHistory().replaceState(BrowserHistoryState.FIRST.getCaption(), null);
    getHistory().pushState(BrowserHistoryState.REST.getCaption(), null);
    getHistory().pushState(BrowserHistoryState.LAST.getCaption(), null);
    getHistory().back();
  }

  private void setPosition(int position) {
    this.position = position;
  }
}
