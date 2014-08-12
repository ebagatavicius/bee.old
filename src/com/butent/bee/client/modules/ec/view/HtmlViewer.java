package com.butent.bee.client.modules.ec.view;

import com.butent.bee.client.modules.ec.EcKeeper;
import com.butent.bee.client.modules.ec.EcStyles;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Frame;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

class HtmlViewer extends EcView {

  private final String caption;
  private final String urlKey;
  private final String htmlKey;

  HtmlViewer(String caption, String urlKey, String htmlKey) {
    super();

    this.caption = caption;
    this.urlKey = urlKey;
    this.htmlKey = htmlKey;
  }

  @Override
  protected void createUi() {
    EcKeeper.getConfiguration(new Consumer<Map<String, String>>() {
      @Override
      public void accept(Map<String, String> input) {
        String url = input.get(urlKey);
        String html = input.get(htmlKey);

        if (!BeeUtils.isEmpty(url)) {
          Frame frame = new Frame(url);
          EcStyles.add(frame, getPrimaryStyle(), "frame");
          add(frame);

        } else if (!BeeUtils.isEmpty(html)) {
          CustomDiv widget = new CustomDiv();
          EcStyles.add(widget, getPrimaryStyle(), "html");
          widget.setHtml(html);
          add(widget);

        } else {
          add(renderNoData(caption));
        }
      }
    });
  }

  @Override
  protected String getPrimaryStyle() {
    return "htmlViewer";
  }
}
