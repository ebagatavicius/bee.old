package com.butent.bee.client.maps;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasWidgetSupplier;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class MapContainer extends Flow implements Presenter, View, HasWidgetSupplier {

  private static final BeeLogger logger = LogUtils.getLogger(MapContainer.class);

  private static final String STYLE_NAME = StyleUtils.CLASS_NAME_PREFIX + "MapContainer";

  private final HeaderView headerView;

  private boolean enabled = true;

  public MapContainer(String caption, MapWidget mapWidget) {
    super(STYLE_NAME);

    this.headerView = new HeaderImpl();
    headerView.create(caption, false, true, null, EnumSet.of(UiOption.ROOT),
        EnumSet.of(Action.CLOSE), Action.NO_ACTIONS, Action.NO_ACTIONS);

    headerView.setViewPresenter(this);
    add(headerView);

    add(mapWidget);
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    ReadyEvent.maybeDelegate(this);
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public String getCaption() {
    return headerView.getCaption();
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public String getIdPrefix() {
    return "map-container";
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public String getSupplierKey() {
    MapWidget mapWidget = getMapWidget();
    List<String> values = (mapWidget == null) ? null : mapWidget.getValues();

    if (BeeUtils.isEmpty(values)) {
      return null;

    } else {
      List<String> data = new ArrayList<>();
      data.add(getCaption());
      data.addAll(values);

      return ViewFactory.SupplierKind.MAP.getKey(Codec.beeSerialize(data));
    }
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case CANCEL:
      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public boolean reactsTo(Action action) {
    return EnumUtils.in(action, Action.CANCEL, Action.CLOSE);
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  private MapWidget getMapWidget() {
    for (Widget widget : this) {
      if (widget instanceof MapWidget) {
        return (MapWidget) widget;
      }
    }
    return null;
  }
}
