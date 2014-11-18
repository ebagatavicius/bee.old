package com.butent.bee.codegen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class CssEnums {

  private static final String DIRECTORY = "src/com/butent/bee/shared/css/values/";
  private static final ListMultimap<String, String> types = ArrayListMultimap.create();

  public static void run() {
    init();
    generateAll();
  }

  private static void generateAll() {
    for (String type : types.keySet()) {
      List<String> values = types.get(type);
      generateType(type, values);
      System.out.println(type + " " + values);
    }
  }

  private static void generateType(String type, List<String> values) {
    List<String> lines = new ArrayList<>();
    lines.add("package com.butent.bee.shared.css.values;");
    lines.add("import com.butent.bee.shared.css.HasCssName;");
    lines.add("public enum " + type + " implements HasCssName {");

    for (int i = 0; i < values.size(); i++) {
      String value = values.get(i).trim();
      String name = value.replace('-', '_').replace(' ', '_').toUpperCase();
      if (Character.isDigit(name.charAt(0))) {
        name = "_" + name;
      }

      lines.add(name + " {");

      lines.add("@Override");
      lines.add("public String getCssName() {");
      lines.add("return \"" + value + "\";");
      lines.add("}");

      lines.add((i < values.size() - 1) ? "}," : "}");
    }
    lines.add("}");

    Path out = Paths.get(DIRECTORY, type + ".java");
    try {
      Files.write(out, lines, Charset.defaultCharset());
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static void init() {
    types.putAll("AlignContent", Lists.newArrayList("flex-start", "flex-end", "center",
        "space-between", "space-around", "stretch"));
    types.putAll("AlignItems", Lists.newArrayList("flex-start", "flex-end", "center",
        "baseline", "stretch"));
    types.putAll("AlignSelf", Lists.newArrayList("auto", "flex-start", "flex-end", "center",
        "baseline", "stretch"));

    types.putAll("AlignmentAdjust", Lists.newArrayList("auto", "baseline", "before-edge",
        "text-before-edge", "middle", "central", "after-edge", "text-after-edge",
        "ideographic", "alphabetic", "hanging", "mathematical"));
    types.putAll("AlignmentBaseline", Lists.newArrayList("baseline", "use-script", "before-edge",
        "text-before-edge", "after-edge", "text-after-edge", "central", "middle", "ideographic",
        "alphabetic", "hanging", "mathematical"));

    types.putAll("All", Lists.newArrayList("initial", "inherit", "unset"));

    types.putAll("AnimationDirection", Lists.newArrayList("normal", "reverse", "alternate",
        "alternate-reverse"));
    types.putAll("AnimationFillMode", Lists.newArrayList("none", "forwards", "backwards",
        "both"));
    types.putAll("AnimationIterationCount", Lists.newArrayList("infinite"));
    types.putAll("AnimationPlayState", Lists.newArrayList("running", "paused"));
    types.putAll("AnimationTimingFunction", Lists.newArrayList("ease", "linear", "ease-in",
        "ease-out", "ease-in-out", "step-start", "step-end"));

    types.putAll("BackfaceVisibility", Lists.newArrayList("visible", "hidden"));

    types.putAll("BackgroundAttachment", Lists.newArrayList("scroll", "fixed", "local"));
    types.putAll("BackgroundClip", Lists.newArrayList("border-box", "padding-box",
        "content-box"));
    types.putAll("BackgroundOrigin", Lists.newArrayList("border-box", "padding-box",
        "content-box"));
    types.putAll("BackgroundPosition", Lists.newArrayList("left", "center", "right", "top",
        "bottom"));
    types.putAll("BackgroundRepeat", Lists.newArrayList("repeat-x", "repeat-y", "repeat",
        "space",
        "round", "no-repeat"));
    types.putAll("BackgroundSize", Lists.newArrayList("auto", "cover", "contain"));

    types.putAll("BaselineShift", Lists.newArrayList("baseline", "sub", "super"));

    types.putAll("BookmarkState", Lists.newArrayList("open", "closed"));

    types.putAll("BorderStyle", Lists.newArrayList("none", "hidden", "dotted", "dashed", "solid",
        "double", "groove", "ridge", "inset", "outset"));
    types.putAll("BorderWidth", Lists.newArrayList("thin", "medium", "thick"));
    types.putAll("BorderCollapse", Lists.newArrayList("collapse", "separate", "inherit"));
    types.putAll("BorderImageRepeat", Lists.newArrayList("stretch", "repeat", "round", "space"));

    types.putAll("BoxDecorationBreak", Lists.newArrayList("slice", "clone"));
    types.putAll("BoxSizing", Lists.newArrayList("content-box", "padding-box", "border-box",
        "inherit"));

    types.putAll("BreakAfter", Lists.newArrayList("auto", "always", "avoid", "left", "right",
        "page", "column", "avoid-page", "avoid-column"));
    types.putAll("BreakBefore", Lists.newArrayList("auto", "always", "avoid", "left", "right",
        "page", "column", "avoid-page", "avoid-column"));
    types.putAll("BreakInside", Lists.newArrayList("auto", "avoid", "avoid-page",
        "avoid-column"));

    types.putAll("CaptionSide", Lists.newArrayList("top", "bottom", "inherit"));

    types.putAll("Clear", Lists.newArrayList("none", "left", "right", "both"));

    types.putAll("ColumnFill", Lists.newArrayList("auto", "balance"));
    types.putAll("ColumnSpan", Lists.newArrayList("none", "all"));

    types.putAll("Cursor", Lists.newArrayList("auto", "default", "none",
        "context-menu", "help", "pointer", "progress", "wait",
        "cell", "crosshair", "text", "vertical-text",
        "alias", "copy", "move", "no-drop", "not-allowed",
        "e-resize", "n-resize", "ne-resize", "nw-resize", "s-resize", "se-resize", "sw-resize",
        "w-resize", "ew-resize", "ns-resize", "nesw-resize", "nwse-resize", "col-resize",
        "row-resize", "all-scroll", "zoom-in", "zoom-out", "inherit"));

    types.putAll("Direction", Lists.newArrayList("ltr", "rtl", "inherit"));

    types.putAll("Display", Lists.newArrayList("inline", "block", "inline-block", "list-item",
        "run-in", "compact", "table", "inline-table", "table-row-group", "table-header-group",
        "table-footer-group", "table-row", "table-column-group", "table-column", "table-cell",
        "table-caption", "ruby", "ruby-base", "ruby-text", "ruby-base-group", "ruby-text-group",
        "flex", "inline-flex", "grid", "inline-grid", "marker", "normal", "none"));

    types.putAll("DominantBaseline", Lists.newArrayList("auto", "use-script", "no-change",
        "reset-size", "alphabetic", "hanging", "ideographic", "mathematical", "central", "middle",
        "text-after-edge", "text-before-edge"));

    types.putAll("DropInitialAfterAdjust", Lists.newArrayList("central", "middle", "after-edge",
        "text-after-edge", "ideographic", "alphabetic", "mathematical"));
    types.putAll("DropInitialAfterAlign", Lists.newArrayList("baseline", "use-script",
        "before-edge", "text-before-edge", "after-edge", "text-after-edge", "central", "middle",
        "ideographic", "alphabetic", "hanging", "mathematical"));
    types.putAll("DropInitialBeforeAdjust", Lists.newArrayList("before-edge", "text-before-edge",
        "middle", "central", "after-edge", "text-after-edge", "ideographic", "hanging",
        "mathematical"));
    types.putAll("DropInitialBeforeAlign", Lists.newArrayList("baseline", "use-script",
        "before-edge", "text-before-edge", "after-edge", "text-after-edge", "central", "middle",
        "ideographic", "alphabetic", "hanging", "mathematical", "caps-height"));

    types.putAll("EmptyCells", Lists.newArrayList("show", "hide", "inherit"));

    types.putAll("ObjectFit", Lists.newArrayList("fill", "contain", "cover", "none",
        "scale-down"));

    types.putAll("FlexDirection", Lists.newArrayList("row", "row-reverse", "column",
        "column-reverse"));
    types.putAll("FlexWrap", Lists.newArrayList("nowrap", "wrap", "wrap-reverse"));
    types.putAll("Float", Lists.newArrayList("left", "right", "none"));

    types.putAll("FontKerning", Lists.newArrayList("auto", "normal", "none"));
    types.putAll("FontSize", Lists.newArrayList("xx-small", "x-small", "small", "medium",
        "large",
        "x-large", "xx-large", "larger", "smaller"));
    types.putAll("FontStretch", Lists.newArrayList("normal", "ultra-condensed",
        "extra-condensed",
        "condensed", "semi-condensed", "semi-expanded", "expanded", "extra-expanded",
        "ultra-expanded"));
    types.putAll("FontStyle", Lists.newArrayList("normal", "italic", "oblique"));
    types.putAll("FontVariant", Lists.newArrayList("normal", "none", "historical-forms",
        "small-caps", "all-small-caps", "petite-caps", "all-petite-caps", "unicase",
        "titling-caps", "ordinal", "slashed-zero", "ruby"));
    types.putAll("FontVariantCaps", Lists.newArrayList("normal", "small-caps", "all-small-caps",
        "petite-caps", "all-petite-caps", "unicase", "titling-caps"));
    types.putAll("FontVariantPosition", Lists.newArrayList("normal", "sub", "super"));
    types.putAll("FontWeight", Lists.newArrayList("normal", "bold", "bolder", "lighter",
        "100", "200", "300", "400", "500", "600", "700", "800", "900"));

    types.putAll("HangingPunctuation", Lists.newArrayList("none", "first", "force-end",
        "allow-end", "last"));
    types.putAll("Hyphens", Lists.newArrayList("none", "manual", "auto"));

    types.putAll("ImageRendering", Lists.newArrayList("auto", "crisp-edges", "pixelated"));

    types.putAll("ImeMode", Lists.newArrayList("auto", "normal", "active", "inactive",
        "disabled",
        "inherit"));

    types.putAll("InlineBoxAlign", Lists.newArrayList("initial", "last"));

    types.putAll("JustifyContent", Lists.newArrayList("flex-start", "flex-end", "center",
        "space-between", "space-around"));

    types.putAll("LineBreak", Lists.newArrayList("auto", "loose", "normal", "strict"));

    types.putAll("LineStacking", Lists.newArrayList("inline-line-height",
        "block-line-height", "max-height", "grid-height", "exclude-ruby", "include-ruby",
        "consider-shifts", "disregard-shifts"));
    types.putAll("LineStackingRuby", Lists.newArrayList("exclude-ruby", "include-ruby"));
    types.putAll("LineStackingShift", Lists.newArrayList("consider-shifts", "disregard-shifts"));
    types.putAll("LineStackingStrategy", Lists.newArrayList("inline-line-height",
        "block-line-height", "max-height", "grid-height"));

    types.putAll("ListStylePosition", Lists.newArrayList("inside", "hanging", "outside"));

    types.putAll("MarqueeDirection", Lists.newArrayList("forward", "reverse"));
    types.putAll("MarqueeSpeed", Lists.newArrayList("slow", "normal", "fast"));
    types.putAll("MarqueeStyle", Lists.newArrayList("scroll", "slide", "alternate"));

    types.putAll("Overflow", Lists.newArrayList("visible", "hidden", "scroll", "auto",
        "no-display", "no-content"));
    types.putAll("OverflowStyle", Lists.newArrayList("auto", "scrollbar", "panner", "move",
        "marquee", "marquee-line", "marquee-block"));

    types.putAll("OverflowWrap", Lists.newArrayList("normal", "break-word"));
    types.putAll("WordWrap", Lists.newArrayList("normal", "break-word"));

    types.putAll("PageBreakAfter", Lists.newArrayList("auto", "always", "avoid", "left", "right",
        "inherit"));
    types.putAll("PageBreakBefore", Lists.newArrayList("auto", "always", "avoid", "left",
        "right", "inherit"));
    types.putAll("PageBreakInside", Lists.newArrayList("auto", "avoid", "inherit"));

    types.putAll("PagePolicy", Lists.newArrayList("start", "first", "last"));

    types.putAll("Pause", Lists.newArrayList("none", "x-weak", "weak", "medium", "strong",
        "x-strong"));

    types.putAll("PerspectiveOrigin", Lists.newArrayList("left", "center", "right", "top",
        "bottom"));
    types.putAll("TransformOrigin", Lists.newArrayList("left", "center", "right", "top",
        "bottom"));

    types.putAll("Pitch", Lists.newArrayList("x-low", "low", "medium", "high", "x-high",
        "inherit"));

    types.putAll("Position", Lists.newArrayList("static", "relative", "absolute", "fixed",
        "inherit"));

    types.putAll("PresentationLevel", Lists.newArrayList("same", "increment"));

    types.putAll("PunctuationTrim", Lists.newArrayList("none", "start", "end", "allow-end",
        "adjacent"));

    types.putAll("RenderingIntent", Lists.newArrayList("auto", "perceptual",
        "relative-colorimetric", "saturation", "absolute-colorimetric", "inherit"));

    types.putAll("Resize", Lists.newArrayList("none", "both", "horizontal", "vertical",
        "inherit"));

    types.putAll("Rest", Lists.newArrayList("none", "x-weak", "weak", "medium", "strong",
        "x-strong"));

    types.putAll("RubyAlign", Lists.newArrayList("start", "center", "space-between",
        "space-around"));
    types.putAll("RubyMerge", Lists.newArrayList("separate", "collapse", "auto"));
    types.putAll("RubyPosition", Lists.newArrayList("over", "under", "inter-character",
        "right", "left"));

    types.putAll("Speak", Lists.newArrayList("auto", "none", "normal"));
    types.putAll("SpeakAs", Lists.newArrayList("normal", "spell-out", "digits",
        "literal-punctuation", "no-punctuation"));
    types.putAll("SpeakHeader", Lists.newArrayList("once", "always", "inherit"));
    types.putAll("SpeakNumeral", Lists.newArrayList("digits", "continuous", "inherit"));
    types.putAll("SpeakPunctuation", Lists.newArrayList("code", "none", "inherit"));

    types.putAll("SpeechRate", Lists.newArrayList("x-slow", "slow", "medium", "fast", "x-fast",
        "faster", "slower", "inherit"));

    types.putAll("TableLayout", Lists.newArrayList("auto", "fixed", "inherit"));

    types.putAll("TargetName", Lists.newArrayList("current", "root", "parent", "new", "modal"));
    types.putAll("TargetNew", Lists.newArrayList("window", "tab", "none"));
    types.putAll("TargetPosition", Lists.newArrayList("above", "behind", "front", "back"));

    types.putAll("TextAlign", Lists.newArrayList("start", "end", "left", "right", "center",
        "justify", "match-parent", "start end"));
    types.putAll("TextAlignLast", Lists.newArrayList("auto", "start", "end", "left", "right",
        "center", "justify"));
    types.putAll("TextDecorationLine", Lists.newArrayList("none", "underline", "overline",
        "line-through", "blink"));
    types.putAll("TextDecorationSkip", Lists.newArrayList("none", "objects", "spaces", "ink",
        "edges", "box-decoration"));
    types.putAll("TextDecorationStyle", Lists.newArrayList("solid", "double", "dotted", "dashed",
        "wavy"));
    types.putAll("TextEmphasisStyle", Lists.newArrayList("none", "filled", "open", "dot",
        "circle",
        "double-circle", "triangle", "sesame"));
    types.putAll("TextEmphasisPosition", Lists.newArrayList("over", "under", "right", "left"));
    types.putAll("TextJustify", Lists.newArrayList("auto", "none", "inter-word", "distribute"));
    types.putAll("TextTransform", Lists.newArrayList("none", "capitalize", "uppercase",
        "lowercase", "full-width"));
    types.putAll("TextUnderlinePosition", Lists.newArrayList("auto", "under", "left", "right"));

    types.putAll("WhiteSpace", Lists.newArrayList("normal", "pre", "nowrap", "pre-wrap",
        "pre-line"));

    types.putAll("WordBreak", Lists.newArrayList("normal", "keep-all", "break-all"));

    types.putAll("TextHeight", Lists.newArrayList("auto", "font-size", "text-size", "max-size"));
    types.putAll("TextOverflow", Lists.newArrayList("clip", "ellipsis", "inherit"));

    types.putAll("TransformStyle", Lists.newArrayList("flat", "preserve-3d"));

    types.putAll("TransitionTimingFunction", Lists.newArrayList("ease", "linear", "ease-in",
        "ease-out", "ease-in-out", "step-start", "step-end"));

    types.putAll("UnicodeBidi", Lists.newArrayList("normal", "embed", "bidi-override",
        "inherit"));
    types.putAll("VerticalAlign", Lists.newArrayList("auto", "use-script", "baseline", "sub",
        "super", "top", "text-top", "central", "middle", "bottom", "text-bottom"));

    types.putAll("Visibility", Lists.newArrayList("visible", "hidden", "collapse"));

    types.putAll("VoiceBalance", Lists.newArrayList("left", "center", "right", "leftwards",
        "rightwards"));
    types.putAll("VoiceRate", Lists.newArrayList("normal", "x-slow", "slow", "medium", "fast",
        "x-fast"));
    types.putAll("VoiceStress", Lists.newArrayList("normal", "strong", "moderate", "none",
        "reduced"));

    types.putAll("Volume", Lists.newArrayList("silent", "x-soft", "soft", "medium", "loud",
        "x-loud", "inherit"));
  }

  private CssEnums() {
  }
}
