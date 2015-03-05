package com.butent.bee.client.timeboard;

import com.google.common.collect.Range;

import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TimeBoardRowLayout {

  public static final class GroupLayout {
    private final Long groupId;

    private final int firstRow;
    private final int lastRow;

    private final boolean hasOverlap;

    private GroupLayout(Long groupId, int firstRow, int lastRow, boolean hasOverlap) {
      this.groupId = groupId;
      this.firstRow = firstRow;
      this.lastRow = lastRow;
      this.hasOverlap = hasOverlap;
    }

    public int getFirstRow() {
      return firstRow;
    }

    public Long getGroupId() {
      return groupId;
    }

    public int getLastRow() {
      return lastRow;
    }

    public int getSize() {
      return getLastRow() - getFirstRow() + 1;
    }

    public boolean hasOverlap() {
      return hasOverlap;
    }
  }

  public static final class RowData {
    private final Long groupId;
    private final List<HasDateRange> rowItems = new ArrayList<>();

    private RowData(Long groupId, HasDateRange item) {
      this.groupId = groupId;
      add(item);
    }

    public boolean contains(JustDate date) {
      if (date != null) {
        for (HasDateRange item : rowItems) {
          if (item.getRange().contains(date)) {
            return true;
          }
        }
      }
      return false;
    }

    public List<HasDateRange> getRowItems() {
      return rowItems;
    }

    Long getGroupId() {
      return groupId;
    }

    boolean hasGroup(Long id) {
      return Objects.equals(id, groupId);
    }

    private void add(HasDateRange item) {
      if (item != null) {
        rowItems.add(item);
      }
    }
  }

  public static int countRows(List<TimeBoardRowLayout> layout, int minSize) {
    int result = 0;
    for (TimeBoardRowLayout crl : layout) {
      result += crl.getSize(minSize);
    }
    return result;
  }

  private static Set<Range<JustDate>> clash(Collection<? extends HasDateRange> items,
      Range<JustDate> range, Range<JustDate> activeRange) {

    Set<Range<JustDate>> overlap = intersection(items, range);
    if (overlap.isEmpty() || activeRange == null) {
      return overlap;
    } else {
      return clash(overlap, activeRange);
    }
  }

  private static Set<Range<JustDate>> clash(Collection<Range<JustDate>> ranges,
      Range<JustDate> range) {

    Set<Range<JustDate>> result = new HashSet<>();
    if (ranges == null || range == null) {
      return result;
    }

    for (Range<JustDate> item : ranges) {
      if (item != null && item.isConnected(range)) {
        Range<JustDate> section = item.intersection(range);
        if (!section.isEmpty()) {
          result.add(section);
        }
      }
    }
    return result;
  }

  private static Set<Range<JustDate>> intersection(Collection<? extends HasDateRange> items,
      Range<JustDate> range) {

    Set<Range<JustDate>> result = new HashSet<>();
    if (items == null || range == null) {
      return result;
    }

    for (HasDateRange item : items) {
      if (item != null && item.getRange() != null && item.getRange().isConnected(range)) {
        Range<JustDate> section = item.getRange().intersection(range);
        if (!section.isEmpty()) {
          result.add(section);
        }
      }
    }
    return result;
  }

  private final int dataIndex;

  private final Set<HasDateRange> inactivity = new HashSet<>();

  private final List<RowData> rows = new ArrayList<>();

  private final Set<Range<JustDate>> overlap = new HashSet<>();

  public TimeBoardRowLayout(int dataIndex) {
    this.dataIndex = dataIndex;
  }

  public void addInactivity(Collection<? extends HasDateRange> items, Range<JustDate> activeRange) {
    if (!items.isEmpty()) {
      inactivity.addAll(items);

      if (!rows.isEmpty()) {
        for (HasDateRange item : items) {
          for (RowData rowData : rows) {
            Set<Range<JustDate>> over = clash(rowData.getRowItems(), item.getRange(), activeRange);
            if (!over.isEmpty()) {
              overlap.addAll(over);
            }
          }
        }
      }
    }
  }

  public void addItem(Long itemGroupId, HasDateRange item, Range<JustDate> activeRange,
      Blender blender) {

    boolean added = false;

    for (RowData rowData : rows) {
      Set<Range<JustDate>> over = clash(rowData.getRowItems(), item.getRange(), activeRange);

      if (over.isEmpty()) {
        if (!added && rowData.hasGroup(itemGroupId)) {
          rowData.add(item);
          added = true;
        }

      } else if (blender == null) {
        overlap.addAll(over);

      } else {
        List<HasDateRange> incompatible = new ArrayList<>();
        for (HasDateRange rowItem : rowData.getRowItems()) {
          if (!blender.willItBlend(item, rowItem)) {
            incompatible.add(rowItem);
          }
        }

        if (!incompatible.isEmpty()) {
          overlap.addAll(clash(incompatible, item.getRange(), activeRange));
        }
      }
    }

    if (!added) {
      rows.add(new RowData(itemGroupId, item));
    }
  }

  public void addItems(Long itemGroupId, Collection<? extends HasDateRange> items,
      Range<JustDate> activeRange) {
    addItems(itemGroupId, items, activeRange, null);
  }

  public void addItems(Long itemGroupId, Collection<? extends HasDateRange> items,
      Range<JustDate> activeRange, Blender blender) {

    for (HasDateRange item : items) {
      addItem(itemGroupId, item, activeRange, blender);
    }
  }

  public int getDataIndex() {
    return dataIndex;
  }

  public List<GroupLayout> getGroups() {
    List<GroupLayout> result = new ArrayList<>();
    if (rows.isEmpty()) {
      return result;
    }

    Long lastGroup = null;
    int firstRow = 0;
    boolean over = false;

    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      Long currentGroup = rows.get(rowIndex).getGroupId();

      if (rowIndex == 0) {
        lastGroup = currentGroup;

      } else if (!Objects.equals(lastGroup, currentGroup)) {
        result.add(new GroupLayout(lastGroup, firstRow, rowIndex - 1, over));

        lastGroup = currentGroup;
        firstRow = rowIndex;
        over = false;
      }

      if (!over && hasOverlap()) {
        over = overlaps(rows.get(rowIndex).getRowItems());
      }

      if (rowIndex == rows.size() - 1) {
        result.add(new GroupLayout(lastGroup, firstRow, rowIndex, over));
      }
    }

    return result;
  }

  public Set<HasDateRange> getInactivity() {
    return inactivity;
  }

  public Set<Range<JustDate>> getOverlap(Range<JustDate> activeRange) {
    return clash(overlap, activeRange);
  }

  public List<RowData> getRows() {
    return rows;
  }

  public int getSize(int minSize) {
    return Math.max(rows.size(), minSize);
  }

  public boolean hasOverlap() {
    return !overlap.isEmpty();
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  private boolean overlaps(Collection<? extends HasDateRange> items) {
    if (items == null || !hasOverlap()) {
      return false;
    }

    for (Range<JustDate> range : overlap) {
      if (BeeUtils.intersects(items, range)) {
        return true;
      }
    }
    return false;
  }
}
