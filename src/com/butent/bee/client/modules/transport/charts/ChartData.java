package com.butent.bee.client.modules.transport.charts;

import com.google.gwt.user.client.ui.HasEnabled;

import com.butent.bee.client.Global;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ChartData implements HasEnabled {

  enum Type implements HasCaption {
    DRIVER(Localized.dictionary().drivers(), ValueType.TEXT),
    DRIVER_GROUP(Localized.dictionary().driverGroupsShort(), ValueType.TEXT),
    CARGO(Localized.dictionary().cargos(), ValueType.TEXT),
    CARGO_TYPE(Localized.dictionary().trCargoTypes(), ValueType.TEXT),
    CUSTOMER(Localized.dictionary().transportationCustomers(), ValueType.TEXT),
    MANAGER(Localized.dictionary().managers(), ValueType.TEXT),
    LOADING(Localized.dictionary().cargoLoading(), ValueType.TEXT),
    ORDER(Localized.dictionary().trOrders(), ValueType.TEXT),
    ORDER_STATUS(Localized.dictionary().trOrderStatus(), ValueType.TEXT),
    PLACE(Localized.dictionary().cargoHandlingPlaces(), ValueType.TEXT),
    TRAILER(Localized.dictionary().trailers(), ValueType.TEXT),
    TRIP(Localized.dictionary().trips(), ValueType.TEXT),
    TRIP_STATUS(Localized.dictionary().trTripStatus(), ValueType.TEXT),
    TRIP_ARRIVAL(Localized.dictionary().transportArrival(), ValueType.DATE),
    TRIP_DEPARTURE(Localized.dictionary().transportDeparture(), ValueType.DATE),
    TRUCK(Localized.dictionary().trucks(), ValueType.TEXT),
    UNLOADING(Localized.dictionary().cargoUnloading(), ValueType.TEXT),
    VEHICLE_GROUP(Localized.dictionary().vehicleGroupsShort(), ValueType.TEXT),
    VEHICLE_MODEL(Localized.dictionary().vehicleModelsShort(), ValueType.TEXT),
    VEHICLE_TYPE(Localized.dictionary().trVehicleTypesShort(), ValueType.TEXT);

    private final String caption;
    private final ValueType valueType;

    Type(String caption, ValueType valueType) {
      this.caption = caption;
      this.valueType = valueType;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  private final Type type;

  private final Set<String> items = new HashSet<>();
  private final List<String> orderedItems = new ArrayList<>();

  private final Set<Long> ids = new HashSet<>();
  private final Map<String, Long> itemToId = new HashMap<>();

  private final List<String> selectedItems = new ArrayList<>();
  private final List<String> savedSelection = new ArrayList<>();

  private boolean enabled = true;

  ChartData(Type type) {
    this.type = type;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;

    if (!enabled) {
      deselectAll();
    }
  }

  void add(String name) {
    add(name, null);
  }

  void add(String name, Long id) {
    if (name != null && !name.isEmpty()) {
      items.add(name);

      if (id != null) {
        ids.add(id);
        itemToId.put(name, id);
      }
    }
  }

  <E extends Enum<?> & HasCaption> void add(E item) {
    if (item != null) {
      add(item.getCaption(), (long) item.ordinal());
    }
  }

  void add(JustDate date) {
    if (date != null) {
      add(Format.renderDate(date), (long) date.getDays());
    }
  }

  void addUser(Long userId) {
    if (userId != null) {
      String signature = Global.getUsers().getSignature(userId);
      if (signature != null) {
        add(signature, userId);
      }
    }
  }

  boolean contains(Enum<?> e) {
    if (e == null) {
      return false;
    }

    long id = e.ordinal();
    return contains(id);
  }

  boolean contains(JustDate date) {
    if (date == null) {
      return false;
    }

    long id = date.getDays();
    return contains(id);
  }

  boolean contains(Long id) {
    return id != null && ids.contains(id);
  }

  boolean contains(String name) {
    return name != null && items.contains(name);
  }

  boolean containsAny(Collection<Long> itemIds) {
    if (BeeUtils.isEmpty(itemIds) || ids.isEmpty()) {
      return false;
    }

    for (Long id : itemIds) {
      if (contains(id)) {
        return true;
      }
    }

    return false;
  }

  void deselectAll() {
    if (!selectedItems.isEmpty()) {
      selectedItems.clear();
    }
  }

  Long getItemId(String item) {
    return itemToId.get(item);
  }

  List<String> getOrderedItems() {
    if (orderedItems.size() != size()) {
      orderedItems.clear();
      orderedItems.addAll(items);

      if (size() > 1) {
        Comparator<String> comparator;

        if (type.valueType == ValueType.DATE) {
          comparator = (o1, o2) -> BeeUtils.compareNullsFirst(itemToId.get(o1), itemToId.get(o2));
        } else {
          comparator = null;
        }

        orderedItems.sort(comparator);
      }
    }
    return orderedItems;
  }

  int getNumberOfSelectedItems() {
    return selectedItems.size();
  }

  int getNumberOfUnselectedItems() {
    return size() - getNumberOfSelectedItems();
  }

  List<String> getSelectedItems() {
    List<String> result = new ArrayList<>();

    if (!selectedItems.isEmpty()) {
      result.addAll(selectedItems);
    }
    return result;
  }

  Type getType() {
    return type;
  }

  boolean hasSelection() {
    return !selectedItems.isEmpty();
  }

  boolean isEmpty() {
    return items.isEmpty();
  }

  void saveState() {
    BeeUtils.overwrite(savedSelection, selectedItems);
  }

  void restoreState() {
    BeeUtils.overwrite(selectedItems, savedSelection);
  }

  boolean setItemSelected(String item, boolean selected) {
    if (BeeUtils.isEmpty(item)) {
      return false;

    } else if (selected) {
      if (selectedItems.contains(item)) {
        return false;
      } else {
        selectedItems.add(item);
        return true;
      }

    } else {
      return selectedItems.remove(item);
    }
  }

  private int size() {
    return items.size();
  }
}
