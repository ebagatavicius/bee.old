package com.butent.bee.client.layout;

import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.style.Font;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.ui.Flexibility;
import com.butent.bee.shared.ui.Flexible;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FlexLayout {

  private static class Adapter {
    private final int index;

    private final int baseSize;
    private final int hypotheticalSize;

    private int factor = BeeConst.UNDEF;

    private int unclampedSize;
    private int clampedSize;

    public Adapter(int index, int baseSize, int hypotheticalSize) {
      super();
      this.index = index;
      this.baseSize = baseSize;
      this.hypotheticalSize = hypotheticalSize;
    }

    private int getBaseSize() {
      return baseSize;
    }

    private int getClampedSize() {
      return clampedSize;
    }

    private int getFactor() {
      return factor;
    }

    private int getHypotheticalSize() {
      return hypotheticalSize;
    }

    private int getIndex() {
      return index;
    }

    private int getUnclampedSize() {
      return unclampedSize;
    }

    private int getViolation() {
      return getClampedSize() - getUnclampedSize();
    }

    private boolean isMaxViolation() {
      return getClampedSize() < getUnclampedSize();
    }

    private boolean isMinViolation() {
      return getClampedSize() > getUnclampedSize();
    }

    private void setClampedSize(int clampedSize) {
      this.clampedSize = clampedSize;
    }

    private void setFactor(int factor) {
      this.factor = factor;
    }

    private void setUnclampedSize(int unclampedSize) {
      this.unclampedSize = unclampedSize;
    }
  }

  public static boolean doLayout(int containerSize, Font font, Orientation orientation,
      List<? extends Flexible> items, Flexibility defaultFlexibility) {
    Set<Integer> frozen = Collections.emptySet();
    return doLayout(containerSize, font, orientation, items, defaultFlexibility, frozen);
  }

  private static boolean doLayout(int containerSize, Font font, Orientation orientation,
      List<? extends Flexible> items, Flexibility defaultFlexibility, Set<Integer> frozen) {

    boolean changed = false;
    if (containerSize <= 0 || items.isEmpty()) {
      return changed;
    }

    List<Adapter> adapters = new ArrayList<>();
    int totHypothetical = 0;
    int totUsed = 0;

    for (int i = 0; i < items.size(); i++) {
      Flexible item = items.get(i);
      int hypotheticalSize = BeeConst.UNDEF;

      if (item.isFlexible() && !frozen.contains(i)) {
        Flexibility flexibility = item.getFlexibility();
        int baseSize = BeeConst.UNDEF;

        if (flexibility != null) {
          if (flexibility.getBasisWidth() > 0) {
            baseSize = getDefiniteSize(flexibility, containerSize, font);
          } else if (flexibility.isBasisAuto()) {
            hypotheticalSize = item.getHypotheticalSize(orientation, true);
          }
        }

        if (BeeConst.isUndef(baseSize) && BeeConst.isUndef(hypotheticalSize)
            && defaultFlexibility != null && defaultFlexibility.getBasisWidth() > 0) {
          baseSize = getDefiniteSize(defaultFlexibility, containerSize, font);
        }

        if (BeeConst.isUndef(baseSize) && BeeConst.isUndef(hypotheticalSize)) {
          hypotheticalSize = item.getHypotheticalSize(orientation, true);
        }

        if (BeeConst.isUndef(baseSize)) {
          baseSize = hypotheticalSize;
        }
        if (BeeConst.isUndef(hypotheticalSize)) {
          hypotheticalSize = item.clampSize(orientation, baseSize);
        }

        adapters.add(new Adapter(i, baseSize, hypotheticalSize));

      } else {
        hypotheticalSize = item.getHypotheticalSize(orientation, false);
        if (hypotheticalSize > 0) {
          totUsed += hypotheticalSize;
        }
      }

      if (hypotheticalSize > 0) {
        totHypothetical += hypotheticalSize;
      }
    }

    if (adapters.isEmpty()) {
      return changed;
    }

    if (totHypothetical == containerSize) {
      for (Adapter adapter : adapters) {
        int size = adapter.getHypotheticalSize();
        if (size > 0 && items.get(adapter.getIndex()).updateSize(orientation, size)) {
          changed = true;
        }
      }
      return changed;
    }

    boolean isGrowing = totHypothetical < containerSize;
    int countFactors = 0;
    int totFactors = 0;

    int totBase = 0;
    int totScaled = 0;

    for (Adapter adapter : adapters) {
      Flexible item = items.get(adapter.getIndex());
      int factor = BeeConst.UNDEF;

      Flexibility flexibility = item.getFlexibility();
      if (flexibility != null) {
        factor = isGrowing ? flexibility.getGrow() : flexibility.getShrink();
      }

      if (factor < 0 && defaultFlexibility != null) {
        factor = isGrowing ? defaultFlexibility.getGrow() : defaultFlexibility.getShrink();
      }

      adapter.setFactor(factor);

      if (factor > 0 && adapter.getBaseSize() > 0) {
        countFactors++;
        totFactors += factor;
        totBase += adapter.getBaseSize();
        totScaled += factor * adapter.getBaseSize();

      } else {
        int size = adapter.getHypotheticalSize();
        if (size > 0) {
          if (item.updateSize(orientation, size)) {
            changed = true;
          }
          totUsed += size;
        }

        adapter.setFactor(BeeConst.UNDEF);
      }
    }

    if (totFactors == 0) {
      return changed;
    }

    int freeSpace = containerSize - totUsed - totBase;

    boolean ok = isGrowing && freeSpace > 0 || !isGrowing && freeSpace < 0;
    if (!ok) {
      for (Adapter adapter : adapters) {
        if (adapter.getFactor() > 0 && adapter.getHypotheticalSize() > 0) {
          int size = adapter.getHypotheticalSize();
          if (items.get(adapter.getIndex()).updateSize(orientation, size)) {
            changed = true;
          }
        }
      }
      return changed;
    }

    int count = 0;
    int distributed = 0;
    int totViolation = 0;

    for (Adapter adapter : adapters) {
      int factor = adapter.getFactor();
      if (factor <= 0) {
        continue;
      }

      count++;

      int distr;
      if (count == countFactors) {
        distr = freeSpace - distributed;

      } else if (isGrowing) {
        distr = Math.min(freeSpace * factor / totFactors, freeSpace - distributed);

      } else {
        int decr = Math.abs(freeSpace) * factor * adapter.getBaseSize() / totScaled;
        distr = Math.max(-decr, freeSpace - distributed);
      }

      distributed += distr;

      int unclamped = adapter.getBaseSize() + distr;

      adapter.setUnclampedSize(unclamped);
      adapter.setClampedSize(items.get(adapter.getIndex()).clampSize(orientation, unclamped));

      totViolation += adapter.getViolation();
    }

    if (count == 1 || totViolation == 0) {
      for (Adapter adapter : adapters) {
        if (adapter.getFactor() > 0 && adapter.getClampedSize() > 0) {
          int size = adapter.getClampedSize();
          if (items.get(adapter.getIndex()).updateSize(orientation, size)) {
            changed = true;
          }
        }
      }
      return changed;
    }

    Set<Integer> freeze = new HashSet<>();
    if (!BeeUtils.isEmpty(frozen)) {
      freeze.addAll(frozen);
    }

    for (Adapter adapter : adapters) {
      if (adapter.getFactor() > 0 && adapter.getClampedSize() > 0) {
        if (totViolation > 0 && adapter.isMinViolation()
            || totViolation < 0 && adapter.isMaxViolation()) {
          freeze.add(adapter.getIndex());

          int size = adapter.getClampedSize();
          if (items.get(adapter.getIndex()).updateSize(orientation, size)) {
            changed = true;
          }
        }
      }
    }

    if (!freeze.isEmpty()) {
      changed |= doLayout(containerSize, font, orientation, items, defaultFlexibility, freeze);
    }
    return changed;
  }

  private static int getDefiniteSize(Flexibility flexibility, int containerSize, Font font) {
    if (flexibility.getBasisWidth() < 0) {
      return BeeConst.UNDEF;
    } else if (flexibility.getBasisUnit() == null) {
      return flexibility.getBasisWidth();
    } else if (CssUnit.PCT.equals(flexibility.getBasisUnit()) && containerSize <= 0) {
      return BeeConst.UNDEF;
    } else {
      return Rulers.getIntPixels(flexibility.getBasisWidth(), flexibility.getBasisUnit(), font,
          containerSize);
    }
  }

  private FlexLayout() {
  }
}
