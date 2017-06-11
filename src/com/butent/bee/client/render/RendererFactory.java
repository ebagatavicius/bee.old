package com.butent.bee.client.render;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.modules.trade.DiscountRenderer;
import com.butent.bee.client.modules.trade.TotalRenderer;
import com.butent.bee.client.modules.trade.VatRenderer;
import com.butent.bee.client.modules.transport.CargoPlaceRenderer;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.RowFormatter;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.HasValueStartIndex;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.ui.RenderableToken;
import com.butent.bee.shared.ui.RendererDescription;
import com.butent.bee.shared.ui.RendererType;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RendererFactory {

  private static final BeeLogger logger = LogUtils.getLogger(RendererFactory.class);

  private static final Table<String, String, ProvidesGridColumnRenderer> gcrProviders =
      HashBasedTable.create();

  private static final Map<String, RowFormatter> treeFormatters = new HashMap<>();

  public static AbstractCellRenderer createRenderer(String viewName, List<String> renderColumns) {
    return createRenderer(viewName, renderColumns, null);
  }

  public static AbstractCellRenderer getGridColumnRenderer(String gridName, String columnName,
      List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
      CellSource cellSource) {

    ProvidesGridColumnRenderer provider = gcrProviders.get(gridName, columnName);
    if (provider == null) {
      return null;
    } else {
      return provider.getRenderer(columnName, dataColumns, columnDescription, cellSource);
    }
  }

  public static AbstractCellRenderer getRenderer(RendererDescription description,
      Calculation calculation, List<RenderableToken> tokens, String enumKey,
      List<String> renderColumns, List<BeeColumn> dataColumns, CellSource source) {

    if (description != null) {
      return createRenderer(description, enumKey, renderColumns, dataColumns, source);

    } else if (calculation != null) {
      return createRenderer(calculation, dataColumns, source);

    } else if (!BeeUtils.isEmpty(tokens)) {
      return createRenderer(tokens, dataColumns);

    } else if (!BeeUtils.isEmpty(enumKey) && source != null) {
      return new EnumRenderer(source, enumKey);

    } else if (!BeeUtils.isEmpty(renderColumns)) {
      if (renderColumns.size() == 1) {
        int index = DataUtils.getColumnIndex(renderColumns.get(0), dataColumns);

        if (BeeConst.isUndef(index)) {
          return new SimpleRenderer(CellSource.forProperty(renderColumns.get(0), null,
              ValueType.TEXT));
        } else {
          return new SimpleRenderer(CellSource.forColumn(dataColumns.get(index), index));
        }

      } else {
        return new JoinRenderer(dataColumns, null, renderColumns);
      }

    } else {
      return null;
    }
  }

  public static AbstractCellRenderer getRenderer(RendererDescription description,
      Calculation calculation, List<RenderableToken> tokens, String enumKey,
      List<String> renderColumns, List<BeeColumn> dataColumns, CellSource cellSource,
      Relation relation) {

    List<BeeColumn> columns;
    CellSource source;

    if (relation != null && relation.renderSource()) {
      columns = Data.getColumns(relation.getViewName());
      source = null;
    } else {
      columns = dataColumns;
      source = getDataSource(description, calculation, enumKey, dataColumns, cellSource, relation);
    }

    return getRenderer(description, calculation, tokens, enumKey, renderColumns, columns, source);
  }

  public static RowFormatter getTreeFormatter(String treeName) {
    return BeeUtils.isEmpty(treeName) ? null : treeFormatters.get(treeName);
  }

  public static void registerGcrProvider(String gridName, String columnName,
      ProvidesGridColumnRenderer provider) {

    Assert.notEmpty(gridName);
    Assert.notEmpty(columnName);
    Assert.notNull(provider);

    gcrProviders.put(gridName, columnName, provider);
  }

  public static void registerTreeFormatter(String treeName, RowFormatter rowFormatter) {
    Assert.notEmpty(treeName);
    Assert.notNull(rowFormatter);

    treeFormatters.put(treeName, rowFormatter);
  }

  private static AbstractCellRenderer createRenderer(Calculation calculation,
      List<? extends IsColumn> dataColumns, CellSource source) {

    Assert.notNull(calculation);

    String columnId = (source == null) ? null : source.getName();

    Evaluator evaluator = Evaluator.create(calculation, columnId, dataColumns);
    if (source != null) {
      Integer index = source.getIndex();
      if (index != null) {
        evaluator.setColIndex(index);
      }
    }

    return new EvalRenderer(source, evaluator);
  }

  private static AbstractCellRenderer createRenderer(List<RenderableToken> tokens,
      List<? extends IsColumn> dataColumns) {

    if (BeeUtils.isEmpty(tokens) || BeeUtils.isEmpty(dataColumns)) {
      return null;
    }

    List<ColumnToken> columnTokens = new ArrayList<>();
    for (RenderableToken token : tokens) {
      String source = token.getSource();
      if (BeeUtils.isEmpty(source)) {
        continue;
      }

      int index = DataUtils.getColumnIndex(source, dataColumns);
      ValueType type = null;

      if (BeeConst.isUndef(index)) {
        if (BeeUtils.same(source, DataUtils.ID_TAG)) {
          index = DataUtils.ID_INDEX;
          type = DataUtils.ID_TYPE;
        } else if (BeeUtils.same(source, DataUtils.VERSION_TAG)) {
          index = DataUtils.VERSION_INDEX;
          type = ValueType.DATE_TIME;
        }

      } else {
        type = dataColumns.get(index).getType();
      }

      if (type == null) {
        logger.severe("token source not recognized:", source);
      } else {
        columnTokens.add(ColumnToken.create(index, type, token));
      }
    }

    if (columnTokens.isEmpty()) {
      logger.severe("cannot create TokenRenderer");
      return null;
    }
    return new TokenRenderer(columnTokens);
  }

  private static AbstractCellRenderer createRenderer(RendererDescription description,
      String enumKey, List<String> renderColumns, List<BeeColumn> dataColumns,
      CellSource source) {

    Assert.notNull(description);
    RendererType type = description.getType();
    Assert.notNull(type);

    if (source == null && type.requiresSource()) {
      logger.warning("renderer", type.getTypeCode(), "requires source");
      return null;
    }

    AbstractCellRenderer renderer = null;
    switch (type) {
      case LIST:
        renderer = new ListRenderer(source);
        break;

      case MAP:
        renderer = new MapRenderer(source, description.getSeparator());
        break;

      case RANGE:
        renderer = new RangeRenderer(source, description.getSeparator(), description.getOptions());
        break;

      case ENUM:
        if (!BeeUtils.isEmpty(enumKey)) {
          renderer = new EnumRenderer(source, enumKey);
        } else {
          logger.warning("EnumRenderer: key not specified");
        }
        break;

      case JOIN:
        renderer = new JoinRenderer(dataColumns, description.getSeparator(), renderColumns);
        break;

      case FLAG:
        renderer = new FlagRenderer(source);
        break;

      case STAR:
        renderer = new StarRenderer(source);
        break;

      case ATTACHMENT:
        renderer = new AttachmentRenderer(source);
        break;

      case IMAGE:
        renderer = new ImageRenderer(source);
        break;

      case FILE_ICON:
        renderer = new FileIconRenderer(source);
        break;

      case FILE_SIZE:
        renderer = new FileSizeRenderer(source);
        break;

      case PHOTO:
        renderer = new PhotoRenderer(source);
        break;

      case MAIL:
        renderer = new MailAddressRenderer(dataColumns, renderColumns);
        break;

      case URL:
        renderer = new UrlRenderer(source);
        break;

      case TOTAL:
        renderer = new TotalRenderer(dataColumns);
        break;

      case VAT:
        renderer = new VatRenderer(dataColumns);
        break;

      case DISCOUNT:
        renderer = new DiscountRenderer(dataColumns);
        break;

      case BRANCH:
        renderer = new BranchRenderer(source, description.getSeparator(), description.getOptions());
        break;

      case TIME:
        renderer = new TimeRenderer(source, description.getOptions());
        break;

      case TOKEN:
        logger.severe("renderer", type.name(), "not supported");
        break;

      case PLACE:
        renderer = new CargoPlaceRenderer(source, renderColumns, description.getOptions());
        break;
    }

    if (renderer instanceof HasValueStartIndex && description.getValueStartIndex() != null) {
      ((HasValueStartIndex) renderer).setValueStartIndex(description.getValueStartIndex());
    }

    if (renderer instanceof HasItems && description.getItems() != null) {
      ((HasItems) renderer).setItems(description.getItems());
    }
    return renderer;
  }

  private static AbstractCellRenderer createRenderer(String viewName, List<String> renderColumns,
      String separator) {

    Assert.notEmpty(viewName);
    Assert.notEmpty(renderColumns);

    DataInfo dataInfo = Data.getDataInfo(viewName);
    if (dataInfo == null) {
      return null;
    }

    if (renderColumns.size() > 1) {
      return new JoinRenderer(dataInfo.getColumns(), separator, renderColumns);
    } else {
      int index = dataInfo.getColumnIndex(renderColumns.get(0));
      return new SimpleRenderer(CellSource.forColumn(dataInfo.getColumns().get(index), index));
    }
  }

  private static CellSource getDataSource(RendererDescription description, Calculation calculation,
      String enumKey, List<? extends IsColumn> dataColumns, CellSource source, Relation relation) {

    if (relation == null || BeeUtils.isEmpty(enumKey)) {
      return source;
    }
    if (description != null && !RendererType.ENUM.equals(description.getType())) {
      return source;
    }
    if (calculation != null) {
      return source;
    }

    int index = DataUtils.getColumnIndex(relation.getOriginalTarget(), dataColumns);
    if (!BeeConst.isUndef(index) && ValueType.isNumeric(dataColumns.get(index).getType())) {
      return CellSource.forColumn(dataColumns.get(index), index);
    } else {
      return source;
    }
  }

  private RendererFactory() {
  }
}
