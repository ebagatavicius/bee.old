package com.butent.bee.shared.modules.ec;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.SelectableValue;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EcGroupFilters implements BeeSerializable {

  public static EcGroupFilters restore(String s) {
    EcGroupFilters groupFilters = new EcGroupFilters();
    groupFilters.deserialize(s);
    return groupFilters;
  }

  private final List<EcBrand> brands = new ArrayList<>();

  private final List<EcCriterion> criteria = new ArrayList<>();

  public EcGroupFilters() {
    super();
  }

  public boolean clearSelection() {
    boolean changed = false;

    for (EcBrand brand : brands) {
      changed |= brand.isSelected();
      brand.setSelected(false);
    }

    for (EcCriterion criterion : criteria) {
      changed |= criterion.clearSelection();
    }

    return changed;
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);

    brands.clear();
    criteria.clear();

    String[] values = Codec.beeDeserializeCollection(arr[0]);
    if (values != null) {
      for (String v : values) {
        brands.add(EcBrand.restore(v));
      }
    }

    values = Codec.beeDeserializeCollection(arr[1]);
    if (values != null) {
      for (String v : values) {
        criteria.add(EcCriterion.restore(v));
      }
    }
  }

  public List<EcBrand> getBrands() {
    return brands;
  }

  public List<EcCriterion> getCriteria() {
    return criteria;
  }

  public Set<Long> getSelectedBrands() {
    Set<Long> selectedBrands = new HashSet<>();

    for (EcBrand brand : brands) {
      if (brand.isSelected()) {
        selectedBrands.add(brand.getId());
      }
    }

    return selectedBrands;
  }

  public Multimap<Long, String> getSelectedCriteria() {
    Multimap<Long, String> selectedCriteria = ArrayListMultimap.create();

    for (EcCriterion criterion : criteria) {
      for (SelectableValue sv : criterion.getValues()) {
        if (sv.isSelected()) {
          selectedCriteria.put(criterion.getId(), sv.getValue());
        }
      }
    }

    return selectedCriteria;
  }

  public int getSize() {
    int size = brands.size();
    for (EcCriterion criterion : criteria) {
      size += criterion.getSize();
    }
    return size;
  }

  public boolean isEmpty() {
    return brands.isEmpty() && criteria.isEmpty();
  }

  @Override
  public String serialize() {
    Object[] arr = new Object[] {getBrands(), getCriteria()};
    return Codec.beeSerialize(arr);
  }
}
