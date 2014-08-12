// CHECKSTYLE:OFF
package com.butent.bee.shared.data;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.butent.bee.shared.data.Defaults.DefaultExpression;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Handles data table information storage in XML structure.
 */
@XmlRootElement(name = "Table", namespace = DataUtils.TABLE_NAMESPACE)
public class XmlTable {

  public static abstract class XmlSqlEngine {
    @XmlElement(name = "PostgreSql", namespace = DataUtils.TABLE_NAMESPACE)
    public String postgreSql;
    @XmlElement(name = "MsSql", namespace = DataUtils.TABLE_NAMESPACE)
    public String msSql;
    @XmlElement(name = "Oracle", namespace = DataUtils.TABLE_NAMESPACE)
    public String oracle;
  }

  @XmlSeeAlso({XmlBoolean.class,
      XmlInteger.class, XmlLong.class, XmlDouble.class, XmlNumeric.class,
      XmlChar.class, XmlString.class, XmlText.class, XmlBlob.class,
      XmlDate.class, XmlDateTime.class,
      XmlRelation.class, XmlEnum.class})
  public abstract static class XmlField extends XmlSqlEngine {
    @XmlAttribute
    public String name;
    @XmlAttribute
    public String type;
    @XmlAttribute
    public Integer precision;
    @XmlAttribute
    public Integer scale;
    @XmlAttribute
    public boolean notNull;
    @XmlAttribute
    public boolean unique;
    @XmlAttribute
    public DefaultExpression defExpr;
    @XmlAttribute
    public String defValue;
    @XmlAttribute
    public boolean translatable;
    @XmlAttribute
    public String label;
    @XmlAttribute
    public boolean audit;

    private boolean safe;

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      XmlField other = (XmlField) obj;

      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!BeeUtils.same(name, other.name)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    public boolean isProtected() {
      return safe;
    }
  }

  @XmlRootElement(name = "Boolean", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlBoolean extends XmlField {
  }

  @XmlRootElement(name = "Integer", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlInteger extends XmlField {
  }

  @XmlRootElement(name = "Long", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlLong extends XmlField {
  }

  @XmlRootElement(name = "Double", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlDouble extends XmlField {
  }

  @XmlRootElement(name = "Numeric", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlNumeric extends XmlField {
  }

  @XmlRootElement(name = "Char", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlChar extends XmlField {
  }

  @XmlRootElement(name = "String", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlString extends XmlField {
  }

  @XmlRootElement(name = "Text", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlText extends XmlField {
  }

  @XmlRootElement(name = "Blob", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlBlob extends XmlField {
  }

  @XmlRootElement(name = "Date", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlDate extends XmlField {
  }

  @XmlRootElement(name = "DateTime", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlDateTime extends XmlField {
  }

  @XmlRootElement(name = "Relation", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlRelation extends XmlField {
    @XmlAttribute
    public String relation;
    @XmlTransient
    public String relationField;
    @XmlAttribute
    public String cascade;
    @XmlAttribute
    public boolean editable;
  }

  @XmlRootElement(name = "Enum", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlEnum extends XmlField {
    @XmlAttribute
    public String key;
  }

  @XmlSeeAlso({XmlCheck.class, XmlUnique.class, XmlReference.class})
  public static abstract class XmlConstraint extends XmlSqlEngine {
  }

  @XmlRootElement(name = "Check", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlCheck extends XmlConstraint {
  }

  @XmlRootElement(name = "Unique", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlUnique extends XmlConstraint {
    @XmlAttribute
    public List<String> fields;
  }

  @XmlRootElement(name = "Reference", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlReference extends XmlConstraint {
    @XmlAttribute
    public List<String> fields;
    @XmlAttribute
    public String refTable;
    @XmlAttribute
    public List<String> refFields;
    @XmlAttribute
    public String cascade;
  }

  @XmlRootElement(name = "Index", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlIndex extends XmlSqlEngine {
    @XmlAttribute
    public List<String> fields;
    @XmlAttribute
    public boolean unique;
  }

  @XmlRootElement(name = "Trigger", namespace = DataUtils.TABLE_NAMESPACE)
  public static class XmlTrigger extends XmlSqlEngine {
    @XmlAttribute
    public String timing;
    @XmlAttribute
    public List<String> events;
    @XmlAttribute
    public String scope;
  }

  @XmlAttribute
  public String name;
  @XmlAttribute
  public int idChunk;
  @XmlAttribute
  public String idName;
  @XmlAttribute
  public String versionName;
  @XmlAttribute
  public boolean audit;
  @XmlAttribute
  public int x;
  @XmlAttribute
  public int y;

  @XmlElementWrapper(name = "Fields", namespace = DataUtils.TABLE_NAMESPACE)
  @XmlElementRef
  public List<XmlField> fields;

  @XmlElementWrapper(name = "Indexes", namespace = DataUtils.TABLE_NAMESPACE)
  @XmlElementRef
  public Set<XmlIndex> indexes;

  @XmlElementWrapper(name = "Constraints", namespace = DataUtils.TABLE_NAMESPACE)
  @XmlElementRef
  public Set<XmlConstraint> constraints;

  @XmlElementWrapper(name = "Triggers", namespace = DataUtils.TABLE_NAMESPACE)
  @XmlElement(name = "Trigger", namespace = DataUtils.TABLE_NAMESPACE)
  public Set<XmlTrigger> triggers;

  private boolean safe = false;

  public XmlTable getChanges(XmlTable otherTable) {
    XmlTable diff = null;

    if (otherTable != null) {
      boolean upd = false;
      diff = new XmlTable();
      diff.name = name;

      upd = upd || !Objects.equal(x, otherTable.x);
      diff.x = otherTable.x;

      upd = upd || !Objects.equal(y, otherTable.y);
      diff.y = otherTable.y;

      if (!BeeUtils.isEmpty(otherTable.fields)) {
        for (XmlField field : otherTable.fields) {
          if (!isProtected(field)) {
            if (diff.fields == null) {
              diff.fields = Lists.newArrayList(field);

            } else if (diff.findField(field) == null) {
              diff.fields.add(field);
            }
            upd = true;
          }
        }
      }
      if (!upd) {
        diff = null;
      }
    }
    return diff;
  }

  public boolean isProtected() {
    return safe;
  }

  public boolean isProtected(XmlField field) {
    XmlField fld = findField(field);

    if (fld != null) {
      return fld.isProtected();
    }
    return false;
  }

  public void merge(XmlTable otherTable) {
    XmlTable diff = getChanges(otherTable);

    if (diff != null) {
      x = diff.x;
      y = diff.y;

      if (!BeeUtils.isEmpty(diff.fields)) {
        if (fields == null) {
          fields = Lists.newArrayList();
        }
        for (XmlField field : diff.fields) {
          removeField(field);
          fields.add(field);
        }
      }
    }
  }

  public XmlTable protect() {
    if (!BeeUtils.isEmpty(fields)) {
      for (XmlField field : fields) {
        field.safe = true;
      }
    }
    this.safe = true;
    return this;
  }

  private XmlField findField(XmlField field) {
    if (!BeeUtils.isEmpty(fields)) {
      for (XmlField fld : fields) {
        if (Objects.equal(field, fld)) {
          return fld;
        }
      }
    }
    return null;
  }

  private void removeField(XmlField field) {
    if (!BeeUtils.isEmpty(fields)) {
      fields.remove(field);
    }
  }
}
