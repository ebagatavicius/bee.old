package com.butent.bee.client.modules.calendar.layout;

import com.butent.bee.shared.modules.calendar.CalendarItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemStackingManager {

  private final Map<Integer, List<ItemLayoutDescription>> layers = new HashMap<>();

  private final int maxLayer;

  private int highestLayer;

  public ItemStackingManager(int maxLayer) {
    this.maxLayer = maxLayer;
  }

  public void assignLayer(ItemLayoutDescription description) {
    boolean layerAssigned = false;
    int layer = 0;

    do {
      initLayer(layer);
      layerAssigned = assignLayer(layer, description);
      layer++;
    } while (!layerAssigned);
  }

  public void assignLayer(int fromWeekDay, int toWeekDay, CalendarItem item) {
    assignLayer(new ItemLayoutDescription(fromWeekDay, toWeekDay, item));
  }

  public int countOverLimit(int day) {
    int count = 0;
    for (int layer = 0; layer <= highestLayer; layer++) {
      List<ItemLayoutDescription> descriptions = layers.get(layer);
      if (descriptions != null) {
        for (ItemLayoutDescription description : descriptions) {
          if (layer > maxLayer && description.overlaps(day, day)) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public List<ItemLayoutDescription> getDescriptionsInLayer(int layerIndex) {
    return layers.get(layerIndex);
  }

  public List<CalendarItem> getOverLimit(int day) {
    List<CalendarItem> result = new ArrayList<>();

    for (int layer = 0; layer <= highestLayer; layer++) {
      List<ItemLayoutDescription> descriptions = layers.get(layer);
      if (descriptions != null) {
        for (ItemLayoutDescription description : descriptions) {
          if (layer > maxLayer && description.overlaps(day, day)) {
            result.add(description.getItem());
          }
        }
      }
    }
    return result;
  }

  public int lowestLayerIndex(int day) {
    return nextLowestLayerIndex(day, 0);
  }

  public int nextLowestLayerIndex(int day, int fromLayer) {
    boolean layerFound = false;
    int currentlyInspectedLayer = fromLayer;

    do {
      if (isLayerAllocated(currentlyInspectedLayer)) {
        if (overlaps(layers.get(currentlyInspectedLayer), day, day)) {
          currentlyInspectedLayer++;
        } else {
          layerFound = true;
        }
      } else {
        layerFound = true;
      }
    } while (!layerFound);

    return currentlyInspectedLayer;
  }

  private boolean assignLayer(int layer, ItemLayoutDescription description) {
    List<ItemLayoutDescription> layerDescriptions = layers.get(layer);

    boolean assigned = false;
    if (!overlaps(layerDescriptions, description.getWeekStartDay(), description.getWeekEndDay())) {
      highestLayer = Math.max(highestLayer, layer);

      if (layer > maxLayer && description.spansMoreThanADay()) {
        ItemLayoutDescription split = description.split();
        layerDescriptions.add(description);
        assignLayer(split);
      } else {
        layerDescriptions.add(description);
      }

      assigned = true;
    }
    return assigned;
  }

  private void initLayer(int layerIndex) {
    if (!isLayerAllocated(layerIndex)) {
      layers.put(layerIndex, new ArrayList<ItemLayoutDescription>());
    }
  }

  private boolean isLayerAllocated(int layerIndex) {
    return layers.get(layerIndex) != null;
  }

  private static boolean overlaps(List<ItemLayoutDescription> descriptions, int start,
      int end) {
    if (descriptions != null) {
      for (ItemLayoutDescription description : descriptions) {
        if (description.overlaps(start, end)) {
          return true;
        }
      }
    }
    return false;
  }
}
