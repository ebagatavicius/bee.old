package com.butent.bee.client.modules.cars;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_PHOTO;

import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;

import java.util.Objects;

public class PhotoHandler extends AbstractFormInterceptor {

  private Image image;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (Objects.equals(name, COL_PHOTO) && widget instanceof Image) {
      image = (Image) widget;
      image.addClickHandler(new PhotoPicker(PhotoHandler.this::onSetPhoto));
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    if (image != null) {
      String url;
      Long fileId = DataUtils.getLong(form.getDataColumns(), row, COL_PHOTO);

      if (DataUtils.isId(fileId)) {
        url = FileUtils.getUrl(fileId);
      } else {
        url = "images/logo.png";
      }
      image.setUrl(url);
    }
    super.beforeRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new PhotoHandler();
  }

  private void onSetPhoto(NewFileInfo fileInfo) {
    Long id = getActiveRowId();
    int idx = getFormView().getDataIndex(COL_PHOTO);

    if (fileInfo != null) {
      FileUtils.uploadFile(fileInfo, fileId -> {
        if (Objects.equals(id, getActiveRowId())) {
          getActiveRow().setValue(idx, fileId);
          getFormView().refresh();
        }
      });
    } else {
      getActiveRow().clearCell(idx);
      getFormView().refresh();
    }
  }
}
