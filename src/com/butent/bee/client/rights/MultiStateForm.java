package com.butent.bee.client.rights;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
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
import java.util.function.Consumer;

abstract class MultiStateForm extends RightsForm {

  private static final String STYLE_STATE_LABEL = STYLE_PREFIX + "state-label";
  private static final String STYLE_STATE_LABEL_CELL = STYLE_STATE_LABEL + STYLE_SUFFIX_CELL;
  private static final String STYLE_STATE_TOGGLE = STYLE_PREFIX + "state-toggle";
  private static final String STYLE_STATE_TOGGLE_CELL = STYLE_STATE_TOGGLE + STYLE_SUFFIX_CELL;

  private static final String STYLE_ROLE_COMMAND = STYLE_PREFIX + "role-command";
  private static final String STYLE_ROLE_SELECTOR = STYLE_PREFIX + "role-selector";
  private static final String STYLE_ROLE_EMPTY = STYLE_PREFIX + "role-empty";
  private static final String STYLE_ROLE_NOT_EMPTY = STYLE_PREFIX + "role-not-empty";

  private static final String STYLE_MSO = STYLE_PREFIX + "mso";
  private static final String STYLE_MSO_SELECTED = STYLE_MSO + "-selected";

  private static final String DATA_KEY_STATE = "rights-state";

  private static final String DATA_TYPE_STATE_LABEL = "sl";
  private static final String DATA_TYPE_STATE_TOGGLE = "st";

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
  public void afterCreatePresenter(Presenter presenter) {
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
        roleCommand.setHtml(Localized.dictionary().selectRole());
        roleCommand.addStyleName(STYLE_ROLE_EMPTY);
      }

      roleSelector.addSelectorHandler(event -> {
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
              getRoleData(input -> {
                if (BeeUtils.isTrue(input)) {
                  refreshValues();

                } else {
                  setRoleId(oldId);
                  setRoleName(oldName);

                  roleCommand.setHtml(oldName);
                }
              });

            } else {
              roleCommand.removeStyleName(STYLE_ROLE_EMPTY);
              roleCommand.addStyleName(STYLE_ROLE_NOT_EMPTY);

              init();
            }
          }
        }
      });

      final Runnable activateSelector = () -> {
        roleSelector.clearValue();
        roleSelector.setFocus(true);

        roleSelector.startEdit(null, DataSelector.SHOW_SELECTOR, null, null);
      };

      roleCommand.addClickHandler(event -> {
        if (DataUtils.isId(getRoleId()) && !changes.isEmpty()) {
          onClose(activateSelector);
        } else {
          activateSelector.run();
        }
      });

      header.addCommandItem(roleCommand);
      header.addCommandItem(roleSelector);
    }

    if (DataUtils.isId(getRoleId())) {
      init();
    }
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

  protected List<RightsState> getRightsStates() {
    return new ArrayList<>(getObjectType().getRegisteredStates());
  }

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
      getTable().addModuleWidget(row, ms);
      row++;
    }
    getTable().setModuleSelectionHandler(this::onSelectModule);
    getTable().setRightObjectSelectionHandler(this::onSelectObject);

    ModuleAndSub ms = modules.get(0);
    getTable().setSelectedModule(ms);

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
      BeeKeeper.getScreen().notifyInfo(Localized.dictionary().noChanges());
      if (callback != null) {
        callback.accept(false);
      }

    } else {
      ParameterList params = BeeKeeper.getRpc().createParameters(Service.SET_ROLE_RIGHTS);
      params.addQueryItem(COL_OBJECT_TYPE, getObjectType().ordinal());
      params.addQueryItem(COL_ROLE, getRoleId());

      Map<String, String> diff = new HashMap<>();
      for (String objectName : changes.keySet()) {
        diff.put(objectName, EnumUtils.joinIndexes(changes.get(objectName)));
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

            String message = Localized.dictionary().roleRightsSaved(BeeUtils.joinWords(
                BeeUtils.bracket(getObjectType().getCaption()), Localized.dictionary().role(),
                getRoleName()), size);
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

  private static Widget createStateLabel(RightsState state) {
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

    toggle.addClickHandler(event -> {
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

    toggle.addClickHandler(event -> {
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
            Map<String, String> rights =
                Codec.deserializeLinkedHashMap(response.getResponseAsString());

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
    renderColumn(RightsTable.MODULE_COL + 1, filterByModule(moduleAndSub));
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

    int row = getTable().getValueStartRow();

    for (RightsObject object : columnObjects) {
      getTable().addRightObjectWidget(row, col, object);

      if (getTable().isLeaf(col)) {
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

    if (getTable().isLeaf(col)) {
      List<Toggle> stateToggles = getStateToggles();

      for (Toggle toggle : stateToggles) {
        toggle.setChecked(checkedStates.contains(getRightsState(toggle)));
      }

    } else {
      RightsObject object = columnObjects.get(0);
      Widget widget = getTable().getWidget(getValueStartRow(), col);

      if (widget != null) {
        getTable().addCellStyleName(widget, STYLE_MSO_SELECTED);
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
