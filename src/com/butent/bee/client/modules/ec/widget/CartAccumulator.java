package com.butent.bee.client.modules.ec.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputInteger;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.modules.ec.EcUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

public class CartAccumulator extends HtmlTable implements HasKeyDownHandlers {

  private static final String STYLE_PREFIX = EcStyles.name("cartAccumulator-");

  private static final String STYLE_PANEL = STYLE_PREFIX + "panel";
  private static final String STYLE_INPUT = STYLE_PREFIX + "input";
  private static final String STYLE_SPIN = STYLE_PREFIX + "spin";
  private static final String STYLE_PLUS = STYLE_PREFIX + "plus";
  private static final String STYLE_MINUS = STYLE_PREFIX + "minus";
  private static final String STYLE_ADD = STYLE_PREFIX + "add";
  private static final String STYLE_COUNT = STYLE_PREFIX + "count";

  private final InputInteger input;

  private final long articleId;

  public CartAccumulator(final EcItem item, int quantity) {
    super(STYLE_PANEL);

    this.articleId = item.getArticleId();

    int row = 0;
    int col = 0;

    this.input = new InputInteger();
    input.setValue(quantity);

    input.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          int value = input.getIntValue();

          if (value > 0 && DomUtils.isInView(input)) {
            EcKeeper.addToCart(item, value);
            input.setValue(0);

            onAddToCart();
          }
        }
      }
    });

    EcKeeper.bindKeyPress(input);

    setWidgetAndStyle(row, col++, input, STYLE_INPUT);

    Flow spin = new Flow();

    FaLabel plus = new FaLabel(FontAwesome.PLUS_SQUARE_O);
    plus.addStyleName(STYLE_PLUS);

    plus.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        int value = Math.max(input.getIntValue() + 1, 1);
        input.setValue(value);
      }
    });
    spin.add(plus);

    FaLabel minus = new FaLabel(FontAwesome.MINUS_SQUARE_O);
    minus.addStyleName(STYLE_MINUS);

    minus.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        int value = Math.max(input.getIntValue() - 1, 0);
        input.setValue(value);
      }
    });
    spin.add(minus);

    setWidgetAndStyle(row, col++, spin, STYLE_SPIN);

    Image cart = new Image(EcUtils.imageUrl("shoppingcart_add.png"));
    cart.setAlt("cart");

    cart.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        int value = input.getIntValue();
        if (value > 0) {
          EcKeeper.addToCart(item, value);
          input.setValue(0);

          onAddToCart();
        }
      }
    });

    setWidgetAndStyle(row, col++, cart, STYLE_ADD);

    int count = EcKeeper.getQuantityInCart(articleId);
    if (count > 0) {
      renderCount(count);
    }
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return input.addKeyDownHandler(handler);
  }

  public void focus() {
    input.setFocus(true);
  }

  public long getArticleId() {
    return articleId;
  }

  public InputInteger getInput() {
    return input;
  }

  public void renderCount(int count) {
    int row = 1;
    int col = 0;

    CustomDiv widget = new CustomDiv();
    if (count > 0) {
      widget.setText(Localized.getMessages().ecInMyCart(count));
    }

    if (getRowCount() > row) {
      widget.addStyleName(STYLE_COUNT);
      setWidget(row, col, widget);
    } else {
      setWidgetAndStyle(row, col, widget, STYLE_COUNT);
      getCellFormatter().setColSpan(row, col, getCellCount(0));
    }
  }

  private void onAddToCart() {
    int count = EcKeeper.getQuantityInCart(articleId);
    renderCount(count);

    if (UiHelper.isModal(this) && BeeKeeper.getScreen().getScreenPanel() != null) {
      Collection<? extends CartAccumulator> accumulators =
          UiHelper.getChildren(BeeKeeper.getScreen().getScreenPanel(), getClass());

      if (!BeeUtils.isEmpty(accumulators)) {
        for (CartAccumulator accumulator : accumulators) {
          if (accumulator.articleId == articleId) {
            accumulator.renderCount(count);
          }
        }
      }
    }
  }
}
