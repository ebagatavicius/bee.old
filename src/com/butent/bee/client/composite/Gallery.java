package com.butent.bee.client.composite;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.IdCallback;
import com.butent.bee.client.data.ParentRowCreator;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.event.logical.HasSummaryChangeHandlers;
import com.butent.bee.client.event.logical.ParentRowEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent;
import com.butent.bee.client.event.logical.SummaryChangeEvent.Handler;
import com.butent.bee.client.images.Images;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.ui.HasFosterParent;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HandlesActions;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class Gallery extends Flow implements HasViewName, HasFosterParent, ParentRowEvent.Handler,
    HasSummaryChangeHandlers, HandlesActions {

  private final class Picture extends Flow {

    private final RowInfo rowInfo;
    private Integer ordinal;

    private Picture(RowInfo rowInfo, Integer ordinal, String url) {
      super(STYLE_PICTURE);

      this.rowInfo = rowInfo;
      this.ordinal = ordinal;

      if (rowInfo.isEditable()) {
        add(createControls());
      }
      add(createImage(url));
    }

    private Widget createControls() {
      Flow controls = new Flow(STYLE_PICTURE_CONTROLS);

      if (ordinal != null) {
        FaLabel left = new FaLabel(FontAwesome.CHEVRON_LEFT, STYLE_PICTURE_CONTROL);
        left.addStyleName(STYLE_MOVE_LEFT);

        left.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            move(getRowId(), Direction.WEST);
          }
        });

        controls.add(left);

        FaLabel right = new FaLabel(FontAwesome.CHEVRON_RIGHT, STYLE_PICTURE_CONTROL);
        right.addStyleName(STYLE_MOVE_RIGHT);

        right.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            move(getRowId(), Direction.EAST);
          }
        });

        controls.add(right);
      }

      if (rowInfo.isRemovable()) {
        FaLabel delete = new FaLabel(Action.DELETE.getIcon(), STYLE_PICTURE_CONTROL);
        delete.addStyleName(STYLE_DELETE);

        delete.setTitle(Action.DELETE.getCaption());

        delete.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            delete(Picture.this.rowInfo);
          }
        });

        controls.add(delete);
      }

      return controls;
    }

    private Widget createImage(String url) {
      Image image = new Image();
      image.addStyleName(STYLE_IMAGE);

      image.addLoadHandler(new LoadHandler() {
        @Override
        public void onLoad(LoadEvent event) {
          if (event.getSource() instanceof Image) {
            Image source = (Image) event.getSource();

            int width = source.getNaturalWidth();
            int height = source.getNaturalHeight();

            if (width > 0 && height > 0) {
              source.setTitle(BeeUtils.joinWords(width, BeeConst.CHAR_TIMES, height));
            }
          }
        }
      });

      image.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (event.getSource() instanceof Image) {
            Image source = (Image) event.getSource();
            Image copy = new Image(source.getUrl());

            Global.showModalWidget(source.getTitle(), copy);
          }
        }
      });

      image.setUrl(url);

      return image;
    }

    private Integer getOrdinal() {
      return ordinal;
    }

    private long getRowId() {
      return rowInfo.getId();
    }

    private long getVersion() {
      return rowInfo.getVersion();
    }

    private void setOrdinal(Integer ordinal) {
      this.ordinal = ordinal;
    }

    private void setVersion(long version) {
      rowInfo.setVersion(version);
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(Gallery.class);

  private static final String ATTR_PICTURE_COLUMN = "pictureColumn";
  private static final String ATTR_ORDER_COLUMN = "orderColumn";
  private static final String ATTR_PARENT_REL_COLUMN = "parentRelColumn";
  private static final String ATTR_CACHE = "cache";

  private static final String STYLE_NAME = BeeConst.CSS_CLASS_PREFIX + "Gallery";
  private static final String STYLE_PREFIX = STYLE_NAME + "-";

  private static final String STYLE_READ_ONLY = STYLE_PREFIX + "read-only";

  private static final String STYLE_COMMAND_PANEL = STYLE_PREFIX + "command-panel";
  private static final String STYLE_COMMAND = STYLE_PREFIX + "command";

  private static final String STYLE_PICTURE_PANEL = STYLE_PREFIX + "picture-panel";
  private static final String STYLE_PICTURE = STYLE_PREFIX + "picture";
  private static final String STYLE_IMAGE = STYLE_PREFIX + "image";

  private static final String STYLE_PICTURE_CONTROLS = STYLE_PICTURE + "-controls";
  private static final String STYLE_PICTURE_CONTROL = STYLE_PICTURE + "-control";

  private static final String STYLE_MOVE_LEFT = STYLE_PICTURE + "-move-left";
  private static final String STYLE_MOVE_RIGHT = STYLE_PICTURE + "-move-right";
  private static final String STYLE_DELETE = STYLE_PICTURE + "-delete";

  public static Gallery create(Map<String, String> attributes) {
    Assert.notNull(attributes);

    String viewName = getAttribute(attributes, UiConstants.ATTR_VIEW_NAME, true);
    String relColumn = getAttribute(attributes, UiConstants.ATTR_REL_COLUMN, true);
    String pictureColumn = getAttribute(attributes, ATTR_PICTURE_COLUMN, true);

    if (Data.isViewVisible(viewName) && BeeUtils.allNotEmpty(relColumn, pictureColumn)) {
      String orderColumn = getAttribute(attributes, ATTR_ORDER_COLUMN, false);
      String parentRelColumn = getAttribute(attributes, ATTR_PARENT_REL_COLUMN, false);

      boolean readOnly = BeeConst.isTrue(attributes.get(UiConstants.ATTR_READ_ONLY));

      CachingPolicy cachingPolicy = BeeConst.isTrue(attributes.get(ATTR_CACHE))
          ? CachingPolicy.FULL : CachingPolicy.NONE;

      return new Gallery(viewName, relColumn, pictureColumn, orderColumn, parentRelColumn,
          readOnly, cachingPolicy);

    } else {
      return null;
    }
  }

  private static String getAttribute(Map<String, String> attributes, String name,
      boolean required) {

    String value = attributes.get(name);
    if (required && BeeUtils.isEmpty(value)) {
      logger.severe(NameUtils.getClassName(Gallery.class), "attribute", name, "not found");
    }

    return value;
  }

  private final String viewName;

  private final String relColumn;
  private final String pictureColumn;

  private final String orderColumn;
  private final Order order;

  private final String parentRelColumn;
  private int parentIndex = BeeConst.UNDEF;

  private Long relId;

  private String parentId;
  private HandlerRegistration parentRowReg;

  private final boolean readOnly;
  private final CachingPolicy cachingPolicy;

  private boolean summarize;

  private final Flow picturePanel;

  private final FileCollector collector;

  public Gallery(String viewName, String relColumn, String pictureColumn, String orderColumn,
      String parentRelColumn, boolean readOnly, CachingPolicy cachingPolicy) {

    super(STYLE_NAME);

    this.viewName = viewName;

    this.relColumn = relColumn;
    this.pictureColumn = pictureColumn;

    this.orderColumn = orderColumn;
    this.order = BeeUtils.isEmpty(orderColumn) ? null : Order.ascending(orderColumn);

    this.parentRelColumn = parentRelColumn;

    this.readOnly = readOnly || !Data.isViewEditable(viewName);
    if (this.readOnly) {
      addStyleName(STYLE_READ_ONLY);
    }

    this.cachingPolicy = cachingPolicy;

    List<Action> actions = new ArrayList<>();
    actions.add(Action.REFRESH);

    if (!this.readOnly && BeeKeeper.getUser().canCreateData(viewName)) {
      actions.add(Action.ADD);
    }

    add(createCommandPanel(actions));

    this.picturePanel = new Flow(STYLE_PICTURE_PANEL);
    add(picturePanel);

    this.collector = createCollector();
    add(collector);
  }

  @Override
  public com.google.gwt.event.shared.HandlerRegistration addSummaryChangeHandler(Handler handler) {
    return addHandler(handler, SummaryChangeEvent.getType());
  }

  @Override
  public String getIdPrefix() {
    return "gallery";
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public Value getSummary() {
    return new IntegerValue(picturePanel.getWidgetCount());
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case ADD:
        collector.clickInput();
        break;

      case REFRESH:
        invalidateCache();
        refresh();
        break;

      default:
        logger.warning(NameUtils.getName(this), action, "not implemented");
    }
  }

  @Override
  public void onParentRow(ParentRowEvent event) {
    ensureParentIndex(event.getViewName());

    Long parentValue = getParentValue(event.getRow());
    setRelId(parentValue);

    refresh();
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
    if (isAttached()) {
      register();
    }
  }

  @Override
  public void setSummarize(boolean summarize) {
    this.summarize = summarize;
  }

  @Override
  public boolean summarize() {
    return summarize;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    register();
  }

  @Override
  protected void onUnload() {
    unregister();
    super.onUnload();
  }

  private void clearPictures() {
    if (!picturePanel.isEmpty()) {
      picturePanel.clear();
      SummaryChangeEvent.maybeFire(this);
    }
  }

  private FileCollector createCollector() {
    FileCollector fc = FileCollector.headless(new Consumer<Collection<? extends FileInfo>>() {
      @Override
      public void accept(Collection<? extends FileInfo> input) {
        NotificationListener notificationListener = ViewHelper.getForm(Gallery.this);
        if (notificationListener == null) {
          notificationListener = BeeKeeper.getScreen();
        }

        Collection<? extends FileInfo> files = Images.sanitizeInput(input, notificationListener);
        if (!files.isEmpty()) {
          read(files, new Callback<List<String>>() {

            @Override
            public void onSuccess(final List<String> pictures) {
              ensureRelId(new IdCallback() {

                @Override
                public void onSuccess(Long relValue) {
                  upload(relValue, pictures);
                }
              });
            }
          });
        }
      }
    });

    fc.setAccept(Keywords.ACCEPT_IMAGE);
    fc.bindDnd(this);

    return fc;
  }

  private Widget createCommandPanel(Collection<Action> actions) {
    Flow panel = new Flow(STYLE_COMMAND_PANEL);

    for (Action action : actions) {
      FaLabel widget = new FaLabel(action.getIcon(), STYLE_COMMAND);
      initCommand(action, widget);

      panel.add(widget);
    }

    return panel;
  }

  private void delete(final RowInfo rowInfo) {
    Global.confirmDelete(Data.getViewCaption(viewName), Icon.WARNING,
        Collections.singletonList(Localized.dictionary().deletePictureQuestion()),
        new ConfirmationCallback() {

          @Override
          public void onConfirm() {
            Queries.deleteRow(viewName, rowInfo.getId(), rowInfo.getVersion(),
                new Queries.IntCallback() {

                  @Override
                  public void onSuccess(Integer result) {
                    RowDeleteEvent.fire(BeeKeeper.getBus(), viewName, rowInfo.getId());

                    int index = getPictureIndex(rowInfo.getId());
                    if (!BeeConst.isUndef(index)) {
                      picturePanel.remove(index);
                    }
                  }
                });
          }
        });
  }

  private void ensureParentIndex(String parentViewName) {
    if (BeeConst.isUndef(getParentIndex())) {
      if (BeeUtils.isEmpty(parentRelColumn)) {
        setParentIndex(DataUtils.ID_INDEX);
      } else {
        setParentIndex(Data.getColumnIndex(parentViewName, parentRelColumn));
      }
    }
  }

  private void ensureRelId(final IdCallback callback) {
    if (DataUtils.isId(relId)) {
      callback.onSuccess(relId);

    } else {
      FormView form = ViewHelper.getForm(this);

      if (form != null && form.getViewPresenter() instanceof ParentRowCreator) {
        ((ParentRowCreator) form.getViewPresenter()).createParentRow(form,
            new Callback<IsRow>() {
              @Override
              public void onSuccess(IsRow result) {
                Long parentValue = getParentValue(result);

                if (DataUtils.isId(parentValue)) {
                  setRelId(parentValue);
                  callback.onSuccess(parentValue);

                } else {
                  callback.onFailure(getViewName(), "parent row not created");
                }
              }
            });

      } else {
        callback.onFailure(getViewName(), "parent row creator not available");
      }
    }
  }

  private Filter getFilter() {
    return Filter.equals(relColumn, relId);
  }

  private int getMaxOrdinal() {
    int max = -1;

    if (!BeeUtils.isEmpty(orderColumn) && !picturePanel.isEmpty()) {
      for (Widget widget : picturePanel) {
        if (widget instanceof Picture) {
          Integer ordinal = ((Picture) widget).getOrdinal();
          if (ordinal != null) {
            max = Math.max(max, ordinal);
          }
        }
      }
    }
    return max;
  }

  private int getParentIndex() {
    return parentIndex;
  }

  private HandlerRegistration getParentRowReg() {
    return parentRowReg;
  }

  private Long getParentValue(IsRow row) {
    if (row == null) {
      return null;
    } else if (getParentIndex() >= 0) {
      return row.getLong(getParentIndex());
    } else {
      return row.getId();
    }
  }

  private Picture getPicture(int index) {
    if (picturePanel.isWidgetIndex(index) && picturePanel.getWidget(index) instanceof Picture) {
      return (Picture) picturePanel.getWidget(index);
    } else {
      return null;
    }
  }

  private int getPictureIndex(long rowId) {
    for (int i = 0; i < picturePanel.getWidgetCount(); i++) {
      if (picturePanel.getWidget(i) instanceof Picture
          && ((Picture) picturePanel.getWidget(i)).getRowId() == rowId) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private void initCommand(final Action action, FaLabel widget) {
    widget.addStyleName(action.getStyleName());
    UiHelper.initActionWidget(action, widget);

    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        handleAction(action);
      }
    });
  }

  private void invalidateCache() {
    if (cachingPolicy != null && cachingPolicy != CachingPolicy.NONE && DataUtils.isId(relId)) {
      Global.getCache().invalidateQuery(viewName, getFilter(), order);
    }
  }

  private void move(long rowId, Direction direction) {
    final int srcIndex = getPictureIndex(rowId);
    final int dstIndex = (direction == Direction.WEST) ? srcIndex - 1 : srcIndex + 1;

    final Picture srcPicture = getPicture(srcIndex);
    final Picture dstPicture = getPicture(dstIndex);

    if (srcPicture != null && dstPicture != null
        && srcPicture.getOrdinal() != null && dstPicture.getOrdinal() != null
        && !Objects.equals(srcPicture.getOrdinal(), dstPicture.getOrdinal())) {

      BeeRowSet rowSet = new BeeRowSet(viewName,
          Collections.singletonList(Data.getColumn(viewName, orderColumn)));

      String[] data = new String[1];

      data[0] = BeeUtils.toString(dstPicture.getOrdinal());
      rowSet.addRow(srcPicture.getRowId(), srcPicture.getVersion(), data);

      data[0] = BeeUtils.toString(srcPicture.getOrdinal());
      rowSet.addRow(dstPicture.getRowId(), dstPicture.getVersion(), data);

      Queries.updateRows(rowSet, new RpcCallback<RowInfoList>() {
        @Override
        public void onSuccess(RowInfoList result) {
          if (result.size() == 2) {
            Integer srcOrdinal = srcPicture.getOrdinal();

            srcPicture.setVersion(result.get(0).getVersion());
            srcPicture.setOrdinal(dstPicture.getOrdinal());

            dstPicture.setVersion(result.get(1).getVersion());
            dstPicture.setOrdinal(srcOrdinal);

            if (srcIndex < dstIndex) {
              picturePanel.insert(dstPicture, srcIndex);
            } else {
              picturePanel.insert(srcPicture, dstIndex);
            }
          }

          invalidateCache();
        }
      });
    }
  }

  private static void read(Collection<? extends FileInfo> files,
      final Callback<List<String>> callback) {

    final Latch latch = new Latch(files.size());
    final List<String> pictures = new ArrayList<>();

    for (FileInfo fileInfo : files) {
      if (fileInfo instanceof NewFileInfo) {
        FileUtils.readAsDataURL(((NewFileInfo) fileInfo).getNewFile(), new Consumer<String>() {
          @Override
          public void accept(String input) {
            pictures.add(input);
            latch.decrement();

            if (latch.isOpen()) {
              callback.onSuccess(pictures);
            }
          }
        });
      }
    }
  }

  private void register() {
    unregister();
    if (!BeeUtils.isEmpty(getParentId())) {
      setParentRowReg(BeeKeeper.getBus().registerParentRowHandler(getParentId(), this, false));
    }
  }

  private void refresh() {
    clearPictures();

    if (DataUtils.isId(relId)) {
      Queries.getRowSet(viewName, null, getFilter(), order, cachingPolicy,
          new Queries.RowSetCallback() {
            @Override
            public void onSuccess(BeeRowSet result) {
              if (!result.isEmpty()) {
                render(result);
                SummaryChangeEvent.maybeFire(Gallery.this);
              }
            }
          });
    }
  }

  private void render(BeeRowSet data) {
    int ordinalIndex =
        BeeUtils.isEmpty(orderColumn) ? BeeConst.UNDEF : data.getColumnIndex(orderColumn);
    int pictureIndex = data.getColumnIndex(pictureColumn);

    boolean removable = !readOnly && BeeKeeper.getUser().canDeleteData(viewName);

    for (BeeRow row : data) {
      RowInfo rowInfo = new RowInfo(row, row.isEditable() && !readOnly);
      rowInfo.setRemovable(removable && row.isRemovable());

      Integer ordinal =
          BeeConst.isUndef(ordinalIndex) ? null : BeeUtils.unbox(row.getInteger(ordinalIndex));

      Picture picture = new Picture(rowInfo, ordinal, row.getString(pictureIndex));
      picturePanel.add(picture);
    }
  }

  private void setParentIndex(int parentIndex) {
    this.parentIndex = parentIndex;
  }

  private void setParentRowReg(HandlerRegistration parentRowReg) {
    this.parentRowReg = parentRowReg;
  }

  private void setRelId(Long relId) {
    this.relId = relId;
  }

  private void unregister() {
    if (getParentRowReg() != null) {
      getParentRowReg().removeHandler();
      setParentRowReg(null);
    }
  }

  private void upload(Long relValue, Collection<String> pictures) {
    List<String> colNames = StringList.of(relColumn, pictureColumn, orderColumn);
    List<BeeColumn> columns = Data.getColumns(viewName, colNames);

    final BeeRowSet rowSet = new BeeRowSet(viewName, columns);

    int relIndex = rowSet.getColumnIndex(relColumn);
    int pictureIndex = rowSet.getColumnIndex(pictureColumn);

    int ordinalIndex;
    int maxOrdinal;

    if (BeeUtils.isEmpty(orderColumn)) {
      ordinalIndex = BeeConst.UNDEF;
      maxOrdinal = BeeConst.UNDEF;
    } else {
      ordinalIndex = rowSet.getColumnIndex(orderColumn);
      maxOrdinal = getMaxOrdinal();
    }

    for (String picture : pictures) {
      IsRow row = rowSet.addEmptyRow();

      row.setValue(relIndex, relValue);
      row.setValue(pictureIndex, picture);

      if (!BeeConst.isUndef(ordinalIndex)) {
        row.setValue(ordinalIndex, ++maxOrdinal);
      }
    }

    Queries.insertRows(rowSet, new RpcCallback<RowInfoList>() {
      @Override
      public void onSuccess(RowInfoList result) {
        if (result.size() == rowSet.getNumberOfRows()) {
          for (int i = 0; i < result.size(); i++) {
            RowInfo rowInfo = result.get(i);
            BeeRow row = rowSet.getRow(i);

            row.setId(rowInfo.getId());
            row.setVersion(rowInfo.getVersion());

            row.setEditable(rowInfo.isEditable());
            row.setRemovable(rowInfo.isRemovable());
          }

          render(rowSet);
          SummaryChangeEvent.maybeFire(Gallery.this);
        }

        invalidateCache();
      }
    });
  }
}
