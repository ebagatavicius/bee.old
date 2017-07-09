package com.butent.bee.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.event.logical.CatchEvent;
import com.butent.bee.client.event.logical.CatchEvent.CatchHandler;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.TreePresenter;
import com.butent.bee.client.tree.HasTreeItems;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeContainer extends Flow implements TreeView, SelectionHandler<TreeItem>,
    CatchEvent.CatchHandler<TreeItem> {

  private final class ActionListener implements ClickHandler {
    private final Action action;

    private ActionListener(Action action) {
      this.action = action;
    }

    @Override
    public void onClick(ClickEvent event) {
      if (getViewPresenter() != null) {
        getViewPresenter().handleAction(action);
      }
    }
  }

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "TreeView";

  private Presenter viewPresenter;
  private boolean enabled = true;
  private final List<String> favorite = new ArrayList<>();
  private final Tree tree;
  private final Map<Long, TreeItem> items = new HashMap<>();

  private final String caption;
  private boolean hasDnD;

  private State state;

  private final Set<Action> enabledActions = new HashSet<>();

  public TreeContainer(String caption, Set<Action> disabledActions, String viewName,
      String favorite) {
    super(STYLE_NAME);

    this.caption = caption;

    Flow hdr = new Flow();
    hdr.addStyleName(STYLE_NAME + "-actions");

    if (!BeeUtils.isEmpty(favorite)
        && BeeKeeper.getScreen().getUserInterface().hasComponent(Component.FAVORITES)) {
      setFavorite(NameUtils.toList(favorite));

      hdr.add(createActionWidget(Action.BOOKMARK));
      enabledActions.add(Action.BOOKMARK);
    }
    if (BeeKeeper.getUser().canEditData(viewName)) {
      if (BeeKeeper.getUser().canCreateData(viewName) && !disabledActions.contains(Action.ADD)) {
        hdr.add(createActionWidget(Action.ADD));
        enabledActions.add(Action.ADD);
      }
      if (BeeKeeper.getUser().canDeleteData(viewName) && !disabledActions.contains(Action.DELETE)) {
        hdr.add(createActionWidget(Action.DELETE));
        enabledActions.add(Action.DELETE);
      }
      if (!disabledActions.contains(Action.EDIT)) {
        hdr.add(createActionWidget(Action.EDIT));
        enabledActions.add(Action.EDIT);
      }
      if (!disabledActions.contains(Action.MOVE)) {
        this.hasDnD = true;
        enabledActions.add(Action.MOVE);
      }
    }
    if (!disabledActions.contains(Action.REFRESH)) {
      hdr.add(createActionWidget(Action.REFRESH));
      enabledActions.add(Action.REFRESH);
    }
    if (!enabledActions.isEmpty()) {
      add(hdr);
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
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<IsRow> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  @Override
  public void afterRequery() {
    if (getState() == null) {
      setState(State.INITIALIZED);

      if (isAttached()) {
        ReadyEvent.fire(this);
      }
    }
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
    Collection<IsRow> childs = new ArrayList<>();

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
  public State getState() {
    return state;
  }

  @Override
  public TreePresenter getTreePresenter() {
    if (viewPresenter instanceof TreePresenter) {
      return (TreePresenter) viewPresenter;
    }
    return null;
  }

  @Override
  public String getViewName() {
    return (getTreePresenter() == null) ? null : getTreePresenter().getViewName();
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
  public void launch() {
    TreePresenter treePresenter = getTreePresenter();
    if (treePresenter != null) {
      treePresenter.launch();
    }
  }

  @Override
  public void onCatch(CatchEvent<TreeItem> event) {
    boolean isConsumable = !event.isConsumed();

    if (isConsumable) {
      event.consume();
    }
    TreeItem destination = event.getDestination();

    CatchEvent<IsRow> catchEvent = CatchEvent.fire(this, (IsRow) event.getPacket().getUserObject(),
        destination == null ? null : (IsRow) destination.getUserObject(), () -> {
          if (isConsumable) {
            event.executeScheduled();
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
    BeeKeeper.getBus().fireEventFromSource(new ParentRowEvent(getViewName(), item, item != null),
        getId());
  }

  @Override
  public boolean reactsTo(Action action) {
    return enabledActions.contains(action);
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
    UiHelper.enableChildren(this, enabled);
  }

  @Override
  public void setState(State state) {
    this.state = state;
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

  @Override
  protected void onLoad() {
    super.onLoad();

    if (getState() == State.INITIALIZED) {
      ReadyEvent.fire(this);
    }
  }

  private Widget createActionWidget(Action action) {
    FaLabel widget = new FaLabel(action.getIcon());

    widget.addStyleName(STYLE_NAME + "-action");
    widget.addStyleName(action.getStyleName());
    UiHelper.initActionWidget(action, widget);

    widget.addClickHandler(new ActionListener(action));

    return widget;
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

  private void setFavorite(List<String> favorite) {
    BeeUtils.overwrite(this.favorite, favorite);
  }
}
