package com.butent.bee.client.modules.payroll;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.*;

import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IdPair;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.payroll.PayrollConstants.ObjectStatus;
import com.butent.bee.shared.modules.payroll.PayrollConstants.WorkScheduleKind;
import com.butent.bee.shared.time.DateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.YearMonth;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class EmployeeSchedule extends WorkScheduleWidget {

  private final long employeeId;

  EmployeeSchedule(long employeeId, WorkScheduleKind kind) {
    super(kind, ScheduleParent.EMPLOYEE);

    this.employeeId = employeeId;
  }

  @Override
  public String getCaption() {
    BeeRow row = findEmployee(employeeId);

    if (row == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinItems(
          BeeUtils.joinWords(DataUtils.getString(getEmData(), row, COL_FIRST_NAME),
              DataUtils.getString(getEmData(), row, COL_LAST_NAME)),
          DataUtils.getString(getEmData(), row, ALS_COMPANY_NAME),
          BeeUtils.joinWords(Localized.getLabel(getEmData().getColumn(COL_TAB_NUMBER)),
              DataUtils.getString(getEmData(), row, COL_TAB_NUMBER)));
    }
  }

  @Override
  protected void clearData() {
    super.clearData();
    setObData(null);
  }

  @Override
  protected List<Partition> filterPartitions(DateRange filterRange) {
    List<Partition> result = new ArrayList<>();

    if (!DataUtils.isEmpty(getObData())) {
      Set<Long> mainWs = new HashSet<>();
      Set<Long> mainEo = new HashSet<>();

      Multimap<Long, Long> substWs = HashMultimap.create();
      Multimap<Long, Long> substEo = HashMultimap.create();

      if (!DataUtils.isEmpty(getWsData())) {
        int objectIndex = getWsData().getColumnIndex(COL_PAYROLL_OBJECT);
        int substIndex = getWsData().getColumnIndex(COL_SUBSTITUTE_FOR);

        int dateIndex = getWsData().getColumnIndex(COL_WORK_SCHEDULE_DATE);

        for (BeeRow row : getWsData()) {
          if (filterRange.contains(row.getDate(dateIndex))) {
            Long obj = row.getLong(objectIndex);

            if (DataUtils.isId(obj)) {
              Long subst = row.getLong(substIndex);

              if (DataUtils.isId(subst) && !Objects.equals(obj, subst)) {
                substWs.put(obj, subst);
              } else {
                mainWs.add(obj);
              }
            }
          }
        }
      }

      if (!DataUtils.isEmpty(getEoData())) {
        int objectIndex = getEoData().getColumnIndex(COL_PAYROLL_OBJECT);
        int substIndex = getEoData().getColumnIndex(COL_SUBSTITUTE_FOR);

        int fromIndex = getEoData().getColumnIndex(COL_EMPLOYEE_OBJECT_FROM);
        int untilIndex = getEoData().getColumnIndex(COL_EMPLOYEE_OBJECT_UNTIL);

        for (BeeRow row : getEoData()) {
          DateRange range = DateRange.closed(row.getDate(fromIndex), row.getDate(untilIndex));
          if (filterRange.intersects(range)) {
            Long obj = row.getLong(objectIndex);

            if (DataUtils.isId(obj)) {
              Long subst = row.getLong(substIndex);

              if (DataUtils.isId(subst) && !Objects.equals(obj, subst)) {
                if (isSubstitutionEnabled()) {
                  substEo.put(obj, subst);
                }
              } else {
                mainEo.add(obj);
              }
            }
          }
        }
      }

      if (!mainWs.isEmpty() || !mainEo.isEmpty() || !substWs.isEmpty() || !substEo.isEmpty()) {
        int statusIndex = getObData().getColumnIndex(COL_LOCATION_STATUS);

        DateRange employeeRange = getEmployeeRange(employeeId);
        boolean intersects = employeeRange == null || filterRange.intersects(employeeRange);

        for (BeeRow row : getObData()) {
          long id = row.getId();

          ObjectStatus status = EnumUtils.getEnumByIndex(ObjectStatus.class,
              row.getInteger(statusIndex));

          if (mainWs.contains(id)) {
            result.add(new Partition(row));

          } else if (intersects && status == ObjectStatus.ACTIVE && mainEo.contains(id)) {
            result.add(new Partition(row));
          }

          if (substWs.containsKey(id)) {
            for (Long subst : substWs.get(id)) {
              result.add(new Partition(row, subst));
            }
          }

          if (intersects && status == ObjectStatus.ACTIVE && substEo.containsKey(id)) {
            for (Long subst : substEo.get(id)) {
              if (!substWs.containsEntry(id, subst)) {
                result.add(new Partition(row, subst));
              }
            }
          }
        }
      }
    }

    return result;
  }

  @Override
  protected long getEmployeeId(long partId) {
    return employeeId;
  }

  @Override
  protected String getPartitionCaption(long partId) {
    return getObjectName(partId);
  }

  @Override
  protected List<Integer> getPartitionContactIndexes() {
    return Collections.singletonList(getObData().getColumnIndex(COL_ADDRESS));
  }

  @Override
  protected List<BeeColumn> getPartitionDataColumns() {
    return getObData().getColumns();
  }

  @Override
  protected List<Integer> getPartitionInfoIndexes() {
    return Collections.singletonList(getObData().getColumnIndex(ALS_COMPANY_NAME));
  }

  @Override
  protected List<Integer> getPartitionNameIndexes() {
    return Collections.singletonList(getObData().getColumnIndex(COL_LOCATION_NAME));
  }

  @Override
  protected long getRelationId() {
    return employeeId;
  }

  @Override
  protected Filter getWorkScheduleRelationFilter() {
    return Filter.equals(COL_EMPLOYEE, employeeId);
  }

  @Override
  protected void initCalendarInfo(YearMonth ym, CalendarInfo calendarInfo) {
    calendarInfo.setTcChanges(getTimeCardChanges(employeeId, ym));

    BeeRow employee = DataUtils.isEmpty(getEmData()) ? null : getEmData().getRow(0);
    JustDate activeFrom = (employee == null)
        ? null : DataUtils.getDate(getEmData(), employee, COL_DATE_OF_EMPLOYMENT);
    JustDate activeUntil = (employee == null)
        ? null : DataUtils.getDate(getEmData(), employee, COL_DATE_OF_DISMISSAL);

    calendarInfo.setInactiveDays(getInactiveDays(ym, activeFrom, activeUntil));
  }

  @Override
  protected boolean isActive(YearMonth ym) {
    return ym != null && ym.getRange().intersects(getEmployeeRange(employeeId));
  }

  @Override
  protected Widget renderAppender(Collection<IdPair> partIds, YearMonth ym,
      String selectorStyleName) {

    Flow panel = new Flow();

    Relation relation = Relation.create();
    relation.setViewName(VIEW_LOCATIONS);
    relation.disableNewRow();
    relation.disableEdit();

    relation.setChoiceColumns(Lists.newArrayList(COL_LOCATION_NAME, ALS_COMPANY_NAME, COL_ADDRESS));

    Filter filter = Filter.equals(COL_LOCATION_STATUS, ObjectStatus.ACTIVE);
    relation.setFilter(filter);

    UnboundSelector selector = UnboundSelector.create(relation,
        Lists.newArrayList(COL_LOCATION_NAME));

    selector.addStyleName(selectorStyleName);
    DomUtils.setPlaceholder(selector, Localized.dictionary().newObjectLocation());

    if (!BeeUtils.isEmpty(partIds)) {
      Set<Long> ids = new HashSet<>();
      for (IdPair pair : partIds) {
        if (!pair.hasB()) {
          ids.add(pair.getA());
        }
      }

      selector.getOracle().setExclusions(ids);
    }

    selector.addSelectorHandler(event -> {
      if (event.isChanged() && DataUtils.hasId(event.getRelatedRow())) {
        addEmployeeObject(employeeId, event.getRelatedRow().getId(), true);
      }
    });

    panel.add(selector);
    return panel;
  }

  @Override
  protected void updateCalendarInfo(YearMonth ym, Partition partition, CalendarInfo calendarInfo) {
  }

  @Override
  void refresh() {
    Set<String> viewNames = new HashSet<>();
    Map<String, Filter> filters = new HashMap<>();

    String employeeIdColumn = Data.getIdColumn(VIEW_EMPLOYEES);

    Filter wsFilter = getWorkScheduleFilter();
    Filter eoFilter = Filter.equals(COL_EMPLOYEE, employeeId);

    viewNames.add(VIEW_EMPLOYEES);
    filters.put(VIEW_EMPLOYEES,
        Filter.or(Filter.compareId(employeeId),
            Filter.in(employeeIdColumn, VIEW_WORK_SCHEDULE, COL_SUBSTITUTE_FOR, wsFilter),
            Filter.in(employeeIdColumn, VIEW_EMPLOYEE_OBJECTS, COL_SUBSTITUTE_FOR, eoFilter)));

    viewNames.add(VIEW_WORK_SCHEDULE);
    filters.put(VIEW_WORK_SCHEDULE, wsFilter);

    viewNames.add(VIEW_EMPLOYEE_OBJECTS);
    filters.put(VIEW_EMPLOYEE_OBJECTS, eoFilter);

    viewNames.add(VIEW_TIME_CARD_CHANGES);
    filters.put(VIEW_TIME_CARD_CHANGES, Filter.and(Filter.equals(COL_EMPLOYEE, employeeId),
        getTimeCardChangesFilter()));

    viewNames.add(VIEW_TIME_CARD_CODES);

    WorkScheduleKind kind = getWorkScheduleKind();

    if (kind != null) {
      filters.put(VIEW_TIME_CARD_CODES, Filter.notNull(kind.getTccColumnName()));
    }
    viewNames.add(VIEW_TIME_RANGES);
    viewNames.add(VIEW_WORK_SCHEDULE_LOCKS);
    filters.put(VIEW_WORK_SCHEDULE_LOCKS, wsFilter);

    Queries.getData(viewNames, filters, CachingPolicy.NONE, new Queries.DataCallback() {
      @Override
      public void onSuccess(Collection<BeeRowSet> result) {
        clearData();

        for (BeeRowSet rowSet : result) {
          switch (rowSet.getViewName()) {
            case VIEW_EMPLOYEES:
              setEmData(rowSet);
              break;

            case VIEW_WORK_SCHEDULE:
              setWsData(rowSet);
              break;

            case VIEW_EMPLOYEE_OBJECTS:
              setEoData(rowSet);
              break;

            case VIEW_TIME_CARD_CHANGES:
              setTcData(rowSet);
              break;

            case VIEW_TIME_CARD_CODES:
              setTimeCardCodes(rowSet);
              break;

            case VIEW_TIME_RANGES:
              setTimeRanges(rowSet);
              break;
            case VIEW_WORK_SCHEDULE_LOCKS:
              setTableLocks(rowSet);
              break;
          }
        }

        ClassifierKeeper.getHolidays(input -> {
          setHolidays(input);
          getObjects(() -> render());
        });
      }
    });
  }

  private void getObjects(final Runnable callback) {
    Set<Long> objects = new HashSet<>();

    if (!DataUtils.isEmpty(getWsData())) {
      objects.addAll(getWsData().getDistinctLongs(getWsData().getColumnIndex(COL_PAYROLL_OBJECT)));
    }
    if (!DataUtils.isEmpty(getEoData())) {
      objects.addAll(getEoData().getDistinctLongs(getEoData().getColumnIndex(COL_PAYROLL_OBJECT)));
    }

    if (objects.isEmpty()) {
      callback.run();

    } else {
      Queries.getRowSet(VIEW_LOCATIONS, null, Filter.idIn(objects), new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          setObData(result);
          callback.run();
        }
      });
    }
  }
}
