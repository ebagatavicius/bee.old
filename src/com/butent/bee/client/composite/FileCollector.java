package com.butent.bee.client.composite;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.DndHelper;
import com.butent.bee.client.event.DndTarget;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.DateTimeLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FileCollector extends HtmlTable implements DragOverHandler, DropHandler,
    DragEnterHandler, DragLeaveHandler, HasSelectionHandlers<FileInfo>, HasOptions {

  public enum Column implements HasCaption {
    DATE("date", Localized.getConstants().date(), false, false) {
      @Override
      Widget createDisplay() {
        return new DateTimeLabel(false);
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        InputDateTime editor = new InputDateTime();
        editor.setDateTime(fileInfo.getFileDate());
        return editor;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
        if (widget instanceof DateTimeLabel) {
          ((DateTimeLabel) widget).setValue(fileInfo.getFileDate());
        }
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        boolean changed = false;
        if (widget instanceof InputDateTime) {
          DateTime dt = ((InputDateTime) widget).getDateTime();

          if (!TimeUtils.equals(dt, fileInfo.getFileDate())) {
            changed = true;
            fileInfo.setFileDate(dt);
          }
        }
        return changed;
      }
    },

    VERSION("version", Localized.getConstants().fileVersion(), false, false) {
      @Override
      Widget createDisplay() {
        return new Label();
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        InputText editor = new InputText();
        editor.setValue(fileInfo.getFileVersion());
        return editor;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
        if (widget instanceof Label) {
          ((Label) widget).setHtml(BeeUtils.trim(fileInfo.getFileVersion()));
        }
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        if (widget instanceof InputText) {
          String value = ((InputText) widget).getValue();
          if (!BeeUtils.equalsTrim(fileInfo.getFileVersion(), value)) {
            fileInfo.setFileVersion(Strings.emptyToNull(BeeUtils.trim(value)));
            return true;
          }
        }
        return false;
      }
    },

    NAME("name", Localized.getConstants().fileName(), true, false) {
      @Override
      Widget createDisplay() {
        return new Label();
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        InputText editor = new InputText();
        editor.setValue(BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName()));
        return editor;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
        if (widget instanceof Label) {
          ((Label) widget).setHtml(BeeUtils.notEmpty(fileInfo.getCaption(), fileInfo.getName()));
        }
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        boolean changed = false;
        if (widget instanceof InputText) {
          String value = BeeUtils.trim(((InputText) widget).getValue());

          if (BeeUtils.isEmpty(value) || BeeUtils.equalsTrim(value, fileInfo.getName())) {
            changed = !BeeUtils.isEmpty(fileInfo.getCaption());
            fileInfo.setCaption(null);
          } else if (!BeeUtils.equalsTrim(value, fileInfo.getCaption())) {
            changed = true;
            fileInfo.setCaption(value);
          }
        }
        return changed;
      }
    },

    DESCRIPTION("description", Localized.getConstants().fileDescription(), false, false) {
      @Override
      Widget createDisplay() {
        return new Label();
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        InputArea editor = new InputArea();
        editor.setValue(BeeUtils.trim(fileInfo.getDescription()));
        return editor;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
        if (widget instanceof Label) {
          ((Label) widget).setHtml(BeeUtils.trim(fileInfo.getDescription()));
        }
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        if (widget instanceof InputArea) {
          String value = ((InputArea) widget).getValue();
          if (!BeeUtils.equalsTrim(fileInfo.getDescription(), value)) {
            fileInfo.setDescription(Strings.emptyToNull(BeeUtils.trim(value)));
            return true;
          }
        }
        return false;
      }
    },

    SIZE("size", Localized.getConstants().fileSize(), false, true) {
      @Override
      Widget createDisplay() {
        return new Label();
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        return null;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
        if (widget instanceof Label) {
          ((Label) widget).setHtml(FileUtils.sizeToText(fileInfo.getSize()));
        }
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        return false;
      }
    },

    TYPE("type", Localized.getConstants().fileType(), false, false) {
      @Override
      Widget createDisplay() {
        return new Label();
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        InputText editor = new InputText();
        editor.setValue(fileInfo.getType());
        return editor;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
        if (widget instanceof Label) {
          ((Label) widget).setHtml(BeeUtils.trim(fileInfo.getType()));
        }
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        if (widget instanceof InputText) {
          String value = ((InputText) widget).getValue();
          if (!BeeUtils.equalsTrim(fileInfo.getType(), value)) {
            fileInfo.setType(Strings.emptyToNull(BeeUtils.trim(value)));
            return true;
          }
        }
        return false;
      }
    },

    EDIT("edit", Localized.getConstants().actionEdit(), false, true) {
      @Override
      Widget createDisplay() {
        Image widget = new Image(Global.getImages().silverEdit());
        widget.setTitle(getCaption().toLowerCase());
        return widget;
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        return null;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        return false;
      }
    },

    DELETE("delete", Localized.getConstants().actionRemove(), false, true) {
      @Override
      Widget createDisplay() {
        Image widget = new Image(Global.getImages().silverMinus());
        widget.setTitle(getCaption().toLowerCase());
        return widget;
      }

      @Override
      Widget createEditor(FileInfo fileInfo) {
        return null;
      }

      @Override
      void refresh(Widget widget, FileInfo fileInfo) {
      }

      @Override
      boolean update(Widget widget, FileInfo fileInfo) {
        return false;
      }
    };

    private final String label;
    private final String caption;

    private final boolean alwaysVisible;
    private final boolean readOnly;

    private Column(String label, String caption, boolean alwaysVisible, boolean readOnly) {
      this.label = label;
      this.caption = caption;
      this.alwaysVisible = alwaysVisible;
      this.readOnly = readOnly;
    }

    @Override
    public String getCaption() {
      return caption;
    }

    abstract Widget createDisplay();

    abstract Widget createEditor(FileInfo fileInfo);

    abstract void refresh(Widget widget, FileInfo fileInfo);

    abstract boolean update(Widget widget, FileInfo fileInfo);

    private String getLabel() {
      return label;
    }

    private boolean isAlwaysVisible() {
      return alwaysVisible;
    }

    private boolean isReadOnly() {
      return readOnly;
    }
  }

  public static final List<Column> ALL_COLUMNS = Lists.newArrayList(Column.values());

  private static final Collection<FileInfo> FILE_STACK = new ArrayList<>();

  private static final String STYLE_PREFIX = "bee-FileCollector-";

  private static final String STYLE_FACE = "face";

  private static final String STYLE_CELL = "Cell";

  private static final String STYLE_COLUMN = "Column";

  private static final String STYLE_ROW = "Row";

  private static final String STYLE_EDITING = "editing";
  private static final String STYLE_EDITOR = "editor-";
  private static final String STYLE_CAPTION = "caption";

  private static final String STYLE_INPUT = "input";
  private static final int FACE_COLUMN = 0;
  private static final int INPUT_COLUMN = 1;
  private static final List<Column> DEFAULT_VISIBLE_COLUMNS = Lists.newArrayList(Column.NAME,
      Column.SIZE, Column.EDIT, Column.DELETE);

  private static final List<Column> DEFAULT_EDITABLE_COLUMNS = Lists.newArrayList(Column.NAME);

  public static IdentifiableWidget getDefaultFace() {
    return new Button(Localized.getConstants().chooseFiles());
  }

  public static FileCollector headless(Consumer<Collection<? extends FileInfo>> fileConsumer) {
    Assert.notNull(fileConsumer);
    return new FileCollector(fileConsumer);
  }

  public static List<Column> parseColumns(String input) {
    List<Column> columns = Lists.newArrayList();
    if (BeeUtils.isEmpty(input) || BeeUtils.same(input, BeeConst.NONE)) {
      return columns;
    }

    if (BeeUtils.same(input, BeeConst.ALL)) {
      Collections.addAll(columns, Column.values());
      return columns;
    }

    List<String> labels = NameUtils.toList(input);
    for (String label : labels) {
      for (Column column : Column.values()) {
        if (BeeUtils.inListSame(label, column.getLabel(), column.getCaption())) {
          if (!columns.contains(column)) {
            columns.add(column);
          }
        }
      }
    }
    return columns;
  }

  private final InputFile inputFile;

  private final List<FileInfo> files = Lists.newArrayList();

  private final Consumer<Collection<? extends FileInfo>> fileConsumer;

  private Element dropArea;
  private int dndCounter;

  private String options;

  private final List<Column> columns = Lists.newArrayList();
  private final List<Column> editable = Lists.newArrayList();

  public FileCollector(IdentifiableWidget face) {
    this(face, DEFAULT_VISIBLE_COLUMNS);
  }

  public FileCollector(IdentifiableWidget face, List<Column> visibleColumns) {
    this(face, visibleColumns, DEFAULT_EDITABLE_COLUMNS);
  }

  public FileCollector(IdentifiableWidget face, List<Column> visibleColumns,
      List<Column> editableColumns) {
    Assert.notNull(face);

    this.inputFile = createInput();
    this.fileConsumer = null;

    Binder.addClickHandler(face.asWidget(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        FileCollector.this.clickInput();
      }
    });

    face.asWidget().addStyleName(STYLE_PREFIX + STYLE_FACE);

    setWidget(0, FACE_COLUMN, face.asWidget(), STYLE_PREFIX + STYLE_FACE + STYLE_CELL);
    setWidget(0, INPUT_COLUMN, inputFile, STYLE_PREFIX + "hiddenCell");

    addStyleName(STYLE_PREFIX + "panel");

    getRowFormatter().addStyleName(0, STYLE_PREFIX + STYLE_FACE + STYLE_ROW);

    initColumns(visibleColumns, editableColumns);

    addFiles(popFiles());
  }

  private FileCollector(Consumer<Collection<? extends FileInfo>> fileConsumer) {
    this.inputFile = createInput();
    this.fileConsumer = fileConsumer;

    setWidget(0, 0, inputFile);
    addStyleName(STYLE_PREFIX + "headless");
  }

  public void addFiles(Collection<? extends FileInfo> fileInfos) {
    if (fileInfos != null) {
      if (headless()) {
        fileConsumer.accept(fileInfos);
      } else {
        for (FileInfo fileInfo : fileInfos) {
          addFile(fileInfo);
        }
      }
    }
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<FileInfo> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  public void bindDnd(DndTarget target) {
    target.addDragEnterHandler(this);
    target.addDragLeaveHandler(this);
    target.addDragOverHandler(this);
    target.addDropHandler(this);

    setDropArea(target.getElement());
  }

  @Override
  public void clear() {
    getFiles().clear();
    setDndCounter(0);

    int rowCount = getRowCount() - 1;
    for (int r = 0; r < rowCount; r++) {
      removeRow(0);
    }
  }

  public void clickInput() {
    inputFile.click();
  }

  public List<FileInfo> getFiles() {
    return files;
  }

  @Override
  public String getIdPrefix() {
    return "collector";
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public boolean isEmpty() {
    return getFiles().isEmpty();
  }

  @Override
  public void onDragEnter(DragEnterEvent event) {
    setDndCounter(getDndCounter() + 1);
    if (getDndCounter() <= 1 && DndHelper.hasFiles(event)) {
      showDropArea();
    }
  }

  @Override
  public void onDragLeave(DragLeaveEvent event) {
    setDndCounter(getDndCounter() - 1);
    if (getDndCounter() <= 0) {
      setDndCounter(0);
      hideDropArea();
    }
  }

  @Override
  public void onDragOver(DragOverEvent event) {
    EventUtils.setDropEffect(event, EventUtils.EFFECT_COPY);
  }

  @Override
  public void onDrop(DropEvent event) {
    event.stopPropagation();
    event.preventDefault();

    setDndCounter(0);
    hideDropArea();

    addFiles(FileUtils.getNewFileInfos(FileUtils.getFiles(event.getNativeEvent())));
  }

  public static void pushFiles(Collection<FileInfo> files) {
    FILE_STACK.clear();

    if (!BeeUtils.isEmpty(files)) {
      FILE_STACK.addAll(files);
    }
  }

  public static Collection<FileInfo> popFiles() {
    Collection<FileInfo> files = new ArrayList<>(FILE_STACK);
    FILE_STACK.clear();
    return files;
  }

  public void setAccept(String accept) {
    inputFile.setAccept(accept);
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  private void addFile(final FileInfo info) {
    if (info == null || contains(info)) {
      return;
    }

    getFiles().add(info);

    int row = getRowCount() - 1;
    insertRow(row);
    getRowFormatter().addStyleName(row, STYLE_PREFIX + STYLE_ROW);

    for (int col = 0; col < columns.size(); col++) {
      Column column = columns.get(col);

      Widget widget = column.createDisplay();
      widget.addStyleName(STYLE_PREFIX + column.getLabel());

      switch (column) {
        case EDIT:
          ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              int index = getIndex(info.getName());
              if (index >= 0 && !editable.isEmpty()) {
                edit(index);
              }
            }
          });
          break;

        case DELETE:
          ((HasClickHandlers) widget).addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              int index = getIndex(info.getName());
              if (index >= 0) {
                getFiles().remove(index);
                removeRow(index);
                SelectionEvent.fire(FileCollector.this, info);
              }
            }
          });
          break;

        default:
          column.refresh(widget, info);
      }

      setWidget(row, col, widget, STYLE_PREFIX + column.getLabel() + STYLE_CELL);
      if (row == 0) {
        getColumnFormatter().addStyleName(col, STYLE_PREFIX + column.getLabel() + STYLE_COLUMN);
      }
    }
    SelectionEvent.fire(this, info);
  }

  private boolean contains(FileInfo info) {
    return getIndex(info.getName()) >= 0;
  }

  private InputFile createInput() {
    final InputFile input = new InputFile(true);

    input.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        if (!input.isEmpty()) {
          addFiles(FileUtils.getNewFileInfos(input.getFiles()));
          input.clear();
        }
      }
    });

    input.getElement().getStyle().setVisibility(Visibility.HIDDEN);
    input.addStyleName(STYLE_PREFIX + "hiddenWidget");

    return input;
  }

  private void edit(final int index) {
    getRowFormatter().addStyleName(index, STYLE_PREFIX + STYLE_EDITING);

    final FileInfo fi = getFiles().get(index);

    String pfx = STYLE_PREFIX + STYLE_EDITOR;

    final HtmlTable panel = new HtmlTable();
    panel.addStyleName(pfx + "panel");

    int colCaption = 0;
    final int colInput = 1;

    int row = 0;

    for (Column column : editable) {
      Label captionWidget = new Label(column.getCaption());
      captionWidget.addStyleName(pfx + STYLE_CAPTION);
      panel.setWidget(row, colCaption, captionWidget, pfx + STYLE_CAPTION + STYLE_CELL);

      Widget editor = column.createEditor(fi);
      editor.addStyleName(pfx + column.getLabel());
      panel.setWidget(row, colInput, editor, pfx + column.getLabel() + STYLE_CELL);

      panel.getRowFormatter().addStyleName(row, pfx + column.getLabel() + STYLE_ROW);
      if (row == 0) {
        panel.getColumnFormatter().addStyleName(colCaption, pfx + STYLE_CAPTION + STYLE_COLUMN);
        panel.getColumnFormatter().addStyleName(colInput, pfx + STYLE_INPUT + STYLE_COLUMN);
      }
      row++;
    }

    Global.inputWidget(Localized.getConstants().fileDataCorrection(), panel, new InputCallback() {
      @Override
      public void onCancel() {
        getRowFormatter().removeStyleName(index, STYLE_PREFIX + STYLE_EDITING);
      }

      @Override
      public void onSuccess() {
        getRowFormatter().removeStyleName(index, STYLE_PREFIX + STYLE_EDITING);

        Set<Column> changedColumns = Sets.newHashSet();
        for (int i = 0; i < editable.size(); i++) {
          Column column = editable.get(i);
          if (column.update(panel.getWidget(i, colInput), fi)) {
            changedColumns.add(column);
          }
        }

        if (!changedColumns.isEmpty()) {
          refresh(index, changedColumns);
        }
      }
    }, null, getCellFormatter().getElement(index, columns.indexOf(Column.EDIT)));
  }

  private int getDndCounter() {
    return dndCounter;
  }

  private Element getDropArea() {
    return dropArea;
  }

  private int getIndex(String fileName) {
    for (int i = 0; i < getFiles().size(); i++) {
      if (BeeUtils.same(getFiles().get(i).getName(), fileName)) {
        return i;
      }
    }
    return BeeConst.UNDEF;
  }

  private boolean headless() {
    return fileConsumer != null;
  }

  private void hideDropArea() {
    if (StyleUtils.hasClassName(getDropArea(), StyleUtils.DROP_AREA)) {
      getDropArea().removeClassName(StyleUtils.DROP_AREA);
    }
  }

  private void initColumns(List<Column> visibleColumns, List<Column> editableColumns) {
    if (!BeeUtils.isEmpty(editableColumns)) {
      for (Column column : editableColumns) {
        if (!column.isReadOnly() && !editable.contains(column)) {
          editable.add(column);
        }
      }
    }

    List<Column> vc = Lists.newArrayList();
    if (BeeUtils.isEmpty(visibleColumns)) {
      vc.addAll(DEFAULT_VISIBLE_COLUMNS);
    } else {
      vc.addAll(visibleColumns);
    }

    for (Column column : vc) {
      if (Column.EDIT.equals(column) && editable.isEmpty()) {
        continue;
      }
      if (!columns.contains(column)) {
        columns.add(column);
      }
    }

    for (Column column : Column.values()) {
      if (column.isAlwaysVisible() && !columns.contains(column)) {
        columns.add(column);
      }
    }

    for (Column column : editable) {
      if (!columns.contains(column)) {
        columns.add(column);
      }
    }

    if (!editable.isEmpty() && !columns.contains(Column.EDIT)) {
      columns.add(Column.EDIT);
    }
  }

  private void refresh(int row, Collection<Column> changedColumns) {
    FileInfo info = getFiles().get(row);

    for (int col = 0; col < columns.size(); col++) {
      Column column = columns.get(col);
      if (changedColumns.contains(column)) {
        column.refresh(getWidget(row, col), info);
      }
    }
  }

  private void setDndCounter(int dndCounter) {
    this.dndCounter = dndCounter;
  }

  private void setDropArea(Element dropArea) {
    this.dropArea = dropArea;
  }

  private void showDropArea() {
    if (!StyleUtils.hasClassName(getDropArea(), StyleUtils.DROP_AREA)) {
      getDropArea().addClassName(StyleUtils.DROP_AREA);
    }
  }
}