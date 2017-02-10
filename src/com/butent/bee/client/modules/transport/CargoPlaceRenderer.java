package com.butent.bee.client.modules.transport;

import com.google.gwt.json.client.JSONObject;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FlagRenderer;
import com.butent.bee.client.utils.JsonUtils;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.html.builder.elements.Br;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CargoPlaceRenderer extends AbstractCellRenderer {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "places-";
  private static final String PFX_VIEW_NAME = "Cargo";
  private static final String PFX_FLG = "flg_";
  private static final String OPT_VIEW = "viewName";
  private static final String OPT_RENDER = "render";

  final Map<String, String> renderData = new LinkedHashMap<>();
  private final FlagRenderer flagRenderer;
  private final DataInfo viewData;
  private final List<String> renderColumns;

  public CargoPlaceRenderer(CellSource source, List<String> renderColumns, String options) {
    super(source);

    String viewName = PFX_VIEW_NAME + source.getName();
    JSONObject renderDescription = null;

    if (JsonUtils.isJson(options)) {
      JSONObject obj = JsonUtils.parseObject(options);
      if (obj != null) {
        viewName = BeeUtils.notEmpty(JsonUtils.getString(obj, OPT_VIEW), viewName);
        renderDescription = obj.get(OPT_RENDER) != null ? obj.get(OPT_RENDER).isObject() : null;
      }
    }

    if (renderDescription != null) {
      for (String key : renderDescription.keySet()) {
        renderData.put(key, JsonUtils.getString(renderDescription, key));
      }
    }
    this.viewData = Data.getDataInfo(viewName);
    this.renderColumns = !BeeUtils.isEmpty(renderColumns) ? renderColumns : null;
    flagRenderer = new FlagRenderer(CellSource.forProperty(COL_PLACE_COUNTRY, null,
        ValueType.TEXT));
  }

  @Override
  public String render(IsRow row) {
    HtmlTable table = new HtmlTable();
    table.addStyleName(STYLE_PREFIX + "table");
    int r = -1;

    if (viewData == null) {
      return BeeConst.STRING_EMPTY;
    }

    String handlingData = getString(row);

    if (BeeUtils.isEmpty(handlingData)) {
      return BeeConst.STRING_EMPTY;
    }

    for (SimpleRowSet.SimpleRow handle : SimpleRowSet.restore(handlingData)) {
      for (String column : BeeUtils.nvl(renderColumns, viewData.getColumnNames(false))) {

       BeeColumn beeColumn = viewData.getColumn(column);

        if (beeColumn == null) {
          continue;
        }
        String txt = renderColumn(beeColumn, handle);

        if (BeeUtils.isEmpty(txt)) {
          continue;
        }
        r++;
        table.setHtml(r, 0, Localized.getLabel(beeColumn), STYLE_PREFIX + "caption");
        table.setHtml(r, 1, txt);
      }
      r++;
      table.setHtml(r, 0, new Br().build());
    }
    return table.getRowCount() > 0 ? table.toString() : BeeConst.STRING_EMPTY;
  }

  private String renderColumn(BeeColumn column, SimpleRowSet.SimpleRow row) {
    return  renderColumn(column, row, false);
  }

  private String renderColumn(BeeColumn column, SimpleRowSet.SimpleRow row, boolean renderFlag) {
    String result = "";

    if (column == null) {
      return  result;
    }

    if (renderData.containsKey(column.getId())) {
      for (String rendCol : NameUtils.toList(renderData.get(column.getId()))) {
        BeeColumn beeColumn = viewData.getColumn(BeeUtils.removePrefix(rendCol, PFX_FLG));
        result = BeeUtils.joinWords(result, renderColumn(beeColumn, row,
            BeeUtils.isPrefix(rendCol, PFX_FLG)));
      }
      return result;
    }

    if (!row.hasColumn(column.getId())) {
      return result;
    }
    result = row.getValue(column.getId());

    if (BeeUtils.isEmpty(result)) {
      return result;
    }

    if (renderFlag) {
      BeeRow fr = new BeeRow(0, 0);
      fr.setProperty(COL_PLACE_COUNTRY, result);
      return flagRenderer.render(fr);
    }

    switch (column.getType()) {
      case BOOLEAN:
        result = BeeUtils.toBoolean(result)
            ? Localized.dictionary().yes()
            : Localized.dictionary().no();
        break;
      case DATE:
      case DATE_TIME:
        DateTimeLabel dt = new DateTimeLabel(false);
        dt.setValue(new DateTime(BeeUtils.toLong(result)));
        result = dt.getHtml();
        break;
      default:
        return result;
    }
    return result;
  }
}
