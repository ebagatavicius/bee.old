package com.butent.bee.server.modules;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent.ViewDeleteEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.QueryServiceBean.ViewDataProvider;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.SqlDelete;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.websocket.Endpoint;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.ColumnValueFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.CustomFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.ParameterType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.websocket.messages.ParameterMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
@Lock(LockType.READ)
public class ParamHolderBean {

  private static BeeLogger logger = LogUtils.getLogger(ParamHolderBean.class);

  @EJB
  ModuleHolderBean moduleBean;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  SystemBean sys;

  private final Table<String, String, BeeParameter> parameters = TreeBasedTable.create();
  private EventBus parameterEventBus;

  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.same(svc, SVC_GET_RELATION_PARAMETER)) {
      response = ResponseObject.response(getRelationInfo(reqInfo.getParameter(COL_PARAMETER)));

    } else if (BeeUtils.same(svc, SVC_SET_PARAMETER)) {
      setParameter(reqInfo.getParameter(COL_PARAMETER), reqInfo.getParameter(COL_PARAMETER_VALUE),
          BeeUtils.toBoolean(reqInfo.getParameter(VAR_PARAMETER_DEFAULT)));
      response = ResponseObject.emptyResponse();
    }
    if (response == null) {
      String msg = BeeUtils.joinWords("Parameters service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }
    return response;
  }

  public Boolean getBoolean(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getBoolean(usr.getCurrentUserId()) : parameter.getBoolean();
  }

  public Collection<String> getCollection(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getCollection(usr.getCurrentUserId()) : parameter.getCollection();
  }

  public JustDate getDate(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getDate(usr.getCurrentUserId()) : parameter.getDate();
  }

  public DateTime getDateTime(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getDateTime(usr.getCurrentUserId()) : parameter.getDateTime();
  }

  public Double getDouble(String name) {
    Number n = getNumber(name);

    if (n != null) {
      return n.doubleValue();
    }
    return null;
  }

  public Integer getInteger(String name) {
    Number n = getNumber(name);

    if (n != null) {
      return n.intValue();
    }
    return null;
  }

  public Long getLong(String name) {
    Number n = getNumber(name);

    if (n != null) {
      return n.longValue();
    }
    return null;
  }

  public Map<String, String> getMap(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getMap(usr.getCurrentUserId()) : parameter.getMap();
  }

  public Number getNumber(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getNumber(usr.getCurrentUserId()) : parameter.getNumber();
  }

  public BeeParameter getParameter(String name) {
    Assert.notEmpty(name);
    Assert.state(hasParameter(name), "Unknown parameter: " + name);

    return BeeUtils.peek(parameters.row(BeeUtils.normalize(name)).values());
  }

  public Collection<BeeParameter> getParameters(String module) {
    Assert.notEmpty(module);
    Collection<BeeParameter> params = new ArrayList<>();

    for (BeeParameter param : parameters.values()) {
      if (BeeUtils.same(param.getModule(), module)) {
        params.add(BeeParameter.restore(param.serialize()));
      }
    }
    return params;
  }

  public Long getRelation(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getRelation(usr.getCurrentUserId()) : parameter.getRelation();
  }

  public Pair<Long, String> getRelationInfo(String name) {
    return getRelationInfo(name, false);
  }

  public String getText(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getText(usr.getCurrentUserId()) : parameter.getText();
  }

  public Long getTime(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getTime(usr.getCurrentUserId()) : parameter.getTime();
  }

  public String getValue(String name) {
    BeeParameter parameter = getParameter(name);
    return parameter.supportsUsers()
        ? parameter.getValue(usr.getCurrentUserId()) : parameter.getValue();
  }

  public boolean hasParameter(String name) {
    return parameters.containsRow(BeeUtils.normalize(name));
  }

  public void init() {
    parameterEventBus = new EventBus(logger::error);

    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void checkRelation(ViewDeleteEvent event) {
        if (event.isBefore()) {
          String table = BeeUtils.normalize(sys.getViewSource(event.getTargetName()));

          if (parameters.containsColumn(table)) {
            for (BeeParameter param : parameters.column(table).values()) {
              if (!DataUtils.isId(param.getId())) {
                continue;
              }
              for (Long userId : param.getUsers()) {
                Long id = BeeUtils.toLongOrNull(DataUtils.isId(userId)
                    ? param.getValue(userId) : param.getValue());

                if (DataUtils.isId(id) && event.getIds().contains(id)) {
                  event.addErrorMessage(Localized.dictionary()
                      .recordIsInUse(BeeUtils.joinWords(Localized.dictionary().parameter(),
                          param.getModule(), param.getName())));
                  return;
                }
              }
            }
          }
        }
      }
    });

    BeeView.registerConditionProvider(VAR_PARAMETERS_MODULE, (view, args) -> null);

    QueryServiceBean.registerViewDataProvider(TBL_PARAMETERS, new ViewDataProvider() {
      @Override
      public BeeRowSet getViewData(BeeView view, SqlSelect query, Filter filter) {
        BeeRowSet rs = new BeeRowSet(view.getName(), view.getRowSetColumns());

        String module = getFilterValue(filter, VAR_PARAMETERS_MODULE);

        if (!BeeUtils.isEmpty(module)) {
          String name = getFilterValue(filter, COL_PARAMETER_NAME);

          for (BeeParameter prm : getParameters(module)) {
            if (BeeUtils.isEmpty(name) || BeeUtils.containsSame(prm.getName(), name)) {
              addRow(rs, prm);
            }
          }
        }
        return rs;
      }

      @Override
      public int getViewSize(BeeView view, SqlSelect query, Filter filter) {
        String module = getFilterValue(filter, VAR_PARAMETERS_MODULE);
        int cnt = 0;

        if (!BeeUtils.isEmpty(module)) {
          String name = getFilterValue(filter, COL_PARAMETER_NAME);

          for (BeeParameter prm : getParameters(module)) {
            if (BeeUtils.isEmpty(name) || BeeUtils.containsSame(prm.getName(), name)) {
              cnt++;
            }
          }
        }
        return cnt;
      }

      private void addRow(BeeRowSet rs, BeeParameter prm) {
        BeeRow newRow = rs.addEmptyRow();
        newRow.setId(prm.getName().hashCode());

        newRow.setValue(rs.getColumnIndex(COL_PARAMETER_NAME), prm.getName());
        newRow.setValue(rs.getColumnIndex(COL_PARAMETER_VALUE), prm.serialize());

        if (prm.getType() == ParameterType.RELATION) {
          if (prm.supportsUsers()) {
            newRow.setProperty(VAR_PARAMETER_DEFAULT + COL_RELATION,
                getRelationInfo(prm.getName(), true).getB());
          }
          newRow.setProperty(COL_RELATION, getRelationInfo(prm.getName()).getB());
        }
        newRow.setProperty(COL_USER, prm.supportsUsers() ? "1" : null);
      }

      private String getFilterValue(Filter filter, String column) {
        String value = null;

        if (filter != null) {
          if (filter instanceof CompoundFilter) {
            for (Filter subFilter : ((CompoundFilter) filter).getSubFilters()) {
              value = getFilterValue(subFilter, column);

              if (!BeeUtils.isEmpty(value)) {
                break;
              }
            }
          } else if (filter instanceof ColumnValueFilter && filter.involvesColumn(column)) {
            value = BeeUtils.peek(((ColumnValueFilter) filter).getValue()).getString();

          } else if (filter instanceof CustomFilter
              && BeeUtils.same(((CustomFilter) filter).getKey(), column)) {

            value = BeeUtils.peek(((CustomFilter) filter).getArgs());
          }
        }
        return value;
      }
    });
  }

  public void postParameterEvent(ParameterEvent event) {
    parameterEventBus.post(event);
  }

  @Lock(LockType.WRITE)
  public void refreshParameters(String module) {
    Collection<BeeParameter> defaults = moduleBean.getModuleDefaultParameters(module);

    if (BeeUtils.isEmpty(defaults)) {
      return;
    }
    Set<String> names = new HashSet<>();

    for (BeeParameter param : defaults) {
      putParameter(param);
      names.add(param.getName());
    }
    SimpleRowSet data = qs.getData(new SqlSelect()
        .addFields(TBL_PARAMETERS, COL_PARAMETER_NAME, COL_PARAMETER_VALUE)
        .addField(TBL_PARAMETERS, sys.getIdName(TBL_PARAMETERS), COL_PARAMETER)
        .addFrom(TBL_PARAMETERS)
        .setWhere(SqlUtils.inList(TBL_PARAMETERS, COL_PARAMETER_NAME, names)));

    Map<Long, BeeParameter> ids = new HashMap<>();

    for (BeeParameter param : defaults) {
      SimpleRow row = data.getRowByKey(COL_PARAMETER_NAME, param.getName());

      if (Objects.nonNull(row)) {
        param.setId(row.getLong(COL_PARAMETER));
        param.setValue(row.getValue(COL_PARAMETER_VALUE));

        if (param.supportsUsers()) {
          ids.put(param.getId(), param);
        }
      }
    }
    if (!BeeUtils.isEmpty(ids)) {
      data = qs.getData(new SqlSelect()
          .addFields(TBL_USER_PARAMETERS, COL_PARAMETER, COL_USER, COL_PARAMETER_VALUE)
          .addFrom(TBL_USER_PARAMETERS)
          .setWhere(SqlUtils.inList(TBL_USER_PARAMETERS, COL_PARAMETER, ids.keySet())));

      for (SimpleRow row : data) {
        ids.get(row.getLong(COL_PARAMETER))
            .setValue(row.getLong(COL_USER), row.getValue(COL_PARAMETER_VALUE));
      }
    }
  }

  @Lock(LockType.WRITE)
  public void registerParameterEventHandler(ParameterEventHandler eventHandler) {
    parameterEventBus.register(eventHandler);
  }

  public void setParameter(String name, String value, boolean defaultMode) {
    BeeParameter param = getParameter(name);

    if (!DataUtils.isId(param.getId())) {
      param.setId(qs.insertData(new SqlInsert(TBL_PARAMETERS)
          .addConstant(COL_PARAMETER_NAME, param.getName())
          .addConstant(COL_PARAMETER_VALUE, param.getDefValue())));
    }
    if (param.supportsUsers() && !defaultMode) {
      Long userId = usr.getCurrentUserId();
      IsCondition wh = SqlUtils.equals(TBL_USER_PARAMETERS, COL_PARAMETER, param.getId(),
          COL_USER, userId);

      if (Objects.equals(value, param.getDefValue())) {
        qs.updateData(new SqlDelete(TBL_USER_PARAMETERS)
            .setWhere(wh));
      } else {
        if (!BeeUtils.isPositive(qs.updateData(new SqlUpdate(TBL_USER_PARAMETERS)
            .addConstant(COL_PARAMETER_VALUE, value)
            .setWhere(wh)))) {

          qs.insertData(new SqlInsert(TBL_USER_PARAMETERS)
              .addConstant(COL_PARAMETER, param.getId())
              .addConstant(COL_USER, userId)
              .addConstant(COL_PARAMETER_VALUE, value));
        }
      }
      param.setValue(userId, value);

    } else {
      qs.updateData(new SqlUpdate(TBL_PARAMETERS)
          .addConstant(COL_PARAMETER_VALUE, value)
          .setWhere(sys.idEquals(TBL_PARAMETERS, param.getId())));

      param.setValue(value);
    }
    ParameterMessage message = new ParameterMessage(param);

    if (!param.supportsUsers() || defaultMode) {
      Endpoint.sendToAll(message);
    } else {
      Endpoint.sendToUser(usr.getCurrentUserId(), message);
    }
    postParameterEvent(new ParameterEvent(name));
  }

  private Pair<Long, String> getRelationInfo(String name, boolean defaultMode) {
    BeeParameter param = getParameter(name);

    String display = null;
    Long relation = (param.supportsUsers() && !defaultMode)
        ? param.getRelation(usr.getCurrentUserId()) : param.getRelation();

    if (DataUtils.isId(relation)) {
      Pair<String, String> relInfo = Pair.restore(param.getOptions());
      BeeView view = sys.getView(relInfo.getA());
      String table = view.getSourceName();

      display = qs.getValue(new SqlSelect()
          .addFields(table, view.getColumnField(relInfo.getB()))
          .addFrom(table)
          .setWhere(sys.idEquals(table, relation)));
    }
    return Pair.of(relation, display);
  }

  private void putParameter(BeeParameter parameter) {
    String name = BeeUtils.normalize(parameter.getName());

    if (hasParameter(name)) {
      BeeParameter oldParam = getParameter(name);
      Assert.state(BeeUtils.same(oldParam.getModule(), parameter.getModule()),
          "Duplicate parameter name: " + name
              + " (modules: " + oldParam.getModule() + ", " + parameter.getModule() + ")");
    }
    parameters.put(name, parameter.getType() == ParameterType.RELATION
        ? BeeUtils.normalize(Pair.restore(parameter.getOptions()).getA()) : "", parameter);
  }
}
