package com.butent.bee.client.modules.administration;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.DndDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class CompanyStructureForm extends AbstractFormInterceptor implements HandlesAllDataEvents {

  private static final BeeLogger logger = LogUtils.getLogger(CompanyStructureForm.class);

  private static final List<String> viewNames = Lists.newArrayList(VIEW_DEPARTMENTS,
      VIEW_DEPARTMENT_EMPLOYEES);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "orgChart-";

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_NODE_ROW = STYLE_PREFIX + "node-row";
  // private static final String STYLE_CONN_ROW = STYLE_PREFIX + "conn-row";

  private static final String STYLE_LEVEL_PREFIX = STYLE_PREFIX + "level-";

  private static final String STYLE_DEPARTMENT_PREFIX = STYLE_PREFIX + "department-";
  private static final String STYLE_DEPARTMENT_CELL = STYLE_DEPARTMENT_PREFIX + "cell";
  private static final String STYLE_DEPARTMENT_PANEL = STYLE_DEPARTMENT_PREFIX + "panel";
  private static final String STYLE_DEPARTMENT_LABEL = STYLE_DEPARTMENT_PREFIX + "label";

  private static final String STYLE_DEPARTMENT_DRAG = STYLE_DEPARTMENT_PREFIX + "drag";
  private static final String STYLE_DEPARTMENT_DRAG_OVER = STYLE_DEPARTMENT_PREFIX + "dragOver";

  private static final String STYLE_BOSS_PREFIX = STYLE_PREFIX + "boss-";
  private static final String STYLE_BOSS_CONTAINER = STYLE_BOSS_PREFIX + "container";
  private static final String STYLE_BOSS_LABEL = STYLE_BOSS_PREFIX + "label";
  private static final String STYLE_BOSS_PHOTO = STYLE_BOSS_PREFIX + "photo";
  private static final String STYLE_BOSS_DRAG = STYLE_BOSS_PREFIX + "drag";

  private static final String STYLE_EMPLOYEE_PREFIX = STYLE_PREFIX + "employee-";
  private static final String STYLE_EMPLOYEE_PANEL = STYLE_EMPLOYEE_PREFIX + "panel";
  private static final String STYLE_EMPLOYEE_CONTAINER = STYLE_EMPLOYEE_PREFIX + "container";
  private static final String STYLE_EMPLOYEE_LABEL = STYLE_EMPLOYEE_PREFIX + "label";
  private static final String STYLE_EMPLOYEE_PHOTO = STYLE_EMPLOYEE_PREFIX + "photo";
  private static final String STYLE_EMPLOYEE_DRAG = STYLE_EMPLOYEE_PREFIX + "drag";

  private static final String DATA_TYPE_DEPARTMENT = "OrgChartDepartment";
  private static final String DATA_TYPE_BOSS = "OrgChartBoss";
  private static final String DATA_TYPE_EMPLOYEE = "OrgChartEmployee";

  private static final Set<String> DND_TYPES = ImmutableSet.of(DATA_TYPE_DEPARTMENT,
      DATA_TYPE_BOSS, DATA_TYPE_EMPLOYEE);

  private final List<HandlerRegistration> handlerRegistry = new ArrayList<>();

  private BeeRowSet departments;
  private BeeRowSet employees;

  CompanyStructureForm() {
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {
    switch (action) {
      case ADD:
        RowFactory.createRow(VIEW_DEPARTMENTS, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            refresh();
          }
        });
        return false;

      case REFRESH:
        refresh();
        return false;

      default:
        return super.beforeAction(action, presenter);
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new CompanyStructureForm();
  }

  @Override
  public void onLoad(FormView form) {
    if (handlerRegistry.isEmpty()) {
      handlerRegistry.addAll(BeeKeeper.getBus().registerDataHandler(this, false));
    }
    refresh();

    super.onLoad(form);
  }

  @Override
  public void onUnload(FormView form) {
    EventUtils.clearRegistry(handlerRegistry);
    super.onUnload(form);
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    if (isEventRelevant(event)) {
      refresh();
    }
  }

  private static boolean isEventRelevant(DataEvent event) {
    for (String viewName : viewNames) {
      if (event.hasView(viewName)) {
        return true;
      }
    }
    return false;
  }

  private Flow getPanel() {
    IdentifiableWidget widget = getFormView().getRootWidget();
    if (widget instanceof Flow) {
      return (Flow) widget;
    } else {
      logger.severe(NameUtils.getName(this), "panel not found");
      return null;
    }
  }

  private Multimap<Integer, Long> layout() {
    List<Long> rowIds = departments.getRowIds();

    Multimap<Long, Long> children = ArrayListMultimap.create();
    Multimap<Integer, Long> levels = ArrayListMultimap.create();

    int parentIndex = departments.getColumnIndex(COL_DEPARTMENT_PARENT);
    for (BeeRow row : departments) {
      Long id = row.getId();
      Long parent = row.getLong(parentIndex);

      if (DataUtils.isId(parent) && !Objects.equals(id, parent) && rowIds.contains(id)) {
        children.put(parent, id);
      } else {
        levels.put(0, id);
      }
    }

    for (int level = 0; level < departments.getNumberOfRows(); level++) {
      List<Long> parents = new ArrayList<>(levels.get(level));
      if (parents.isEmpty()) {
        break;
      }

      for (Long parent : parents) {
        if (children.containsKey(parent)) {
          for (Long child : children.get(parent)) {
            if (!levels.containsValue(child)) {
              levels.put(level + 1, child);
            }
          }
        }
      }
    }

    return levels;
  }

  private void refresh() {
    Queries.getData(viewNames, CachingPolicy.NONE, new Queries.DataCallback() {
      @Override
      public void onSuccess(Collection<BeeRowSet> result) {
        for (BeeRowSet rowSet : result) {
          switch (rowSet.getViewName()) {
            case VIEW_DEPARTMENTS:
              departments = rowSet;
              break;

            case VIEW_DEPARTMENT_EMPLOYEES:
              employees = rowSet;
              break;
          }
        }

        Flow panel = getPanel();
        if (panel != null) {
          panel.clear();

          if (!DataUtils.isEmpty(departments)) {
            render(panel, layout());
          }
        }
      }
    });
  }

  private void render(Flow panel, Multimap<Integer, Long> levels) {
    HtmlTable table = new HtmlTable(STYLE_TABLE);

    int row = 0;
    int col = 0;

    for (int level : levels.keySet()) {
      col = 0;

      for (long depId : levels.get(level)) {
        table.setWidget(row, col++, renderDepartment(depId), STYLE_DEPARTMENT_CELL);
      }

      table.getRowFormatter().addStyleName(row, STYLE_NODE_ROW);
      table.getRowFormatter().addStyleName(row, STYLE_LEVEL_PREFIX + BeeUtils.toString(level));

      row++;
    }

    panel.add(table);
  }

  private Widget renderDepartment(final long id) {
    final Flow panel = new Flow(STYLE_DEPARTMENT_PANEL);

    BeeRow department = departments.getRowById(id);

    DndDiv label = new DndDiv(STYLE_DEPARTMENT_LABEL);

    String name = DataUtils.getString(departments, department, COL_DEPARTMENT_NAME);
    label.setText(name);

    String fullName = department.getProperty(PROP_DEPARTMENT_FULL_NAME);
    if (!BeeUtils.isEmpty(fullName) && !BeeUtils.equalsTrim(name, fullName)) {
      label.setTitle(fullName);
    }

    label.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        RowEditor.open(VIEW_DEPARTMENTS, id, Opener.MODAL, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            refresh();
          }
        });
      }
    });

    DndHelper.makeSource(label, DATA_TYPE_DEPARTMENT, id, STYLE_DEPARTMENT_DRAG);

    panel.add(label);

    Long head = DataUtils.getLong(departments, department, COL_DEPARTMENT_HEAD);
    if (DataUtils.isId(head) && !DataUtils.isEmpty(employees)) {
      BeeRow employee = employees.getRowById(head);
      if (employee != null) {
        panel.add(renderEmployee(employee, true));
      }
    }

    List<BeeRow> depEmployees = filterEmployees(id, head);
    if (!depEmployees.isEmpty()) {
      panel.add(renderEmployees(depEmployees));
    }

    DndHelper.makeTarget(panel, DND_TYPES, STYLE_DEPARTMENT_DRAG_OVER,
        new Predicate<Object>() {
          @Override
          public boolean apply(Object input) {
            if (validateDataType(DndHelper.getDataType()) && validateDndContent(input)) {
              return isTarget(DndHelper.getDataType(), (Long) input, id);
            } else {
              return false;
            }
          }
        },
        new BiConsumer<DropEvent, Object>() {
          @Override
          public void accept(DropEvent t, Object u) {
            panel.removeStyleName(STYLE_DEPARTMENT_DRAG_OVER);

            if (validateDataType(DndHelper.getDataType()) && validateDndContent(u)) {
              acceptDrop(DndHelper.getDataType(), (Long) u, id);
            }
          }
        });

    return panel;
  }

  private static boolean validateDataType(String dataType) {
    return DND_TYPES.contains(dataType);
  }

  private static boolean validateDndContent(Object content) {
    if (content instanceof Long) {
      return DataUtils.isId((Long) content);
    } else {
      return false;
    }
  }

  private BeeRow findEmployee(long depId, long persId) {
    if (DataUtils.isEmpty(employees)) {
      return null;
    }

    int departmentIndex = employees.getColumnIndex(COL_DEPARTMENT);
    int personIndex = employees.getColumnIndex(COL_COMPANY_PERSON);

    for (BeeRow row : employees) {
      if (Objects.equals(row.getLong(departmentIndex), depId)
          && Objects.equals(row.getLong(personIndex), persId)) {
        return row;
      }
    }
    return null;
  }

  private boolean hasEmployee(long depId, long persId) {
    return findEmployee(depId, persId) != null;
  }

  private Long getEmployeeRelation(long emplId, String colName) {
    if (DataUtils.isEmpty(employees)) {
      return null;
    }

    BeeRow row = employees.getRowById(emplId);
    if (row == null) {
      return null;
    } else {
      return DataUtils.getLong(employees, row, colName);
    }
  }

  private Long getDepartmentParent(long depId) {
    if (DataUtils.isEmpty(departments)) {
      return null;
    }

    BeeRow row = departments.getRowById(depId);
    if (row == null) {
      return null;
    } else {
      return DataUtils.getLong(departments, row, COL_DEPARTMENT_PARENT);
    }
  }

  private boolean isTarget(String dataType, long source, long target) {
    switch (dataType) {
      case DATA_TYPE_DEPARTMENT:
        return !Objects.equals(source, target)
            && !Objects.equals(getDepartmentParent(source), target);

      case DATA_TYPE_BOSS:
      case DATA_TYPE_EMPLOYEE:
        Long persId = getEmployeeRelation(source, COL_COMPANY_PERSON);
        return DataUtils.isId(persId) && !hasEmployee(target, persId);

      default:
        return false;
    }
  }

  private void acceptDrop(String dataType, final long source, final long target) {
    switch (dataType) {
      case DATA_TYPE_DEPARTMENT:
        Long unbind = null;
        if (!DataUtils.isEmpty(departments)) {
          Long parent = target;

          for (int i = 0; i < departments.getNumberOfRows(); i++) {
            Long p = getDepartmentParent(parent);

            if (p == null) {
              break;
            } else if (Objects.equals(p, source)) {
              unbind = parent;
              break;
            } else {
              parent = p;
            }
          }
        }

        final IntCallback callback = new IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            if (BeeUtils.isPositive(result)) {
              fireRefresh(VIEW_DEPARTMENTS);
            }
          }
        };

        if (unbind == null) {
          updateDepartmentParent(source, target, callback);

        } else {
          updateDepartmentParent(unbind, getDepartmentParent(source), new IntCallback() {
            @Override
            public void onSuccess(Integer result) {
              if (BeeUtils.isPositive(result)) {
                updateDepartmentParent(source, target, callback);
              }
            }
          });
        }

        break;

      case DATA_TYPE_BOSS:
        Long oldDep = getEmployeeRelation(source, COL_DEPARTMENT);
        Queries.update(VIEW_DEPARTMENTS, oldDep, COL_DEPARTMENT_HEAD, LongValue.getNullValue(),
            new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                if (BeeUtils.isPositive(result)) {
                  fireRefresh(VIEW_DEPARTMENTS);
                  updateEmployeeDepartment(source, target);
                }
              }
            });

        break;

      case DATA_TYPE_EMPLOYEE:
        updateEmployeeDepartment(source, target);
        break;
    }
  }

  private static void updateDepartmentParent(long depId, Long parent, IntCallback callback) {
    Queries.update(VIEW_DEPARTMENTS, depId, COL_DEPARTMENT_PARENT, new LongValue(parent), callback);
  }

  private static void updateEmployeeDepartment(long emplId, long depId) {
    Queries.update(VIEW_DEPARTMENT_EMPLOYEES, emplId, COL_DEPARTMENT, new LongValue(depId),
        new IntCallback() {
          @Override
          public void onSuccess(Integer result) {
            if (BeeUtils.isPositive(result)) {
              fireRefresh(VIEW_DEPARTMENT_EMPLOYEES);
            }
          }
        });
  }

  private List<BeeRow> filterEmployees(Long depId, Long exclude) {
    if (DataUtils.isEmpty(employees)) {
      return Collections.emptyList();

    } else {
      List<BeeRow> rows = DataUtils.filterRows(employees, COL_DEPARTMENT, depId);

      if (!rows.isEmpty() && DataUtils.isId(exclude)) {
        int index = BeeConst.UNDEF;
        for (int i = 0; i < rows.size(); i++) {
          if (DataUtils.idEquals(rows.get(i), exclude)) {
            index = i;
            break;
          }
        }

        if (!BeeConst.isUndef(index)) {
          rows.remove(index);
        }
      }
      return rows;
    }
  }

  private Widget renderEmployee(BeeRow employee, boolean boss) {
    Flow container = new Flow();
    container.addStyleName(boss ? STYLE_BOSS_CONTAINER : STYLE_EMPLOYEE_CONTAINER);

    final long emplId = employee.getId();

    String fullName = BeeUtils.joinWords(
        DataUtils.getString(employees, employee, COL_FIRST_NAME),
        DataUtils.getString(employees, employee, COL_LAST_NAME));

    String positionName = DataUtils.getString(employees, employee, ALS_POSITION_NAME);
    String companyName = DataUtils.getString(employees, employee, ALS_COMPANY_NAME);

    String photo = DataUtils.getString(employees, employee, COL_PHOTO);
    if (!BeeUtils.isEmpty(photo)) {
      Image image = new Image(PhotoRenderer.getUrl(photo));
      image.addStyleName(boss ? STYLE_BOSS_PHOTO : STYLE_EMPLOYEE_PHOTO);

      image.setTitle(BeeUtils.buildLines(fullName, positionName, companyName));

      image.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Long person = getEmployeeRelation(emplId, COL_PERSON);
          RowEditor.open(VIEW_PERSONS, person, Opener.MODAL, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              refresh();
            }
          });
        }
      });

      container.add(image);
    }

    Label label = new Label(fullName);
    label.addStyleName(boss ? STYLE_BOSS_LABEL : STYLE_EMPLOYEE_LABEL);

    label.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Long cp = getEmployeeRelation(emplId, COL_COMPANY_PERSON);
        RowEditor.open(VIEW_COMPANY_PERSONS, cp, Opener.MODAL, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            refresh();
          }
        });
      }
    });

    container.add(label);

    if (boss) {
      DndHelper.makeSource(container, DATA_TYPE_BOSS, emplId, STYLE_BOSS_DRAG);
    } else {
      DndHelper.makeSource(container, DATA_TYPE_EMPLOYEE, emplId, STYLE_EMPLOYEE_DRAG);
    }

    return container;
  }

  private Widget renderEmployees(List<BeeRow> depEmployees) {
    Flow panel = new Flow(STYLE_EMPLOYEE_PANEL);

    for (BeeRow employee : depEmployees) {
      panel.add(renderEmployee(employee, false));
    }

    return panel;
  }

  private static void fireRefresh(String viewName) {
    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), viewName);
  }
}
