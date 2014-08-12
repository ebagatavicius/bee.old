package com.butent.bee.client.screen;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Place;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.TabbedPages.SelectionOrigin;
import com.butent.bee.client.screen.TilePanel.Tile;
import com.butent.bee.client.ui.HandlesHistory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;

class Workplace extends Place {

  private static final BeeLogger logger = LogUtils.getLogger(Workplace.class);

  private final Workspace workspace;

  Workplace(Workspace workspace, String id) {
    super(id);
    this.workspace = Assert.notNull(workspace);
  }

  @Override
  public boolean activate() {
    Tile tile = getTile();
    if (tile == null) {
      return false;
    }

    if (DomUtils.idEquals(workspace.getActiveTile(), getId())) {
      return true;
    }

    int index = workspace.getPageIndex(tile);
    workspace.selectPage(index, SelectionOrigin.SCRIPT);

    tile.activate(false);

    return true;
  }

  @Override
  public boolean onHistory(Place place, boolean forward) {
    Tile tile = getTile();
    if (tile == null) {
      return false;
    }

    IdentifiableWidget content = tile.getContent();
    if (content instanceof HandlesHistory) {
      boolean ok = ((HandlesHistory) content).onHistory(place, forward);
      if (ok) {
        return ok;
      }
    }

    return tile.onHistory(place, forward);
  }

  private Tile getTile() {
    Widget child = DomUtils.getChildQuietly(workspace, getId());
    if (child instanceof Tile) {
      return (Tile) child;
    } else {
      logger.warning("workplace", getId(), "not found");
      return null;
    }
  }
}
