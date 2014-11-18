package com.butent.bee.client.validation;

import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasBounds;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public final class ValidationHelper {

  public static final CellValidateEvent.Handler DO_NOT_VALIDATE = new CellValidateEvent.Handler() {
    @Override
    public Boolean validateCell(CellValidateEvent event) {
      event.cancel();
      return true;
    }
  };

  public static List<String> getBounds(HasBounds obj) {
    if (obj == null) {
      return new ArrayList<>();
    } else {
      return getBounds(obj.getMinValue(), obj.getMaxValue());
    }
  }

  public static List<String> getBounds(String minValue, String maxValue) {
    List<String> result = new ArrayList<>();

    if (!BeeUtils.isEmpty(minValue)) {
      result.add(BeeUtils.joinWords(Localized.getConstants().minValue(), minValue));
    }
    if (!BeeUtils.isEmpty(maxValue)) {
      result.add(BeeUtils.joinWords(Localized.getConstants().maxValue(), maxValue));
    }

    return result;
  }

  public static void showError(NotificationListener notificationListener, String caption,
      List<String> messages) {
    Assert.notNull(notificationListener);

    if (BeeUtils.isEmpty(messages)) {
      notificationListener.notifySevere(caption);

    } else if (BeeUtils.isEmpty(caption) || messages.contains(caption)) {
      notificationListener.notifySevere(ArrayUtils.toArray(messages));

    } else {
      String[] arr = new String[messages.size() + 1];
      arr[0] = caption;

      for (int i = 0; i < messages.size(); i++) {
        arr[i + 1] = messages.get(i);
      }
      notificationListener.notifySevere(arr);
    }
  }

  public static Boolean validateCell(CellValidation validation, HasCellValidationHandlers source,
      ValidationOrigin origin) {
    Assert.notNull(validation);
    if (source == null) {
      return validateCell(validation);
    }

    CellValidateEvent event = new CellValidateEvent(validation, origin);
    event.setValidationPhase(ValidationPhase.PRE);
    Boolean ok = source.fireCellValidation(event);

    if (BeeUtils.isTrue(ok) && !event.isCanceled()) {
      event.setValidationPhase(ValidationPhase.DEF);
      ok = validateCell(event.getCellValidation());
    }

    if (BeeUtils.isTrue(ok) && !event.isCanceled()) {
      event.setValidationPhase(ValidationPhase.POST);
      ok = source.fireCellValidation(event);
    }

    return ok;
  }

  public static boolean validateRow(IsRow row, Evaluator validation,
      NotificationListener notificationListener) {
    Assert.notNull(row);
    if (validation == null) {
      return true;
    }

    validation.update(row);
    String message = validation.evaluate();

    if (BeeUtils.isEmpty(message)) {
      return true;
    } else {
      if (notificationListener != null) {
        notificationListener.notifySevere(message);
      }
      return false;
    }
  }

  private static boolean validateCell(CellValidation cv) {
    List<String> messages = new ArrayList<>();

    boolean checkForNull = !cv.isAdding() || !cv.hasDefaults();

    if (cv.shouldValidateEditorInput()) {
      messages.addAll(cv.getEditor().validate(checkForNull));
    }

    if (messages.isEmpty()) {
      if (!checkForNull && BeeUtils.isEmpty(cv.getNewValue())) {
        return true;
      }
      if (!cv.isAdding() && BeeUtils.equalsTrimRight(cv.getOldValue(), cv.getNewValue())) {
        return true;
      }
    }

    if (messages.isEmpty() && cv.shouldEditorValidateNewValue()) {
      messages.addAll(cv.getEditor().validate(cv.getNewValue(), checkForNull));
    }

    if (messages.isEmpty() && cv.getEvaluator() != null) {
      cv.getEvaluator().update(cv.getRow(), BeeConst.UNDEF, cv.getColIndex(), cv.getType(),
          cv.getOldValue(), cv.getNewValue());
      String msg = cv.getEvaluator().evaluate();

      if (!BeeUtils.isEmpty(msg)) {
        messages.add(msg);
      }
    }

    if (messages.isEmpty() && !cv.isNullable() && BeeUtils.isEmpty(cv.getNewValue())) {
      messages.add(Localized.getConstants().valueRequired());
    }

    if (messages.isEmpty()) {
      return true;
    } else {
      if (cv.getNotificationListener() != null) {
        showError(cv.getNotificationListener(), cv.getCaption(), messages);
      }
      return false;
    }
  }

  private ValidationHelper() {
  }
}
