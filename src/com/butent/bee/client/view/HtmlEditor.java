package com.butent.bee.client.view;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DecisionCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.event.logical.ReadyEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.richtext.RichTextEditor;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Frame;
import com.butent.bee.client.widget.InputArea;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.HasWidgetSupplier;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.EnumSet;

public class HtmlEditor extends Flow implements Presenter, View, Printable, HasWidgetSupplier {

  private static final BeeLogger logger = LogUtils.getLogger(HtmlEditor.class);

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "HtmlEditor-";

  private static final String STYLE_VIEW = STYLE_PREFIX + "view";
  private static final String STYLE_CANVAS = STYLE_PREFIX + "canvas";

  private static final String STYLE_COMMAND = STYLE_PREFIX + "command";
  private static final String STYLE_FRAME = STYLE_PREFIX + "frame";
  private static final String STYLE_LABEL = STYLE_PREFIX + "label";

  private static final String STYLE_SUFFIX_URL = "-url";
  private static final String STYLE_SUFFIX_HTML = "-html";
  private static final String STYLE_SUFFIX_TEXT = "-text";

  private static final EnumSet<UiOption> uiOptions = EnumSet.of(UiOption.VIEW);

  private final String supplierKey;
  private final String caption;

  private final String oldUrl;
  private final String oldHtml;

  private String currentUrl;
  private String currentHtml;

  private final HeaderView headerView;
  private final Flow canvas;

  private final Frame urlFrame;
  private final CustomDiv htmlLabel;

  private final InputArea inputArea;
  private final RichTextEditor richText;

  private final BiConsumer<String, String> onSave;

  private boolean enabled = true;

  public HtmlEditor(String supplierKey, String caption, String url, String html,
      BiConsumer<String, String> onSave) {

    super(STYLE_VIEW);
    addStyleName(UiOption.getStyleName(uiOptions));

    this.supplierKey = supplierKey;
    this.caption = caption;

    this.oldUrl = url;
    this.oldHtml = html;

    this.currentUrl = url;
    this.currentHtml = html;

    this.onSave = onSave;

    this.headerView = new HeaderImpl();
    headerView.create(caption, false, true, null, uiOptions,
        EnumSet.of(Action.SAVE, Action.PRINT, Action.CLOSE), Action.NO_ACTIONS, Action.NO_ACTIONS);

    headerView.setViewPresenter(this);
    add(headerView);

    this.canvas = new Flow();
    canvas.addStyleName(STYLE_CANVAS);

    this.urlFrame = new Frame();
    urlFrame.addStyleName(STYLE_FRAME + STYLE_SUFFIX_URL);
    canvas.add(urlFrame);

    this.htmlLabel = new CustomDiv(STYLE_LABEL + STYLE_SUFFIX_HTML);
    canvas.add(htmlLabel);

    add(canvas);

    this.inputArea = new InputArea();
    inputArea.addStyleName(STYLE_PREFIX + "inputArea");

    this.richText = new RichTextEditor(true);
    richText.addStyleName(STYLE_PREFIX + "richText");

    initHeader();
    initCanvas();
  }

  @Override
  public HandlerRegistration addReadyHandler(ReadyEvent.Handler handler) {
    return addHandler(handler, ReadyEvent.getType());
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public String getSupplierKey() {
    return supplierKey;
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case SAVE:
        saveAndClose();
        break;

      case PRINT:
        Printer.print(this);
        break;

      case CANCEL:
        close();
        break;

      case CLOSE:
        if (changed()) {
          DecisionCallback callback = new DecisionCallback() {
            @Override
            public void onConfirm() {
              saveAndClose();
            }

            @Override
            public void onDeny() {
              close();
            }
          };

          Global.decide(getCaption(), Lists.newArrayList(Localized.getConstants().saveChanges()),
              callback, DialogConstants.DECISION_YES);

        } else {
          close();
        }
        break;

      default:
        logger.warning(getCaption(), action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    if (headerView.asWidget().getElement().isOrHasChild(source)) {
      return headerView.onPrint(source, target);
    } else {
      return true;
    }
  }

  @Override
  public void onResize() {
    super.onResize();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        updateSizes();
      }
    });
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public boolean reactsTo(Action action) {
    return EnumUtils.in(action, Action.SAVE, Action.PRINT, Action.CANCEL, Action.CLOSE);
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    updateSizes();

    ReadyEvent.fire(this);
  }

  private boolean changed() {
    return !BeeUtils.equalsTrim(oldUrl, getCurrentUrl())
        || !BeeUtils.equalsTrim(oldHtml, getCurrentHtml());
  }

  private void close() {
    BeeKeeper.getScreen().closeWidget(this);
  }

  private void editHtml() {
    richText.setValue(getCurrentHtml());

    Global.inputWidget("Html", richText, new InputCallback() {
      @Override
      public void onSuccess() {
        if (!BeeUtils.equalsTrim(richText.getValue(), getCurrentHtml())) {
          updateHtml(richText.getValue());
        }
      }
    });
  }

  private void editText() {
    inputArea.setValue(getCurrentHtml());

    Global.inputWidget("Text", inputArea, new InputCallback() {
      @Override
      public void onSuccess() {
        if (!BeeUtils.equalsTrim(inputArea.getValue(), getCurrentHtml())) {
          updateHtml(inputArea.getValue());
        }
      }
    });
  }

  private void editUrl() {
    Global.inputString("Url", null, new StringCallback(false) {
      @Override
      public void onSuccess(String value) {
        if (!BeeUtils.equalsTrim(value, getCurrentUrl())) {
          updateUrl(value);
        }
      }
    }, null, getCurrentUrl(), 100, null, 300, CssUnit.PX);
  }

  private String getCurrentHtml() {
    return currentHtml;
  }

  private String getCurrentUrl() {
    return currentUrl;
  }

  private boolean hasHtml() {
    return !BeeUtils.isEmpty(getCurrentHtml());
  }

  private boolean hasUrl() {
    return !BeeUtils.isEmpty(getCurrentUrl());
  }

  private void initCanvas() {
    if (hasUrl()) {
      urlFrame.setUrl(getCurrentUrl());
    } else {
      StyleUtils.hideDisplay(urlFrame);
    }

    if (hasHtml()) {
      htmlLabel.setHtml(getCurrentHtml());
    } else {
      StyleUtils.hideDisplay(htmlLabel);
    }
  }

  private void initHeader() {
    Button urlCommamnd = new Button("Url", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        editUrl();
      }
    });

    urlCommamnd.addStyleName(STYLE_COMMAND);
    urlCommamnd.addStyleName(STYLE_COMMAND + STYLE_SUFFIX_URL);

    headerView.addCommandItem(urlCommamnd);

    Button htmlCommamnd = new Button("Html", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        editHtml();
      }
    });

    htmlCommamnd.addStyleName(STYLE_COMMAND);
    htmlCommamnd.addStyleName(STYLE_COMMAND + STYLE_SUFFIX_HTML);

    headerView.addCommandItem(htmlCommamnd);

    Button textCommamnd = new Button("Text", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        editText();
      }
    });

    textCommamnd.addStyleName(STYLE_COMMAND);
    textCommamnd.addStyleName(STYLE_COMMAND + STYLE_SUFFIX_TEXT);

    headerView.addCommandItem(textCommamnd);
  }

  private void saveAndClose() {
    onSave.accept(getCurrentUrl(), getCurrentHtml());
    close();
  }

  private void setCurrentHtml(String currentHtml) {
    this.currentHtml = currentHtml;
  }

  private void setCurrentUrl(String currentUrl) {
    this.currentUrl = currentUrl;
  }

  private void updateHtml(String newHtml) {
    boolean had = hasHtml();
    setCurrentHtml(BeeUtils.trim(newHtml));

    if (hasHtml() != had) {
      updateSizes();
    }

    if (hasHtml()) {
      if (!had) {
        StyleUtils.unhideDisplay(htmlLabel);
      }
      htmlLabel.setHtml(getCurrentHtml());

    } else if (had) {
      htmlLabel.setHtml(BeeConst.STRING_EMPTY);
      StyleUtils.hideDisplay(htmlLabel);
    }
  }

  private void updateSizes() {
    if (!hasUrl() && !hasHtml()) {
      return;
    }

    int width = canvas.getOffsetWidth();
    if (width <= 0) {
      width = BeeKeeper.getScreen().getActivePanelWidth();
    }
    int height = canvas.getOffsetHeight();
    if (height <= 0) {
      height = BeeKeeper.getScreen().getActivePanelHeight();
    }

    if (width <= 10 || height <= 10) {
      return;
    }

    int margin = BeeUtils.resize(Math.min(width, height), 30, 1000, 0, 20);

    if (hasUrl() && hasHtml()) {
      StyleUtils.setLeft(urlFrame, margin);
      StyleUtils.setTop(urlFrame, margin);
      StyleUtils.setSize(urlFrame, width / 2 - margin * 2, height - margin * 2);

      StyleUtils.setLeft(htmlLabel, width / 2 + margin);
      StyleUtils.setTop(htmlLabel, margin);
      StyleUtils.setSize(htmlLabel, width / 2 - margin * 2, height - margin * 2);

    } else if (hasUrl()) {
      StyleUtils.setLeft(urlFrame, margin);
      StyleUtils.setTop(urlFrame, margin);
      StyleUtils.setSize(urlFrame, width - margin * 2, height - margin * 2);

    } else if (hasHtml()) {
      StyleUtils.setLeft(htmlLabel, margin);
      StyleUtils.setTop(htmlLabel, margin);
      StyleUtils.setSize(htmlLabel, width - margin * 2, height - margin * 2);
    }
  }

  private void updateUrl(String newUrl) {
    boolean had = hasUrl();
    setCurrentUrl(BeeUtils.trim(newUrl));

    if (hasUrl() != had) {
      updateSizes();
    }

    if (hasUrl()) {
      if (!had) {
        StyleUtils.unhideDisplay(urlFrame);
      }
      urlFrame.setUrl(getCurrentUrl());

    } else if (had) {
      urlFrame.setUrl(BeeConst.STRING_EMPTY);
      StyleUtils.hideDisplay(urlFrame);
    }
  }
}
