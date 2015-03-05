package com.butent.bee.client.grid.cell;

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.view.grid.GridMenu;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.EventState;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.rights.RightsState;
import com.butent.bee.shared.rights.RightsUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.HashMap;
import java.util.Map;

public class RightsCell extends AbstractCell<String> implements HasViewName {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "RightsCell-";

  private static final String STYLE_SUFFIX_ON = "-on";
  private static final String STYLE_SUFFIX_OFF = "-off";

  private static final Map<RightsState, Pair<SafeHtml, SafeHtml>> TEMPLATE;
  private static final Map<String, Pair<RightsState, Boolean>> TARGETS;

  static {
    TEMPLATE = new HashMap<>();
    TARGETS = new HashMap<>();

    FaLabel widgetOn;
    FaLabel widgetOff;

    FontAwesome faOn = null;
    FontAwesome faOff = null;

    for (RightsState state : GridMenu.ALL_STATES) {
      switch (state) {
        case VIEW:
          faOn = FontAwesome.EYE;
          faOff = FontAwesome.EYE_SLASH;
          break;

        case EDIT:
          faOn = FontAwesome.EDIT;
          faOff = FontAwesome.EDIT;
          break;

        case DELETE:
          faOn = FontAwesome.TRASH_O;
          faOff = FontAwesome.TRASH_O;
          break;

        case CREATE:
          Assert.unsupported();
          break;
      }

      widgetOn = new FaLabel(faOn, true);
      widgetOn.addStyleName(STYLE_PREFIX + state.name().toLowerCase());
      widgetOn.addStyleName(STYLE_PREFIX + state.name().toLowerCase() + STYLE_SUFFIX_ON);
      widgetOn.setTitle(state.getCaption());

      widgetOff = new FaLabel(faOff, true);
      widgetOff.addStyleName(STYLE_PREFIX + state.name().toLowerCase());
      widgetOff.addStyleName(STYLE_PREFIX + state.name().toLowerCase() + STYLE_SUFFIX_OFF);
      widgetOff.setTitle(state.getCaption());

      SafeHtml htmlOn = SafeHtmlUtils.fromTrustedString(widgetOn.getElement().getString());
      SafeHtml htmlOff = SafeHtmlUtils.fromTrustedString(widgetOff.getElement().getString());

      TEMPLATE.put(state, Pair.of(htmlOn, htmlOff));

      TARGETS.put(widgetOn.getId(), Pair.of(state, true));
      TARGETS.put(widgetOff.getId(), Pair.of(state, false));
    }
  }

  private final String viewName;
  private final long roleId;

  public RightsCell(String viewName, long roleId) {
    super(EventUtils.EVENT_TYPE_CLICK);

    this.viewName = viewName;
    this.roleId = roleId;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public EventState onBrowserEvent(CellContext context, Element parent, String value, Event event) {
    EventState state = super.onBrowserEvent(context, parent, value, event);

    if (state.proceed() && parent != null && EventUtils.isClick(event)
        && !BeeUtils.isEmpty(viewName) && DataUtils.hasId(context.getRow())) {

      String targetId = EventUtils.getEventTargetId(event);

      if (!BeeUtils.isEmpty(targetId) && TARGETS.containsKey(targetId)) {
        Pair<RightsState, Boolean> pair = TARGETS.get(targetId);
        update(context.getRow(), pair.getA(), !pair.getB(), parent);

        state = EventState.CONSUMED;
      }
    }

    return state;
  }

  @Override
  public void render(CellContext context, String value, SafeHtmlBuilder sb) {
    if (context.getRow() != null) {
      render(context.getRow(), sb);
    }
  }

  private void render(IsRow row, SafeHtmlBuilder sb) {
    for (RightsState state : GridMenu.ALL_STATES) {
      String value = row.getProperty(RightsUtils.getAlias(state, roleId));

      if (!BeeUtils.isEmpty(value)) {
        boolean on = Codec.unpack(value);

        Pair<SafeHtml, SafeHtml> pair = TEMPLATE.get(state);
        SafeHtml html = on ? pair.getA() : pair.getB();

        sb.append(html);
      }
    }
  }

  private void update(IsRow row, RightsState state, boolean value, Element cellElement) {
    row.setProperty(RightsUtils.getAlias(state, roleId), Codec.pack(value));

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    render(row, sb);
    cellElement.setInnerHTML(sb.toSafeHtml().asString());

    ParameterList params = BeeKeeper.getRpc().createParameters(Service.SET_ROW_RIGHTS);

    params.addDataItem(Service.VAR_VIEW_NAME, viewName);
    params.addDataItem(Service.VAR_ID, row.getId());

    params.addDataItem(AdministrationConstants.COL_ROLE, roleId);
    params.addDataItem(AdministrationConstants.COL_STATE, state.ordinal());
    params.addDataItem(Service.VAR_VALUE, Codec.pack(value));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          response.notify(BeeKeeper.getScreen());
        }
      }
    });
  }
}
