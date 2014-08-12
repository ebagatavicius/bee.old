package com.butent.bee.shared.ui;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Color implements BeeSerializable {

  private static final String HEX_PREFIX = "#";

  private static final Map<String, String> names = Maps.newHashMap();

  private static final Splitter rgbSplitter =
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults();
  private static final Splitter hslSplitter =
      Splitter.on(BeeConst.CHAR_COMMA).omitEmptyStrings().trimResults();

  static {
    initNames();
  }

  public static String blend(String c1, String c2) {
    if (BeeUtils.isEmpty(c1)) {
      return c2;
    } else if (BeeUtils.isEmpty(c2) || c1.equals(c2)) {
      return c1;
    } else {
      return blend(Lists.newArrayList(c1, c2));
    }
  }

  public static String blend(Collection<String> input) {
    if (BeeUtils.isEmpty(input)) {
      return null;
    }

    List<int[]> colors = new ArrayList<>();
    for (String s : input) {
      byte[] rgb = getRgb(s);

      if (rgb != null) {
        int[] color = new int[3];

        for (int i = 0; i < color.length; i++) {
          color[i] = rgb[i] & 0xff;
        }
        colors.add(color);
      }
    }

    if (colors.isEmpty()) {
      return null;
    }

    int[] result = new int[3];

    for (int[] color : colors) {
      for (int i = 0; i < result.length; i++) {
        result[i] += color[i];
      }
    }

    if (colors.size() > 1) {
      for (int i = 0; i < result.length; i++) {
        result[i] = BeeUtils.round(result[i] / (double) colors.size());
      }
    }

    return normalize(result[0], result[1], result[2]);
  }

  public static Map<String, String> getNames() {
    return names;
  }

  public static byte[] getRgb(String value) {
    String normalized = normalize(value);

    if (BeeUtils.hasLength(normalized, 7) && BeeUtils.isHexString(normalized.substring(1))) {
      byte[] arr = new byte[3];
      for (int i = 0; i < arr.length; i++) {
        arr[i] = (byte) Integer.parseInt(normalized.substring(i * 2 + 1, i * 2 + 3), 16);
      }
      return arr;

    } else {
      return null;
    }
  }

  public static String normalize(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    }

    String key = BeeUtils.remove(value.trim(), BeeConst.CHAR_SPACE).toLowerCase();
    if (names.containsKey(key)) {
      return names.get(key);

    } else if (key.startsWith(HEX_PREFIX)) {
      return BeeUtils.isHexString(key.substring(1)) ? normalizeHex(key.substring(1)) : null;

    } else if (key.startsWith("rgb")) {
      List<Integer> rgb = Lists.newArrayList();

      for (String s : rgbSplitter.split(key)) {
        int v = Math.max(extract(s), 0);

        if (s.contains(BeeConst.STRING_PERCENT)) {
          rgb.add(BeeUtils.round(255 * Math.min(v, 100) / 100.0));
        } else {
          rgb.add(Math.min(v, 255));
        }
      }

      if (rgb.size() >= 3) {
        return normalize(rgb.get(0), rgb.get(1), rgb.get(2));
      } else {
        return null;
      }

    } else if (key.startsWith("hsl")) {
      List<Integer> hsl = Lists.newArrayList();

      for (String s : hslSplitter.split(key)) {
        int v = extract(s);
        if (hsl.isEmpty()) {
          hsl.add((v % 360 + 360) % 360);
        } else {
          hsl.add(BeeUtils.clamp(v, 0, 100));
        }
      }

      if (hsl.size() >= 3) {
        int[] arr = hslToRgb(hsl.get(0) / 360.0, hsl.get(1) / 100.0, hsl.get(2) / 100.0);
        return normalize(arr[0], arr[1], arr[2]);
      } else {
        return null;
      }

    } else if (BeeUtils.isHexString(key)) {
      return normalizeHex(key);

    } else {
      return null;
    }
  }

  public static Color restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    Color color = new Color();
    color.deserialize(s);

    return color;
  }

  public static boolean validate(String value) {
    return !BeeUtils.isEmpty(normalize(value));
  }

  private static int extract(String input) {
    return BeeUtils.val(input, true);
  }

  private static int[] hslToRgb(double hue, double sat, double light) {
    double m2 = (light <= 0.5) ? light * (sat + 1) : light + sat - light * sat;
    double m1 = light * 2 - m2;

    double r = hueToRgb(m1, m2, hue + 1.0 / 3);
    double g = hueToRgb(m1, m2, hue);
    double b = hueToRgb(m1, m2, hue - 1.0 / 3);

    return new int[] {BeeUtils.round(r * 255), BeeUtils.round(g * 255), BeeUtils.round(b * 255)};
  }

  private static double hueToRgb(double m1, double m2, double z) {
    double x = (z < 0) ? z + 1 : (z > 1) ? z - 1 : z;

    if (x * 6 < 1) {
      return m1 + (m2 - m1) * x * 6;
    } else if (x * 2 < 1) {
      return m2;
    } else if (x * 3 < 2) {
      return m1 + (m2 - m1) * (2.0 / 3 - x) * 6;
    } else {
      return m1;
    }
  }

  private static void initNames() {
    if (!names.isEmpty()) {
      names.clear();
    }

    names.put(Colors.ALICEBLUE, "#F0F8FF");
    names.put(Colors.ANTIQUEWHITE, "#FAEBD7");
    names.put(Colors.AQUA, "#00FFFF");
    names.put(Colors.AQUAMARINE, "#7FFFD4");
    names.put(Colors.AZURE, "#F0FFFF");
    names.put(Colors.BEIGE, "#F5F5DC");
    names.put(Colors.BISQUE, "#FFE4C4");
    names.put(Colors.BLACK, "#000000");
    names.put(Colors.BLANCHEDALMOND, "#FFEBCD");
    names.put(Colors.BLUE, "#0000FF");
    names.put(Colors.BLUEVIOLET, "#8A2BE2");
    names.put(Colors.BROWN, "#A52A2A");
    names.put(Colors.BURLYWOOD, "#DEB887");
    names.put(Colors.CADETBLUE, "#5F9EA0");
    names.put(Colors.CHARTREUSE, "#7FFF00");
    names.put(Colors.CHOCOLATE, "#D2691E");
    names.put(Colors.CORAL, "#FF7F50");
    names.put(Colors.CORNFLOWERBLUE, "#6495ED");
    names.put(Colors.CORNSILK, "#FFF8DC");
    names.put(Colors.CRIMSON, "#DC143C");
    names.put(Colors.CYAN, "#00FFFF");
    names.put(Colors.DARKBLUE, "#00008B");
    names.put(Colors.DARKCYAN, "#008B8B");
    names.put(Colors.DARKGOLDENROD, "#B8860B");
    names.put(Colors.DARKGRAY, "#A9A9A9");
    names.put(Colors.DARKGREEN, "#006400");
    names.put(Colors.DARKGREY, "#A9A9A9");
    names.put(Colors.DARKKHAKI, "#BDB76B");
    names.put(Colors.DARKMAGENTA, "#8B008B");
    names.put(Colors.DARKOLIVEGREEN, "#556B2F");
    names.put(Colors.DARKORANGE, "#FF8C00");
    names.put(Colors.DARKORCHID, "#9932CC");
    names.put(Colors.DARKRED, "#8B0000");
    names.put(Colors.DARKSALMON, "#E9967A");
    names.put(Colors.DARKSEAGREEN, "#8FBC8F");
    names.put(Colors.DARKSLATEBLUE, "#483D8B");
    names.put(Colors.DARKSLATEGRAY, "#2F4F4F");
    names.put(Colors.DARKSLATEGREY, "#2F4F4F");
    names.put(Colors.DARKTURQUOISE, "#00CED1");
    names.put(Colors.DARKVIOLET, "#9400D3");
    names.put(Colors.DEEPPINK, "#FF1493");
    names.put(Colors.DEEPSKYBLUE, "#00BFFF");
    names.put(Colors.DIMGRAY, "#696969");
    names.put(Colors.DIMGREY, "#696969");
    names.put(Colors.DODGERBLUE, "#1E90FF");
    names.put(Colors.FIREBRICK, "#B22222");
    names.put(Colors.FLORALWHITE, "#FFFAF0");
    names.put(Colors.FORESTGREEN, "#228B22");
    names.put(Colors.FUCHSIA, "#FF00FF");
    names.put(Colors.GAINSBORO, "#DCDCDC");
    names.put(Colors.GHOSTWHITE, "#F8F8FF");
    names.put(Colors.GOLD, "#FFD700");
    names.put(Colors.GOLDENROD, "#DAA520");
    names.put(Colors.GRAY, "#808080");
    names.put(Colors.GREEN, "#008000");
    names.put(Colors.GREENYELLOW, "#ADFF2F");
    names.put(Colors.GREY, "#808080");
    names.put(Colors.HONEYDEW, "#F0FFF0");
    names.put(Colors.HOTPINK, "#FF69B4");
    names.put(Colors.INDIANRED, "#CD5C5C");
    names.put(Colors.INDIGO, "#4B0082");
    names.put(Colors.IVORY, "#FFFFF0");
    names.put(Colors.KHAKI, "#F0E68C");
    names.put(Colors.LAVENDER, "#E6E6FA");
    names.put(Colors.LAVENDERBLUSH, "#FFF0F5");
    names.put(Colors.LAWNGREEN, "#7CFC00");
    names.put(Colors.LEMONCHIFFON, "#FFFACD");
    names.put(Colors.LIGHTBLUE, "#ADD8E6");
    names.put(Colors.LIGHTCORAL, "#F08080");
    names.put(Colors.LIGHTCYAN, "#E0FFFF");
    names.put(Colors.LIGHTGOLDENRODYELLOW, "#FAFAD2");
    names.put(Colors.LIGHTGRAY, "#D3D3D3");
    names.put(Colors.LIGHTGREEN, "#90EE90");
    names.put(Colors.LIGHTGREY, "#D3D3D3");
    names.put(Colors.LIGHTPINK, "#FFB6C1");
    names.put(Colors.LIGHTSALMON, "#FFA07A");
    names.put(Colors.LIGHTSEAGREEN, "#20B2AA");
    names.put(Colors.LIGHTSKYBLUE, "#87CEFA");
    names.put(Colors.LIGHTSLATEGRAY, "#778899");
    names.put(Colors.LIGHTSLATEGREY, "#778899");
    names.put(Colors.LIGHTSTEELBLUE, "#B0C4DE");
    names.put(Colors.LIGHTYELLOW, "#FFFFE0");
    names.put(Colors.LIME, "#00FF00");
    names.put(Colors.LIMEGREEN, "#32CD32");
    names.put(Colors.LINEN, "#FAF0E6");
    names.put(Colors.MAGENTA, "#FF00FF");
    names.put(Colors.MAROON, "#800000");
    names.put(Colors.MEDIUMAQUAMARINE, "#66CDAA");
    names.put(Colors.MEDIUMBLUE, "#0000CD");
    names.put(Colors.MEDIUMORCHID, "#BA55D3");
    names.put(Colors.MEDIUMPURPLE, "#9370DB");
    names.put(Colors.MEDIUMSEAGREEN, "#3CB371");
    names.put(Colors.MEDIUMSLATEBLUE, "#7B68EE");
    names.put(Colors.MEDIUMSPRINGGREEN, "#00FA9A");
    names.put(Colors.MEDIUMTURQUOISE, "#48D1CC");
    names.put(Colors.MEDIUMVIOLETRED, "#C71585");
    names.put(Colors.MIDNIGHTBLUE, "#191970");
    names.put(Colors.MINTCREAM, "#F5FFFA");
    names.put(Colors.MISTYROSE, "#FFE4E1");
    names.put(Colors.MOCCASIN, "#FFE4B5");
    names.put(Colors.NAVAJOWHITE, "#FFDEAD");
    names.put(Colors.NAVY, "#000080");
    names.put(Colors.OLDLACE, "#FDF5E6");
    names.put(Colors.OLIVE, "#808000");
    names.put(Colors.OLIVEDRAB, "#6B8E23");
    names.put(Colors.ORANGE, "#FFA500");
    names.put(Colors.ORANGERED, "#FF4500");
    names.put(Colors.ORCHID, "#DA70D6");
    names.put(Colors.PALEGOLDENROD, "#EEE8AA");
    names.put(Colors.PALEGREEN, "#98FB98");
    names.put(Colors.PALETURQUOISE, "#AFEEEE");
    names.put(Colors.PALEVIOLETRED, "#DB7093");
    names.put(Colors.PAPAYAWHIP, "#FFEFD5");
    names.put(Colors.PEACHPUFF, "#FFDAB9");
    names.put(Colors.PERU, "#CD853F");
    names.put(Colors.PINK, "#FFC0CB");
    names.put(Colors.PLUM, "#DDA0DD");
    names.put(Colors.POWDERBLUE, "#B0E0E6");
    names.put(Colors.PURPLE, "#800080");
    names.put(Colors.RED, "#FF0000");
    names.put(Colors.ROSYBROWN, "#BC8F8F");
    names.put(Colors.ROYALBLUE, "#4169E1");
    names.put(Colors.SADDLEBROWN, "#8B4513");
    names.put(Colors.SALMON, "#FA8072");
    names.put(Colors.SANDYBROWN, "#F4A460");
    names.put(Colors.SEAGREEN, "#2E8B57");
    names.put(Colors.SEASHELL, "#FFF5EE");
    names.put(Colors.SIENNA, "#A0522D");
    names.put(Colors.SILVER, "#C0C0C0");
    names.put(Colors.SKYBLUE, "#87CEEB");
    names.put(Colors.SLATEBLUE, "#6A5ACD");
    names.put(Colors.SLATEGRAY, "#708090");
    names.put(Colors.SLATEGREY, "#708090");
    names.put(Colors.SNOW, "#FFFAFA");
    names.put(Colors.SPRINGGREEN, "#00FF7F");
    names.put(Colors.STEELBLUE, "#4682B4");
    names.put(Colors.TAN, "#D2B48C");
    names.put(Colors.TEAL, "#008080");
    names.put(Colors.THISTLE, "#D8BFD8");
    names.put(Colors.TOMATO, "#FF6347");
    names.put(Colors.TURQUOISE, "#40E0D0");
    names.put(Colors.VIOLET, "#EE82EE");
    names.put(Colors.WHEAT, "#F5DEB3");
    names.put(Colors.WHITE, "#FFFFFF");
    names.put(Colors.WHITESMOKE, "#F5F5F5");
    names.put(Colors.YELLOW, "#FFFF00");
    names.put(Colors.YELLOWGREEN, "#9ACD32");
  }

  private static String normalize(int r, int g, int b) {
    return HEX_PREFIX + toHex(r) + toHex(g) + toHex(b);
  }

  private static String normalizeHex(String hex) {
    if (hex.length() == 3) {
      String r = hex.substring(0, 1).toUpperCase();
      String g = hex.substring(1, 2).toUpperCase();
      String b = hex.substring(2, 3).toUpperCase();

      return HEX_PREFIX + r + r + g + g + b + b;

    } else if (hex.length() == 6) {
      return HEX_PREFIX + hex.toUpperCase();

    } else {
      return null;
    }
  }

  private static String toHex(int value) {
    String hex = Integer.toHexString(value);
    return (hex.length() == 1) ? BeeConst.STRING_ZERO + hex : hex;
  }

  private long id;

  private String background;
  private String foreground;

  public Color(long id, String background, String foreground) {
    super();
    this.id = id;
    this.background = background;
    this.foreground = foreground;
  }

  private Color() {
    super();
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 3);

    setId(BeeUtils.toLong(arr[0]));
    setBackground(arr[1]);
    setForeground(arr[2]);
  }

  public String getBackground() {
    return background;
  }

  public String getForeground() {
    return foreground;
  }

  public long getId() {
    return id;
  }

  @Override
  public String serialize() {
    List<String> values = Lists.newArrayList(BeeUtils.toString(getId()), getBackground(),
        getForeground());
    return Codec.beeSerialize(values);
  }

  private void setBackground(String background) {
    this.background = background;
  }

  private void setForeground(String foreground) {
    this.foreground = foreground;
  }

  private void setId(long id) {
    this.id = id;
  }
}
