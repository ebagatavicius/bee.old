package com.butent.bee.client.render;

import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class MailAddressRenderer extends AbstractCellRenderer {

  private final List<Integer> labelIndexes = new ArrayList<>();
  private final int addressIndex;

  public MailAddressRenderer(List<? extends IsColumn> dataColumns, List<String> renderColumns) {
    super(null);

    int size = renderColumns.size();

    if (size > 0) {
      for (int i = 0; i < size - 1; i++) {
        int index = DataUtils.getColumnIndex(renderColumns.get(i), dataColumns);
        if (!BeeConst.isUndef(index)) {
          labelIndexes.add(index);
        }
      }

      this.addressIndex = DataUtils.getColumnIndex(renderColumns.get(size - 1), dataColumns);

    } else {
      this.addressIndex = BeeConst.UNDEF;
    }
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    String labels;
    if (labelIndexes.isEmpty()) {
      labels = null;
    } else if (labelIndexes.size() > 1) {
      labels = DataUtils.join(row, labelIndexes, Format.getDateRenderer(),
          Format.getDateTimeRenderer());
    } else {
      labels = row.getString(labelIndexes.get(0));
    }

    String address = BeeConst.isUndef(addressIndex) ? null : row.getString(addressIndex);
    if (BeeUtils.isEmpty(address)) {
      return labels;
    } else {
      return BeeUtils.joinWords(labels, BeeConst.STRING_LT + address.trim() + BeeConst.STRING_GT);
    }
  }
}
