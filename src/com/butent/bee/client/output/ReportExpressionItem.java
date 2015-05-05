package com.butent.bee.client.output;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.CustomSpan;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportExpressionItem extends ReportItem {

  private static final String EXPRESSION = "EXPRESSION";

  private List<Pair<String, ReportItem>> expression = new ArrayList<>();
  private List<Pair<String, ReportItem>> temporaryExpression = new ArrayList<>();

  public ReportExpressionItem(String caption) {
    super(BeeUtils.randomString(10), caption);
  }

  public void addExpressionItem(String sep, ReportItem item) {
    addItem(expression, sep, item);
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeMap(data);

    if (!BeeUtils.isEmpty(map)) {
      for (String itemData : Codec.beeDeserializeCollection(map.get(EXPRESSION))) {
        Pair<String, String> pair = Pair.restore(itemData);
        addItem(expression, BeeUtils.isEmpty(pair.getA())
            ? pair.getA() : Codec.decodeBase64(pair.getA()), ReportItem.restore(pair.getB()));
      }
    }
  }

  @Override
  public ReportValue evaluate(SimpleRow row) {
    List<String> values = new ArrayList<>();
    StringBuilder display = new StringBuilder();

    for (Pair<String, ReportItem> pair : expression) {
      ReportValue val = pair.getB().evaluate(row);
      values.add(val.getValue());

      if (!BeeUtils.isEmpty(val.toString())) {
        if (display.length() > 0) {
          display.append(pair.getA());
        }
        display.append(val);
      }
    }
    return ReportValue.of(values.toString(), display.toString());
  }

  @Override
  public String getCaption() {
    String cap = super.getCaption();

    if (Objects.equals(cap, getExpression())) {
      cap = BeeConst.STRING_EMPTY;
    }
    return cap;
  }

  @Override
  public Widget getExpressionWidget(Report report) {
    Flow container = new Flow(getStyle() + "-expression");
    temporaryExpression.clear();
    temporaryExpression.addAll(expression);
    render(container, report);
    return container;
  }

  @Override
  public String getFormatedCaption() {
    String cap = getCaption();

    if (BeeUtils.isEmpty(cap)) {
      StringBuilder display = new StringBuilder();

      for (Pair<String, ReportItem> pair : expression) {
        if (display.length() > 0) {
          display.append(pair.getA());
        }
        display.append(BeeUtils.embrace(pair.getB().getFormatedCaption()));
      }
      cap = display.toString();
    }
    return cap;
  }

  @Override
  public List<ReportItem> getMembers() {
    List<ReportItem> members = new ArrayList<>();

    for (Pair<String, ReportItem> pair : expression) {
      members.addAll(pair.getB().getMembers());
    }
    return members;
  }

  @Override
  public String getStyle() {
    return STYLE_TEXT;
  }

  @Override
  public String saveOptions() {
    if (BeeUtils.isEmpty(temporaryExpression)) {
      return Localized.getMessages().dataNotAvailable(Localized.getConstants().expression());
    }
    expression.clear();
    expression.addAll(temporaryExpression);
    return super.saveOptions();
  }

  @Override
  public String serialize() {
    List<Pair<String, ReportItem>> list = new ArrayList<>();

    for (Pair<String, ReportItem> pair : expression) {
      list.add(Pair.of(BeeUtils.isEmpty(pair.getA())
          ? pair.getA() : Codec.encodeBase64(pair.getA()), pair.getB()));
    }
    return serialize(Codec.beeSerialize(Collections.singletonMap(EXPRESSION, list)));
  }

  private static void addItem(List<Pair<String, ReportItem>> list, String sep, ReportItem item) {
    if (item != null) {
      list.add(Pair.of(encodeSpaces(BeeUtils.nvl(sep, BeeConst.STRING_SPACE)), item));
    }
  }

  private static String encodeSpaces(String value) {
    return value.replace(BeeConst.CHAR_SPACE, BeeConst.CHAR_NBSP);
  }

  private void render(final Flow container, final Report report) {
    final Runnable refresh = new Runnable() {
      @Override
      public void run() {
        render(container, report);
      }
    };
    container.clear();

    for (int i = 0; i < temporaryExpression.size(); i++) {
      final Pair<String, ReportItem> pair = temporaryExpression.get(i);

      if (i > 0) {
        Label sep = new Label(pair.getA());
        sep.addStyleName(getStyle() + "-separator");
        sep.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            final InputText input = new InputText();
            input.setValue(pair.getA());

            Global.inputWidget(Localized.getConstants().separator(), input, new InputCallback() {
              @Override
              public void onSuccess() {
                pair.setA(encodeSpaces(input.getValue()));
                refresh.run();
              }
            });
            input.setFocus(true);
          }
        });
        container.add(sep);
      }
      container.add(ReportItem.renderDnd(pair.getB(), temporaryExpression, i, report, refresh));
    }
    CustomSpan add = new CustomSpan(STYLE_ADD);
    add.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        chooseItem(report, false, new Consumer<ReportItem>() {
          @Override
          public void accept(ReportItem item) {
            addItem(temporaryExpression, null, item);
            refresh.run();
          }
        });
      }
    });
    container.add(add);
  }
}
