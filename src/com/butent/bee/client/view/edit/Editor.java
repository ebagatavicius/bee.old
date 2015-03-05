package com.butent.bee.client.view.edit;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.user.client.ui.Focusable;

import com.butent.bee.client.dialog.TabulationHandler;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasStringValue;

import java.util.List;

/**
 * Contains requirements for user interface components which are able to edit data values.
 */

public interface Editor extends IdentifiableWidget, HasStringValue, Focusable,
    HasAllFocusHandlers, HasEditChangeHandlers, HasEditState, HasEditStopHandlers,
    EnablableWidget, HasOptions, TabulationHandler, HasSummaryChangeHandlers {

  void clearValue();

  EditorAction getDefaultFocusAction();

  String getNormalizedValue();

  FormWidget getWidgetType();

  boolean handlesKey(int keyCode);

  boolean isNullable();

  boolean isOrHasPartner(Node node);

  void normalizeDisplay(String normalizedValue);

  void render(String value);

  void setNullable(boolean nullable);

  void startEdit(String oldValue, char charCode, EditorAction onEntry, Element sourceElement);

  List<String> validate(boolean checkForNull);

  List<String> validate(String normalizedValue, boolean checkForNull);
}
