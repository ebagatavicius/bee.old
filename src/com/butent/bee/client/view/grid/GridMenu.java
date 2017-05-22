package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
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
import com.butent.bee.client.rights.RightsHelper;
import com.butent.bee.client.rights.Roles;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
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
    },

    MERGE(Action.MERGE) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return isEditable(gridDescription)
            && BeeKeeper.getUser().canMergeData(gridDescription.getViewName());
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return presenter.getGridView().getSelectedRows(SelectedRows.MERGEABLE).size() == 2;
      }
    },

    EXPORT(Action.EXPORT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isWidgetVisible(RegulatedWidget.EXPORT_TO_XLS);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return presenter.getGridView().getGrid().getRowCount() > 0;
      }
    },

    CONFIGURE(Action.CONFIGURE) {
      @Override
      String getLabel() {
        return Localized.dictionary().columns();
      }

      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return UiOption.hasSettings(uiOptions);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty();
      }
    },

    RESET(Action.RESET_SETTINGS) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return UiOption.hasSettings(uiOptions);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty()
            && GridSettings.contains(presenter.getGridView().getGridKey());
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
    },

    PRINT(Action.PRINT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return true;
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty();
      }
    },

    NEW_ROW_FORM(GridFormKind.NEW_ROW) {
      @Override
      String getLabel() {
        return Localized.dictionary().inputForm();
      }

      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeUtils.contains(gridDescription.getNewRowForm(),
            GridDescription.FORM_ITEM_SEPARATOR)
            && isEditable(gridDescription)
            && BeeKeeper.getUser().canCreateData(gridDescription.getViewName());
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return presenter.getGridView().getFormCount(GridFormKind.NEW_ROW) > 1;
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.PLUS);
      }

      @Override
      boolean separatorBefore() {
        return true;
      }
    },

    EDIT_FORM(GridFormKind.EDIT) {
      @Override
      String getLabel() {
        return Localized.dictionary().editForm();
      }

      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeUtils.contains(gridDescription.getEditForm(),
            GridDescription.FORM_ITEM_SEPARATOR);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return presenter.getGridView().getFormCount(GridFormKind.EDIT) > 1;
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return new FaLabel(FontAwesome.EDIT);
      }

      @Override
      boolean separatorBefore() {
        return true;
      }
    },

    RIGHTS_VIEW(RightsState.VIEW) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty()
            && !Global.getParameterMap(AdministrationConstants.PRM_RECORD_DEPENDENCY)
            .containsKey(presenter.getViewName());
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
      boolean separatorBefore() {
        return true;
      }
    },

    RIGHTS_EDIT(RightsState.EDIT) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty()
            && !Global.getParameterMap(AdministrationConstants.PRM_RECORD_DEPENDENCY)
            .containsKey(presenter.getViewName());
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        if (presenter.getRightsStates().contains(RightsState.EDIT)) {
          return new FaLabel(FontAwesome.CHECK);
        } else {
          return null;
        }
      }
    },

    RIGHTS_DELETE(RightsState.DELETE) {
      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty()
            && !Global.getParameterMap(AdministrationConstants.PRM_RECORD_DEPENDENCY)
            .containsKey(presenter.getViewName());
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        if (presenter.getRightsStates().contains(RightsState.DELETE)) {
          return new FaLabel(FontAwesome.CHECK);
        } else {
          return null;
        }
      }
    },

    RIGHTS_ALL(Action.RIGHTS) {
      @Override
      String getLabel() {
        return Localized.dictionary().rightsAll();
      }

      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        return BeeKeeper.getUser().isAdministrator() && isEditable(gridDescription);
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        return !presenter.getGridView().isEmpty()
            && !Global.getParameterMap(AdministrationConstants.PRM_RECORD_DEPENDENCY)
            .containsKey(presenter.getViewName());
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
      void select(final GridPresenter presenter, Integer subIndex) {
        Roles.getData(input -> {
          if (!BeeUtils.isEmpty(input)) {
            presenter.setRoles(input);
            presenter.handleAction(Action.RIGHTS);
          }
        });
      }
    },
    ADDICTED(Action.RIGHTS) {
      private String label = Localized.dictionary().recordDependent();

      @Override
      String getLabel() {
        return label;
      }

      @Override
      boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions) {
        ensureLabel(gridDescription.getViewName());
        return Data.getDataInfo(gridDescription.getViewName(), false) != null;
      }

      @Override
      boolean isVisible(GridPresenter presenter) {
        ensureLabel(presenter.getViewName());
        return Data.getDataInfo(presenter.getViewName(), false) != null
            && Global.getParameterMap(AdministrationConstants.PRM_RECORD_DEPENDENCY)
            .containsKey(presenter.getViewName());
      }

      @Override
      Widget renderIcon(GridPresenter presenter) {
        return null;
      }

      @Override
      boolean separatorBefore() {
        return true;
      }

      void ensureLabel(String viewName) {
        Map<String, String> dependency = Global.getParameterMap(AdministrationConstants
            .PRM_RECORD_DEPENDENCY);

        if (dependency.containsKey(viewName)) {
          setLabel(BeeUtils.joinWords(Localized.dictionary().recordDependent(),
              RightsHelper.buildDependencyName(dependency, viewName)));
        }
      }

      void setLabel(String label) {
        this.label = label;
      }
    };

    private static void handleRights(final GridPresenter presenter, final RightsState rightsState) {
      Roles.getData(input -> {
        if (!BeeUtils.isEmpty(input)) {
          presenter.setRoles(input);
          presenter.handleRights(rightsState);
        }
      });
    }

    private static boolean isEditable(GridDescription gridDescription) {
      return !BeeUtils.isTrue(gridDescription.isReadOnly())
          && Data.isViewEditable(gridDescription.getViewName());
    }

    private final Action action;
    private final RightsState rightsState;
    private final GridFormKind formKind;

    Item(Action action) {
      this.action = action;
      this.rightsState = null;
      this.formKind = null;
    }

    Item(RightsState rightsState) {
      this.action = Action.RIGHTS;
      this.rightsState = rightsState;
      this.formKind = null;
    }

    Item(GridFormKind formKind) {
      this.action = null;
      this.rightsState = null;
      this.formKind = formKind;
    }

    String getLabel() {
      if (rightsState != null) {
        return Localized.dictionary().rights() + " - " + rightsState.getCaption();
      } else if (action != null) {
        return action.getCaption();
      } else {
        return null;
      }
    }

    abstract boolean isEnabled(GridDescription gridDescription, Collection<UiOption> uiOptions);

    abstract boolean isVisible(GridPresenter presenter);

    Widget renderIcon(GridPresenter presenter) {
      if (action == null) {
        return null;
      } else {
        return new FaLabel(action.getIcon());
      }
    }

    void select(GridPresenter presenter, Integer subIndex) {
      if (rightsState != null) {
        handleRights(presenter, rightsState);
      } else if (action != null) {
        presenter.handleAction(action);
      } else if (formKind != null && subIndex != null) {
        presenter.getGridView().selectForm(formKind, subIndex);
      }
    }

    boolean separatorBefore() {
      return false;
    }

    private String getStyleSuffix() {
      return name().toLowerCase().replace(BeeConst.CHAR_UNDER, BeeConst.CHAR_MINUS);
    }

    private boolean isDisablable() {
      if (action == null) {
        return false;
      } else {
        return action.isDisablable();
      }
    }
  }

  public static final List<RightsState> ALL_STATES =
      Lists.newArrayList(RightsState.VIEW, RightsState.EDIT, RightsState.DELETE);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "GridMenu-";

  private static final String STYLE_POPUP = STYLE_PREFIX + "popup";
  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_ICON = STYLE_PREFIX + "icon";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";

  private static final String STYLE_FORM_ITEM = STYLE_PREFIX + "form-item";
  private static final String STYLE_FORM_SELECTED = STYLE_PREFIX + "form-selected";

  private static final String STYLE_SECTION_HEADER = STYLE_PREFIX + "section-header";
  private static final String STYLE_SEPARATOR = STYLE_PREFIX + "separator";

  private static final String KEY_SUB_INDEX = "sub";

  private final List<Item> enabledItems = new ArrayList<>();

  public GridMenu(GridDescription gridDescription, Collection<UiOption> uiOptions,
      GridInterceptor gridInterceptor) {

    Set<Action> enabledActions = GridUtils.getEnabledActions(gridDescription, gridInterceptor);
    Set<Action> disabledActions = GridUtils.getDisabledActions(gridDescription, gridInterceptor);

    boolean ok;
    for (Item item : Item.values()) {
      if (item.action != null && disabledActions.contains(item.action)) {
        ok = false;
      } else if (item.action != null && enabledActions.contains(item.action)) {
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
    if (action != null) {
      for (Item item : enabledItems) {
        if (item.action == action) {
          return item.isVisible(presenter);
        }
      }
    }
    return false;
  }

  public void open(final GridPresenter presenter, boolean enabled) {
    final HtmlTable table = new HtmlTable(STYLE_TABLE);
    int r = 0;

    for (Item item : enabledItems) {
      if ((enabled || !item.isDisablable()) && item.isVisible(presenter)) {
        if (item.separatorBefore() && r > 0) {
          CustomDiv separator = new CustomDiv();
          table.setWidgetAndStyle(r, 0, separator, STYLE_SEPARATOR);

          table.getCellFormatter().setColSpan(r, 0, 2);
          r++;
        }

        Widget icon = item.renderIcon(presenter);
        if (icon != null) {
          table.setWidgetAndStyle(r, 0, icon, STYLE_ICON);
        }

        String text = item.getLabel();
        if (!BeeUtils.isEmpty(text)) {
          Label label = new Label(text);
          UiHelper.makePotentiallyBold(label.getElement(), text);

          table.setWidgetAndStyle(r, 1, label, STYLE_LABEL);
        }

        table.getRowFormatter().addStyleName(r, STYLE_PREFIX + item.getStyleSuffix());
        if (item.formKind != null || item == Item.ADDICTED) {
          table.getRowFormatter().addStyleName(r, STYLE_SECTION_HEADER);
        }

        DomUtils.setDataIndex(table.getRow(r), item.ordinal());

        r++;

        if (item.formKind != null) {
          List<String> formLabels = presenter.getGridView().getFormLabels(item.formKind);

          for (int formIndex = 0; formIndex < formLabels.size(); formIndex++) {
            boolean selected = formIndex == presenter.getGridView().getFormIndex(item.formKind);
            if (selected) {
              table.setWidgetAndStyle(r, 0, new FaLabel(FontAwesome.CHECK), STYLE_ICON);
            }

            String formText = formLabels.get(formIndex);
            Label label = new Label(formText);
            UiHelper.makePotentiallyBold(label.getElement(), formText);

            table.setWidgetAndStyle(r, 1, label, STYLE_LABEL);

            Element rowElement = table.getRow(r);
            rowElement.addClassName(selected ? STYLE_FORM_ITEM : STYLE_FORM_SELECTED);

            DomUtils.setDataIndex(rowElement, item.ordinal());
            DomUtils.setDataProperty(rowElement, KEY_SUB_INDEX, formIndex);

            r++;
          }
        }
      }
    }

    if (!table.isEmpty()) {
      table.addClickHandler(event -> {
        Element targetElement = EventUtils.getEventTargetElement(event);
        TableRowElement rowElement = DomUtils.getParentRow(targetElement, true);

        int index = DomUtils.getDataIndexInt(rowElement);

        if (!BeeConst.isUndef(index)) {
          Item item = EnumUtils.getEnumByIndex(Item.class, index);
          Integer subIndex = DomUtils.getDataPropertyInt(rowElement, KEY_SUB_INDEX);

          if (item != null && item != Item.ADDICTED
              && (item.formKind == null || subIndex != null)) {
            UiHelper.closeDialog(table);
            item.select(presenter, subIndex);
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
