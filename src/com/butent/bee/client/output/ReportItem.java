package com.butent.bee.client.output;

import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndWidget;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.report.ResultCalculator;
import com.butent.bee.shared.report.ResultHolder;
import com.butent.bee.shared.report.ResultValue;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class ReportItem implements BeeSerializable {

  protected static final String STYLE_PREFIX = "bee-rep-";

  private static final String STYLE_ITEM = STYLE_PREFIX + "item";
  protected static final String STYLE_ADD = STYLE_ITEM + "-add";
  private static final String STYLE_REMOVE = STYLE_ITEM + "-remove";
  private static final String STYLE_CAPTION = STYLE_ITEM + "-cap";
  private static final String STYLE_OPTION_CAPTION = STYLE_PREFIX + "option-cap";

  public static final String STYLE_BOOLEAN = STYLE_PREFIX + "boolean";
  public static final String STYLE_DATE = STYLE_PREFIX + "date";
  public static final String STYLE_ENUM = STYLE_PREFIX + "enum";
  public static final String STYLE_NUM = STYLE_PREFIX + "num";
  public static final String STYLE_TEXT = STYLE_PREFIX + "text";

  private enum Serial {
    CLAZZ, NAME, CAPTION, EXPRESSION, DATA
  }

  private String name = BeeUtils.randomString(10);
  private String caption;
  private String expression;

  protected ReportItem(String expression, String caption) {
    setCaption(caption);
    setExpression(Assert.notEmpty(expression));
  }

  @SuppressWarnings("unchecked")
  public Object calculate(Object total, ResultValue value, ReportFunction function) {
    if (!BeeUtils.isEmpty(value.toString())) {
      switch (function) {
        case COUNT:
          ResultCalculator<Integer> calculator;

          if (total == null) {
            calculator = new ResultCalculator<Integer>() {
              @Override
              public ResultCalculator calculate(ReportFunction fnc, Integer val) {
                setResult(BeeUtils.unbox(getResult()) + val);
                return this;
              }
            };
          } else {
            calculator = (ResultCalculator<Integer>) total;
          }
          return calculator.calculate(function, 1);
        case LIST:
          if (total == null) {
            return new TreeSet(Collections.singleton(value)) {
              @Override
              public String toString() {
                String s = super.toString();
                return BeeUtils.isDelimited(s, '[', ']') ? s.substring(1, s.length() - 1) : s;
              }
            };
          }
          ((Collection<ResultValue>) total).add(value);
          break;
        case MAX:
          if (total == null || value.compareTo((ResultValue) total) == BeeConst.COMPARE_MORE) {
            return value;
          }
          break;
        case MIN:
          if (total == null || value.compareTo((ResultValue) total) == BeeConst.COMPARE_LESS) {
            return value;
          }
          break;
        case SUM:
          Assert.unsupported();
          break;
      }
    }
    return total;
  }

  public static void chooseItem(List<ReportItem> items, Boolean numeric,
      Consumer<ReportItem> consumer) {

    List<String> options = items.stream().map(ReportItem::getCaption).collect(Collectors.toList());

    boolean text = numeric == null || !numeric;
    boolean number = numeric == null || numeric;

    String formulaCap = Localized.dictionary().formula() + "...";
    String constantCap = Localized.dictionary().constant() + "...";

    if (text) {
      options.add(Localized.dictionary().expression() + "...");
      options.add(constantCap);
    }
    if (number) {
      options.add(formulaCap);

      if (!text) {
        options.add(constantCap);
      }
    }
    Global.choice(null, null, options, value -> {
      if (BeeUtils.isIndex(items, value)) {
        consumer.accept(items.get(value));
      } else {
        final ReportItem item;

        if (options.get(value).equals(formulaCap)) {
          item = new ReportFormulaItem(null);
        } else if (options.get(value).equals(constantCap)) {
          item = text ? new ReportTextConstantItem(null, null) : new ReportConstantItem(null, null);
        } else {
          item = new ReportExpressionItem(null);
        }
        item.edit(items, () -> consumer.accept(item));
      }
    });
  }

  public void clearFilter() {
  }

  public ReportItem copy() {
    return restore(serialize());
  }

  @Override
  public void deserialize(String data) {
  }

  public void edit(List<ReportItem> reportItems, Runnable onSave) {
    HtmlTable table = new HtmlTable();
    table.setColumnCellClasses(0, STYLE_OPTION_CAPTION);
    int c = 0;

    final InputText cap = new InputText();
    cap.setValue(getCaption());

    table.setText(c, 0, Localized.dictionary().name());
    table.setWidget(c++, 1, cap);

    Widget expr = getExpressionWidget(reportItems);

    if (expr != null) {
      table.setText(c, 0, getExpressionCaption());
      table.setWidget(c++, 1, expr);
    }
    expr = getOptionsWidget();

    if (expr != null) {
      table.setText(c, 0, getOptionsCaption());
      table.setWidget(c, 1, expr);
    }
    Global.inputWidget(getCaption(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        return saveOptions();
      }

      @Override
      public void onSuccess() {
        setCaption(cap.getValue());

        if (onSave != null) {
          onSave.run();
        }
      }
    });
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ReportItem)) {
      return false;
    }
    return Objects.equals(getExpression(), ((ReportItem) obj).getExpression());
  }

  public abstract ResultValue evaluate(SimpleRow row, Dictionary dictionary);

  public ResultValue evaluate(ResultValue rowGroup, ResultValue[] rowValues, ResultValue colGroup,
      ResultHolder resultHolder) {
    Assert.unsupported();
    return null;
  }

  public EnumSet<ReportFunction> getAvailableFunctions() {
    return EnumSet.of(ReportFunction.MIN, ReportFunction.MAX, ReportFunction.COUNT,
        ReportFunction.LIST);
  }

  public String getCaption() {
    return BeeUtils.notEmpty(caption, getExpression());
  }

  public String getExpressionCaption() {
    return Localized.dictionary().expression();
  }

  public String getExpression() {
    return expression;
  }

  @SuppressWarnings("unused")
  public Widget getExpressionWidget(List<ReportItem> reportItems) {
    Label xpr = new Label(getExpression());
    xpr.addStyleName("bee-output");
    return xpr;
  }

  public Object getFilter() {
    return null;
  }

  public Widget getFilterWidget() {
    return null;
  }

  public String getFormatedCaption() {
    return getCaption();
  }

  public List<ReportItem> getMembers() {
    return Collections.singletonList(this);
  }

  public String getName() {
    return name;
  }

  public String getOptionsCaption() {
    return null;
  }

  public Widget getOptionsWidget() {
    return null;
  }

  public abstract String getStyle();

  @Override
  public int hashCode() {
    return Objects.hashCode(getExpression());
  }

  public boolean isResultItem() {
    return false;
  }

  public DndWidget render(List<ReportItem> reportItems, Runnable onRemove, Runnable onSave) {
    Flow box = new Flow(STYLE_ITEM);

    InlineLabel label = new InlineLabel(getFormatedCaption());
    label.addStyleName(STYLE_CAPTION);
    box.add(label);

    if (onRemove != null) {
      CustomDiv remove = new CustomDiv(STYLE_REMOVE);
      remove.setText(String.valueOf(BeeConst.CHAR_TIMES));

      remove.addClickHandler(event -> onRemove.run());
      box.add(remove);
    }
    box.addClickHandler(event -> edit(reportItems, onSave));
    return box;
  }

  public static <T> Widget renderDnd(ReportItem item, List<T> collection, int idx,
      List<ReportItem> reportItems, Runnable onUpdate) {

    DndWidget widget = item.render(reportItems, () -> {
      collection.remove(idx);

      if (onUpdate != null) {
        onUpdate.run();
      }
    }, onUpdate);
    String contentType = Integer.toHexString(collection.hashCode());

    DndHelper.makeSource(widget, contentType, idx, null);
    DndHelper.makeTarget(widget, Collections.singletonList(contentType), STYLE_ITEM + "-over",
        o -> !Objects.equals(o, idx), (ev, index) -> {
          T element = collection.remove((int) index);

          if (idx > collection.size()) {
            collection.add(element);
          } else {
            collection.add(idx, element);
          }
          if (onUpdate != null) {
            onUpdate.run();
          }
        });
    return widget.asWidget();
  }

  public static ReportItem restore(String data) {
    if (BeeUtils.isEmpty(data)) {
      return null;
    }
    Map<String, String> map = Codec.deserializeLinkedHashMap(data);
    String clazz = map.get(Serial.CLAZZ.name());
    String expression = map.get(Serial.EXPRESSION.name());
    String caption = map.get(Serial.CAPTION.name());

    ReportItem item = null;

    if (NameUtils.getClassName(ReportBooleanItem.class).equals(clazz)) {
      item = new ReportBooleanItem(expression, caption);

    } else if (NameUtils.getClassName(ReportDateItem.class).equals(clazz)) {
      item = new ReportDateItem(expression, caption);

    } else if (NameUtils.getClassName(ReportDateTimeItem.class).equals(clazz)) {
      item = new ReportDateTimeItem(expression, caption);

    } else if (NameUtils.getClassName(ReportEnumItem.class).equals(clazz)) {
      item = new ReportEnumItem(expression, caption);

    } else if (NameUtils.getClassName(ReportNumericItem.class).equals(clazz)) {
      item = new ReportNumericItem(expression, caption);

    } else if (NameUtils.getClassName(ReportTimeDurationItem.class).equals(clazz)) {
      item = new ReportTimeDurationItem(expression, caption);

    } else if (NameUtils.getClassName(ReportTextItem.class).equals(clazz)) {
      item = new ReportTextItem(expression, caption);

    } else if (NameUtils.getClassName(ReportTextConstantItem.class).equals(clazz)) {
      item = new ReportTextConstantItem(expression, caption);

    } else if (NameUtils.getClassName(ReportExpressionItem.class).equals(clazz)) {
      item = new ReportExpressionItem(caption);

    } else if (NameUtils.getClassName(ReportFormulaItem.class).equals(clazz)) {
      item = new ReportFormulaItem(caption);

    } else if (NameUtils.getClassName(ReportConstantItem.class).equals(clazz)) {
      item = new ReportConstantItem(BeeUtils.toDecimalOrNull(expression), caption);

    } else if (NameUtils.getClassName(ReportResultItem.class).equals(clazz)) {
      item = new ReportResultItem(expression, caption);

    } else {
      Assert.unsupported("Unsupported class name: " + clazz);
    }
    item.name = map.get(Serial.NAME.name());
    item.deserialize(map.get(Serial.DATA.name()));

    return item;
  }

  public String saveOptions() {
    return null;
  }

  @Override
  public String serialize() {
    return serialize(null);
  }

  public ReportItem setCaption(String cap) {
    this.caption = cap;
    return this;
  }

  public ReportItem setExpression(String xpr) {
    this.expression = xpr;
    return this;
  }

  @SuppressWarnings("unused")
  public ReportItem setFilter(String value) {
    return this;
  }

  public boolean validate(SimpleRow row) {
    return row != null;
  }

  protected String serialize(String data) {
    Map<String, Object> map = new HashMap<>();

    for (Serial key : Serial.values()) {
      Object value = null;

      switch (key) {
        case CAPTION:
          value = getCaption();
          break;
        case CLAZZ:
          value = NameUtils.getClassName(this.getClass());
          break;
        case DATA:
          value = data;
          break;
        case EXPRESSION:
          value = getExpression();
          break;
        case NAME:
          value = getName();
          break;
      }
      map.put(key.name(), value);
    }
    return Codec.beeSerialize(map);
  }
}
