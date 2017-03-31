package com.butent.bee.client.view.navigation;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;

import com.butent.bee.client.Global;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.event.logical.ScopeChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.NavigationOrigin;
import com.butent.bee.shared.utils.BeeUtils;

public class SimplePager extends AbstractPager {

  private final class GoCommand extends Command {
    private Navigation goTo;

    private GoCommand(Navigation goTo) {
      super();
      this.goTo = goTo;
    }

    @Override
    public void execute() {
      if (!isEnabled() || getDisplay() == null) {
        return;
      }

      switch (goTo) {
        case FIRST:
          setPageStart(0);
          break;
        case REWIND:
          rewind();
          break;
        case PREV:
          previousPage();
          break;
        case NEXT:
          nextPage();
          break;
        case FORWARD:
          forward();
          break;
        case LAST:
          setPageStart(getRowCount() - getPageSize());
          break;
      }
    }
  }

  /**
   * Contains possible navigation options like first, previous, next, last etc.
   */
  private enum Navigation {
    FIRST, REWIND, PREV, NEXT, FORWARD, LAST
  }

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "SimplePager-";
  private static final String STYLE_CONTAINER = STYLE_PREFIX + "container";

  private static final String STYLE_COMMAND = STYLE_PREFIX + "command";
  private static final String STYLE_DISABLED_COMMAND = STYLE_PREFIX + "command-disabled";
  private static final String STYLE_INFO = STYLE_PREFIX + "info";

  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getFormat("#,###");
  private static final String POSITION_SEPARATOR = " - ";
  private static final String ROW_COUNT_SEPARATOR = " / ";

  private static final int MIN_ROW_COUNT_FOR_FAST_NAVIGATION = 100;
  private static final int MIN_FAST_PAGES = 3;
  private static final int MAX_FAST_PAGES = 20;

  private final Image widgetFirst;
  private final Image widgetRewind;
  private final Image widgetPrev;
  private final Image widgetNext;
  private final Image widgetForward;
  private final Image widgetLast;

  private final Label widgetInfo;

  private final boolean showPageSize;

  private int maxRowCount;

  public SimplePager(int maxRowCount) {
    this(maxRowCount, true);
  }

  public SimplePager(int maxRowCount, boolean showPageSize) {
    this(maxRowCount, showPageSize, maxRowCount >= MIN_ROW_COUNT_FOR_FAST_NAVIGATION);
  }

  public SimplePager(int maxRowCount, boolean showPageSize, boolean showFastNavigation) {
    this.maxRowCount = maxRowCount;
    this.showPageSize = showPageSize;

    this.widgetFirst = createCommand(Global.getImages().first(), Navigation.FIRST);
    this.widgetPrev = createCommand(Global.getImages().previous(), Navigation.PREV);
    this.widgetNext = createCommand(Global.getImages().next(), Navigation.NEXT);
    this.widgetLast = createCommand(Global.getImages().last(), Navigation.LAST);

    if (showFastNavigation) {
      this.widgetRewind = createCommand(Global.getImages().rewind(), Navigation.REWIND);
      this.widgetForward = createCommand(Global.getImages().forward(), Navigation.FORWARD);
    } else {
      this.widgetRewind = null;
      this.widgetForward = null;
    }

    Flow container = new Flow(STYLE_CONTAINER);
    initWidget(container);

    container.add(widgetFirst);
    if (widgetRewind != null) {
      container.add(widgetRewind);
    }
    container.add(widgetPrev);

    this.widgetInfo = new Label();
    widgetInfo.addStyleName(STYLE_INFO);

    int width = getMaxInfoWidth(maxRowCount);
    if (width > 0) {
      StyleUtils.setMinWidth(widgetInfo, width);
    }

    container.add(widgetInfo);

    container.add(widgetNext);
    if (widgetForward != null) {
      container.add(widgetForward);
    }
    container.add(widgetLast);
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  public boolean goFirst() {
    if (widgetFirst.isEnabled()) {
      widgetFirst.getCommand().execute();
      return true;
    } else {
      return false;
    }
  }

  public boolean goPrevious() {
    if (widgetPrev.isEnabled()) {
      widgetPrev.getCommand().execute();
      return true;
    } else {
      return false;
    }
  }

  public boolean goNext() {
    if (widgetNext.isEnabled()) {
      widgetNext.getCommand().execute();
      return true;
    } else {
      return false;
    }
  }

  public boolean goLast() {
    if (widgetLast.isEnabled()) {
      widgetLast.getCommand().execute();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    return !DomUtils.isImageElement(source);
  }

  @Override
  public void onScopeChange(ScopeChangeEvent event) {
    if (event == null) {
      return;
    }

    int start = BeeUtils.toNonNegativeInt(event.getStart());
    int length = BeeUtils.toNonNegativeInt(event.getLength());
    int rowCount = BeeUtils.toNonNegativeInt(event.getTotal());

    if (start >= rowCount) {
      start = Math.max(rowCount - 1, 0);
    }
    if (start + length > rowCount) {
      length = Math.max(rowCount - start, 0);
    }

    if (rowCount > getMaxRowCount()) {
      setMaxRowCount(rowCount);
      StyleUtils.setWidth(widgetInfo, getMaxInfoWidth(rowCount));
    }

    widgetInfo.setHtml(createText(Math.min(start + 1, rowCount),
        Math.min(rowCount, start + length), rowCount));

    widgetFirst.setEnabled(start > 0);
    widgetPrev.setEnabled(start > 0);

    widgetNext.setEnabled(start + length < rowCount);
    widgetLast.setEnabled(start + length < rowCount);

    if (widgetRewind != null && widgetForward != null) {
      if (start > 0) {
        widgetRewind.setEnabled(true);
        widgetRewind.setTitle(format(getRewindPosition(start, length, rowCount) + 1));
      } else {
        widgetRewind.setEnabled(false);
        UiHelper.clearTitle(widgetRewind);
      }

      if (start + length < rowCount) {
        widgetForward.setEnabled(true);
        widgetForward.setTitle(format(getForwardPosition(start, length, rowCount) + 1));
      } else {
        widgetForward.setEnabled(false);
        UiHelper.clearTitle(widgetForward);
      }
    }
  }

  @Override
  protected NavigationOrigin getNavigationOrigin() {
    return NavigationOrigin.PAGER;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    ReadyEvent.fire(this);
  }

  private Image createCommand(ImageResource resource, Navigation navigation) {
    Image command = new Image(resource, new GoCommand(navigation), STYLE_DISABLED_COMMAND);
    command.addStyleName(STYLE_COMMAND);

    return command;
  }

  private String createText(int start, int end, int rowCount) {
    StringBuilder sb = new StringBuilder(format(start));
    if (showPageSize) {
      sb.append(POSITION_SEPARATOR).append(format(end));
    }
    sb.append(ROW_COUNT_SEPARATOR).append(format(rowCount));
    return sb.toString();
  }

  private static String format(int x) {
    if (NUMBER_FORMAT == null) {
      return BeeUtils.toString(x);
    } else {
      return NUMBER_FORMAT.format(x);
    }
  }

  private void forward() {
    if (getDisplay() == null) {
      return;
    }

    int start = getPageStart();
    int length = getPageSize();
    int rowCount = getRowCount();

    if (start < 0 || length <= 0 || rowCount <= length || start + length >= rowCount) {
      return;
    }
    setPageStart(getForwardPosition(start, length, rowCount));
  }

  private static int getFastStep(int pageSize, int rowCount) {
    if (pageSize <= 0 || MIN_FAST_PAGES <= 0 || MAX_FAST_PAGES <= 0
        || rowCount <= pageSize * MIN_FAST_PAGES) {
      return pageSize;
    }
    return BeeUtils.clamp((int) Math.sqrt(rowCount / pageSize), MIN_FAST_PAGES, MAX_FAST_PAGES)
        * pageSize;
  }

  private static int getForwardPosition(int pageStart, int pageSize, int rowCount) {
    int step = getFastStep(pageSize, rowCount);
    if (pageStart + step + pageSize >= rowCount) {
      return rowCount - pageSize;
    } else {
      int pos = pageStart + step;
      return pos - pos % pageSize;
    }
  }

  private int getMaxInfoWidth(int rowCount) {
    return Rulers.getLineWidth(null, createText(rowCount, rowCount, rowCount), false) + 1;
  }

  private int getMaxRowCount() {
    return maxRowCount;
  }

  private static int getRewindPosition(int pageStart, int pageSize, int rowCount) {
    int step = getFastStep(pageSize, rowCount);
    if (step >= pageStart + pageSize) {
      return 0;
    } else {
      int pos = pageStart - step;
      return pos - pos % pageSize;
    }
  }

  private void rewind() {
    if (getDisplay() == null) {
      return;
    }

    int start = getPageStart();
    int length = getPageSize();
    int rowCount = getRowCount();

    if (start <= 0 || length <= 0 || rowCount <= length) {
      return;
    }
    setPageStart(getRewindPosition(start, length, rowCount));
  }

  private void setMaxRowCount(int maxRowCount) {
    this.maxRowCount = maxRowCount;
  }
}
