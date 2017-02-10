package com.butent.bee.client.grid;

import com.google.gwt.i18n.client.NumberFormat;

import com.butent.bee.client.grid.cell.FooterCell;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.shared.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.i18n.HasDateTimeFormat;
import com.butent.bee.client.i18n.HasNumberFormat;
import com.butent.bee.client.modules.trade.DiscountRenderer;
import com.butent.bee.client.modules.trade.TotalRenderer;
import com.butent.bee.client.modules.trade.VatRenderer;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.style.HasTextAlign;
import com.butent.bee.client.style.HasVerticalAlign;
import com.butent.bee.client.style.HasWhiteSpace;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.HasScale;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.css.values.VerticalAlign;
import com.butent.bee.shared.css.values.WhiteSpace;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.HasRowValue;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.DateTimeValue;
import com.butent.bee.shared.data.value.DateValue;
import com.butent.bee.shared.data.value.HasValueType;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.FooterDescription;
import com.butent.bee.shared.ui.HasValueFormatter;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;
import java.util.function.Function;

public class ColumnFooter extends Header<String> implements HasTextAlign, HasVerticalAlign,
    HasWhiteSpace, HasDateTimeFormat, HasNumberFormat, HasScale, HasOptions, HasValueType,
    HasValueFormatter {

  public enum Aggregate {
    SUM, COUNT, MIN, MAX, AVG
  }

  private enum EvaluatorType {
    TOTAL {
      @Override
      HasRowValue getFunction(List<? extends IsColumn> columns) {
        return new TotalRenderer(columns);
      }
    },
    VAT {
      @Override
      HasRowValue getFunction(List<? extends IsColumn> columns) {
        return new VatRenderer(columns);
      }
    },
    DISCOUNT {
      @Override
      HasRowValue getFunction(List<? extends IsColumn> columns) {
        return new DiscountRenderer(columns);
      }
    };

    abstract HasRowValue getFunction(List<? extends IsColumn> columns);
  }

  private static final Aggregate DEFAULT_AGGREGATE = Aggregate.SUM;
  private static final ValueType DEFAULT_VALUE_TYPE = ValueType.NUMBER;

  private String html;

  private CellSource cellSource;
  private HasRowValue rowEvaluator;
  private ValueType valueType;

  private TextAlign textAlign;
  private VerticalAlign verticalAlign;

  private WhiteSpace whiteSpace;

  private DateTimeFormat dateTimeFormat;
  private NumberFormat numberFormat;

  private Function<Value, String> valueFormatter;

  private int scale = BeeConst.UNDEF;

  private String options;

  private Aggregate aggregate;

  public ColumnFooter(CellSource cellSource, AbstractColumn<?> column,
      ColumnDescription columnDescription, List<? extends IsColumn> dataColumns) {

    this(cellSource);
    init(column, columnDescription, dataColumns);
  }

  private ColumnFooter(CellSource cellSource) {
    super(new FooterCell());
    this.cellSource = cellSource;
  }

  public boolean dependsOnSource(String source) {
    if (BeeUtils.isEmpty(source)) {
      return false;
    } else if (getAggregate() == null) {
      return false;
    } else if (getRowEvaluator() != null) {
      return getRowEvaluator().dependsOnSource(source);
    } else if (getCellSource() != null) {
      return BeeUtils.same(getCellSource().getName(), source);
    } else {
      return false;
    }
  }

  public Aggregate getAggregate() {
    return aggregate;
  }

  public CellSource getCellSource() {
    return cellSource;
  }

  @Override
  public DateTimeFormat getDateTimeFormat() {
    return dateTimeFormat;
  }

  public String getHtml() {
    return html;
  }

  @Override
  public NumberFormat getNumberFormat() {
    return numberFormat;
  }

  @Override
  public String getOptions() {
    return options;
  }

  public HasRowValue getRowEvaluator() {
    return rowEvaluator;
  }

  @Override
  public int getScale() {
    return scale;
  }

  @Override
  public TextAlign getTextAlign() {
    return textAlign;
  }

  @Override
  public String getValue(CellContext context) {
    return (context == null) ? null : reduce(context.getGrid().getRowData());
  }

  @Override
  public Function<Value, String> getValueFormatter() {
    return valueFormatter;
  }

  @Override
  public ValueType getValueType() {
    return valueType;
  }

  @Override
  public VerticalAlign getVerticalAlign() {
    return verticalAlign;
  }

  @Override
  public WhiteSpace getWhiteSpace() {
    return whiteSpace;
  }

  public String reduce(List<IsRow> data) {
    if (getAggregate() != null && !BeeUtils.isEmpty(data)) {
      Value value = calculate(data);
      if (value == null && BeeUtils.contains(getOptions(), BeeConst.CHAR_ZERO)) {
        value = new NumberValue(BeeConst.DOUBLE_ZERO);
      }

      if (value != null) {
        if (getValueFormatter() != null) {
          return getValueFormatter().apply(value);
        } else {
          return Format.render(value.getString(), getValueType(),
              getDateTimeFormat(), getNumberFormat(), getScale());
        }
      }
    }

    return getHtml();
  }

  public void setAggregate(Aggregate aggregate) {
    this.aggregate = aggregate;
  }

  public void setCellSource(CellSource cellSource) {
    this.cellSource = cellSource;
  }

  @Override
  public void setDateTimeFormat(DateTimeFormat dateTimeFormat) {
    this.dateTimeFormat = dateTimeFormat;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  @Override
  public void setNumberFormat(NumberFormat numberFormat) {
    this.numberFormat = numberFormat;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setRowEvaluator(HasRowValue rowEvaluator) {
    this.rowEvaluator = rowEvaluator;
  }

  @Override
  public void setScale(int scale) {
    this.scale = scale;
  }

  @Override
  public void setTextAlign(TextAlign textAlign) {
    this.textAlign = textAlign;
  }

  public void setValueFormatter(Function<Value, String> valueFormatter) {
    this.valueFormatter = valueFormatter;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  @Override
  public void setVerticalAlign(VerticalAlign verticalAlign) {
    this.verticalAlign = verticalAlign;
  }

  @Override
  public void setWhiteSpace(WhiteSpace whiteSpace) {
    this.whiteSpace = whiteSpace;
  }

  protected Value calculate(List<IsRow> data) {
    double total = BeeConst.DOUBLE_ZERO;
    long count = 0;
    Value reduced = null;

    for (IsRow row : data) {
      Value value = getRowValue(row);
      if (value != null && !value.isNull()) {
        switch (getAggregate()) {
          case AVG:
            total += value.getDouble();
            break;

          case COUNT:
            break;

          case MAX:
            if (reduced == null || BeeUtils.isMore(value, reduced)) {
              reduced = value;
            }
            break;

          case MIN:
            if (reduced == null || BeeUtils.isLess(value, reduced)) {
              reduced = value;
            }
            break;

          case SUM:
            total += value.getDouble();
            break;
        }
        count++;
      }
    }

    if (count <= 0) {
      return null;
    }

    switch (getAggregate()) {
      case AVG:
        if (ValueType.isNumeric(getValueType())) {
          return new NumberValue(total / count);
        } else if (getValueType() == ValueType.DATE) {
          return new DateValue(new JustDate(BeeUtils.toInt(total / count)));
        } else if (getValueType() == ValueType.DATE_TIME) {
          return new DateTimeValue(new DateTime(BeeUtils.toLong(total / count)));
        } else {
          return null;
        }

      case COUNT:
        return new LongValue(count);

      case MAX:
      case MIN:
        return reduced;

      case SUM:
        return new NumberValue(total);
    }
    return null;
  }

  protected Value getRowValue(IsRow row) {
    if (row == null) {
      return null;
    } else if (getRowEvaluator() != null) {
      return getRowEvaluator().getRowValue(row);
    } else if (getCellSource() != null) {
      return getCellSource().getValue(row);
    } else {
      return null;
    }
  }

  protected void init(AbstractColumn<?> column, ColumnDescription columnDescription,
      List<? extends IsColumn> dataColumns) {

    Assert.notNull(column);
    Assert.notNull(columnDescription);

    FooterDescription footerDescription = columnDescription.getFooterDescription();

    if (footerDescription != null) {
      if (!BeeUtils.isEmpty(footerDescription.getText())) {
        setHtml(Localized.maybeTranslate(footerDescription.getText()));
      } else if (!BeeUtils.isEmpty(footerDescription.getHtml())) {
        setHtml(footerDescription.getHtml());
      }

      if (!BeeUtils.isEmpty(footerDescription.getType())) {
        setValueType(ValueType.getByTypeCode(footerDescription.getType()));
      }

      if (!BeeUtils.isEmpty(footerDescription.getHorAlign())) {
        UiHelper.setHorizontalAlignment(this, footerDescription.getHorAlign());
      }
      if (!BeeUtils.isEmpty(footerDescription.getVertAlign())) {
        UiHelper.setVerticalAlignment(this, footerDescription.getVertAlign());
      }

      if (!BeeUtils.isEmpty(footerDescription.getWhiteSpace())) {
        UiHelper.setWhiteSpace(this, footerDescription.getWhiteSpace());
      }

      if (footerDescription.getScale() != null) {
        setScale(footerDescription.getScale());
      }

      if (!BeeUtils.isEmpty(footerDescription.getOptions())) {
        setOptions(footerDescription.getOptions());
      }

      if (!BeeUtils.isEmpty(footerDescription.getAggregate())) {
        setAggregate(EnumUtils.getEnumByName(Aggregate.class, footerDescription.getAggregate()));
      }

      EvaluatorType evaluatorType = EnumUtils.getEnumByName(EvaluatorType.class,
          footerDescription.getEvaluator());

      if (evaluatorType != null) {
        if (getAggregate() == null) {
          setAggregate(DEFAULT_AGGREGATE);
        }
        setRowEvaluator(evaluatorType.getFunction(dataColumns));

      } else {
        String expression = footerDescription.getExpression();
        if (!BeeUtils.isEmpty(expression) && getAggregate() == null) {
          setAggregate(DEFAULT_AGGREGATE);
        }

        if (getAggregate() != null) {
          Calculation calculation;

          if (BeeUtils.isEmpty(expression)) {
            calculation = columnDescription.getRender();
          } else {
            calculation = new Calculation(expression, null);
          }

          if (calculation != null) {
            setRowEvaluator(Evaluator.create(calculation, null, dataColumns));

          } else if (column instanceof HasCellRenderer) {
            AbstractCellRenderer renderer = ((HasCellRenderer) column).getRenderer();
            if (renderer instanceof HasRowValue) {
              setRowEvaluator((HasRowValue) renderer);
            }
          }
        }
      }
    }

    if (BeeConst.isUndef(getScale())) {
      if (column instanceof HasScale) {
        setScale(((HasScale) column).getScale());
      } else if (columnDescription.getScale() != null) {
        setScale(columnDescription.getScale());
      } else if (getCellSource() != null) {
        setScale(getCellSource().getScale());
      }
    }

    if (getValueType() == null) {
      if (getAggregate() == Aggregate.COUNT) {
        setValueType(ValueType.LONG);
      } else if (column.getValueType() != null) {
        setValueType(column.getValueType());
      } else if (columnDescription.getValueType() != null) {
        setValueType(columnDescription.getValueType());
      } else if (getCellSource() != null && getCellSource().getValueType() != null) {
        setValueType(getCellSource().getValueType());
      } else if (getAggregate() != null) {
        setValueType(DEFAULT_VALUE_TYPE);
      }
    }

    if (getTextAlign() == null) {
      if (getAggregate() == Aggregate.COUNT) {
        setTextAlign(TextAlign.RIGHT);
      } else if (getAggregate() != null && getValueType() != null) {
        UiHelper.setDefaultHorizontalAlignment(this, getValueType());
      } else if (column.getTextAlign() != null) {
        setTextAlign(column.getTextAlign());
      }
    }

    if (getVerticalAlign() == null) {
      setVerticalAlign(VerticalAlign.MIDDLE);
    }

    if (getWhiteSpace() == null) {
      if (getAggregate() == Aggregate.COUNT) {
        setWhiteSpace(WhiteSpace.NOWRAP);
      } else if (getAggregate() != null && getValueType() != null) {
        UiHelper.setDefaultWhiteSpace(this, getValueType());
      } else if (column.getWhiteSpace() != null && getValueType() == column.getValueType()) {
        setWhiteSpace(column.getWhiteSpace());
      }
    }

    if (footerDescription != null && !BeeUtils.isEmpty(footerDescription.getFormat())
        && getValueType() != null) {
      Format.setFormat(this, getValueType(), footerDescription.getFormat());
    }

    if (getValueType() == column.getValueType()) {
      if (getDateTimeFormat() == null && column instanceof HasDateTimeFormat) {
        setDateTimeFormat(((HasDateTimeFormat) column).getDateTimeFormat());
      }

      if (getNumberFormat() == null && column instanceof HasNumberFormat
          && (footerDescription == null || footerDescription.getScale() == null)) {
        setNumberFormat(((HasNumberFormat) column).getNumberFormat());
      }
    }

    if (column instanceof HasCellRenderer) {
      AbstractCellRenderer renderer = ((HasCellRenderer) column).getRenderer();
      if (renderer instanceof HasValueFormatter) {
        setValueFormatter(((HasValueFormatter) renderer).getValueFormatter());
      }
    }
  }
}
