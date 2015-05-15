package com.butent.bee.client.imports;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.GridPanel;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.imports.ImportType;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.websocket.messages.ProgressMessage;

import java.util.Map;
import java.util.Objects;

public class ImportsForm extends AbstractFormInterceptor implements ClickHandler {
  private final class ImportCallback extends ResponseCallback {

    private final String progressId;

    public ImportCallback(String progressId) {
      this.progressId = progressId;
    }

    @Override
    public void onResponse(ResponseObject response) {
      if (progressId != null) {
        Endpoint.removeProgress(progressId);
        Endpoint.send(ProgressMessage.close(progressId));
      }
      setImporting(false);
      Assert.notNull(response);

      if (response.hasErrors()) {
        response.notify(getFormView());
        return;
      }
      Map<String, String> data = Codec.deserializeMap(response.getResponseAsString());

      HtmlTable table = new HtmlTable(StyleUtils.NAME_INFO_TABLE);
      int r = 0;
      table.setColumnCellClasses(1, StyleUtils.className(TextAlign.CENTER));
      table.setColumnCellClasses(2, StyleUtils.className(TextAlign.CENTER));
      table.setText(r, 1, Localized.getConstants().imported(),
          StyleUtils.className(FontWeight.BOLD));
      table.setText(r, 2, Localized.getConstants().errors(), StyleUtils.className(FontWeight.BOLD));

      for (final String viewName : data.keySet()) {
        Pair<String, String> pair = Pair.restore(data.get(viewName));

        DataInfo info = Data.getDataInfo(viewName, false);

        final String cap = Data.getDataInfo(viewName, false) != null
            ? Data.getViewCaption(viewName) : viewName;

        table.setText(++r, 0, cap);
        table.setText(r, 1, pair.getA());

        InternalLink lbl = null;

        if (pair.getB() != null) {
          final BeeRowSet rs = BeeRowSet.restore(pair.getB());
          lbl = new InternalLink(BeeUtils.toString(rs.getNumberOfRows()));

          lbl.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
              Global.showModalGrid(cap, rs);
            }
          });
        }
        table.setWidget(r, 2, lbl);
      }
      Global.showModalWidget(table);
    }
  }

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "co-updateRates-";

  GridPanel imports;
  Button action;

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (widget instanceof HasClickHandlers && BeeUtils.same(name, SVC_DO_IMPORT)) {
      action = (Button) widget;
      action.addClickHandler(this);
      setImporting(false);
    }
    if (widget instanceof GridPanel && BeeUtils.same(name, TBL_IMPORT_OPTIONS)) {
      imports = (GridPanel) widget;
      imports.setGridInterceptor(new ImportOptionsGrid());

    } else if (widget instanceof ChildGrid && BeeUtils.same(name, TBL_IMPORT_PROPERTIES)) {
      ((ChildGrid) widget).setGridInterceptor(new ImportPropertiesGrid());
    }
    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public FormInterceptor getInstance() {
    return new ImportsForm();
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    if (imports == null) {
      return;
    }
    GridView grid = imports.getGridView();
    IsRow row = grid.getActiveRow();

    if (row == null) {
      getFormView().notifyWarning(Localized.getConstants().selectImport());
    } else {
      final ParameterList args = AdministrationKeeper.createArgs(SVC_DO_IMPORT);
      args.addDataItem(COL_IMPORT_OPTION, row.getId());

      Integer typeIndex = row.getInteger(grid.getDataIndex(COL_IMPORT_TYPE));
      args.addDataItem(COL_IMPORT_TYPE, typeIndex);

      if (clickEvent.isShiftKeyDown()) {
        args.addDataItem(VAR_IMPORT_TEST, 1);
      }
      ImportType type = EnumUtils.getEnumByIndex(ImportType.class, typeIndex);

      switch (type) {
        case TRACKING:
          importTracking(args);
          break;

        default:
          final String cap;

          if (Objects.equals(type, ImportType.DATA)) {
            String viewName = row.getString(grid.getDataIndex(COL_IMPORT_DATA));
            cap = row.getString(grid.getDataIndex(COL_IMPORT_DESCRIPTION));

            if (!BeeKeeper.getUser().canCreateData(viewName)) {
              getFormView().notifyWarning(Localized.getConstants().actionNotAllowed());
              return;
            }
          } else {
            cap = type.getCaption();
          }
          upload(new Callback<String>() {
            @Override
            public void onFailure(String... reason) {
              setImporting(false);
              super.onFailure(reason);
            }

            @Override
            public void onSuccess(String fileName) {
              setImporting(true);
              args.addDataItem(VAR_IMPORT_FILE, fileName);

              Endpoint.initProgress(cap, new Consumer<String>() {
                @Override
                public void accept(String progress) {
                  if (!BeeUtils.isEmpty(progress)) {
                    args.addDataItem(Service.VAR_PROGRESS, progress);
                  }
                  BeeKeeper.getRpc().makePostRequest(args, new ImportCallback(progress));
                }
              });
            }
          });
          break;
      }
    }
  }

  private void importTracking(final ParameterList args) {
    Flow panel = new Flow(STYLE_PREFIX + "panel");

    Label lowLabel = new Label(Localized.getConstants().dateFromShort());
    lowLabel.addStyleName(STYLE_PREFIX + "lowLabel");
    panel.add(lowLabel);

    final InputDate lowInput = new InputDate();
    lowInput.addStyleName(STYLE_PREFIX + "lowInput");
    lowInput.setDate(TimeUtils.today());
    lowInput.setNullable(false);
    panel.add(lowInput);

    CustomDiv rangeSeparator = new CustomDiv(STYLE_PREFIX + "rangeSeparator");
    panel.add(rangeSeparator);

    Label highLabel = new Label(Localized.getConstants().dateToShort());
    highLabel.addStyleName(STYLE_PREFIX + "highLabel");
    panel.add(highLabel);

    final InputDate highInput = new InputDate();
    highInput.addStyleName(STYLE_PREFIX + "highInput");
    highInput.setDate(TimeUtils.today(1));
    highInput.setNullable(false);
    panel.add(highInput);

    CustomDiv actionSeparator = new CustomDiv(STYLE_PREFIX + "actionSeparator");
    panel.add(actionSeparator);

    Button submit = new Button(Localized.getConstants().actionImport());
    submit.addStyleName(STYLE_PREFIX + "submit");
    panel.add(submit);

    final DialogBox dialog = DialogBox.create(null, STYLE_PREFIX + "dialog");
    dialog.setWidget(panel);

    dialog.setAnimationEnabled(true);
    dialog.setHideOnEscape(true);

    dialog.center();

    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        JustDate lowDate = lowInput.getDate();
        if (lowDate == null) {
          BeeKeeper.getScreen().notifyWarning(Localized.getConstants().valueRequired());
          lowInput.setFocus(true);
          return;
        }

        JustDate hightDate = highInput.getDate();
        if (hightDate == null) {
          BeeKeeper.getScreen().notifyWarning(Localized.getConstants().valueRequired());
          highInput.setFocus(true);
          return;
        }

        if (TimeUtils.isMeq(lowDate, hightDate)) {
          BeeKeeper.getScreen().notifyWarning(Localized.getConstants().invalidRange(),
              BeeUtils.joinWords(lowDate, hightDate));
          return;
        }
        setImporting(true);
        dialog.close();

        args.addDataItem(VAR_DATE_LOW, lowDate.getDays());
        args.addDataItem(VAR_DATE_HIGH, hightDate.getDays());

        Endpoint.initProgress(ImportType.TRACKING.getCaption(), new Consumer<String>() {
          @Override
          public void accept(String progress) {
            if (!BeeUtils.isEmpty(progress)) {
              args.addDataItem(Service.VAR_PROGRESS, progress);
            }
            BeeKeeper.getRpc().makePostRequest(args, new ImportCallback(progress));
          }
        });
      }
    });
  }

  private void setImporting(boolean importing) {
    if (action != null) {
      action.setEnabled(!importing);
      action.setText(importing
          ? Localized.getConstants().importing() : Localized.getConstants().actionImport());
    }
  }

  private static void upload(final Callback<String> fileCallback) {
    final Popup popup = new Popup(Popup.OutsideClick.CLOSE);
    final InputFile widget = new InputFile(false);

    widget.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        popup.close();
        FileUtils.uploadTempFile(BeeUtils.peek(FileUtils.getNewFileInfos(widget.getFiles())),
            fileCallback);
      }
    });
    popup.setWidget(widget);
    popup.center();
  }
}
