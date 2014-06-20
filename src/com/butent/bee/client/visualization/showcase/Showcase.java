package com.butent.bee.client.visualization.showcase;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.visualization.AbstractDataTable.ColumnType;
import com.butent.bee.client.visualization.DataTable;
import com.butent.bee.client.visualization.VisualizationUtils;
import com.butent.bee.client.visualization.visualizations.AnnotatedTimeLine;
import com.butent.bee.client.visualization.visualizations.Gauge;
import com.butent.bee.client.visualization.visualizations.GeoMap;
import com.butent.bee.client.visualization.visualizations.ImageAreaChart;
import com.butent.bee.client.visualization.visualizations.ImageBarChart;
import com.butent.bee.client.visualization.visualizations.ImageChart;
import com.butent.bee.client.visualization.visualizations.ImageLineChart;
import com.butent.bee.client.visualization.visualizations.ImagePieChart;
import com.butent.bee.client.visualization.visualizations.ImageSparklineChart;
import com.butent.bee.client.visualization.visualizations.IntensityMap;
import com.butent.bee.client.visualization.visualizations.MapVisualization;
import com.butent.bee.client.visualization.visualizations.MotionChart;
import com.butent.bee.client.visualization.visualizations.OrgChart;
import com.butent.bee.client.visualization.visualizations.Table;
import com.butent.bee.client.visualization.visualizations.corechart.CoreChart;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Initializes visualization demos.
 */

public final class Showcase {

  private static final BeeLogger logger = LogUtils.getLogger(Showcase.class);
  
  private static boolean pomInjected;
  private static boolean pomLoaded;

  public static void open() {
    logger.info("loading api");
    final long start = System.currentTimeMillis();

    injectPom();

    VisualizationUtils.loadVisualizationApi(new Runnable() {
      @Override
      public void run() {
        logger.info(TimeUtils.elapsedSeconds(start), "api loaded");
        LeftTabPanel panel = new LeftTabPanel();

        panel.add(new AnnotatedDemo(), "AnnotatedTimeLine");
        panel.add(new AreaDemo(), "AreaChart");
        panel.add(new ImageAreaDemo(), "AreaChart (Image)");
        panel.add(new BarDemo(), "BarChart");
        panel.add(new ImageBarDemo(), "BarChart (Image)");
        panel.add(new ColumnDemo(), "ColumnChart");
        panel.add(new GaugeDemo(), "Gauge");
        panel.add(new GeoDemo(), "GeoMap");
        panel.add(new IntensityDemo(), "IntensityMap");
        panel.add(new LineDemo(), "LineChart");
        panel.add(new ImageLineDemo(), "LineChart (Image)");
        panel.add(new MapDemo(), "Map");
        if (isPomLoaded()) {
          panel.add(new MoneyDemo(), "MoneyChart");
        }
        panel.add(new MotionDemo(), "MotionChart");
        panel.add(new OrgDemo(), "OrgChart");
        panel.add(new PieDemo(), "PieChart");
        panel.add(new ImagePieDemo(), "PieChart (Image)");
        panel.add(new ImageDemo(), "RadarChart (Image)");
        panel.add(new ScatterDemo(), "ScatterChart");
        panel.add(new SparklineDemo(), "Sparkline (Image)");
        panel.add(new TableDemo(), "Table");

        panel.init("AreaChart");
        BeeKeeper.getScreen().showWidget(panel);
        logger.info(TimeUtils.elapsedSeconds(start), "showcase ready");
      }
    }, AnnotatedTimeLine.PACKAGE, CoreChart.PACKAGE, Gauge.PACKAGE, GeoMap.PACKAGE,
        ImageChart.PACKAGE, ImageLineChart.PACKAGE, ImageAreaChart.PACKAGE, ImageBarChart.PACKAGE,
        ImagePieChart.PACKAGE, IntensityMap.PACKAGE, MapVisualization.PACKAGE, MotionChart.PACKAGE,
        OrgChart.PACKAGE, Table.PACKAGE, ImageSparklineChart.PACKAGE);
  }

  static DataTable getCompanyPerformance() {
    DataTable data = DataTable.create();
    data.addColumn(ColumnType.STRING, "Metai");
    data.addColumn(ColumnType.NUMBER, "Pajamos");
    data.addColumn(ColumnType.NUMBER, "Sąnaudos");

    int rows = BeeUtils.randomInt(4, 12);
    int min1 = 800;
    int max1 = 2000;
    int min2 = 300;
    int max2 = 1500;

    data.addRows(rows);
    for (int i = 0; i < rows; i++) {
      data.setValue(i, 0, BeeUtils.toString(2011 - rows + i));
      data.setValue(i, 1, BeeUtils.randomInt(min1, max1));
      data.setValue(i, 2, BeeUtils.randomInt(min2, max2));
    }
    return data;
  }

  static DataTable getSales() {
    DataTable data = DataTable.create();
    data.addColumn(ColumnType.STRING, "Pavadinimas");
    data.addColumn(ColumnType.NUMBER, "Kiekis");

    int rows = 5;
    int min = 10;
    int max = 100;

    data.addRows(rows);
    data.setValue(0, 0, "Agurkai");
    data.setValue(1, 0, "Pomidorai");
    data.setValue(2, 0, "Obuoliai");
    data.setValue(3, 0, "Žirniai");
    data.setValue(4, 0, "Bulvės");

    for (int i = 0; i < rows; i++) {
      data.setValue(i, 1, BeeUtils.randomInt(min, max));
    }
    return data;
  }

  private static native boolean checkPom() /*-{
    if ($wnd['PilesOfMoney']) {
      return true;
    }
    return false;
  }-*/;

  private static void injectPom() {
    if (!pomInjected) {
      DomUtils.injectExternalScript(
          "http://visapi-gadgets.googlecode.com/svn/trunk/pilesofmoney/pom.js");
      DomUtils.injectStyleSheet(
          "http://visapi-gadgets.googlecode.com/svn/trunk/pilesofmoney/pom.css");
      pomInjected = true;
    }
  }

  private static boolean isPomLoaded() {
    if (!pomLoaded) {
      pomLoaded = checkPom();
    }
    return pomLoaded;
  }

  private Showcase() {
  }
}
