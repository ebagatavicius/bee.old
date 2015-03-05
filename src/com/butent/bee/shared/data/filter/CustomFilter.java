package com.butent.bee.shared.data.filter;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.List;

public class CustomFilter extends Filter {

  private static final char PREFIX = '{';
  private static final char SUFFIX = '}';

  private static final String KEY_SEPARATOR = BeeConst.STRING_SPACE;
  private static final String ARG_SEPARATOR = BeeConst.STRING_COMMA;

  private static final Splitter splitter =
      Splitter.on(CharMatcher.anyOf(KEY_SEPARATOR + ARG_SEPARATOR)).omitEmptyStrings()
          .trimResults();

  static boolean is(String input) {
    return BeeUtils.isPrefix(input, PREFIX) && BeeUtils.isSuffix(input, SUFFIX)
        && !BeeUtils.isEmpty(unwrap(input));
  }

  static CustomFilter tryParse(String input) {
    String s = unwrap(input);
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    List<String> list = splitter.splitToList(s);
    if (list.isEmpty()) {
      return null;
    }

    CustomFilter filter = new CustomFilter(list.get(0));
    if (list.size() > 1) {
      filter.args.addAll(list.subList(1, list.size()));
    }

    return filter;
  }

  private static String unwrap(String input) {
    return BeeUtils.removeSuffix(BeeUtils.removePrefix(input, PREFIX), SUFFIX);
  }

  private String key;
  private final List<String> args = new ArrayList<>();

  protected CustomFilter() {
    super();
  }

  protected CustomFilter(String key) {
    super();
    this.key = key;
  }

  protected CustomFilter(String key, List<String> args) {
    this(key);
    if (!BeeUtils.isEmpty(args)) {
      this.args.addAll(args);
    }
  }

  @Override
  public void deserialize(String s) {
    setSafe();

    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.notNull(arr);
    Assert.isPositive(arr.length);

    this.key = arr[0];

    if (!args.isEmpty()) {
      args.clear();
    }
    if (arr.length > 1) {
      for (int i = 1; i < arr.length; i++) {
        args.add(arr[i]);
      }
    }
  }

  public String getArg(int index) {
    return BeeUtils.getQuietly(args, index);
  }

  public int getArgCount() {
    return args.size();
  }

  public List<String> getArgs() {
    return args;
  }

  public String getKey() {
    return key;
  }

  @Override
  public boolean involvesColumn(String colName) {
    return false;
  }

  @Override
  public boolean isMatch(List<? extends IsColumn> columns, IsRow row) {
    Assert.unsupported();
    return false;
  }

  @Override
  public String serialize() {
    List<String> values = Lists.newArrayList(key);
    if (!args.isEmpty()) {
      values.addAll(args);
    }
    return super.serialize(values);
  }

  @Override
  public String toString() {
    String s = args.isEmpty() ? key
        : BeeUtils.join(KEY_SEPARATOR, key, BeeUtils.join(ARG_SEPARATOR, args));
    return String.valueOf(PREFIX) + s + String.valueOf(SUFFIX);
  }
}
