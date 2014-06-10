package com.butent.bee.shared.rights;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;

public enum RightsState implements HasLocalizedCaption {
  VIEW(true) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.rightStateView();
    }
  },
  CREATE(true) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.rightStateCreate();
    }
  },
  EDIT(true) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.rightStateEdit();
    }
  },
  DELETE(true) {
    @Override
    public String getCaption(LocalizableConstants constants) {
      return constants.rightStateDelete();
    }
  };

  private final boolean checked;

  private RightsState(boolean checked) {
    this.checked = checked;
  }

  @Override
  public String getCaption() {
    return getCaption(Localized.getConstants());
  }

  public boolean isChecked() {
    return checked;
  }
}
