package com.butent.bee.client.modules.service;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.utils.BeeUtils;

class SelectorHandler implements SelectorEvent.Handler {

  private static void copyDocumentCriteria(Long docDataId, Long objId) {
    ParameterList params = ServiceKeeper.createArgs(SVC_COPY_DOCUMENT_CRITERIA);
    
    params.addQueryItem(COL_DOCUMENT_DATA, docDataId);
    params.addQueryItem(COL_SERVICE_OBJECT, objId);
    
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse()) {
          DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_SERVICE_OBJECTS);
        }
      }
    });
  }
  
  SelectorHandler() {
  }
  
  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event != null && event.isRowCreated() 
        && event.getNewRow() != null && DataUtils.isId(event.getNewRow().getId())
        && BeeUtils.same(event.getRelatedViewName(), VIEW_SERVICE_OBJECTS)) {
      
      FormView form = UiHelper.getForm(event.getSelector());
      if (form != null && BeeUtils.same(form.getViewName(), VIEW_DOCUMENTS)) {
        
        IsRow docRow = form.getActiveRow();
        int index = form.getDataIndex(COL_DOCUMENT_DATA);

        if (docRow != null && DataUtils.isId(docRow.getId()) && !BeeConst.isUndef(index)) {
          Long dataId = docRow.getLong(index);
          if (DataUtils.isId(dataId)) {
            copyDocumentCriteria(dataId, event.getNewRow().getId());
          }
        }
      }
    }
  }
}
