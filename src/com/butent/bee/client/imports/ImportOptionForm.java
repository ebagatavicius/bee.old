package com.butent.bee.client.imports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.cars.CarsConstants;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.Arrays;
import java.util.Objects;

public class ImportOptionForm extends AbstractFormInterceptor implements ClickHandler {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "co-updateRates-";

  private CustomAction importAction = new CustomAction(FontAwesome.CLOUD_UPLOAD, this);
  private FaLabel mappings = new FaLabel(FontAwesome.RANDOM);

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof ChildGrid && BeeUtils.same(name, TBL_IMPORT_PROPERTIES)) {
      ((ChildGrid) widget).setGridInterceptor(new ImportPropertiesGrid());
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterRefresh(FormView form, IsRow row) {
    if (!getHeaderView().hasCommands()) {
      importAction.setTitle(Localized.dictionary().actionImport());
      getHeaderView().addCommandItem(importAction);

      mappings.setTitle(Localized.dictionary().trImportMappings());
      mappings.addClickHandler(clickEvent -> RowEditor.openForm(FORM_IMPORT_MAPPINGS,
          Data.getDataInfo(getViewName()), getActiveRow(), Opener.MODAL, null,
          new ImportOptionMappingsForm(getStringValue(COL_IMPORT_DATA))));
      getHeaderView().addCommandItem(mappings);
    }
    mappings.setVisible(!BeeUtils.isEmpty(getStringValue(COL_IMPORT_DATA)));

    super.afterRefresh(form, row);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ImportOptionForm();
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    ParameterList args = AdministrationKeeper.createArgs(SVC_DO_IMPORT);
    args.addDataItem(COL_IMPORT_OPTION, getActiveRowId());

    Integer typeIndex = getIntegerValue(COL_IMPORT_TYPE);
    args.addDataItem(COL_IMPORT_TYPE, typeIndex);

    if (clickEvent.isShiftKeyDown()) {
      args.addDataItem(VAR_IMPORT_TEST, 1);
    }
    ImportType type = EnumUtils.getEnumByIndex(ImportType.class, typeIndex);

    switch (type) {
      case TRACKING:
        importTracking(args);
        break;

      case CONFIGURATION:
        importConfiguration(args);
        break;

      default:
        if (Objects.equals(type, ImportType.DATA)
            && !BeeKeeper.getUser().canCreateData(getStringValue(COL_IMPORT_DATA))) {
          getFormView().notifyWarning(Localized.dictionary().actionNotAllowed());
          return;
        }
        upload(args, importAction);
        break;
    }
  }

  public static void upload(ParameterList args, CustomAction action) {
    Popup popup = new Popup(Popup.OutsideClick.CLOSE);
    InputFile file = new InputFile(false);

    file.addChangeHandler(event -> {
      popup.close();
      action.running();
      NewFileInfo fileInfo = BeeUtils.peek(FileUtils.getNewFileInfos(file.getFiles()));

      FileUtils.uploadFile(fileInfo, new Callback<FileInfo>() {
        @Override
        public void onFailure(String... reason) {
          action.idle();
          Callback.super.onFailure(reason);
        }

        @Override
        public void onSuccess(FileInfo result) {
          args.addDataItem(VAR_IMPORT_FILE, result.getId());
          ImportCallback.makeRequest(args, action, fileInfo.getName());
        }
      });
    });
    popup.addOpenHandler(event -> file.click());
    popup.setWidget(file);
    popup.center();
  }

  private void importConfiguration(ParameterList args) {
    Relation relation = Relation.create(CarsConstants.TBL_CONF_PRICELIST,
        Arrays.asList("ParentName", CarsConstants.COL_BRANCH_NAME));
    relation.disableNewRow();
    relation.disableEdit();
    UnboundSelector selector = UnboundSelector.create(relation);

    Global.inputWidget(Localized.dictionary().category(), selector, () -> {
          if (DataUtils.isId(selector.getRelatedId())) {
            args.addDataItem(CarsConstants.COL_BRANCH, selector.getRelatedId());
            upload(args, importAction);
          }
        }
    );
  }

  private void importTracking(ParameterList args) {
    Flow panel = new Flow(STYLE_PREFIX + "panel");

    Label lowLabel = new Label(Localized.dictionary().dateFromShort());
    lowLabel.addStyleName(STYLE_PREFIX + "lowLabel");
    panel.add(lowLabel);

    final InputDate lowInput = new InputDate();
    lowInput.addStyleName(STYLE_PREFIX + "lowInput");
    lowInput.setDate(TimeUtils.today());
    lowInput.setNullable(false);
    panel.add(lowInput);

    CustomDiv rangeSeparator = new CustomDiv(STYLE_PREFIX + "rangeSeparator");
    panel.add(rangeSeparator);

    Label highLabel = new Label(Localized.dictionary().dateToShort());
    highLabel.addStyleName(STYLE_PREFIX + "highLabel");
    panel.add(highLabel);

    final InputDate highInput = new InputDate();
    highInput.addStyleName(STYLE_PREFIX + "highInput");
    highInput.setDate(TimeUtils.today(1));
    highInput.setNullable(false);
    panel.add(highInput);

    CustomDiv actionSeparator = new CustomDiv(STYLE_PREFIX + "actionSeparator");
    panel.add(actionSeparator);

    Button submit = new Button(Localized.dictionary().actionImport());
    submit.addStyleName(STYLE_PREFIX + "submit");
    panel.add(submit);

    final DialogBox dialog = DialogBox.create(null, STYLE_PREFIX + "dialog");
    dialog.setWidget(panel);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.center();

    submit.addClickHandler(event -> {
      JustDate lowDate = lowInput.getDate();
      if (lowDate == null) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().valueRequired());
        lowInput.setFocus(true);
        return;
      }

      JustDate hightDate = highInput.getDate();
      if (hightDate == null) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().valueRequired());
        highInput.setFocus(true);
        return;
      }

      if (TimeUtils.isMeq(lowDate, hightDate)) {
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().invalidRange(),
            BeeUtils.joinWords(lowDate, hightDate));
        return;
      }
      dialog.close();

      args.addDataItem(VAR_DATE_LOW, lowDate.getDays());
      args.addDataItem(VAR_DATE_HIGH, hightDate.getDays());

      ImportCallback.makeRequest(args, importAction, getStringValue(COL_IMPORT_DESCRIPTION));
    });
  }
}
