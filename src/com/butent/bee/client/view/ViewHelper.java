package com.butent.bee.client.view;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;

public final class ViewHelper {
  
  private static final ImmutableSet<String> NO_EXCLUSIONS = ImmutableSet.of();

  public static Filter getFilter(HasSearch container, Provider dataProvider) {
    return getFilter(container, dataProvider, NO_EXCLUSIONS);
  }
  
  public static Filter getFilter(HasSearch container, Provider dataProvider,
      ImmutableSet<String> excludeSearchers) {
    Assert.notNull(container);
    Assert.notNull(dataProvider);

    Collection<SearchView> searchers = container.getSearchers();
    if (BeeUtils.isEmpty(searchers)) {
      return null;
    }

    List<Filter> filters = Lists.newArrayListWithCapacity(searchers.size());
    for (SearchView search : searchers) {
      Filter flt = search.getFilter(dataProvider.getColumns(), dataProvider.getIdColumnName(),
          dataProvider.getVersionColumnName(), excludeSearchers);
      if (flt != null && !filters.contains(flt)) {
        filters.add(flt);
      }
    }
    return Filter.and(filters);
  }

  public static Collection<GridView> getGrids(Widget root) {
    Collection<GridView> grids = Sets.newHashSet();

    if (root instanceof GridView) {
      grids.add((GridView) root);

    } else if (root instanceof HasGridView) {
      grids.add(((HasGridView) root).getGridView());
    
    } else if (root instanceof HasWidgets) {
      for (Widget child : (HasWidgets) root) {
        grids.addAll(getGrids(child));
      }

    } else if (root instanceof HasOneWidget) {
      grids.addAll(getGrids(((HasOneWidget) root).getWidget()));
    }
    
    return grids;
  }

  public static HeaderView getHeader(Widget container) {
    if (container instanceof HasWidgets) {
      for (Widget child : (HasWidgets) container) {
        if (child instanceof HeaderView) {
          return (HeaderView) child;
        }
      }
    }
    return null;
  }

  public static Collection<PagerView> getPagers(HasWidgets container) {
    Assert.notNull(container);
    Collection<PagerView> pagers = Sets.newHashSet();

    for (Widget widget : container) {
      if (widget instanceof PagerView) {
        pagers.add((PagerView) widget);
      } else if (widget instanceof HasNavigation) {
        Collection<PagerView> pc = ((HasNavigation) widget).getPagers();
        if (pc != null) {
          pagers.addAll(pc);
        }
      } else if (widget instanceof HasWidgets) {
        pagers.addAll(getPagers((HasWidgets) widget));
      }
    }
    return pagers;
  }
  
  public static PresenterCallback getPresenterCallback() {
    if (BeeKeeper.getUser().openInNewTab()) {
      return PresenterCallback.SHOW_IN_NEW_TAB;
    } else {
      return PresenterCallback.SHOW_IN_ACTIVE_PANEL;
    }
  }

  public static Collection<SearchView> getSearchers(HasWidgets container) {
    Assert.notNull(container);
    Collection<SearchView> searchers = Sets.newHashSet();

    for (Widget widget : container) {
      if (widget instanceof SearchView) {
        searchers.add((SearchView) widget);
      } else if (widget instanceof HasSearch) {
        Collection<SearchView> sc = ((HasSearch) widget).getSearchers();
        if (sc != null) {
          searchers.addAll(sc);
        }
      } else if (widget instanceof HasWidgets) {
        searchers.addAll(getSearchers((HasWidgets) widget));
      }
    }
    return searchers;
  }

  public static boolean hasHeader(Widget container) {
    return getHeader(container) != null;
  }

  private ViewHelper() {
  }
}
