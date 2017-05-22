package com.butent.bee.client.rights;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.ModuleAndSub;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class RightsTable extends HtmlTable {

  public interface ModuleSelectionHandler {
    void onModuleSelect(ModuleAndSub moduleAndSub);
  }

  public interface RightObjectSelectionHandler {
    void onRightObjectSelect(int col, String objectName);
  }

  public static final int MODULE_COL = 0;

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "Rights-";
  public static final String STYLE_SUFFIX_CELL = "-cell";
  public static final String STYLE_MSO = STYLE_PREFIX + "mso";
  public static final String STYLE_MSO_CELL = STYLE_MSO + STYLE_SUFFIX_CELL;

  private static final int DEFAULT_VALUE_START_COLUMN = 1;
  private static final int DEFAULT_VALUE_START_ROW = 2;

  private static final String STYLE_TABLE = STYLE_PREFIX + "table";
  private static final String STYLE_HOVER = STYLE_PREFIX + "hover";
  private static final String STYLE_MODULE = STYLE_PREFIX + "module";
  private static final String STYLE_SUB_MODULE = STYLE_PREFIX + "sub-module";
  private static final String STYLE_MODULE_CELL = STYLE_MODULE + STYLE_SUFFIX_CELL;

  private static final String STYLE_MODULE_SELECTED = STYLE_MODULE + "-selected";
  private static final String STYLE_MSO_COL_PREFIX = STYLE_MSO + "-col-";
  private static final String STYLE_MSO_BRANCH = STYLE_MSO + "-branch";
  private static final String STYLE_MSO_LEAF = STYLE_MSO + "-leaf";
  private static final String STYLE_MSO_LEAF_CELL = STYLE_MSO_LEAF + STYLE_SUFFIX_CELL;

  private static final String STYLE_MSO_BRANCH_CELL = STYLE_MSO_BRANCH + STYLE_SUFFIX_CELL;
  private static final String STYLE_MSO_SELECTED = STYLE_MSO + "-selected";

  private static final String DATA_KEY_MODULE = "rights-module";
  private static final String DATA_TYPE_MODULE = "mod";
  private static final String DATA_KEY_TYPE = "rights-type";
  private static final String DATA_TYPE_OBJECT_LABEL = "ol";
  private static final String DATA_KEY_OBJECT = "rights-object";

  private final BeeLogger logger = LogUtils.getLogger(RightsTable.class);

  private int hoverColumn = BeeConst.UNDEF;
  private ModuleSelectionHandler moduleSelectionHandler;
  private RightObjectSelectionHandler rightObjectSelectionHandler;

  public RightsTable() {
    super(STYLE_TABLE);
    addMouseMoveHandler(event -> {
      Element target = EventUtils.getTargetElement(event.getNativeEvent().getEventTarget());

      for (Element el = target; el != null; el = el.getParentElement()) {
        if (TableCellElement.is(el)) {
          int col = ((TableCellElement) el.cast()).getCellIndex();

          if (getHoverColumn() != col) {
            onColumnHover(col);
          }

        } else if (getId().equals(el.getId())) {
          break;
        }
      }
    });
    addMouseOutHandler(event -> onColumnHover(BeeConst.UNDEF));
  }

  public void addModuleWidget(int row, ModuleAndSub module) {
    setWidget(row, MODULE_COL, createModuleWidget(module), STYLE_MODULE_CELL);
  }

  public void addRightObjectWidget(int row, int col, RightsObject object) {
    setWidget(row, col, createObjectWidget(col, object), STYLE_MSO_CELL);
    getCellFormatter().addStyleName(row, col,
      isLeaf(col) ? STYLE_MSO_LEAF_CELL : STYLE_MSO_BRANCH_CELL);
  }

  public void addCellStyleName(Widget widget, String styleName) {
    TableCellElement cell = DomUtils.getParentCell(widget, false);

    if (cell == null) {
      logger.severe("parent cell not found");
    } else {
      cell.addClassName(styleName);
    }
  }

  public ModuleSelectionHandler getModuleSelectionHandler() {
    return moduleSelectionHandler;
  }

  public RightObjectSelectionHandler getRightObjectSelectionHandler() {
    return rightObjectSelectionHandler;
  }

  public int getValueStartCol() {
    return DEFAULT_VALUE_START_COLUMN;
  }

  public int getValueStartRow() {
    return DEFAULT_VALUE_START_ROW;
  }

  public boolean isLeaf(int col) {
    return col == getValueStartCol() - 2;
  }

  public void setModuleSelectionHandler(ModuleSelectionHandler handler) {
    this.moduleSelectionHandler = handler;
  }

  public void setRightObjectSelectionHandler(RightObjectSelectionHandler handler) {
    this.rightObjectSelectionHandler = handler;
  }

  public void setSelectedModule(ModuleAndSub ms) {
    Widget widget = getWidget(getValueStartRow(), MODULE_COL);

    if (widget != null) {
      addCellStyleName(widget, STYLE_MODULE_SELECTED);
      if (getModuleSelectionHandler() != null) {
        getModuleSelectionHandler().onModuleSelect(ms);
      }
    }

  }

  private boolean cellHasStyleName(Widget widget, String styleName) {
    TableCellElement cell = DomUtils.getParentCell(widget, false);

    if (cell == null) {
      logger.severe("parent cell not found");
      return false;
    } else {
      return cell.hasClassName(styleName);
    }
  }

  private String getObjectName(Element elem) {
    String objectName = DomUtils.getDataProperty(elem, DATA_KEY_OBJECT);

    if (BeeUtils.isEmpty(objectName)) {
      logger.severe("element", elem.getId(), "has no object name");
    }
    return objectName;
  }

  private ModuleAndSub getModule(Widget widget) {
    String value = DomUtils.getDataProperty(widget.getElement(), DATA_KEY_MODULE);
    if (Module.NEVER_MIND.equals(value)) {
      return null;
    } else {
      ModuleAndSub ms = ModuleAndSub.parse(value);
      if (ms == null) {
        logger.severe("Widget", DomUtils.getId(widget), "has no module");
      }
      return ms;
    }
  }

  private static void markObjectLabel(Widget widget, RightsObject object) {
    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_TYPE, DATA_TYPE_OBJECT_LABEL);
    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_OBJECT, object.getName());
  }

  private static void setDataType(Widget widget, String type) {
    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_TYPE, type);
  }

  private void clearTable(int startCol) {
    int rc = getRowCount();
    int maxRow = getValueStartRow();

    for (int row = getValueStartRow(); row < rc; row++) {
      int cc = getCellCount(row);

      for (int col = 0; col < cc; col++) {
        Widget widget = getWidget(row, col);

        if (widget != null) {
          if (col < startCol) {
            maxRow = Math.max(maxRow, row);
          } else {
            getCellFormatter().setStyleName(row, col, BeeConst.STRING_EMPTY);
            remove(widget);
          }
        }
      }
    }

    if (maxRow < rc - 1) {
      for (int i = maxRow + 1; i < rc; i++) {
        removeRow(maxRow + 1);
      }
    }
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
      widget.addClickHandler(event -> {
        if (event.getSource() instanceof Widget) {
          Widget source = (Widget) event.getSource();
          String objectName = getObjectName(source.getElement());

          if (!BeeUtils.isEmpty(objectName) && !cellHasStyleName(source, STYLE_MSO_SELECTED)) {
            TableCellElement cell = getSelectedObjectCell(col);
            if (cell != null) {
              cell.removeClassName(STYLE_MSO_SELECTED);
            }

            addCellStyleName(source, STYLE_MSO_SELECTED);

            clearTable(col + 1);
            if (getRightObjectSelectionHandler() != null) {
              getRightObjectSelectionHandler().onRightObjectSelect(col, objectName);
            }
          }
        }
      });
    }
    return widget;
  }

  private Widget createModuleWidget(ModuleAndSub moduleAndSub) {
    String caption;

    if (moduleAndSub == null) {
      caption = Module.NEVER_MIND;
    } else if (moduleAndSub.hasSubModule()) {
      caption = moduleAndSub.getSubModule().getCaption();
    } else {
      caption = moduleAndSub.getModule().getCaption();
    }

    Label widget = new Label(caption);

    String name;
    if (moduleAndSub == null) {
      widget.addStyleName(STYLE_MODULE);
      name = Module.NEVER_MIND;
    } else {
      widget.addStyleName(moduleAndSub.hasSubModule() ? STYLE_SUB_MODULE : STYLE_MODULE);

      name = moduleAndSub.getName();
      widget.setTitle(name);
    }

    DomUtils.setDataProperty(widget.getElement(), DATA_KEY_MODULE, name);
    setDataType(widget, DATA_TYPE_MODULE);

    widget.addClickHandler(event -> {
      if (event.getSource() instanceof Widget) {
        Widget source = (Widget) event.getSource();
        ModuleAndSub ms = getModule(source);

        if (!cellHasStyleName(source, STYLE_MODULE_SELECTED)) {
          TableCellElement cell = getSelectedModuleCell();
          if (cell != null) {
            cell.removeClassName(STYLE_MODULE_SELECTED);
          }

          addCellStyleName(source, STYLE_MODULE_SELECTED);

          clearTable(MODULE_COL + 1);
          if (moduleSelectionHandler != null) {
            moduleSelectionHandler.onModuleSelect(ms);
          }
        }
      }
    });

    return widget;
  }


  private int getHoverColumn() {
    return hoverColumn;
  }

  private TableCellElement getSelectedObjectCell(int col) {
    for (int row = getValueStartRow(); row < getRowCount(); row++) {
      if (col >= getCellCount(row)) {
        break;
      }

      TableCellElement cell = getCellFormatter().getElement(row, col);
      if (cell.hasClassName(STYLE_MSO_SELECTED)) {
        return cell;
      }
    }
    return null;
  }

  private TableCellElement getSelectedModuleCell() {
    for (int row = getValueStartRow(); row < getRowCount(); row++) {
      TableCellElement cell = getCellFormatter().getElement(row, MODULE_COL);
      if (cell.hasClassName(STYLE_MODULE_SELECTED)) {
        return cell;
      }
    }

    return null;
  }

  private void onColumnHover(int col) {
    if (getHoverColumn() >= getValueStartCol()) {
      List<TableCellElement> cells = getColumnCells(getHoverColumn());
      for (TableCellElement cell : cells) {
        cell.removeClassName(STYLE_HOVER);
      }
    }

    setHoverColumn(col);

    if (col >= getValueStartCol()) {
      List<TableCellElement> cells = getColumnCells(col);
      for (TableCellElement cell : cells) {
        cell.addClassName(STYLE_HOVER);
      }
    }
  }

  private void setHoverColumn(int hoverColumn) {
    this.hoverColumn = hoverColumn;
  }
}
