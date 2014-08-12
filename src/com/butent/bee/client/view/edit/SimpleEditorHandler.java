package com.butent.bee.client.view.edit;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.validation.ValidationHelper;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public final class SimpleEditorHandler implements EditChangeHandler, EditStopEvent.Handler,
    HasCaption {

  public static void observe(String caption, Editor editor) {
    observe(caption, editor, null);
  }

  public static void observe(String caption, Editor editor, Widget focusParent) {
    observe(caption, editor, focusParent, null);
  }

  public static void observe(String caption, Editor editor, Widget focusParent,
      NotificationListener notificationListener) {
    Assert.notNull(editor);
    SimpleEditorHandler handler = new SimpleEditorHandler(caption, editor, focusParent,
        notificationListener);

    editor.addEditChangeHandler(handler);
    editor.addEditStopHandler(handler);

    editor.setHandlesTabulation(true);
  }

  private final String caption;
  private final Editor editor;
  private final Widget focusParent;
  private final NotificationListener notificationListener;

  private SimpleEditorHandler(String caption, Editor editor, Widget focusParent,
      NotificationListener notificationListener) {
    this.caption = caption;
    this.editor = editor;
    this.focusParent = focusParent;
    this.notificationListener = notificationListener;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public void onEditStop(EditStopEvent event) {
    if (event.isError()) {
      getNotificationListener().notifySevere(event.getMessage());

    } else if (event.isCanceled()) {
      normalize();

    } else {
      end(true);
    }
  }

  @Override
  public void onKeyDown(KeyDownEvent event) {
    int keyCode = event.getNativeKeyCode();
    if (editor.handlesKey(keyCode)) {
      return;
    }

    Boolean forward;

    switch (keyCode) {
      case KeyCodes.KEY_ENTER:
        forward = true;
        break;

      case KeyCodes.KEY_TAB:
        forward = !EventUtils.hasModifierKey(event.getNativeEvent());
        break;

      default:
        forward = null;
    }

    if (forward != null) {
      event.preventDefault();
      end(forward);
    }
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    end(true);
  }

  private void end(boolean forward) {
    if (validate()) {
      normalize();
      navigate(forward);
    } else {
      editor.clearValue();
    }
  }

  private NotificationListener getNotificationListener() {
    return BeeUtils.nvl(notificationListener, BeeKeeper.getScreen());
  }

  private void navigate(boolean forward) {
    UiHelper.moveFocus(BeeUtils.nvl(focusParent, editor.asWidget().getParent()), forward);
  }

  private void normalize() {
    editor.normalizeDisplay(editor.getNormalizedValue());
  }

  private boolean validate() {
    List<String> messages = editor.validate(true);

    if (BeeUtils.isEmpty(messages)) {
      return true;
    } else {
      ValidationHelper.showError(getNotificationListener(), getCaption(), messages);
      return false;
    }
  }
}
