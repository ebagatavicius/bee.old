package com.butent.bee.client.output;

import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumSet;

public class ReportDateItem extends ReportItem {

  public enum DateTimeFunction implements HasLocalizedCaption {
    YEAR() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.year();
      }
    },
    YEAR_MONTH() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Metai.Mėnuo";
      }
    },
    MONTH() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.month();
      }
    },
    DAY() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Diena";
      }
    },
    DAY_OF_WEEK() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Savaitės diena";
      }
    },
    DATE() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.date();
      }
    },
    TIME() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.time();
      }
    },
    HOUR() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Valanda";
      }
    },
    MINUTE() {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return "Minutė";
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }
  }

  public ReportDateItem(String name, String caption) {
    super(name, caption);
  }

  @Override
  public ReportItem create() {
    return new ReportDateItem(getName(), getCaption());
  }

  @Override
  public String evaluate(SimpleRow row) {
    String value = null;
    JustDate date = row.getDate(getName());

    if (date != null) {
      DateTimeFunction fnc = EnumUtils.getEnumByName(DateTimeFunction.class, getOptions());

      if (fnc != null) {
        switch (fnc) {
          case DAY:
            value = TimeUtils.padTwo(date.getDom());
            break;
          case DAY_OF_WEEK:
            value = BeeUtils.toString(date.getDow());
            break;
          case MONTH:
            value = TimeUtils.padTwo(date.getMonth());
            break;
          case YEAR:
            value = BeeUtils.toString(date.getYear());
            break;
          case YEAR_MONTH:
            value = BeeUtils.join(".", date.getYear(), TimeUtils.padTwo(date.getMonth()));
            break;
          default:
            Assert.unsupported();
            break;
        }
      } else {
        value = date.toString();
      }
    }
    return value;
  }

  @Override
  public String getOptionsCaption() {
    return "Formatas";
  }

  @Override
  public Editor getOptionsEditor() {
    ListBox editor = new ListBox();
    editor.addItem("");

    for (DateTimeFunction fnc : EnumSet.of(DateTimeFunction.YEAR, DateTimeFunction.YEAR_MONTH,
        DateTimeFunction.MONTH, DateTimeFunction.DAY, DateTimeFunction.DAY_OF_WEEK)) {
      editor.addItem(fnc.getCaption(), fnc.name());
    }
    editor.setValue(getOptions());
    return editor;
  }
}
