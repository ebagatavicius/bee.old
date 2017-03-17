package com.butent.bee.server.rest;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.TBL_COMPANIES;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_COMPANY_NAME;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.tasks.TaskConstants.COL_PUBLISHER;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.shared.i18n.PredefinedFormat;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("service")
public class ServiceCommentWorker {

  private static BeeLogger logger = LogUtils.getLogger(ServiceCommentWorker.class);

  @EJB
  ParamHolderBean prm;
  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;

  @GET
  @Path("comments")
  @Produces(MediaType.APPLICATION_JSON)
  public String getServiceComments(@QueryParam("id") Long serviceId) {
    JsonObjectBuilder json = Json.createObjectBuilder();

    if (!DataUtils.isId(serviceId)) {
      json.add("Bad request", "service id required");
      return json.build().toString();
    }

    try {
      SimpleRow serviceRow = qs.getRow(new SqlSelect()
          .addFields(TBL_SERVICE_MAINTENANCE, sys.getIdName(TBL_SERVICE_MAINTENANCE),
              COL_MAINTENANCE_DESCRIPTION, COL_EQUIPMENT)
          .addFrom(TBL_SERVICE_MAINTENANCE)
          .addFields(TBL_SERVICE_OBJECTS, COL_MODEL, COL_SERIAL_NO)
          .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_MANUFACTURER_NAME)
          .addField(VIEW_MAINTENANCE_STATES, COL_STATE_NAME, COL_STATE)
          .addFromLeft(TBL_SERVICE_OBJECTS,
              sys.joinTables(TBL_SERVICE_OBJECTS, TBL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT))
          .addFromLeft(TBL_COMPANIES,
              sys.joinTables(TBL_COMPANIES, TBL_SERVICE_OBJECTS, COL_MANUFACTURER))
          .addFromLeft(VIEW_MAINTENANCE_STATES,
              sys.joinTables(VIEW_MAINTENANCE_STATES, TBL_SERVICE_MAINTENANCE, COL_STATE))
          .setWhere(sys.idEquals(TBL_SERVICE_MAINTENANCE, serviceId))
          );

      if (serviceRow == null) {
        json.add("Bad request", "service not found");
        return json.build().toString();
      }

      JsonObjectBuilder serviceJson = Json.createObjectBuilder();

      Stream.of(sys.getIdName(TBL_SERVICE_MAINTENANCE), COL_MAINTENANCE_DESCRIPTION, COL_STATE)
          .forEach(column -> {
        String value = serviceRow.getValue(column);

        if (!BeeUtils.isEmpty(value)) {
          serviceJson.add(column, value);
        }
      });
      List<Long> equipmentIds = DataUtils.parseIdList(serviceRow.getValue(COL_EQUIPMENT));

      if (!BeeUtils.isEmpty(equipmentIds)) {
        Set<String> equipmentValues = qs.getValueSet(new SqlSelect()
            .addFields(TBL_EQUIPMENT, COL_EQUIPMENT_NAME)
            .addFrom(TBL_EQUIPMENT)
            .setWhere(sys.idInList(TBL_EQUIPMENT, equipmentIds)));

        if (!equipmentValues.isEmpty()) {
          serviceJson.add(COL_EQUIPMENT, BeeUtils.joinItems(equipmentValues));
        }
      }

      JsonObjectBuilder objectJson = Json.createObjectBuilder();

      Stream.of(COL_MODEL, ALS_MANUFACTURER_NAME, COL_SERIAL_NO).forEach(column -> {
        String value = serviceRow.getValue(column);

        if (!BeeUtils.isEmpty(value)) {
          objectJson.add(column, value);
        }
      });
      serviceJson.add(COL_SERVICE_OBJECT, objectJson);

      SimpleRowSet commentsRowSet = qs.getData(new SqlSelect()
          .addFields(TBL_MAINTENANCE_COMMENTS, COL_PUBLISH_TIME, COL_COMMENT, COL_TERM,
              COL_PUBLISHER)
          .addField(TBL_MAINTENANCE_COMMENTS, COL_EVENT_NOTE, COL_STATE)
          .addFrom(TBL_MAINTENANCE_COMMENTS)
          .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_MAINTENANCE_COMMENTS, COL_SHOW_CUSTOMER),
              SqlUtils.equals(TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE, serviceId)))
          .addOrder(TBL_MAINTENANCE_COMMENTS, sys.getIdName(TBL_MAINTENANCE_COMMENTS
          )));

      if (!commentsRowSet.isEmpty()) {
        DateTimeFormat dateFormatter = DateTimeFormat.of(PredefinedFormat.DATE_SHORT,
            SupportedLocale.USER_DEFAULT.getDateTimeFormatInfo());
        JsonArrayBuilder commentsJsonArray = Json.createArrayBuilder();
        commentsRowSet.forEach(commentRow -> {
          JsonObjectBuilder commentJson = Json.createObjectBuilder();

          Stream.of(COL_PUBLISH_TIME, COL_STATE, COL_COMMENT, COL_TERM).forEach(column -> {
            String value = commentRow.getValue(column);

            if (!BeeUtils.isEmpty(value)) {
              commentJson.add(column, BeeUtils.inListSame(column, COL_PUBLISH_TIME, COL_TERM)
                  ? dateFormatter.format(commentRow.getDateTime(column)) : value);
            }
          });
          String roleParamValue = qs.getValue(new SqlSelect()
              .addFields(TBL_USER_PARAMETERS, COL_PARAMETER_VALUE)
              .addFrom(TBL_USER_PARAMETERS)
              .addFromLeft(TBL_PARAMETERS,
                  sys.joinTables(TBL_PARAMETERS, TBL_USER_PARAMETERS, COL_PARAMETER))
              .setWhere(SqlUtils.and(SqlUtils.equals(TBL_USER_PARAMETERS, COL_USER,
                  commentRow.getValue(COL_PUBLISHER)),
                  SqlUtils.equals(TBL_PARAMETERS, COL_PARAMETER_NAME, PRM_ROLE))));

          if (!BeeUtils.isEmpty(roleParamValue)) {
            String roleName = qs.getValue(new SqlSelect()
                .addFields(TBL_ROLES, COL_ROLE_NAME)
                .addFrom(TBL_ROLES)
                .setWhere(sys.idEquals(TBL_ROLES, BeeUtils.toLong(roleParamValue))));
            commentJson.add(COL_ROLE, roleName);

          } else {
            Pair<Long, String> prmRole = prm.getRelationInfo(PRM_ROLE);

            if (prmRole != null && !BeeUtils.isEmpty(prmRole.getB())) {
              commentJson.add(COL_ROLE, prmRole.getB());
            }
          }
          commentsJsonArray.add(commentJson);
        });
        serviceJson.add(TBL_MAINTENANCE_COMMENTS, commentsJsonArray);
      }

      json.add(COL_SERVICE_MAINTENANCE, serviceJson);
      return json.build().toString();

    } catch (Exception e) {
      logger.error(e);
      json.add("Error", e.getLocalizedMessage());
      return json.build().toString();
    }
  }
}
