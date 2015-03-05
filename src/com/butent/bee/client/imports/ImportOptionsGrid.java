package com.butent.bee.client.imports;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.Thermometer;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.administration.AdministrationKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.websocket.Endpoint;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.client.widget.InternalLink;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

public class ImportOptionsGrid extends AbstractGridInterceptor implements ClickHandler {

  private final class ImportCallback implements ResponseCallback {

    private final String progressId;

    public ImportCallback() {
      this(null);
    }

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
        response.notify(getGridView());
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

        table.setText(++r, 0, Data.getViewCaption(viewName));
        table.setText(r, 1, pair.getA());

        InternalLink lbl = null;

        if (pair.getB() != null) {
          final BeeRowSet rs = BeeRowSet.restore(pair.getB());
          lbl = new InternalLink(BeeUtils.toString(rs.getNumberOfRows()));

          lbl.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
              Global.showModalGrid(Data.getViewCaption(viewName), rs);
            }
          });
        }
        table.setWidget(r, 2, lbl);
      }
      Global.showModalWidget(table);
    }
  }

  private static final String STYLE_UPDATE_RATES_PREFIX = BeeConst.CSS_CLASS_PREFIX
      + "co-updateRates-";

  private final Image loading = new Image(Global.getImages().loading());
  private final Button importButton = new Button(Localized.getConstants().dataImport(), this);

  @Override
  public GridInterceptor getInstance() {
    return new ImportOptionsGrid();
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    setImporting(false);
  }

  @Override
  public void onReadyForInsert(final GridView gridView, final ReadyForInsertEvent event) {
    List<BeeColumn> columns = event.getColumns();

    for (int i = 0; i < columns.size(); i++) {
      if (BeeUtils.same(columns.get(i).getId(), COL_IMPORT_TYPE)) {
        ImportType type = EnumUtils.getEnumByIndex(ImportType.class, event.getValues().get(i));

        if (Objects.equals(type, ImportType.DATA)) {
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
        }
        break;
      }
    }
    super.onReadyForInsert(gridView, event);
  }

  @Override
  public void onClick(ClickEvent event) {
    IsRow row = getGridView().getActiveRow();

    if (row != null) {
      final ParameterList args = AdministrationKeeper.createArgs(SVC_DO_IMPORT);
      args.addDataItem(COL_IMPORT_OPTION, row.getId());

      Integer typeIndex = row.getInteger(getGridView().getDataIndex(COL_IMPORT_TYPE));
      args.addDataItem(COL_IMPORT_TYPE, typeIndex);

      if (event.isShiftKeyDown()) {
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
            String viewName = row.getString(getGridView().getDataIndex(COL_IMPORT_DATA));
            cap = row.getString(getGridView().getDataIndex(COL_IMPORT_DESCRIPTION));

            if (!BeeKeeper.getUser().canCreateData(viewName)) {
              getGridView().notifyWarning(Localized.getConstants().no());
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
              final String progressId;

              if (Endpoint.isOpen()) {
                InlineLabel close = new InlineLabel(String.valueOf(BeeConst.CHAR_TIMES));
                Thermometer th = new Thermometer(cap, BeeConst.DOUBLE_ONE, close);
                progressId = BeeKeeper.getScreen().addProgress(th);

                if (progressId != null) {
                  close.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent ev) {
                      Endpoint.cancelProgress(progressId);
                    }
                  });
                }
              } else {
                progressId = null;
              }
              if (progressId == null) {
                BeeKeeper.getRpc().makePostRequest(args, new ImportCallback());
              } else {
                Endpoint.enqueuePropgress(progressId, new Consumer<String>() {
                  @Override
                  public void accept(String input) {
                    if (!BeeUtils.isEmpty(input)) {
                      args.addDataItem(Service.VAR_PROGRESS, input);
                    } else {
                      Endpoint.cancelProgress(progressId);
                    }
                    BeeKeeper.getRpc().makePostRequest(args, new ImportCallback(progressId));
                  }
                });
              }
            }
          });
          break;
      }
    }
  }

  private void importTracking(final ParameterList args) {
    Flow panel = new Flow(STYLE_UPDATE_RATES_PREFIX + "panel");

    Label lowLabel = new Label(Localized.getConstants().dateFromShort());
    lowLabel.addStyleName(STYLE_UPDATE_RATES_PREFIX + "lowLabel");
    panel.add(lowLabel);

    final InputDate lowInput = new InputDate();
    lowInput.addStyleName(STYLE_UPDATE_RATES_PREFIX + "lowInput");
    lowInput.setDate(TimeUtils.today());
    lowInput.setNullable(false);
    panel.add(lowInput);

    CustomDiv rangeSeparator = new CustomDiv(STYLE_UPDATE_RATES_PREFIX + "rangeSeparator");
    panel.add(rangeSeparator);

    Label highLabel = new Label(Localized.getConstants().dateToShort());
    highLabel.addStyleName(STYLE_UPDATE_RATES_PREFIX + "highLabel");
    panel.add(highLabel);

    final InputDate highInput = new InputDate();
    highInput.addStyleName(STYLE_UPDATE_RATES_PREFIX + "highInput");
    highInput.setDate(TimeUtils.today(1));
    highInput.setNullable(false);
    panel.add(highInput);

    CustomDiv actionSeparator = new CustomDiv(STYLE_UPDATE_RATES_PREFIX + "actionSeparator");
    panel.add(actionSeparator);

    Button submit = new Button(Localized.getConstants().dataImport());
    submit.addStyleName(STYLE_UPDATE_RATES_PREFIX + "submit");
    panel.add(submit);

    final DialogBox dialog = DialogBox.create(null, STYLE_UPDATE_RATES_PREFIX + "dialog");
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

        BeeKeeper.getRpc().makePostRequest(args, new ImportCallback());
      }
    });
  }

  private void setImporting(boolean importing) {
    HeaderView hdr = getGridPresenter().getHeader();
    hdr.clearCommandPanel();

    if (importing) {
      hdr.addCommandItem(loading);
    } else {
      hdr.addCommandItem(importButton);
    }
  }

  private static void upload(final Callback<String> fileCallback) {
    final Popup popup = new Popup(OutsideClick.CLOSE);
    final InputFile widget = new InputFile(false);

    widget.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        popup.close();
        List<NewFileInfo> files = FileUtils.getNewFileInfos(widget.getFiles());

        for (final NewFileInfo fi : files) {
          FileUtils.uploadTempFile(fi, fileCallback);
        }
      }
    });
    popup.setWidget(widget);
    popup.center();
  }
}
