package com.butent.bee.client.ui;

import com.google.common.collect.Maps;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

public class WidgetCreationCallback implements FormFactory.WidgetDescriptionCallback {

  private static final BeeLogger logger = LogUtils.getLogger(WidgetCreationCallback.class);

  private static final String PARENT_IS_FORM = "{form}";
  
  private final Map<String, String> namedWidgets = Maps.newHashMap();
  private final Map<String, String> potentialChildren = Maps.newHashMap();

  private WidgetDescription lastWidgetDescription;
  
  public WidgetCreationCallback() {
    super();
  }
  
  public void addBinding(String widgetName, String widgetId, String parentName) {
    Assert.notNull(widgetId);
    if (!BeeUtils.isEmpty(widgetName)) {
      namedWidgets.put(widgetName, widgetId);
    }
    if (!BeeUtils.isEmpty(parentName)) {
      potentialChildren.put(widgetId, parentName);
    }
  }

  public void bind(Widget root, String defaultId) {
    if (potentialChildren.isEmpty()) {
      return;
    }
    Assert.notNull(root);
    Assert.notNull(defaultId);

    for (Map.Entry<String, String> entry : potentialChildren.entrySet()) {
      Widget child = DomUtils.getChildQuietly(root, entry.getKey());
      if (child == null) {
        logger.severe("id:", entry.getKey(), "widget not found");
        continue;
      }

      if (child instanceof HasFosterParent) {
        String parentName = entry.getValue();

        String parentId;
        if (BeeUtils.isEmpty(parentName) || PARENT_IS_FORM.equalsIgnoreCase(parentName)) {
          parentId = defaultId;
        } else {
          parentId = namedWidgets.get(parentName);
        }

        if (BeeUtils.isEmpty(parentId)) {
          logger.severe("child id:", entry.getKey(), "parent name:", parentName, "not found");
        } else {
          ((HasFosterParent) child).setParentId(parentId);
        }
      }
    }
  }
  
  @Override
  public WidgetDescription getLastWidgetDescription() {
    return lastWidgetDescription; 
  }
  
  public String getWidgetIdByName(String name) {
    return namedWidgets.get(name);
  }

  @Override
  public void onFailure(Object... messages) {
    logger.severe(messages);
  }

  @Override
  public void onSuccess(WidgetDescription result, IdentifiableWidget widget) {
    lastWidgetDescription = result;
    
    String id = result.getWidgetId();
    if (!BeeUtils.isEmpty(id)) {
      if (!BeeUtils.isEmpty(result.getWidgetName())) {
        namedWidgets.put(result.getWidgetName(), id);
      }
      if (result.getWidgetType().isChild() || !BeeUtils.isEmpty(result.getParentName())) {
        potentialChildren.put(id, result.getParentName());
      }
    }
  }
}
