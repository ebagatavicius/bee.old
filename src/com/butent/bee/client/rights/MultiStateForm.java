package com.butent.bee.client.rights;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class MultiStateForm extends RightsForm {

  private static final String STYLE_STATE_LABEL = STYLE_PREFIX + "state-label";
  private static final String STYLE_STATE_LABEL_CELL = STYLE_STATE_LABEL + STYLE_SUFFIX_CELL;
  private static final String STYLE_STATE_TOGGLE = STYLE_PREFIX + "state-toggle";
  private static final String STYLE_STATE_TOGGLE_CELL = STYLE_STATE_TOGGLE + STYLE_SUFFIX_CELL;

  private static final String STYLE_ROLE_COMMAND = STYLE_PREFIX + "role-command";
  private static final String STYLE_ROLE_SELECTOR = STYLE_PREFIX + "role-selector";
  private static final String STYLE_ROLE_EMPTY = STYLE_PREFIX + "role-empty";
  private static final String STYLE_ROLE_NOT_EMPTY = STYLE_PREFIX + "role-not-empty";

  private static final String STYLE_MODULE = STYLE_PREFIX + "module";
  private static final String STYLE_SUB_MODULE = STYLE_PREFIX + "sub-module";
  private static final String STYLE_MODULE_CELL = STYLE_MODULE + STYLE_SUFFIX_CELL;
  private static final String STYLE_MODULE_SELECTED = STYLE_MODULE + "-selected";
  // private static final String STYLE_MODULE_CHANGED = STYLE_MODULE + "-changed";

  private static final String STYLE_MSO = STYLE_PREFIX + "mso";
  private static final String STYLE_MSO_COL_PREFIX = STYLE_MSO + "-col-";
  private static final String STYLE_MSO_CELL = STYLE_MSO + STYLE_SUFFIX_CELL;
  private static final String STYLE_MSO_BRANCH = STYLE_MSO + "-branch";
  private static final String STYLE_MSO_BRANCH_CELL = STYLE_MSO_BRANCH + STYLE_SUFFIX_CELL;
  private static final String STYLE_MSO_LEAF = STYLE_MSO + "-leaf";
  private static final String STYLE_MSO_LEAF_CELL = STYLE_MSO_LEAF + STYLE_SUFFIX_CELL;
  private static final String STYLE_MSO_SELECTED = STYLE_MSO + "-selected";
  // private static final String STYLE_MSO_CHANGED = STYLE_MSO + "-changed";

  private static final String DATA_KEY_STATE = "rights-state";
  private static final String DATA_KEY_MODULE = "rights-module";

  private static final String DATA_TYPE_STATE_LABEL = "sl";
  private static final String DATA_TYPE_STATE_TOGGLE = "st";

  private static final String DATA_TYPE_MODULE = "mod";

  private static final int MODULE_COL = 0;

  private static void addCellStyleName(Widget widget, String styleName) {
    TableCellElement cell = DomUtils.getParentCell(widget, false);

    if (cell == null) {
      severe("parent cell not found");
    } else {
      cell.addClassName(styleName);
    }
  }

  private static boolean cellHasStyleName(Widget widget, String styleName) {
    TableCellElement cell = DomUtils.getParentCell(widget, false);

    if (cell == null) {
      severe("parent cell not found");
      return false;
    } else {
      return cell.hasClassName(styleName);
    }
  }

  private static ModuleAndSub getModule(Widget widget) {
    String value = DomUtils.getDataProperty(widget.getElement(), DATA_KEY_MODULE);
    ModuleAndSub ms = ModuleAndSub.parse(value);
    if (ms == null) {
      severe("Widget", DomUtils.getId(widget), "has no module");
    }
    return ms;
  }

  private static RightsState getRightsState(Widget widget) {
    Integer value = DomUtils.getDataPropertyInt(widget.getElement(), DATA_KEY_STATE);
    RightsState rs = EnumUtils.getEnumByIndex(RightsState.class, value);
    if (rs == null) {
      severe("Widget", DomUtils.getId(widget), "has no rights state");
    }
    return rs;
  }

  private final Multimap<String, RightsState> initialValues = HashMultimap.create();
  private final Multimap<String, RightsState> changes = HashMultimap.create();

  private Long roleId;
  private String roleName;

  @Override
  public void onShow(Presenter presenter) {
    HeaderView header = presenter.getHeader();

    if (header != null && !header.hasCommands()) {
      List<String> columns = Lists.newArrayList(COL_ROLE_NAME);
      Relation relation = Relation.create(VIEW_ROLES, columns);

      relation.disableEdit();
      relation.disableNewRow();

      final DataSelector roleSelector = new DataSelector(relation, true);
      roleSelector.setEditing(true);
      roleSelector.addStyleName(STYLE_ROLE_SELECTOR);

      final Button roleCommand = new Button();
      roleCommand.addStyleName(STYLE_ROLE_COMMAND);

      if (DataUtils.isId(getRoleId())) {
        roleCommand.setHtml(getRoleName());
        roleCommand.addStyleName(STYLE_ROLE_NOT_EMPTY);
      } else {
        roleCommand.setHtml(Localized.getConstants().selectRole());
        roleCommand.addStyleName(STYLE_ROLE_EMPTY);
      }

      roleSelector.addSelectorHandler(new SelectorEvent.Handler() {
        @Override
        public void onDataSelector(SelectorEvent event) {
          if (event.isChanged() && event.getRelatedRow() != null) {
            long id = event.getValue();
            String name = Data.getString(VIEW_ROLES, event.getRelatedRow(), COL_ROLE_NAME);

            final Long oldId = getRoleId();
            final String oldName = getRoleName();

            if (DataUtils.isId(id) && !BeeUtils.isEmpty(name) && !Objects.equals(id, oldId)) {
              setRoleId(id);
              setRoleName(name);

              roleCommand.setHtml(name);

              if (DataUtils.isId(oldId)) {
                getRoleData(new Consumer<Boolean>() {
                  @Override
                  public void accept(Boolean input) {
                    if (BeeUtils.isTrue(input)) {
                      refreshValues();

                    } else {
                      setRoleId(oldId);
                      setRoleName(oldName);

                      roleCommand.setHtml(oldName);
                    }
                  }
                });

              } else {
                roleCommand.removeStyleName(STYLE_ROLE_EMPTY);
                roleCommand.addStyleName(STYLE_ROLE_NOT_EMPTY);

                init();
              }
            }
          }
        }
      });

      final Runnable activateSelector = new Runnable() {
        @Override
        public void run() {
          roleSelector.clearValue();
          roleSelector.setFocus(true);

          roleSelector.startEdit(null, DataSelector.SHOW_SELECTOR, null, null);
        }
      };

      roleCommand.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {

          if (DataUtils.isId(getRoleId()) && !changes.isEmpty()) {
            onClose(activateSelector);
          } else {
            activateSelector.run();
          }
        }
      });

      header.addCommandItem(roleCommand);
      header.addCommandItem(roleSelector);
    }

    if (DataUtils.isId(getRoleId())) {
      init();
    }
  }

  protected void addModuleWidget(int row, ModuleAndSub moduleAndSub) {
    getTable().setWidget(row, MODULE_COL, createModuleWidget(moduleAndSub), STYLE_MODULE_CELL);
  }

  @Override
  protected Set<String> getChangedNames() {
    return changes.keySet();
  }

  @Override
  protected String getChangeMessage(RightsObject object) {
    Collection<RightsState> changedStates = changes.get(object.getName());

    List<String> stateCaptions = new ArrayList<>();
    for (RightsState state : getRightsStates()) {
      if (changedStates.contains(state)) {
        stateCaptions.add(state.getCaption());
      }
    }

    return BeeUtils.joinWords(object.getCaption(), stateCaptions);
  }

  @Override
  protected String getDialogCaption() {
    return BeeUtils.joinWords(getFormView().getCaption(), getRoleName());
  }

  @Override
  protected String getPanelStyleName() {
    return STYLE_PREFIX + "multi-state";
  }

  protected abstract List<RightsState> getRightsStates();

  protected abstract boolean hasValue(RightsObject object);

  @Override
  protected void initData(final Consumer<Boolean> callback) {
    getRoleData(callback);
  }

  @Override
  protected boolean isObjectChecked(String objectName) {
    for (RightsState state : getRightsStates()) {
      if (!isChecked(objectName, state)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void onObjectToggle(boolean checked) {
    List<Toggle> stateToggles = getStateToggles();

    for (Toggle toggle : stateToggles) {
      if (checked) {
        toggle.setChecked(isStateChecked(getRightsState(toggle)));
      } else if (toggle.isChecked()) {
        toggle.setChecked(false);
      }
    }
  }

  @Override
  protected void populateTable() {
    List<ModuleAndSub> modules = getModules();
    if (modules.isEmpty()) {
      severe(getObjectType(), "modules not available");
      return;
    }

    int row = getValueStartRow();

    for (ModuleAndSub ms : modules) {
      addModuleWidget(row, ms);
      row++;
    }

    ModuleAndSub ms = modules.get(0);
    Widget widget = getTable().getWidget(getValueStartRow(), MODULE_COL);

    if (widget != null) {
      addCellStyleName(widget, STYLE_MODULE_SELECTED);
      onSelectModule(ms);
    }

    int col = getValueStartCol();

    for (RightsState state : getRightsStates()) {
      addColumnLabel(col, createStateLabel(state), STYLE_STATE_LABEL_CELL);
      addColumnToggle(col, createStateToggle(state), STYLE_STATE_TOGGLE_CELL);
      col++;
    }
  }

  @Override
  protected void save(final Consumer<Boolean> callback) {
    if (changes.isEmpty() || !DataUtils.isId(getRoleId())) {
      BeeKeeper.getScreen().notifyInfo("no changes");
      if (callback != null) {
        callback.accept(false);
      }

    } else {
      ParameterList params = BeeKeeper.getRpc().createParameters(Service.SET_ROLE_RIGHTS);
      params.addQueryItem(COL_OBJECT_TYPE, getObjectType().ordinal());
      params.addQueryItem(COL_ROLE, getRoleId());

      Map<String, String> diff = new HashMap<>();
      for (String objectName : changes.keySet()) {
        diff.put(objectName, EnumUtils.buildIndexList(changes.get(objectName)));
      }
      params.addDataItem(COL_OBJECT, Codec.beeSerialize(diff));

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          if (response.hasErrors()) {
            response.notify(BeeKeeper.getScreen());
            if (callback != null) {
              callback.accept(false);
            }

          } else {
            int size = changes.size();

            for (Map.Entry<String, RightsState> entry : changes.entries()) {
              if (initialValues.containsEntry(entry.getKey(), entry.getValue())) {
                initialValues.remove(entry.getKey(), entry.getValue());
              } else {
                initialValues.put(entry.getKey(), entry.getValue());
              }
            }

            changes.clear();
            onClearChanges();

            String message = BeeUtils.joinWords(getObjectType(), getRoleName(),
                "saved", size, "changes");
            debug(message);
            BeeKeeper.getScreen().notifyInfo(message);

            if (callback != null) {
              callback.accept(true);
            }
          }
        }
      });
    }
  }

  @Override
  protected void updateObjectValueToggles(String objectName, boolean checked) {
    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && ((Toggle) widget).isChecked() != checked
          && isValueWidget(widget) && hasObject(widget, objectName)) {

        ((Toggle) widget).setChecked(checked);

        boolean isChanged = toggleValue(objectName, getRightsState(widget));
        updateValueCell(widget, isChanged);
      }
    }
  }

  private void clearTable(int startCol) {
    int rc = getTable().getRowCount();
    int maxRow = getValueStartRow();

    for (int row = getValueStartRow(); row < rc; row++) {
      int cc = getTable().getCellCount(row);

      for (int col = 0; col < cc; col++) {
        Widget widget = getTable().getWidget(row, col);

        if (widget != null) {
          if (col < startCol) {
            maxRow = Math.max(maxRow, row);
          } else {
            getTable().getCellFormatter().setStyleName(row, col, BeeConst.STRING_EMPTY);
            getTable().remove(widget);
          }
        }
      }
    }

    if (maxRow < rc - 1) {
      for (int i = maxRow + 1; i < rc; i++) {
        getTable().removeRow(maxRow + 1);
      }
    }
  }

  private Widget createModuleWidget(ModuleAndSub moduleAndSub) {
    String caption = moduleAndSub.hasSubModule()
        ? moduleAndSub.getSubModule().getCaption() : moduleAndSub.getModule().getCaption();
    Label widget = new Label(caption);
    widget.addStyleName(moduleAndSub.hasSubModule() ? STYLE_SUB_MODULE : STYLE_MODULE);

    String name = moduleAndSub.getName();
    widget.setTitle(name);

    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_MODULE, name);
    setDataType(widget, DATA_TYPE_MODULE);

    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Widget) {
          Widget source = (Widget) event.getSource();
          ModuleAndSub ms = getModule(source);

          if (ms != null && !cellHasStyleName(source, STYLE_MODULE_SELECTED)) {
            TableCellElement cell = getSelectedModuleCell();
            if (cell != null) {
              cell.removeClassName(STYLE_MODULE_SELECTED);
            }

            addCellStyleName(source, STYLE_MODULE_SELECTED);

            clearTable(MODULE_COL + 1);
            onSelectModule(ms);
          }
        }
      }
    });

    return widget;
  }

  private Widget createObjectWidget(final int col, RightsObject object) {
    Label widget = new Label(object.getCaption());
    widget.addStyleName(STYLE_MSO);

    widget.addStyleName(STYLE_MSO_COL_PREFIX + col);
    widget.addStyleName(isLeaf(col) ? STYLE_MSO_LEAF : STYLE_MSO_BRANCH);

    String name = object.getName();
    widget.setTitle(name);

    markObjectLabel(widget, object);

    if (!isLeaf(col)) {
      widget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof Widget) {
            Widget source = (Widget) event.getSource();
            String objectName = getObjectName(source);

            if (!BeeUtils.isEmpty(objectName) && !cellHasStyleName(source, STYLE_MSO_SELECTED)) {
              TableCellElement cell = getSelectedObjectCell(col);
              if (cell != null) {
                cell.removeClassName(STYLE_MSO_SELECTED);
              }

              addCellStyleName(source, STYLE_MSO_SELECTED);

              clearTable(col + 1);
              onSelectObject(col, objectName);
            }
          }
        }
      });
    }

    return widget;
  }

  private Widget createStateLabel(RightsState state) {
    Label widget = new Label(state.getCaption());
    widget.addStyleName(STYLE_STATE_LABEL);

    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_STATE, state.ordinal());
    setDataType(widget, DATA_TYPE_STATE_LABEL);

    return widget;
  }

  private Widget createStateToggle(RightsState state) {
    Set<String> names = getLeaves().keySet();
    boolean checked = true;

    for (String name : names) {
      if (!initialValues.containsEntry(name, state)) {
        checked = false;
        break;
      }
    }

    Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
        STYLE_STATE_TOGGLE, checked);

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_STATE, state.ordinal());
    setDataType(toggle, DATA_TYPE_STATE_TOGGLE);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Toggle) {
          Toggle rt = (Toggle) event.getSource();
          updateStateValueToggles(getRightsState(rt), rt.isChecked());

          List<Toggle> objectToggles = getObjectToggles();
          for (Toggle ot : objectToggles) {
            if (rt.isChecked()) {
              ot.setChecked(isObjectChecked(getObjectName(ot)));
            } else if (ot.isChecked()) {
              ot.setChecked(false);
            }
          }
        }
      }
    });

    return toggle;
  }

  private Toggle createValueToggle(String objectName, RightsState state, String title) {
    Toggle toggle = createValueToggle(objectName);
    toggle.setTitle(title);

    if (isChecked(objectName, state)) {
      toggle.setChecked(true);
    }

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_STATE, state.ordinal());

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Toggle) {
          Toggle t = (Toggle) event.getSource();

          String name = getObjectName(t);
          RightsState rs = getRightsState(t);

          boolean isChanged = toggleValue(name, rs);
          updateValueCell(t, isChanged);

          Toggle objectToggle = getObjectToggle(name);
          Toggle stateToggle = getStateToggle(rs);

          if (t.isChecked()) {
            objectToggle.setChecked(isObjectChecked(name));
            stateToggle.setChecked(isStateChecked(rs));

          } else {
            objectToggle.setChecked(false);
            stateToggle.setChecked(false);
          }
        }
      }
    });
    return toggle;
  }

  private Map<String, Integer> getLeaves() {
    Map<String, Integer> leaves = new HashMap<>();

    int col = getValueStartCol() - 2;

    for (int row = getValueStartRow(); row < getTable().getRowCount(); row++) {
      if (col >= getTable().getCellCount(row)) {
        break;
      }
      Widget widget = getTable().getWidget(row, col);
      if (widget == null) {
        break;
      }

      String name = getObjectName(widget);
      if (BeeUtils.isEmpty(name)) {
        break;
      }

      leaves.put(name, row);
    }

    if (leaves.isEmpty()) {
      severe(getObjectType(), getRoleName(), "leaves not found");
    }
    return leaves;
  }

  private void getRoleData(final Consumer<Boolean> callback) {
    ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_ROLE_RIGHTS);
    params.addQueryItem(COL_OBJECT_TYPE, getObjectType().ordinal());
    params.addQueryItem(COL_ROLE, getRoleId());

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          response.notify(BeeKeeper.getScreen());
          callback.accept(false);

        } else {
          if (!initialValues.isEmpty()) {
            initialValues.clear();
          }
          if (!changes.isEmpty()) {
            changes.clear();
          }

          if (response.hasResponse()) {
            Map<String, String> rights = Codec.deserializeMap(response.getResponseAsString());

            Multimap<String, RightsState> values = HashMultimap.create();
            for (Map.Entry<String, String> entry : rights.entrySet()) {
              Set<RightsState> states = EnumUtils.parseIndexSet(RightsState.class,
                  entry.getValue());
              states.retainAll(getRightsStates());

              if (!states.isEmpty()) {
                values.putAll(entry.getKey(), states);
              }
            }

            for (RightsState state : getRightsStates()) {
              if (state.isChecked()) {
                for (RightsObject object : getObjects()) {
                  if (hasValue(object) && !values.containsEntry(object.getName(), state)) {
                    initialValues.put(object.getName(), state);
                  }
                }

              } else if (values.containsValue(state)) {
                for (Map.Entry<String, RightsState> entry : values.entries()) {
                  if (entry.getValue() == state) {
                    initialValues.put(entry.getKey(), entry.getValue());
                  }
                }
              }
            }

          } else {
            for (RightsState state : getRightsStates()) {
              if (state.isChecked()) {
                for (RightsObject object : getObjects()) {
                  if (hasValue(object)) {
                    initialValues.put(object.getName(), state);
                  }
                }
              }
            }
          }

          callback.accept(true);
        }
      }
    });
  }

  private Long getRoleId() {
    return roleId;
  }

  private String getRoleName() {
    return roleName;
  }

  private TableCellElement getSelectedModuleCell() {
    for (int row = getValueStartRow(); row < getTable().getRowCount(); row++) {
      TableCellElement cell = getTable().getCellFormatter().getElement(row, MODULE_COL);
      if (cell.hasClassName(STYLE_MODULE_SELECTED)) {
        return cell;
      }
    }

    return null;
  }

  private TableCellElement getSelectedObjectCell(int col) {
    for (int row = getValueStartRow(); row < getTable().getRowCount(); row++) {
      if (col >= getTable().getCellCount(row)) {
        break;
      }

      TableCellElement cell = getTable().getCellFormatter().getElement(row, col);
      if (cell.hasClassName(STYLE_MSO_SELECTED)) {
        return cell;
      }
    }
    return null;
  }

  private Toggle getStateToggle(RightsState state) {
    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && isDataType(widget, DATA_TYPE_STATE_TOGGLE)
          && getRightsState(widget) == state) {
        return (Toggle) widget;
      }
    }

    severe("rights state", state, "toggle not found");
    return null;
  }

  private List<Toggle> getStateToggles() {
    List<Toggle> toggles = new ArrayList<>();

    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && isDataType(widget, DATA_TYPE_STATE_TOGGLE)) {
        toggles.add((Toggle) widget);
      }
    }
    return toggles;
  }

  private Toggle getToggle(int row, int col) {
    Widget widget = getTable().getWidget(row, col);

    if (widget instanceof Toggle) {
      return (Toggle) widget;
    } else {
      severe("widget at", row, col, "not a toggle");
      return null;
    }
  }

  private boolean isChecked(String objectName, RightsState state) {
    return initialValues.containsEntry(objectName, state)
        != changes.containsEntry(objectName, state);
  }

  private boolean isLeaf(int col) {
    return col == getValueStartCol() - 2;
  }

  private boolean isStateChecked(RightsState state) {
    Set<String> names = getLeaves().keySet();
    for (String name : names) {
      if (!isChecked(name, state)) {
        return false;
      }
    }
    return true;
  }

  private void onSelectModule(ModuleAndSub moduleAndSub) {
    renderColumn(MODULE_COL + 1, filterByModule(moduleAndSub));
  }

  private void onSelectObject(int col, String objectName) {
    renderColumn(col + 1, filterByParent(objectName));
  }

  private void refreshValues() {
    Map<String, Integer> leaves = getLeaves();

    EnumSet<RightsState> checkedStates = EnumSet.copyOf(getRightsStates());

    for (Map.Entry<String, Integer> leaf : leaves.entrySet()) {
      String name = leaf.getKey();
      int row = leaf.getValue();

      boolean objectChecked = true;

      int col = getValueStartCol();
      for (RightsState state : getRightsStates()) {
        Toggle toggle = getToggle(row, col);
        if (toggle == null) {
          break;
        }

        boolean checked = isChecked(name, state);
        toggle.setChecked(checked);
        updateValueCell(toggle, changes.containsEntry(name, state));

        if (!checked) {
          objectChecked = false;
          if (checkedStates.contains(state)) {
            checkedStates.remove(state);
          }
        }
        col++;
      }

      Toggle objectToggle = getToggle(row, getValueStartCol() - 1);
      if (objectToggle == null) {
        break;
      } else {
        objectToggle.setChecked(objectChecked);
      }
    }

    int row = getValueStartRow() - 1;
    int col = getValueStartCol();

    for (RightsState state : getRightsStates()) {
      Toggle toggle = getToggle(row, col);
      if (toggle == null) {
        break;
      } else {
        toggle.setChecked(checkedStates.contains(state));
      }
      col++;
    }
  }

  private void renderColumn(int col, List<RightsObject> columnObjects) {
    EnumSet<RightsState> checkedStates = EnumSet.copyOf(getRightsStates());

    int row = getValueStartRow();

    for (RightsObject object : columnObjects) {
      getTable().setWidget(row, col, createObjectWidget(col, object), STYLE_MSO_CELL);
      getTable().getCellFormatter().addStyleName(row, col,
          isLeaf(col) ? STYLE_MSO_LEAF_CELL : STYLE_MSO_BRANCH_CELL);

      if (isLeaf(col)) {
        addObjectToggle(row, object);

        String objectName = object.getName();
        String objectCaption = object.getCaption();

        int j = getValueStartCol();
        for (RightsState state : getRightsStates()) {
          String title = BeeUtils.joinWords(objectCaption, state.getCaption());
          Toggle valueToggle = createValueToggle(objectName, state, title);

          addValueToggle(row, j++, valueToggle);
          updateValueCell(valueToggle, changes.containsEntry(objectName, state));

          if (!valueToggle.isChecked() && checkedStates.contains(state)) {
            checkedStates.remove(state);
          }
        }

        markValueRow(row);
      }

      row++;
    }

    if (isLeaf(col)) {
      List<Toggle> stateToggles = getStateToggles();

      for (Toggle toggle : stateToggles) {
        toggle.setChecked(checkedStates.contains(getRightsState(toggle)));
      }

    } else {
      RightsObject object = columnObjects.get(0);
      Widget widget = getTable().getWidget(getValueStartRow(), col);

      if (widget != null) {
        addCellStyleName(widget, STYLE_MSO_SELECTED);
        onSelectObject(col, object.getName());
      }
    }
  }

  private void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  private void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  private boolean toggleValue(String objectName, RightsState state) {
    if (BeeUtils.isEmpty(objectName)) {
      severe("toggle value: object name not specified");
      return false;
    } else if (state == null) {
      severe("toggle value: rights state not specified");
      return false;

    } else if (changes.containsEntry(objectName, state)) {
      changes.remove(objectName, state);
      return false;
    } else {
      changes.put(objectName, state);
      return true;
    }
  }

  private void updateStateValueToggles(RightsState state, boolean checked) {
    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && ((Toggle) widget).isChecked() != checked
          && isValueWidget(widget) && getRightsState(widget) == state) {

        ((Toggle) widget).setChecked(checked);

        boolean isChanged = toggleValue(getObjectName(widget), state);
        updateValueCell(widget, isChanged);
      }
    }
  }
}
