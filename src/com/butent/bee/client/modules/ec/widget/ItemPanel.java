package com.butent.bee.client.modules.ec.widget;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.ec.EcBrand;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Set;

public class ItemPanel extends Flow implements SelectionHandler<TreeItem> {

  private static final BeeLogger logger = LogUtils.getLogger(ItemPanel.class);

  private static final String STYLE_PRIMARY = "ItemPanel";
  private static final String STYLE_CATEGORIES = STYLE_PRIMARY + "-categories";
  private static final String STYLE_BRANDS_AND_ITEMS = STYLE_PRIMARY + "-brandsAndItems";
  private static final String STYLE_BRANDS = STYLE_PRIMARY + "-brands";
  private static final String STYLE_ITEMS = STYLE_PRIMARY + "-items";

  private static final String STYLE_CATEGORY = STYLE_PRIMARY + "-category";
  private static final String STYLE_BRAND = STYLE_PRIMARY + "-brand";

  private static final String STYLE_WRAPPER = "wrapper";
  private static final String STYLE_CAPTION = "caption";
  private static final String STYLE_CONTAINER = "container";
  private static final String STYLE_SELECTABLE = "selectable";
  private static final String STYLE_LABEL = "label";
  private static final String STYLE_SELECTED = "selected";
  private static final String STYLE_TREE = "tree";

  private static Set<Long> getBrands(List<EcItem> input) {
    Set<Long> result = Sets.newHashSet();

    int size = input.size();
    for (int i = 0; i < size; i++) {
      Long brand = input.get(i).getBrand();
      if (brand != null) {
        result.add(brand);
      }
    }

    return result;
  }

  private final List<EcItem> items = Lists.newArrayList();
  private final Set<Long> categories = Sets.newHashSet();

  private final Set<Long> brands = Sets.newHashSet();
  private final Set<Long> selectedCategories = Sets.newHashSet();

  private final Set<Long> selectedBrands = Sets.newHashSet();
  private Flow brandWrapper;

  private ItemList itemWrapper;

  public ItemPanel() {
    super(EcStyles.name(STYLE_PRIMARY));
  }

  @Override
  public void clear() {
    super.clear();

    if (!items.isEmpty()) {
      items.clear();
      categories.clear();
      brands.clear();

      selectedCategories.clear();
      selectedBrands.clear();
    }
  }

  @Override
  public void onSelection(SelectionEvent<TreeItem> event) {
    Object userObject = event.getSelectedItem().getUserObject();
    Long category = (userObject instanceof Long) ? (Long) userObject : null;

    selectedCategories.clear();
    if (category != null) {
      selectedCategories.add(category);
    }
    refreshBrandsAndItems();
  }

  public void render(List<EcItem> ecItems) {
    long millis = System.currentTimeMillis();

    if (!isEmpty()) {
      clear();
    }

    int size = ecItems.size();
    for (int i = 0; i < size; i++) {
      EcItem ecItem = ecItems.get(i);
      items.add(ecItem);

      categories.addAll(ecItem.getCategorySet());

      Long brand = ecItem.getBrand();
      if (brand != null && !brands.contains(brand)) {
        brands.add(brand);
      }
    }

    boolean debug = EcKeeper.isDebug();
    if (debug) {
      logger.debug("cat", categories.size(), "man", brands.size(), TimeUtils.elapsedMillis(millis));
      millis = System.currentTimeMillis();
    }

    boolean showCategories = size > 1 && !categories.isEmpty();
    boolean showBrands = size > 1 && !brands.isEmpty();

    if (showCategories) {
      add(renderCategories());
      if (debug) {
        logger.debug("cat rendered", TimeUtils.elapsedMillis(millis));
        millis = System.currentTimeMillis();
      }
    }

    Flow brandsAndItems = new Flow(EcStyles.name(STYLE_BRANDS_AND_ITEMS, STYLE_WRAPPER));

    if (showBrands) {
      this.brandWrapper = new Flow();
      EcStyles.add(brandWrapper, STYLE_BRANDS, STYLE_WRAPPER);

      renderBrands(brands);
      brandsAndItems.add(brandWrapper);
      if (debug) {
        logger.debug("man rendered", TimeUtils.elapsedMillis(millis));
        millis = System.currentTimeMillis();
      }
    } else {
      this.brandWrapper = null;
    }

    this.itemWrapper = new ItemList(items);
    EcStyles.add(itemWrapper, STYLE_ITEMS, STYLE_WRAPPER);
    brandsAndItems.add(itemWrapper);

    add(brandsAndItems);

    if (debug) {
      logger.debug("items rendered", TimeUtils.elapsedMillis(millis));
    }
  }

  private List<EcItem> filterByBrand(List<EcItem> input) {
    if (selectedBrands.isEmpty() || selectedBrands.size() >= brands.size()) {
      return input;

    } else {
      List<EcItem> result = Lists.newArrayList();
      for (EcItem item : input) {
        if (selectedBrands.contains(item.getBrand())) {
          result.add(item);
        }
      }
      return result;
    }
  }

  private List<EcItem> filterByCategory(List<EcItem> input) {
    if (selectedCategories.isEmpty() || selectedCategories.size() >= categories.size()) {
      return input;

    } else {
      List<EcItem> result = Lists.newArrayList();
      for (EcItem item : input) {
        if (BeeUtils.intersects(selectedCategories, item.getCategorySet())) {
          result.add(item);
        }
      }
      return result;
    }
  }

  private void refreshBrandsAndItems() {
    List<EcItem> filteredByCategory = filterByCategory(items);

    if (brandWrapper == null) {
      itemWrapper.render(filteredByCategory);
    } else {
      Set<Long> filteredBrands = getBrands(filteredByCategory);
      if (!selectedBrands.isEmpty()) {
        selectedBrands.retainAll(filteredBrands);
      }

      renderBrands(filteredBrands);
      itemWrapper.render(filterByBrand(filteredByCategory));
    }
  }

  private void refreshItems() {
    itemWrapper.render(filterByBrand(filterByCategory(items)));
  }

  private Widget renderBrand(final Long brand, String name, boolean selectable) {
    if (selectable) {
      final CheckBox checkBox = new CheckBox(name);
      EcStyles.add(checkBox, STYLE_BRAND, STYLE_SELECTABLE);

      if (selectedBrands.contains(brand)) {
        checkBox.setValue(true);
        checkBox.addStyleName(EcStyles.name(STYLE_BRAND, STYLE_SELECTED));
      }

      checkBox.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          boolean selected = checkBox.getValue();
          checkBox.setStyleName(EcStyles.name(STYLE_BRAND, STYLE_SELECTED), selected);

          if (selected) {
            selectedBrands.add(brand);
          } else {
            selectedBrands.remove(brand);
          }
          refreshItems();
        }
      });

      return checkBox;

    } else {
      Label label = new Label(EcKeeper.getBrandName(brand));
      EcStyles.add(label, STYLE_BRAND, STYLE_LABEL);
      return label;
    }
  }

  private void renderBrands(final Set<Long> input) {
    final boolean selectable = items.size() > 1 && input.size() > 1;

    if (!brandWrapper.isEmpty()) {
      brandWrapper.clear();
    }
    if (selectable) {
      brandWrapper.setStyleName(EcStyles.name(STYLE_BRANDS, STYLE_SELECTABLE), selectable);
    }

    if (!input.isEmpty()) {
      String caption = selectable ? Localized.getConstants().ecSelectBrand()
          : Localized.getConstants().ecItemBrand();
      Label label = new Label(caption);
      EcStyles.add(label, STYLE_BRANDS, STYLE_CAPTION);
      brandWrapper.add(label);
    }

    final Flow container = new Flow();
    EcStyles.add(container, STYLE_BRANDS, STYLE_CONTAINER);

    brandWrapper.add(container);

    if (input.size() > 1) {
      EcKeeper.getItemBrands(new Consumer<List<EcBrand>>() {
        @Override
        public void accept(List<EcBrand> allBrands) {
          for (EcBrand brand : allBrands) {
            if (input.contains(brand.getId())) {
              container.add(renderBrand(brand.getId(), brand.getName(), selectable));
            }
          }
        }
      });

    } else {
      for (Long brand : input) {
        container.add(renderBrand(brand, EcKeeper.getBrandName(brand), selectable));
      }
    }
  }

  private Widget renderCategories() {
    boolean selectable = items.size() > 1 && categories.size() > 1;

    Flow panel = new Flow();
    EcStyles.add(panel, STYLE_CATEGORIES, STYLE_WRAPPER);
    if (selectable) {
      EcStyles.add(panel, STYLE_CATEGORIES, STYLE_SELECTABLE);
    }

    if (!selectable) {
      Label caption = new Label(Localized.getConstants().ecItemCategory());
      EcStyles.add(caption, STYLE_CATEGORIES, STYLE_CAPTION);
      panel.add(caption);
    }

    Flow container = new Flow();
    EcStyles.add(container, STYLE_CATEGORIES, STYLE_CONTAINER);

    if (selectable) {
      Tree tree = EcKeeper.buildCategoryTree(categories);
      EcStyles.add(tree, STYLE_CATEGORIES, STYLE_TREE);

      tree.addSelectionHandler(this);

      container.add(tree);

    } else {
      for (Long category : categories) {
        Label label = new Label(EcKeeper.getCategoryName(category));
        EcStyles.add(label, STYLE_CATEGORY, STYLE_LABEL);

        container.add(label);
      }
    }

    panel.add(container);

    return panel;
  }
}
