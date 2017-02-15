package com.butent.bee.server.modules.transport;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEditorBean;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.rest.CrudWorker;
import com.butent.bee.server.rest.RestResponse;
import com.butent.bee.server.rest.annotations.Trusted;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.FertileElement;
import com.butent.bee.shared.html.builder.elements.Form;
import com.butent.bee.shared.html.builder.elements.Input;
import com.butent.bee.shared.i18n.DateOrdering;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.websocket.messages.ModificationMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("transport")
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class ShipmentRequestsWorker {

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  DataEditorBean deb;
  @EJB
  UserServiceBean usr;

  @GET
  @Path("confirm/{id:\\d+}")
  @Trusted
  public String confirm(@PathParam("id") Long requestId, @QueryParam("choice") String choice) {
    FertileElement el = null;

    SimpleRowSet.SimpleRow row = qs.getRow(new SqlSelect()
        .addField(TBL_SHIPMENT_REQUESTS, sys.getVersionName(TBL_SHIPMENT_REQUESTS), "version")
        .addFields(TBL_SHIPMENT_REQUESTS, COL_QUERY_STATUS)
        .addFrom(TBL_SHIPMENT_REQUESTS)
        .setWhere(sys.idEquals(TBL_SHIPMENT_REQUESTS, requestId)));

    Integer currentStatus = row.getInt(COL_QUERY_STATUS);

    if (ShipmentRequestStatus.CONTRACT_SENT.is(currentStatus)) {
      ShipmentRequestStatus status = EnumUtils.getEnumByName(ShipmentRequestStatus.class, choice);

      if (Objects.nonNull(status)) {
        switch (status) {
          case APPROVED:
          case REJECTED:
            BeeRowSet rs = DataUtils.getUpdated(VIEW_SHIPMENT_REQUESTS, requestId,
                row.getLong("version"), DataUtils.getColumn(COL_QUERY_STATUS,
                    sys.getView(VIEW_SHIPMENT_REQUESTS).getRowSetColumns()),
                BeeUtils.toString(currentStatus), BeeUtils.toString(status.ordinal()));

            deb.commitRow(rs, RowInfo.class);

            DataChangeEvent.fireRefresh(
                (event, locality) -> Endpoint.sendToAll(new ModificationMessage(event)),
                VIEW_SHIPMENT_REQUESTS);

            el = div().text(status.getCaption());
            break;
          default:
            break;
        }
      }
      if (Objects.isNull(el)) {
        el = div().text(Localized.dictionary().crmTaskConfirm());

        for (ShipmentRequestStatus s : Arrays.asList(ShipmentRequestStatus.APPROVED,
            ShipmentRequestStatus.REJECTED)) {
          Form form = form()
              .methodGet()
              .append(input().type(Input.Type.HIDDEN).name("choice").value(s.name()))
              .append(input().type(Input.Type.SUBMIT).value(s.getCaption()));

          el.appendChild(form);
        }
      }
    } else {
      el = div().text(BeeUtils.join(":", Localized.dictionary().status(),
          EnumUtils.getLocalizedCaption(ShipmentRequestStatus.class, currentStatus,
              Localized.dictionary())));
    }
    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8());
    doc.getBody().append(el);

    return doc.buildLines();
  }

  @POST
  @Path("request")
  @Produces(RestResponse.JSON_TYPE)
  @Trusted(secret = "B-NOVO Shipment Request")
  public RestResponse request(JsonObject data) {
    if (!usr.validateHost(CrudWorker.getValue(data, COL_QUERY_HOST))) {
      return RestResponse.error(Localized.dictionary().ipBlocked());
    }
    LogUtils.getRootLogger().debug(data);

    try {
      BeeView view = sys.getView(VIEW_SHIPMENT_REQUESTS);
      ResponseObject response = deb.commitRow(buildRowSet(view, data));

      if (response.hasErrors()) {
        return RestResponse.error(response.getErrors());
      }
      String cargo = DataUtils.getString(view.getRowSetColumns(), (IsRow) response.getResponse(),
          COL_CARGO);

      for (String tag : Arrays.asList(TBL_CARGO_LOADING, TBL_CARGO_UNLOADING, VIEW_CARGO_FILES)) {
        List<JsonObject> list = new ArrayList<>();
        JsonArray children = data.getJsonArray(tag);

        if (Objects.nonNull(children)) {
          list.addAll(children.getValuesAs(JsonObject.class));
        }
        view = sys.getView(tag);

        for (JsonObject json : list) {
          BeeRowSet rs = buildRowSet(view, json);
          rs.getColumns().add(view.getBeeColumn(COL_CARGO));
          rs.getRow(0).getValues().add(cargo);
          deb.commitRow(rs);
        }
      }
    } catch (BeeException e) {
      return RestResponse.error(e);
    }
    DataChangeEvent.fireRefresh(
        (event, locality) -> Endpoint.sendToAll(new ModificationMessage(event)),
        VIEW_SHIPMENT_REQUESTS);

    TextConstant constant = TextConstant.SUMBMITTED_REQUEST_CONTENT;

    BeeRowSet rowSet =
        qs.getViewData(VIEW_TEXT_CONSTANTS, Filter.equals(COL_TEXT_CONSTANT, constant));

    String localizedContent = Localized.column(COL_TEXT_CONTENT,
        CrudWorker.getValue(data, COL_USER_LOCALE));

    String text;

    if (DataUtils.isEmpty(rowSet)) {
      text = constant.getDefaultContent();
    } else if (BeeConst.isUndef(DataUtils.getColumnIndex(localizedContent, rowSet.getColumns()))) {
      text = rowSet.getString(0, COL_TEXT_CONTENT);
    } else {
      text = BeeUtils.notEmpty(rowSet.getString(0, localizedContent),
          rowSet.getString(0, COL_TEXT_CONTENT));
    }

    return RestResponse.ok(text);
  }

  private BeeRowSet buildRowSet(BeeView view, JsonObject json) throws BeeException {
    Map<String, Pair<String, String>> relations = new HashMap<>();

    for (String name : new String[] {COL_CARGO_QUANTITY, COL_CARGO_WEIGHT, COL_CARGO_VOLUME}) {
      relations.put(name + COL_UNIT, Pair.of(TBL_UNITS, COL_UNIT_NAME));
    }
    relations.put(COL_EXPEDITION, Pair.of(TBL_EXPEDITION_TYPES, COL_EXPEDITION_TYPE_NAME));
    relations.put(COL_SHIPPING_TERM, Pair.of(TBL_SHIPPING_TERMS, COL_SHIPPING_TERM_NAME));
    relations.put(COL_CARGO_VALUE_CURRENCY, Pair.of(TBL_CURRENCIES, COL_CURRENCY_NAME));

    List<BeeColumn> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();

    for (String col : json.keySet()) {
      if (view.hasColumn(col)) {
        BeeColumn column = view.getBeeColumn(col);
        String value = CrudWorker.getValue(json, col);

        if (BeeUtils.isEmpty(value) || column.isReadOnly() || column.isForeign()) {
          continue;
        }
        Object val = null;

        if (Objects.equals(col, COL_USER_LOCALE)) {
          val = SupportedLocale.getByLanguage(SupportedLocale.normalizeLanguage(value)).ordinal();

        } else if (relations.containsKey(col)) {
          String tbl = relations.get(col).getA();
          String fld = relations.get(col).getB();

          val = qs.getLong(new SqlSelect()
              .addFields(tbl, sys.getIdName(tbl))
              .addFrom(tbl)
              .setWhere(SqlUtils.same(tbl, fld, value)));

          if (Objects.isNull(val)) {
            throw new BeeException(BeeUtils.joinWords(col, value));
          }
        } else {
          switch (column.getType()) {
            case BOOLEAN:
              val = BeeUtils.toBoolean(value);
              break;
            case DATE:
              JustDate date = TimeUtils.parseDate(value, DateOrdering.YMD);

              if (Objects.nonNull(date)) {
                val = date.serialize();
              }
              break;
            case DATE_TIME:
              DateTime datetime = TimeUtils.parseDateTime(value, DateOrdering.YMD);

              if (Objects.nonNull(datetime)) {
                val = datetime.serialize();
              }
              break;
            case DECIMAL:
            case NUMBER:
            case INTEGER:
            case LONG:
              val = BeeUtils.toDecimalOrNull(value);
              break;
            case TEXT:
              val = column.getPrecision() > 0 ? BeeUtils.left(value, column.getPrecision()) : value;
              break;
            default:
              val = value;
              break;
          }
        }
        if (Objects.nonNull(val)) {
          columns.add(column);
          values.add(val.toString());
        }
      }
    }
    return DataUtils.createRowSetForInsert(view.getName(), columns, values);
  }
}
