package com.butent.bee.client.modules.classifiers;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_FILE_HASH;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.File;
import elemental.html.FileReader;

class PersonForm extends AbstractFormInterceptor {

  private static final String PHOTO_FILE_WIDGET_NAME = "PhotoFile";
  private static final String PHOTO_IMAGE_WIDGET_NAME = "PhotoImg";
  private static final String UNSET_PHOTO_WIDGET_NAME = "unsetPhoto";

  private static final String DEFAULT_PHOTO_IMAGE = "images/silver/person_profile.png";

  private Image photoImageWidget;
  private NewFileInfo photoImageAttachment;
  private final Map<Long, NewFileInfo> uploadQueue = new ConcurrentHashMap<>();

  PersonForm() {
    super();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, PHOTO_FILE_WIDGET_NAME) && widget instanceof FileCollector) {
      final FileCollector fc = (FileCollector) widget;
      fc.addSelectionHandler(new SelectionHandler<FileInfo>() {
        @Override
        public void onSelection(SelectionEvent<FileInfo> event) {
          if (!(event.getSelectedItem() instanceof NewFileInfo)) {
            return;
          }
          NewFileInfo fileInfo = (NewFileInfo) event.getSelectedItem();
          fc.clear();

          if (getPhotoImageWidget() != null && fileInfo != null) {
            String type = fileInfo.getType();
            long size = fileInfo.getSize();

            if (!BeeUtils.containsSame(type, "image")) {
              BeeKeeper.getScreen().notifyWarning(
                  Localized.dictionary().invalidImageFileType(fileInfo.getName(), type));

            } else if (size > Images.MAX_SIZE_FOR_DATA_URL) {
              BeeKeeper.getScreen().notifyWarning(
                  Localized.dictionary().fileSizeExceeded(size, Images.MAX_SIZE_FOR_DATA_URL));

            } else {
              setPhotoImageAttachment(fileInfo);
              setPhotoModified(getFormView(), false);
              showImageInFormBeforeUpload(getPhotoImageWidget(), fileInfo.getNewFile());
            }
          }
        }
      });

    } else if (BeeUtils.same(name, PHOTO_IMAGE_WIDGET_NAME) && widget instanceof Image) {
      photoImageWidget = (Image) widget;

    } else if (BeeUtils.same(name, UNSET_PHOTO_WIDGET_NAME)) {
      Binder.addClickHandler(widget.asWidget(), new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          setPhotoImageAttachment(null);
          setPhotoModified(getFormView(), true);
          clearPhoto();
        }
      });
    } else if (BeeUtils.same(name, TBL_COMPANY_PERSONS) && widget instanceof ChildGrid) {
      ((ChildGrid) widget).setGridInterceptor(new AbstractGridInterceptor() {
        @Override
        public ColumnDescription beforeCreateColumn(GridView gridView, ColumnDescription descr) {
          if (BeeUtils.same(descr.getId(), COL_PERSON)) {
            return null;
          }
          return super.beforeCreateColumn(gridView, descr);
        }

        @Override
        public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
          presenter.getGridView().ensureRelId(new IdCallback() {
            @Override
            public void onSuccess(Long id) {
              final String viewName = presenter.getViewName();
              DataInfo dataInfo = Data.getDataInfo(viewName);
              BeeRow newRow = RowFactory.createEmptyRow(dataInfo, true);
              Data.setValue(viewName, newRow, COL_PERSON, id);

              RowFactory.createRow(dataInfo.getNewRowForm(),
                  Localized.dictionary().newPersonCompany(), dataInfo, newRow, Modality.ENABLED,
                  null, new AbstractFormInterceptor() {
                    @Override
                    public boolean beforeCreateWidget(String widgetName, Element description) {
                      if (BeeUtils.startsWith(widgetName, COL_PERSON)) {
                        return false;
                      }
                      return super.beforeCreateWidget(widgetName, description);
                    }

                    @Override
                    public FormInterceptor getInstance() {
                      return null;
                    }
                  }, null,
                  new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow result) {
                      Data.refreshLocal(viewName);
                    }
                  });
            }
          });
          return false;
        }

        @Override
        public String getCaption() {
          return Localized.dictionary().personCompanies();
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }
      });
    }
  }

  @Override
  public void afterInsertRow(IsRow result, boolean forced) {

    if (getPhotoImageAttachment() != null) {
      uploadPhoto(result.getId(), getPhotoImageAttachment());
    }
  }

  @Override
  public void afterUpdateRow(IsRow result) {
    UserData userData = BeeKeeper.getUser().getUserData();

    if (Objects.equals(userData.getPerson(), result.getId())) {
      userData.setFirstName(result.getString(getDataIndex(COL_FIRST_NAME)));
      userData.setLastName(result.getString(getDataIndex(COL_LAST_NAME)));
      BeeKeeper.getScreen().updateUserData(userData);
    }
    if (!getUploadQueue().containsKey(result.getId())) {
      return;
    }
    uploadPhoto(result.getId(), getUploadQueue().get(result.getId()));
  }

  @Override
  public FormInterceptor getInstance() {
    return new PersonForm();
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    final FormView form = getFormView();
    final IsRow row = event.getNewRow();

    ensureUpload(form, event.getOldRow(), row, getUploadQueue(), getPhotoImageAttachment());
  }

  @Override
  public boolean onStartEdit(FormView form, IsRow row, ScheduledCommand focusCommand) {
    showPhoto(form, row);
    if (!DataUtils.isNewRow(row)) {
      createQrButton(form, row);
    }
    return super.onStartEdit(form, row, focusCommand);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {
    showPhoto(form, newRow);
    super.onStartNewRow(form, oldRow, newRow);
  }

  private void clearPhoto() {
    if (getPhotoImageWidget() != null) {
      getPhotoImageWidget().setUrl(DEFAULT_PHOTO_IMAGE);
    }
  }

  private static void createQrButton(final FormView form, final IsRow row) {
    FlowPanel qrFlowPanel = (FlowPanel) Assert.notNull(form.getWidgetByName(QR_FLOW_PANEL));
    qrFlowPanel.clear();
    FaLabel qrCodeLabel = new FaLabel(FontAwesome.QRCODE);
    qrCodeLabel.setTitle(Localized.dictionary().qrCode());
    qrCodeLabel.addStyleName("bee-FontSize-x-large");
    qrFlowPanel.add(qrCodeLabel);

    qrCodeLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        ClassifierKeeper.generateQrCode(form, row);
      }
    });

  }

  private static void setPhotoModified(FormView form, boolean remove) {
    Assert.notNull(form);

    IsRow row = form.getActiveRow();
    IsRow oldRow = form.getOldRow();
    int idxPhoto = form.getDataIndex(COL_PHOTO);

    if (BeeConst.isUndef(idxPhoto)) {
      return;
    }

    if (row != null) {
      row.clearCell(idxPhoto);
    }

    if (!remove && oldRow.isNull(idxPhoto)) {
      oldRow.setValue(idxPhoto, BeeConst.UNDEF);
    }
  }

  private static void showImageInFormBeforeUpload(final Image image, File file) {
    if (image == null || file == null) {
      return;
    }

    if (Features.supportsFileApi()) {
      final FileReader reader = Browser.getWindow().newFileReader();

      reader.setOnload(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          image.setUrl((String) reader.getResult());
        }
      });

      reader.readAsDataURL(file);
    }
  }

  private void showPhoto(FormView form, IsRow row) {
    setPhotoImageAttachment(null);

    if (getPhotoImageWidget() != null) {
      if (DataUtils.isId(row.getLong(form.getDataIndex(COL_PHOTO)))) {
        photoImageWidget
            .setUrl(PhotoRenderer.getPhotoUrl(row.getString(form.getDataIndex(COL_FILE_HASH))));
      } else {
        clearPhoto();
      }
    }
  }

  private static void ensureUpload(FormView form, IsRow oldRow, IsRow row,
      Map<Long, NewFileInfo> queue, NewFileInfo photoFile) {

    int idxPhoto = form.getDataIndex(COL_PHOTO);
    long oldPhoto = BeeUtils.unbox(oldRow.getLong(idxPhoto));
    long newPhoto = BeeUtils.unbox(row.getLong(idxPhoto));

    if (oldPhoto == newPhoto || DataUtils.isNewRow(row)) {
      return;
    }

    if (photoFile != null) {
      queue.put(row.getId(), photoFile);
    }

  }

  private void uploadPhoto(final long rowId, NewFileInfo file) {
    FileUtils.uploadFile(Assert.notNull(file), info ->
        Queries.update(VIEW_PERSONS, rowId, COL_PHOTO, Value.getValue(info.getId()),
            new IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_PERSONS);
                UserData userData = BeeKeeper.getUser().getUserData();

                if (Objects.equals(userData.getPerson(), rowId)) {
                  userData.setPhotoFile(info.getHash());
                  BeeKeeper.getScreen().updateUserData(userData);
                }

                getUploadQueue().remove(rowId);
              }
            }));
  }

  private Image getPhotoImageWidget() {
    return photoImageWidget;
  }

  private NewFileInfo getPhotoImageAttachment() {
    return photoImageAttachment;
  }

  private void setPhotoImageAttachment(NewFileInfo photoImageAttachment) {
    this.photoImageAttachment = photoImageAttachment;

  }

  private Map<Long, NewFileInfo> getUploadQueue() {
    return uploadQueue;
  }

}
