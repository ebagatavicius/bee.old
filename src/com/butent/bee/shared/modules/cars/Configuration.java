package com.butent.bee.shared.modules.cars;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Configuration implements BeeSerializable {

  private static final class OptionInfo extends ConfInfo {
    private final Map<String, Pair<ConfInfo, String>> relations = new HashMap<>();
    private final Map<Option, Boolean> restrictions = new HashMap<>();
    private Set<Option> packets = new TreeSet<>();

    @Override
    public void deserialize(String s) {
      String[] dataInfo = Codec.beeDeserializeCollection(s);

      relations.clear();
      Codec.deserializeHashMap(dataInfo[0]).forEach((key, val) -> {
        Pair<String, String> pair = Pair.restore(val);
        relations.put(key, Pair.of(ConfInfo.restore(pair.getA()), pair.getB()));
      });

      restrictions.clear();
      Codec.deserializeHashMap(dataInfo[1]).forEach((key, val) ->
          restrictions.put(Option.restore(key), BeeUtils.toBoolean(val)));

      packets.clear();
      for (String opt : Codec.beeDeserializeCollection(dataInfo[2])) {
        packets.add(Option.restore(opt));
      }
      super.deserialize(dataInfo[3]);
    }

    public Set<Option> getPackets() {
      return packets;
    }

    public Map<String, String> getRelationCriteria(Bundle bundle) {
      if (hasRelation(bundle)) {
        return relations.get(bundle.getKey()).getA().getCriteria();
      }
      return null;
    }

    public String getRelationDescription(Bundle bundle) {
      if (hasRelation(bundle)) {
        return relations.get(bundle.getKey()).getA().getDescription();
      }
      return null;
    }

    public String getRelationPackets(Bundle bundle) {
      if (hasRelation(bundle)) {
        return relations.get(bundle.getKey()).getB();
      }
      return null;
    }

    public Long getRelationPhoto(Bundle bundle) {
      if (hasRelation(bundle)) {
        return relations.get(bundle.getKey()).getA().getPhoto();
      }
      return null;
    }

    public String getRelationPrice(Bundle bundle) {
      if (hasRelation(bundle)) {
        return relations.get(bundle.getKey()).getA().getPrice();
      }
      return null;
    }

    public Map<Option, Boolean> getRestrictions() {
      return restrictions;
    }

    public boolean hasRelation(Bundle bundle) {
      return relations.containsKey(bundle.getKey());
    }

    public boolean hasRestrictions() {
      return !restrictions.isEmpty();
    }

    public void removeRelation(Bundle bundle) {
      relations.remove(bundle.getKey());
    }

    public static OptionInfo restore(String s) {
      if (BeeUtils.isEmpty(s)) {
        return null;
      }
      OptionInfo optionInfo = new OptionInfo();
      optionInfo.deserialize(s);
      return optionInfo;
    }

    @Override
    public String serialize() {
      return Codec.beeSerialize(new Object[] {relations, restrictions, packets, super.serialize()});
    }

    public void setInfo(ConfInfo info) {
      setPrice(info.getPrice());
      setDescription(info.getDescription());
      setCriteria(info.getCriteria());
      setPhoto(info.getPhoto());
    }

    public void setRelationInfo(Bundle bundle, ConfInfo info, String packet) {
      relations.put(bundle.getKey(), Pair.of(info, packet));
    }
  }

  public static final String DEFAULT_PRICE = BeeUtils.toString(BeeConst.UNDEF);

  private enum Serial {
    ROW_DIMENSIONS, COL_DIMENSIONS, BUNDLES, OPTIONS
  }

  private final List<Dimension> rowDimensions = new ArrayList<>();
  private final List<Dimension> colDimensions = new ArrayList<>();

  private final Map<Bundle, Pair<ConfInfo, Boolean>> bundles = new HashMap<>();
  private final Map<Option, OptionInfo> options = new TreeMap<>();

  public void addBundle(Bundle bundle) {
    if (!bundles.containsKey(bundle)) {
      List<Dimension> allDimensions = new ArrayList<>(rowDimensions);
      allDimensions.addAll(colDimensions);
      List<Option> orphans = processOptions(bundle, allDimensions, null);

      for (Option option : orphans) {
        addDimension(option.getDimension(), null);
      }
      bundles.put(bundle, null);
    }
  }

  public void addDimension(Dimension dimension, Integer ordinal) {
    Integer idx = ordinal;
    List<Dimension> dimensions = BeeUtils.isNegative(idx) ? colDimensions : rowDimensions;

    if (BeeUtils.isNegative(idx)) {
      idx = Math.abs(idx) - 1;
    }
    if (idx != null && BeeUtils.isIndex(dimensions, idx)) {
      dimensions.add(idx, dimension);
    } else {
      dimensions.add(dimension);
    }
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];

      switch (member) {
        case ROW_DIMENSIONS:
        case COL_DIMENSIONS:
          List<Dimension> dimensionList = Objects.equals(member, Serial.ROW_DIMENSIONS)
              ? rowDimensions : colDimensions;
          dimensionList.clear();
          String[] dimData = Codec.beeDeserializeCollection(value);

          if (!ArrayUtils.isEmpty(dimData)) {
            for (String d : dimData) {
              dimensionList.add(Dimension.restore(d));
            }
          }
          break;

        case BUNDLES:
          bundles.clear();
          Codec.deserializeHashMap(value).forEach((key, val) -> {
            Pair<String, String> pair = Pair.restore(val);
            bundles.put(Bundle.restore(key),
                Pair.of(ConfInfo.restore(pair.getA()), BeeUtils.toBoolean(pair.getB())));
          });
          break;

        case OPTIONS:
          options.clear();
          Codec.deserializeHashMap(value).forEach((key, val) ->
              options.put(Option.restore(key), OptionInfo.restore(val)));
          break;
      }
    }
  }

  public Collection<Bundle> getAllBundles() {
    return bundles.keySet();
  }

  public List<Dimension> getAllDimensions() {
    List<Dimension> allDimensions = new ArrayList<>(getRowDimensions());
    allDimensions.addAll(getColDimensions());
    return allDimensions;
  }

  public Map<String, String> getBundleCriteria(Bundle bundle) {
    Pair<ConfInfo, Boolean> pair = bundles.get(bundle);
    return pair != null && pair.getA() != null ? pair.getA().getCriteria() : null;
  }

  public String getBundleDescription(Bundle bundle) {
    Pair<ConfInfo, Boolean> pair = bundles.get(bundle);
    return pair != null && pair.getA() != null ? pair.getA().getDescription() : null;
  }

  public Long getBundlePhoto(Bundle bundle) {
    Pair<ConfInfo, Boolean> pair = bundles.get(bundle);
    return pair != null && pair.getA() != null ? pair.getA().getPhoto() : null;
  }

  public String getBundlePrice(Bundle bundle) {
    Pair<ConfInfo, Boolean> pair = bundles.get(bundle);
    return pair != null && pair.getA() != null ? pair.getA().getPrice() : null;
  }

  public Collection<Bundle> getBundles() {
    return getAllBundles().stream().filter(bundle -> getBundlePrice(bundle) != null)
        .collect(Collectors.toSet());
  }

  public List<Dimension> getColDimensions() {
    return colDimensions;
  }

  public Set<Option> getDeniedOptions(Option option) {
    Set<Option> set = new HashSet<>();

    if (hasRestrictions(option)) {
      for (Map.Entry<Option, Boolean> entry : getRestrictions(option).entrySet()) {
        if (entry.getValue()) {
          set.add(entry.getKey());
        }
      }
    }
    return set;
  }

  public List<Bundle> getMetrics(List<Dimension> dimensions) {
    Set<Bundle> dimensionBundles = new HashSet<>();

    for (Bundle bundle : bundles.keySet()) {
      List<Option> bundleOptions = new ArrayList<>();

      processOptions(bundle, dimensions, (dimension, option) -> {
        if (option != null) {
          bundleOptions.add(option);
        }
        return true;
      });
      if (!bundleOptions.isEmpty()) {
        dimensionBundles.add(new Bundle(bundleOptions));
      }
    }
    List<Bundle> metrics = new ArrayList<>(dimensionBundles);

    metrics.sort((b1, b2) -> {
      List<Option> l1 = new ArrayList<>();
      List<Option> l2 = new ArrayList<>();
      processOptions(b1, dimensions, (dimension, option) -> l1.add(option));
      processOptions(b2, dimensions, (dimension, option) -> l2.add(option));

      for (int i = 0; i < dimensions.size(); i++) {
        Option o1 = l1.get(i);
        Option o2 = l2.get(i);

        if (!Objects.equals(o1, o2)) {
          return o1 == null ? -1 : o1.compareTo(o2);
        }
      }
      return 0;
    });
    return metrics;
  }

  public Map<String, String> getOptionCriteria(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getCriteria() : null;
  }

  public String getOptionDescription(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getDescription() : null;
  }

  public Long getOptionPhoto(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getPhoto() : null;
  }

  public String getOptionPrice(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getPrice() : null;
  }

  public Collection<Option> getOptions() {
    return options.keySet();
  }

  public Set<Option> getPackets(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getPackets() : null;
  }

  public Map<String, String> getRelationCriteria(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getRelationCriteria(bundle) : null;
  }

  public String getRelationDescription(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getRelationDescription(bundle) : null;
  }

  public String getRelationPackets(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getRelationPackets(bundle) : null;
  }

  public Long getRelationPhoto(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getRelationPhoto(bundle) : null;
  }

  public String getRelationPrice(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getRelationPrice(bundle) : null;
  }

  public Set<Option> getRequiredOptions(Option option) {
    Set<Option> set = new HashSet<>();

    if (hasRestrictions(option)) {
      for (Map.Entry<Option, Boolean> entry : getRestrictions(option).entrySet()) {
        if (!entry.getValue()) {
          set.add(entry.getKey());
        }
      }
    }
    return set;
  }

  public Map<Option, Boolean> getRestrictions(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) ? optionInfo.getRestrictions() : null;
  }

  public List<Dimension> getRowDimensions() {
    return rowDimensions;
  }

  public boolean hasRelation(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) && optionInfo.hasRelation(bundle);
  }

  public boolean hasRelations(Bundle bundle) {
    return options.values().stream().anyMatch(optionInfo -> optionInfo.hasRelation(bundle));
  }

  public boolean hasRestrictions(Option option) {
    OptionInfo optionInfo = options.get(option);
    return Objects.nonNull(optionInfo) && optionInfo.hasRestrictions();
  }

  public boolean isBundleBlocked(Bundle bundle) {
    Pair<ConfInfo, Boolean> pair = bundles.get(bundle);
    return pair != null && BeeUtils.unbox(pair.getB());
  }

  public boolean isDefault(Option option, Bundle bundle) {
    return DEFAULT_PRICE.equals(getRelationPrice(option, bundle));
  }

  public boolean isEmpty() {
    return bundles.isEmpty();
  }

  public static List<Option> processOptions(Bundle bundle, Collection<Dimension> dimensions,
      BiFunction<Dimension, Option, Boolean> consumer) {
    List<Option> options = new ArrayList<>();

    if (bundle != null) {
      options.addAll(bundle.getOptions());
    }
    for (Dimension dimension : dimensions) {
      boolean found = false;

      for (Iterator<Option> it = options.iterator(); it.hasNext(); ) {
        Option option = it.next();
        found = Objects.equals(option.getDimension(), dimension);

        if (found) {
          if (consumer != null && !BeeUtils.unbox(consumer.apply(dimension, option))) {
            return options;
          }
          it.remove();
          break;
        }
      }
      if (!found && consumer != null && !BeeUtils.unbox(consumer.apply(dimension, null))) {
        break;
      }
    }
    return options;
  }

  public Set<Bundle> removeBundles(Set<Bundle> oldBundles) {
    Set<Bundle> realBundles = new HashSet<>();

    for (Bundle bundle : oldBundles) {
      if (Objects.nonNull(bundles.remove(bundle))) {
        realBundles.add(bundle);
        removeRelations(bundle);
      }
    }
    return realBundles;
  }

  public Set<Bundle> removeBundlesByDimension(Dimension dimension) {
    Set<Bundle> realBundles = new HashSet<>();

    for (Iterator<Map.Entry<Bundle, Pair<ConfInfo, Boolean>>> iterator = bundles
        .entrySet().iterator();
         iterator.hasNext(); ) {
      Map.Entry<Bundle, Pair<ConfInfo, Boolean>> entry = iterator.next();
      Bundle bundle = entry.getKey();

      for (Option option : bundle.getOptions()) {
        if (Objects.equals(option.getDimension(), dimension)) {
          if (Objects.nonNull(entry.getValue()) && Objects.nonNull(entry.getValue().getA())) {
            realBundles.add(bundle);
          }
          removeRelations(bundle);
          iterator.remove();
          break;
        }
      }
    }
    return realBundles;
  }

  public void removeOption(Option option) {
    options.remove(option);
  }

  public void removeRelation(Option option, Bundle bundle) {
    OptionInfo optionInfo = options.get(option);

    if (Objects.nonNull(optionInfo)) {
      optionInfo.removeRelation(bundle);
    }
  }

  public void removeRestriction(Option option, Option relatedOption) {
    Boolean oldValue = getRestrictions(option).remove(relatedOption);

    if (BeeUtils.unbox(oldValue)) {
      getRestrictions(relatedOption).remove(option);
    }
  }

  public static Configuration restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Configuration conf = new Configuration();
    conf.deserialize(s);
    return conf;
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ROW_DIMENSIONS:
          arr[i++] = rowDimensions;
          break;

        case COL_DIMENSIONS:
          arr[i++] = colDimensions;
          break;

        case BUNDLES:
          arr[i++] = bundles;
          break;

        case OPTIONS:
          arr[i++] = options;
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setBundleInfo(Bundle bundle, ConfInfo info, Boolean blocked) {
    addBundle(bundle);
    bundles.put(bundle, Objects.isNull(info) ? null : Pair.of(info, blocked));

    if (Objects.isNull(bundles.get(bundle))) {
      removeRelations(bundle);
    }
  }

  public void setOptionInfo(Option option, ConfInfo info) {
    addOption(option).setInfo(info);
  }

  public void setRelationInfo(Option option, Bundle bundle, ConfInfo info, String packet) {
    addOption(option).setRelationInfo(bundle, info, packet);
  }

  public void setRestriction(Option option, Option relatedOption, boolean denied) {
    Boolean oldValue = getRestrictions(option).put(relatedOption, denied);

    if (denied) {
      getRestrictions(relatedOption).put(option, denied);
    } else if (BeeUtils.unbox(oldValue)) {
      getRestrictions(relatedOption).remove(option);
    }
  }

  private OptionInfo addOption(Option option) {
    OptionInfo optionInfo = options.get(option);

    if (Objects.isNull(optionInfo)) {
      optionInfo = new OptionInfo();
      options.put(option, optionInfo);
    }
    return optionInfo;
  }

  private void removeRelations(Bundle bundle) {
    for (OptionInfo optionInfo : options.values()) {
      optionInfo.removeRelation(bundle);
    }
  }
}
