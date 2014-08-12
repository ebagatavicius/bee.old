package com.butent.bee.client.view;

import com.google.common.collect.ImmutableSet;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.navigation.PagerView;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public final class ViewHelper {

  private static final ImmutableSet<String> NO_EXCLUSIONS = ImmutableSet.of();

  public static View getActiveView(Element target) {
    Popup popup = Popup.getActivePopup();

    Widget root;
    if (popup == null) {
      IdentifiableWidget activeWidget = BeeKeeper.getScreen().getActiveWidget();
      root = (activeWidget == null) ? null : activeWidget.asWidget();
    } else {
      root = popup.getWidget();
    }

    View view;
    if (root == null) {
      view = null;

    } else if (target != null && root.getElement().isOrHasChild(target)) {
      Widget widget = DomUtils.getChildByElement(root, target);
      view = getView(widget);

    } else {
      view = (root instanceof View) ? (View) root : null;
    }

    return getFacade(view);
  }

  public static View getActiveView(Element target, Action action) {
    Assert.notNull(action);

    View view = getActiveView(target);

    if (view == null || !view.isEnabled()) {
      return null;

    } else if (view.reactsTo(action)) {
      return view;

    } else {
      View actionView = null;

      List<View> children = getChildViews(view.asWidget(), false);

      if (children.isEmpty()) {
        View parent = getView(view.asWidget().getParent());

        while (parent != null) {
          if (parent.isEnabled() && parent.reactsTo(action)) {
            actionView = parent;
            break;
          } else {
            parent = getView(parent.asWidget().getParent());
          }
        }

      } else {
        for (View child : children) {
          if (DomUtils.isVisible(child.getElement()) && child.isEnabled()
              && child.reactsTo(action)) {
            if (actionView == null) {
              actionView = child;
            } else if (!actionView.getElement().isOrHasChild(child.getElement())) {
              actionView = null;
              break;
            }
          }
        }
      }

      if (actionView != null) {
        View facade = getFacade(actionView);
        if (facade != null && !actionView.getId().equals(facade.getId())) {
          actionView = (facade.isEnabled() && facade.reactsTo(action)) ? facade : null;
        }
      }

      return actionView;
    }
  }

  public static List<View> getChildViews(Widget parent, boolean include) {
    List<View> views = new ArrayList<>();

    if (parent instanceof View && include) {
      views.add((View) parent);
    }

    if (parent instanceof HasWidgets) {
      for (Widget child : (HasWidgets) parent) {
        views.addAll(getChildViews(child, true));
      }

    } else if (parent instanceof HasOneWidget) {
      views.addAll(getChildViews(((HasOneWidget) parent).getWidget(), true));
    }

    return views;
  }

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

    List<Filter> filters = new ArrayList<>();
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
    Collection<GridView> grids = new HashSet<>();

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
    Collection<PagerView> pagers = new HashSet<>();

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
    Collection<SearchView> searchers = new HashSet<>();

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

  public static View getView(Widget widget) {
    if (widget == null) {
      return null;
    }

    Widget p = widget;
    for (int i = 0; i < DomUtils.MAX_GENERATIONS; i++) {
      if (p instanceof View) {
        return (View) p;
      }

      p = p.getParent();
      if (p == null) {
        break;
      }
    }
    return null;
  }

  public static boolean hasHeader(Widget container) {
    return getHeader(container) != null;
  }

  public static boolean isActionEnabled(View view, Action action) {
    if (view == null || action == null) {
      return false;

    } else {
      Presenter presenter = view.getViewPresenter();
      HeaderView header = (presenter == null) ? null : presenter.getHeader();

      return header != null && header.isActionEnabled(action);
    }
  }

  private static View getFacade(View baseView) {
    GridView grid;
    if (baseView instanceof HasGridView) {
      grid = ((HasGridView) baseView).getGridView();
    } else if (baseView instanceof GridView) {
      grid = (GridView) baseView;
    } else {
      grid = null;
    }

    if (grid != null) {
      FormView form = grid.getActiveForm();
      if (form != null) {
        return form;
      }
    }

    return baseView;
  }

  private ViewHelper() {
  }
}
