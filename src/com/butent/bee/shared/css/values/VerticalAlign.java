package com.butent.bee.shared.css.values;

import com.butent.bee.shared.css.HasCssName;

public enum VerticalAlign implements HasCssName {
  AUTO {
    @Override
    public String getCssName() {
      return "auto";
    }
  },
  USE_SCRIPT {
    @Override
    public String getCssName() {
      return "use-script";
    }
  },
  BASELINE {
    @Override
    public String getCssName() {
      return "baseline";
    }
  },
  SUB {
    @Override
    public String getCssName() {
      return "sub";
    }
  },
  SUPER {
    @Override
    public String getCssName() {
      return "super";
    }
  },
  TOP {
    @Override
    public String getCssName() {
      return "top";
    }
  },
  TEXT_TOP {
    @Override
    public String getCssName() {
      return "text-top";
    }
  },
  CENTRAL {
    @Override
    public String getCssName() {
      return "central";
    }
  },
  MIDDLE {
    @Override
    public String getCssName() {
      return "middle";
    }
  },
  BOTTOM {
    @Override
    public String getCssName() {
      return "bottom";
    }
  },
  TEXT_BOTTOM {
    @Override
    public String getCssName() {
      return "text-bottom";
    }
  };

  public static boolean isCenter(VerticalAlign verticalAlign) {
    return verticalAlign == MIDDLE || verticalAlign == CENTRAL;
  }
}
