package com.butent.bee.client.event;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.event.dom.client.DragDropEventBase;
import com.google.gwt.event.dom.client.DragEndEvent;
import com.google.gwt.event.dom.client.DragEndHandler;
import com.google.gwt.event.dom.client.DragEnterEvent;
import com.google.gwt.event.dom.client.DragEnterHandler;
import com.google.gwt.event.dom.client.DragEvent;
import com.google.gwt.event.dom.client.DragHandler;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DragStartHandler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.MotionEvent;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.State;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.List;

import elemental.js.html.JsFile;

import elemental.js.html.JsFileList;

import elemental.js.dom.JsDataTransferItem;

import elemental.js.dom.JsDataTransferItemList;

import elemental.js.util.JsIndexable;

import elemental.js.dom.JsClipboard;

public class DndHelper {

  public static final Predicate<Long> alwaysTarget = Predicates.alwaysTrue();
  
  private static final String TRANSFER_TYPE_FILES = "Files";
  private static final String TRANSFER_ITEM_KIND_FILE = "file";

  private static String dataType = null;

  private static Long dataId = null;
  private static Long relatedId = null;

  private static Object data = null;

  private static MotionEvent motionEvent = null;

  public static void fillContent(String contentType, Long contentId, Long relId, Object content) {
    setDataType(contentType);

    setDataId(contentId);
    setRelatedId(relId);

    setData(content);
  }

  public static Object getData() {
    return data;
  }

  public static Long getDataId() {
    return dataId;
  }

  public static List<Property> getDataTransferInfo(DragDropEventBase<?> event) {
    Assert.notNull(event);
    List<Property> result = Lists.newArrayList();

    DataTransfer dataTransfer = event.getDataTransfer();
    if (dataTransfer == null) {
      result.add(new Property("Data Transfer", "is null"));
      return result;
    }

    JsClipboard clipboard = dataTransfer.cast();

    PropertyUtils.addProperties(result,
        "Drop Effect", clipboard.getDropEffect(),
        "Effect Allowed", clipboard.getEffectAllowed());

    JsIndexable types = clipboard.getTypes();
    if (types != null) {
      int length = types.length();
      result.add(new Property("Types", BeeUtils.bracket(length)));

      for (int i = 0; i < length; i++) {
        PropertyUtils.addProperty(result, "type " + i, types.at(i));
      }
    }

    JsDataTransferItemList items = clipboard.getItems();
    if (items != null) {
      int length = items.getLength();
      result.add(new Property("Items", BeeUtils.bracket(length)));

      for (int i = 0; i < length; i++) {
        JsDataTransferItem item = items.item(i);
        if (item != null) {
          PropertyUtils.addProperties(result,
              "item kind " + i, item.getKind(),
              "item type " + i, item.getType());
        }
      }
    }
    
    JsFileList files = clipboard.getFiles();
    if (files != null) {
      int length = files.getLength();
      result.add(new Property("Files", BeeUtils.bracket(length)));

      for (int i = 0; i < length; i++) {
        JsFile file = files.item(i);
        if (file != null) {
          PropertyUtils.addProperty(result, "file " + i, file.getName());
        }
      }
    }

    return result;
  }

  public static String getDataType() {
    return dataType;
  }

  public static Long getRelatedId() {
    return relatedId;
  }

  public static boolean hasFiles(DragDropEventBase<?> event) {
    if (event == null) {
      return false;
    }

    DataTransfer dataTransfer = event.getDataTransfer();
    if (dataTransfer == null) {
      return false;
    }

    JsClipboard clipboard = dataTransfer.cast();

    JsIndexable types = clipboard.getTypes();
    if (types != null) {
      for (int i = 0; i < types.length(); i++) {
        Object type = types.at(i);
        
        if (type != null && BeeUtils.startsSame(type.toString(), TRANSFER_TYPE_FILES)) {
          return true;
        }
      }
    }

    JsDataTransferItemList items = clipboard.getItems();
    if (items != null) {
      for (int i = 0; i < items.getLength(); i++) {
        JsDataTransferItem item = items.item(i);

        if (item != null && BeeUtils.startsSame(item.getKind(), TRANSFER_ITEM_KIND_FILE)) {
          return true;
        }
      }
    }
    
    JsFileList files = clipboard.getFiles();
    return files != null && files.getLength() > 0;
  }
  
  public static boolean isDataType(String contentType) {
    return BeeUtils.same(contentType, getDataType());
  }

  public static void makeSource(final DndSource widget, final String contentType,
      final Long contentId, final Long relId, final Object content,
      final String dragStyle, final boolean fireMotion) {

    Assert.notNull(widget);
    Assert.notNull(contentType);

    DomUtils.setDraggable(widget.asWidget());

    widget.addDragStartHandler(new DragStartHandler() {
      @Override
      public void onDragStart(DragStartEvent event) {
        if (!BeeUtils.isEmpty(dragStyle)) {
          widget.asWidget().addStyleName(dragStyle);
        }

        EventUtils.allowMove(event);
        if (contentId != null) {
          EventUtils.setDndData(event, contentId);
        }

        fillContent(contentType, contentId, relId, content);

        if (fireMotion) {
          setMotionEvent(new MotionEvent(contentType, widget, event.getNativeEvent().getClientX(),
              event.getNativeEvent().getClientY()));
        }
      }
    });

    if (fireMotion) {
      widget.addDragHandler(new DragHandler() {
        @Override
        public void onDrag(DragEvent event) {
          if (getMotionEvent() != null) {
            int x = event.getNativeEvent().getClientX();
            int y = event.getNativeEvent().getClientY();

            if (x > 0 || y > 0) {
              getMotionEvent().moveTo(x, y);
              BeeKeeper.getBus().fireEvent(getMotionEvent());
            }
          }
        }
      });
    }

    widget.addDragEndHandler(new DragEndHandler() {
      @Override
      public void onDragEnd(DragEndEvent event) {
        if (!BeeUtils.isEmpty(dragStyle)) {
          widget.asWidget().removeStyleName(dragStyle);
        }
        reset();
      }
    });
  }

  public static void makeSource(DndSource widget, String contentType, Object content,
      String dragStyle) {
    makeSource(widget, contentType, null, null, content, dragStyle, false);
  }

  public static void makeTarget(final DndTarget widget, final Collection<String> contentTypes,
      final String overStyle, final Predicate<Object> targetPredicate,
      final Consumer<Object> onDrop) {

    Assert.notNull(widget);
    Assert.notEmpty(contentTypes);
    Assert.notNull(targetPredicate);
    Assert.notNull(onDrop);

    widget.addDragEnterHandler(new DragEnterHandler() {
      @Override
      public void onDragEnter(DragEnterEvent event) {
        if (isTarget(contentTypes, targetPredicate)) {
          if (widget.getTargetState() == null) {
            if (!BeeUtils.isEmpty(overStyle)) {
              widget.asWidget().addStyleName(overStyle);
            }
            widget.setTargetState(State.ACTIVATED);

          } else if (widget.getTargetState() == State.ACTIVATED) {
            widget.setTargetState(State.PENDING);
          }
        }
      }
    });

    widget.addDragOverHandler(new DragOverHandler() {
      @Override
      public void onDragOver(DragOverEvent event) {
        if (widget.getTargetState() != null) {
          EventUtils.selectDropMove(event);
        }
      }
    });

    widget.addDragLeaveHandler(new DragLeaveHandler() {
      @Override
      public void onDragLeave(DragLeaveEvent event) {
        if (widget.getTargetState() == State.ACTIVATED) {
          if (!BeeUtils.isEmpty(overStyle)) {
            widget.asWidget().removeStyleName(overStyle);
          }
          widget.setTargetState(null);

        } else if (widget.getTargetState() == State.PENDING) {
          widget.setTargetState(State.ACTIVATED);
        }
      }
    });

    widget.addDropHandler(new DropHandler() {
      @Override
      public void onDrop(DropEvent event) {
        if (widget.getTargetState() != null) {
          event.stopPropagation();
          onDrop.accept(getData());
        }
      }
    });
  }

  public static void reset() {
    fillContent(null, null, null, null);
    setMotionEvent(null);
  }

  private static MotionEvent getMotionEvent() {
    return motionEvent;
  }

  private static boolean isTarget(Collection<String> contentTypes,
      Predicate<Object> targetPredicate) {
    return contentTypes.contains(getDataType()) && targetPredicate.apply(getData());
  }

  private static void setData(Object data) {
    DndHelper.data = data;
  }

  private static void setDataId(Long dataId) {
    DndHelper.dataId = dataId;
  }

  private static void setDataType(String dataType) {
    DndHelper.dataType = dataType;
  }

  private static void setMotionEvent(MotionEvent motionEvent) {
    DndHelper.motionEvent = motionEvent;
  }

  private static void setRelatedId(Long relatedId) {
    DndHelper.relatedId = relatedId;
  }

  private DndHelper() {
  }
}
