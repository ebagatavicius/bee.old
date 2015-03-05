package com.butent.bee.client.modules.documents;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.i18n.Localized;

public class DocumentTemplateForm extends DocumentDataForm {

  private final Button newDocumentButton = new Button(Localized.getConstants().documentNew(),
      new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          RowFactory.createRow(TBL_DOCUMENTS, new RowCallback() {
            @Override
            public void onSuccess(final BeeRow row) {
              DocumentsHandler.copyDocumentData(getLongValue(COL_DOCUMENT_DATA), new IdCallback() {
                @Override
                public void onSuccess(Long dataId) {
                  Queries.update(TBL_DOCUMENTS, Filter.compareId(row.getId()),
                      COL_DOCUMENT_DATA, new LongValue(dataId), new IntCallback() {
                        @Override
                        public void onSuccess(Integer res) {
                          Long oldId = Data.getLong(TBL_DOCUMENTS, row, COL_DOCUMENT_DATA);

                          if (DataUtils.isId(oldId)) {
                            Queries.deleteRow(VIEW_DOCUMENT_DATA, oldId);
                          }
                          RowEditor.open(TBL_DOCUMENTS, row.getId(), Opener.MODAL);
                        }
                      });
                }
              });
            }
          });
        }
      });

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    getHeaderView().clearCommandPanel();

    if (!DataUtils.isNewRow(row)) {
      getHeaderView().addCommandItem(newDocumentButton);
    }
    super.onStart(form);
  }
}
