package com.butent.bee.server.modules;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.BeeView.ConditionProvider;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

  private static final String TBL_PARAMS = "Parameters";
  private static final String TBL_USER_PARAMS = "UserParameters";
  private static final String FLD_USER = "User";
  private static final String FLD_NAME = "Name";
  private static final String FLD_VALUE = "Value";
  private static final String FLD_PARAM = "Parameter";

  @EJB
  ModuleHolderBean moduleBean;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  SystemBean sys;

  private final Table<String, String, BeeParameter> parameters = TreeBasedTable.create();
  private final EventBus parameterEventBus = new EventBus();

  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response = null;

    if (BeeUtils.same(svc, SVC_GET_PARAMETERS)) {
      response = ResponseObject
          .response(getParameters(reqInfo.getParameter(VAR_PARAMETERS_MODULE)));

    } else if (BeeUtils.same(svc, SVC_GET_PARAMETER)) {
      response = ResponseObject.response(getValue(reqInfo.getParameter(VAR_PARAMETER)));

    } else if (BeeUtils.same(svc, SVC_GET_RELATION_PARAMETER)) {
      response = ResponseObject.response(getRelationInfo(reqInfo.getParameter(VAR_PARAMETER)));

    } else if (BeeUtils.same(svc, SVC_SET_PARAMETER)) {
      setParameter(reqInfo.getParameter(VAR_PARAMETER), reqInfo.getParameter(VAR_PARAMETER_VALUE));
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
    BeeParameter param = getParameter(name);

    String display = null;
    Long relation = param.supportsUsers()
        ? param.getRelation(usr.getCurrentUserId()) : param.getRelation();

    if (DataUtils.isId(relation)) {
      Pair<String, String> relInfo = Pair.restore(param.getOptions());

      display = qs.getValue(new SqlSelect()
          .addFields(relInfo.getA(), relInfo.getB())
          .addFrom(relInfo.getA())
          .setWhere(sys.idEquals(relInfo.getA(), relation)));
    }
    return Pair.of(relation, display);
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
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
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
                  event.addErrorMessage(Localized.getMessages()
                      .recordIsInUse(BeeUtils.joinWords(Localized.getConstants().parameter(),
                          param.getModule(), param.getName())));
                  return;
                }
              }
            }
          }
        }
      }
    });

    BeeView.registerConditionProvider(TBL_PARAMS, new ConditionProvider() {
      @Override
      public IsCondition getCondition(BeeView view, List<String> args) {
        return null;
      }
    });

    QueryServiceBean.registerViewDataProvider(TBL_PARAMS, new ViewDataProvider() {
      @Override
      public BeeRowSet getViewData(BeeView view, SqlSelect query, Filter filter) {
        BeeRowSet rs = new BeeRowSet(view.getName(), view.getRowSetColumns());

        String module = getFilterValue(filter, TBL_PARAMS);

        if (!BeeUtils.isEmpty(module)) {
          String name = getFilterValue(filter, FLD_NAME);

          if (!BeeUtils.isEmpty(name)) {
            addRow(rs, getParameter(name));
          } else {
            for (BeeParameter prm : getParameters(module)) {
              addRow(rs, prm);
            }
          }
        }
        return rs;
      }

      @Override
      public int getViewSize(BeeView view, SqlSelect query, Filter filter) {
        String module = getFilterValue(filter, TBL_PARAMS);

        if (!BeeUtils.isEmpty(module)) {
          if (!BeeUtils.isEmpty(getFilterValue(filter, FLD_NAME))) {
            return 1;
          }
          return getParameters(module).size();
        }
        return 0;
      }

      private void addRow(BeeRowSet rs, BeeParameter prm) {
        BeeRow newRow = rs.addEmptyRow();
        newRow.setId(prm.getName().hashCode());
        boolean hasValue;

        if (prm.supportsUsers()) {
          hasValue = prm.hasValue(usr.getCurrentUserId());
          newRow.setProperty(FLD_USER, "1");
        } else {
          hasValue = prm.hasValue();
        }
        if (hasValue) {
          newRow.setProperty("HasValue", "1");
        }
        if (prm.getType() == ParameterType.RELATION) {
          newRow.setProperty(COL_RELATION, getRelationInfo(prm.getName()).getB());
        }
        newRow.setValue(rs.getColumnIndex(FLD_NAME), prm.getName());
        newRow.setValue(rs.getColumnIndex(FLD_VALUE), prm.serialize());
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
          } else if (filter instanceof ColumnValueFilter
              && ((ColumnValueFilter) filter).involvesColumn(column)) {

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
        .addFields(TBL_PARAMS, FLD_NAME, FLD_VALUE)
        .addField(TBL_PARAMS, sys.getIdName(TBL_PARAMS), FLD_PARAM)
        .addFrom(TBL_PARAMS)
        .setWhere(SqlUtils.inList(TBL_PARAMS, FLD_NAME, names)));

    names.clear();

    for (BeeParameter param : defaults) {
      if (param.supportsUsers()) {
        names.add(param.getName());
      } else {
        param.setValue(data.getValueByKey(FLD_NAME, param.getName(), FLD_VALUE));
      }
      param.setId(BeeUtils.toLongOrNull(data.getValueByKey(FLD_NAME, param.getName(), FLD_PARAM)));
    }
    if (!BeeUtils.isEmpty(names)) {
      data = qs.getData(new SqlSelect()
          .addFields(TBL_USER_PARAMS, FLD_PARAM, FLD_USER, FLD_VALUE)
          .addFrom(TBL_USER_PARAMS)
          .addFromInner(TBL_PARAMS, sys.joinTables(TBL_PARAMS, TBL_USER_PARAMS, FLD_PARAM))
          .setWhere(SqlUtils.inList(TBL_PARAMS, FLD_NAME, names)));

      for (BeeParameter param : defaults) {
        if (param.supportsUsers() && DataUtils.isId(param.getId())) {
          for (SimpleRow row : data) {
            if (Objects.equals(param.getId(), row.getLong(FLD_PARAM))) {
              param.setValue(row.getLong(FLD_USER), row.getValue(FLD_VALUE));
            }
          }
        }
      }
    }
  }

  @Lock(LockType.WRITE)
  public void registerParameterEventHandler(ParameterEventHandler eventHandler) {
    parameterEventBus.register(eventHandler);
  }

  @Lock(LockType.WRITE)
  public void setParameter(String name, String value) {
    BeeParameter param = getParameter(name);

    if (!DataUtils.isId(param.getId())) {
      param.setId(qs.insertData(new SqlInsert(TBL_PARAMS)
          .addConstant(FLD_NAME, param.getName())));
    }
    if (param.supportsUsers()) {
      Long userId = usr.getCurrentUserId();
      IsCondition wh = SqlUtils.equals(TBL_USER_PARAMS, FLD_PARAM, param.getId(), FLD_USER, userId);

      if (value == null) {
        qs.updateData(new SqlDelete(TBL_USER_PARAMS)
            .setWhere(wh));
      } else {
        if (!BeeUtils.isPositive(qs.updateData(new SqlUpdate(TBL_USER_PARAMS)
            .addConstant(FLD_VALUE, value)
            .setWhere(wh)))) {

          qs.insertData(new SqlInsert(TBL_USER_PARAMS)
              .addConstant(FLD_PARAM, param.getId())
              .addConstant(FLD_USER, userId)
              .addConstant(FLD_VALUE, value));
        }
      }
      param.setValue(userId, value);

    } else {
      qs.updateData(new SqlUpdate(TBL_PARAMS)
          .addConstant(FLD_VALUE, value)
          .setWhere(sys.idEquals(TBL_PARAMS, param.getId())));

      param.setValue(value);
    }
    postParameterEvent(new ParameterEvent(name));
  }

  private BeeParameter getParameter(String name) {
    Assert.notEmpty(name);
    Assert.state(hasParameter(name), "Unknown parameter: " + name);

    return BeeUtils.peek(parameters.row(BeeUtils.normalize(name)).values());
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
