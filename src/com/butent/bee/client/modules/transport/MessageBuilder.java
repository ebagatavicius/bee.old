package com.butent.bee.client.modules.transport;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.dom.client.Style;
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
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputBoolean;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class MessageBuilder extends FaLabel implements ClickHandler {

  private final class MessageDialog extends DialogBox implements ClickHandler {

    private static final String prfx = TBL_CARGO_PLACES;

    private final HtmlTable driverPanel = new HtmlTable();
    private final Label characterCount = new Label();
    private final InputArea messagePanel = new InputArea();
    private final Map<String, Collection<String>> templates = new LinkedHashMap<>();

    private BeeRowSet tripInfo;
    private BeeRowSet cargoInfo;
    private SimpleRowSet placesInfo;

    private final Map<Long, Long> cargoTripIds = new HashMap<>();
    private final TabBar tabs = new TabBar(Orientation.HORIZONTAL);

    private MessageDialog(BeeRowSet trip, Set<Long> driverIds, Map<Long, Long> ids) {
      super(Localized.dictionary().message());

      setAnimationEnabled(true);
      setHideOnEscape(true);
      addDefaultCloseBox();

      Flow panel = new Flow();
      panel.add(driverPanel);

      Horizontal hor = new Horizontal();
      hor.add(new Button(Localized.dictionary().template(), this));
      hor.add(tabs);
      tabs.addSelectionHandler(selectionEvent -> renderMessage());
      panel.add(hor);

      characterCount.getElement().getStyle().setTextAlign(Style.TextAlign.RIGHT);
      panel.add(characterCount);

      messagePanel.setVisibleLines(15);
      messagePanel.setWidth("100%");
      messagePanel.addInputHandler(event -> updateCharacterCount());
      panel.add(messagePanel);

      SimplePanel simple = new SimplePanel(new Button(Localized.dictionary().send(),
          handler -> sendMessage()));
      simple.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
      panel.add(simple);

      setWidget(panel);

      Global.getParameterMap(PRM_MESSAGE_TEMPLATE).forEach((key, value) -> {
        String[] items;
        try {
          items = Codec.beeDeserializeCollection(value);
        } catch (Exception e) {
          items = null;
        }
        if (!ArrayUtils.isEmpty(items)) {
          templates.put(key, Arrays.asList(items));
        }
      });
      renderTemplates(null, false);

      Queries.getRowSet(TBL_DRIVERS,
          Lists.newArrayList(COL_FIRST_NAME, COL_LAST_NAME, COL_MOBILE),
          BeeUtils.isEmpty(driverIds) ? Filter.isFalse() : Filter.idIn(driverIds),
          new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet result) {
              Map<String, String> drivers = new HashMap<>();

              for (int i = 0; i < result.getNumberOfRows(); i++) {
                drivers.put(BeeUtils.joinWords(result.getString(i, COL_FIRST_NAME),
                    result.getString(i, COL_LAST_NAME)), result.getString(i, COL_MOBILE));
              }
              setDrivers(drivers);
            }
          });
      tripInfo = trip;
      cargoTripIds.putAll(ids);
      setCargoInfo(Data.createRowSet(TBL_ORDER_CARGO), null);

      Queries.getRowSet(VIEW_ORDER_CARGO, null, Filter.idIn(cargoTripIds.keySet()),
          new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet cargo) {
              ParameterList args = TransportHandler.createArgs(SVC_GET_CARGO_PLACES);
              args.addDataItem(COL_CARGO_TRIP, DataUtils.buildIdList(cargoTripIds.values()));

              BeeKeeper.getRpc().makeRequest(args, new ResponseCallback() {
                @Override
                public void onResponse(ResponseObject response) {
                  response.notify(gridView);
                  SimpleRowSet places = null;

                  if (!response.hasErrors()) {
                    Multimap<Long, Integer> indexes = LinkedListMultimap.create();
                    places = SimpleRowSet.restore(response.getResponseAsString());

                    for (int i = 0; i < places.getNumberOfRows(); i++) {
                      indexes.put(places.getLong(i, COL_CARGO), i);
                    }
                    for (BeeRow row : cargo) {
                      row.setProperty(prfx, Codec.beeSerialize(indexes.get(row.getId())));
                    }
                  }
                  setCargoInfo(cargo, places);
                }
              });
            }
          });
    }

    @Override
    public void onClick(ClickEvent arg) {
      String caption;
      Collection<String> template;
      Dictionary loc = Localized.dictionary();

      if (BeeUtils.isPositive(tabs.getItemCount())) {
        caption = ((Label) tabs.getSelectedWidget()).getText();
        template = templates.get(caption);
      } else {
        caption = null;
        template = null;
      }
      Global.inputCollection(loc.template(), caption, false, template, input -> {
        if (BeeUtils.isEmpty(input)) {
          templates.remove(caption);
          renderTemplates(null, true);
        } else {
          Global.inputString(loc.name(), null, new StringCallback() {
            @Override
            public void onSuccess(String name) {
              templates.put(name, input);
              renderTemplates(name, true);
            }
          }, null, caption);
        }
      }, value -> {
        InputText editor = new InputText();

        if (!BeeUtils.isEmpty(value)) {
          editor.setValue(value);
        } else {
          List<String> caps = new ArrayList<>();
          List<String> keys = new ArrayList<>();

          Stream.of(cargoInfo, tripInfo)
              .forEach(rowSet -> rowSet.getColumns().stream()
                  .filter(col -> !Objects.equals(col.getType(), ValueType.LONG))
                  .forEach(col -> {
                    caps.add(Localized.getLabel(col));
                    keys.add(col.getId() + (Objects.equals(col.getType(), ValueType.BOOLEAN)
                        ? "+" + loc.yes() + "-" + loc.no() : ""));
                  }));
          caps.add(loc.cargoHandlingPlaces() + " " + loc.unloading());
          keys.add(prfx + VAR_UNLOADING + "+" + loc.unloading() + "-" + loc.loading());

          Data.getColumns(TBL_CARGO_LOADING).stream()
              .filter(col -> col.getType() != ValueType.LONG)
              .filter(col -> !BeeUtils.isSuffix(col.getId(), VAR_UNBOUND))
              .forEach(col -> {
                caps.add(loc.cargoHandlingPlaces() + " " + Localized.getLabel(col));
                keys.add(prfx + col.getId());
              });
          Global.choice(loc.value(), null, caps,
              item -> editor.setValue(caps.get(item) + ": {" + keys.get(item) + "}"));
        }
        return editor;
      });
    }

    private void renderMessage() {
      StringBuilder sb = new StringBuilder();

      if (!BeeConst.isUndef(tabs.getSelectedTab()) && !DataUtils.isEmpty(cargoInfo)) {
        RegExp regExp = RegExp.compile("\\{\\w+[^\\}]*\\}", "g");
        RegExp subRegExp = RegExp.compile("\\{(\\w+)(\\+([^\\-]*))?(\\-(.*))?\\}");

        for (BeeRow cargoRow : cargoInfo) {
          if (sb.length() > 0) {
            sb.append("\n");
          }
          for (String rowExpr : templates.get(((Label) tabs.getSelectedWidget()).getText())) {
            boolean hasPlaces = false;
            StringBuilder row = new StringBuilder();
            SplitResult split = regExp.split(rowExpr);

            for (int i = 0; i < split.length(); i++) {
              row.append(split.get(i));
              MatchResult match = regExp.exec(rowExpr);

              if (match != null) {
                String chunk = match.getGroup(0);
                MatchResult subMatch = subRegExp.exec(chunk);
                String col = subMatch.getGroup(1);
                String value;
                int idx = DataUtils.getColumnIndex(col, cargoInfo.getColumns());

                if (!BeeConst.isUndef(idx)) {
                  value = parseValue(cargoInfo.getColumn(idx).getType(), cargoRow, idx,
                      subMatch.getGroup(3), subMatch.getGroup(5));
                } else {
                  idx = DataUtils.getColumnIndex(col, tripInfo.getColumns());

                  if (!BeeConst.isUndef(idx)) {
                    value = parseValue(tripInfo.getColumn(idx).getType(), tripInfo.getRow(0), idx,
                        subMatch.getGroup(3), subMatch.getGroup(5));
                  } else {
                    hasPlaces = hasPlaces || BeeUtils.isPrefix(col, prfx);
                    value = chunk;
                  }
                }
                row.append(BeeUtils.nvl(value, ""));
              }
            }
            if (hasPlaces) {
              String[] indexes = Codec.beeDeserializeCollection(cargoRow.getProperty(prfx));

              if (!ArrayUtils.isEmpty(indexes)) {
                String expr = row.toString();
                row = new StringBuilder();
                SplitResult subSplit = regExp.split(expr);
                int x = 1;

                for (String idx : indexes) {
                  if (row.length() > 0) {
                    row.append("\n");
                  }
                  for (int i = 0; i < subSplit.length(); i++) {
                    row.append(subSplit.get(i));
                    MatchResult match = regExp.exec(expr);

                    if (match != null) {
                      String chunk = match.getGroup(0);
                      MatchResult subMatch = subRegExp.exec(chunk);
                      String col = subMatch.getGroup(1);
                      String value;

                      if (BeeUtils.isPrefix(col, prfx)) {
                        col = BeeUtils.removePrefix(col, prfx);
                        value = placesInfo.getValue(BeeUtils.toInt(idx), col);

                        switch (col) {
                          case VAR_UNLOADING:
                            value = parseBoolean(BeeUtils.toBooleanOrNull(value),
                                subMatch.getGroup(3), subMatch.getGroup(5));
                            break;

                          case COL_PLACE_ORDINAL:
                            value = BeeUtils.notEmpty(value, BeeUtils.toString(x));
                            break;
                          case COL_PLACE_DATE:
                            value = Format.renderDateTime(TimeUtils.toDateTimeOrNull(value));
                            break;
                        }
                      } else {
                        value = chunk;
                      }
                      row.append(BeeUtils.nvl(value, ""));
                    }
                  }
                  x++;
                }
              }
            }
            sb.append(row).append("\n");
          }
        }
      }
      messagePanel.setValue(sb.toString());
      updateCharacterCount();
    }

    private void renderTemplates(String name, boolean save) {
      tabs.clear();
      int idx = BeeConst.UNDEF;
      int c = 0;

      for (String s : templates.keySet()) {
        if (BeeUtils.same(s, name)) {
          idx = c;
        }
        tabs.addItem(s);
        c++;
      }
      if (BeeConst.isUndef(idx) && BeeUtils.isPositive(c)) {
        idx = 0;
      }
      if (!BeeConst.isUndef(idx)) {
        tabs.selectTab(idx);
      }
      if (save) {
        Global.setParameter(PRM_MESSAGE_TEMPLATE, Codec.beeSerialize(templates), false);
      }
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
        errors.add(Localized.dictionary().mobile());
      }
      final String msg = messagePanel.getValue();

      if (BeeUtils.isEmpty(msg)) {
        errors.add(Localized.dictionary().message());
      }
      if (!BeeUtils.isEmpty(errors)) {
        Global.showError(Localized.dictionary().noData(), errors);
        return;
      }
      ParameterList args = TransportHandler.createArgs(SVC_SEND_MESSAGE);
      args.addDataItem(COL_MOBILE, Codec.beeSerialize(phones));
      args.addDataItem(COL_DESCRIPTION, msg);

      BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(gridView);

          if (response.hasErrors()) {
            return;
          }
          if (!cargoTripIds.isEmpty()) {
            Queries.update(gridView.getViewName(), Filter.idIn(cargoTripIds.values()),
                COL_CARGO_MESSAGE, Value.getValue(msg), new Queries.IntCallback() {
                  @Override
                  public void onSuccess(Integer result) {
                    DataChangeEvent.fireRefresh(BeeKeeper.getBus(), gridView.getViewName());
                  }
                });
          }
        }
      });
      close();
    }

    private void setCargoInfo(BeeRowSet cargo, SimpleRowSet places) {
      cargoInfo = cargo;
      placesInfo = places;
      renderMessage();
    }

    private void setDrivers(Map<String, String> drivers) {
      driverPanel.clear();
      drivers.put(Localized.dictionary().mobile(), null);
      int c = 0;

      for (Map.Entry<String, String> driver : drivers.entrySet()) {
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

    private void updateCharacterCount() {
      characterCount.setText(BeeUtils.toString(messagePanel.getValue().length()));
    }
  }

  private final GridView gridView;

  public MessageBuilder(GridView gridView) {
    super(FontAwesome.WHATSAPP);

    setTitle(Localized.dictionary().message());
    addClickHandler(this);

    this.gridView = Assert.notNull(gridView);
    Assert.state(BeeUtils.same(Data.getViewTable(gridView.getViewName()), TBL_CARGO_TRIPS));
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    Set<Long> ids = new HashSet<>();

    for (RowInfo rowInfo : gridView.getSelectedRows(GridView.SelectedRows.ALL)) {
      ids.add(rowInfo.getId());
    }
    if (BeeUtils.isEmpty(ids) && gridView.getActiveRow() != null) {
      ids.add(gridView.getActiveRow().getId());
    }
    Long tripId = null;
    Map<Long, Long> cargoTripIds = new HashMap<>();

    for (IsRow row : gridView.getRowData()) {
      Long id = row.getId();

      if (ids.contains(id)) {
        if (DataUtils.isId(tripId)
            && !Objects.equals(tripId, row.getLong(gridView.getDataIndex(COL_TRIP)))) {
          tripId = null;
          break;
        }
        tripId = row.getLong(gridView.getDataIndex(COL_TRIP));
        cargoTripIds.put(row.getLong(gridView.getDataIndex(COL_CARGO)), id);
      }
    }
    if (!DataUtils.isId(tripId)) {
      gridView.notifyWarning(Localized.dictionary().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(VIEW_TRIPS, null, Filter.compareId(tripId), new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet trip) {
        Queries.getDistinctLongs(TBL_TRIP_DRIVERS, COL_DRIVER,
            Filter.equals(COL_TRIP, trip.getRow(0).getId()), new RpcCallback<Set<Long>>() {
              @Override
              public void onSuccess(Set<Long> driverIds) {
                new MessageDialog(trip, driverIds, cargoTripIds);
              }
            });
      }
    });
  }

  private static String parseBoolean(Boolean value, String yes, String no) {
    return BeeUtils.unbox(value) ? BeeUtils.notEmpty(yes, "+") : BeeUtils.notEmpty(no, "-");
  }

  private static String parseValue(ValueType type, BeeRow row, int col, String yes, String no) {
    String value = null;

    switch (type) {
      case BOOLEAN:
        value = parseBoolean(row.getBoolean(col), yes, no);
        break;

      case DATE:
        JustDate date = row.getDate(col);

        if (date != null) {
          value = Format.renderDate(date);
        }
        break;

      case DATE_TIME:
        value = Format.renderDateTime(row.getDateTime(col));
        break;

      default:
        value = row.getString(col);
        break;
    }
    return value;
  }
}
