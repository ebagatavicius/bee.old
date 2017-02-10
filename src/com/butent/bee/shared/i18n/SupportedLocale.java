package com.butent.bee.shared.i18n;

import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoDE;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoEN;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoET;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoFI;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoLT;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoLV;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfoRU;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public enum SupportedLocale implements HasCaption {
  LT {
    @Override
    public String getCaption() {
      return "Lietuviškai";
    }

    @Override
    public String getIconName() {
      return "lithuanian";
    }

    @Override
    public String getLanguage() {
      return "lt";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoLT.getInstance();
    }
  },

  EN {
    @Override
    public String getCaption() {
      return "English";
    }

    @Override
    public String getIconName() {
      return "english";
    }

    @Override
    public String getLanguage() {
      return "en";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoEN.getInstance();
    }
  },

  ET {
    @Override
    public String getCaption() {
      return "Eesti";
    }

    @Override
    public String getIconName() {
      return "estonian";
    }

    @Override
    public String getLanguage() {
      return "et";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoET.getInstance();
    }
  },

  RU {
    @Override
    public String getCaption() {
      return "Русский";
    }

    @Override
    public String getIconName() {
      return "russian";
    }

    @Override
    public String getLanguage() {
      return "ru";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoRU.getInstance();
    }
  },

  FI {
    @Override
    public String getCaption() {
      return "Suomi";
    }

    @Override
    public String getIconName() {
      return "finnish";
    }

    @Override
    public String getLanguage() {
      return "fi";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoFI.getInstance();
    }
  },

  LV {
    @Override
    public String getCaption() {
      return "Latviešu";
    }

    @Override
    public String getIconName() {
      return "latvian";
    }

    @Override
    public String getLanguage() {
      return "lv";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoLV.getInstance();
    }
  },

  DE {
    @Override
    public String getCaption() {
      return "Deutsch";
    }

    @Override
    public String getIconName() {
      return "german";
    }

    @Override
    public String getLanguage() {
      return "de";
    }

    @Override
    public DateTimeFormatInfo getDateTimeFormatInfo() {
      return DateTimeFormatInfoDE.getInstance();
    }
  };

  public static final SupportedLocale DICTIONARY_DEFAULT = EN;
  public static final SupportedLocale USER_DEFAULT = LT;
  public static final List<String> ACTIVE_LOCALES = new ArrayList<>();

  public static SupportedLocale getByLanguage(String language) {
    for (SupportedLocale locale : values()) {
      if (BeeUtils.same(locale.getLanguage(), language)) {
        return locale;
      }
    }
    return null;
  }

  public static String normalizeLanguage(String language) {
    return parse(language).getLanguage();
  }

  public static SupportedLocale parse(String language) {
    return BeeUtils.nvl(getByLanguage(language), USER_DEFAULT);
  }

  public static List<SupportedLocale> parseList(String languages) {
    List<SupportedLocale> result = new ArrayList<>();

    for (String language : NameUtils.toList(languages)) {
      SupportedLocale supportedLocale = getByLanguage(language);
      if (supportedLocale != null && !result.contains(supportedLocale)) {
        result.add(supportedLocale);
      }
    }

    return result;
  }

  public String getDictionaryCustomColumnName() {
    return "Custom" + getLanguage().toUpperCase();
  }

  public String getDictionaryDefaultColumnName() {
    return "Default" + getLanguage().toUpperCase();
  }

  public String getDictionaryFileName() {
    return "dictionary_" + getLanguage().toLowerCase();
  }

  public abstract String getIconName();

  public abstract String getLanguage();

  public abstract DateTimeFormatInfo getDateTimeFormatInfo();

  public boolean isActive() {
    return ACTIVE_LOCALES.stream()
        .anyMatch(loc -> Objects.equals(this, USER_DEFAULT) || BeeUtils.same(loc, getLanguage()));
  }
}
