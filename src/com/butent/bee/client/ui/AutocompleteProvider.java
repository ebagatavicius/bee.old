package com.butent.bee.client.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.OptionElement;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Features;
import com.butent.bee.client.screen.BodyPanel;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.DataEvent;
import com.butent.bee.shared.data.event.HandlesAllDataEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.html.Autocomplete;
import com.butent.bee.shared.html.Keywords;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.ui.HasAutocomplete;
import com.butent.bee.shared.ui.HasSuggestionSource;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AutocompleteProvider implements HandlesAllDataEvents {

  private enum Type {
    NATIVE {
      @Override
      boolean enable(IdentifiableWidget widget, String key, Autocomplete autocomplete) {
        if (isCandidate(widget)) {
          ((HasAutocomplete) widget).setName(key);

          if (autocomplete == null) {
            ((HasAutocomplete) widget).setAutocomplete(Autocomplete.ON);
          } else {
            ((HasAutocomplete) widget).setAutocomplete(autocomplete);
          }

          return true;

        } else {
          return false;
        }
      }

      @Override
      String getValue(IdentifiableWidget widget) {
        return isCandidate(widget) ? ((HasAutocomplete) widget).getValue() : null;
      }

      @Override
      boolean isCandidate(IdentifiableWidget widget) {
        if (widget instanceof HasAutocomplete) {
          return ((HasAutocomplete) widget).isMultiline() ? supportsMultiline() : isSupported();
        } else {
          return false;
        }
      }

      @Override
      boolean isSubmittable(IdentifiableWidget widget, String value) {
        if (isCandidate(widget)) {
          HasAutocomplete field = (HasAutocomplete) widget;
          return !BeeUtils.anyEmpty(field.getName(), field.getAutocomplete())
              && !BeeUtils.same(field.getAutocomplete(), Autocomplete.OFF)
              && BeeUtils.hasLength(BeeUtils.trim(value), MIN_VALUE_LENGTH);
        } else {
          return false;
        }
      }

      @Override
      boolean isSupported() {
        return Features.supportsAutocompleteInput();
      }

      @Override
      void refresh() {
      }

      @Override
      boolean save(Collection<Pair<IdentifiableWidget, String>> widgetsAndValues) {
        Set<String> names = new HashSet<>();
        List<Pair<IdentifiableWidget, String>> duplicates = new ArrayList<>();

        List<Element> elements = new ArrayList<>();

        for (Pair<IdentifiableWidget, String> wav : widgetsAndValues) {
          HasAutocomplete field = (HasAutocomplete) wav.getA();

          String name = field.getName();
          if (names.contains(name)) {
            duplicates.add(wav);

          } else {
            names.add(name);
            Element element = field.isMultiline()
                ? Document.get().createTextAreaElement() : Document.get().createTextInputElement();

            DomUtils.setName(element, name);
            DomUtils.setAutocomplete(element, field.getAutocomplete());

            DomUtils.setValue(element, wav.getB());
            elements.add(element);
          }
        }

        if (elements.isEmpty()) {
          return false;
        }

        String frameName = NameUtils.createUniqueName("frame");

        final IFrameElement frame = Document.get().createIFrameElement();

        frame.setName(frameName);
        frame.setSrc(Keywords.URL_ABOUT_BLANK);

        BodyPanel.conceal(frame);

        final FormElement form = Document.get().createFormElement();

        form.setTarget(frameName);
        form.setMethod(Keywords.METHOD_POST);
        form.setAction(Keywords.URL_ABOUT_BLANK);

        for (Element element : elements) {
          form.appendChild(element);
        }

        BodyPanel.conceal(form);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            form.submit();

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
              @Override
              public void execute() {
                form.removeFromParent();
                frame.removeFromParent();
              }
            });
          }
        });

        if (!duplicates.isEmpty()) {
          save(duplicates);
        }

        return true;
      }

      @Override
      boolean supportsMultiline() {
        return Features.supportsAutocompleteTextArea();
      }
    },

    DATA_LIST {
      @Override
      boolean enable(IdentifiableWidget widget, String key, Autocomplete autocomplete) {
        if (isCandidate(widget)) {
          ((HasSuggestionSource) widget).setSuggestionSource(key);
          return true;
        } else {
          return false;
        }
      }

      @Override
      String getValue(IdentifiableWidget widget) {
        return isCandidate(widget) ? ((HasSuggestionSource) widget).getValue() : null;
      }

      @Override
      boolean isCandidate(IdentifiableWidget widget) {
        if (widget instanceof HasSuggestionSource) {
          return ((HasSuggestionSource) widget).isMultiline() ? supportsMultiline() : isSupported();
        } else {
          return false;
        }
      }

      @Override
      boolean isSubmittable(IdentifiableWidget widget, String value) {
        if (isCandidate(widget)) {
          return !BeeUtils.isEmpty(((HasSuggestionSource) widget).getSuggestionSource())
              && BeeUtils.hasLength(BeeUtils.trim(value), MIN_VALUE_LENGTH);
        } else {
          return false;
        }
      }

      @Override
      boolean isSupported() {
        return Features.supportsElementDataList() && Features.supportsAttributeList();
      }

      @Override
      void refresh() {
        BeeKeeper.getRpc().makeGetRequest(Service.GET_AUTOCOMPLETE, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            load(response.hasResponse() ? response.getResponseAsString() : null);
          }
        });
      }

      @Override
      boolean save(Collection<Pair<IdentifiableWidget, String>> widgetsAndValues) {
        boolean updated = false;

        for (Pair<IdentifiableWidget, String> wav : widgetsAndValues) {
          HasSuggestionSource widget = (HasSuggestionSource) wav.getA();

          String key = BeeUtils.trim(widget.getSuggestionSource());
          String value = BeeUtils.trim(wav.getB());

          if (!BeeUtils.anyEmpty(key, value) && INSTANCE.updateData(key, value)) {
            ParameterList params = new ParameterList(Service.UPDATE_AUTOCOMPLETE);
            params.addDataItem(COL_AUTOCOMPLETE_KEY, key);
            params.addDataItem(COL_AUTOCOMPLETE_VALUE, value);

            BeeKeeper.getRpc().makeRequest(params);
            updated = true;
          }
        }

        return updated;
      }

      @Override
      boolean supportsMultiline() {
        return false;
      }
    };

    abstract boolean enable(IdentifiableWidget widget, String key, Autocomplete autocomplete);

    abstract String getValue(IdentifiableWidget widget);

    abstract boolean isCandidate(IdentifiableWidget widget);

    abstract boolean isSubmittable(IdentifiableWidget widget, String value);

    abstract boolean isSupported();

    abstract void refresh();

    abstract boolean save(Collection<Pair<IdentifiableWidget, String>> widgetsAndValues);

    abstract boolean supportsMultiline();
  }

  private static final BeeLogger logger = LogUtils.getLogger(AutocompleteProvider.class);

  private static final String KEY_PREFIX = "ac-";
  private static final String KEY_SEPARATOR = "-";

  private static final int MIN_VALUE_LENGTH = 3;

  private static final AutocompleteProvider INSTANCE = new AutocompleteProvider();

  public static boolean enableAutocomplete(IdentifiableWidget widget, String key) {
    Assert.notNull(widget);
    Assert.notEmpty(key);

    return INSTANCE.enable(widget, key, null);
  }

  public static boolean enableAutocomplete(IdentifiableWidget widget, String first, String second) {
    return enableAutocomplete(widget, generateKey(first, second));
  }

  public static boolean isAutocompleteCandidate(IdentifiableWidget widget) {
    return INSTANCE.isCandidate(widget);
  }

  public static void load(String serialized) {
    INSTANCE.clearData();

    String[] arr = Codec.beeDeserializeCollection(serialized);
    if (ArrayUtils.isEmpty(arr)) {
      return;
    }

    int numberOfKeys = 0;
    int numberOfValues = 0;

    int pos = 0;
    while (pos < arr.length - 2) {
      String key = arr[pos];
      if (BeeUtils.isEmpty(key)) {
        logger.severe("load autocomplete: no key at", pos, "of", arr.length);
        break;
      }

      pos++;
      int cnt = BeeUtils.toInt(arr[pos]);
      if (cnt <= 0 && pos + cnt >= arr.length) {
        logger.severe("invalid count:", arr[pos], "at", pos, "of", arr.length, "key", key);
        break;
      }

      List<String> values = new ArrayList<>();

      pos++;
      for (int i = pos; i < pos + cnt; i++) {
        values.add(arr[pos]);
      }

      if (INSTANCE.addData(key, values)) {
        numberOfKeys++;
        numberOfValues += values.size();
      }

      pos += cnt;
    }

    logger.info("autocomplete", numberOfKeys, numberOfValues);
  }

  public static boolean maybeEnableAutocomplete(IdentifiableWidget widget,
      Map<String, String> attributes, String formName, String widgetName,
      String viewName, String columnId) {

    if (widget == null || BeeUtils.isEmpty(attributes)) {
      return false;
    }

    String ac = attributes.get(HasAutocomplete.ATTR_AUTOCOMPLETE);
    if (BeeConst.isFalse(ac)) {
      return false;
    }

    String key = attributes.get(HasAutocomplete.ATTR_AUTOCOMPLETE_KEY);
    String field = attributes.get(HasAutocomplete.ATTR_AUTOCOMPLETE_FIELD);

    if (BeeUtils.allEmpty(key, field)) {
      if (BeeUtils.isEmpty(ac)) {
        return false;
      }

      key = generateKey(viewName, columnId);
      if (BeeUtils.isEmpty(key)) {
        key = generateKey(formName, widgetName);

        if (BeeUtils.isEmpty(key)) {
          logger.warning("autocomplete cannot generate key:", formName, viewName, attributes);
          return false;
        }
      }
    }

    if (BeeUtils.isEmpty(field)) {
      return enableAutocomplete(widget, key);
    }

    Autocomplete autocomplete = new Autocomplete();

    String section = attributes.get(HasAutocomplete.ATTR_AUTOCOMPLETE_SECTION);
    if (!BeeUtils.isEmpty(section)) {
      autocomplete.setSection(section);
    }

    String hint = attributes.get(HasAutocomplete.ATTR_AUTOCOMPLETE_HINT);
    if (!BeeUtils.isEmpty(hint)) {
      autocomplete.setHint(hint);
    }

    String contact = attributes.get(HasAutocomplete.ATTR_AUTOCOMPLETE_CONTACT);
    if (!BeeUtils.isEmpty(contact)) {
      autocomplete.setContact(contact);
    }

    autocomplete.setField(field);

    if (BeeUtils.isEmpty(key)) {
      key = KEY_PREFIX + BeeUtils.join(KEY_SEPARATOR, section, hint, contact, field);
    }

    return INSTANCE.enable(widget, key, autocomplete);
  }

  public static boolean retainValue(IdentifiableWidget widget) {
    if (INSTANCE.isSubmittable(widget)) {
      Set<IdentifiableWidget> widgets = Collections.singleton(widget);
      return INSTANCE.save(widgets);
    } else {
      return false;
    }
  }

  public static boolean retainValue(IdentifiableWidget widget, String value) {
    if (INSTANCE.isSubmittable(widget, value)) {
      return INSTANCE.save(widget, value);
    } else {
      return false;
    }
  }

  public static boolean retainValues(Collection<? extends IdentifiableWidget> widgets) {
    if (BeeUtils.isEmpty(widgets)) {
      return false;
    }

    List<IdentifiableWidget> fields = new ArrayList<>();
    for (IdentifiableWidget widget : widgets) {
      if (INSTANCE.isSubmittable(widget)) {
        fields.add(widget);
      }
    }

    return fields.isEmpty() ? false : INSTANCE.save(fields);
  }

  public static boolean retainValues(FormView form) {
    Assert.notNull(form);

    List<IdentifiableWidget> widgets = new ArrayList<>();

    for (EditableWidget editableWidget : form.getEditableWidgets()) {
      if (editableWidget.isDirty() && INSTANCE.isSubmittable(editableWidget.getEditor())) {
        widgets.add(editableWidget.getEditor());
      }
    }

    return widgets.isEmpty() ? false : INSTANCE.save(widgets);
  }

  public static boolean switchTo(String input) {
    Type newType = EnumUtils.getEnumByName(Type.class, input);
    if (newType != null && newType != INSTANCE.getType()) {
      INSTANCE.setType(newType);
      newType.refresh();
      logger.debug(NameUtils.getClassName(AutocompleteProvider.class), newType.name());

      return true;

    } else {
      return false;
    }
  }

  private static OptionElement createOption(String value) {
    OptionElement option = Document.get().createOptionElement();
    option.setValue(value);
    return option;
  }

  private static String generateKey(String first, String second) {
    if (BeeUtils.anyEmpty(first, second)) {
      return null;
    } else {
      return KEY_PREFIX + first.trim() + KEY_SEPARATOR + second.trim();
    }
  }

  private static OptionElement getOptionElement(Element parent, String value) {
    for (Element child = parent.getFirstChildElement(); child != null; child =
        child.getNextSiblingElement()) {
      if (OptionElement.is(child) && BeeUtils.same(OptionElement.as(child).getValue(), value)) {
        return OptionElement.as(child);
      }
    }

    return null;
  }

  private final Element dataContainer;

  private Type type;

  private AutocompleteProvider() {
    this.dataContainer = Document.get().createDivElement();
    dataContainer.addClassName(BeeConst.CSS_CLASS_PREFIX + "AutocompleteData");

    BodyPanel.get().getElement().appendChild(dataContainer);

    this.type = Type.NATIVE;

    BeeKeeper.getBus().registerDataHandler(this, false);
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onDataChange(DataChangeEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowInsert(RowInsertEvent event) {
    onDataEvent(event);
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    onDataEvent(event);
  }

  private boolean addData(String key, List<String> values) {
    if (Features.supportsElementDataList()) {
      Element dataList = DomUtils.createElement(Tags.DATA_LIST);
      dataList.setId(key);

      for (String value : values) {
        OptionElement option = createOption(value);
        dataList.appendChild(option);
      }

      dataContainer.appendChild(dataList);
      return true;

    } else {
      return false;
    }
  }

  private boolean addData(String key, String value) {
    return addData(key, Collections.singletonList(value));
  }

  private void clearData() {
    dataContainer.removeAllChildren();
  }

  private boolean enable(IdentifiableWidget widget, String key, Autocomplete autocomplete) {
    return getType() != null && getType().enable(widget, key, autocomplete);
  }

  private Element getDataList(String key) {
    for (Element child = dataContainer.getFirstChildElement(); child != null; child =
        child.getNextSiblingElement()) {
      if (DomUtils.idEquals(child, key)) {
        return child;
      }
    }

    return null;
  }

  private Type getType() {
    return type;
  }

  private boolean isCandidate(IdentifiableWidget widget) {
    return getType() != null && getType().isCandidate(widget);
  }

  private boolean isSubmittable(IdentifiableWidget widget) {
    return getType() != null && isSubmittable(widget, getType().getValue(widget));
  }

  private boolean isSubmittable(IdentifiableWidget widget, String value) {
    return getType() != null && getType().isSubmittable(widget, value);
  }

  private void onDataEvent(DataEvent event) {
    if (event != null && VIEW_AUTOCOMPLETE.equalsIgnoreCase(event.getViewName())
        && getType() != null) {
      getType().refresh();
    }
  }

  private boolean save(Collection<IdentifiableWidget> widgets) {
    if (getType() != null) {
      List<Pair<IdentifiableWidget, String>> widgetsAndValues = new ArrayList<>();
      for (IdentifiableWidget widget : widgets) {
        widgetsAndValues.add(Pair.of(widget, BeeUtils.trim(getType().getValue(widget))));
      }

      return getType().save(widgetsAndValues);

    } else {
      return false;
    }
  }

  private boolean save(IdentifiableWidget widget, String value) {
    if (getType() != null) {
      List<Pair<IdentifiableWidget, String>> widgetsAndValues = new ArrayList<>();
      widgetsAndValues.add(Pair.of(widget, BeeUtils.trim(value)));

      return getType().save(widgetsAndValues);

    } else {
      return false;
    }
  }

  private void setType(Type type) {
    this.type = type;
  }

  private boolean updateData(String key, String value) {
    Element dataList = getDataList(key);

    if (dataList == null) {
      return addData(key, value);

    } else {
      OptionElement option = getOptionElement(dataList, value);

      if (option == null) {
        dataList.insertFirst(createOption(value));
        return true;

      } else if (value.equals(option.getValue()) && option.getPreviousSiblingElement() == null) {
        return false;

      } else {
        option.removeFromParent();
        dataList.insertFirst(createOption(value));
        return true;
      }
    }
  }
}
