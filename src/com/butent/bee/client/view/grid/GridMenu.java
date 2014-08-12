package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.rights.Roles;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.RegulatedWidget;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.UserInterface.Component;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GridMenu {

  private enum Item {
    BOOKMARK(Action.BOOKMARK) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return !BeeUtils.isEmpty(gridDescription.getFavorite())
            && BeeKeeper.getScreen().getUserInterface().hasComponent(Component.FAVORITES);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        IsRow row = presenter.getActiveRow();
        return DataUtils.hasId(row)
            && !Global.getFavorites().isBookmarked(presenter.getViewName(), row);
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.BOOKMARK_O);
      }

      @Override
      void select(GridPresenter presenter) {
        presenter.handleAction(Action.BOOKMARK);
      }
    },

    COPY(Action.COPY) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return !BeeUtils.isEmpty(gridDescription.getEnableCopy()) && isEditable(gridDescription)
            && BeeKeeper.getUser().canCreateData(gridDescription.getViewName());
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return presenter.getMainView().isEnabled() && presenter.getActiveRow() != null;
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.COPY);
      }

      @Override
      void select(GridPresenter presenter) {
        presenter.handleAction(Action.COPY);
      }
    },

    EXPORT(Action.EXPORT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return true;
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return presenter.getGridView().getGrid().getRowCount() > 0;
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.FILE_EXCEL_O);
      }

      @Override
      void select(GridPresenter presenter) {
        presenter.handleAction(Action.EXPORT);
      }
    },

    CONFIGURE(Action.CONFIGURE) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return UiOption.hasSettings(uiOptions);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getRowData().isEmpty();
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.COG);
      }

      @Override
      void select(GridPresenter presenter) {
        presenter.handleAction(Action.CONFIGURE);
      }
    },

    AUDIT(Action.AUDIT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.AUDIT)
            && UiOption.hasSettings(uiOptions);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getSelectedRows(SelectedRows.ALL).isEmpty()
            || presenter.getActiveRow() != null;
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.HISTORY);
      }

      @Override
      void select(GridPresenter presenter) {
        presenter.handleAction(Action.AUDIT);
      }
    },

    PRINT(Action.PRINT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return true;
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getRowData().isEmpty();
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.PRINT);
      }

      @Override
      void select(GridPresenter presenter) {
        presenter.handleAction(Action.PRINT);
      }
    },

    RIGHTS_VIEW(RightsState.VIEW) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getRowData().isEmpty();
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        if (presenter.getRightsStates().contains(RightsState.VIEW)) {
          return new FaLabel(FontAwesome.CHECK);
        } else {
          return null;
        }
      }

      @Override
      void select(GridPresenter presenter) {
        handleRights(presenter, RightsState.VIEW);
      }
    },

    RIGHTS_EDIT(RightsState.EDIT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getRowData().isEmpty();
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        if (presenter.getRightsStates().contains(RightsState.EDIT)) {
          return new FaLabel(FontAwesome.CHECK);
        } else {
          return null;
        }
      }

      @Override
      void select(GridPresenter presenter) {
        handleRights(presenter, RightsState.EDIT);
      }
    },

    RIGHTS_DELETE(RightsState.DELETE) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getRowData().isEmpty();
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        if (presenter.getRightsStates().contains(RightsState.DELETE)) {
          return new FaLabel(FontAwesome.CHECK);
        } else {
          return null;
        }
      }

      @Override
      void select(GridPresenter presenter) {
        handleRights(presenter, RightsState.DELETE);
      }
    },

    RIGHTS_ALL(Action.RIGHTS) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().getRowData().isEmpty();
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        if (presenter.getRightsStates().containsAll(ALL_STATES)) {
          return new FaLabel(FontAwesome.CHECK);
        } else {
          return null;
        }
      }

      @Override
      Widget renderLabel() {
        return new Label(Localized.getConstants().rightsAll());
      }

      @Override
      void select(final GridPresenter presenter) {
        Roles.getData(new Consumer<Map<Long, String>>() {
          @Override
          public void accept(Map<Long, String> input) {
            if (!BeeUtils.isEmpty(input)) {
              presenter.setRoles(input);
              presenter.handleAction(Action.RIGHTS);
            }
          }
        });
      }
    };

    private static void handleRights(final GridPresenter presenter, final RightsState rightsState) {
      Roles.getData(new Consumer<Map<Long, String>>() {
        @Override
        public void accept(Map<Long, String> input) {
          if (!BeeUtils.isEmpty(input)) {
            presenter.setRoles(input);
            presenter.handleRights(rightsState);
          }
        }
      });
    }

    private static boolean isEditable(GridDescription gridDescription) {
      return !BeeUtils.isTrue(gridDescription.isReadOnly())
          && Data.isViewEditable(gridDescription.getViewName());
    }

    private final Action action;
    private final RightsState rightsState;

    private Item(Action action) {
      this.action = action;
      this.rightsState = null;
    }

    private Item(RightsState rightsState) {
      this.action = Action.RIGHTS;
      this.rightsState = rightsState;
    }

    abstract boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions);

    abstract boolean isVisible(GridPresenter presenter);

    abstract Widget renderIcon(GridPresenter presenter);

    abstract void select(GridPresenter presenter);

    Widget renderLabel() {
      if (rightsState != null) {
        return new Label(Localized.getConstants().rights() + " - " + rightsState.getCaption());
      } else if (action != null) {
        return new Label(action.getCaption());
      } else {
        return null;
      }
    }

    private String getStyleSuffix() {
      return name().toLowerCase().replace(BeeConst.CHAR_UNDER, BeeConst.CHAR_MINUS);
    }
  }

  public static final List<RightsState> ALL_STATES =
      Lists.newArrayList(RightsState.VIEW, RightsState.EDIT, RightsState.DELETE);

  private static final String STYLE_PREFIX = StyleUtils.CLASS_NAME_PREFIX + "GridMenu-";

  private static final String STYLE_POPUP = STYLE_PREFIX + "popup";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_ICON = STYLE_PREFIX + "icon";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";

  private final List<Item> enabledItems = new ArrayList<>();

  public GridMenu(GridDescription gridDescription, Collection<UiOption> uiOptions) {
    Set<Action> enabledActions = gridDescription.getEnabledActions();
    Set<Action> disabledActions = gridDescription.getDisabledActions();

    boolean ok;
    for (Item item : Item.values()) {
      if (disabledActions.contains(item.action)) {
        ok = false;
      } else if (enabledActions.contains(item.action)) {
        ok = true;
      } else {
        ok = item.isEnabled(gridDescription, uiOptions);
      }

      if (ok) {
        enabledItems.add(item);
      }
    }
  }

  public boolean isActionVisible(GridPresenter presenter, Action action) {
    for (Item item : enabledItems) {
      if (item.action == action) {
        return item.isVisible(presenter);
      }
    }
    return false;
  }

  public void open(final GridPresenter presenter) {
    final HtmlTable table = new HtmlTable(STYLE_TABLE);
    int r = 0;

    for (Item item : enabledItems) {
      if (item.isVisible(presenter)) {
        Widget icon = item.renderIcon(presenter);
        if (icon != null) {
          table.setWidgetAndStyle(r, 0, icon, STYLE_ICON);
        }

        Widget label = item.renderLabel();
        if (label != null) {
          table.setWidgetAndStyle(r, 1, label, STYLE_LABEL);
        }

        table.getRowFormatter().addStyleName(r, STYLE_PREFIX + item.getStyleSuffix());
        DomUtils.setDataIndex(table.getRow(r), item.ordinal());

        r++;
      }
    }

    if (!table.isEmpty()) {
      table.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Element targetElement = EventUtils.getEventTargetElement(event);
          TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);
          int index = DomUtils.getDataIndexInt(rowElement);
          Item item = EnumUtils.getEnumByIndex(Item.class, index);

          if (item != null) {
            UiHelper.closeDialog(table);
            item.select(presenter);
          }
        }
      });

      Popup popup = new Popup(OutsideClick.CLOSE, STYLE_POPUP);
      popup.setWidget(table);

      popup.setHideOnEscape(true);
      popup.showRelativeTo(presenter.getHeader().getElement());
    }
  }
}
