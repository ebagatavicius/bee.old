package com.butent.bee.client.modules.administration;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.InputPassword;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.UiConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Objects;

public final class PasswordService {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "ChangePassword-";

  private static final String STYLE_LABEL = STYLE_PREFIX + "label";
  private static final String STYLE_INPUT = STYLE_PREFIX + "input";

  public static void change() {
    final Long userId = BeeKeeper.getUser().getUserId();
    if (userId == null) {
      return;
    }

    final String viewName = AdministrationConstants.VIEW_USERS;
    final String colName = AdministrationConstants.COL_PASSWORD;

    Queries.getValue(viewName, userId, colName, new RpcCallback<String>() {
      @Override
      public void onSuccess(String result) {
        openDialog(result, new Consumer<String>() {
          @Override
          public void accept(String input) {
            Queries.update(viewName, userId, colName, new TextValue(input));
          }
        });
      }
    });
  }

  static void changePassword(final FormView userForm) {
    Assert.notNull(userForm);

    openDialog(Objects.equals(BeeKeeper.getUser().getUserId(), userForm.getActiveRowId())
        ? userForm.getStringValue(AdministrationConstants.COL_PASSWORD) : null,
        new Consumer<String>() {
          @Override
          public void accept(String input) {
            userForm.updateCell(AdministrationConstants.COL_PASSWORD, input);
          }
        });
  }

  private static boolean isEnter(KeyDownEvent event) {
    return event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
        && !EventUtils.hasModifierKey(event.getNativeEvent());
  }

  private static void openDialog(final String oldPass, final Consumer<String> callback) {

    HtmlTable table = new HtmlTable(STYLE_PREFIX + "table");
    int row = 0;

    final InputPassword inpOld;
    if (BeeUtils.isEmpty(oldPass)) {
      inpOld = null;
    } else {
      inpOld = new InputPassword(UiConstants.MAX_PASSWORD_LENGTH);
      table.setText(row, 0, Localized.getConstants().oldPassword(), STYLE_LABEL);
      table.setWidgetAndStyle(row, 1, inpOld, STYLE_INPUT);
      row++;
    }

    final InputPassword inpNew = new InputPassword(UiConstants.MAX_PASSWORD_LENGTH);
    table.setText(row, 0, Localized.getConstants().newPassword(), STYLE_LABEL);
    table.setWidgetAndStyle(row, 1, inpNew, STYLE_INPUT);
    row++;

    final InputPassword inpNew2 = new InputPassword(UiConstants.MAX_PASSWORD_LENGTH);
    table.setText(row, 0, Localized.getConstants().repeatNewPassword(), STYLE_LABEL);
    table.setWidgetAndStyle(row, 1, inpNew2, STYLE_INPUT);
    row++;

    if (inpOld != null) {
      inpOld.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (isEnter(event)) {
            inpNew.setFocus(true);
          }
        }
      });
    }

    inpNew.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (isEnter(event)) {
          inpNew2.setFocus(true);
        }
      }
    });

    inpNew2.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (isEnter(event)) {
          Popup popup = UiHelper.getParentPopup(inpNew2);
          if (popup != null && popup.getOnSave() != null) {
            popup.getOnSave().accept(null);
          }
        }
      }
    });

    Global.inputWidget(Localized.getConstants().changePassword(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        if (!BeeUtils.isEmpty(oldPass) && inpOld != null) {
          String old = BeeUtils.trim(inpOld.getValue());

          if (BeeUtils.isEmpty(old)) {
            inpOld.setFocus(true);
            return Localized.getConstants().oldPasswordIsRequired();

          } else if (!Objects.equals(Codec.encodePassword(old), oldPass)) {
            inpOld.setFocus(true);
            return Localized.getConstants().oldPasswordIsInvalid();
          }
        }

        String newPass = BeeUtils.trim(inpNew.getValue());

        if (BeeUtils.isEmpty(newPass)) {
          inpNew.setFocus(true);
          return Localized.getConstants().newPasswordIsRequired();

        } else if (!newPass.equals(BeeUtils.trim(inpNew2.getValue()))) {
          inpNew.setFocus(true);
          return Localized.getConstants().newPasswordsDoesNotMatch();
        }

        return super.getErrorMessage();
      }

      @Override
      public void onSuccess() {
        callback.accept(Codec.encodePassword(BeeUtils.trim(inpNew.getValue())));
      }
    }, STYLE_PREFIX + "dialog");
  }

  private PasswordService() {
  }
}
