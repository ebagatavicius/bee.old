package com.butent.bee.client.layout;

import com.google.common.collect.Lists;
import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;

import com.butent.bee.client.dom.Rulers;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.ui.Orientation;

import java.util.List;

public class Layout {

  public enum Alignment {
    BEGIN, END, STRETCH;
  }

  public interface AnimationCallback {

    void onAnimationComplete();

    void onLayout(Layer layer, double progress);
  }

  class Layer {
    final Element container;
    final Element child;

    Object userObject;

    boolean setLeft;
    boolean setRight;
    boolean setTop;
    boolean setBottom;
    boolean setWidth;
    boolean setHeight;

    boolean setTargetLeft = true;
    boolean setTargetRight = true;
    boolean setTargetTop = true;
    boolean setTargetBottom = true;
    boolean setTargetWidth;
    boolean setTargetHeight;

    CssUnit leftUnit;
    CssUnit topUnit;
    CssUnit rightUnit;
    CssUnit bottomUnit;
    CssUnit widthUnit;
    CssUnit heightUnit;

    CssUnit targetLeftUnit = CssUnit.PX;
    CssUnit targetTopUnit = CssUnit.PX;
    CssUnit targetRightUnit = CssUnit.PX;
    CssUnit targetBottomUnit = CssUnit.PX;
    CssUnit targetWidthUnit;
    CssUnit targetHeightUnit;

    double left;
    double top;
    double right;
    double bottom;
    double width;
    double height;

    double sourceLeft;
    double sourceTop;
    double sourceRight;
    double sourceBottom;
    double sourceWidth;
    double sourceHeight;

    double targetLeft;
    double targetTop;
    double targetRight;
    double targetBottom;
    double targetWidth;
    double targetHeight;

    Alignment hPos = Alignment.STRETCH;
    Alignment vPos = Alignment.STRETCH;

    boolean visible = true;

    Layer(Element container, Element child, Object userObject) {
      this.container = container;
      this.child = child;
      this.userObject = userObject;
    }

    Element getContainerElement() {
      return container;
    }

    Object getUserObject() {
      return this.userObject;
    }

    void setBottomHeight(double bv, CssUnit bu, double hv, CssUnit hu) {
      this.setTargetBottom = true;
      this.setTargetHeight = true;
      this.setTargetTop = false;

      this.targetBottom = bv;
      this.targetHeight = hv;
      this.targetBottomUnit = bu;
      this.targetHeightUnit = hu;
    }

    void setChildHorizontalPosition(Alignment position) {
      this.hPos = position;
    }

    void setChildVerticalPosition(Alignment position) {
      this.vPos = position;
    }

    void setLeftRight(double lv, CssUnit lu, double rv, CssUnit ru) {
      this.setTargetLeft = true;
      this.setTargetRight = true;
      this.setTargetWidth = false;

      this.targetLeft = lv;
      this.targetRight = rv;
      this.targetLeftUnit = lu;
      this.targetRightUnit = ru;
    }

    void setLeftWidth(double lv, CssUnit lu, double wv, CssUnit wu) {
      this.setTargetLeft = true;
      this.setTargetWidth = true;
      this.setTargetRight = false;

      this.targetLeft = lv;
      this.targetWidth = wv;
      this.targetLeftUnit = lu;
      this.targetWidthUnit = wu;
    }

    void setRightWidth(double rv, CssUnit ru, double wv, CssUnit wu) {
      this.setTargetRight = true;
      this.setTargetWidth = true;
      this.setTargetLeft = false;

      this.targetRight = rv;
      this.targetWidth = wv;
      this.targetRightUnit = ru;
      this.targetWidthUnit = wu;
    }

    void setTopBottom(double tv, CssUnit tu, double bv, CssUnit bu) {
      this.setTargetTop = true;
      this.setTargetBottom = true;
      this.setTargetHeight = false;

      this.targetTop = tv;
      this.targetBottom = bv;
      this.targetTopUnit = tu;
      this.targetBottomUnit = bu;
    }

    void setTopHeight(double tv, CssUnit tu, double hv, CssUnit hu) {
      this.setTargetTop = true;
      this.setTargetHeight = true;
      this.setTargetBottom = false;

      this.targetTop = tv;
      this.targetHeight = hv;
      this.targetTopUnit = tu;
      this.targetHeightUnit = hu;
    }

    void setVisible(boolean visible) {
      this.visible = visible;
    }
  }

  private final LayoutImpl impl = new LayoutImpl();

  private final List<Layer> layers = Lists.newArrayList();
  private final Element parentElem;

  private Animation animation;

  Layout(Element parent) {
    this.parentElem = parent;
    impl.initParent(parent);
  }

  Layer attachChild(Element child) {
    return attachChild(child, null);
  }

  Layer attachChild(Element child, Element before) {
    return attachChild(child, before, null);
  }

  Layer attachChild(Element child, Element before, Object userObject) {
    Element container = impl.attachChild(parentElem, child, before);
    Layer layer = new Layer(container, child, userObject);
    layers.add(layer);
    return layer;
  }

  Layer attachChild(Element child, Object userObject) {
    return attachChild(child, null, userObject);
  }

  void fillParent() {
    impl.fillParent(parentElem);
  }

  void layout() {
    layout(0);
  }

  void layout(int duration) {
    layout(duration, null);
  }

  void layout(int duration, final AnimationCallback callback) {
    if (animation != null) {
      animation.cancel();
    }

    if (duration == 0) {
      for (Layer layer : layers) {
        layer.left = layer.targetLeft;
        layer.sourceLeft = layer.targetLeft;

        layer.top = layer.targetTop;
        layer.sourceTop = layer.targetTop;

        layer.right = layer.targetRight;
        layer.sourceRight = layer.targetRight;

        layer.bottom = layer.targetBottom;
        layer.sourceBottom = layer.targetBottom;

        layer.width = layer.targetWidth;
        layer.sourceWidth = layer.targetWidth;

        layer.height = layer.targetHeight;
        layer.sourceHeight = layer.targetHeight;

        layer.setLeft = layer.setTargetLeft;
        layer.setTop = layer.setTargetTop;
        layer.setRight = layer.setTargetRight;
        layer.setBottom = layer.setTargetBottom;
        layer.setWidth = layer.setTargetWidth;
        layer.setHeight = layer.setTargetHeight;

        layer.leftUnit = layer.targetLeftUnit;
        layer.topUnit = layer.targetTopUnit;
        layer.rightUnit = layer.targetRightUnit;
        layer.bottomUnit = layer.targetBottomUnit;
        layer.widthUnit = layer.targetWidthUnit;
        layer.heightUnit = layer.targetHeightUnit;

        impl.layout(layer);
      }

      if (callback != null) {
        callback.onAnimationComplete();
      }
      return;
    }

    int parentWidth = parentElem.getClientWidth();
    int parentHeight = parentElem.getClientHeight();
    for (Layer l : layers) {
      adjustHorizontalConstraints(parentWidth, l);
      adjustVerticalConstraints(parentHeight, l);
    }

    animation = new Animation() {
      @Override
      protected void onCancel() {
        onComplete();
      }

      @Override
      protected void onComplete() {
        animation = null;
        layout();
        if (callback != null) {
          callback.onAnimationComplete();
        }
      }

      @Override
      protected void onUpdate(double progress) {
        for (Layer layer : layers) {
          if (layer.setTargetLeft) {
            layer.left = layer.sourceLeft + (layer.targetLeft - layer.sourceLeft) * progress;
          }
          if (layer.setTargetRight) {
            layer.right = layer.sourceRight + (layer.targetRight - layer.sourceRight) * progress;
          }

          if (layer.setTargetTop) {
            layer.top = layer.sourceTop + (layer.targetTop - layer.sourceTop) * progress;
          }
          if (layer.setTargetBottom) {
            layer.bottom = layer.sourceBottom
                + (layer.targetBottom - layer.sourceBottom) * progress;
          }

          if (layer.setTargetWidth) {
            layer.width = layer.sourceWidth + (layer.targetWidth - layer.sourceWidth) * progress;
          }
          if (layer.setTargetHeight) {
            layer.height = layer.sourceHeight
                + (layer.targetHeight - layer.sourceHeight) * progress;
          }

          impl.layout(layer);
          if (callback != null) {
            callback.onLayout(layer, progress);
          }
        }
      }
    };

    animation.run(duration, parentElem);
  }

  void removeChild(Layer layer) {
    impl.removeChild(layer.container, layer.child);
    layers.remove(layer);
  }

  private void adjustHorizontalConstraints(int parentWidth, Layer layer) {
    Orientation orientation = Orientation.HORIZONTAL;

    double leftPx = layer.left * getUnitSize(layer.leftUnit, orientation);
    double rightPx = layer.right * getUnitSize(layer.rightUnit, orientation);
    double widthPx = layer.width * getUnitSize(layer.widthUnit, orientation);

    if (layer.setLeft && !layer.setTargetLeft) {
      layer.setLeft = false;

      if (!layer.setWidth) {
        layer.setTargetWidth = true;
        layer.sourceWidth = (parentWidth - (leftPx + rightPx))
            / getUnitSize(layer.targetWidthUnit, orientation);
      } else {
        layer.setTargetRight = true;
        layer.sourceRight = (parentWidth - (leftPx + widthPx))
            / getUnitSize(layer.targetRightUnit, orientation);
      }

    } else if (layer.setWidth && !layer.setTargetWidth) {
      layer.setWidth = false;

      if (!layer.setLeft) {
        layer.setTargetLeft = true;
        layer.sourceLeft = (parentWidth - (rightPx + widthPx))
            / getUnitSize(layer.targetLeftUnit, orientation);
      } else {
        layer.setTargetRight = true;
        layer.sourceRight = (parentWidth - (leftPx + widthPx))
            / getUnitSize(layer.targetRightUnit, orientation);
      }

    } else if (layer.setRight && !layer.setTargetRight) {
      layer.setRight = false;

      if (!layer.setWidth) {
        layer.setTargetWidth = true;
        layer.sourceWidth = (parentWidth - (leftPx + rightPx))
            / getUnitSize(layer.targetWidthUnit, orientation);
      } else {
        layer.setTargetLeft = true;
        layer.sourceLeft = (parentWidth - (rightPx + widthPx))
            / getUnitSize(layer.targetLeftUnit, orientation);
      }
    }

    layer.setLeft = layer.setTargetLeft;
    layer.setRight = layer.setTargetRight;
    layer.setWidth = layer.setTargetWidth;

    layer.leftUnit = layer.targetLeftUnit;
    layer.rightUnit = layer.targetRightUnit;
    layer.widthUnit = layer.targetWidthUnit;
  }

  private void adjustVerticalConstraints(int parentHeight, Layer layer) {
    Orientation orientation = Orientation.VERTICAL;

    double topPx = layer.top * getUnitSize(layer.topUnit, orientation);
    double bottomPx = layer.bottom * getUnitSize(layer.bottomUnit, orientation);
    double heightPx = layer.height * getUnitSize(layer.heightUnit, orientation);

    if (layer.setTop && !layer.setTargetTop) {
      layer.setTop = false;

      if (!layer.setHeight) {
        layer.setTargetHeight = true;
        layer.sourceHeight = (parentHeight - (topPx + bottomPx))
            / getUnitSize(layer.targetHeightUnit, orientation);
      } else {
        layer.setTargetBottom = true;
        layer.sourceBottom = (parentHeight - (topPx + heightPx))
            / getUnitSize(layer.targetBottomUnit, orientation);
      }

    } else if (layer.setHeight && !layer.setTargetHeight) {
      layer.setHeight = false;

      if (!layer.setTop) {
        layer.setTargetTop = true;
        layer.sourceTop = (parentHeight - (bottomPx + heightPx))
            / getUnitSize(layer.targetTopUnit, orientation);
      } else {
        layer.setTargetBottom = true;
        layer.sourceBottom = (parentHeight - (topPx + heightPx))
            / getUnitSize(layer.targetBottomUnit, orientation);
      }

    } else if (layer.setBottom && !layer.setTargetBottom) {
      layer.setBottom = false;

      if (!layer.setHeight) {
        layer.setTargetHeight = true;
        layer.sourceHeight = (parentHeight - (topPx + bottomPx))
            / getUnitSize(layer.targetHeightUnit, orientation);
      } else {
        layer.setTargetTop = true;
        layer.sourceTop = (parentHeight - (bottomPx + heightPx))
            / getUnitSize(layer.targetTopUnit, orientation);
      }
    }

    layer.setTop = layer.setTargetTop;
    layer.setBottom = layer.setTargetBottom;
    layer.setHeight = layer.setTargetHeight;

    layer.topUnit = layer.targetTopUnit;
    layer.bottomUnit = layer.targetBottomUnit;
    layer.heightUnit = layer.targetHeightUnit;
  }

  private double getUnitSize(CssUnit unit, Orientation orientation) {
    if (unit == null) {
      return 1;
    } else {
      return Rulers.getUnitSize(parentElem, unit, orientation);
    }
  }
}
