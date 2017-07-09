package com.butent.bee.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.animation.HasAnimatableActivity;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;

import java.util.Collection;
import java.util.Set;

/**
 * Contains requirements for data header implementing classes.
 */

public interface HeaderView extends View, IndexedPanel, Printable, HasClickHandlers {

  void create(String caption, boolean hasData, boolean readOnly, String viewName,
      Collection<UiOption> options, Set<Action> enabledActions, Set<Action> disabledActions,
      Set<Action> hiddenActions);

  void addCaptionStyle(String style);

  void addCommandItem(IdentifiableWidget widget);

  void addCommandItem(HasAnimatableActivity widget, int duration);

  void clearCommandPanel();

  boolean enableCommandByStyleName(String styleName, boolean enable);

  Widget getActionWidget(Action action);

  Widget getCommandByStyleName(String styleName);

  int getHeight();

  String getRowMessage();

  boolean hasAction(Action action);

  boolean hasCommands();

  boolean insertControl(Widget w, int beforeIndex);

  boolean isActionEnabled(Action action);

  boolean isActionOrCommand(Element target);

  void removeCaptionStyle(String style);

  boolean removeCommandByStyleName(String styleName);

  void setActionHandler(HandlesActions actionHandler);

  void setCaption(String caption);

  void setCaptionTitle(String title);

  void setHeight(int height);

  void setMessage(int index, String message, String styleName);

  void showAction(Action action, boolean visible);

  void showReadOnly(boolean readOnly);

  void showRowId(IsRow row);

  void showRowMessage(Evaluator evaluator, IsRow row);

  boolean startCommandByStyleName(String styleName, int duration);

  boolean stopAction(Action action);

  default boolean stopCommandByStyleName(String styleName) {
    return stopCommandByStyleName(styleName, false);
  }

  boolean stopCommandByStyleName(String styleName, boolean disableAnimation);
}
