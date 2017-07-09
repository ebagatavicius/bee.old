package com.butent.bee.client.modules.cars;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.client.modules.cars.ConfPricelistForm.*;
import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_FILE_HASH;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.InputBoxes;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.utils.FileUtils;
import com.butent.bee.client.view.form.CloseCallback;
import com.butent.bee.client.view.search.SearchBox;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.exceptions.BeeException;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.html.builder.elements.Table;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.cars.Bundle;
import com.butent.bee.shared.modules.cars.Configuration;
import com.butent.bee.shared.modules.cars.Dimension;
import com.butent.bee.shared.modules.cars.Option;
import com.butent.bee.shared.modules.cars.Specification;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SpecificationBuilder implements InputCallback {

  private static final class Branch {
    private final Long id;
    private final String name;
    private final String photo;
    private final Boolean blocked;
    private final List<Branch> childs = new ArrayList<>();
    private Branch parent;
    private Configuration configuration;

    private Branch(Long id, String name, String photo, Boolean blocked) {
      this.id = id;
      this.name = name;
      this.photo = photo;
      this.blocked = blocked;
    }

    public void addChild(Branch child) {
      child.parent = this;
      childs.add(child);
    }

    public List<Branch> getChilds() {
      return childs;
    }

    public Configuration getConfiguration() {
      return configuration;
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public Branch getParent() {
      return parent;
    }

    public String getPath() {
      List<String> path = new ArrayList<>();
      Branch br = this;

      while (br.getParent() != null) {
        path.add(0, br.getName());
        br = br.getParent();
      }
      return BeeUtils.joinWords(path);
    }

    public String getPhoto() {
      return photo;
    }

    public boolean isBlocked() {
      return BeeUtils.unbox(blocked);
    }

    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }
  }

  public static final String STYLE_PREFIX = "bee-spec";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "-dialog";
  private static final String STYLE_CONTAINER = STYLE_PREFIX + "-container";
  private static final String STYLE_OPTIONS = STYLE_PREFIX + "-options";
  private static final String STYLE_BOX = STYLE_PREFIX + "-box";
  public static final String STYLE_THUMBNAIL = STYLE_PREFIX + "-thumbnail";
  private static final String STYLE_SELECTABLE = STYLE_PREFIX + "-selectable";
  public static final String STYLE_BLOCKED = STYLE_PREFIX + "-blocked";
  public static final String STYLE_DESCRIPTION = STYLE_PREFIX + "-description";
  public static final String STYLE_SUMMARY = STYLE_PREFIX + "-summary";

  private Specification template;
  private final Consumer<Specification> callback;
  private final Flow container = new Flow(STYLE_CONTAINER);
  private String searchTag = "";

  private Branch currentBranch;
  private final Specification specification = new Specification();

  public SpecificationBuilder(Specification template, Consumer<Specification> callback) {
    this.template = template;
    this.callback = callback;

    Queries.getRowSet(TBL_CONF_PRICELIST, null, result -> {
      Branch tree = new Branch(null, null, null, null);
      Multimap<Long, Branch> hierarchy = LinkedHashMultimap.create();

      for (int i = 0; i < result.getNumberOfRows(); i++) {
        hierarchy.put(result.getLong(i, COL_BRANCH), new Branch(result.getRow(i).getId(),
            result.getString(i, COL_BRANCH_NAME), result.getString(i, COL_FILE_HASH),
            result.getBoolean(i, COL_BLOCKED)));
      }
      fillTree(tree, hierarchy);
      setBranch(tree);
    });
    Popup dialog;

    if (Objects.isNull(callback)) {
      dialog = Global.showModalWidget(Localized.dictionary().specification(), container);
    } else {
      dialog = Global.inputWidget(Localized.dictionary().specification(), container, this);
    }
    dialog.addStyleName(STYLE_DIALOG);
    StyleUtils.setWidth(dialog, BeeKeeper.getScreen().getWidth() * 0.7, CssUnit.PX);
    StyleUtils.setHeight(dialog, BeeKeeper.getScreen().getHeight() * 0.9, CssUnit.PX);
  }

  @Override
  public String getErrorMessage() {
    Multimap<Dimension, Option> allOptions = getAvailableOptions();

    for (Dimension dimension : allOptions.keySet()) {
      if (dimension.isRequired()) {
        boolean ok = false;

        for (Option option : allOptions.get(dimension)) {
          if (specification.getOptions().contains(option)) {
            ok = true;
            break;
          }
        }
        if (!ok) {
          Global.showError(BeeUtils.join(": ", dimension.getName(),
              Localized.dictionary().valueRequired()));
          return InputBoxes.SILENT_ERROR;
        }
      }
    }
    return null;
  }

  @Override
  public void onClose(CloseCallback closeCallback) {
    if (specification.getBundle() != null) {
      Global.confirm(Localized.dictionary().actionClose(), closeCallback::onClose);
    } else {
      closeCallback.onClose();
    }
  }

  @Override
  public void onSuccess() {
    if (specification.getBundle() != null) {
      List<String> selectedOptions = new ArrayList<>();
      Dimension dimension = null;
      Configuration configuration = currentBranch.getConfiguration();

      for (Option option : specification.getOptions()) {
        if (!option.getDimension().isRequired()) {
          if (!Objects.equals(option.getDimension(), dimension)) {
            dimension = option.getDimension();
            selectedOptions.add("<i>" + dimension + ":</i>");
          }
          selectedOptions.add(BeeUtils.notEmpty(BeeUtils
                  .notEmpty(configuration.getRelationDescription(option, specification.getBundle()),
                      configuration.getOptionDescription(option), option.getDescription()),
              BeeUtils.joinWords(option.getCode(), option.getName())));
        }
      }
      if (!BeeUtils.isEmpty(selectedOptions)) {
        specification.setDescription(BeeUtils.join("<br><br><b>"
                + Localized.dictionary().additionalEquipment() + "</b><br>",
            specification.getDescription(), BeeUtils.join("<br>", selectedOptions)));
      }
      specification.setCriteria(collectCriteria(configuration));

      ParameterList args = CarsKeeper.createSvcArgs(SVC_SAVE_OBJECT);
      args.addDataItem(COL_OBJECT, Codec.beeSerialize(specification));

      BeeKeeper.getRpc().makePostRequest(args, response -> {
        response.notify(BeeKeeper.getScreen());

        if (!response.hasErrors()) {
          specification.setId(response.getResponseAsLong());
          callback.accept(specification);
        }
      });
    } else {
      callback.accept(null);
    }
  }

  public static String renderCriteria(Map<String, String> criteria) {
    Table crit = table().addClass(STYLE_SUMMARY);

    if (!BeeUtils.isEmpty(criteria)) {
      criteria.forEach((key, val) ->
          crit.append(tr().append(td().text(key)).append(td().text(val))));
    }
    return crit.toString();
  }

  private static Widget buildThumbnail(String choiceCaption, List<Widget[]> choices,
      Consumer<Integer> onChoice, Widget... widgets) {

    Flow thumbnail = new Flow(STYLE_THUMBNAIL);
    String styleActive = STYLE_THUMBNAIL + "-active";

    if (!ArrayUtils.isEmpty(widgets)) {
      for (Widget widget : widgets) {
        if (widget != null) {
          thumbnail.add(widget);
        }
      }
    }
    if (onChoice != null) {
      if (BeeUtils.isEmpty(choices)) {
        thumbnail.addClickHandler(clickEvent -> onChoice.accept(BeeConst.UNDEF));
        thumbnail.addStyleName(styleActive);

      } else if (choices.size() > 1 || thumbnail.isEmpty()) {
        thumbnail.addClickHandler(clickEvent -> {
          Flow box = new Flow(STYLE_BOX);
          int i = 0;

          for (Widget[] caps : choices) {
            int idx = i++;

            box.add(buildThumbnail(null, null, index -> {
              UiHelper.getParentPopup(box).close();
              onChoice.accept(idx);
            }, ArrayUtils.isEmpty(caps) ? new Widget[] {new FaLabel(FontAwesome.MINUS)} : caps));
          }
          Global.showModalWidget(choiceCaption, box, thumbnail.getElement());
        });
        if (thumbnail.isEmpty()) {
          if (!BeeUtils.isEmpty(choiceCaption)) {
            thumbnail.add(new Label(choiceCaption));
          }
          thumbnail.add(new FaLabel(FontAwesome.QUESTION));
        }
        thumbnail.addStyleName(styleActive);
      }
    }
    return thumbnail;
  }

  private Map<String, String> collectCriteria(Configuration configuration) {
    Map<String, String> criteria = configuration.getBundleCriteria(specification.getBundle());

    specification.getOptions().forEach(option -> {
      criteria.putAll(configuration.getOptionCriteria(option));
      criteria.putAll(configuration.getRelationCriteria(option, specification.getBundle()));
    });
    return criteria;
  }

  private void collectRestrictions(Multimap<Dimension, Option> allOptions, Option option,
      boolean on, Map<Option, Boolean> options) throws BeeException {

    if (!allOptions.containsValue(option) || Objects.equals(options.get(option), on)) {
      return;
    }
    if (options.containsKey(option)) {
      if (options.get(option) != null) {
        throw new BeeException(option.toString());
      } else if (on) {
        options.put(option, true);
        return;
      }
    } else if (!on) {
      options.put(option, false);
      return;
    }
    options.put(option, on);
    Configuration configuration = currentBranch.getConfiguration();

    if (on) {
      if (option.getDimension().isRequired()) {
        for (Option opt : allOptions.get(option.getDimension())) {
          if (!Objects.equals(opt, option)) {
            collectRestrictions(allOptions, opt, false, options);
          }
        }
      }
      for (Option opt : configuration.getDeniedOptions(option)) {
        collectRestrictions(allOptions, opt, false, options);
      }
      for (Option opt : configuration.getRequiredOptions(option)) {
        collectRestrictions(allOptions, opt, true, options);
      }
    } else {
      for (Option opt : options.keySet()) {
        if (configuration.getRequiredOptions(opt).contains(option)) {
          collectRestrictions(allOptions, opt, false, options);
        }
      }
    }
  }

  private static void fillTree(Branch parent, Multimap<Long, Branch> hierarchy) {
    if (hierarchy.containsKey(parent.getId())) {
      for (Branch branch : hierarchy.get(parent.getId())) {
        fillTree(branch, hierarchy);
        parent.addChild(branch);
      }
    }
  }

  private void filter(HtmlTable table, String value) {
    int nameIdx = 2;

    for (int i = 0; i < table.getRowCount(); i++) {
      List<TableCellElement> cells = table.getRowCells(i);

      if (cells.size() > 2) {
        boolean show = BeeUtils.isEmpty(value)
            || BeeUtils.containsSame(cells.get(1).getInnerText(), value)
            || BeeUtils.containsSame(cells.get(nameIdx).getInnerText(), value);

        Widget packetWidget = table.getWidget(i, nameIdx);
        int packetIdx = DomUtils.getDataIndexInt(table.getRow(i));

        if (Objects.nonNull(packetWidget)) {
          table.setText(i, nameIdx, packetWidget.getElement().getInnerText());

        } else if (!BeeConst.isUndef(packetIdx)) {
          if (show) {
            table.getRowFormatter().setVisible(packetIdx, true);

          } else if (Objects.isNull(table.getWidget(packetIdx, nameIdx))) {
            Label name = new Label(table.getCellFormatter().getElement(packetIdx, nameIdx)
                .getInnerText());
            name.setStyleName(STYLE_PACKET_COLLAPSED);

            name.addClickHandler(clickEvent -> {
              for (int j = packetIdx + 1; j < table.getRowCount(); j++) {
                if (!Objects.equals(DomUtils.getDataIndexInt(table.getRow(j)), packetIdx)) {
                  break;
                }
                table.getRowFormatter().setVisible(j, true);
              }
              table.setText(packetIdx, nameIdx, name.getText());
            });
            table.setWidget(packetIdx, nameIdx, name);
          }
        }
        table.getRowFormatter().setVisible(i, show);
      }
    }
    searchTag = value;
  }

  private static Branch findBranch(Branch branch, Long id) {
    Branch found = null;

    if (DataUtils.isId(branch.getId()) && Objects.equals(branch.getId(), id)) {
      found = branch;
    } else {
      for (Branch childBranch : branch.getChilds()) {
        found = findBranch(childBranch, id);

        if (Objects.nonNull(found)) {
          break;
        }
      }
    }
    return found;
  }

  private Multimap<Dimension, Option> getAvailableOptions() {
    Multimap<Dimension, Option> options = TreeMultimap.create();

    if (specification.getBundle() != null) {
      Configuration configuration = currentBranch.getConfiguration();

      for (Option option : configuration.getOptions()) {
        if (configuration.hasRelation(option, specification.getBundle())) {
          options.put(option.getDimension(), option);
        }
      }
    }
    return options;
  }

  private static Image getPhoto(String photo) {
    return BeeUtils.isEmpty(photo) ? null : new Image(FileUtils.getUrl(photo));
  }

  private Integer normPrice(Option option) {
    Configuration configuration = currentBranch.getConfiguration();
    Integer price = BeeUtils.toInt(BeeUtils.notEmpty(configuration.getRelationPrice(option,
        specification.getBundle()), configuration.getOptionPrice(option)));

    if (BeeUtils.isNegative(price)) {
      price = 0;
    }
    return price;
  }

  private void refresh() {
    renderDescription();
    Configuration configuration = currentBranch.getConfiguration();
    int scroll = 0;

    for (Widget widget : container) {
      if (widget.getElement().hasClassName(STYLE_OPTIONS)) {
        scroll = widget.getElement().getScrollTop();
      }
    }
    container.clear();
    Flow header = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);
    Flow boxes = new Flow(StyleUtils.NAME_FLEXIBLE);
    header.add(boxes);
    container.add(header);

    // BRANCHES
    List<Branch> path = new ArrayList<>();
    Branch br = currentBranch;

    while (br.getParent() != null) {
      path.add(0, br);
      br = br.getParent();
    }
    Flow branchBox = new Flow(STYLE_BOX);

    for (Branch branch : path) {
      List<Branch> branches = branch.getParent().getChilds().stream().filter(b -> !b.isBlocked())
          .collect(Collectors.toList());
      Widget label = new Label(branch.getName());

      if (branch.isBlocked()) {
        label.addStyleName(STYLE_BLOCKED);
      }
      Widget[] cap = new Widget[] {getPhoto(branch.getPhoto()), label};

      if (!BeeUtils.isEmpty(branches)) {
        List<Widget[]> caps = new ArrayList<>();
        branches.forEach(b ->
            caps.add(new Widget[] {getPhoto(b.getPhoto()), new Label(b.getName())}));

        branchBox.add(buildThumbnail(null, caps, index -> setBranch(branches.get(index)), cap));
      } else {
        branchBox.add(buildThumbnail(null, null, null, cap));
      }
    }
    List<Branch> branches = currentBranch.getChilds().stream().filter(b -> !b.isBlocked())
        .collect(Collectors.toList());

    if (branches.size() == 1 && configuration.isEmpty()) {
      setBranch(BeeUtils.peek(branches));
      return;
    } else if (!BeeUtils.isEmpty(branches)) {
      List<Widget[]> caps = new ArrayList<>();
      branches.forEach(b ->
          caps.add(new Widget[] {getPhoto(b.getPhoto()), new Label(b.getName())}));

      branchBox.add(buildThumbnail(null, caps, index -> setBranch(branches.get(index))));
    }
    boxes.add(branchBox);

    // BUNDLES
    Flow bundleBox = new Flow(STYLE_BOX);

    List<Dimension> allDimensions = configuration.getAllDimensions();
    List<Bundle> bundles = configuration.getMetrics(allDimensions).stream()
        .filter(b -> !configuration.isBundleBlocked(b)).collect(Collectors.toList());
    Set<Option> options = new HashSet<>();
    Holder<Boolean> stop = Holder.of(false);
    Holder<Boolean> proceed = Holder.of(!configuration.isEmpty());
    boolean blocked = configuration.isBundleBlocked(specification.getBundle());

    Configuration.processOptions(specification.getBundle(), allDimensions, (dimension, option) -> {
      Map<Option, Bundle> choices = new LinkedHashMap<>();

      for (Iterator<Bundle> iterator = bundles.iterator(); iterator.hasNext(); ) {
        Configuration.processOptions(iterator.next(), Collections.singleton(dimension),
            (dim, opt) -> {
              if (!Objects.equals(option, opt)) {
                iterator.remove();
              }
              if (!choices.containsKey(opt)) {
                Set<Option> tmp = new HashSet<>(options);

                if (opt != null) {
                  tmp.add(opt);
                }
                choices.put(opt, BeeUtils.isEmpty(tmp) ? null : new Bundle(tmp));
              }
              return true;
            });
      }
      List<Widget[]> caps = new ArrayList<>();
      choices.keySet().forEach(o -> caps.add(Objects.isNull(o) ? null
          : new Widget[] {getPhoto(o.getPhoto()), new Label(o.toString())}));

      if (option != null) {
        Label label = new Label(option.toString());

        if (blocked) {
          label.addStyleName(STYLE_BLOCKED);
        }
        bundleBox.add(buildThumbnail(dimension.getName(), caps, BeeUtils.isEmpty(caps) ? null
                : index -> setBundle(choices.get(new ArrayList<>(choices.keySet()).get(index))),
            new Label(dimension.getName()), getPhoto(option.getPhoto()), label));
        options.add(option);

      } else if (!BeeUtils.isEmpty(choices)) {
        boolean hasEmptyChoices = choices.containsKey(null);

        if (!hasEmptyChoices && choices.size() == 1) {
          setBundle(BeeUtils.peek(choices.values()));
          stop.set(true);
        } else {
          bundleBox.add(buildThumbnail(dimension.getName(), caps,
              index -> setBundle(choices.get(new ArrayList<>(choices.keySet()).get(index))),
              new Label(dimension.getName()),
              new FaLabel(hasEmptyChoices ? FontAwesome.MINUS : FontAwesome.QUESTION)));
        }
        proceed.set(hasEmptyChoices);
        return hasEmptyChoices;
      }
      return true;
    });
    if (stop.get()) {
      return;
    }
    boxes.add(bundleBox);

    if (!proceed.get()) {
      return;
    }
    // OPTIONS
    Flow subContainer = new Flow(STYLE_OPTIONS);
    container.add(subContainer);

    Multimap<Dimension, Option> allOptions = getAvailableOptions();
    Flow optionBox = new Flow(STYLE_BOX);
    HtmlTable selectable = new HtmlTable(STYLE_SELECTABLE);

    for (Dimension dimension : allOptions.keySet()) {
      List<Option> opts = new ArrayList<>(allOptions.get(dimension));

      if (dimension.isRequired()) {
        Option option = null;
        Option def = null;

        for (Option opt : opts) {
          if (specification.getOptions().contains(opt)) {
            option = opt;
            break;
          } else if (def == null && configuration.isDefault(opt, specification.getBundle())
              && !configuration.hasRestrictions(opt)) {
            def = opt;
          }
        }
        if (option == null && def != null) {
          option = def;
          specification.addOption(option, normPrice(option));
        }
        List<Widget[]> caps = new ArrayList<>();
        opts.forEach(o -> caps.add(new Widget[] {
            getPhoto(BeeUtils.nvl(configuration.getRelationPhoto(o, specification.getBundle()),
                configuration.getOptionPhoto(o), o.getPhoto())), new Label(o.toString())}));

        optionBox.add(buildThumbnail(dimension.getName(), caps,
            index -> toggleOption(allOptions, opts.get(index), true, false),
            Objects.isNull(option) ? null : new Widget[] {
                new Label(dimension.getName()),
                getPhoto(BeeUtils.nvl(configuration.getRelationPhoto(option,
                    specification.getBundle()), configuration.getOptionPhoto(option),
                    option.getPhoto())),
                new Label(option.toString())}));
      } else {
        int rowSelectable = BeeConst.UNDEF;

        for (Option option : opts) {
          if (!configuration.isDefault(option, specification.getBundle())) {
            CheckBox check = new CheckBox();
            check.setChecked(specification.getOptions().contains(option));
            check.addValueChangeHandler(valueChangeEvent ->
                toggleOption(allOptions, option, check.isChecked(), false));

            if (BeeConst.isUndef(rowSelectable)) {
              rowSelectable = selectable.getRowCount();
              selectable.setText(rowSelectable, 0, dimension.getName(), STYLE_GROUP);
              selectable.getCellFormatter().setColSpan(rowSelectable, 0, 4);
            }
            rowSelectable++;
            selectable.setWidget(rowSelectable, 0, check);
            selectable.setText(rowSelectable, 1, option.getCode());
            selectable.setText(rowSelectable, 2, option.getName());
            selectable.setText(rowSelectable, 3, BeeUtils.toString(normPrice(option)));

            Set<Long> packets = DataUtils.parseIdSet(configuration.getRelationPackets(option,
                specification.getBundle()));
            int packetIdx = rowSelectable;

            for (Option opt : configuration.getPackets(option)) {
              if (!packets.contains(opt.getId())) {
                rowSelectable++;
                selectable.getCellFormatter().setStyleName(rowSelectable, 0, STYLE_PACKET);
                selectable.setText(rowSelectable, 1, opt.getCode(), STYLE_PACKET);
                selectable.setText(rowSelectable, 2, opt.getName(), STYLE_PACKET);
                selectable.getCellFormatter().setStyleName(rowSelectable, 3, STYLE_PACKET);
                DomUtils.setDataIndex(selectable.getRow(rowSelectable), packetIdx);
              }
            }
          }
        }
      }
    }
    Flow descriptionBox = new Flow(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);

    CustomDiv descr = new CustomDiv(STYLE_DESCRIPTION);
    descr.setHtml(specification.getDescription());
    descriptionBox.add(descr);

    CustomDiv crit = new CustomDiv();
    crit.setHtml(renderCriteria(collectCriteria(configuration)));
    descriptionBox.add(crit);

    subContainer.add(descriptionBox);
    subContainer.add(optionBox);

    if (selectable.getRowCount() > 0) {
      InputText search = new SearchBox(Localized.dictionary().search());

      if (!BeeUtils.isEmpty(searchTag)) {
        search.setValue(searchTag);
        filter(selectable, searchTag);
      }
      search.addInputHandler(inputEvent -> filter(selectable, search.getValue()));
      subContainer.add(search);
      subContainer.add(selectable);
    }
    subContainer.getElement().setScrollTop(scroll);
    header.add(specification.renderSummary(true));
  }

  private void renderDescription() {
    List<String> defaults = new ArrayList<>();
    Bundle bundle = specification.getBundle();

    if (bundle != null) {
      Configuration configuration = currentBranch.getConfiguration();
      Dimension dimension = null;
      String description = configuration.getBundleDescription(bundle);

      if (!BeeUtils.isEmpty(description)) {
        defaults.add(description);
      } else {
        for (Option option : getAvailableOptions().values()) {
          if (!option.getDimension().isRequired() && configuration.isDefault(option, bundle)
              && Collections.disjoint(configuration.getDeniedOptions(option),
              specification.getOptions())) {

            if (!Objects.equals(option.getDimension(), dimension)) {
              dimension = option.getDimension();
              defaults.add("<i>" + dimension + ":</i>");
            }
            defaults.add(BeeUtils.notEmpty(BeeUtils
                    .notEmpty(configuration.getRelationDescription(option, bundle),
                        configuration.getOptionDescription(option), option.getDescription()),
                BeeUtils.joinWords(option.getCode(), option.getName())));
          }
        }
        if (!defaults.isEmpty()) {
          defaults.add(0, "<b>" + Localized.dictionary().equipment() + "</b>");
        }
      }
    }
    specification.setDescription(BeeUtils.join("<br>", defaults));
  }

  private void setBranch(Branch branch) {
    if (Objects.nonNull(template)) {
      Branch br = findBranch(branch, template.getBranchId());
      String templateBranch = template.getBranchName();

      if (Objects.isNull(br)) {
        BeeKeeper.getScreen().notifySevere(Localized.dictionary().keyNotFound(templateBranch));
        template = null;
        br = branch;
      } else {
        if (!Objects.equals(templateBranch, br.getPath())) {
          BeeKeeper.getScreen().notifyWarning(Localized.dictionary().keyNotFound(templateBranch));
        }
      }
      currentBranch = br;
    } else {
      currentBranch = branch;
    }
    specification.setBranch(currentBranch.getId(), currentBranch.getPath());

    if (Objects.isNull(currentBranch.getConfiguration())) {
      if (DataUtils.isId(currentBranch.getId())) {
        ParameterList args = CarsKeeper.createSvcArgs(SVC_GET_CONFIGURATION);
        args.addDataItem(COL_BRANCH, currentBranch.getId());

        BeeKeeper.getRpc().makePostRequest(args, response -> {
          response.notify(BeeKeeper.getScreen());

          if (!response.hasErrors()) {
            currentBranch.setConfiguration(Configuration.restore(response.getResponseAsString()));
            setBundle(null);
          }
        });
        return;
      } else {
        currentBranch.setConfiguration(new Configuration());
      }
    }
    setBundle(null);
  }

  private void setBundle(Bundle bundle) {
    searchTag = "";
    specification.getOptions().clear();
    Configuration configuration = currentBranch.getConfiguration();

    if (Objects.nonNull(template)) {
      Bundle templateBundle = template.getBundle();
      Integer price = BeeUtils.toIntOrNull(configuration.getBundlePrice(templateBundle));

      if (Objects.isNull(price)) {
        BeeKeeper.getScreen().notifySevere(Localized.dictionary().keyNotFound(templateBundle));
        specification.setBundle(null, null);
      } else {
        specification.setBundle(templateBundle, price);
        Multimap<Dimension, Option> allOptions = getAvailableOptions();

        for (Option option : template.getOptions()) {
          toggleOption(allOptions, option, true, true);
        }
      }
      template = null;
    } else {
      specification.setBundle(bundle, BeeUtils.toIntOrNull(configuration.getBundlePrice(bundle)));
    }
    refresh();
  }

  private void toggleOption(Multimap<Dimension, Option> allOptions, Option option, boolean on,
      boolean silent) {
    Map<Option, Boolean> toggle = new TreeMap<>();

    for (Option opt : specification.getOptions()) {
      toggle.put(opt, null);
    }
    ConfirmationCallback confirmationCallback = new ConfirmationCallback() {
      @Override
      public void onCancel() {
        refresh();
      }

      @Override
      public void onConfirm() {
        for (Option opt : toggle.keySet()) {
          Boolean action = toggle.get(opt);

          if (action != null) {
            if (action) {
              specification.addOption(opt, normPrice(opt));
            } else {
              specification.getOptions().remove(opt);
            }
          }
        }
        if (!silent) {
          refresh();
        }
      }
    };
    try {
      Set<Option> defaults = new HashSet<>();

      for (Option opt : allOptions.values()) {
        if (!opt.getDimension().isRequired()
            && currentBranch.getConfiguration().isDefault(opt, specification.getBundle())) {
          defaults.add(opt);
          toggle.put(opt, null);
        }
      }
      collectRestrictions(allOptions, option, on, toggle);
      toggle.keySet().removeAll(defaults);

      List<String> msgs = new ArrayList<>();
      Dimension dimension = option.getDimension();

      if (!silent) {
        for (Option opt : toggle.keySet()) {
          Boolean action = toggle.get(opt);

          if (action != null && !Objects.equals(opt, option)
              && (!dimension.isRequired() || !Objects.equals(dimension, opt.getDimension()))
              && !Objects.equals(specification.getOptions().contains(opt), action)) {

            msgs.add(BeeUtils.joinWords(action
                ? "<span style=\"font-family:" + FontAwesome.class.getSimpleName()
                + "; color:green;\">" + FontAwesome.PLUS_CIRCLE.getCode() + "</span>"
                : "<span style=\"font-family:" + FontAwesome.class.getSimpleName()
                + "; color:red;\">" + FontAwesome.BAN.getCode() + "</span>", opt));
          }
        }
      }
      if (BeeUtils.isEmpty(msgs)) {
        confirmationCallback.onConfirm();
      } else {
        Global.confirm(option.toString(), Icon.QUESTION, msgs, confirmationCallback);
      }
    } catch (BeeException e) {
      if (!silent) {
        Global.showError(option.toString(), Collections.singletonList(e.getMessage()));
        confirmationCallback.onCancel();
      }
    }
  }
}
