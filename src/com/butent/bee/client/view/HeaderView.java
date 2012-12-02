package com.butent.bee.client.view;

import com.google.gwt.user.client.ui.IndexedPanel;

import com.butent.bee.client.output.Printable;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasCaption;

import java.util.Collection;
import java.util.Set;

/**
 * Contains requirements for data header implementing classes.
 */

public interface HeaderView extends View, IndexedPanel, Printable, HasCaption {

  void create(String caption, boolean hasData, boolean readOnly, Collection<UiOption> options,
      Set<Action> enabledActions, Set<Action> disabledActions);

  void addCaptionStyle(String style);

  void addCommandItem(IdentifiableWidget widget);  

  void clearCommandPanel();  
  
  int getHeight();

  boolean hasAction(Action action);

  boolean isActionEnabled(Action action);

  void removeCaptionStyle(String style);

  void setCaption(String caption);

  void setMessage(String message);

  void showAction(Action action, boolean visible);
}
