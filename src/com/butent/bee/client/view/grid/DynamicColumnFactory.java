package com.butent.bee.client.view.grid;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import com.butent.bee.client.event.logical.RenderingEvent;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.RightsColumn;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.CustomProperties;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DynamicColumnFactory {

  private static final BeeLogger logger = LogUtils.getLogger(DynamicColumnFactory.class);

  private static final String ROLE_COLUMN_PREFIX = "#Role_";

  private static final Table<String, String, DynamicColumnEnumerator> enumerators =
      HashBasedTable.create();

  private static final Table<String, String, ColumnDescription> descriptionCache =
      HashBasedTable.create();

  public static void checkDynamicColumns(GridView gridView, RenderingEvent event, String dynGroup) {
    Assert.notNull(gridView);
    Assert.notNull(event);
    Assert.notEmpty(dynGroup);

    Collection<DynamicColumnIdentity> dynamicColumns = getDynamicColumns(gridView, dynGroup);

    List<ColumnDescription> columnDescriptions = Lists.newArrayList();

    if (!BeeUtils.isEmpty(dynamicColumns)) {
      ColumnDescription template = null;

      for (DynamicColumnIdentity dynamicColumn : dynamicColumns) {
        String id = dynamicColumn.getId();
        ColumnDescription columnDescription = descriptionCache.get(gridView.getGridName(), id);

        if (columnDescription == null) {
          if (template == null) {
            template = getTemplate(gridView, dynGroup);
            if (template == null) {
              break;
            }
          }

          columnDescription = generateColumnDescription(template, dynamicColumn);
          descriptionCache.put(gridView.getGridName(), id, columnDescription);
        }

        columnDescriptions.add(columnDescription);
      }
    }

    if (maybeUpdateGrid(gridView, dynGroup, columnDescriptions)) {
      event.setDataChanged();
    }
  }

  public static void checkRightsColumns(GridPresenter presenter, GridView gridView,
      RenderingEvent event) {

    boolean has = hasRightsColumns(gridView);
    if (has == !presenter.getRightsStates().isEmpty()) {
      if (has) {
        event.setDataChanged();
      }
      return;
    }

    if (has) {
      List<ColumnInfo> predefinedColumns = gridView.getGrid().getPredefinedColumns();
      List<String> rightsColumns = new ArrayList<>();

      for (ColumnInfo columnInfo : predefinedColumns) {
        if (columnInfo.isRightsColumn()) {
          rightsColumns.add(columnInfo.getColumnId());
        }
      }

      for (String columnId : rightsColumns) {
        gridView.getGrid().removeColumn(columnId);
      }

    } else if (BeeUtils.isEmpty(presenter.getRoles())) {
      logger.severe("roles not available");

    } else {
      BiMap<Long, String> roles = HashBiMap.create(presenter.getRoles());

      List<String> roleNames = new ArrayList<>(roles.values());
      if (roleNames.size() > 1) {
        Collections.sort(roleNames, Collator.DEFAULT);
      }

      String viewName = gridView.getViewName();

      ColumnInfo columnInfo = createRoleColumnInfo(viewName, 0L,
          Localized.getConstants().rightsDefault());
      gridView.getGrid().addColumn(columnInfo);

      for (String roleName : roleNames) {
        Long roleId = roles.inverse().get(roleName);

        if (DataUtils.isId(roleId)) {
          columnInfo = createRoleColumnInfo(viewName, roleId, roleName);
          gridView.getGrid().addColumn(columnInfo);
        }
      }
    }

    event.setDataChanged();
  }

  public static void registerColumnDescription(String gridName, String columnId,
      ColumnDescription columnDescription) {
    Assert.notEmpty(gridName);
    Assert.notEmpty(columnId);
    Assert.notNull(columnDescription);

    descriptionCache.put(gridName, columnId, columnDescription);
  }

  public static void registerEnumerator(String gridName, String dynGroup,
      DynamicColumnEnumerator enumerator) {
    Assert.notEmpty(gridName);
    Assert.notEmpty(dynGroup);
    Assert.notNull(enumerator);

    enumerators.put(gridName, dynGroup, enumerator);
  }

  private static ColumnInfo createRoleColumnInfo(String viewName, long roleId, String roleName) {
    String columnId = ROLE_COLUMN_PREFIX + roleId;

    RightsColumn column = new RightsColumn(viewName, roleId);
    ColumnHeader header = new ColumnHeader(columnId, roleName, roleName);

    ColumnInfo columnInfo = new ColumnInfo(columnId, roleName, null, column, header);

    columnInfo.setCellResizable(false);
    columnInfo.setExportable(false);

    return columnInfo;
  }

  private static ColumnDescription generateColumnDescription(ColumnDescription template,
      DynamicColumnIdentity dynamicColumn) {

    ColumnDescription columnDescription = template.copy();
    columnDescription.setId(dynamicColumn.getId());

    if (!BeeUtils.isEmpty(dynamicColumn.getCaption())) {
      columnDescription.setCaption(dynamicColumn.getCaption());
    }
    if (!BeeUtils.isEmpty(dynamicColumn.getLabel())) {
      columnDescription.setLabel(dynamicColumn.getLabel());
    }

    if (!BeeUtils.isEmpty(dynamicColumn.getSource())) {
      if (BeeUtils.isEmpty(template.getSource())) {
        columnDescription.replaceSource(template.getId(), dynamicColumn.getSource());
        columnDescription.setSource(dynamicColumn.getSource());
      } else {
        columnDescription.replaceSource(template.getSource(), dynamicColumn.getSource());
      }

    } else if (!BeeUtils.isEmpty(dynamicColumn.getProperty())) {
      if (BeeUtils.isEmpty(template.getProperty())) {
        columnDescription.replaceSource(template.getId(), dynamicColumn.getProperty());
        columnDescription.setProperty(dynamicColumn.getProperty());
      } else {
        columnDescription.replaceSource(template.getProperty(), dynamicColumn.getProperty());
      }

    } else {
      columnDescription.replaceSource(template.getId(), dynamicColumn.getId());
    }

    return columnDescription;
  }

  private static Collection<DynamicColumnIdentity> getDynamicColumns(GridView gridView,
      String dynGroup) {

    Collection<DynamicColumnIdentity> result;

    DynamicColumnEnumerator enumerator = enumerators.get(gridView.getGridName(), dynGroup);
    if (enumerator != null) {
      result = enumerator.getDynamicColumns(gridView, dynGroup);

    } else {
      result = (gridView.getGridInterceptor() == null) ? null
          : gridView.getGridInterceptor().getDynamicColumns(gridView, dynGroup);

      if (result == null) {
        ColumnDescription template = getTemplate(gridView, dynGroup);
        List<? extends IsRow> dataRows = gridView.getRowData();

        if (template != null && !BeeUtils.isEmpty(dataRows)) {
          if (template.getColType() == ColType.PROPERTY) {
            String prefix = BeeUtils.notEmpty(template.getProperty(), dynGroup);
            result = getDynamicPropertyColumns(template, prefix, dataRows);
          } else {
            String prefix = BeeUtils.notEmpty(template.getSource(), dynGroup);
            result = getDynamicSourceColumns(template, prefix, gridView.getDataColumns(), dataRows);
          }
        }
      }
    }

    return result;
  }

  private static List<DynamicColumnIdentity> getDynamicPropertyColumns(ColumnDescription template,
      String prefix, List<? extends IsRow> rows) {

    Assert.notNull(template);
    Assert.notEmpty(prefix);
    Assert.notNull(rows);

    Set<String> names = Sets.newHashSet();

    for (IsRow row : rows) {
      CustomProperties properties = row.getProperties();
      if (!BeeUtils.isEmpty(properties)) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          if (BeeUtils.isPrefix(entry.getKey(), prefix) && !BeeUtils.isEmpty(entry.getValue())) {
            names.add(entry.getKey().trim());
          }
        }
      }
    }

    List<DynamicColumnIdentity> result = Lists.newArrayList();

    for (String name : names) {
      String suffix = BeeUtils.removePrefix(name, prefix);
      String caption = BeeUtils.joinWords(Localized.maybeTranslate(template.getCaption()), suffix);

      DynamicColumnIdentity dynamicColumn = new DynamicColumnIdentity(name, caption);

      if (!BeeUtils.isEmpty(template.getLabel())) {
        dynamicColumn.setLabel(BeeUtils.joinWords(Localized.maybeTranslate(template.getLabel()),
            suffix));
      }

      dynamicColumn.setProperty(name);
      result.add(dynamicColumn);
    }

    if (result.size() > 1) {
      Collections.sort(result);
    }

    return result;
  }

  private static List<DynamicColumnIdentity> getDynamicSourceColumns(ColumnDescription template,
      String prefix, List<BeeColumn> dataColumns, List<? extends IsRow> rows) {

    Assert.notNull(template);
    Assert.notEmpty(prefix);
    Assert.notEmpty(dataColumns);
    Assert.notNull(rows);

    Set<Integer> indexes = Sets.newHashSet();
    for (int i = 0; i < dataColumns.size(); i++) {
      if (BeeUtils.isPrefix(dataColumns.get(i).getId(), prefix)) {
        indexes.add(i);
      }
    }

    List<DynamicColumnIdentity> result = Lists.newArrayList();

    for (int index : indexes) {
      boolean found = false;

      for (IsRow row : rows) {
        if (!row.isNull(index)) {
          found = true;
          break;
        }
      }

      if (found) {
        BeeColumn dataColumn = dataColumns.get(index);
        String id = dataColumn.getId();

        String suffix = BeeUtils.removePrefix(id, prefix);
        String caption = BeeUtils.isEmpty(template.getCaption()) ? Localized.getLabel(dataColumn)
            : BeeUtils.joinWords(Localized.maybeTranslate(template.getCaption()), suffix);

        DynamicColumnIdentity dynamicColumn = new DynamicColumnIdentity(id, caption);

        if (!BeeUtils.isEmpty(template.getLabel())) {
          dynamicColumn.setLabel(BeeUtils.joinWords(Localized.maybeTranslate(template.getLabel()),
              suffix));
        }

        dynamicColumn.setSource(id);
        result.add(dynamicColumn);
      }
    }

    if (result.size() > 1) {
      Collections.sort(result);
    }

    return result;
  }

  private static int getInsertionIndex(GridView gridView, List<ColumnInfo> predefinedColumns,
      List<Integer> visibleColumns, String dynGroup, String columnId) {

    int result = BeeConst.UNDEF;

    Multimap<String, Integer> indexesByGroup = ArrayListMultimap.create();
    int rightsIndex = BeeConst.UNDEF;

    for (int i = 0; i < visibleColumns.size(); i++) {
      ColumnInfo columnInfo = predefinedColumns.get(visibleColumns.get(i));

      if (columnInfo.hasDynGroup(dynGroup)) {
        if (BeeUtils.isLess(columnId, columnInfo.getColumnId())) {
          return i;
        } else {
          result = i + 1;
        }

      } else if (columnInfo.getDynGroup() != null) {
        indexesByGroup.put(columnInfo.getDynGroup(), i);

      } else if (BeeConst.isUndef(rightsIndex) && columnInfo.isRightsColumn()) {
        rightsIndex = i;
      }
    }

    if (!BeeConst.isUndef(result)) {
      return result;
    }
    if (!BeeConst.isUndef(rightsIndex)) {
      result = rightsIndex;
    }
    if (indexesByGroup.isEmpty()) {
      return result;
    }

    List<String> columnGroups = gridView.getDynamicColumnGroups();

    int groupIndex = columnGroups.indexOf(dynGroup);
    if (groupIndex < 0 || groupIndex >= columnGroups.size() - 1) {
      return result;
    }

    for (int i = groupIndex + 1; i < columnGroups.size(); i++) {
      if (indexesByGroup.containsKey(columnGroups.get(i))) {
        return BeeUtils.min(indexesByGroup.get(columnGroups.get(i)));
      }
    }

    return result;
  }

  private static ColumnDescription getTemplate(GridView gridView, String dynGroup) {
    Assert.notNull(gridView);
    Assert.notEmpty(dynGroup);

    ColumnDescription template = gridView.getGridDescription().getColumn(dynGroup);
    if (template == null) {
      logger.severe("dynamic group not found:", gridView.getGridName(), dynGroup);
    }
    return template;
  }

  private static boolean hasRightsColumns(GridView gridView) {
    List<ColumnInfo> predefinedColumns = gridView.getGrid().getPredefinedColumns();

    for (ColumnInfo columnInfo : predefinedColumns) {
      if (columnInfo.isRightsColumn()) {
        return true;
      }
    }
    return false;
  }

  private static boolean maybeUpdateGrid(GridView gridView, String dynGroup,
      Collection<ColumnDescription> columnDescriptions) {

    Assert.notNull(gridView);
    Assert.notEmpty(dynGroup);

    boolean changed = false;

    Set<Integer> hide = Sets.newHashSet();
    Set<Integer> show = Sets.newHashSet();

    List<ColumnDescription> add = Lists.newArrayList();

    List<ColumnInfo> predefinedColumns = gridView.getGrid().getPredefinedColumns();
    List<Integer> visibleColumns = gridView.getGrid().getVisibleColumns();

    for (int i = 0; i < predefinedColumns.size(); i++) {
      ColumnInfo columnInfo = predefinedColumns.get(i);
      if (columnInfo.hasDynGroup(dynGroup) && visibleColumns.contains(i)
          && !GridUtils.containsColumn(columnDescriptions, columnInfo.getColumnId())) {
        hide.add(i);
      }
    }

    if (!BeeUtils.isEmpty(columnDescriptions)) {
      for (ColumnDescription columnDescription : columnDescriptions) {
        int index = GridUtils.getColumnIndex(predefinedColumns, columnDescription.getId());

        if (BeeConst.isUndef(index)) {
          add.add(columnDescription);
        } else if (!visibleColumns.contains(index)) {
          show.add(index);
        }
      }
    }

    if (!hide.isEmpty()) {
      visibleColumns.removeAll(hide);
      changed = true;
    }

    if (!show.isEmpty()) {
      for (int predefIndex : show) {
        int index = getInsertionIndex(gridView, predefinedColumns, visibleColumns, dynGroup,
            predefinedColumns.get(predefIndex).getColumnId());
        BeeUtils.addQuietly(visibleColumns, index, predefIndex);
      }

      changed = true;
    }

    if (!add.isEmpty()) {
      for (ColumnDescription columnDescription : add) {
        int index = getInsertionIndex(gridView, predefinedColumns, visibleColumns, dynGroup,
            columnDescription.getId());
        if (gridView.addColumn(columnDescription, dynGroup, index)) {
          changed = true;
        }
      }
    }

    return changed;
  }

  private DynamicColumnFactory() {
  }
}
