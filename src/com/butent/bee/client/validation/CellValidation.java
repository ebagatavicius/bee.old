package com.butent.bee.client.validation;

import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;

public class CellValidation {

  private final String oldValue;
  private final String newValue;

  private final Editor editor;
  private final EditorValidation editorValidation;

  private final Evaluator evaluator;

  private final IsRow row;
  private final BeeColumn column;
  private final int colIndex;

  private final ValueType type;
  private final boolean nullable;

  private final String caption;
  private final NotificationListener notificationListener;

  public CellValidation(String oldValue, String newValue, Editor editor,
      EditorValidation editorValidation, Evaluator evaluator, IsRow row, BeeColumn column,
      int colIndex, ValueType type, boolean nullable, String caption,
      NotificationListener notificationListener) {

    super();
    this.oldValue = oldValue;
    this.newValue = newValue;

    this.editor = editor;
    this.editorValidation = editorValidation;

    this.evaluator = evaluator;

    this.row = row;
    this.column = column;
    this.colIndex = colIndex;

    this.type = type;
    this.nullable = nullable;

    this.caption = caption;
    this.notificationListener = notificationListener;
  }

  public String getCaption() {
    return caption;
  }

  public int getColIndex() {
    return colIndex;
  }

  public BeeColumn getColumn() {
    return column;
  }

  public Editor getEditor() {
    return editor;
  }

  public EditorValidation getEditorValidation() {
    return editorValidation;
  }

  public Evaluator getEvaluator() {
    return evaluator;
  }

  public String getNewValue() {
    return newValue;
  }

  public NotificationListener getNotificationListener() {
    return notificationListener;
  }

  public String getOldValue() {
    return oldValue;
  }

  public IsRow getRow() {
    return row;
  }

  public ValueType getType() {
    return type;
  }

  public boolean hasDefaults() {
    return getColumn() != null && getColumn().hasDefaults();
  }

  public boolean isAdding() {
    return DataUtils.isNewRow(getRow());
  }

  public boolean isEditing() {
    return DataUtils.hasId(getRow());
  }

  public boolean isNullable() {
    return nullable;
  }

  public boolean shouldEditorValidateNewValue() {
    return getEditor() != null && getEditorValidation() != null
        && getEditorValidation().validateNewValue();
  }

  public boolean shouldValidateEditorInput() {
    return getEditor() != null && getEditorValidation() != null
        && getEditorValidation().validateInput();
  }
}
