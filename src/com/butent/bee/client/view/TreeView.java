package com.butent.bee.client.view;

import com.google.gwt.event.logical.shared.HasSelectionHandlers;

import com.butent.bee.client.event.logical.CatchEvent;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.shared.HasState;
import com.butent.bee.shared.Launchable;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.HasCaption;

import java.util.Collection;
import java.util.List;

public interface TreeView extends View, HasSelectionHandlers<IsRow>,
    CatchEvent.HasCatchHandlers<IsRow>, HasCaption, HasViewName, HasState, Launchable {

  void addItem(Long parentId, String text, IsRow item, boolean focus);

  void afterRequery();

  Collection<IsRow> getChildItems(IsRow item, boolean recurse);

  List<String> getFavorite();

  IsRow getParentItem(IsRow item);

  List<IsRow> getPath(Long id);

  List<String> getPathLabels(Long id, String colName);

  IsRow getSelectedItem();

  TreePresenter getTreePresenter();

  void removeItem(IsRow item);

  void removeItems();

  void updateItem(String text, IsRow item);
}
