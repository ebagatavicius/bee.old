package com.butent.bee.client.modules.transport;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.gwt.user.client.ui.SimplePanel;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.InputEvent;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class TripCargoGrid extends AbstractGridInterceptor implements ClickHandler {

  private static class Action {

    private final GridView gridView;
    private final int cargoIndex;
    private final int tripIndex;
    private final DialogBox dialog;

    public Action(GridView gridView) {
      CellGrid grd = gridView.getGrid();
      this.gridView = gridView;
      this.cargoIndex = DataUtils.getColumnIndex(COL_CARGO, gridView.getDataColumns());
      this.tripIndex = DataUtils.getColumnIndex(COL_TRIP, gridView.getDataColumns());

      this.dialog = DialogBox.create(Localized.getConstants().trAssignCargo());
      dialog.setHideOnEscape(true);

      Horizontal container = new Horizontal();
      container.setBorderSpacing(5);

      container.add(new Label(Localized.getConstants().trCargoSelectCargo()));

      Relation relation = Relation.create(VIEW_WAITING_CARGO,
          Lists.newArrayList("OrderNo", "CustomerName", "LoadingPostIndex", "LoadingCountryName",
              "UnloadingPostIndex", "UnloadingCountryName"));
      relation.disableNewRow();

      CompoundFilter filter = Filter.and();

      for (IsRow row : grd.getRowData()) {
        filter.add(Filter.compareId(Operator.NE, row.getLong(cargoIndex)));
      }
      relation.setFilter(filter);
      relation.setCaching(Relation.Caching.QUERY);

      final UnboundSelector selector = UnboundSelector.create(relation,
          Lists.newArrayList("OrderNo", "Description"));

      selector.addEditStopHandler(new EditStopEvent.Handler() {
        @Override
        public void onEditStop(EditStopEvent event) {
          if (event.isChanged()) {
            addCargo(BeeUtils.toLong(selector.getValue()));
          }
        }
      });
      container.add(selector);
      dialog.setWidget(container);
    }

    private void addCargo(final long cargoId) {
      if (!DataUtils.isId(cargoId)) {
        return;
      }
      dialog.close();

      gridView.ensureRelId(new IdCallback() {
        @Override
        public void onSuccess(Long result) {
          insertCargo(result, cargoId);
        }
      });
    }

    private void insertCargo(long tripId, long cargoId) {
      List<BeeColumn> columns = DataUtils.getColumns(gridView.getDataColumns(), tripIndex,
          cargoIndex);
      List<String> values = Queries.asList(tripId, cargoId);

      Queries.insert(gridView.getViewName(), columns, values, null, new RowCallback() {
        @Override
        public void onSuccess(BeeRow row) {
          RowInsertEvent.fire(BeeKeeper.getBus(), gridView.getViewName(), row, gridView.getId());
          gridView.getGrid().insertRow(row, false);
        }
      });
    }
  }

  private class MessageDialog extends DialogBox implements ClickHandler {

    private final HtmlTable driverPanel = new HtmlTable();
    private final Label characterCount = new Label();
    private final InputArea messagePanel = new InputArea();
    private final Map<String, String> fields = new LinkedHashMap<>();
    private Collection<String> template;
    private BeeRowSet cargoInfo;
    private Collection<Long> selectedCargos;

    protected MessageDialog() {
      super(Localized.getConstants().message());
      setAnimationEnabled(true);
      setHideOnEscape(true);
      addDefaultCloseBox();

      Flow panel = new Flow();
      panel.add(driverPanel);

      SimplePanel simple = new SimplePanel(new Button(Localized.getConstants().template(), this));
      simple.getElement().getStyle().setTextAlign(TextAlign.CENTER);
      panel.add(simple);

      characterCount.getElement().getStyle().setTextAlign(TextAlign.RIGHT);
      panel.add(characterCount);

      messagePanel.setVisibleLines(15);
      messagePanel.setWidth("100%");
      messagePanel.addInputHandler(new InputHandler() {
        @Override
        public void onInput(InputEvent event) {
          updateCharacterCount();
        }
      });
      panel.add(messagePanel);

      simple = new SimplePanel(new Button(Localized.getConstants().send(), new ClickHandler() {
        @Override
        public void onClick(ClickEvent arg) {
          sendMessage();
        }
      }));
      simple.getElement().getStyle().setTextAlign(TextAlign.CENTER);
      panel.add(simple);

      setWidget(panel);

      for (String fld : new String[] {COL_DESCRIPTION, "Loading", "Unloading",
          "Weight", "Volume", "Quantity", "Partial", "Outsized", "Dangerous", "Number",
          "ShippingTermName", "CmrNumber", "LDM", "Length", "Width", "Height", "Palettes",
          "ExchangeOfPalettes", COL_CARGO_DIRECTIONS}) {

        String lbl;
        String xpr;
        BeeColumn col = Data.getColumn(TBL_ORDER_CARGO, fld);

        if (col != null) {
          lbl = Localized.getLabel(col);
        } else {
          lbl = Localized.maybeTranslate("=" + fld.toLowerCase());
        }
        switch (fld) {
          case "Volume":
          case "Quantity":
            xpr = lbl + ": {" + fld + "}{" + fld + "UnitName}";
            break;

          case "Loading":
          case "Unloading":
            xpr = lbl + ": {" + fld + "Date} {" + fld + "Contact} {" + fld + "Company} {"
                + fld + "Address} {" + fld + "PostIndex} {" + fld + "CityName} {"
                + fld + "CountryName} {" + fld + "Number} ";
            break;

          default:
            xpr = lbl + ": {" + fld + "}";
            break;
        }
        fields.put(xpr, lbl);
      }
      Global.getParameter(PRM_MESSAGE_TEMPLATE, new Consumer<String>() {
        @Override
        public void accept(String parameter) {
          String[] flds = Codec.beeDeserializeCollection(parameter);

          if (flds == null) {
            flds = new String[0];
          }
          setTemplate(Lists.newArrayList(flds));
        }
      });
    }

    @Override
    public void onClick(ClickEvent arg) {
      Global.inputCollection(Localized.getConstants().template(),
          Localized.getConstants().content(), false, template, new Consumer<Collection<String>>() {
            @Override
            public void accept(Collection<String> input) {
              Global.setParameter(PRM_MESSAGE_TEMPLATE, Codec.beeSerialize(input));
              setTemplate(input);
            }
          }, new Function<String, Editor>() {
            @Override
            public Editor apply(String value) {
              final Editor editor = new InputText();

              if (!BeeUtils.isEmpty(value)) {
                editor.setValue(value);
              } else {
                final ListBox box = new ListBox();

                for (Entry<String, String> entry : fields.entrySet()) {
                  box.addItem(entry.getValue(), entry.getKey());
                }
                box.setAllVisible();
                box.setWidth("100%");

                Global.inputWidget(Localized.getConstants().value(), box, new InputCallback() {
                  @Override
                  public void onSuccess() {
                    editor.setValue(box.getValue());
                  }
                });
              }
              return editor;
            }
          });
    }

    public void setCargoInfo(BeeRowSet cargo, Set<Long> selected) {
      this.cargoInfo = cargo;
      this.selectedCargos = selected;
      renderMessage();
    }

    public void setDrivers(Map<String, String> drivers) {
      driverPanel.clear();
      int c = 0;

      for (Entry<String, String> driver : drivers.entrySet()) {
        InputBoolean check = new InputBoolean(driver.getKey());
        check.setChecked(true);
        driverPanel.setWidget(c, 0, check);

        InputText phone = new InputText();
        phone.setValue(driver.getValue());
        driverPanel.setWidget(c, 1, phone);
        c++;
      }
      if (!isShowing()) {
        center();
      }
    }

    public void setTemplate(Collection<String> template) {
      this.template = template;
      renderMessage();
    }

    private void renderMessage() {
      StringBuilder sb = new StringBuilder();

      if (!BeeUtils.isEmpty(template) && !DataUtils.isEmpty(cargoInfo)) {
        RegExp r = RegExp.compile("\\{\\w+\\}", "g");

        for (int i = 0; i < cargoInfo.getNumberOfRows(); i++) {
          if (sb.length() > 0) {
            sb.append("\n");
          }
          for (String rowExpr : template) {
            SplitResult s = r.split(rowExpr);

            for (int j = 0; j < s.length(); j++) {
              sb.append(s.get(j));

              MatchResult m = r.exec(rowExpr);

              if (m != null) {
                String chunk = m.getGroup(0);
                String value = null;
                int idx = cargoInfo.getColumnIndex(chunk.substring(1, chunk.length() - 1));

                if (idx != BeeConst.UNDEF) {
                  switch (cargoInfo.getColumn(idx).getType()) {
                    case BOOLEAN:
                      value = BeeUtils.unbox(cargoInfo.getBoolean(i, idx))
                          ? Localized.getConstants().yes() : Localized.getConstants().no();
                      break;

                    case DATE:
                      JustDate date = cargoInfo.getDate(i, idx);

                      if (date != null) {
                        value = date.toString();
                      }
                      break;

                    case DATE_TIME:
                      DateTime dateTime = cargoInfo.getDateTime(i, idx);

                      if (dateTime != null) {
                        value = dateTime.toCompactString();
                      }
                      break;

                    default:
                      value = cargoInfo.getString(i, idx);
                      break;
                  }
                } else {
                  value = chunk;
                }
                sb.append(BeeUtils.nvl(value, ""));
              }
            }
            sb.append("\n");
          }
        }
      }
      messagePanel.setValue(sb.toString());
      updateCharacterCount();
    }

    private void sendMessage() {
      List<String> errors = new ArrayList<>();
      Set<String> phones = new HashSet<>();

      for (int i = 0; i < driverPanel.getRowCount(); i++) {
        if (((InputBoolean) driverPanel.getWidget(i, 0)).isChecked()) {
          String phone = ((InputText) driverPanel.getWidget(i, 1)).getValue();

          if (!BeeUtils.isEmpty(phone)) {
            phones.add(phone);
          }
        }
      }
      if (BeeUtils.isEmpty(phones)) {
        errors.add(Localized.getConstants().mobile());
      }
      final String msg = messagePanel.getValue();

      if (BeeUtils.isEmpty(msg)) {
        errors.add(Localized.getConstants().message());
      }
      if (!BeeUtils.isEmpty(errors)) {
        Global.showError(Localized.getConstants().noData(), errors);
        return;
      }
      ParameterList args = TransportHandler.createArgs(SVC_SEND_MESSAGE);
      args.addDataItem(COL_MOBILE, Codec.beeSerialize(phones));
      args.addDataItem(COL_DESCRIPTION, msg);

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getGridView());

          if (response.hasErrors()) {
            return;
          }
          if (!BeeUtils.isEmpty(selectedCargos)) {
            Queries.update(getViewName(), Filter.idIn(selectedCargos), COL_CARGO_MESSAGE,
                Value.getValue(msg), new IntCallback() {
                  @Override
                  public void onSuccess(Integer result) {
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), getViewName());
                  }
                });
          }
        }
      });
      close();
    }

    private void updateCharacterCount() {
      characterCount.setText(BeeUtils.toString(messagePanel.getValue().length()));
    }
  }

  private final FormView tripForm;

  public TripCargoGrid(FormView tripForm) {
    this.tripForm = tripForm;
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    Action action = new Action(presenter.getGridView());
    action.dialog.focusOnOpen(action.dialog.getContent());

    CellGrid grd = presenter.getGridView().getGrid();
    action.dialog.showAt(grd.getAbsoluteLeft(), grd.getAbsoluteTop());

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TripCargoGrid(tripForm);
  }

  @Override
  public void onClick(ClickEvent arg) {
    Set<Long> cargos = new HashSet<>();
    final Set<Long> selected = new HashSet<>();

    for (RowInfo rowInfo : getGridView().getSelectedRows(SelectedRows.ALL)) {
      selected.add(rowInfo.getId());
    }
    for (IsRow row : getGridView().getRowData()) {
      if (BeeUtils.isEmpty(selected) || selected.contains(row.getId())) {
        cargos.add(row.getLong(getGridView().getDataIndex(COL_CARGO)));
      }
    }
    final MessageDialog dialog = new MessageDialog();

    Set<Long> driverIds = new HashSet<>();
    ChildGrid drv = (ChildGrid) tripForm.getWidgetByName(TBL_TRIP_DRIVERS);

    if (drv != null) {
      for (IsRow widget : drv.getPresenter().getGridView().getRowData()) {
        driverIds.add(Data.getLong(TBL_TRIP_DRIVERS, widget, COL_DRIVER));
      }
    }
    if (!BeeUtils.isEmpty(driverIds)) {
      Queries.getRowSet(TBL_DRIVERS, Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, COL_MOBILE),
          Filter.idIn(driverIds), new RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet result) {
              Map<String, String> drivers = new LinkedHashMap<>();

              for (int i = 0; i < result.getNumberOfRows(); i++) {
                drivers.put(BeeUtils.joinWords(result.getString(i, COL_FIRST_NAME),
                    result.getString(i, COL_LAST_NAME)), result.getString(i, COL_MOBILE));
              }
              dialog.setDrivers(drivers);
            }
          });
    } else {
      dialog.setDrivers(ImmutableMap.of(Localized.getConstants().mobile(), ""));
    }
    if (!BeeUtils.isEmpty(cargos)) {
      if (BeeUtils.isEmpty(selected)) {
        for (IsRow row : getGridView().getRowData()) {
          selected.add(row.getId());
        }
      }
      Queries.getRowSet(TBL_ORDER_CARGO, null, Filter.idIn(cargos), new RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          dialog.setCargoInfo(result, selected);
        }
      });
    } else {
      dialog.setCargoInfo(Data.createRowSet(TBL_ORDER_CARGO), null);
    }
  }

  @Override
  public void onLoad(GridView gridView) {
    if (BeeUtils.same(tripForm.getFormName(), FORM_TRIP)) {
      HeaderView hdr = gridView.getViewPresenter().getHeader();
      hdr.addCommandItem(new Button(Localized.getConstants().message(), this));
    }
  }
}