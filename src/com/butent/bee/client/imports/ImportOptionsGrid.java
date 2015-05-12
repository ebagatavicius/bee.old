package com.butent.bee.client.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.modules.transport.TransportConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ImportOptionsGrid extends AbstractGridInterceptor {

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override
  public void onReadyForInsert(final GridView gridView, final ReadyForInsertEvent event) {
    List<BeeColumn> columns = event.getColumns();

    for (int i = 0; i < columns.size(); i++) {
      if (BeeUtils.same(columns.get(i).getId(), COL_IMPORT_TYPE)) {
        ImportType type = EnumUtils.getEnumByIndex(ImportType.class, event.getValues().get(i));

        switch (type) {
          case TRACKING:
            event.consume();
            event.getColumns().add(DataUtils.getColumn(COL_IMPORT_DATA, gridView.getDataColumns()));
            event.getValues().add(TransportConstants.TBL_VEHICLE_TRACKING);
            gridView.fireEvent(event);
            return;

          case DATA:
            event.consume();
            final ListBox listBox = new ListBox();
            Map<String, String> map = new TreeMap<>();

            for (DataInfo dataInfo : Data.getDataInfoProvider().getViews()) {
              String viewName = dataInfo.getViewName();
              map.put(BeeUtils.parenthesize(dataInfo.getModule() + "." + viewName), viewName);
            }
            for (Entry<String, String> entry : map.entrySet()) {
              listBox.addItem(BeeUtils.joinWords(Data.getViewCaption(entry.getValue()),
                  entry.getKey()), entry.getValue());
            }
            Global.inputWidget(Localized.getConstants().data(), listBox, new InputCallback() {
              @Override
              public void onSuccess() {
                String viewName = listBox.getValue();

                if (!BeeUtils.isEmpty(viewName)) {
                  event.getColumns()
                      .add(DataUtils.getColumn(COL_IMPORT_DATA, gridView.getDataColumns()));
                  event.getValues().add(viewName);
                  gridView.fireEvent(event);
                }
              }
            });
            return;

          default:
            break;
        }
        break;
      }
    }
    super.onReadyForInsert(gridView, event);
  }
}
