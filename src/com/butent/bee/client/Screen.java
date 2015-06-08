package com.butent.bee.client;

import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.Workspace;
import com.butent.bee.client.ui.HasProgress;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.ui.UserInterface;

import java.util.List;
import java.util.Set;

/**
 * manages the main browser window and it's main containing elements (f.e. panels).
 */

public interface Screen extends NotificationListener, HasExtendedInfo {

  boolean activateDomainEntry(Domain domain, Long key);

  void activateWidget(IdentifiableWidget widget);

  void addCommandItem(IdentifiableWidget widget);

  void addDomainEntry(Domain domain, IdentifiableWidget widget, Long key, String caption);

  String addProgress(HasProgress widget);

  void closeAll();

  void closeWidget(IdentifiableWidget widget);

  boolean containsDomainEntry(Domain domain, Long key);

  int getActivePanelHeight();

  int getActivePanelWidth();

  IdentifiableWidget getActiveWidget();

  HasWidgets getCommandPanel();

  Flow getDomainHeader(Domain domain, Long key);

  int getHeight();

  Set<Direction> getHiddenDirections();

  List<IdentifiableWidget> getOpenWidgets();

  Split getScreenPanel();

  UserInterface getUserInterface();

  int getWidth();

  Workspace getWorkspace();

  void hideDirections(Set<Direction> directions);

  void init();

  void onLoad();

  void onWidgetChange(IdentifiableWidget widget);

  boolean removeDomainEntry(Domain domain, Long key);

  void removeProgress(String id);

  void restore(List<String> spaces, boolean append);

  String serialize();

  void show(IdentifiableWidget widget);

  void showInNewPlace(IdentifiableWidget widget);

  void start(UserData userData);

  void updateActivePanel(IdentifiableWidget widget);

  void updateCommandPanel(IdentifiableWidget widget);

  void updateMenu(IdentifiableWidget widget);

  void updateNewsSize(int size);

  boolean updateProgress(String id, String label, double value);

  void updateUserData(UserData userData);

  FaLabel getOnlineUserLabel();

  Flow getOnlineUserSize();

  Flow getOnlineEmailSize();

  FaLabel getOnlineEmailLabel();
}
