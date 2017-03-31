package com.butent.bee.client.modules.transport;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.shared.data.filter.Filter;

import java.util.stream.Stream;

public class CargoExpensesGrid extends ParentRowRefreshGrid {
  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    Stream.of(COL_CARGO_INCOME, COL_CARGO_TRIP).filter(source::equals).findAny()
        .ifPresent(s -> ((DataSelector) editor).addSelectorHandler(event -> {
          if (event.isOpened()) {
            if (parentExists()) {
              getGridView().ensureRelId(relId ->
                  event.getSelector().setAdditionalFilter(Filter.equals(COL_CARGO, relId)));
            } else {
              event.getSelector().setAdditionalFilter(Filter.isFalse());
            }
          }
        }));
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public GridInterceptor getInstance() {
    return new CargoExpensesGrid();
  }
}
