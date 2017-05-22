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
import com.butent.bee.shared.data.value.DateValue;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

class LocationSchedule extends WorkScheduleWidget {

  private static final String VALUE_SEPARATOR = BeeConst.STRING_COLON + BeeConst.STRING_SPACE;

  private static final String dateOfEmploymentLabel =
      Data.getColumnLabel(VIEW_EMPLOYEES, COL_DATE_OF_EMPLOYMENT);
  private static final String dateOfDismissalLabel =
      Data.getColumnLabel(VIEW_EMPLOYEES, COL_DATE_OF_DISMISSAL);

  private final long objectId;

  LocationSchedule(long objectId, WorkScheduleKind kind) {
    super(kind, ScheduleParent.LOCATION);

    this.objectId = objectId;
  }

  @Override
  public String getCaption() {
    BeeRow row = findObject(objectId);

    if (row == null) {
      return BeeConst.STRING_EMPTY;
    } else {
      return BeeUtils.joinItems(DataUtils.getString(getObData(), row, COL_LOCATION_NAME),
          DataUtils.getString(getObData(), row, COL_ADDRESS),
          DataUtils.getString(getObData(), row, ALS_COMPANY_NAME));
    }
  }

  @Override
  protected void clearData() {
    super.clearData();
    setEmData(null);
  }

  @Override
  protected List<Partition> filterPartitions(DateRange filterRange) {
    List<Partition> result = new ArrayList<>();

    if (!DataUtils.isEmpty(getEmData())) {
      Set<Long> mainWs = new HashSet<>();
      Set<Long> mainEo = new HashSet<>();

      Multimap<Long, Long> substWs = HashMultimap.create();
      Multimap<Long, Long> substEo = HashMultimap.create();

      if (!DataUtils.isEmpty(getWsData())) {
        int employeeIndex = getWsData().getColumnIndex(COL_EMPLOYEE);
        int substIndex = getWsData().getColumnIndex(COL_SUBSTITUTE_FOR);

        int dateIndex = getWsData().getColumnIndex(COL_WORK_SCHEDULE_DATE);

        for (BeeRow row : getWsData()) {
          if (filterRange.contains(row.getDate(dateIndex))) {
            Long empl = row.getLong(employeeIndex);

            if (DataUtils.isId(empl)) {
              Long subst = row.getLong(substIndex);

              if (DataUtils.isId(subst) && !Objects.equals(empl, subst)) {
                substWs.put(empl, subst);
              } else {
                mainWs.add(empl);
              }
            }
          }
        }
      }

      if (!DataUtils.isEmpty(getEoData())) {
        int employeeIndex = getEoData().getColumnIndex(COL_EMPLOYEE);
        int substIndex = getEoData().getColumnIndex(COL_SUBSTITUTE_FOR);

        int fromIndex = getEoData().getColumnIndex(COL_EMPLOYEE_OBJECT_FROM);
        int untilIndex = getEoData().getColumnIndex(COL_EMPLOYEE_OBJECT_UNTIL);

        for (BeeRow row : getEoData()) {
          DateRange range = DateRange.closed(row.getDate(fromIndex), row.getDate(untilIndex));
          if (filterRange.intersects(range)) {
            Long empl = row.getLong(employeeIndex);

            if (DataUtils.isId(empl)) {
              Long subst = row.getLong(substIndex);

              if (DataUtils.isId(subst) && !Objects.equals(empl, subst)) {
                if (isSubstitutionEnabled()) {
                  substEo.put(empl, subst);
                }
              } else {
                mainEo.add(empl);
              }
            }
          }
        }
      }

      if (!mainWs.isEmpty() || !mainEo.isEmpty() || !substWs.isEmpty() || !substEo.isEmpty()) {
        int fromIndex = getEmData().getColumnIndex(COL_DATE_OF_EMPLOYMENT);
        int untilIndex = getEmData().getColumnIndex(COL_DATE_OF_DISMISSAL);

        for (BeeRow row : getEmData()) {
          long id = row.getId();
          DateRange range = DateRange.closed(row.getDate(fromIndex), row.getDate(untilIndex));

          if (mainWs.contains(id)) {
            result.add(new Partition(row));

          } else if (mainEo.contains(id) && filterRange.intersects(range)) {
            result.add(new Partition(row));
          }

          if (substWs.containsKey(id)) {
            for (Long subst : substWs.get(id)) {
              result.add(new Partition(row, subst));
            }
          }

          if (substEo.containsKey(id) && filterRange.intersects(range)) {
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
    return partId;
  }

  @Override
  protected String getPartitionCaption(long partId) {
    return getEmployeeFullName(partId);
  }

  @Override
  protected List<Integer> getPartitionContactIndexes() {
    List<Integer> contactIndexes = new ArrayList<>();
    contactIndexes.add(getEmData().getColumnIndex(ALS_DEPARTMENT_NAME));
    contactIndexes.add(getEmData().getColumnIndex(COL_MOBILE));
    contactIndexes.add(getEmData().getColumnIndex(COL_PHONE));
    return contactIndexes;
  }

  @Override
  protected List<BeeColumn> getPartitionDataColumns() {
    return getEmData().getColumns();
  }

  @Override
  protected List<Integer> getPartitionInfoIndexes() {
    List<Integer> infoIndexes = new ArrayList<>();
    infoIndexes.add(getEmData().getColumnIndex(ALS_COMPANY_NAME));
    infoIndexes.add(getEmData().getColumnIndex(COL_TAB_NUMBER));
    return infoIndexes;
  }

  @Override
  protected List<Integer> getPartitionNameIndexes() {
    List<Integer> nameIndexes = new ArrayList<>();
    nameIndexes.add(getEmData().getColumnIndex(COL_FIRST_NAME));
    nameIndexes.add(getEmData().getColumnIndex(COL_LAST_NAME));
    return nameIndexes;
  }

  @Override
  protected long getRelationId() {
    return objectId;
  }

  @Override
  protected Filter getWorkScheduleRelationFilter() {
    return Filter.equals(COL_PAYROLL_OBJECT, objectId);
  }

  @Override
  protected void initCalendarInfo(YearMonth ym, CalendarInfo calendarInfo) {
  }

  @Override
  protected boolean isActive(YearMonth ym) {
    if (DataUtils.isEmpty(getObData())) {
      return false;

    } else {
      int statusIndex = getObData().getColumnIndex(COL_LOCATION_STATUS);
      ObjectStatus status = EnumUtils.getEnumByIndex(ObjectStatus.class,
          getObData().getInteger(0, statusIndex));

      return status == ObjectStatus.ACTIVE;
    }
  }

  @Override
  protected Widget renderAppender(Collection<IdPair> partIds, YearMonth ym,
      String selectorStyleName) {

    Flow panel = new Flow();

    Relation relation = Relation.create();
    relation.setViewName(VIEW_EMPLOYEES);
    relation.disableNewRow();
    relation.disableEdit();

    relation.setChoiceColumns(Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME,
        ALS_COMPANY_NAME, ALS_DEPARTMENT_NAME, ALS_POSITION_NAME));
    relation.setSearchableColumns(Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME));

    Filter filter = Filter.and(
        Filter.or(Filter.isNull(COL_DATE_OF_EMPLOYMENT),
            Filter.isLessEqual(COL_DATE_OF_EMPLOYMENT, new DateValue(ym.getLast()))),
        Filter.or(Filter.isNull(COL_DATE_OF_DISMISSAL),
            Filter.isMoreEqual(COL_DATE_OF_DISMISSAL, new DateValue(ym.getDate()))));

    relation.setFilter(filter);

    UnboundSelector selector = UnboundSelector.create(relation,
        Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME));

    selector.addStyleName(selectorStyleName);
    DomUtils.setPlaceholder(selector, Localized.dictionary().newEmployee());

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
        addEmployeeObject(event.getRelatedRow().getId(), objectId, false);
      }
    });

    panel.add(selector);
    return panel;
  }

  @Override
  protected void updateCalendarInfo(YearMonth ym, Partition partition, CalendarInfo calendarInfo) {
    long employeeId = partition.getId();
    calendarInfo.setTcChanges(getTimeCardChanges(employeeId, ym));

    JustDate activeFrom = DataUtils.getDate(getEmData(), partition.getRow(),
        COL_DATE_OF_EMPLOYMENT);
    JustDate activeUntil = DataUtils.getDate(getEmData(), partition.getRow(),
        COL_DATE_OF_DISMISSAL);

    calendarInfo.setInactiveDays(getInactiveDays(ym, activeFrom, activeUntil));

    if (calendarInfo.getInactiveDays().isEmpty()) {
      calendarInfo.setSubTitle(null);

    } else {
      String from = (activeFrom == null)
          ? null : BeeUtils.join(VALUE_SEPARATOR, dateOfEmploymentLabel, activeFrom);
      String until = (activeUntil == null)
          ? null : BeeUtils.join(VALUE_SEPARATOR, dateOfDismissalLabel, activeUntil);

      calendarInfo.setSubTitle(BeeUtils.buildLines(from, until));
    }
  }

  @Override
  void refresh() {
    Set<String> viewNames = new HashSet<>();
    Map<String, Filter> filters = new HashMap<>();

    viewNames.add(VIEW_LOCATIONS);
    filters.put(VIEW_LOCATIONS, Filter.compareId(objectId));

    viewNames.add(VIEW_WORK_SCHEDULE);
    filters.put(VIEW_WORK_SCHEDULE, getWorkScheduleFilter());

    viewNames.add(VIEW_EMPLOYEE_OBJECTS);
    filters.put(VIEW_EMPLOYEE_OBJECTS, Filter.equals(COL_PAYROLL_OBJECT, objectId));

    viewNames.add(VIEW_TIME_CARD_CODES);

    WorkScheduleKind kind = getWorkScheduleKind();
    if (kind != null) {
      filters.put(VIEW_TIME_CARD_CODES, Filter.notNull(kind.getTccColumnName()));
    }
    viewNames.add(VIEW_TIME_RANGES);
    viewNames.add(VIEW_WORK_SCHEDULE_LOCKS);
    filters.put(VIEW_WORK_SCHEDULE_LOCKS, getWorkScheduleFilter());

    Queries.getData(viewNames, filters, CachingPolicy.NONE, new Queries.DataCallback() {
      @Override
      public void onSuccess(Collection<BeeRowSet> result) {
        clearData();

        for (BeeRowSet rowSet : result) {
          switch (rowSet.getViewName()) {
            case VIEW_LOCATIONS:
              setObData(rowSet);
              break;

            case VIEW_WORK_SCHEDULE:
              setWsData(rowSet);
              break;

            case VIEW_EMPLOYEE_OBJECTS:
              setEoData(rowSet);
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

          getEmployees(employees -> {
            if (employees.isEmpty()) {
              setTcData(null);
              render();

            } else {
              Filter tccFilter = Filter.and(Filter.any(COL_EMPLOYEE, employees),
                  getTimeCardChangesFilter());

              Queries.getRowSet(VIEW_TIME_CARD_CHANGES, null, tccFilter,
                  new Queries.RowSetCallback() {
                    @Override
                    public void onSuccess(BeeRowSet tcRowSet) {
                      setTcData(tcRowSet);
                      render();
                    }
                  });
            }
          });
        });
      }
    });
  }

  private void getEmployees(final Consumer<Set<Long>> consumer) {
    final Set<Long> employees = new HashSet<>();

    if (!DataUtils.isEmpty(getWsData())) {
      DataUtils.addNotNullLongs(employees, getWsData(), COL_EMPLOYEE);
      DataUtils.addNotNullLongs(employees, getWsData(), COL_SUBSTITUTE_FOR);
    }
    if (!DataUtils.isEmpty(getEoData())) {
      DataUtils.addNotNullLongs(employees, getEoData(), COL_EMPLOYEE);
      DataUtils.addNotNullLongs(employees, getEoData(), COL_SUBSTITUTE_FOR);
    }

    if (employees.isEmpty()) {
      consumer.accept(employees);

    } else {
      Queries.getRowSet(VIEW_EMPLOYEES, null, Filter.idIn(employees), new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          setEmData(result);

          if (!DataUtils.isEmpty(result)) {
            employees.addAll(result.getRowIds());
          }

          consumer.accept(employees);
        }
      });
    }
  }
}
