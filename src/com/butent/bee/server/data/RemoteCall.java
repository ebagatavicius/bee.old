// CHECKSTYLE:OFF
package com.butent.bee.server.data;

import com.google.common.base.Splitter;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Enables to get data from the system using SOAP web service protocol.
 */
// @WebService
@SOAPBinding(parameterStyle = ParameterStyle.BARE)
@Stateless
public class RemoteCall {

  /**
   * Contains column information - name and value.
   */
  @XmlRootElement(name = "col")
  public static class DataColumn {
    @XmlAttribute
    public String name;
    @XmlValue
    public String value;
  }

  /**
   * Contains a list of columns for a row.
   */
  @XmlRootElement(name = "row")
  public static class DataRow {
    @XmlAttribute
    public String id;
    @XmlAttribute
    public String version;
    @XmlElementRef
    public List<DataColumn> columns;

    public void addColumn(String name, String value) {
      if (columns == null) {
        columns = new ArrayList<>();
      }
      DataColumn col = new DataColumn();
      col.name = name;
      col.value = value;
      columns.add(col);
    }
  }

  /**
   * Holds SOAP web service response data.
   */
  @XmlRootElement(name = "data")
  public static class DataHolder {
    @XmlAttribute
    public String error;
    @XmlAttribute
    public Integer affected;
    @XmlAttribute
    public List<String> columns;
    @XmlElementRef
    @XmlElementWrapper
    public List<DataRow> rows;
  }

  /**
   * Holds such parameters for SOAP web service requests as view, columns, filter or orderBy.
   */
  @XmlRootElement(name = "params")
  public static class ParamHolder {
    @XmlAttribute
    public String view;
    @XmlAttribute
    public String columns;
    @XmlElement
    public String filter;
    @XmlAttribute
    public String order;
    @XmlAttribute
    public Integer limit;
    @XmlAttribute
    public Integer offset;
    @XmlAttribute
    public boolean showId;
    @XmlAttribute
    public boolean showVersion;
  }

  @EJB
  QueryServiceBean qs;
  @EJB
  SystemBean sys;
  @EJB
  UserServiceBean usr;

  public String getViewData(String params) {
    Object rs = buildRequest(ParamHolder.class, params);
    ParamHolder prm = null;
    boolean skipColumns = false;

    if (rs instanceof ParamHolder) {
      prm = (ParamHolder) rs;

      if (!sys.isView(prm.view)) {
        rs = "Not a view: " + prm.view;

      } else {
        rs = null;
        Filter filter = null;
        Order order = null;
        List<String> columns = new ArrayList<>();
        BeeView view = sys.getView(prm.view);

        if (!BeeUtils.isEmpty(prm.columns)) {
          for (String col : Splitter
              .on(BeeConst.CHAR_COMMA)
              .omitEmptyStrings()
              .trimResults()
              .split(prm.columns)) {

            if (col.equals(BeeConst.CHAR_MINUS)) {
              skipColumns = true;
              break;
            } else if (!view.hasColumn(col)) {
              rs = "Wrong column: " + col;
              break;
            } else {
              columns.add(col);
            }
          }
        }
        if (rs == null && !BeeUtils.isEmpty(prm.filter)) {
          filter = view.parseFilter(prm.filter, usr.getCurrentUserId());
          if (filter == null) {
            rs = "Wrong filter: " + prm.filter;
          }
        }
        if (rs == null && !BeeUtils.isEmpty(prm.order)) {
          order = view.parseOrder(prm.order);
          if (order == null) {
            rs = "Wrong order: " + prm.order;
          }
        }
        if (rs == null) {
          rs = qs.getViewData(prm.view, filter, order,
              BeeUtils.toNonNegativeInt(prm.limit), BeeUtils.toNonNegativeInt(prm.offset), columns);
        }
      }
    }
    DataHolder data = new DataHolder();

    if (rs instanceof BeeRowSet) {
      BeeRowSet rowSet = (BeeRowSet) rs;
      data.affected = rowSet.getNumberOfRows();

      data.columns = new ArrayList<>();
      for (BeeColumn column : rowSet.getColumns()) {
        data.columns.add(column.getLabel());
      }

      data.rows = new ArrayList<>();

      if (prm.showId || prm.showVersion || !skipColumns) {
        for (BeeRow r : rowSet.getRows()) {
          DataRow row = new DataRow();

          if (prm.showId) {
            row.id = BeeUtils.toString(r.getId());
          }
          if (prm.showVersion) {
            row.version = new DateTime(r.getVersion()).toString();
          }
          if (!skipColumns) {
            for (String col : data.columns) {
              row.addColumn(col, r.getString(rowSet.getColumnIndex(col)));
            }
          }
          data.rows.add(row);
        }
      }
    } else {
      data.error = (rs == null) ? BeeConst.NULL : rs.toString();
    }
    return buildResponse(data);
  }

  private static <T> Object buildRequest(Class<T> clazz, String params) {
    Object ret = null;

    try {
      Unmarshaller unmarshaller = JAXBContext.newInstance(clazz).createUnmarshaller();
      ret = unmarshaller.unmarshal(new StringReader(params));

    } catch (JAXBException e) {
      String err;

      if (e.getLinkedException() != null) {
        err = e.getLinkedException().getMessage();
      } else {
        err = e.getMessage();
      }
      ret = err;
    } catch (Exception e) {
      ret = e.getMessage();
    }
    return ret;
  }

  private static <T> String buildResponse(T data) {
    String ret;

    try {
      JAXBContext c = JAXBContext.newInstance(data.getClass());
      StringWriter writer = new StringWriter();
      Marshaller xx = c.createMarshaller();
      xx.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      xx.marshal(data, writer);
      ret = writer.toString();

    } catch (JAXBException e) {
      String err;

      if (e.getLinkedException() != null) {
        err = e.getLinkedException().getMessage();
      } else {
        err = e.getMessage();
      }
      ret = err;
    } catch (Exception e) {
      ret = e.getMessage();
    }
    return ret;
  }
}
