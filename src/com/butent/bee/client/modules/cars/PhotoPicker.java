package com.butent.bee.client.modules.cars;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.utils.NewFileInfo;
import com.butent.bee.client.widget.InputFile;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.function.Consumer;

public class PhotoPicker implements ClickHandler, InputCallback {

  private final Consumer<NewFileInfo> photoConsumer;
  private final InputFile inputFile = new InputFile(false);

  public PhotoPicker(Consumer<NewFileInfo> photoConsumer) {
    this.photoConsumer = Assert.notNull(photoConsumer);

    inputFile.addChangeHandler(changeEvent -> {
      UiHelper.closeDialog(inputFile);
      onSuccess();
    });
  }

  @Override
  public String getErrorMessage() {
    if (inputFile.isEmpty()) {
      return Localized.dictionary().valueRequired();
    }
    return null;
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    inputFile.clear();
    Global.inputWidget(Localized.dictionary().photo(), inputFile, this, null, null,
        EnumSet.of(Action.DELETE));
  }

  @Override
  public void onDelete(DialogBox dialog) {
    dialog.close();
    photoConsumer.accept(null);
  }

  @Override
  public void onSuccess() {
    photoConsumer.accept(BeeUtils.peek(FileUtils.getNewFileInfos(inputFile.getFiles())));
  }
}
