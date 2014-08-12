package com.butent.bee.client.modules.ec.view;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.layout.FieldSet;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.modules.ec.widget.ItemList;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Legend;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.SelectableValue;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcBrand;
import com.butent.bee.shared.modules.ec.EcCriterion;
import com.butent.bee.shared.modules.ec.EcGroup;
import com.butent.bee.shared.modules.ec.EcGroupFilters;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class SearchByGroup extends EcView implements HasCaption {

  private static final String STYLE_NAME = "searchByGroup";

  private static final String STYLE_GROUP_SELECTED = EcStyles.name(STYLE_NAME, "groupSelected");
  private static final String STYLE_BRAND_SELECTED = EcStyles.name(STYLE_NAME, "brandSelected");
  private static final String STYLE_VALUE_SELECTED = EcStyles.name(STYLE_NAME, "valueSelected");

  private static Widget renderBrands(List<EcBrand> brands) {
    Legend legend = new Legend(Localized.getConstants().ecItemBrand());
    EcStyles.add(legend, STYLE_NAME, "brandsLagend");

    FieldSet panel = new FieldSet(EcStyles.name(STYLE_NAME, "brands"), legend);

    Flow container = new Flow(EcStyles.name(STYLE_NAME, "brandsContainer"));

    for (final EcBrand brand : brands) {
      final CheckBox checkBox = new CheckBox(brand.getName());
      EcStyles.add(checkBox, STYLE_NAME, "brand");

      if (brand.isSelected()) {
        checkBox.setValue(true);
        checkBox.addStyleName(STYLE_BRAND_SELECTED);
      }

      checkBox.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          boolean selected = checkBox.getValue();
          checkBox.setStyleName(STYLE_BRAND_SELECTED, selected);

          brand.setSelected(selected);
        }
      });

      container.add(checkBox);
    }

    panel.add(container);

    return panel;
  }

  private static Widget renderCriterion(EcCriterion criterion) {
    Legend legend = new Legend(criterion.getName());
    EcStyles.add(legend, STYLE_NAME, "criterionCaption");

    FieldSet panel = new FieldSet(EcStyles.name(STYLE_NAME, "criterion"), legend);

    Flow container = new Flow(EcStyles.name(STYLE_NAME, "valuesContainer"));

    for (final SelectableValue selectableValue : criterion.getValues()) {
      final CheckBox checkBox = new CheckBox(selectableValue.getValue());
      EcStyles.add(checkBox, STYLE_NAME, "value");

      if (selectableValue.isSelected()) {
        checkBox.setValue(true);
        checkBox.addStyleName(STYLE_VALUE_SELECTED);
      }

      checkBox.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          boolean selected = checkBox.getValue();
          checkBox.setStyleName(STYLE_VALUE_SELECTED, selected);

          selectableValue.setSelected(selected);
        }
      });

      container.add(checkBox);
    }

    panel.add(container);

    return panel;
  }

  private final boolean moto;

  private final List<EcGroup> groups = Lists.newArrayList();

  private int groupIndex = BeeConst.UNDEF;
  private final Map<Long, EcGroupFilters> filters = Maps.newHashMap();

  private final FieldSet groupPanel;

  private final Flow filtersAndItemsPanel;

  SearchByGroup(boolean moto) {
    super();
    this.moto = moto;

    this.groupPanel = new FieldSet(EcStyles.name(STYLE_NAME, "groups"));
    this.filtersAndItemsPanel = new Flow(EcStyles.name(STYLE_NAME, "filtersAndItems"));
  }

  @Override
  public String getCaption() {
    return moto ? Localized.getConstants().ecBikeItems() : Localized.getConstants().ecGroups();
  }

  @Override
  protected void createUi() {
    ParameterList params = EcKeeper.createArgs(SVC_GET_ITEM_GROUPS);
    if (moto) {
      params.addQueryItem(COL_GROUP_MOTO, 1);
    }

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        EcKeeper.dispatchMessages(response);

        if (response.hasResponse()) {
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());
          if (arr != null) {
            for (String s : arr) {
              groups.add(EcGroup.restore(s));
            }
          }
        }

        render();
      }
    });
  }

  @Override
  protected String getPrimaryStyle() {
    return STYLE_NAME;
  }

  private int getGroupIndex() {
    return groupIndex;
  }

  private void refreshItems(EcGroup group, EcGroupFilters groupFilters) {
    ParameterList params = EcKeeper.createArgs(SVC_GET_GROUP_ITEMS);
    params.addQueryItem(COL_GROUP, group.getId());
    if (groupFilters != null) {
      params.addDataItem(VAR_FILTER, groupFilters.serialize());
    }

    EcKeeper.requestItems(SVC_GET_GROUP_ITEMS, group.getName(), params,
        new Consumer<List<EcItem>>() {
          @Override
          public void accept(List<EcItem> items) {
            renderItems(items);
          }
        });
  }

  private void render() {
    if (!isEmpty()) {
      clear();
    }

    if (groups.isEmpty()) {
      add(renderNoData(getCaption()));

    } else {
      add(groupPanel);
      add(filtersAndItemsPanel);

      renderGroups();
    }
  }

  private void renderFilters(final EcGroup group, final EcGroupFilters groupFilters) {
    Flow panel = new Flow(EcStyles.name(STYLE_NAME, "filters"));

    final Horizontal filterContainer = new Horizontal();
    EcStyles.add(filterContainer, STYLE_NAME, "filtersContainer");

    if (!groupFilters.getBrands().isEmpty()) {
      filterContainer.add(renderBrands(groupFilters.getBrands()));
    }
    for (EcCriterion criterion : groupFilters.getCriteria()) {
      filterContainer.add(renderCriterion(criterion));
    }

    panel.add(filterContainer);

    Flow actionPanel = new Flow(EcStyles.name(STYLE_NAME, "actions"));

    Button doFilter = new Button(Localized.getConstants().doFilter());
    EcStyles.add(doFilter, STYLE_NAME, "doFilter");

    doFilter.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        refreshItems(group, groupFilters);
      }
    });

    actionPanel.add(doFilter);

    Button clearSelection = new Button(Localized.getConstants().removeFilter());
    EcStyles.add(clearSelection, STYLE_NAME, "clearSelection");

    clearSelection.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (groupFilters.clearSelection()) {
          Collection<Widget> children = UiHelper.getChildrenByStyleName(filterContainer,
              Sets.newHashSet(STYLE_BRAND_SELECTED, STYLE_VALUE_SELECTED));
          for (Widget child : children) {
            if (child instanceof CheckBox) {
              ((CheckBox) child).setValue(false);
              child.removeStyleName(STYLE_BRAND_SELECTED);
              child.removeStyleName(STYLE_VALUE_SELECTED);
            }
          }

          refreshItems(group, null);
        }
      }
    });

    actionPanel.add(clearSelection);
    panel.add(actionPanel);

    filtersAndItemsPanel.add(panel);
  }

  private void renderGroups() {
    if (!groupPanel.isEmpty()) {
      groupPanel.clear();
    }

    Legend legend = new Legend(Localized.getConstants().ecSelectGroup());
    EcStyles.add(legend, STYLE_NAME, "groupsLegend");
    groupPanel.add(legend);

    Flow container = new Flow(EcStyles.name(STYLE_NAME, "groupsContainer"));

    String groupStyle = EcStyles.name(STYLE_NAME, "group");

    ClickHandler clickHandler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof Widget) {
          selectGroup(DomUtils.getDataIndexInt(((Widget) event.getSource()).getElement()));
        }
      }
    };

    for (int i = 0; i < groups.size(); i++) {
      EcGroup group = groups.get(i);

      CustomDiv groupWidget = new CustomDiv(groupStyle);
      groupWidget.setHtml(group.getName());
      DomUtils.setDataIndex(groupWidget.getElement(), i);

      groupWidget.addClickHandler(clickHandler);

      container.add(groupWidget);
    }

    groupPanel.add(container);
  }

  private void renderItems(final List<EcItem> items) {
    EcKeeper.ensureCategoriesAndBrandsAndStockLabels(new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        for (Widget widget : filtersAndItemsPanel) {
          if (widget instanceof ItemList) {
            ((ItemList) widget).render(items);
            return;
          }
        }

        ItemList itemList = new ItemList(items);
        filtersAndItemsPanel.add(itemList);
      }
    });
  }

  private void selectGroup(int index) {
    if (BeeUtils.isIndex(groups, index) && getGroupIndex() != index) {
      if (!BeeConst.isUndef(getGroupIndex())) {
        Element selected = DomUtils.getChildByDataIndex(groupPanel.getElement(), getGroupIndex(),
            true);
        if (selected != null) {
          selected.removeClassName(STYLE_GROUP_SELECTED);
        }
      }

      setGroupIndex(index);

      Element selected = DomUtils.getChildByDataIndex(groupPanel.getElement(), index, true);
      if (selected != null) {
        selected.addClassName(STYLE_GROUP_SELECTED);
      }

      showGroup(groups.get(index));
    }
  }

  private void setGroupIndex(int groupIndex) {
    this.groupIndex = groupIndex;
  }

  private void showGroup(final EcGroup group) {
    if (!filtersAndItemsPanel.isEmpty()) {
      filtersAndItemsPanel.clear();
    }

    Label label = new Label(group.getName());
    EcStyles.add(label, STYLE_NAME, "groupLabel");
    filtersAndItemsPanel.add(label);

    final long id = group.getId();

    if (group.hasFilters()) {
      if (filters.containsKey(id)) {
        renderFilters(group, filters.get(id));
      } else {
        ParameterList params = EcKeeper.createArgs(SVC_GET_GROUP_FILTERS);
        params.addQueryItem(COL_GROUP, id);

        BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            EcKeeper.dispatchMessages(response);

            if (response.hasResponse(EcGroupFilters.class)) {
              EcGroupFilters groupFilters = EcGroupFilters.restore(response.getResponseAsString());

              if (groupFilters.isEmpty()) {
                refreshItems(group, null);
              } else {
                filters.put(id, groupFilters);
                renderFilters(group, groupFilters);
              }
            }
          }
        });
      }

    } else {
      refreshItems(group, null);
    }
  }
}
