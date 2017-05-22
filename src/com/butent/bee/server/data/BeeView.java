package com.butent.bee.server.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.butent.bee.server.Invocation;
import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.i18n.Localizations;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsFrom;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeObject;
import com.butent.bee.shared.data.Defaults.DefaultExpression;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.SqlConstants.SqlDataType;
import com.butent.bee.shared.data.SqlConstants.SqlFunction;
import com.butent.bee.shared.data.XmlExpression;
import com.butent.bee.shared.data.XmlExpression.XmlBulk;
import com.butent.bee.shared.data.XmlExpression.XmlCase;
import com.butent.bee.shared.data.XmlExpression.XmlCast;
import com.butent.bee.shared.data.XmlExpression.XmlConcat;
import com.butent.bee.shared.data.XmlExpression.XmlDivide;
import com.butent.bee.shared.data.XmlExpression.XmlHasMember;
import com.butent.bee.shared.data.XmlExpression.XmlHasMembers;
import com.butent.bee.shared.data.XmlExpression.XmlMinus;
import com.butent.bee.shared.data.XmlExpression.XmlMultiply;
import com.butent.bee.shared.data.XmlExpression.XmlName;
import com.butent.bee.shared.data.XmlExpression.XmlNvl;
import com.butent.bee.shared.data.XmlExpression.XmlPlus;
import com.butent.bee.shared.data.XmlExpression.XmlSwitch;
import com.butent.bee.shared.data.XmlView;
import com.butent.bee.shared.data.XmlView.XmlAggregateColumn;
import com.butent.bee.shared.data.XmlView.XmlColumn;
import com.butent.bee.shared.data.XmlView.XmlColumns;
import com.butent.bee.shared.data.XmlView.XmlExternalJoin;
import com.butent.bee.shared.data.XmlView.XmlHiddenColumn;
import com.butent.bee.shared.data.XmlView.XmlIdColumn;
import com.butent.bee.shared.data.XmlView.XmlOrder;
import com.butent.bee.shared.data.XmlView.XmlSimpleColumn;
import com.butent.bee.shared.data.XmlView.XmlSimpleJoin;
import com.butent.bee.shared.data.XmlView.XmlVersionColumn;
import com.butent.bee.shared.data.filter.ColumnColumnFilter;
import com.butent.bee.shared.data.filter.ColumnInFilter;
import com.butent.bee.shared.data.filter.ColumnIsNullFilter;
import com.butent.bee.shared.data.filter.ColumnNotNullFilter;
import com.butent.bee.shared.data.filter.ColumnValueFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.CompoundType;
import com.butent.bee.shared.data.filter.CustomFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterParser;
import com.butent.bee.shared.data.filter.IdFilter;
import com.butent.bee.shared.data.filter.IsFalseFilter;
import com.butent.bee.shared.data.filter.IsTrueFilter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.filter.VersionFilter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.i18n.SupportedLocale;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.PropertyUtils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements database view management - contains parameters for views and their fields and methods
 * for doing operations with them.
 */

public class BeeView implements BeeObject, HasExtendedInfo {

  @FunctionalInterface
  public interface ConditionProvider {
    IsCondition getCondition(BeeView view, List<String> args);
  }

  private final class ColumnInfo {
    private final String colName;
    private final String alias;
    private final BeeField field;
    private final String locale;
    private final SqlFunction aggregate;
    private final boolean hidden;
    private final String parentName;
    private final String ownerAlias;
    private final XmlExpression xmlExpression;
    private IsExpression expression;
    private final String label;
    private final Boolean editable;

    private ColumnInfo(String alias, BeeField field, String colName, String locale,
        SqlFunction aggregate, boolean hidden, String parent, String owner,
        XmlExpression expression, String label, Boolean editable) {
      this.colName = colName;
      this.alias = alias;
      this.field = field;
      this.locale = locale;
      this.aggregate = aggregate;
      this.hidden = hidden;
      this.parentName = parent;
      this.ownerAlias = owner;
      this.xmlExpression = expression;

      if (BeeUtils.isEmpty(label) && field != null) {
        this.label = field.getLabel();
      } else {
        this.label = label;
      }

      this.editable = editable;
    }

    public SqlFunction getAggregate() {
      return aggregate;
    }

    public String getAlias() {
      return alias;
    }

    public Pair<DefaultExpression, Object> getDefaults() {
      Pair<DefaultExpression, Object> defaults = null;

      if (field != null) {
        defaults = field.getDefaults();
      }
      return defaults;
    }

    public String getEnumKey() {
      String key = null;

      if (field != null) {
        key = field.getEnumKey();
      }
      return key;
    }

    public IsExpression getExpression() {
      if (expression == null) {
        expression = parse(xmlExpression, Sets.newHashSet(getName()));
      }
      return expression;
    }

    public String getField() {
      String fldName = null;

      if (field != null) {
        fldName = field.getName();
      }
      return fldName;
    }

    public String getLabel() {
      return label;
    }

    public int getLevel() {
      int level = 0;

      if (!BeeUtils.isEmpty(getParent())) {
        ColumnInfo parent = getColumnInfo(getParent());

        if (!((BeeRelation) parent.field).isEditable()) {
          level = 1;
        }
        level += parent.getLevel();
      }
      return level;
    }

    public String getLocale() {
      return locale;
    }

    public String getName() {
      return colName;
    }

    public String getOwner() {
      return ownerAlias;
    }

    public String getParent() {
      return parentName;
    }

    public int getPrecision() {
      int precision = BeeConst.UNDEF;

      if (field != null) {
        precision = field.getPrecision();
      }
      return precision;
    }

    public String getRelation() {
      String relName = null;

      if (field != null && field instanceof BeeRelation) {
        relName = ((BeeRelation) field).getRelation();
      }
      return relName;
    }

    public int getScale() {
      int scale = BeeConst.UNDEF;

      if (field != null) {
        scale = field.getScale();
      }
      return scale;
    }

    public String getTable() {
      String tblName = null;

      if (field != null) {
        tblName = field.getOwner().getName();
      }
      return tblName;
    }

    public SqlDataType getType() {
      if (xmlExpression != null) {
        return EnumUtils.getEnumByName(SqlDataType.class, xmlExpression.type);
      } else {
        return field.getType();
      }
    }

    public boolean isEditable() {
      if (editable == null) {
        return !isReadOnly() && !isHidden() && getLevel() <= 0;
      } else {
        return editable;
      }
    }

    public boolean isHidden() {
      return hidden;
    }

    public boolean isNullable() {
      return field == null || !field.isNotNull() || !BeeUtils.isEmpty(locale);
    }

    public boolean isReadOnly() {
      return getAggregate() != null || getExpression() != null;
    }

    private IsExpression parse(XmlExpression xmlExpr, Set<String> history) {
      if (xmlExpr == null) {
        return null;
      }
      IsExpression expr = null;

      if (xmlExpr instanceof XmlName) {
        expr = SqlUtils.name(xmlExpr.content);

      } else if (xmlExpr instanceof XmlHasMember) {
        IsExpression member = parse(((XmlHasMember) xmlExpr).member, history);

        if (xmlExpr instanceof XmlSwitch) {
          XmlSwitch sw = (XmlSwitch) xmlExpr;
          List<IsExpression> cases = new ArrayList<>();

          for (XmlCase xmlCase : sw.cases) {
            cases.add(parse(xmlCase.whenExpression, history));
            cases.add(parse(xmlCase.thenExpression.member, history));
          }
          cases.add(parse(sw.elseExpression.member, history));

          expr = SqlUtils.sqlCase(member, cases.toArray());

        } else if (xmlExpr instanceof XmlCast) {
          XmlCast cast = (XmlCast) xmlExpr;
          expr = SqlUtils.cast(member, SqlDataType.valueOf(cast.type), cast.precision, cast.scale);
        }
      } else if (xmlExpr instanceof XmlHasMembers) {
        List<IsExpression> members = new ArrayList<>();

        for (XmlExpression z : ((XmlHasMembers) xmlExpr).members) {
          members.add(parse(z, history));
        }
        if (xmlExpr instanceof XmlPlus) {
          expr = SqlUtils.plus(members.toArray());
        } else if (xmlExpr instanceof XmlMinus) {
          expr = SqlUtils.minus(members.toArray());
        } else if (xmlExpr instanceof XmlMultiply) {
          expr = SqlUtils.multiply(members.toArray());
        } else if (xmlExpr instanceof XmlDivide) {
          expr = SqlUtils.divide(members.toArray());
        } else if (xmlExpr instanceof XmlBulk) {
          expr = SqlUtils.expression(members.toArray());
        } else if (xmlExpr instanceof XmlNvl) {
          expr = SqlUtils.nvl(members.toArray());
        } else if (xmlExpr instanceof XmlConcat) {
          expr = SqlUtils.concat(members.toArray());
        }
      } else if (xmlExpr.content != null && !xmlExpr.content.replaceAll("\\s", "").isEmpty()) {
        List<Object> x = new ArrayList<>();
        x.add(")");
        String xpr = xmlExpr.content;
        String regex = "^(.*)\"(\\w+)\"(.*)$";

        while (xpr.matches(regex)) {
          String s = xpr.replaceFirst(regex, "$3");
          String col = xpr.replaceFirst(regex, "$2");
          xpr = xpr.replaceFirst(regex, "$1");

          if (!BeeUtils.isEmpty(s)) {
            x.add(s);
          }
          if (hasColumn(col)) {
            ColumnInfo info = getColumnInfo(col);
            Assert.state(!history.contains(info.getName()),
                BeeUtils.joinWords("Parsing cycle detected.",
                    "View:", BeeView.this.getName(), "Column:", info.getName()));

            if (info.expression == null) {
              history.add(info.getName());
              info.expression = parse(info.xmlExpression, history);
              history.remove(info.getName());
            }
            IsExpression xp = getSqlExpression(col);
            x.add(xp);
          } else {
            x.add("\"" + col + "\"");
          }
        }
        if (!BeeUtils.isEmpty(xpr)) {
          x.add(xpr);
        }
        x.add("(");
        Collections.reverse(x);
        expr = SqlUtils.expression(x.toArray());
      }
      if (expr == null) {
        Assert.unsupported(NameUtils.getClassName(xmlExpr.getClass()));
      }
      return expr;
    }
  }

  private enum JoinType {
    INNER, RIGHT, LEFT, FULL
  }

  private static BeeLogger logger = LogUtils.getLogger(BeeView.class);

  private static final Map<String, ConditionProvider> conditionProviders =
      new ConcurrentHashMap<>();

  public static void registerConditionProvider(String key, ConditionProvider provider) {
    Assert.notEmpty(key);
    Assert.notNull(provider);

    conditionProviders.put(key, provider);
  }

  private static void initColumn(ColumnInfo info, BeeColumn column, boolean required) {
    column.setId(info.getName());
    column.setLabel(BeeUtils.notEmpty(info.getLabel(), info.getName()));

    column.setType(info.getType().toValueType());

    column.setNullable(info.isNullable() && !required);

    column.setPrecision(info.getPrecision());
    column.setScale(info.getScale());

    column.setReadOnly(info.isReadOnly());
    column.setEditable(info.isEditable());

    column.setLevel(info.getLevel());

    column.setDefaults(info.getDefaults());

    column.setEnumKey(info.getEnumKey());
  }

  private final String module;
  private final String name;
  private final BeeTable source;
  private final String sourceAlias;

  private final boolean readOnly;

  private final String relationInfo;

  private final String caption;
  private final String editForm;

  private final String rowCaption;
  private final String newRowForm;
  private final String newRowColumns;

  private final String newRowCaption;
  private final Integer cacheMaximumSize;

  private final String cacheEviction;
  private boolean hasAggregate;
  private final boolean hasGrouping;
  private final SqlSelect query;
  private final Map<String, ColumnInfo> columns = new LinkedHashMap<>();
  private final String filter;
  private final Map<HasConditions, String> joinFilters = new HashMap<>();

  private Order order;

  BeeView(String module, XmlView xmlView, Map<String, BeeTable> tables) {
    Assert.notNull(xmlView);
    this.module = BeeUtils.notEmpty(xmlView.module, module);
    this.name = Assert.notEmpty(xmlView.name);

    if (xmlView.relation instanceof Node && BeeUtils.same(((Node) xmlView.relation).getNodeName(),
        AdministrationConstants.COL_RELATION)) {
      this.relationInfo = XmlUtils.toString((Node) xmlView.relation, true);
    } else {
      this.relationInfo = null;
    }
    this.caption = xmlView.caption;

    this.editForm = xmlView.editForm;
    this.rowCaption = xmlView.rowCaption;

    this.newRowForm = xmlView.newRowForm;
    this.newRowColumns = xmlView.newRowColumns;
    this.newRowCaption = xmlView.newRowCaption;

    this.cacheMaximumSize = xmlView.cacheMaximumSize;
    this.cacheEviction = xmlView.cacheEviction;

    this.source = tables.get(BeeUtils.normalize(xmlView.source));
    Assert.notNull(source);

    this.sourceAlias = getSourceName();

    this.query = new SqlSelect().addFrom(getSourceName(), getSourceAlias());

    addColumns(source, getSourceAlias(), xmlView.columns, null, tables);
    setColumns();

    Set<String> grouping;
    hasGrouping = xmlView.groupBy != null && !BeeUtils.isEmpty(xmlView.groupBy.columns);

    if (hasGrouping) {
      this.readOnly = true;
      grouping = xmlView.groupBy.columns;
    } else {
      this.readOnly = xmlView.readOnly;
      grouping = Collections.emptySet();
    }
    setGrouping(grouping);

    this.filter = xmlView.filter;

    if (!BeeUtils.isEmpty(xmlView.orders)) {
      this.order = new Order();

      for (XmlOrder ord : xmlView.orders) {
        order.add(ord.column, !ord.descending, ord.nulls);
      }
    }
  }

  public BeeColumn getBeeColumn(String colName) {
    BeeColumn column = new BeeColumn();
    UserServiceBean usr = Invocation.locateRemoteBean(UserServiceBean.class);

    initColumn(colName, column, usr.isColumnRequired(this, colName));
    return column;
  }

  public String getCacheEviction() {
    return cacheEviction;
  }

  public Integer getCacheMaximumSize() {
    return cacheMaximumSize;
  }

  public String getCaption() {
    return caption;
  }

  public SqlFunction getColumnAggregate(String colName) {
    return getColumnInfo(colName).getAggregate();
  }

  public int getColumnCount() {
    return getColumnNames().size();
  }

  public String getColumnEnumKey(String colName) {
    return getColumnInfo(colName).getEnumKey();
  }

  public IsExpression getColumnExpression(String colName) {
    return getColumnInfo(colName).getExpression();
  }

  public String getColumnField(String colName) {
    if (getSourceIdName().equals(colName) || getSourceVersionName().equals(colName)) {
      return colName;
    } else {
      return getColumnInfo(colName).getField();
    }
  }

  public String getColumnLabel(String colName) {
    return getColumnInfo(colName).getLabel();
  }

  public int getColumnLevel(String colName) {
    return getColumnInfo(colName).getLevel();
  }

  public String getColumnLocale(String colName) {
    return getColumnInfo(colName).getLocale();
  }

  public String getColumnName(String colName) {
    return getColumnInfo(colName).getName();
  }

  public Collection<String> getColumnNames() {
    Collection<String> cols = new ArrayList<>();

    for (ColumnInfo col : columns.values()) {
      if (!col.isHidden()) {
        cols.add(col.getName());
      }
    }
    return cols;
  }

  public String getColumnOwner(String colName) {
    return getColumnInfo(colName).getOwner();
  }

  public String getColumnParent(String colName) {
    return getColumnInfo(colName).getParent();
  }

  public int getColumnPrecision(String colName) {
    return getColumnInfo(colName).getPrecision();
  }

  public String getColumnRelation(String colName) {
    return getColumnInfo(colName).getRelation();
  }

  public int getColumnScale(String colName) {
    return getColumnInfo(colName).getScale();
  }

  public String getColumnSource(String colName) {
    return getColumnInfo(colName).getAlias();
  }

  public String getColumnTable(String colName) {
    if (getSourceIdName().equals(colName) || getSourceVersionName().equals(colName)) {
      return getSourceName();
    } else {
      return getColumnInfo(colName).getTable();
    }
  }

  public SqlDataType getColumnType(String colName) {
    return getColumnInfo(colName).getType();
  }

  public IsCondition getCondition(Filter flt) {
    if (flt != null) {
      String clazz = NameUtils.getClassName(flt.getClass());

      if (NameUtils.getClassName(ColumnValueFilter.class).equals(clazz)) {
        return getCondition((ColumnValueFilter) flt);

      } else if (NameUtils.getClassName(ColumnColumnFilter.class).equals(clazz)) {
        return getCondition((ColumnColumnFilter) flt);

      } else if (NameUtils.getClassName(ColumnIsNullFilter.class).equals(clazz)) {
        return getCondition((ColumnIsNullFilter) flt);

      } else if (NameUtils.getClassName(ColumnNotNullFilter.class).equals(clazz)) {
        return getCondition((ColumnNotNullFilter) flt);

      } else if (NameUtils.getClassName(CompoundFilter.class).equals(clazz)) {
        return getCondition((CompoundFilter) flt);

      } else if (NameUtils.getClassName(IdFilter.class).equals(clazz)) {
        return getCondition((IdFilter) flt);

      } else if (NameUtils.getClassName(VersionFilter.class).equals(clazz)) {
        return getCondition((VersionFilter) flt);

      } else if (NameUtils.getClassName(IsFalseFilter.class).equals(clazz)) {
        return SqlUtils.sqlFalse();

      } else if (NameUtils.getClassName(IsTrueFilter.class).equals(clazz)) {
        return SqlUtils.sqlTrue();

      } else if (NameUtils.getClassName(ColumnInFilter.class).equals(clazz)) {
        return getCondition((ColumnInFilter) flt);

      } else if (NameUtils.getClassName(CustomFilter.class).equals(clazz)) {
        return getCondition((CustomFilter) flt);

      } else {
        Assert.unsupported("Unsupported class name: " + clazz);
      }
    }
    return null;
  }

  public String getEditForm() {
    return editForm;
  }

  @Override
  public List<ExtendedProperty> getExtendedInfo() {
    List<ExtendedProperty> info = new ArrayList<>();

    PropertyUtils.addProperties(info, false, "Module", getModule(), "Name", getName(),
        "Source", getSourceName(), "Source Alias", getSourceAlias(),
        "Source Id Name", getSourceIdName(), "Source Version Name", getSourceVersionName(),
        "Read Only", isReadOnly(), "Caption", getCaption(), "Edit Form", getEditForm(),
        "Row Caption", getRowCaption(), "New Row Form", getNewRowForm(),
        "New Row Columns", getNewRowColumns(), "New Row Caption", getNewRowCaption(),
        "Cache Maximum Size", getCacheMaximumSize(), "Cache Eviction", getCacheEviction(),
        "Query", query.getQuery(), "Columns", columns.size());

    int i = 0;
    for (String col : columns.keySet()) {
      String key = BeeUtils.joinWords("Column", ++i, col);

      PropertyUtils.addChildren(info, key,
          "Table", getColumnTable(col), "Alias", getColumnSource(col),
          "Field", getColumnField(col), "Type", getColumnType(col), "Locale", getColumnLocale(col),
          "Aggregate Function", getColumnAggregate(col), "Hidden", isColHidden(col),
          "Read Only", isColReadOnly(col), "Editable", isColEditable(col),
          "Not Null", !isColNullable(col), "Level", getColumnLevel(col),
          "Expression", Objects.nonNull(getColumnExpression(col)) ? getColumnExpression(col)
              .getSqlString(SqlBuilderFactory.getBuilder(SqlEngine.GENERIC)) : null,
          "Parent Column", getColumnParent(col), "Owner Alias", getColumnOwner(col),
          "Label", getColumnLabel(col), "Enum key", getColumnEnumKey(col));
    }
    if (order != null) {
      info.add(new ExtendedProperty("Order", BeeUtils.toString(order.getSize())));
      i = 0;
      for (Order.Column ordCol : order.getColumns()) {
        info.add(new ExtendedProperty("Order", BeeUtils.toString(++i), ordCol.toString()));
      }
    }
    return info;
  }

  public List<IsFrom> getFrom() {
    return query.getFrom();
  }

  @Override
  public String getModule() {
    return module;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getNewRowCaption() {
    return newRowCaption;
  }

  public String getNewRowColumns() {
    return newRowColumns;
  }

  public String getNewRowForm() {
    return newRowForm;
  }

  public SqlSelect getQuery(Long userId) {
    return getQuery(userId, null);
  }

  public SqlSelect getQuery(Long userId, Filter flt, Order ord, Collection<String> cols) {

    SqlSelect ss;

    if (!BeeUtils.isEmpty(joinFilters)) {
      synchronized (this) {
        for (Entry<HasConditions, String> joinFilter : joinFilters.entrySet()) {
          HasConditions join = joinFilter.getKey();
          join.clear();
          join.add(getCondition(parseFilter(joinFilter.getValue(), userId)));
        }
        ss = query.copyOf(true);
      }
    } else {
      ss = query.copyOf();
    }
    Collection<String> activeCols = null;

    if (!BeeUtils.isEmpty(cols)) {
      ss.resetFields();
      activeCols = new HashSet<>();

      for (String col : cols) {
        if (!isColHidden(col)) {
          String colName = getColumnName(col);
          ss.addExpr(getSqlExpression(col), colName);
          activeCols.add(colName);
        }
      }
    }
    setFilter(ss, flt, userId);

    String src = getSourceAlias();
    String idCol = getSourceIdName();
    String verCol = getSourceVersionName();
    Order o = BeeUtils.nvl(ord, order);
    String alias;
    String colName;

    boolean idUsed = false;

    if (o != null) {
      for (Order.Column ordCol : o.getColumns()) {
        for (String col : ordCol.getSources()) {
          if (hasColumn(col)) {
            if (isColAggregate(col) || Objects.nonNull(getColumnExpression(col))) {
              alias = null;
              colName = getColumnName(col);

              if (isColHidden(col)
                  || (!BeeUtils.isEmpty(activeCols) && !BeeUtils.contains(activeCols, colName))) {
                logger.warning("view: ", getName(), "order by:", col, ". Column not in field list");
                continue;
              }
            } else {
              alias = getColumnSource(col);
              colName = getColumnField(col);
            }

          } else if (BeeUtils.same(col, idCol)) {
            alias = src;
            colName = idCol;
            idUsed = true;

          } else if (BeeUtils.same(col, verCol)) {
            alias = src;
            colName = verCol;

          } else {
            logger.warning("view: ", getName(), "order by:", col, ". Column not recognized");
            continue;
          }

          ss.addOrderBy(ordCol, alias, colName);
        }
      }
    }

    if (hasGrouping) {
      ss.addMin(src, idCol)
          .addOrder(null, idCol);
    } else {
      ss.addFields(src, idCol, verCol);
      if (!idUsed) {
        ss.addOrder(src, idCol);
      }
    }
    return ss;
  }

  public SqlSelect getQuery(Long userId, Filter flt) {
    return getQuery(userId, flt, null, null);
  }

  public String getRelationInfo() {
    return relationInfo;
  }

  public String getRootField(String colName) {
    if (hasColumn(colName)) {
      ColumnInfo column = getColumnInfo(colName);

      if (BeeUtils.isEmpty(column.getParent()) || column.getLevel() <= 0) {
        return column.getField();
      } else {
        return getRootField(column.getParent());
      }

    } else {
      return null;
    }
  }

  public String getRowCaption() {
    return rowCaption;
  }

  public List<BeeColumn> getRowSetColumns() {
    List<BeeColumn> result = new ArrayList<>();
    UserServiceBean usr = Invocation.locateRemoteBean(UserServiceBean.class);

    for (ColumnInfo info : columns.values()) {
      if (!info.isHidden()) {
        BeeColumn column = new BeeColumn();
        initColumn(info, column, usr.isColumnRequired(this, info.getName()));
        result.add(column);
      }
    }

    return result;
  }

  public int getRowSetIndex(String colName) {
    int index = 0;

    for (ColumnInfo info : columns.values()) {
      if (!info.isHidden()) {
        if (BeeUtils.same(info.getName(), colName)) {
          return index;
        }

        index++;
      }
    }

    logger.warning("view", getName(), "column", colName, "not found");
    return BeeConst.UNDEF;
  }

  public String getSourceAlias() {
    return sourceAlias;
  }

  public String getSourceIdName() {
    return source.getIdName();
  }

  public String getSourceName() {
    return source.getName();
  }

  public String getSourceVersionName() {
    return source.getVersionName();
  }

  public ListMultimap<String, String> getTranslationColumns() {
    ListMultimap<String, String> result = ArrayListMultimap.create();

    columns.forEach((colName, columnInfo) -> {
      if (!BeeUtils.isEmpty(columnInfo.getLocale()) && columnInfo.field.isTranslatable()
          && !BeeUtils.isEmpty(Localized.extractLanguage(colName))) {
        result.put(columnInfo.getField(), colName);
      }
    });

    return result;
  }

  public Map<String, String> getTranslationColumns(String fieldName) {
    Map<String, String> result = new HashMap<>();

    columns.forEach((colName, columnInfo) -> {
      if (!BeeUtils.isEmpty(columnInfo.getLocale()) && columnInfo.field.isTranslatable()
          && !BeeUtils.isEmpty(Localized.extractLanguage(colName))
          && BeeUtils.same(columnInfo.getField(), fieldName)) {
        result.put(columnInfo.getLocale(), colName);
      }
    });

    return result;
  }

  public List<ViewColumn> getViewColumns() {
    List<ViewColumn> result = new ArrayList<>();

    for (ColumnInfo cInf : columns.values()) {
      result.add(new ViewColumn(cInf.getName(), cInf.getParent(), cInf.getTable(), cInf.getField(),
          cInf.getRelation(), cInf.getLevel(), cInf.isHidden(), cInf.isReadOnly(),
          cInf.isEditable()));
    }
    return result;
  }

  public boolean hasColumn(String colName) {
    return !BeeUtils.isEmpty(colName) && columns.containsKey(BeeUtils.normalize(colName));
  }

  public void initColumn(String colName, BeeColumn column, boolean required) {
    initColumn(getColumnInfo(colName), column, required);
  }

  public boolean isColAggregate(String colName) {
    return getColumnAggregate(colName) != null;
  }

  public boolean isColCalculated(String colName) {
    return Objects.nonNull(getColumnExpression(colName))
        && Objects.isNull(getColumnSource(colName));
  }

  public boolean isColEditable(String colName) {
    return getColumnInfo(colName).isEditable();
  }

  public boolean isColHidden(String colName) {
    return getColumnInfo(colName).isHidden();
  }

  public boolean isColNullable(String colName) {
    return getColumnInfo(colName).isNullable();
  }

  public boolean isColReadOnly(String colName) {
    return getColumnInfo(colName).isReadOnly();
  }

  public boolean isEmpty() {
    return getColumnCount() == 0;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public Filter parseFilter(String flt, Long userId) {
    Assert.notEmpty(flt);
    List<IsColumn> cols = Lists.newArrayListWithCapacity(columns.size());

    for (String col : columns.keySet()) {
      cols.add(new BeeColumn(getColumnType(col).toValueType(), col));
    }
    Filter f = FilterParser.parse(flt, cols, getSourceIdName(), getSourceVersionName(), userId);

    if (f == null) {
      logger.warning("Error in filter expression:", flt);
    }
    return f;
  }

  public Order parseOrder(String input) {
    Assert.notEmpty(input);

    Set<String> colNames = Sets.newHashSet(getSourceIdName(), getSourceVersionName());
    colNames.addAll(getColumnNames());

    return Order.parse(input, colNames);
  }

  private void addColumn(String alias, BeeField field, String colName, String locale,
      String aggregateType, boolean hidden, String parent, XmlExpression expression, String label,
      Boolean editable) {

    Assert.state(!BeeUtils.inListSame(colName, getSourceIdName(), getSourceVersionName()),
        BeeUtils.joinWords("Reserved column name:", getName(), colName));
    Assert.state(!hasColumn(colName),
        BeeUtils.joinWords("Duplicate column name:", getName(), colName));

    String ownerAlias = null;
    String newAlias = alias;
    SqlFunction aggregate = null;

    if (field != null) {
      BeeTable table = field.getOwner();

      if (!BeeUtils.isEmpty(locale)) {
        if (field.isTranslatable()) {
          ownerAlias = newAlias;
          newAlias = table.joinTranslationField(query, ownerAlias, field, locale);
        } else {
          logger.warning("Field is not translatable:", table.getName() + "." + field.getName(),
              "View:", getName());
          return;
        }
      } else if (field.isExtended()) {
        ownerAlias = newAlias;
        newAlias = table.joinExtField(query, ownerAlias, field);
      }
    }
    if (!BeeUtils.isEmpty(aggregateType)) {
      aggregate = SqlFunction.valueOf(aggregateType);
    }
    columns.put(BeeUtils.normalize(colName),
        new ColumnInfo(newAlias, field, colName, locale, aggregate, hidden, parent, ownerAlias,
            expression, label, editable));
  }

  private void addColumns(BeeTable table, String alias, XmlColumns cols, String parent,
      Map<String, BeeTable> tables) {
    Assert.notNull(table);
    Assert.notEmpty(alias);
    Assert.notNull(cols);

    for (XmlColumn column : cols.columns) {
      if (column instanceof XmlSimpleJoin) {
        XmlSimpleJoin col = (XmlSimpleJoin) column;
        BeeTable relTable;
        BeeField field;
        IsCondition join;
        String als;
        String relAls = SqlUtils.uniqueName();

        if (col instanceof XmlExternalJoin) {
          relTable = tables.get(BeeUtils.normalize(((XmlExternalJoin) col).source));
          Assert.notNull(relTable);
          als = relAls;
          field = relTable.getField(col.name);

          if (field.isExtended()) {
            logger.warning("Inverse join is not supported on extended fields:",
                relTable.getName() + "." + field.getName(), "View:", getName());
            continue;
          }
          join = SqlUtils.join(alias,
              BeeUtils.notEmpty(((XmlExternalJoin) col).targetName, table.getIdName()),
              relAls, field.getName());
        } else {
          Assert.state(table.hasField(col.name), BeeUtils.joinWords("View:", getName(),
              "Unknown field name:", table.getName(), col.name));
          field = table.getField(col.name);
          Assert.state(field instanceof BeeRelation);

          if (field.isExtended()) {
            als = table.joinExtField(query, alias, field);
          } else {
            als = alias;
          }
          relTable = tables.get(BeeUtils.normalize(((BeeRelation) field).getRelation()));
          join = SqlUtils.join(als, field.getName(), relAls, relTable.getIdName());
        }
        if (!BeeUtils.isEmpty(col.filter)) {
          HasConditions compound = SqlUtils.and();
          String flt = col.filter;

          if (BeeUtils.isPrefix(flt, CompoundType.OR.name())) {
            flt = BeeUtils.removePrefix(flt, CompoundType.OR.name());
            join = SqlUtils.or(join, compound);
          } else {
            join = SqlUtils.and(join, compound);
          }
          joinFilters.put(compound, flt);
        }
        String relTbl = relTable.getName();

        switch (JoinType.valueOf(col.joinType)) {
          case INNER:
            query.addFromInner(relTbl, relAls, join);
            break;
          case LEFT:
            query.addFromLeft(relTbl, relAls, join);
            break;
          case RIGHT:
            query.addFromRight(relTbl, relAls, join);
            break;
          case FULL:
            query.addFromFull(relTbl, relAls, join);
            break;
        }
        String colName = SqlUtils.uniqueName();
        addColumn(als, field, colName, null, null, true, parent, null, null, null);
        addColumns(relTable, relAls, col, colName, tables);

      } else if (column instanceof XmlColumns) {
        addColumns(table, alias, (XmlColumns) column, parent, tables);

      } else if (column instanceof XmlIdColumn) {
        XmlExpression xpr = new XmlName();
        xpr.type = SqlDataType.LONG.name();
        xpr.content = BeeUtils.join(".", alias, table.getIdName());
        addColumn(alias, null, column.name, null, ((XmlIdColumn) column).aggregate,
            ((XmlIdColumn) column).hidden, parent, xpr, ((XmlIdColumn) column).label, null);

      } else if (column instanceof XmlVersionColumn) {
        XmlExpression xpr = new XmlName();
        xpr.type = SqlDataType.LONG.name();
        xpr.content = BeeUtils.join(".", alias, table.getVersionName());
        addColumn(alias, null, column.name, null, ((XmlVersionColumn) column).aggregate,
            ((XmlVersionColumn) column).hidden, parent, xpr, ((XmlVersionColumn) column).label,
            null);

      } else if (column instanceof XmlSimpleColumn) {
        XmlSimpleColumn col = (XmlSimpleColumn) column;

        String colName = BeeUtils.notEmpty(col.alias, col.name);
        String aggregate = null;
        boolean hidden = col instanceof XmlHiddenColumn;

        if (col instanceof XmlAggregateColumn) {
          aggregate = ((XmlAggregateColumn) col).aggregate;
        }
        if (col.expr != null) {
          addColumn(null, null, colName, null, aggregate, hidden, null, col.expr, col.label,
              col.editable);
        } else {
          Assert.state(table.hasField(col.name), BeeUtils.joinWords("View:", getName(),
              "Unknown field name:", table.getName(), col.name));

          BeeField field = table.getField(col.name);
          addColumn(alias, field, colName, col.locale, aggregate, hidden, parent, null, col.label,
              col.editable);

          if (field.isTranslatable() && BeeUtils.allEmpty(parent, col.locale)) {
            for (SupportedLocale locale : SupportedLocale.values()) {
              if (locale.isActive() && !locale.isUserDefault()) {
                String lang = locale.getLanguage();
                addColumn(alias, field, Localized.column(colName, lang), lang, aggregate, hidden,
                    parent, null, Localized.maybeTranslate(BeeUtils.notEmpty(col.label,
                        field.getLabel()), Localizations.getGlossary(locale)), col.editable);
              }
            }
          }
        }
      }
    }
  }

  private ColumnInfo getColumnInfo(String colName) {
    Assert.state(hasColumn(colName), "Unknown view column: " + getName() + "." + colName);
    return columns.get(BeeUtils.normalize(colName));
  }

  private IsCondition getCondition(ColumnColumnFilter flt) {
    return SqlUtils.compare(
        getSqlExpression(flt.getColumn()), flt.getOperator(), getSqlExpression(flt.getValue()));
  }

  private IsCondition getCondition(ColumnInFilter flt) {
    SystemBean sys = Invocation.locateRemoteBean(SystemBean.class);

    if (sys == null) {
      return null;
    }
    BeeView inView = sys.getView(flt.getInView());

    if (inView == null) {
      logger.warning(flt.getClass().getSimpleName(), "view not found:", flt.getInView());
      return null;
    }

    String column = flt.getColumn();
    String tbl;
    String fld;

    if (BeeUtils.same(column, getSourceIdName()) || BeeUtils.same(column, ColumnInFilter.ID_TAG)) {
      tbl = getSourceAlias();
      fld = getSourceIdName();
    } else {
      tbl = getColumnTable(column);
      fld = getColumnField(column);
    }

    String inColumn = flt.getInColumn();
    String inTbl;
    String inFld;

    if (BeeUtils.same(inColumn, ColumnInFilter.ID_TAG)) {
      inTbl = inView.getSourceAlias();
      inFld = inView.getSourceIdName();
    } else {
      inTbl = inView.getColumnTable(inColumn);
      inFld = inView.getColumnField(inColumn);
    }

    Filter inFilter = flt.getInFilter();

    SqlSelect inQuery = new SqlSelect()
        .setDistinctMode(true)
        .addFields(inTbl, inFld);

    if (flt.needsFrom() || !inView.getSourceAlias().equals(inTbl)) {
      inQuery.setFrom(inView.getFrom());
    } else {
      inQuery.addFrom(inTbl);
    }

    if (inFilter != null) {
      inQuery.setWhere(inView.getCondition(inFilter));
    }

    return SqlUtils.in(tbl, fld, inQuery);
  }

  private IsCondition getCondition(ColumnIsNullFilter flt) {
    return SqlUtils.isNull(getSqlExpression(flt.getColumn()));
  }

  private IsCondition getCondition(ColumnNotNullFilter flt) {
    return SqlUtils.notNull(getSqlExpression(flt.getColumn()));
  }

  private IsCondition getCondition(ColumnValueFilter flt) {
    IsExpression expr = getSqlExpression(flt.getColumn());

    if (flt.getOperator() == Operator.IN) {
      return SqlUtils.inList(expr, flt.getValue());
    } else {
      HasConditions wh = flt.getOperator() == Operator.EQ ? SqlUtils.or() : SqlUtils.and();

      for (Value value : flt.getValue()) {
        wh.add(SqlUtils.compare(expr, flt.getOperator(), SqlUtils.constant(value)));
      }
      return wh;
    }
  }

  private IsCondition getCondition(CompoundFilter flt) {
    HasConditions condition = null;

    if (!flt.isEmpty()) {
      switch (flt.getType()) {
        case AND:
          condition = SqlUtils.and();
          break;
        case OR:
          condition = SqlUtils.or();
          break;
        case NOT:
          return SqlUtils.not(getCondition(flt.getSubFilters().get(0)));
      }
      for (Filter subFilter : flt.getSubFilters()) {
        condition.add(getCondition(subFilter));
      }
    }
    return condition;
  }

  private IsCondition getCondition(CustomFilter flt) {
    ConditionProvider provider = conditionProviders.get(flt.getKey());
    if (provider == null) {
      logger.severe("custom filter", flt, "provider not registered");
      return null;
    } else {
      return provider.getCondition(this, flt.getArgs());
    }
  }

  private IsCondition getCondition(IdFilter flt) {
    IsExpression expr = SqlUtils.field(getSourceAlias(), getSourceIdName());

    if (flt.getOperator() == Operator.IN) {
      return SqlUtils.inList(expr, flt.getValue());
    } else {
      HasConditions wh = flt.getOperator() == Operator.EQ ? SqlUtils.or() : SqlUtils.and();

      for (Long id : flt.getValue()) {
        wh.add(SqlUtils.compare(expr, flt.getOperator(), SqlUtils.constant(id)));
      }
      return wh;
    }
  }

  private IsCondition getCondition(VersionFilter flt) {
    return SqlUtils.compare(SqlUtils.field(getSourceAlias(), getSourceVersionName()),
        flt.getOperator(), SqlUtils.constant(flt.getValue()));
  }

  private IsExpression getSqlExpression(String colName) {
    IsExpression expr = getColumnExpression(colName);

    if (expr == null) {
      expr = SqlUtils.field(getColumnSource(colName), getColumnField(colName));
    }
    SqlFunction aggregate = getColumnAggregate(colName);

    if (aggregate != null) {
      expr = SqlUtils.aggregate(aggregate, expr);
    }
    return expr;
  }

  private void setColumns() {
    for (String colName : getColumnNames()) {
      query.addExpr(getSqlExpression(colName), colName);
    }
  }

  private void setFilter(SqlSelect ss, Filter flt, Long userId) {
    CompoundFilter f = Filter.and();

    if (filter != null) {
      f.add(parseFilter(filter, userId));
    }
    if (flt != null) {
      f.add(flt);
    }

    if (!f.isEmpty()) {
      IsCondition condition = getCondition(f);

      if (hasAggregate) {
        for (String col : columns.keySet()) {
          if (isColAggregate(col) && f.involvesColumn(col)) {
            ss.setHaving(condition);
            break;
          }
        }
        if (ss.getHaving() != null) {
          for (String col : columns.keySet()) {
            if (isColHidden(col) && !isColCalculated(col) && f.involvesColumn(col)) {
              ss.addGroup(getSqlExpression(col));
            }
          }
          return;
        }
      }
      ss.setWhere(condition);
    }
  }

  private void setGrouping(Set<String> groupBy) {
    Set<String> group = new LinkedHashSet<>();

    for (String col : groupBy) {
      query.addGroup(getSqlExpression(col));
    }
    for (String col : getColumnNames()) {
      if (isColAggregate(col)) {
        hasAggregate = true;
      } else if (!isColCalculated(col) && !groupBy.contains(col)) {
        group.add(col);
      }
    }
    if (hasAggregate) {
      if (!hasGrouping) {
        query.addGroup(getSourceAlias(), getSourceIdName(), getSourceVersionName());
      }
      for (String col : group) {
        query.addGroup(getSqlExpression(col));
      }
    }
  }
}
