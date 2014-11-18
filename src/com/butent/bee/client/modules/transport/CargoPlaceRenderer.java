package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.FlagRenderer;
import com.butent.bee.client.render.ProvidesGridColumnRenderer;
import com.butent.bee.client.widget.DateLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CargoPlaceRenderer extends AbstractCellRenderer {

  public static class Provider implements ProvidesGridColumnRenderer {
    @Override
    public AbstractCellRenderer getRenderer(String columnName,
        List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
        CellSource cellSource) {
      return new CargoPlaceRenderer(dataColumns, columnName);
    }
  }

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "places-";

  final Map<String, Pair<Integer, String>> data = new LinkedHashMap<>();
  private final FlagRenderer flagRenderer;
  private final int postIndex;

  public CargoPlaceRenderer(List<? extends IsColumn> columns, String prefix) {
    super(null);

    data.put(COL_PLACE_DATE, Pair.of(DataUtils.getColumnIndex(prefix + COL_PLACE_DATE, columns),
        Localized.getConstants().date()));
    data.put(COL_PLACE_COMPANY,
        Pair.of(DataUtils.getColumnIndex(prefix + COL_PLACE_COMPANY, columns),
            Localized.getConstants().company()));
    data.put(COL_PLACE_CONTACT,
        Pair.of(DataUtils.getColumnIndex(prefix + COL_PLACE_CONTACT, columns),
            Localized.getConstants().contact()));
    data.put(COL_PLACE_ADDRESS,
        Pair.of(DataUtils.getColumnIndex(prefix + COL_PLACE_ADDRESS, columns),
            Localized.getConstants().address()));
    data.put(COL_PLACE_CITY, Pair.of(DataUtils.getColumnIndex(prefix + "CityName", columns),
        Localized.getConstants().city()));
    data.put(COL_PLACE_COUNTRY,
        Pair.of(DataUtils.getColumnIndex(prefix + "CountryName", columns),
            Localized.getConstants().country()));
    data.put(COL_PLACE_NUMBER,
        Pair.of(DataUtils.getColumnIndex(prefix + COL_PLACE_NUMBER, columns),
            Localized.getConstants().ref()));

    int codeIndex = DataUtils.getColumnIndex(prefix + "CountryCode", columns);

    if (codeIndex != BeeConst.UNDEF) {
      flagRenderer = new FlagRenderer(CellSource.forColumn(columns.get(codeIndex), codeIndex));
    } else {
      flagRenderer = null;
    }
    postIndex = DataUtils.getColumnIndex(prefix + COL_PLACE_POST_INDEX, columns);
  }

  @Override
  public String render(IsRow row) {
    HtmlTable table = new HtmlTable();
    table.addStyleName(STYLE_PREFIX + "table");
    int r = -1;

    for (String item : data.keySet()) {
      Pair<Integer, String> itemInfo = data.get(item);

      if (itemInfo.getA() != BeeConst.UNDEF) {
        String txt = row.getString(itemInfo.getA());

        if (!BeeUtils.isEmpty(txt)) {
          r++;
          table.setHtml(r, 0, itemInfo.getB(), STYLE_PREFIX + "caption");

          if (BeeUtils.same(item, COL_PLACE_DATE)) {
            DateLabel dt = new DateLabel(false);
            dt.setValue(new JustDate(BeeUtils.toLong(txt)));
            table.setWidget(r, 1, dt);

          } else if (BeeUtils.same(item, COL_PLACE_COUNTRY)) {
            if (flagRenderer != null) {
              txt = BeeUtils.joinWords(flagRenderer.render(row), txt);
            }
            table.setHtml(r, 1, txt);

          } else if (BeeUtils.same(item, COL_PLACE_CITY)) {
            if (postIndex != BeeConst.UNDEF) {
              txt = BeeUtils.joinItems(txt, row.getString(postIndex));
            }
            table.setHtml(r, 1, txt);

          } else {
            table.setHtml(r, 1, txt);
          }
        }
      }
    }
    return table.getRowCount() > 0 ? table.toString() : BeeConst.STRING_EMPTY;
  }
}
