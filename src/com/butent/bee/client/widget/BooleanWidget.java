package com.butent.bee.client.widget;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasValue;

import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.ui.HasCheckedness;

/**
 * Determines which classes have to be extended for boolean widget implementing classes.
 */

public interface BooleanWidget extends Focusable, HasValue<Boolean>, IdentifiableWidget,
    HasClickHandlers, EnablableWidget, HasCheckedness {
}
