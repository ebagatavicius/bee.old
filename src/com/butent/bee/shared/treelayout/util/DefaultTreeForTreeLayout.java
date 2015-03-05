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

import com.butent.bee.shared.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a generic implementation for the {@link TreeForTreeLayout} interface, applicable to any
 * type of tree node.
 * <p>
 * It allows you to create a tree "from scratch", without creating any new class.
 * <p>
 * To create a tree you must provide the root of the tree (see
 * {@link #DefaultTreeForTreeLayout(Object)}. Then you can incrementally construct the tree by
 * adding children to the root or other nodes of the tree (see {@link #addChild(Object, Object)} and
 * {@link #addChildren(Object, Object...)}).
 * 
 * @author Udo Borkowski (ub@abego.org)
 * 
 * @param <TreeNode>
 */

public class DefaultTreeForTreeLayout<N> extends AbstractTreeForTreeLayout<N> {

  private List<N> emptyList;

  private List<N> getEmptyList() {
    if (emptyList == null) {
      emptyList = new ArrayList<>();
    }
    return emptyList;
  }

  private Map<N, List<N>> childrenMap = new HashMap<>();
  private Map<N, N> parents = new HashMap<>();

  /**
   * Creates a new instance with a given node as the root.
   * 
   * @param root the node to be used as the root.
   */
  public DefaultTreeForTreeLayout(N root) {
    super(root);
  }

  @Override
  public N getParent(N node) {
    return parents.get(node);
  }

  @Override
  public List<N> getChildren(N node) {
    List<N> result = childrenMap.get(node);
    return result == null ? getEmptyList() : result;
  }

  /**
   * 
   * @param node
   * @return true iff the node is in the tree
   */
  public boolean hasNode(N node) {
    return node == getRoot() || parents.containsKey(node);
  }

  /**
   * @param parentNode [hasNode(parentNode)]
   * @param node [!hasNode(node)]
   */
  public void addChild(N parentNode, N node) {
    Assert.isTrue(hasNode(parentNode), "parentNode is not in the tree");
    Assert.isTrue(!hasNode(node), "node is already in the tree");

    List<N> list = childrenMap.get(parentNode);
    if (list == null) {
      list = new ArrayList<>();
      childrenMap.put(parentNode, list);
    }
    list.add(node);
    parents.put(node, parentNode);
  }

  @SuppressWarnings("unchecked")
  public void addChildren(N parentNode, N... nodes) {
    for (N node : nodes) {
      addChild(parentNode, node);
    }
  }
}
