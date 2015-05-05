package com.butent.bee.client.modules.classifiers;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.xml.client.Element;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.composite.FileCollector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
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
import com.butent.bee.client.view.add.ReadyForInsertEvent;
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
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Objects;

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

          if (photoImageWidget != null && fileInfo != null) {
            String type = fileInfo.getType();
            long size = fileInfo.getSize();

            if (!BeeUtils.containsSame(type, "image")) {
              BeeKeeper.getScreen().notifyWarning(
                  Localized.getMessages().invalidImageFileType(fileInfo.getName(), type));

            } else if (size > Images.MAX_SIZE_FOR_DATA_URL) {
              BeeKeeper.getScreen().notifyWarning(
                  Localized.getMessages().fileSizeExceeded(size, Images.MAX_SIZE_FOR_DATA_URL));

            } else {
              photoImageAttachment = fileInfo;
              setPhotoFileName(fileInfo);

              showImageInFormBeforeUpload(photoImageWidget, fileInfo.getFile());
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
          FormView form = getFormView();
          IsRow row = form.getActiveRow();

          row.clearCell(form.getDataIndex(COL_PHOTO));
          photoImageAttachment = null;
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
                  Localized.getConstants().newPersonCompany(), dataInfo, newRow, null,
                  new AbstractFormInterceptor() {
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
                  },
                  new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow result) {
                      Data.onViewChange(viewName, DataChangeEvent.RESET_REFRESH);
                    }
                  });
            }
          });
          return false;
        }

        @Override
        public String getCaption() {
          return Localized.getConstants().personCompanies();
        }

        @Override
        public GridInterceptor getInstance() {
          return null;
        }
      });
    }
  }

  @Override
  public FormInterceptor getInstance() {
    return new PersonForm();
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    FormView form = getFormView();
    IsRow row = form.getActiveRow();

    final String photoFileName = getPhotoFileName(form, row);

    if (!BeeUtils.isEmpty(photoFileName) && photoImageAttachment != null) {
      if (photoImageWidget != null) {
        PhotoRenderer.addToCache(photoFileName, photoImageWidget.getUrl());
      }

      FileUtils.uploadPhoto(photoImageAttachment, photoFileName, null, new Callback<String>() {
        @Override
        public void onFailure(String... reason) {
          BeeKeeper.getScreen().notifySevere(Localized.getConstants().imageUploadFailed());
        }

        @Override
        public void onSuccess(String result) {
        }
      });
    }
  }

  @Override
  public void onSaveChanges(HasHandlers listener, SaveChangesEvent event) {
    final FormView form = getFormView();
    final IsRow row = form.getActiveRow();

    final String photoFileName = getPhotoFileName(form, row);
    final String oldPhoto = getPhotoFileName(form, form.getOldRow());

    if (BeeUtils.equalsTrim(oldPhoto, photoFileName)) {
      updateUserData(form, row);

    } else if (BeeUtils.isEmpty(photoFileName)) {
      updateUserData(form, row);
      FileUtils.deletePhoto(oldPhoto, null);

    } else if (photoImageAttachment != null) {
      if (photoImageWidget != null) {
        PhotoRenderer.addToCache(photoFileName, photoImageWidget.getUrl());
      }

      updateUserData(form, row);

      FileUtils.uploadPhoto(photoImageAttachment, photoFileName, oldPhoto, new Callback<String>() {
        @Override
        public void onFailure(String... reason) {
          setPhotoFileName(form, row, oldPhoto);
          updateUserData(form, row);

          BeeKeeper.getScreen().notifySevere(Localized.getConstants().imageUploadFailed());
        }

        @Override
        public void onSuccess(String result) {
        }
      });
    }
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
    if (photoImageWidget != null) {
      photoImageWidget.setUrl(DEFAULT_PHOTO_IMAGE);
    }
  }

  private static void createQrButton(final FormView form, final IsRow row) {
    FlowPanel qrFlowPanel = (FlowPanel) Assert.notNull(form.getWidgetByName(QR_FLOW_PANEL));
    qrFlowPanel.clear();
    FaLabel qrCodeLabel = new FaLabel(FontAwesome.QRCODE);
    qrCodeLabel.setTitle(Localized.getConstants().qrCode());
    qrCodeLabel.addStyleName("bee-FontSize-x-large");
    qrFlowPanel.add(qrCodeLabel);

    qrCodeLabel.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent arg0) {
        ClassifierKeeper.generateQrCode(form, row);
      }
    });

  }

  private static String getPhotoFileName(FormView form, IsRow row) {
    if (form == null || row == null) {
      return null;
    } else {
      return row.getString(form.getDataIndex(COL_PHOTO));
    }
  }

  private static void setPhotoFileName(FormView form, IsRow row, String value) {
    if (form != null && row != null) {
      row.setValue(form.getDataIndex(COL_PHOTO), value);
    }
  }

  private boolean setPhotoFileName(NewFileInfo fileInfo) {
    FormView form = getFormView();
    IsRow row = form.getActiveRow();

    if (fileInfo == null || row == null) {
      return false;
    } else {
      setPhotoFileName(form, row, FileUtils.generatePhotoFileName(fileInfo.getName()));
      return true;
    }
  }

  private static void showImageInFormBeforeUpload(final Image image, File file) {
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
    photoImageAttachment = null;

    if (photoImageWidget != null) {
      String photoFileName = row.getString(form.getDataIndex(COL_PHOTO));
      if (!BeeUtils.isEmpty(photoFileName)) {
        photoImageWidget.setUrl(PhotoRenderer.getUrl(photoFileName));
      } else {
        clearPhoto();
      }
    }
  }

  private static void updateUserData(FormView form, IsRow row) {
    UserData userData = BeeKeeper.getUser().getUserData();

    if (form != null && row != null && userData != null
        && Objects.equals(userData.getPerson(), row.getId())) {

      userData.setFirstName(row.getString(form.getDataIndex(COL_FIRST_NAME)));
      userData.setLastName(row.getString(form.getDataIndex(COL_LAST_NAME)));
      userData.setPhotoFileName(getPhotoFileName(form, row));

      BeeKeeper.getScreen().updateUserData(userData);
    }
  }
}
