package com.butent.bee.client.i18n;

import com.google.gwt.core.client.JavaScriptObject;

import com.butent.bee.client.dom.Features;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NullOrdering;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class Collator implements Comparator<String> {

  public enum Case {
    INSENSITIVE, SENSITIVE;
  }

  public static class Options extends JavaScriptObject {

    public static Options create() {
      return JavaScriptObject.createObject().cast();
    }

    protected Options() {
    }

    public final Options caseFirstFalse() {
      setCaseFirst("false");
      return this;
    }

    public final Options caseFirstLower() {
      setCaseFirst("lower");
      return this;
    }

    public final Options caseFirstUpper() {
      setCaseFirst("upper");
      return this;
    }

    public final Options localeMatcherBestFit() {
      setLocaleMatcher("best fit");
      return this;
    }

    public final Options localeMatcherLookup() {
      setLocaleMatcher("lookup");
      return this;
    }

    public final Options sensitivityAccent() {
      setSensitivity("accent");
      return this;
    }

    public final Options sensitivityBase() {
      setSensitivity("base");
      return this;
    }

    public final Options sensitivityCase() {
      setSensitivity("case");
      return this;
    }

    public final Options sensitivityVariant() {
      setSensitivity("variant");
      return this;
    }

    //@formatter:off
    public final native void setCaseFirst(String caseFirst) /*-{
      this.caseFirst = caseFirst;
    }-*/;

    public final native void setIgnorePunctuation(boolean ignorePunctuation) /*-{
      this.ignorePunctuation = ignorePunctuation;
    }-*/;

    public final native void setLocaleMatcher(String localeMatcher) /*-{
      this.localeMatcher = localeMatcher;
    }-*/;

    public final native void setNumeric(boolean numeric) /*-{
      this.numeric = numeric;
    }-*/;

    public final native void setSensitivity(String sensitivity) /*-{
      this.sensitivity = sensitivity;
    }-*/;

    public final native void setUsage(String usage) /*-{
      this.usage = usage;
    }-*/;
    //@formatter:on

    public final Options usageSearch() {
      setUsage("search");
      return this;
    }

    public final Options usageSort() {
      setUsage("sort");
      return this;
    }
  }

  public static final Collator CASE_SENSITIVE_NULLS_FIRST = new Collator(Case.SENSITIVE,
      NullOrdering.NULLS_FIRST, Localized.getConstants().languageTag());

  public static final Collator CASE_SENSITIVE_NULLS_LAST = new Collator(Case.SENSITIVE,
      NullOrdering.NULLS_LAST, Localized.getConstants().languageTag());

  public static final Collator CASE_INSENSITIVE_NULLS_FIRST = new Collator(Case.INSENSITIVE,
      NullOrdering.NULLS_FIRST, Localized.getConstants().languageTag());

  public static final Collator CASE_INSENSITIVE_NULLS_LAST = new Collator(Case.INSENSITIVE,
      NullOrdering.NULLS_LAST, Localized.getConstants().languageTag());

  public static final Collator DEFAULT = new Collator(Case.INSENSITIVE, NullOrdering.DEFAULT,
      Localized.getConstants().languageTag());

  private final Case caseSensitivity;
  private final NullOrdering nullOrdering;

  private final String locales;
  private final Options options;

  public Collator(Case caseSensitivity, NullOrdering nullOrdering, String locales) {
    this(caseSensitivity, nullOrdering, locales, null);
  }

  public Collator(Case caseSensitivity, NullOrdering nullOrdering, String locales,
      Options options) {

    this.caseSensitivity = caseSensitivity;
    this.nullOrdering = nullOrdering;

    if (Features.supportsIntl()) {
      this.locales = locales;
      this.options = options;
    } else {
      this.locales = null;
      this.options = null;
    }
  }

  @Override
  public int compare(String o1, String o2) {
    if (o1 == null || o2 == null) {
      return BeeUtils.compare(o1, o2, nullOrdering);

    } else if (locales == null) {
      return compareImpl(normalize(o1), normalize(o2));

    } else if (options == null) {
      return compareImpl(normalize(o1), normalize(o2), locales);

    } else {
      return compareImpl(normalize(o1), normalize(o2), locales, options);
    }
  }

  public void sort(List<String> list) {
    if (list != null && list.size() > 1) {
      Collections.sort(list, this);
    }
  }

//@formatter:off
  private native int compareImpl(String source, String target) /*-{
    return source.localeCompare(target);
  }-*/;

  private native int compareImpl(String source, String target, String loc) /*-{
    // chromium issue 314210
    var z;
    try {
      z = source.localeCompare(target, loc);
    } catch (err) {
      z = source.localeCompare(target);
    }
    return z;
  }-*/;

  private native int compareImpl(String source, String target, String loc, Options opt) /*-{
    return source.localeCompare(target, loc, opt);
  }-*/;
//@formatter:on

  private String normalize(String s) {
    return (caseSensitivity == Case.INSENSITIVE) ? s.toLowerCase() : s;
  }
}
