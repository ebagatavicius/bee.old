package com.butent.bee.client.view.edit;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dom.Dimensions;
import com.butent.bee.client.images.star.Stars;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.EditorDescription;
import com.butent.bee.shared.ui.EditorType;
import com.butent.bee.shared.ui.HasStringValue;
import com.butent.bee.shared.utils.BeeUtils;

public final class EditorAssistant {

  public static void doEditorAction(Editor widget, String value, char charCode,
      EditorAction action) {
    Assert.notNull(widget);

    boolean acceptChar;
    if (charCode < BeeConst.CHAR_SPACE) {
      acceptChar = false;
    } else if (widget instanceof HasCharacterFilter) {
      acceptChar = ((HasCharacterFilter) widget).acceptChar(charCode);
    } else {
      acceptChar = true;
    }
    String charValue = acceptChar ? BeeUtils.toString(charCode) : BeeConst.STRING_EMPTY;

    String v;

    if (BeeUtils.isEmpty(value)) {
      v = charValue;
    } else if (!acceptChar) {
      v = value;
    } else if (action == null || action == EditorAction.REPLACE) {
      v = charValue;
    } else if (action == EditorAction.ADD_FIRST) {
      v = charValue + BeeUtils.trim(value);
    } else if (action == EditorAction.ADD_LAST) {
      v = BeeUtils.trimRight(value) + charValue;
    } else {
      v = value;
    }
    widget.setValue(BeeUtils.trimRight(v));

    if (widget instanceof TextBox && !BeeUtils.isEmpty(value) && action != null) {
      TextBox box = (TextBox) widget;
      int p = BeeConst.UNDEF;
      int len = box.getText().length();

      switch (action) {
        case ADD_FIRST:
          p = acceptChar ? 1 : 0;
          break;
        case ADD_LAST:
          p = len;
          break;
        case END:
          p = len;
          break;
        case HOME:
          p = 0;
          break;
        case REPLACE:
          p = len;
          break;
        case SELECT:
          box.selectAll();
          break;
      }
      if (p >= 0 && p <= len) {
        box.setCursorPos(p);
      }
    }
  }

  public static void editStarCell(Integer starCount, EditStartEvent event, final CellSource source,
      final Consumer<Integer> updater) {

    Assert.notNull(event);
    Assert.notNull(source);

    final IsRow row = event.getRowValue();
    if (row == null) {
      return;
    }

    final Integer oldValue = source.getInteger(row);

    final Element element = event.getSourceElement();

    if (event.isDelete()) {
      if (oldValue != null) {
        source.clear(row);
        if (element != null) {
          element.setInnerHTML(BeeConst.STRING_EMPTY);
        }

        if (updater != null) {
          updater.accept(null);
        }
      }

    } else if (event.getCharCode() == BeeConst.CHAR_PLUS
        || event.getCharCode() == BeeConst.CHAR_MINUS) {

      boolean forward = event.getCharCode() == BeeConst.CHAR_PLUS;
      int count = Stars.count();

      int newValue;
      if (oldValue == null) {
        newValue = forward ? 0 : count - 1;
      } else if (forward) {
        newValue = BeeUtils.rotateForwardExclusive(oldValue, 0, count);
      } else {
        newValue = BeeUtils.rotateBackwardExclusive(oldValue, 0, count);
      }

      source.set(row, newValue);
      if (element != null) {
        element.setInnerHTML(Stars.getHtml(newValue));
      }

      if (updater != null) {
        updater.accept(newValue);
      }

    } else {
      Global.getMsgBoxen().pickStar(starCount, oldValue, element, new ChoiceCallback() {
        @Override
        public void onCancel() {
          refocus();
        }

        @Override
        public void onSuccess(int value) {
          if (oldValue == null || (value - 1) != oldValue) {
            source.set(row, value == 0 ? null : value - 1);
            if (element != null) {
              if (value == 0) {
                element.setInnerHTML(BeeConst.STRING_EMPTY);
              } else {
                element.setInnerHTML(Stars.getHtml(value - 1));
              }
            }

            if (updater != null) {
              updater.accept(value == 0 ? null : value - 1);
            }
          }

          refocus();
        }

        private void refocus() {
          if (element != null) {
            element.focus();
          }
        }
      });
    }

    event.consume();
  }

  public static Dimensions getDefaultDimensions(EditorDescription editorDescription) {
    Assert.notNull(editorDescription);

    Dimensions dimensions = new Dimensions();

    EditorType editorType = editorDescription.getType();
    if (editorType != null) {
      if (BeeUtils.isPositive(editorType.getDefaultWidth())) {
        dimensions.setWidth(editorType.getDefaultWidth());
      }
      if (BeeUtils.isPositive(editorType.getDefaultHeight())) {
        dimensions.setHeight(editorType.getDefaultHeight());
      }
      if (BeeUtils.isPositive(editorType.getMinWidth())) {
        dimensions.setMinWidth(editorType.getMinWidth());
      }
      if (BeeUtils.isPositive(editorType.getMinHeight())) {
        dimensions.setMinHeight(editorType.getMinHeight());
      }
    }

    if (BeeUtils.isPositive(editorDescription.getWidth())) {
      dimensions.setWidth(editorDescription.getWidth());
    }
    if (BeeUtils.isPositive(editorDescription.getHeight())) {
      dimensions.setHeight(editorDescription.getHeight());
    }
    if (BeeUtils.isPositive(editorDescription.getMinWidth())) {
      dimensions.setMinWidth(editorDescription.getMinWidth());
    }
    if (BeeUtils.isPositive(editorDescription.getMinHeight())) {
      dimensions.setMinHeight(editorDescription.getMinHeight());
    }

    return dimensions;
  }

  public static String getValue(Widget widget) {
    if (widget instanceof Editor) {
      return ((Editor) widget).getValue();
    } else if (widget instanceof HasStringValue) {
      return ((HasStringValue) widget).getValue();
    } else {
      return null;
    }
  }

  private EditorAssistant() {
  }
}
