package com.butent.bee.client.validation;

import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class CellValidateEvent {

  public interface Handler {
    Boolean validateCell(CellValidateEvent event);
  }

  private final CellValidation cellValidation;
  private final ValidationOrigin validationOrigin;

  private ValidationPhase validationPhase;
  private boolean canceled;

  public CellValidateEvent(CellValidation cellValidation, ValidationOrigin validationOrigin) {
    super();
    this.cellValidation = cellValidation;
    this.validationOrigin = validationOrigin;
  }

  public void cancel() {
    setCanceled(true);
  }

  public CellValidation getCellValidation() {
    return cellValidation;
  }

  public String getColumnId() {
    return (cellValidation.getColumn() == null) ? null : cellValidation.getColumn().getId();
  }

  public String getNewValue() {
    return cellValidation.getNewValue();
  }

  public long getRowId() {
    return cellValidation.getRow().getId();
  }

  public ValidationOrigin getValidationOrigin() {
    return validationOrigin;
  }

  public ValidationPhase getValidationPhase() {
    return validationPhase;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public boolean isCellValidation() {
    return validationOrigin != null && validationOrigin.isCell();
  }

  public boolean isFormValidation() {
    return validationOrigin != null && validationOrigin.isForm();
  }

  public boolean isGridValidation() {
    return validationOrigin != null && validationOrigin.isGrid();
  }

  public boolean isNewRow() {
    return DataUtils.isNewRow(cellValidation.getRow());
  }

  public boolean isPostValidation() {
    return validationPhase != null && validationPhase.isPostValidation();
  }

  public boolean isPreValidation() {
    return validationPhase != null && validationPhase.isPreValidation();
  }

  public boolean sameValue() {
    return BeeUtils.equalsTrimRight(cellValidation.getOldValue(), cellValidation.getNewValue());
  }

  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  public void setValidationPhase(ValidationPhase validationPhase) {
    this.validationPhase = validationPhase;
  }
}
