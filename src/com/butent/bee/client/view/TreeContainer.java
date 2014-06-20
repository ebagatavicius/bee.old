package com.butent.bee.client.view;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.CatchEvent;
import com.butent.bee.client.event.logical.CatchEvent.CatchHandler;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.client.tree.HasTreeItems;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TreeContainer extends Flow implements TreeView, SelectionHandler<TreeItem>,
    CatchEvent.CatchHandler<TreeItem> {

  private final class ActionListener extends Command {
    private final Action action;

    private ActionListener(Action action) {
      super();
      this.action = action;
    }

    @Override
    public void execute() {
      if (getViewPresenter() != null) {
        getViewPresenter().handleAction(action);
      }
    }
  }

  private static final String STYLE_NAME = "bee-TreeView";

  private Presenter viewPresenter;
  private boolean enabled = true;
  private final List<String> favorite = Lists.newArrayList();
  private final Tree tree;
  private final Map<Long, TreeItem> items = Maps.newHashMap();

  private final String caption;
  private final boolean hasDnD;

  public TreeContainer(String caption, boolean hideActions, String viewName) {
    this(caption, hideActions, viewName, BeeConst.STRING_EMPTY);
  }

  public TreeContainer(String caption, boolean hideActions, String viewName, String favorite) {
    super();
    addStyleName(STYLE_NAME);

    this.caption = caption;

    if (!hideActions) {
      boolean editable = BeeKeeper.getUser().canEditData(viewName);
      this.hasDnD = editable;

      Flow hdr = new Flow();
      hdr.addStyleName(STYLE_NAME + "-actions");

      boolean bookmarkable = !BeeUtils.isEmpty(favorite)
          && BeeKeeper.getScreen().getUserInterface().hasComponent(Component.FAVORITES);

      Image img;

      if (editable && BeeKeeper.getUser().canCreateData(viewName)) {
        img = new Image(Global.getImages().silverAdd(), new ActionListener(Action.ADD));
        img.addStyleName(STYLE_NAME + "-add");
        img.setTitle(Action.ADD.getCaption());
        hdr.add(img);
      }

      if (editable && BeeKeeper.getUser().canDeleteData(viewName)) {
        img = new Image(Global.getImages().silverDelete(), new ActionListener(Action.DELETE));
        img.addStyleName(STYLE_NAME + "-delete");
        img.setTitle(Action.DELETE.getCaption());
        hdr.add(img);
      }

      if (bookmarkable) {
        setFavorite(NameUtils.toList(favorite));

        img = new Image(Global.getImages().silverBookmarkAdd(),
            new ActionListener(Action.BOOKMARK));
        img.addStyleName(STYLE_NAME + "-bookmark");
        img.setTitle(Action.BOOKMARK.getCaption());
        hdr.add(img);
      }

      if (editable) {
        img = new Image(Global.getImages().silverEdit(), new ActionListener(Action.EDIT));
        img.addStyleName(STYLE_NAME + "-edit");
        img.setTitle(Action.EDIT.getCaption());
        hdr.add(img);
      }

      img = new Image(Global.getImages().silverReload(), new ActionListener(Action.REFRESH));
      img.addStyleName(STYLE_NAME + "-refresh");
      img.setTitle(Action.REFRESH.getCaption());
      hdr.add(img);

      add(hdr);
    } else {
      this.hasDnD = false;
    }
    this.tree = new Tree(caption);
    add(tree);

    getTree().addStyleName(STYLE_NAME + "-tree");
    getTree().addSelectionHandler(this);

    if (hasDnD) {
      getTree().addCatchHandler(this);
    }
  }

  @Override
  public HandlerRegistration addCatchHandler(CatchHandler<IsRow> handler) {
    return addHandler(handler, CatchEvent.getType());
  }

  @Override
  public void addItem(Long parentId, String text, IsRow item, boolean focus) {
    Assert.notNull(item);
    long id = item.getId();
    Assert.state(!items.containsKey(id), "Item already exists in a tree: " + id);

    TreeItem treeItem = new TreeItem(text, item);

    if (hasDnD) {
      treeItem.makeDraggable();
    }
    items.put(id, treeItem);

    HasTreeItems parentItem = items.containsKey(parentId) ? items.get(parentId) : getTree();
    parentItem.addItem(treeItem);

    if (focus) {
      getTree().setSelectedItem(treeItem);
      getTree().ensureSelectedItemVisible();
    }
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<IsRow> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public Collection<IsRow> getChildItems(IsRow item, boolean recurse) {
    Assert.notNull(item);
    Assert.contains(items, item.getId());

    TreeItem treeItem = items.get(item.getId());
    Collection<IsRow> childs = Lists.newArrayList();

    if (treeItem.getChildCount() > 0) {
      for (int i = 0; i < treeItem.getChildCount(); i++) {
        TreeItem childTree = treeItem.getChild(i);
        IsRow child = (IsRow) childTree.getUserObject();
        childs.add(child);

        if (recurse) {
          childs.addAll(getChildItems(child, recurse));
        }
      }
    }
    return childs;
  }

  @Override
  public List<String> getFavorite() {
    return favorite;
  }

  @Override
  public IsRow getParentItem(IsRow item) {
    Assert.notNull(item);
    Assert.contains(items, item.getId());

    TreeItem treeItem = items.get(item.getId());

    if (treeItem.getParentItem() != null) {
      return (IsRow) treeItem.getParentItem().getUserObject();
    }
    return null;
  }

  @Override
  public List<IsRow> getPath(Long id) {
    Assert.notNull(id);

    List<IsRow> path = new ArrayList<>();

    TreeItem item = items.get(id);

    while (item != null && item.getUserObject() instanceof IsRow) {
      path.add((IsRow) item.getUserObject());
      item = item.getParentItem();
    }

    if (path.size() > 1) {
      Collections.reverse(path);
    }
    return path;
  }

  @Override
  public List<String> getPathLabels(Long id, String colName) {
    Assert.notNull(id);
    Assert.notEmpty(colName);

    List<String> labels = new ArrayList<>();

    List<IsRow> path = getPath(id);
    int index = Data.getColumnIndex(getViewName(), colName);

    if (!BeeUtils.isEmpty(path) && !BeeConst.isUndef(index)) {
      for (IsRow row : path) {
        labels.add(row.getString(index));
      }
    }
    return labels;
  }

  @Override
  public IsRow getSelectedItem() {
    TreeItem selected = getTree().getSelectedItem();
    if (selected == null || !(selected.getUserObject() instanceof IsRow)) {
      return null;
    }
    return (IsRow) selected.getUserObject();
  }

  @Override
  public TreePresenter getTreePresenter() {
    if (viewPresenter instanceof TreePresenter) {
      return (TreePresenter) viewPresenter;
    }
    return null;
  }

  @Override
  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onCatch(final CatchEvent<TreeItem> event) {
    final boolean isConsumable = !event.isConsumed();

    if (isConsumable) {
      event.consume();
    }
    TreeItem destination = event.getDestination();

    CatchEvent<IsRow> catchEvent = CatchEvent.fire(this, (IsRow) event.getPacket().getUserObject(),
        destination == null ? null : (IsRow) destination.getUserObject(),
        new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            if (isConsumable) {
              event.executeScheduled();
            }
          }
        });

    if (!catchEvent.isConsumed()) {
      catchEvent.consume();
      catchEvent.executeScheduled();
    }
  }

  @Override
  public void onSelection(SelectionEvent<TreeItem> event) {
    IsRow item = null;

    if (event == null) {
      getTree().setSelectedItem(null);

    } else if (event.getSelectedItem() != null) {
      item = (IsRow) event.getSelectedItem().getUserObject();
    }
    SelectionEvent.fire(this, item);
  }

  @Override
  public void removeItem(IsRow item) {
    Assert.notNull(item);
    Assert.contains(items, item.getId());

    TreeItem treeItem = items.get(item.getId());

    if (treeItem.isSelected()) {
      onSelection(null);
    }
    removeFromCache(treeItem);
    treeItem.remove();
  }

  @Override
  public void removeItems() {
    getTree().removeItems();
    items.clear();
    onSelection(null);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (enabled == isEnabled()) {
      return;
    }
    this.enabled = enabled;
    DomUtils.enableChildren(this, enabled);
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  @Override
  public void updateItem(String text, IsRow item) {
    Assert.notNull(item);
    Assert.contains(items, item.getId());

    TreeItem treeItem = items.get(item.getId());

    treeItem.setHtml(text);
    treeItem.setUserObject(item);

    if (treeItem.isSelected()) {
      getTree().setSelectedItem(treeItem);
    }
  }

  private void setFavorite(List<String> favorite) {
    BeeUtils.overwrite(this.favorite, favorite);
  }

  private Tree getTree() {
    return tree;
  }

  private void removeFromCache(TreeItem item) {
    for (int i = 0; i < item.getChildCount(); i++) {
      removeFromCache(item.getChild(i));
    }
    items.remove(((IsRow) item.getUserObject()).getId());
  }

  @Override
  public String getViewName() {
    return (getTreePresenter() == null) ? null : getTreePresenter().getViewName();
  }
}
