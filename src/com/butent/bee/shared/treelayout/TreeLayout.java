/*
 * [The "BSD license"] Copyright (c) 2011, abego Software GmbH, Germany (http://www.abego.org) All
 * rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. 3. Neither the name of the abego Software
 * GmbH nor the names of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.butent.bee.shared.treelayout;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.treelayout.Configuration.AlignmentInLevel;
import com.butent.bee.shared.treelayout.Configuration.Location;
import com.butent.bee.shared.utils.BeeUtils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implements the actual tree layout algorithm.
 * <p>
 * The nodes with their final layout can be retrieved through {@link #getNodeBounds()}.
 * <p>
 * See <a href="package-summary.html">this summary</a> to get an overview how to use TreeLayout.
 * 
 * 
 * @author Udo Borkowski (ub@abego.org)
 * 
 * @param <T>
 */
public class TreeLayout<T> {
  /*
   * Differences between this implementation and original algorithm
   * --------------------------------------------------------------
   * 
   * For easier reference the same names (or at least similar names) as in the paper of Buchheim,
   * J&uuml;nger, and Leipert are used in this implementation. However in the external interface
   * "first" and "last" are used instead of "left most" and "right most". The implementation also
   * supports tree layouts with the root at the left (or right) side. In that case using "left most"
   * would refer to the "top" child, i.e. using "first" is less confusing.
   * 
   * Also the y coordinate is not the level but directly refers the y coordinate of a level, taking
   * node's height and gapBetweenLevels into account. When the root is at the left or right side the
   * y coordinate actually becomes an x coordinate.
   * 
   * Instead of just using a constant "distance" to calculate the position to the next node we refer
   * to the "size" (width or height) of the node and a "gapBetweenNodes".
   */

  private static final BeeLogger logger = LogUtils.getLogger(TreeLayout.class);

  // ------------------------------------------------------------------------
  // tree

  private final TreeForTreeLayout<T> tree;

  /**
   * Returns the Tree the layout is created for.
   */
  public TreeForTreeLayout<T> getTree() {
    return tree;
  }

  // ------------------------------------------------------------------------
  // nodeExtentProvider

  private final NodeExtentProvider<T> nodeExtentProvider;

  /**
   * Returns the {@link NodeExtentProvider} used by this {@link TreeLayout}.
   */
  public NodeExtentProvider<T> getNodeExtentProvider() {
    return nodeExtentProvider;
  }

  private double getNodeHeight(T node) {
    return nodeExtentProvider.getHeight(node);
  }

  private double getNodeWidth(T node) {
    return nodeExtentProvider.getWidth(node);
  }

  private double getWidthOrHeightOfNode(T treeNode, boolean returnWidth) {
    return returnWidth ? getNodeWidth(treeNode) : getNodeHeight(treeNode);
  }

  /**
   * When the level changes in Y-axis (i.e. root location Top or Bottom) the height of a node is its
   * thickness, otherwise the node's width is its thickness.
   * <p>
   * The thickness of a node is used when calculating the locations of the levels.
   * 
   * @param treeNode
   * @return
   */
  private double getNodeThickness(T treeNode) {
    return getWidthOrHeightOfNode(treeNode, !isLevelChangeInYAxis());
  }

  /**
   * When the level changes in Y-axis (i.e. root location Top or Bottom) the width of a node is its
   * size, otherwise the node's height is its size.
   * <p>
   * The size of a node is used when calculating the distance between two nodes.
   * 
   * @param treeNode
   * @return
   */
  private double getNodeSize(T treeNode) {
    return getWidthOrHeightOfNode(treeNode, isLevelChangeInYAxis());
  }

  // ------------------------------------------------------------------------
  // configuration

  private final Configuration<T> configuration;

  /**
   * Returns the Configuration used by this {@link TreeLayout}.
   */
  public Configuration<T> getConfiguration() {
    return configuration;
  }

  private boolean isLevelChangeInYAxis() {
    Location rootLocation = configuration.getRootLocation();
    return rootLocation == Location.TOP || rootLocation == Location.BOTTOM;
  }

  private int getLevelChangeSign() {
    Location rootLocation = configuration.getRootLocation();
    return rootLocation == Location.BOTTOM
        || rootLocation == Location.RIGHT ? -1 : 1;
  }

  // ------------------------------------------------------------------------
  // bounds

  private double boundsLeft = Double.MAX_VALUE;
  private double boundsRight = Double.MIN_VALUE;
  private double boundsTop = Double.MAX_VALUE;
  private double boundsBottom = Double.MIN_VALUE;

  private void updateBounds(T node, double centerX, double centerY) {
    double width = getNodeWidth(node);
    double height = getNodeHeight(node);
    double left = centerX - width / 2;
    double right = centerX + width / 2;
    double top = centerY - height / 2;
    double bottom = centerY + height / 2;
    if (boundsLeft > left) {
      boundsLeft = left;
    }
    if (boundsRight < right) {
      boundsRight = right;
    }
    if (boundsTop > top) {
      boundsTop = top;
    }
    if (boundsBottom < bottom) {
      boundsBottom = bottom;
    }
  }

  /**
   * Returns the bounds of the tree layout.
   * <p>
   * The bounds of a TreeLayout is the smallest rectangle containing the bounds of all nodes in the
   * layout. It always starts at (0,0).
   * 
   * @return the bounds of the tree layout
   */
  public Rectangle2D getBounds() {
    return new Rectangle2D.Double(0, 0, boundsRight - boundsLeft, boundsBottom - boundsTop);
  }

  // ------------------------------------------------------------------------
  // size of level

  private final List<Double> sizeOfLevel = new ArrayList<>();

  private void calcSizeOfLevels(T node, int level) {
    double oldSize;
    if (sizeOfLevel.size() <= level) {
      sizeOfLevel.add(Double.valueOf(0));
      oldSize = 0;
    } else {
      oldSize = sizeOfLevel.get(level);
    }

    double size = getNodeThickness(node);
    // size = nodeExtentProvider.getHeight(node);
    if (oldSize < size) {
      sizeOfLevel.set(level, size);
    }

    if (!tree.isLeaf(node)) {
      for (T child : tree.getChildren(node)) {
        calcSizeOfLevels(child, level + 1);
      }
    }
  }

  /**
   * Returns the number of levels of the tree.
   * 
   * @return [level > 0]
   */
  public int getLevelCount() {
    return sizeOfLevel.size();
  }

  /**
   * Returns the size of a level.
   * <p>
   * When the root is located at the top or bottom the size of a level is the maximal height of the
   * nodes of that level. When the root is located at the left or right the size of a level is the
   * maximal width of the nodes of that level.
   * 
   * @param level
   * @return the size of the level [level >= 0 && level < levelCount]
   */
  public double getSizeOfLevel(int level) {
    Assert.isTrue(level >= 0, "level must be >= 0");
    Assert.isTrue(level < getLevelCount(), "level must be < levelCount");

    return sizeOfLevel.get(level);
  }

  // ------------------------------------------------------------------------
  // NormalizedPosition

  /**
   * The algorithm calculates the position starting with the root at 0. I.e. the left children will
   * get negative positions. However we want the result to be normalized to (0,0).
   * <p>
   * {@link NormalizedPosition} will normalize the position (given relative to the root position),
   * taking the current bounds into account. This way the left most node bounds will start at x = 0,
   * the top most node bounds at y = 0.
   */
  private class NormalizedPosition extends Point2D {
    private double xRelativeToRoot;
    private double yRelativeToRoot;

    public NormalizedPosition(double xRelativeToRoot, double yRelativeToRoot) {
      setLocation(xRelativeToRoot, yRelativeToRoot);
    }

    @Override
    public double getX() {
      return xRelativeToRoot - boundsLeft;
    }

    @Override
    public double getY() {
      return yRelativeToRoot - boundsTop;
    }

    @Override
    // never called from outside
    public void setLocation(double xr, double yr) {
      this.xRelativeToRoot = xr;
      this.yRelativeToRoot = yr;
    }
  }

  // ------------------------------------------------------------------------
  // The Algorithm

  private final boolean useIdentity;

  private final Map<T, Double> mod;
  private final Map<T, T> thread;
  private final Map<T, Double> prelim;
  private final Map<T, Double> change;
  private final Map<T, Double> shift;
  private final Map<T, T> ancestor;
  private final Map<T, Integer> number;
  private final Map<T, Point2D> positions;

  private double getMod(T node) {
    Double d = mod.get(node);
    return d != null ? d.doubleValue() : 0;
  }

  private void setMod(T node, double d) {
    mod.put(node, d);
  }

  private T getThread(T node) {
    T n = thread.get(node);
    return n != null ? n : null;
  }

  private void setThread(T node, T thr) {
    this.thread.put(node, thr);
  }

  private T getAncestor(T node) {
    T n = ancestor.get(node);
    return n != null ? n : node;
  }

  private void setAncestor(T node, T anc) {
    this.ancestor.put(node, anc);
  }

  private double getPrelim(T node) {
    Double d = prelim.get(node);
    return d != null ? d.doubleValue() : 0;
  }

  private void setPrelim(T node, double d) {
    prelim.put(node, d);
  }

  private double getChange(T node) {
    Double d = change.get(node);
    return d != null ? d.doubleValue() : 0;
  }

  private void setChange(T node, double d) {
    change.put(node, d);
  }

  private double getShift(T node) {
    Double d = shift.get(node);
    return d != null ? d.doubleValue() : 0;
  }

  private void setShift(T node, double d) {
    shift.put(node, d);
  }

  /**
   * The distance of two nodes is the distance of the centers of both noded.
   * <p>
   * I.e. the distance includes the gap between the nodes and half of the sizes of the nodes.
   * 
   * @param v
   * @param w
   * @return the distance between node v and w
   */
  private double getDistance(T v, T w) {
    double sizeOfNodes = getNodeSize(v) + getNodeSize(w);

    double distance = sizeOfNodes / 2 + configuration.getGapBetweenNodes(v, w);
    return distance;
  }

  private T nextLeft(T v) {
    return tree.isLeaf(v) ? getThread(v) : tree.getFirstChild(v);
  }

  private T nextRight(T v) {
    return tree.isLeaf(v) ? getThread(v) : tree.getLastChild(v);
  }

  /**
   * 
   * @param node [tree.isChildOfParent(node, parentNode)]
   * @param parentNode parent of node
   * @return
   */
  private int getNumber(T node, T parentNode) {
    Integer n = number.get(node);
    if (n == null) {
      int i = 1;
      for (T child : tree.getChildren(parentNode)) {
        number.put(child, i++);
      }
      n = number.get(node);
    }

    return n.intValue();
  }

  /**
   * 
   * @param vIMinus
   * @param v
   * @param parentOfV
   * @param defaultAncestor
   * @return the greatest distinct ancestor of vIMinus and its right neighbor v
   */
  private T ancestor(T vIMinus, T v, T parentOfV, T defaultAncestor) {
    T anc = getAncestor(vIMinus);

    // when the ancestor of vIMinus is a sibling of v (i.e. has the same
    // parent as v) it is also the greatest distinct ancestor vIMinus and
    // v. Otherwise it is the defaultAncestor

    return tree.isChildOfParent(anc, parentOfV) ? anc : defaultAncestor;
  }

  private void moveSubtree(T wMinus, T wPlus, T parent, double sh) {
    int subtrees = getNumber(wPlus, parent) - getNumber(wMinus, parent);
    setChange(wPlus, getChange(wPlus) - sh / subtrees);
    setShift(wPlus, getShift(wPlus) + sh);
    setChange(wMinus, getChange(wMinus) + sh / subtrees);
    setPrelim(wPlus, getPrelim(wPlus) + sh);
    setMod(wPlus, getMod(wPlus) + sh);
  }

  /**
   * In difference to the original algorithm we also pass in the leftSibling and the parent of v.
   * <p>
   * <b>Why adding the parameter 'parent of v' (parentOfV) ?</b>
   * <p>
   * In this method we need access to the parent of v. Not every tree implementation may support
   * efficient (i.e. constant time) access to it. On the other hand the (only) caller of this method
   * can provide this information with only constant extra time.
   * <p>
   * Also we need access to the "left most sibling" of v. Not every tree implementation may support
   * efficient (i.e. constant time) access to it. On the other hand the "left most sibling" of v is
   * also the "first child" of the parent of v. The first child of a parent node we can get in
   * constant time. As we got the parent of v we can so also get the "left most sibling" of v in
   * constant time.
   * <p>
   * <b>Why adding the parameter 'leftSibling' ?</b>
   * <p>
   * In this method we need access to the "left sibling" of v. Not every tree implementation may
   * support efficient (i.e. constant time) access to it. However it is easy for the caller of this
   * method to provide this information with only constant extra time.
   * <p>
   * <p>
   * <p>
   * In addition these extra parameters avoid the need for {@link TreeForTreeLayout} to include
   * extra methods "getParent", "getLeftSibling", or "getLeftMostSibling". This keeps the interface
   * {@link TreeForTreeLayout} small and avoids redundant implementations.
   * 
   * @param v
   * @param defaultAncestor
   * @param leftSibling [nullable] the left sibling v, if there is any
   * @param parentOfV the parent of v
   * @return the (possibly changes) defaultAncestor
   */
  private T apportion(T v, T defaultAncestor, T leftSibling, T parentOfV) {
    T w = leftSibling;
    if (w == null) {
      // v has no left sibling
      return defaultAncestor;
    }
    // v has left sibling w

    // The following variables "v..." are used to traverse the contours to
    // the subtrees. "Minus" refers to the left, "Plus" to the right
    // subtree. "I" refers to the "inside" and "O" to the outside contour.
    T vOPlus = v;
    T vIPlus = v;
    T vIMinus = w;
    // get leftmost sibling of vIPlus, i.e. get the leftmost sibling of
    // v, i.e. the leftmost child of the parent of v (which is passed
    // in)
    T vOMinus = tree.getFirstChild(parentOfV);

    Double sIPlus = getMod(vIPlus);
    Double sOPlus = getMod(vOPlus);
    Double sIMinus = getMod(vIMinus);
    Double sOMinus = getMod(vOMinus);

    T nextRightVIMinus = nextRight(vIMinus);
    T nextLeftVIPlus = nextLeft(vIPlus);

    while (nextRightVIMinus != null && nextLeftVIPlus != null) {
      vIMinus = nextRightVIMinus;
      vIPlus = nextLeftVIPlus;
      vOMinus = nextLeft(vOMinus);
      vOPlus = nextRight(vOPlus);
      setAncestor(vOPlus, v);
      double sh = (getPrelim(vIMinus) + sIMinus)
          - (getPrelim(vIPlus) + sIPlus)
          + getDistance(vIMinus, vIPlus);

      if (sh > 0) {
        moveSubtree(ancestor(vIMinus, v, parentOfV, defaultAncestor), v, parentOfV, sh);
        sIPlus = sIPlus + sh;
        sOPlus = sOPlus + sh;
      }
      sIMinus = sIMinus + getMod(vIMinus);
      sIPlus = sIPlus + getMod(vIPlus);
      sOMinus = sOMinus + getMod(vOMinus);
      sOPlus = sOPlus + getMod(vOPlus);

      nextRightVIMinus = nextRight(vIMinus);
      nextLeftVIPlus = nextLeft(vIPlus);
    }

    if (nextRightVIMinus != null && nextRight(vOPlus) == null) {
      setThread(vOPlus, nextRightVIMinus);
      setMod(vOPlus, getMod(vOPlus) + sIMinus - sOPlus);
    }

    if (nextLeftVIPlus != null && nextLeft(vOMinus) == null) {
      setThread(vOMinus, nextLeftVIPlus);
      setMod(vOMinus, getMod(vOMinus) + sIPlus - sOMinus);
      return v;
    }
    return defaultAncestor;
  }

  /**
   * 
   * @param v [!tree.isLeaf(v)]
   */
  private void executeShifts(T v) {
    double sh = 0;
    double ch = 0;

    for (T w : tree.getChildrenReverse(v)) {
      ch = ch + getChange(w);
      setPrelim(w, getPrelim(w) + sh);
      setMod(w, getMod(w) + sh);
      sh = sh + getShift(w) + ch;
    }
  }

  /**
   * In difference to the original algorithm we also pass in the leftSibling (see
   * {@link #apportion(Object, Object, Object, Object)} for a motivation).
   * 
   * @param v
   * @param leftSibling [nullable] the left sibling v, if there is any
   */
  private void firstWalk(T v, T leftSibling) {
    if (tree.isLeaf(v)) {
      // No need to set prelim(v) to 0 as the getter takes care of this.

      T w = leftSibling;
      if (w != null) {
        // v has left sibling

        setPrelim(v, getPrelim(w) + getDistance(v, w));
      }

    } else {
      // v is not a leaf

      T defaultAncestor = tree.getFirstChild(v);
      T previousChild = null;
      for (T w : tree.getChildren(v)) {
        firstWalk(w, previousChild);
        defaultAncestor = apportion(w, defaultAncestor, previousChild, v);
        previousChild = w;
      }
      executeShifts(v);
      double midpoint = (getPrelim(tree.getFirstChild(v)) + getPrelim(tree
          .getLastChild(v))) / 2.0;
      T w = leftSibling;
      if (w != null) {
        // v has left sibling

        setPrelim(v, getPrelim(w) + getDistance(v, w));
        setMod(v, getPrelim(v) - midpoint);

      } else {
        // v has no left sibling

        setPrelim(v, midpoint);
      }
    }
  }

  /**
   * In difference to the original algorithm we also pass in extra level information.
   * 
   * @param v
   * @param m
   * @param level
   * @param levelStart
   */
  private void secondWalk(T v, double m, int level, double levelStart) {
    // construct the position from the prelim and the level information

    // The rootLocation affects the way how x and y are changed and in what
    // direction.
    double levelChangeSign = getLevelChangeSign();
    boolean levelChangeOnYAxis = isLevelChangeInYAxis();
    double levelSize = getSizeOfLevel(level);

    double x = getPrelim(v) + m;

    double y;
    AlignmentInLevel alignment = configuration.getAlignmentInLevel();
    if (alignment == AlignmentInLevel.CENTER) {
      y = levelStart + levelChangeSign * (levelSize / 2);
    } else if (alignment == AlignmentInLevel.TOWARDS_ROOT) {
      y = levelStart + levelChangeSign * (getNodeThickness(v) / 2);
    } else {
      y = levelStart + levelSize - levelChangeSign * (getNodeThickness(v) / 2);
    }

    if (!levelChangeOnYAxis) {
      double t = x;
      x = y;
      y = t;
    }

    positions.put(v, new NormalizedPosition(x, y));

    // update the bounds
    updateBounds(v, x, y);

    // recurse
    if (!tree.isLeaf(v)) {
      double nextLevelStart = levelStart
          + (levelSize + configuration.getGapBetweenLevels(level + 1))
          * levelChangeSign;
      for (T w : tree.getChildren(v)) {
        secondWalk(w, m + getMod(v), level + 1, nextLevelStart);
      }
    }
  }

  // ------------------------------------------------------------------------
  // nodeBounds

  private Map<T, Rectangle2D.Double> nodeBounds;

  /**
   * Returns the layout of the tree nodes by mapping each node of the tree to its bounds (position
   * and size).
   * <p>
   * For each rectangle x and y will be >= 0. At least one rectangle will have an x == 0 and at
   * least one rectangle will have an y == 0.
   * 
   * @return maps each node of the tree to its bounds (position and size).
   */
  public Map<T, Rectangle2D.Double> getNodeBounds() {
    if (nodeBounds == null) {
      if (this.useIdentity) {
        nodeBounds = new IdentityHashMap<>();
      } else {
        nodeBounds = new HashMap<>();
      }

      for (Entry<T, Point2D> entry : positions.entrySet()) {
        T node = entry.getKey();
        Point2D pos = entry.getValue();
        double w = getNodeWidth(node);
        double h = getNodeHeight(node);
        double x = pos.getX() - w / 2;
        double y = pos.getY() - h / 2;
        nodeBounds.put(node, new Rectangle2D.Double(x, y, w, h));
      }
    }
    return nodeBounds;
  }

  // ------------------------------------------------------------------------
  // constructor

  /**
   * Creates a TreeLayout for a given tree.
   * <p>
   * In addition to the tree the {@link NodeExtentProvider} and the {@link Configuration} must be
   * given.
   * 
   * @param useIdentity [default: false] when true, identity ("==") is used instead of equality
   *          ("equals(...)") when checking nodes. Within a tree each node must only be once (using
   *          this check).
   */
  public TreeLayout(TreeForTreeLayout<T> tree,
      NodeExtentProvider<T> nodeExtentProvider,
      Configuration<T> configuration, boolean useIdentity) {

    this.tree = tree;
    this.nodeExtentProvider = nodeExtentProvider;
    this.configuration = configuration;
    this.useIdentity = useIdentity;

    if (this.useIdentity) {
      this.mod = new IdentityHashMap<>();
      this.thread = new IdentityHashMap<>();
      this.prelim = new IdentityHashMap<>();
      this.change = new IdentityHashMap<>();
      this.shift = new IdentityHashMap<>();
      this.ancestor = new IdentityHashMap<>();
      this.number = new IdentityHashMap<>();
      this.positions = new IdentityHashMap<>();
    } else {
      this.mod = new HashMap<>();
      this.thread = new HashMap<>();
      this.prelim = new HashMap<>();
      this.change = new HashMap<>();
      this.shift = new HashMap<>();
      this.ancestor = new HashMap<>();
      this.number = new HashMap<>();
      this.positions = new HashMap<>();
    }

    // No need to explicitly set mod, thread and ancestor as their getters
    // are taking care of the initial values. This avoids a full tree walk
    // through and saves some memory as no entries are added for
    // "initial values".

    T r = tree.getRoot();
    firstWalk(r, null);
    calcSizeOfLevels(r, 0);
    secondWalk(r, -getPrelim(r), 0, 0);
  }

  public TreeLayout(TreeForTreeLayout<T> tree, NodeExtentProvider<T> nodeExtentProvider,
      Configuration<T> configuration) {
    this(tree, nodeExtentProvider, configuration, false);
  }

  // ------------------------------------------------------------------------
  // checkTree

  private void addUniqueNodes(Map<T, T> nodes, T newNode) {
    if (nodes.put(newNode, newNode) != null) {
      throw new RuntimeException(BeeUtils.joinWords("Node used more than once in tree:", newNode));
    }
    for (T n : tree.getChildren(newNode)) {
      addUniqueNodes(nodes, n);
    }
  }

  /**
   * Check if the tree is a "valid" tree.
   * <p>
   * Typically you will use this method during development when you get an unexpected layout from
   * your trees.
   * <p>
   * The following checks are performed:
   * <ul>
   * <li>Each node must only occur once in the tree.</li>
   * </ul>
   */
  public void checkTree() {
    Map<T, T> nodes;
    if (this.useIdentity) {
      nodes = new IdentityHashMap<>();
    } else {
      nodes = new HashMap<>();
    }

    // Traverse the tree and check if each node is only used once.
    addUniqueNodes(nodes, tree.getRoot());
  }

  // ------------------------------------------------------------------------
  // dumpTree

  private void dumpTree(T node, int indent, DumpConfiguration dumpConfiguration) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indent; i++) {
      sb.append(dumpConfiguration.indent);
    }

    if (dumpConfiguration.includeObjectToString) {
      sb.append("[");
      sb.append(node.getClass().getName() + "@"
          + Integer.toHexString(node.hashCode()));
      if (node.hashCode() != System.identityHashCode(node)) {
        sb.append("/identityHashCode:");
        sb.append(Integer.toHexString(System.identityHashCode(node)));
      }
      sb.append("]");
    }

    sb.append(node != null ? node.toString() : BeeConst.NULL);

    if (dumpConfiguration.includeNodeSize) {
      sb.append(" (size: ");
      sb.append(getNodeWidth(node));
      sb.append("x");
      sb.append(getNodeHeight(node));
      sb.append(")");
    }

    logger.debug(sb.toString());

    for (T n : tree.getChildren(node)) {
      dumpTree(n, indent + 1, dumpConfiguration);
    }
  }

  public static class DumpConfiguration {
    /**
     * The text used to indent the output per level.
     */
    private final String indent;
    /**
     * When true the dump also includes the size of each node, otherwise not.
     */
    private final boolean includeNodeSize;
    /**
     * When true, the text as returned by {@link Object#toString()}, is included in the dump, in
     * addition to the text returned by the possibly overridden toString method of the node. When
     * the hashCode method is overridden the output will also include the "identityHashCode".
     */
    private final boolean includeObjectToString;

    /**
     * 
     * @param indent [default: "    "]
     * @param includeNodeSize [default: false]
     * @param includePointer [default: false]
     */
    public DumpConfiguration(String indent, boolean includeNodeSize, boolean includePointer) {
      this.indent = indent;
      this.includeNodeSize = includeNodeSize;
      this.includeObjectToString = includePointer;
    }

    public DumpConfiguration() {
      this("    ", false, false);
    }
  }

  /**
   * Prints a dump of the tree to the given printStream, using the node's "toString" method.
   * 
   * @param printStream
   * @param dumpConfiguration [default: new DumpConfiguration()]
   */
  public void dumpTree(DumpConfiguration dumpConfiguration) {
    dumpTree(tree.getRoot(), 0, dumpConfiguration);
  }

  public void dumpTree() {
    dumpTree(new DumpConfiguration());
  }
}
