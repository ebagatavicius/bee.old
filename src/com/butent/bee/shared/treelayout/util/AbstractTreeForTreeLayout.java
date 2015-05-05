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

package com.butent.bee.shared.treelayout.util;

import com.google.common.collect.Lists;

import com.butent.bee.shared.treelayout.TreeForTreeLayout;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

/**
 * Provides an easy way to implement the {@link TreeForTreeLayout} interface by defining just two
 * simple methods and a constructor.
 * <p>
 * To use this class the underlying tree must provide the children as a list (see
 * {@link #getChildrenList(Object)} and give direct access to the parent of a node (see
 * {@link #getParent(Object)}).
 * <p>
 * 
 * See also {@link DefaultTreeForTreeLayout}.
 * 
 * @author Udo Borkowski (ub@abego.org)
 * 
 * @param <TreeNode>
 */
public abstract class AbstractTreeForTreeLayout<T> implements TreeForTreeLayout<T> {

  /**
   * Returns the parent of a node, if it has one.
   * <p>
   * Time Complexity: O(1)
   * 
   * @param node
   * @return [nullable] the parent of the node, or null when the node is a root.
   */
  public abstract T getParent(T node);

  private final T root;

  public AbstractTreeForTreeLayout(T root) {
    this.root = root;
  }

  @Override
  public T getRoot() {
    return root;
  }

  public boolean isRoot(T node) {
    return node != null && node.equals(root);
  }

  @Override
  public boolean isLeaf(T node) {
    return getChildren(node).isEmpty();
  }

  @Override
  public boolean isChildOfParent(T node, T parentNode) {
    return getParent(node) == parentNode;
  }

  @Override
  public abstract List<T> getChildren(T node);

  @Override
  public List<T> getChildrenReverse(T node) {
    return Lists.reverse(getChildren(node));
  }

  @Override
  public T getFirstChild(T parentNode) {
    return getChildren(parentNode).get(0);
  }

  @Override
  public T getLastChild(T parentNode) {
    return BeeUtils.getLast(getChildren(parentNode));
  }

  @Override
  public abstract int getLevel(T node);
}