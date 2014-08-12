package com.butent.bee.client.modules.ec;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.i18n.Collator;
import com.butent.bee.client.tree.Tree;
import com.butent.bee.client.tree.TreeItem;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.ec.DeliveryMethod;
import com.butent.bee.shared.modules.ec.EcBrand;
import com.butent.bee.shared.modules.ec.EcCarModel;
import com.butent.bee.shared.modules.ec.EcCarType;
import com.butent.bee.shared.modules.ec.EcItem;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class EcData {

  private final class CategoryComparator implements Comparator<Long> {
    private CategoryComparator() {
    }

    @Override
    public int compare(Long o1, Long o2) {
      return Collator.DEFAULT.compare(categoryNames.get(o1), categoryNames.get(o2));
    }
  }

  private final List<String> carManufacturers = Lists.newArrayList();
  private final Map<String, List<EcCarModel>> carModelsByManufacturer = Maps.newHashMap();
  private final Map<Long, List<EcCarType>> carTypesByModel = Maps.newHashMap();

  private final Map<Long, String> categoryNames = Maps.newHashMap();
  private final CategoryComparator categoryComparator = new CategoryComparator();

  private final Set<Long> categoryRoots = Sets.newHashSet();
  private final Multimap<Long, Long> categoryByParent = HashMultimap.create();
  private final Map<Long, Long> categoryByChild = Maps.newHashMap();

  private final List<EcBrand> itemBrands = Lists.newArrayList();
  private final Map<Long, String> brandNames = Maps.newHashMap();

  private final List<DeliveryMethod> deliveryMethods = Lists.newArrayList();

  private final Map<String, String> configuration = Maps.newHashMap();

  private final Map<String, String> clientInfo = Maps.newHashMap();
  private final List<String> clientStockLabels = Lists.newArrayList();

  private BeeRowSet warehouses;

  EcData() {
    super();
  }

  Tree buildCategoryTree(Set<Long> ids) {
    List<Long> roots = Lists.newArrayList();
    Multimap<Long, Long> data = HashMultimap.create();

    for (long id : ids) {
      Long parent = getParent(id, ids);
      if (parent == null) {
        roots.add(id);
      } else {
        data.put(parent, id);
      }
    }

    Tree tree = new Tree();

    TreeItem rootItem = new TreeItem(Localized.getConstants().ecSelectCategory());
    tree.addItem(rootItem);

    if (roots.size() > 1) {
      Collections.sort(roots, categoryComparator);
    }

    for (long id : roots) {
      TreeItem treeItem = createCategoryTreeItem(id);
      rootItem.addItem(treeItem);

      fillTree(data, id, treeItem);
    }

    return tree;
  }

  void ensureBrands(final Consumer<Boolean> callback) {
    if (itemBrands.isEmpty()) {
      getItemBrands(new Consumer<List<EcBrand>>() {
        @Override
        public void accept(List<EcBrand> input) {
          callback.accept(true);
        }
      });
    } else {
      callback.accept(true);
    }
  }

  void ensureCategories(final Consumer<Boolean> callback) {
    if (categoryNames.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CATEGORIES);
      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            categoryNames.clear();
            categoryRoots.clear();
            categoryByParent.clear();
            categoryByChild.clear();

            for (int i = 0; i < arr.length; i += 3) {
              long id = BeeUtils.toLong(arr[i]);
              long parent = BeeUtils.toLong(arr[i + 1]);
              String name = arr[i + 2];

              categoryNames.put(id, name);

              if (parent > 0) {
                categoryByParent.put(parent, id);
                categoryByChild.put(id, parent);
              } else {
                categoryRoots.add(id);
              }
            }

            callback.accept(true);
          }
        }
      });

    } else {
      callback.accept(true);
    }
  }

  void ensureCategoriesAndBrandsAndStockLabels(final Consumer<Boolean> callback) {
    if (!categoryNames.isEmpty() && !itemBrands.isEmpty() && !clientStockLabels.isEmpty()) {
      callback.accept(true);
    }

    final Holder<Integer> latch = Holder.of(0);

    Consumer<Boolean> consumer = new Consumer<Boolean>() {
      @Override
      public void accept(Boolean input) {
        if (latch.get() >= 2) {
          callback.accept(input);
        } else {
          latch.set(latch.get() + 1);
        }
      }
    };

    ensureCategories(consumer);
    ensureBrands(consumer);
    ensureClientStockLabels(consumer);
  }

  void ensureClientStockLabels(final Consumer<Boolean> callback) {
    if (clientStockLabels.isEmpty()) {
      getClientStockLabels(new Consumer<Boolean>() {
        @Override
        public void accept(Boolean input) {
          callback.accept(true);
        }
      });
    } else {
      callback.accept(true);
    }
  }

  void ensureWarehouses(final Consumer<Boolean> callback) {
    if (warehouses == null) {
      Queries.getRowSet(ClassifierConstants.VIEW_WAREHOUSES, null, new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          warehouses = result;
          callback.accept(result != null);
        }
      });
    } else {
      callback.accept(true);
    }
  }

  String getBrandName(long brand) {
    return brandNames.get(brand);
  }

  void getCarManufacturers(final Consumer<List<String>> callback) {
    if (carManufacturers.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CAR_MANUFACTURERS);
      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            carManufacturers.clear();
            for (String manufacturer : arr) {
              if (!BeeUtils.isEmpty(manufacturer)) {
                carManufacturers.add(manufacturer);
              }
            }

            callback.accept(carManufacturers);
          }
        }
      });

    } else {
      callback.accept(carManufacturers);
    }
  }

  void getCarModels(final String manufacturer, final Consumer<List<EcCarModel>> callback) {
    if (carModelsByManufacturer.containsKey(manufacturer)) {
      callback.accept(carModelsByManufacturer.get(manufacturer));

    } else {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CAR_MODELS);
      params.addDataItem(VAR_MANUFACTURER, manufacturer);

      BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            List<EcCarModel> carModels = Lists.newArrayList();
            for (String s : arr) {
              carModels.add(EcCarModel.restore(s));
            }
            carModelsByManufacturer.put(manufacturer, carModels);

            callback.accept(carModels);
          }
        }
      });
    }
  }

  void getCarTypes(final long modelId, final Consumer<List<EcCarType>> callback) {
    if (carTypesByModel.containsKey(modelId)) {
      callback.accept(carTypesByModel.get(modelId));

    } else {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CAR_TYPES);
      params.addQueryItem(VAR_MODEL, modelId);

      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            List<EcCarType> carTypes = Lists.newArrayList();
            for (String s : arr) {
              carTypes.add(EcCarType.restore(s));
            }
            carTypesByModel.put(modelId, carTypes);

            callback.accept(carTypes);
          }
        }
      });
    }
  }

  String getCategoryFullName(long categoryId, String separator) {
    List<String> names = Lists.newArrayList();

    for (Long parent = categoryId; parent != null; parent = categoryByChild.get(parent)) {
      String name = getCategoryName(parent);
      if (name != null) {
        names.add(name);
      }
    }

    if (names.isEmpty()) {
      return null;
    } else if (names.size() == 1) {
      return names.get(0);
    } else {
      return BeeUtils.join(separator, Lists.reverse(names));
    }
  }

  String getCategoryName(long categoryId) {
    return categoryNames.get(categoryId);
  }

  List<String> getCategoryNames(EcItem item) {
    List<String> names = Lists.newArrayList();

    Set<Long> categoryIds = item.getCategorySet();
    for (Long categoryId : categoryIds) {
      String name = categoryNames.get(categoryId);
      if (name != null && !names.contains(name)) {
        names.add(name);
      }
    }

    if (names.size() > 1) {
      Collections.sort(names);
    }
    return names;
  }

  void getClientValues(final List<String> keys, final Consumer<List<String>> callback) {
    if (clientInfo.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CLIENT_INFO);
      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);

          Map<String, String> map = Codec.deserializeMap(response.getResponseAsString());
          if (!map.isEmpty()) {
            clientInfo.clear();
            clientInfo.putAll(map);

            getClientValues(keys, callback);
          }
        }
      });

    } else {
      List<String> values = Lists.newArrayList();
      for (String key : keys) {
        values.add(clientInfo.get(key));
      }

      callback.accept(values);
    }
  }

  void getConfiguration(final Consumer<Map<String, String>> callback) {
    if (configuration.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CONFIGURATION);
      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);

          Map<String, String> map = Codec.deserializeMap(response.getResponseAsString());
          if (!map.isEmpty()) {
            configuration.clear();
            configuration.putAll(map);

            callback.accept(map);
          }
        }
      });

    } else {
      callback.accept(configuration);
    }
  }

  void getDeliveryMethods(final Consumer<List<DeliveryMethod>> callback) {
    if (!deliveryMethods.isEmpty()) {
      callback.accept(deliveryMethods);

    } else {
      ParameterList params = EcKeeper.createArgs(SVC_GET_DELIVERY_METHODS);

      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            deliveryMethods.clear();
            for (String s : arr) {
              deliveryMethods.add(DeliveryMethod.restore(s));
            }

            callback.accept(deliveryMethods);
          }
        }
      });
    }
  }

  void getItemBrands(final Consumer<List<EcBrand>> callback) {
    if (itemBrands.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(SVC_GET_ITEM_BRANDS);
      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (arr != null) {
            itemBrands.clear();
            brandNames.clear();

            for (String s : arr) {
              EcBrand brand = EcBrand.restore(s);

              itemBrands.add(brand);
              brandNames.put(brand.getId(), brand.getName());
            }

            callback.accept(itemBrands);
          }
        }
      });

    } else {
      callback.accept(itemBrands);
    }
  }

  String getPrimaryStockLabel() {
    return BeeUtils.getQuietly(clientStockLabels, 0);
  }

  String getSecondaryStockLabel() {
    return BeeUtils.getQuietly(clientStockLabels, 1);
  }

  String getWarehouseLabel(String code) {
    if (BeeUtils.isEmpty(code) || DataUtils.isEmpty(warehouses)) {
      return null;
    } else {
      int codeIndex = warehouses.getColumnIndex(ClassifierConstants.COL_WAREHOUSE_CODE);
      int nameIndex = warehouses.getColumnIndex(ClassifierConstants.COL_WAREHOUSE_NAME);

      for (BeeRow row : warehouses.getRows()) {
        if (code.equals(row.getString(codeIndex))) {
          return BeeUtils.notEmpty(row.getString(nameIndex), code);
        }
      }
      return code;
    }
  }

  void saveConfiguration(final String key, final String value) {
    ParameterList params;

    if (BeeUtils.isEmpty(value)) {
      params = EcKeeper.createArgs(SVC_CLEAR_CONFIGURATION);
      params.addDataItem(Service.VAR_COLUMN, key);
    } else {
      params = EcKeeper.createArgs(SVC_SAVE_CONFIGURATION);
      params.addQueryItem(Service.VAR_COLUMN, key);
      params.addDataItem(Service.VAR_VALUE, value);
    }

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        EcKeeper.dispatchMessages(response);
        if (BeeUtils.same(key, response.getResponseAsString())) {
          configuration.put(key, value);
        }
      }
    });
  }

  private TreeItem createCategoryTreeItem(long id) {
    TreeItem treeItem = new TreeItem(categoryNames.get(id));
    treeItem.setUserObject(id);

    return treeItem;
  }

  private void fillTree(Multimap<Long, Long> data, long parent, TreeItem parentItem) {
    if (data.containsKey(parent)) {
      List<Long> children = Lists.newArrayList(data.get(parent));
      if (children.size() > 1) {
        Collections.sort(children, categoryComparator);
      }

      for (long id : children) {
        TreeItem childItem = createCategoryTreeItem(id);
        parentItem.addItem(childItem);

        fillTree(data, id, childItem);
      }
    }
  }

  private void getClientStockLabels(final Consumer<Boolean> callback) {
    if (clientStockLabels.isEmpty()) {
      ParameterList params = EcKeeper.createArgs(SVC_GET_CLIENT_STOCK_LABELS);
      BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          EcKeeper.dispatchMessages(response);
          String[] arr = Codec.beeDeserializeCollection(response.getResponseAsString());

          if (!ArrayUtils.isEmpty(arr)) {
            clientStockLabels.clear();
            for (String s : arr) {
              clientStockLabels.add(s);
            }

            callback.accept(true);
          }
        }
      });

    } else {
      callback.accept(true);
    }
  }

  private Long getParent(long categoryId, Collection<Long> filter) {
    for (Long parent = categoryByChild.get(categoryId); parent != null; parent =
        categoryByChild.get(parent)) {
      if (filter.contains(parent)) {
        return parent;
      }
    }
    return null;
  }
}
