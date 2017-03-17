package com.butent.bee.client.view.search;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;

import com.butent.bee.client.Global;
import com.butent.bee.client.event.logical.OpenEvent;
import com.butent.bee.client.ui.AutocompleteProvider;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.widget.InputLong;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class IdFilterSupplier extends AbstractFilterSupplier {

  private final InputLong editor;

  private Long oldValue;

  public IdFilterSupplier(String viewName, BeeColumn column, String label, String options) {
    super(viewName, column, label, options);

    this.editor = new InputLong();
    editor.addStyleName(DEFAULT_STYLE_PREFIX + "id-editor");

    if (!BeeUtils.isEmpty(viewName)) {
      AutocompleteProvider.enableAutocomplete(editor, viewName.trim() + "-id-filter");
    }

    editor.addKeyDownHandler(event -> {
      if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
        IdFilterSupplier.this.onSave();
      }
    });
  }

  @Override
  protected List<? extends IdentifiableWidget> getAutocompletableWidgets() {
    return Lists.newArrayList(editor);
  }

  @Override
  public FilterValue getFilterValue() {
    String value = getEditorValue();
    return value.isEmpty() ? null : FilterValue.of(value);
  }

  @Override
  public String getLabel() {
    return getEditorValue();
  }

  @Override
  public void onRequest(Element target, Scheduler.ScheduledCommand onChange) {
    setOldValue(BeeUtils.toLongOrNull(getEditorValue()));
    openDialog(target, editor, OpenEvent.focus(editor), onChange);
  }

  @Override
  public Filter parse(FilterValue input) {
    if (input != null && BeeUtils.isLong(input.getValue())) {
      return Filter.compareId(BeeUtils.toLong(input.getValue()));
    } else {
      return null;
    }
  }

  @Override
  public void setFilterValue(FilterValue filterValue) {
    editor.setValue((filterValue == null) ? null : filterValue.getValue());
  }

  @Override
  protected void onDialogCancel() {
    if (getOldValue() == null) {
      editor.clearValue();
    } else {
      editor.setValue(BeeUtils.toString(getOldValue()));
    }
  }

  private String getEditorValue() {
    return BeeUtils.trim(editor.getValue());
  }

  private Long getOldValue() {
    return oldValue;
  }

  private void onSave() {
    String value = getEditorValue();

    if (BeeUtils.isEmpty(value)) {
      update(getOldValue() != null);

    } else {
      Long id = BeeUtils.toLongOrNull(value);
      if (id == null) {
        Global.showError(Localized.dictionary().invalidIdValue(value));
      } else {
        update(!id.equals(getOldValue()));
      }
    }
  }

  private void setOldValue(Long oldValue) {
    this.oldValue = oldValue;
  }
}
