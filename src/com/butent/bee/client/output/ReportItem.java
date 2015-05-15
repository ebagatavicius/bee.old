package com.butent.bee.client.output;

import com.google.common.base.Predicates;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.ChoiceCallback;
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
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.ReportFunction;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

public abstract class ReportItem implements BeeSerializable {

  protected static final String STYLE_PREFIX = "bee-rep-";

  private static final String STYLE_ITEM = STYLE_PREFIX + "item";
  protected static final String STYLE_ADD = STYLE_ITEM + "-add";
  private static final String STYLE_REMOVE = STYLE_ITEM + "-remove";
  private static final String STYLE_CAPTION = STYLE_ITEM + "-cap";
  private static final String STYLE_OPTION_CAPTION = STYLE_PREFIX + "option-cap";

  public static final String STYLE_BOOLEAN = STYLE_PREFIX + "boolean";
  public static final String STYLE_DATE = STYLE_PREFIX + "date";
  public static final String STYLE_DATETIME = STYLE_PREFIX + "datetime";
  public static final String STYLE_ENUM = STYLE_PREFIX + "enum";
  public static final String STYLE_NUM = STYLE_PREFIX + "num";
  public static final String STYLE_TEXT = STYLE_PREFIX + "text";

  private enum Serial {
    CLAZZ, NAME, CAPTION, EXPRESSION, DATA
  }

  private final String name;
  private String caption;
  private String expression;

  protected ReportItem(String name, String caption) {
    this.name = Assert.notEmpty(name);
    setCaption(caption);
    setExpression(getName());
  }

  @SuppressWarnings("unchecked")
  public Object calculate(Object total, ReportValue value, ReportFunction function) {
    if (value != null && !BeeUtils.isEmpty(value.getValue())) {
      switch (function) {
        case COUNT:
          if (total == null) {
            return 1;
          }
          return (int) total + 1;
        case LIST:
          if (total == null) {
            return new TreeSet<>(Arrays.asList(value));
          }
          ((Collection<ReportValue>) total).add(value);
          break;
        case MAX:
          if (total == null || value.compareTo((ReportValue) total) > 0) {
            return value;
          }
          break;
        case MIN:
          if (total == null || value.compareTo((ReportValue) total) < 0) {
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

  public static void chooseItem(final Report report, Boolean numeric,
      final Consumer<ReportItem> consumer) {

    final List<ReportItem> items = new ArrayList<>();
    final List<String> options = new ArrayList<>();

    boolean other = numeric == null || !numeric;
    boolean number = numeric == null || numeric;

    for (ReportItem item : report.getItems()) {
      if ((item instanceof ReportNumericItem) && number
          || !(item instanceof ReportNumericItem) && other
          || (item instanceof ReportDateItem) && number && !other) {
        items.add(item);
        options.add(item.getCaption());
      }
    }
    if (other) {
      options.add(Localized.getConstants().expression() + "...");
    }
    if (number) {
      options.add(Localized.getConstants().formula() + "...");

      if (!other) {
        options.add(Localized.getConstants().constant() + "...");
      }
    }
    Global.choice(null, null, options, new ChoiceCallback() {
      @Override
      public void onSuccess(int value) {
        if (BeeUtils.isIndex(items, value)) {
          consumer.accept(items.get(value));
        } else {
          final ReportItem item;

          if (options.get(value).equals(Localized.getConstants().formula() + "...")) {
            item = new ReportFormulaItem(null);
          } else if (options.get(value).equals(Localized.getConstants().constant() + "...")) {
            item = new ReportConstantItem(null, null);
          } else {
            item = new ReportExpressionItem(null);
          }
          item.edit(report, new Runnable() {
            @Override
            public void run() {
              consumer.accept(item);
            }
          });
          return;
        }
      }
    });
  }

  public void clearFilter() {
  }

  public ReportItem clone() {
    return restore(serialize());
  }

  @Override
  public void deserialize(String data) {
  }

  public void edit(Report report, final Runnable onSave) {
    HtmlTable table = new HtmlTable();
    table.setColumnCellClasses(0, STYLE_OPTION_CAPTION);
    int c = 0;

    final InputText cap = new InputText();
    cap.setValue(getCaption());

    table.setText(c, 0, Localized.getConstants().name());
    table.setWidget(c++, 1, cap);

    Widget expr = getExpressionWidget(report);

    if (expr != null) {
      table.setText(c, 0, Localized.getConstants().expression());
      table.setWidget(c++, 1, expr);
    }

    if (getOptionsWidget() != null) {
      table.setText(c, 0, getOptionsCaption());
      table.setWidget(c++, 1, getOptionsWidget());
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
    return Objects.equals(getName(), ((ReportItem) obj).getName());
  }

  public abstract ReportValue evaluate(SimpleRow row);

  public EnumSet<ReportFunction> getAvailableFunctions() {
    return EnumSet.of(ReportFunction.MIN, ReportFunction.MAX, ReportFunction.COUNT,
        ReportFunction.LIST);
  }

  public String getCaption() {
    return BeeUtils.notEmpty(caption, getExpression());
  }

  public String getExpression() {
    return expression;
  }

  @SuppressWarnings("unused")
  public Widget getExpressionWidget(Report report) {
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
    return Objects.hashCode(getName());
  }

  public DndWidget render(final Report report, final Runnable onRemove, final Runnable onSave) {
    Flow box = new Flow(STYLE_ITEM);

    InlineLabel label = new InlineLabel(getFormatedCaption());
    label.addStyleName(STYLE_CAPTION);
    box.add(label);

    if (onRemove != null) {
      CustomDiv remove = new CustomDiv(STYLE_REMOVE);
      remove.setText(String.valueOf(BeeConst.CHAR_TIMES));

      remove.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          onRemove.run();
        }
      });
      box.add(remove);
    }
    box.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        edit(report, onSave);
      }
    });
    return box;
  }

  public static <T> Widget renderDnd(ReportItem item, final List<T> collection, final int idx,
      Report report, final Runnable onUpdate) {

    DndWidget widget = item.render(report, new Runnable() {
      @Override
      public void run() {
        collection.remove(idx);

        if (onUpdate != null) {
          onUpdate.run();
        }
      }
    }, onUpdate);
    String contentType = Integer.toHexString(collection.hashCode());

    DndHelper.makeSource(widget, contentType, idx, null);
    DndHelper.makeTarget(widget, Arrays.asList(contentType), STYLE_ITEM + "-over",
        Predicates.not(Predicates.equalTo((Object) idx)), new BiConsumer<DropEvent, Object>() {
          @Override
          public void accept(DropEvent ev, Object index) {
            T element = collection.remove((int) index);

            if (idx > collection.size()) {
              collection.add(element);
            } else {
              collection.add(idx, element);
            }
            if (onUpdate != null) {
              onUpdate.run();
            }
          }
        });
    return widget.asWidget();
  }

  public static ReportItem restore(String data) {
    if (BeeUtils.isEmpty(data)) {
      return null;
    }
    Map<String, String> map = Codec.deserializeMap(data);
    String clazz = map.get(Serial.CLAZZ.name());
    String name = map.get(Serial.NAME.name());
    String caption = map.get(Serial.CAPTION.name());

    ReportItem item = null;

    if (NameUtils.getClassName(ReportBooleanItem.class).equals(clazz)) {
      item = new ReportBooleanItem(name, caption);

    } else if (NameUtils.getClassName(ReportDateItem.class).equals(clazz)) {
      item = new ReportDateItem(name, caption);

    } else if (NameUtils.getClassName(ReportDateTimeItem.class).equals(clazz)) {
      item = new ReportDateTimeItem(name, caption);

    } else if (NameUtils.getClassName(ReportEnumItem.class).equals(clazz)) {
      item = new ReportEnumItem(name, caption);

    } else if (NameUtils.getClassName(ReportNumericItem.class).equals(clazz)) {
      item = new ReportNumericItem(name, caption);

    } else if (NameUtils.getClassName(ReportTextItem.class).equals(clazz)) {
      item = new ReportTextItem(name, caption);

    } else if (NameUtils.getClassName(ReportExpressionItem.class).equals(clazz)) {
      item = new ReportExpressionItem(caption);

    } else if (NameUtils.getClassName(ReportFormulaItem.class).equals(clazz)) {
      item = new ReportFormulaItem(caption);

    } else if (NameUtils.getClassName(ReportConstantItem.class).equals(clazz)) {
      item = new ReportConstantItem(null, caption);

    } else {
      Assert.unsupported("Unsupported class name: " + clazz);
    }
    item.setExpression(map.get(Serial.EXPRESSION.name()));
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

  @SuppressWarnings("unchecked")
  public Object summarize(Object total, Object value, ReportFunction function) {
    if (value != null) {
      if (total == null) {
        if (function == ReportFunction.LIST) {
          return new TreeSet<>((Collection<ReportValue>) value);
        }
        return value;
      }
      switch (function) {
        case COUNT:
          return (int) total + (int) value;
        case LIST:
          ((Collection<ReportValue>) total).addAll((Collection<ReportValue>) value);
          break;
        default:
          return calculate(total, (ReportValue) value, function);
      }
    }
    return total;
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
