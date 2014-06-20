package com.butent.bee.client.rights;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Toggle;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Attributes;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class MultiRoleForm extends RightsForm {

  private static final String STYLE_ROLE_LABEL = STYLE_PREFIX + "role-label";
  private static final String STYLE_ROLE_LABEL_CELL = STYLE_ROLE_LABEL + STYLE_SUFFIX_CELL;
  private static final String STYLE_ROLE_TOGGLE = STYLE_PREFIX + "role-toggle";
  private static final String STYLE_ROLE_TOGGLE_CELL = STYLE_ROLE_TOGGLE + STYLE_SUFFIX_CELL;

  private static final String STYLE_ROLE_ORIENTATION = STYLE_PREFIX + "role-orientation";
  private static final String STYLE_ROLE_ORIENTATION_CELL = STYLE_ROLE_ORIENTATION
      + STYLE_SUFFIX_CELL;
  private static final String STYLE_COLUMN_LABEL_ORIENTATION = STYLE_PREFIX
      + "column-label-orientation";
  private static final String STYLE_COLUMN_LABEL_ORIENTATION_CELL =
      STYLE_COLUMN_LABEL_ORIENTATION + STYLE_SUFFIX_CELL;

  private static final String STYLE_USER_COMMAND = STYLE_PREFIX + "user-command";
  private static final String STYLE_USER_CLEAR = STYLE_PREFIX + "user-clear";
  private static final String STYLE_USER_SELECTOR = STYLE_PREFIX + "user-selector";
  private static final String STYLE_USER_EMPTY = STYLE_PREFIX + "user-empty";
  private static final String STYLE_USER_NOT_EMPTY = STYLE_PREFIX + "user-not-empty";

  private static final String DATA_KEY_ROLE = "rights-role";

  private static final String DATA_TYPE_ROLE_LABEL = "rl";
  private static final String DATA_TYPE_ROLE_TOGGLE = "rt";

  private static final int ROLE_ORIENTATION_ROW = 0;
  private static final int ROLE_ORIENTATION_COL = 0;

  private static final int COLUMN_LABEL_ORIENTATION_ROW = 0;
  private static final int COLUMN_LABEL_ORIENTATION_COL = 1;

  private static Long getRoleId(Widget widget) {
    Long roleId = DomUtils.getDataPropertyLong(widget.getElement(), DATA_KEY_ROLE);
    if (roleId == null) {
      severe("widget", DomUtils.getId(widget), "has no role");
    }
    return roleId;
  }

  private final BiMap<Long, String> roles = HashBiMap.create();

  private final Multimap<String, Long> initialValues = HashMultimap.create();
  private final Multimap<String, Long> changes = HashMultimap.create();

  private Orientation roleOrientation = Orientation.HORIZONTAL;
  private Orientation columnLabelOrientation = Orientation.HORIZONTAL;

  private FaLabel roleOrientationToggle;
  private Toggle columnLabelOrientationToggle;

  private Long userId;

  @Override
  public void onShow(Presenter presenter) {
    HeaderView header = presenter.getHeader();

    if (header != null && !header.hasCommands()) {
      List<String> columns = Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, ALS_COMPANY_NAME);
      Relation relation = Relation.create(VIEW_USERS, columns);

      relation.disableEdit();
      relation.disableNewRow();

      final DataSelector userSelector = new DataSelector(relation, true);
      userSelector.setEditing(true);
      userSelector.addStyleName(STYLE_USER_SELECTOR);

      final Button userCommand = new Button(Localized.getConstants().user());
      userCommand.addStyleName(STYLE_USER_COMMAND);
      userCommand.addStyleName(STYLE_USER_EMPTY);

      final CustomDiv userClear = new CustomDiv(STYLE_USER_CLEAR);
      userClear.addStyleName(STYLE_USER_EMPTY);
      userClear.setText(String.valueOf(BeeConst.CHAR_TIMES));

      userSelector.addSelectorHandler(new SelectorEvent.Handler() {
        @Override
        public void onDataSelector(SelectorEvent event) {
          if (event.isChanged()) {
            long value = event.getValue();

            if (DataUtils.isId(value) && !Objects.equals(value, getUserId())) {
              setUserId(value);
              userCommand.setHtml(Global.getUsers().getSignature(value));

              userCommand.removeStyleName(STYLE_USER_EMPTY);
              userCommand.addStyleName(STYLE_USER_NOT_EMPTY);

              userClear.removeStyleName(STYLE_USER_EMPTY);
              userClear.addStyleName(STYLE_USER_NOT_EMPTY);

              resetFilter();

              checkUserRights();
            }
          }
        }
      });

      userCommand.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          userSelector.clearValue();
          userSelector.setFocus(true);

          userSelector.startEdit(null, DataSelector.SHOW_SELECTOR, null, null);
        }
      });

      userClear.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (getUserId() != null) {
            setUserId(null);

            userCommand.setHtml(Localized.getConstants().user());
            userCommand.removeStyleName(STYLE_USER_NOT_EMPTY);
            userCommand.addStyleName(STYLE_USER_EMPTY);

            userClear.removeStyleName(STYLE_USER_NOT_EMPTY);
            userClear.addStyleName(STYLE_USER_EMPTY);

            resetFilter();
          }
        }
      });

      header.addCommandItem(userCommand);
      header.addCommandItem(userClear);
      header.addCommandItem(userSelector);
    }

    init();
  }

  @Override
  protected Set<String> getChangedNames() {
    return changes.keySet();
  }

  @Override
  protected String getChangeMessage(RightsObject object) {
    return BeeUtils.joinWords(object.getCaption(),
        BeeUtils.bracket(changes.get(object.getName()).size()));
  }

  @Override
  protected String getPanelStyleName() {
    return STYLE_PREFIX + "multi-role";
  }

  protected abstract RightsState getRightsState();

  @Override
  protected int getValueStartCol() {
    return 2;
  }

  @Override
  protected void initData(final Consumer<Boolean> callback) {
    Roles.getData(new Consumer<Map<Long, String>>() {
      @Override
      public void accept(Map<Long, String> roleData) {
        if (BeeUtils.isEmpty(roleData)) {
          callback.accept(false);
          return;
        }

        if (!roles.isEmpty()) {
          roles.clear();
        }
        roles.putAll(roleData);

        ParameterList params = BeeKeeper.getRpc().createParameters(Service.GET_STATE_RIGHTS);
        params.addQueryItem(COL_OBJECT_TYPE, getObjectType().ordinal());
        params.addQueryItem(COL_STATE, getRightsState().ordinal());

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

                if (getRightsState().isChecked()) {
                  Set<Long> ids = Sets.newHashSet(roles.keySet());

                  for (RightsObject object : getObjects()) {
                    if (rights.containsKey(object.getName())) {
                      Set<Long> values = Sets.newHashSet(ids);
                      values.removeAll(DataUtils.parseIdSet(rights.get(object.getName())));

                      if (!values.isEmpty()) {
                        initialValues.putAll(object.getName(), values);
                      }

                    } else {
                      initialValues.putAll(object.getName(), ids);
                    }
                  }

                } else if (!rights.isEmpty()) {
                  for (Map.Entry<String, String> entry : rights.entrySet()) {
                    initialValues.putAll(entry.getKey(), DataUtils.parseIdSet(entry.getValue()));
                  }
                }

              } else if (getRightsState().isChecked()) {
                Set<Long> ids = Sets.newHashSet(roles.keySet());

                for (RightsObject object : getObjects()) {
                  initialValues.putAll(object.getName(), ids);
                }
              }

              callback.accept(true);
            }
          }
        });
      }
    });
  }

  @Override
  protected boolean isObjectChecked(String objectName) {
    for (Long roleId : roles.keySet()) {
      if (!isChecked(objectName, roleId)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void onObjectToggle(boolean checked) {
    List<Toggle> roleToggles = getRoleToggles();

    for (Toggle toggle : roleToggles) {
      if (checked) {
        toggle.setChecked(isRoleChecked(getRoleId(toggle)));
      } else if (toggle.isChecked()) {
        toggle.setChecked(false);
      }
    }
  }

  @Override
  protected void populateTable() {
    List<String> roleNames = new ArrayList<>();

    roleNames.addAll(roles.values());
    if (roleNames.size() > 1) {
      Collections.sort(roleNames);
    }

    if (roleOrientationToggle == null) {
      this.roleOrientationToggle = new FaLabel(FontAwesome.EXCHANGE, STYLE_ROLE_ORIENTATION);

      roleOrientationToggle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          switchRoleOrientation();
        }
      });
    }

    addRoleOrientationToggle();

    if (columnLabelOrientationToggle == null) {
      this.columnLabelOrientationToggle = new Toggle(FontAwesome.ELLIPSIS_H,
          FontAwesome.ELLIPSIS_V, STYLE_COLUMN_LABEL_ORIENTATION,
          columnLabelOrientation.isVertical());

      columnLabelOrientationToggle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          switchColumnLabelOrientation();
        }
      });
    }

    addColumnLabelOrientationToggle();

    int col = getValueStartCol();

    for (String roleName : roleNames) {
      long roleId = getRoleId(roleName);

      addColumnLabel(col, createRoleLabel(roleId, roleName), STYLE_ROLE_LABEL_CELL);
      addColumnToggle(col, createRoleToggle(roleId), STYLE_ROLE_TOGGLE_CELL);
      col++;
    }

    int row = getValueStartRow();

    for (RightsObject object : getObjects()) {
      addObjectLabel(row, object);
      addObjectToggle(row, object);

      col = getValueStartCol();

      String objectName = object.getName();
      String objectCaption = object.getCaption();

      for (String roleName : roleNames) {
        long roleId = getRoleId(roleName);
        String title = BeeUtils.joinWords(objectCaption, roleName);

        addValueToggle(row, col++, createValueToggle(objectName, roleId, title));
      }

      afterCreateValueRow(row, object);
      row++;
    }

    for (RightsObject object : getObjects()) {
      if (object.hasChildren()) {
        for (Long roleId : roles.keySet()) {
          if (!isChecked(object.getName(), roleId)) {
            enableChildren(object, roleId, false);
          }
        }
      }
    }

    if (roleOrientation.isVertical()) {
      transposeTable();
    }

    getTable().addStyleName(getRoleOrientationStyleName());
    getTable().addStyleName(getColumnLabelOrientationStyleName());
  }

  @Override
  protected void save(final Consumer<Boolean> callback) {
    if (changes.isEmpty()) {
      BeeKeeper.getScreen().notifyInfo("no changes");
      if (callback != null) {
        callback.accept(false);
      }

    } else {
      ParameterList params = BeeKeeper.getRpc().createParameters(Service.SET_STATE_RIGHTS);
      params.addQueryItem(COL_OBJECT_TYPE, getObjectType().ordinal());
      params.addQueryItem(COL_STATE, getRightsState().ordinal());

      Map<String, String> diff = Maps.newHashMap();
      for (String objectName : changes.keySet()) {
        diff.put(objectName, DataUtils.buildIdList(changes.get(objectName)));
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

            for (Map.Entry<String, Long> entry : changes.entries()) {
              if (initialValues.containsEntry(entry.getKey(), entry.getValue())) {
                initialValues.remove(entry.getKey(), entry.getValue());
              } else {
                initialValues.put(entry.getKey(), entry.getValue());
              }
            }

            changes.clear();
            onClearChanges();

            String message = BeeUtils.joinWords(getObjectType(), getRightsState(),
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

        boolean isChanged = toggleValue(objectName, getRoleId(widget));
        updateValueCell(widget, isChanged);
      }
    }

    RightsObject object = findObject(objectName);
    if (object != null && object.hasChildren()) {
      for (Long roleId : roles.keySet()) {
        if (checked && object.hasParent()) {
          boolean value = checked;

          String parentName = object.getParent();
          while (!BeeUtils.isEmpty(parentName)) {
            if (!isChecked(parentName, roleId)) {
              value = false;
              break;
            }

            RightsObject parent = findObject(parentName);
            if (parent != null && parent.hasParent()) {
              parentName = parent.getParent();
            } else {
              break;
            }
          }

          enableChildren(object, roleId, value);

        } else {
          enableChildren(object, roleId, checked);
        }
      }
    }
  }

  private void addColumnLabelOrientationToggle() {
    getTable().setWidget(COLUMN_LABEL_ORIENTATION_ROW, COLUMN_LABEL_ORIENTATION_COL,
        columnLabelOrientationToggle, STYLE_COLUMN_LABEL_ORIENTATION_CELL);
  }

  private void addRoleOrientationToggle() {
    getTable().setWidget(ROLE_ORIENTATION_ROW, ROLE_ORIENTATION_COL, roleOrientationToggle,
        STYLE_ROLE_ORIENTATION_CELL);
  }

  private void checkUserRights() {
    Queries.getRowSet(VIEW_USER_ROLES, Lists.newArrayList(COL_ROLE),
        Filter.equals(COL_USER, getUserId()), new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            if (DataUtils.isEmpty(result)) {
              BeeKeeper.getScreen().notifyWarning("user has no roles");

            } else {
              int index = result.getColumnIndex(COL_ROLE);

              Set<Long> userRoles = Sets.newHashSet();
              for (BeeRow row : result) {
                userRoles.add(row.getLong(index));
              }

              markUserValues(userRoles);
            }
          }
        });
  }

  private Widget createRoleLabel(final long roleId, String roleName) {
    final Label widget = new Label(roleName);
    widget.addStyleName(STYLE_ROLE_LABEL);

    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_ROLE, roleId);
    setDataType(widget, DATA_TYPE_ROLE_LABEL);

    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        RowEditor.open(VIEW_ROLES, roleId, Opener.MODAL, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            String name = Data.getString(VIEW_ROLES, result, COL_ROLE_NAME);

            if (!BeeUtils.equalsTrimRight(name, roles.get(roleId))) {
              roles.put(roleId, name);
              widget.setHtml(name);
            }
          }
        });
      }
    });

    return widget;
  }

  private Widget createRoleToggle(final long roleId) {
    boolean checked = true;
    for (RightsObject object : getObjects()) {
      if (!initialValues.containsEntry(object.getName(), roleId)) {
        checked = false;
        break;
      }
    }

    Toggle toggle = new Toggle(FontAwesome.SQUARE_O, FontAwesome.CHECK_SQUARE_O,
        STYLE_ROLE_TOGGLE, checked);

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_ROLE, roleId);
    setDataType(toggle, DATA_TYPE_ROLE_TOGGLE);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Toggle) {
          Toggle rt = (Toggle) event.getSource();
          updateRoleValueToggles(getRoleId(rt), rt.isChecked());

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

  private Toggle createValueToggle(String objectName, long roleId, String title) {
    Toggle toggle = createValueToggle(objectName);
    toggle.setTitle(title);

    if (initialValues.containsEntry(objectName, roleId)) {
      toggle.setChecked(true);
    }

    DomUtils.setDataProperty(toggle.getElement(), DATA_KEY_ROLE, roleId);

    toggle.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Toggle) {
          Toggle t = (Toggle) event.getSource();

          String name = getObjectName(t);
          Long role = getRoleId(t);

          boolean isChanged = toggleValue(name, role);
          updateValueCell(t, isChanged);

          Toggle objectToggle = getObjectToggle(name);
          Toggle roleToggle = getRoleToggle(role);

          if (t.isChecked()) {
            objectToggle.setChecked(isObjectChecked(name));
            roleToggle.setChecked(isRoleChecked(role));

          } else {
            objectToggle.setChecked(false);
            roleToggle.setChecked(false);
          }

          RightsObject object = findObject(name);
          if (object != null && object.hasChildren()) {
            enableChildren(object, role, t.isChecked());
          }
        }
      }
    });
    return toggle;
  }

  private void enableChildren(RightsObject object, Long roleId, boolean enabled) {
    List<RightsObject> children = Lists.newArrayList();

    for (RightsObject ro : getObjects()) {
      if (object.getName().equals(ro.getParent())) {
        children.add(ro);
      }
    }

    if (children.isEmpty()) {
      warning("object", object.getName(), "children not found");

    } else {
      for (Widget widget : getTable()) {
        if (isValueWidget(widget)
            && DomUtils.dataEquals(widget.getElement(), DATA_KEY_ROLE, roleId)) {

          String childName = getObjectName(widget);
          for (RightsObject child : children) {
            if (child.getName().equals(childName)) {
              enableValueWidet(widget, enabled);

              if (child.hasChildren()) {
                boolean childEnabled = enabled
                    && widget instanceof Toggle && ((Toggle) widget).isChecked();
                enableChildren(child, roleId, childEnabled);
              }
            }
          }
        }
      }
    }
  }

  private String getColumnLabelOrientationStyleName() {
    return STYLE_PREFIX + "column-label-" + columnLabelOrientation.name().toLowerCase();
  }

  private List<TableCellElement> getRoleCells(Long roleId) {
    List<TableCellElement> cells = Lists.newArrayList();

    NodeList<Element> nodes = Selectors.getNodes(getTable(),
        Selectors.attributeEquals(Attributes.DATA_PREFIX + DATA_KEY_ROLE, roleId));

    if (DomUtils.isEmpty(nodes)) {
      warning("role", roleId, "nodes nof found");

    } else {
      for (int i = 0; i < nodes.getLength(); i++) {
        TableCellElement cell = DomUtils.getParentCell(nodes.getItem(i), true);
        if (cell != null) {
          cells.add(cell);
        }
      }
    }

    return cells;
  }

  private Long getRoleId(String roleName) {
    return roles.inverse().get(roleName);
  }

  private String getRoleOrientationStyleName() {
    return STYLE_PREFIX + "role-" + roleOrientation.name().toLowerCase();
  }

  private Toggle getRoleToggle(long roleId) {
    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && isDataType(widget, DATA_TYPE_ROLE_TOGGLE)
          && DomUtils.dataEquals(widget.getElement(), DATA_KEY_ROLE, roleId)) {
        return (Toggle) widget;
      }
    }

    severe("role", roleId, "toggle not found");
    return null;
  }

  private List<Toggle> getRoleToggles() {
    List<Toggle> toggles = Lists.newArrayList();

    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && isDataType(widget, DATA_TYPE_ROLE_TOGGLE)) {
        toggles.add((Toggle) widget);
      }
    }
    return toggles;
  }

  private Long getUserId() {
    return userId;
  }

  private boolean isChecked(String objectName, long roleId) {
    return initialValues.containsEntry(objectName, roleId)
        != changes.containsEntry(objectName, roleId);
  }

  private boolean isRoleChecked(long roleId) {
    for (RightsObject object : getObjects()) {
      if (!isChecked(object.getName(), roleId)) {
        return false;
      }
    }
    return true;
  }

  private void markUserValues(Set<Long> userRoles) {
    for (Long roleId : roles.keySet()) {
      if (!userRoles.contains(roleId)) {
        setNotMatched(getRoleCells(roleId));
      }
    }

    for (RightsObject object : getObjects()) {
      String objectName = object.getName();

      boolean checked = false;
      for (Long roleId : userRoles) {
        if (isChecked(objectName, roleId)) {
          checked = true;
          break;
        }
      }

      if (!checked) {
        setNotMatched(getObjectCells(objectName));
      }
    }
  }

  private void setUserId(Long userId) {
    this.userId = userId;
  }

  private void switchColumnLabelOrientation() {
    getTable().removeStyleName(getColumnLabelOrientationStyleName());
    this.columnLabelOrientation = columnLabelOrientation.invert();
    getTable().addStyleName(getColumnLabelOrientationStyleName());
  }

  private void switchRoleOrientation() {
    transposeTable();

    getTable().removeStyleName(getRoleOrientationStyleName());
    this.roleOrientation = roleOrientation.invert();
    getTable().addStyleName(getRoleOrientationStyleName());
  }

  private boolean toggleValue(String objectName, Long roleId) {
    if (BeeUtils.isEmpty(objectName)) {
      severe("toggle value: object name not specified");
      return false;
    } else if (roleId == null) {
      severe("toggle value: role id not specified");
      return false;

    } else if (changes.containsEntry(objectName, roleId)) {
      changes.remove(objectName, roleId);
      return false;
    } else {
      changes.put(objectName, roleId);
      return true;
    }
  }

  private void transposeTable() {
    getTable().remove(roleOrientationToggle);
    getTable().remove(columnLabelOrientationToggle);

    getTable().transpose();

    addRoleOrientationToggle();
    addColumnLabelOrientationToggle();

    markValueRows();
  }

  private void updateRoleValueToggles(long roleId, boolean checked) {
    for (Widget widget : getTable()) {
      if (widget instanceof Toggle && ((Toggle) widget).isChecked() != checked
          && isValueWidget(widget)
          && DomUtils.dataEquals(widget.getElement(), DATA_KEY_ROLE, roleId)) {

        ((Toggle) widget).setChecked(checked);

        boolean isChanged = toggleValue(getObjectName(widget), roleId);
        updateValueCell(widget, isChanged);

        if (checked) {
          enableValueWidet(widget, checked);
        }
      }
    }

    if (!checked) {
      for (RightsObject object : getObjects()) {
        if (object.hasChildren() && !object.hasParent()) {
          enableChildren(object, roleId, checked);
        }
      }
    }
  }
}
