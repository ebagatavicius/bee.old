package com.butent.bee.shared.modules.ec;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.modules.ec.EcConstants.EcDisplayedPrice;
import com.butent.bee.shared.modules.ec.EcConstants.EcSupplier;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class EcItem implements BeeSerializable, HasCaption {

  private enum Serial {
    ARTICLE_ID, BRAND, CODE, NAME, SUPPLIERS, CATEGORIES, CLIENT_PRICE, FEATURED_PRICE, LIST_PRICE,
    DESCRIPTION, CRITERIA, NOVELTY, FEATURED, UNIT, PRIMARY_STOCK, SECONDARY_STOCK, ANALOG_COUNT
  }

  public static final Splitter CATEGORY_SPLITTER =
      Splitter.on(EcConstants.CATEGORY_ID_SEPARATOR).trimResults().omitEmptyStrings();

  public static String joinCategories(Collection<Long> categories) {
    StringBuilder sb = new StringBuilder();
    for (Long category : categories) {
      sb.append(category).append(EcConstants.CATEGORY_ID_SEPARATOR);
    }
    return sb.toString();
  }

  public static EcItem restore(String s) {
    EcItem item = new EcItem();
    item.deserialize(s);
    return item;
  }

  private long articleId;

  private Long brand;
  private String code;
  private String name;

  private String categories;
  private final Collection<ArticleSupplier> suppliers = Lists.newArrayList();

  private int clientPrice;
  private int featuredPrice;
  private int listPrice;

  private String description;
  private final List<ArticleCriteria> criteria = Lists.newArrayList();

  private boolean novelty;
  private boolean featured;

  private String unit;

  private int primaryStock;
  private int secondaryStock;

  private int analogCount;

  public EcItem(long articleId) {
    this.articleId = articleId;
  }

  private EcItem() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (value == null) {
        continue;
      }

      switch (member) {
        case ARTICLE_ID:
          this.articleId = BeeUtils.toLong(value);
          break;

        case BRAND:
          setBrand(BeeUtils.toLongOrNull(value));
          break;

        case CODE:
          setCode(value);
          break;

        case NAME:
          setName(value);
          break;

        case SUPPLIERS:
          Collection<ArticleSupplier> sups = Lists.newArrayList();

          String[] suppArr = Codec.beeDeserializeCollection(value);
          if (suppArr != null) {
            for (String supplier : suppArr) {
              sups.add(ArticleSupplier.restore(supplier));
            }
          }

          setSuppliers(sups);
          break;

        case CATEGORIES:
          setCategories(value);
          break;

        case CLIENT_PRICE:
          setClientPrice(BeeUtils.toInt(value));
          break;

        case FEATURED_PRICE:
          setFeaturedPrice(BeeUtils.toInt(value));
          break;

        case LIST_PRICE:
          setListPrice(BeeUtils.toInt(value));
          break;

        case DESCRIPTION:
          setDescription(value);
          break;

        case CRITERIA:
          if (!criteria.isEmpty()) {
            criteria.clear();
          }

          String[] critArr = Codec.beeDeserializeCollection(value);
          if (critArr != null) {
            for (String crit : critArr) {
              criteria.add(ArticleCriteria.restore(crit));
            }
          }
          break;

        case NOVELTY:
          setNovelty(Codec.unpack(value));
          break;

        case FEATURED:
          setFeatured(Codec.unpack(value));
          break;

        case UNIT:
          setUnit(value);
          break;

        case PRIMARY_STOCK:
          setPrimaryStock(BeeUtils.toInt(value));
          break;

        case SECONDARY_STOCK:
          setSecondaryStock(BeeUtils.toInt(value));
          break;

        case ANALOG_COUNT:
          setAnalogCount(BeeUtils.toInt(value));
          break;
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof EcItem) && articleId == ((EcItem) obj).articleId;
  }

  public int getAnalogCount() {
    return analogCount;
  }

  public long getArticleId() {
    return articleId;
  }

  public Long getBrand() {
    return brand;
  }

  @Override
  public String getCaption() {
    return BeeUtils.joinWords(getName(), getCode());
  }

  public Set<Long> getCategorySet() {
    Set<Long> result = Sets.newHashSet();

    if (getCategories() != null) {
      for (String s : CATEGORY_SPLITTER.split(getCategories())) {
        result.add(BeeUtils.toLong(s));
      }
    }

    return result;
  }

  public int getClientPrice() {
    return clientPrice;
  }

  public String getCode() {
    return code;
  }

  public List<ArticleCriteria> getCriteria() {
    return criteria;
  }

  public String getDescription() {
    return description;
  }

  public int getFeaturedPrice() {
    return featuredPrice;
  }

  public int getListPrice() {
    return listPrice;
  }

  public String getName() {
    return name;
  }

  public int getPrice() {
    if (isFeatured() && getFeaturedPrice() > 0 && getFeaturedPrice() < getClientPrice()) {
      return getFeaturedPrice();
    } else {
      return getClientPrice();
    }
  }

  public int getPrimaryStock() {
    return primaryStock;
  }

  public double getRealClientPrice() {
    return clientPrice / 100d;
  }

  public double getRealListPrice() {
    return listPrice / 100d;
  }

  public double getRealPrice() {
    return getPrice() / 100d;
  }

  public int getSecondaryStock() {
    return secondaryStock;
  }

  public int getSupplierPrice(EcDisplayedPrice displayedPrice, Double marginPercent) {
    EcSupplier displayedSupplier = EcDisplayedPrice.getSupplier(displayedPrice);

    int result = 0;
    int aggregate = 0;

    for (ArticleSupplier articleSupplier : getSuppliers()) {
      int supplierPrice = articleSupplier.getListPrice(marginPercent);

      if (supplierPrice > 0) {
        if (displayedSupplier != null && articleSupplier.getSupplier() == displayedSupplier) {
          result = supplierPrice;
          break;
        }

        if (displayedPrice == EcDisplayedPrice.MIN) {
          if (articleSupplier.totalStock() > 0) {
            if (result > 0) {
              result = Math.min(result, supplierPrice);
            } else {
              result = supplierPrice;
            }

          } else {
            if (aggregate > 0) {
              aggregate = Math.min(aggregate, supplierPrice);
            } else {
              aggregate = supplierPrice;
            }
          }

        } else if (displayedPrice == EcDisplayedPrice.MAX) {
          if (articleSupplier.totalStock() > 0) {
            result = Math.max(result, supplierPrice);
          } else {
            aggregate = Math.max(aggregate, supplierPrice);
          }

        } else {
          result = Math.max(result, supplierPrice);
        }
      }
    }

    if (result <= 0 && aggregate > 0) {
      return aggregate;
    } else {
      return result;
    }
  }

  public Collection<ArticleSupplier> getSuppliers() {
    return suppliers;
  }

  public String getUnit() {
    return unit;
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(articleId);
  }

  public boolean isFeatured() {
    return featured;
  }

  public boolean isNovelty() {
    return novelty;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ARTICLE_ID:
          arr[i++] = articleId;
          break;

        case BRAND:
          arr[i++] = brand;
          break;

        case CODE:
          arr[i++] = code;
          break;

        case NAME:
          arr[i++] = name;
          break;

        case SUPPLIERS:
          arr[i++] = suppliers;
          break;

        case CATEGORIES:
          arr[i++] = categories;
          break;

        case CLIENT_PRICE:
          arr[i++] = clientPrice;
          break;

        case FEATURED_PRICE:
          arr[i++] = featuredPrice;
          break;

        case LIST_PRICE:
          arr[i++] = listPrice;
          break;

        case DESCRIPTION:
          arr[i++] = description;
          break;

        case CRITERIA:
          arr[i++] = criteria;
          break;

        case NOVELTY:
          arr[i++] = Codec.pack(novelty);
          break;

        case FEATURED:
          arr[i++] = Codec.pack(featured);
          break;

        case UNIT:
          arr[i++] = unit;
          break;

        case PRIMARY_STOCK:
          arr[i++] = primaryStock;
          break;

        case SECONDARY_STOCK:
          arr[i++] = secondaryStock;
          break;

        case ANALOG_COUNT:
          arr[i++] = analogCount;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAnalogCount(int analogCount) {
    this.analogCount = analogCount;
  }

  public void setBrand(Long brand) {
    this.brand = brand;
  }

  public void setCategories(String categories) {
    this.categories = categories;
  }

  public void setClientPrice(Double price) {
    this.clientPrice = BeeUtils.isDouble(price) ? BeeUtils.round(price * 100) : 0;
  }

  public void setClientPrice(int clientPrice) {
    this.clientPrice = clientPrice;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public void setCriteria(List<ArticleCriteria> criteria) {
    BeeUtils.overwrite(this.criteria, criteria);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setFeatured(boolean featured) {
    this.featured = featured;
  }

  public void setFeaturedPrice(Double price) {
    this.featuredPrice = BeeUtils.isDouble(price) ? BeeUtils.round(price * 100) : 0;
  }

  public void setFeaturedPrice(int featuredPrice) {
    this.featuredPrice = featuredPrice;
  }

  public void setListPrice(int listPrice) {
    this.listPrice = listPrice;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNovelty(boolean novelty) {
    this.novelty = novelty;
  }

  public void setPrimaryStock(int primaryStock) {
    this.primaryStock = primaryStock;
  }

  public void setSecondaryStock(int secondaryStock) {
    this.secondaryStock = secondaryStock;
  }

  public void setSuppliers(Collection<ArticleSupplier> suppliers) {
    BeeUtils.overwrite(this.suppliers, suppliers);
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public int totalStock() {
    return getPrimaryStock() + getSecondaryStock();
  }

  private String getCategories() {
    return categories;
  }
}
