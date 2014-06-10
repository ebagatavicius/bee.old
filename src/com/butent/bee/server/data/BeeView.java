package com.butent.bee.server.data;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.server.data.BeeTable.BeeRelation;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlBuilderFactory;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeConst.SqlEngine;
import com.butent.bee.shared.HasExtendedInfo;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
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
import com.butent.bee.shared.data.XmlView.XmlExternalJoin;
import com.butent.bee.shared.data.XmlView.XmlHiddenColumn;
import com.butent.bee.shared.data.XmlView.XmlIdColumn;
import com.butent.bee.shared.data.XmlView.XmlOrder;
import com.butent.bee.shared.data.XmlView.XmlSimpleColumn;
import com.butent.bee.shared.data.XmlView.XmlSimpleJoin;
import com.butent.bee.shared.data.filter.ColumnColumnFilter;
import com.butent.bee.shared.data.filter.ColumnInFilter;
import com.butent.bee.shared.data.filter.ColumnIsNullFilter;
import com.butent.bee.shared.data.filter.ColumnNotNullFilter;
import com.butent.bee.shared.data.filter.ColumnValueFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
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
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.ExtendedProperty;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements database view management - contains parameters for views and their fields and methods
 * for doing operations with them.
 */

public class BeeView implements BeeObject, HasExtendedInfo {

  public interface ConditionProvider {
    IsCondition getCondition(BeeView view, List<String> args);
  }

  public interface ViewFinder extends Function<String, BeeView> {
  }

  private class ColumnInfo {
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

    public ColumnInfo(String alias, BeeField field, String colName, String locale,
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
        level = level + parent.getLevel();
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

    public Boolean isEditable() {
      return editable;
    }

    public boolean isHidden() {
      return hidden;
    }

    public boolean isNullable() {
      return field == null || !field.isNotNull();
    }

    public boolean isReadOnly() {
      return isHidden() || getAggregate() != null || getExpression() != null;
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
          List<IsExpression> cases = Lists.newArrayList();

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
        List<IsExpression> members = Lists.newArrayList();

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
        List<Object> x = Lists.newArrayList();
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
    INNER, RIGHT, LEFT, FULL;
  }

  private static BeeLogger logger = LogUtils.getLogger(BeeView.class);

  private static final Map<String, ConditionProvider> conditionProviders =
      new ConcurrentHashMap<>();

  public static void registerConditionProvider(String key, ConditionProvider provider) {
    Assert.notEmpty(key);
    Assert.notNull(provider);

    conditionProviders.put(key, provider);
  }

  private static void initColumn(ColumnInfo info, BeeColumn column) {
    column.setId(info.getName());
    column.setLabel(BeeUtils.notEmpty(info.getLabel(), info.getName()));

    column.setType(info.getType().toValueType());
    column.setNullable(info.isNullable());

    column.setPrecision(info.getPrecision());
    column.setScale(info.getScale());

    boolean ro = info.isReadOnly();
    column.setReadOnly(ro);

    int level = info.getLevel();
    column.setLevel(level);

    Boolean editable = info.isEditable();
    if (editable == null) {
      editable = !ro && level <= 0;
    }
    column.setEditable(editable);

    column.setDefaults(info.getDefaults());

    column.setEnumKey(info.getEnumKey());
  }

  private final String module;
  private final String name;
  private final BeeTable source;
  private final String sourceAlias;

  private final boolean readOnly;

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
  private final Map<String, ColumnInfo> columns = Maps.newLinkedHashMap();
  private final Filter filter;

  private Order order;

  BeeView(String module, XmlView xmlView, Map<String, BeeTable> tables, Long userId) {
    Assert.notNull(xmlView);
    this.module = BeeUtils.notEmpty(xmlView.module, module);
    this.name = xmlView.name;
    Assert.notEmpty(name);

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

    if (BeeUtils.isEmpty(xmlView.filter)) {
      this.filter = null;
    } else {
      this.filter = parseFilter(xmlView.filter, userId);
    }

    if (!BeeUtils.isEmpty(xmlView.orders)) {
      this.order = new Order();

      for (XmlOrder ord : xmlView.orders) {
        order.add(ord.column, !ord.descending);
      }
    }
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

  public Pair<DefaultExpression, Object> getColumnDefaults(String colName) {
    return getColumnInfo(colName).getDefaults();
  }

  public String getColumnEnumKey(String colName) {
    return getColumnInfo(colName).getEnumKey();
  }

  public IsExpression getColumnExpression(String colName) {
    return getColumnInfo(colName).getExpression();
  }

  public String getColumnField(String colName) {
    return getColumnInfo(colName).getField();
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
    Collection<String> cols = Lists.newArrayList();

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
    return getColumnInfo(colName).getTable();
  }

  public SqlDataType getColumnType(String colName) {
    return getColumnInfo(colName).getType();
  }

  public IsCondition getCondition(Filter flt, ViewFinder viewFinder) {
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
        return getCondition((CompoundFilter) flt, viewFinder);

      } else if (NameUtils.getClassName(IdFilter.class).equals(clazz)) {
        return getCondition((IdFilter) flt);

      } else if (NameUtils.getClassName(VersionFilter.class).equals(clazz)) {
        return getCondition((VersionFilter) flt);

      } else if (NameUtils.getClassName(IsFalseFilter.class).equals(clazz)) {
        return SqlUtils.sqlFalse();

      } else if (NameUtils.getClassName(IsTrueFilter.class).equals(clazz)) {
        return SqlUtils.sqlTrue();

      } else if (NameUtils.getClassName(ColumnInFilter.class).equals(clazz)) {
        return getCondition((ColumnInFilter) flt, viewFinder);

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
    List<ExtendedProperty> info = Lists.newArrayList();

    PropertyUtils.addProperties(info, false, "Module", getModule(), "Name", getName(),
        "Source", getSourceName(), "Source Alias", getSourceAlias(),
        "Source Id Name", getSourceIdName(), "Source Version Name", getSourceVersionName(),
        "Filter", getFilter(), "Read Only", isReadOnly(), "Caption", getCaption(),
        "Edit Form", getEditForm(), "Row Caption", getRowCaption(), "New Row Form",
        getNewRowForm(),
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
          "Level", getColumnLevel(col),
          "Expression", isColCalculated(col) ? getColumnExpression(col)
              .getSqlString(SqlBuilderFactory.getBuilder(SqlEngine.GENERIC)) : null,
          "Parent Column", getColumnParent(col), "Owner Alias", getColumnOwner(col),
          "Label", getColumnLabel(col), "Enum key", getColumnEnumKey(col));
    }
    if (order != null) {
      info.add(new ExtendedProperty("Orders", BeeUtils.toString(order.getSize())));
      i = 0;
      for (Order.Column ordCol : order.getColumns()) {
        String key = BeeUtils.joinWords("Order", ++i, ordCol.isAscending() ? "" : "DESC");
        PropertyUtils.addChildren(info, key, "Sources", ordCol.getSources());
      }
    }
    return info;
  }

  public Filter getFilter() {
    return filter;
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

  public SqlSelect getQuery() {
    return getQuery(null, null);
  }

  public SqlSelect getQuery(Filter flt, Order ord, List<String> cols, ViewFinder viewFinder) {
    SqlSelect ss = query.copyOf();
    Collection<String> activeCols = null;

    if (!BeeUtils.isEmpty(cols)) {
      ss.resetFields();
      activeCols = Sets.newHashSet();

      for (String col : cols) {
        if (!isColHidden(col)) {
          String colName = getColumnName(col);
          ss.addExpr(getSqlExpression(col), colName);
          activeCols.add(colName);
        }
      }
    }
    setFilter(ss, flt, viewFinder);

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
            if (isColAggregate(col) || isColCalculated(col)) {
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

          if (!ordCol.isAscending()) {
            ss.addOrderDesc(alias, colName);
          } else {
            ss.addOrder(alias, colName);
          }
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

  public SqlSelect getQuery(Filter flt, ViewFinder viewFinder) {
    return getQuery(flt, null, null, viewFinder);
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
    List<BeeColumn> result = Lists.newArrayList();

    for (ColumnInfo info : columns.values()) {
      if (!info.isHidden()) {
        BeeColumn column = new BeeColumn();
        initColumn(info, column);
        result.add(column);
      }
    }

    return result;
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

  public List<ViewColumn> getViewColumns() {
    List<ViewColumn> result = Lists.newArrayList();

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

  public void initColumn(String colName, BeeColumn column) {
    initColumn(getColumnInfo(colName), column);
  }

  public boolean isColAggregate(String colName) {
    return getColumnAggregate(colName) != null;
  }

  public boolean isColCalculated(String colName) {
    return getColumnExpression(colName) != null;
  }

  public Boolean isColEditable(String colName) {
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
        BeeUtils.joinWords("Dublicate column name:", getName(), colName));

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

  private void addColumns(BeeTable table, String alias, Collection<XmlColumn> cols, String parent,
      Map<String, BeeTable> tables) {
    Assert.notNull(table);
    Assert.notEmpty(alias);
    Assert.notEmpty(cols);

    for (XmlColumn column : cols) {
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
          join = SqlUtils.join(alias, table.getIdName(), relAls, field.getName());
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
        addColumns(relTable, relAls, col.columns, colName, tables);

      } else if (column instanceof XmlIdColumn) {
        XmlExpression xpr = new XmlName();
        xpr.type = SqlDataType.LONG.name();
        xpr.content = BeeUtils.join(".", alias, table.getIdName());
        addColumn(alias, null, column.name, null, ((XmlIdColumn) column).aggregate, false, parent,
            xpr, null, null);

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
          addColumn(alias, table.getField(col.name), colName, col.locale, aggregate, hidden,
              parent, null, col.label, col.editable);
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

  private IsCondition getCondition(ColumnInFilter flt, ViewFinder viewFinder) {
    if (viewFinder == null) {
      logger.warning(flt.getClass().getSimpleName(), "view finder not supplied");
      return null;
    }
    BeeView inView = viewFinder.apply(flt.getInView());
    if (inView == null) {
      logger.warning(flt.getClass().getSimpleName(), "view not found:", flt.getInView());
      return null;
    }

    String column = flt.getColumn();

    String tbl;
    String fld;

    if (BeeUtils.same(column, getSourceIdName())) {
      tbl = getSourceAlias();
      fld = getSourceIdName();
    } else {
      tbl = getColumnTable(column);
      fld = getColumnField(column);
    }

    String inTbl = inView.getColumnTable(flt.getInColumn());
    String inFld = inView.getColumnField(flt.getInColumn());

    Filter inFilter = flt.getInFilter();

    return SqlUtils.in(tbl, fld, inTbl, inFld, inView.getCondition(inFilter, viewFinder));
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

  private IsCondition getCondition(CompoundFilter flt, ViewFinder viewFinder) {
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
          return SqlUtils.not(getCondition(flt.getSubFilters().get(0), viewFinder));
      }
      for (Filter subFilter : flt.getSubFilters()) {
        condition.add(getCondition(subFilter, viewFinder));
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

  private void setFilter(SqlSelect ss, Filter flt, ViewFinder viewFinder) {
    CompoundFilter f = Filter.and();

    if (filter != null) {
      f.add(filter);
    }
    if (flt != null) {
      f.add(flt);
    }

    if (!f.isEmpty()) {
      IsCondition condition = getCondition(f, viewFinder);

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
    Set<String> group = Sets.newLinkedHashSet();

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
