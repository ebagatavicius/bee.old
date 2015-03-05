package com.butent.bee.client.ui;

import com.google.gwt.user.client.ui.IsWidget;

import com.butent.bee.shared.IsUnique;

public interface HasProgress extends IsWidget, IsUnique {
  void update(double value);

  void update(String label, double value);
}
