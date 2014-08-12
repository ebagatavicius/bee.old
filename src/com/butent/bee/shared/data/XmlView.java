// CHECKSTYLE:OFF
package com.butent.bee.shared.data;

import com.butent.bee.shared.utils.NullOrdering;

import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Handles views information storage in XML structure.
 */

@XmlRootElement(name = "View", namespace = DataUtils.VIEW_NAMESPACE)
public class XmlView {

  @XmlSeeAlso({XmlSimpleColumn.class, XmlHiddenColumn.class, XmlIdColumn.class,
      XmlAggregateColumn.class, XmlSimpleJoin.class, XmlExternalJoin.class})
  public abstract static class XmlColumn {
    @XmlAttribute
    public String name;
  }

  @XmlRootElement(name = "SimpleColumn", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlSimpleColumn extends XmlColumn {
    @XmlAttribute
    public String alias;
    @XmlAttribute
    public String locale;
    @XmlElementRef
    public XmlExpression expr;
    @XmlAttribute
    public String label;
    @XmlAttribute
    public Boolean editable;
  }

  @XmlRootElement(name = "HiddenColumn", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlHiddenColumn extends XmlSimpleColumn {
  }

  @XmlRootElement(name = "AggregateColumn", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlAggregateColumn extends XmlSimpleColumn {
    @XmlAttribute
    public String aggregate;
  }

  @XmlRootElement(name = "IdColumn", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlIdColumn extends XmlColumn {
    @XmlAttribute
    public String aggregate;
  }

  @XmlRootElement(name = "SimpleJoin", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlSimpleJoin extends XmlColumn {
    @XmlAttribute
    public String joinType;
    @XmlAttribute
    public String filter;
    @XmlElementRef
    public Collection<XmlColumn> columns;
  }

  @XmlRootElement(name = "ExternalJoin", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlExternalJoin extends XmlSimpleJoin {
    @XmlAttribute
    public String source;
  }

  @XmlRootElement(name = "GroupBy", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlGroup {
    @XmlAttribute
    public Set<String> columns;
  }

  @XmlRootElement(name = "OrderBy", namespace = DataUtils.VIEW_NAMESPACE)
  public static class XmlOrder {
    @XmlAttribute
    public String column;
    @XmlAttribute
    public boolean descending;
    @XmlAttribute
    public NullOrdering nulls;
  }

  @XmlAttribute
  public String module;
  @XmlAttribute
  public String name;
  @XmlAttribute
  public String source;

  @XmlAttribute
  public String filter;

  @XmlAttribute
  public boolean readOnly;

  @XmlAttribute
  public String caption;

  @XmlAttribute
  public String editForm;
  @XmlAttribute
  public String rowCaption;

  @XmlAttribute
  public String newRowForm;
  @XmlAttribute
  public String newRowColumns;
  @XmlAttribute
  public String newRowCaption;

  @XmlAttribute
  public Integer cacheMaximumSize;
  @XmlAttribute
  public String cacheEviction;

  @XmlAnyElement
  public Object relation;

  @XmlElementWrapper(name = "Columns", namespace = DataUtils.VIEW_NAMESPACE)
  @XmlElementRef
  public Collection<XmlColumn> columns;

  @XmlElementRef
  public XmlGroup groupBy;

  @XmlElementWrapper(name = "Order", namespace = DataUtils.VIEW_NAMESPACE)
  @XmlElementRef
  public Collection<XmlOrder> orders;
}
