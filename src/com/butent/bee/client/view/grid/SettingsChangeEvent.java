package com.butent.bee.client.view.grid;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import com.butent.bee.client.ui.HasDimensions;
import com.butent.bee.shared.utils.BeeUtils;

public final class SettingsChangeEvent extends GwtEvent<SettingsChangeEvent.Handler> {

  public interface Handler extends EventHandler {
    void onSettingsChange(SettingsChangeEvent event);
  }

  public interface HasSettingsChangeHandlers extends HasHandlers {
    HandlerRegistration addSettingsChangeHandler(Handler handler);
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static void fireHeight(HasSettingsChangeHandlers source, ComponentType componentType,
      int height) {
    source.fireEvent(new SettingsChangeEvent(componentType, null, HasDimensions.ATTR_HEIGHT,
        BeeUtils.toString(height)));
  }

  public static void fireWidth(HasSettingsChangeHandlers source, String columnName, int width) {
    source.fireEvent(new SettingsChangeEvent(null, columnName, HasDimensions.ATTR_WIDTH,
        BeeUtils.toString(width)));
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final ComponentType componentType;
  private final String columnName;

  private final String attribute;
  private final String value;

  private SettingsChangeEvent(ComponentType componentType, String columnName, String attribute,
      String value) {
    super();
    this.componentType = componentType;
    this.columnName = columnName;
    this.attribute = attribute;
    this.value = value;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getColumnName() {
    return columnName;
  }

  public ComponentType getComponentType() {
    return componentType;
  }

  public String getValue() {
    return value;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onSettingsChange(this);
  }
}
