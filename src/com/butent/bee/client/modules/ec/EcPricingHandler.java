package com.butent.bee.client.modules.ec;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.column.AbstractColumn;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.render.HasCellRenderer;
import com.butent.bee.client.view.edit.EditableColumn;
import com.butent.bee.client.view.grid.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.GridInterceptor;
import com.butent.bee.client.widget.InputNumber;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.ec.EcConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

class EcPricingHandler extends AbstractGridInterceptor {
  
  private static final class CategoryNameRenderer extends AbstractCellRenderer {
    
    private final int nameIndex;
    private final int fullNameIndex;

    private final int indent;

    private static final Element element = Document.get().createDivElement(); 
    
    private CategoryNameRenderer(int nameIndex, int fullNameIndex, String options) {
      super(null);
      
      this.nameIndex = nameIndex;
      this.fullNameIndex = fullNameIndex;
      
      this.indent = BeeUtils.positive(BeeUtils.toInt(options), 10);
    }

    @Override
    public String render(IsRow row) {
      if (row == null) {
        return null;
      }
      
      String name = row.getString(nameIndex);
      String fullName = row.getString(fullNameIndex);
      
      int level = BeeUtils.count(fullName, EcConstants.CATEGORY_NAME_SEPARATOR);
      if (level > 0) {
        element.setInnerText(BeeUtils.replicate(BeeConst.CHAR_NBSP, level * indent) + name);
        element.setTitle(fullName);
        return element.getString();
      } else {
        return name;
      }
    }
  }
  
  EcPricingHandler() {
  }

  @Override
  public boolean afterCreateColumn(String columnName, List<? extends IsColumn> dataColumns,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer,
      EditableColumn editableColumn) {

    if (BeeUtils.same(columnName, "Name") && column instanceof HasCellRenderer) {
      int nameIndex = DataUtils.getColumnIndex(EcConstants.COL_TCD_CATEGORY_NAME, dataColumns);
      int fullNameIndex = DataUtils.getColumnIndex(EcConstants.COL_TCD_CATEGORY_FULL_NAME,
          dataColumns);

      ((HasCellRenderer) column).setRenderer(new CategoryNameRenderer(nameIndex, fullNameIndex,
          column.getOptions()));
    }
    
    return super.afterCreateColumn(columnName, dataColumns, column, header, footer, editableColumn);
  }

  @Override
  public GridInterceptor getInstance() {
    return new EcPricingHandler();
  }

  @Override
  public void onShow(final GridPresenter presenter) {
    EcKeeper.getConfiguration(new Consumer<Map<String, String>>() {
      @Override
      public void accept(Map<String, String> input) {
        String value = input.get(EcConstants.COL_CONFIG_MARGIN_DEFAULT_PERCENT);
        
        String stylePrefix = EcStyles.name("Margins-defPercent-");
        
        presenter.getHeader().clearCommandPanel();
        
        Label label = new Label(Localized.getConstants().ecMarginDefaultPercent());
        label.addStyleName(stylePrefix + "label");
        presenter.getHeader().addCommandItem(label);
        
        InputNumber dmpInput = new InputNumber();
        dmpInput.addStyleName(stylePrefix + "input");

        if (!BeeUtils.isEmpty(value)) {
          dmpInput.setValue(value);
        }
        
        dmpInput.addValueChangeHandler(new ValueChangeHandler<String>() {
          @Override
          public void onValueChange(ValueChangeEvent<String> event) {
            EcKeeper.saveConfiguration(EcConstants.COL_CONFIG_MARGIN_DEFAULT_PERCENT,
                event.getValue());
          }
        });
        
        presenter.getHeader().addCommandItem(dmpInput);
      }
    });
  }
}
