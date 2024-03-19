/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.junit.jupiter.api.Test;

/**
 * @author Paulo Martins <martins.tuga@gmail.com>
 */
class AbstractNodeTest {

  private static final String NODE_1 = "node1";

  private static final String NODE_2 = "node2";

  @Test
  void testRootNodeEquals() {
    final RootNode rootNode1 = createRootNode(NODE_1, "propName1", "propValue1");
    final RootNode rootNode2 = createRootNode(NODE_1, "propName1", "propValue1");
    assertEquals(rootNode1, rootNode2);
  }

  @Test
  void testRootNodeNotEquals() {
    final RootNode rootNode1 = createRootNode(NODE_1, "propName1", "propValue1");
    final RootNode rootNode2 = createRootNode(NODE_1, "propName2", "propValue2");
    assertNotEquals(rootNode1, rootNode2);
  }

  private RootNode createRootNode(String nodeName, String propertyName, String propertyValue) {
    RootNode rootNode = new RootNode(createComplexNode(nodeName));
    rootNode.setDefaultNamespace("testNamespace");
    rootNode.getConfig().getProperties().put(propertyName, propertyValue);
    return rootNode;
  }

  @Test
  void testComplexNodeEquals() {
    // Instantiating object 1
    ComplexNode complexNode1 = createComplexNode(NODE_1);
    // Instantiating object 2
    ComplexNode complexNode2 = createComplexNode(NODE_1);
    assertEquals(complexNode1, complexNode2);
  }

  @Test
  void testComplexNodeNotEquals() {
    // Instantiating object 1
    ComplexNode complexNode1 = createComplexNode(NODE_1);
    // Instantiating object 2
    ComplexNode complexNode2 = createComplexNode(NODE_2);
    assertNotEquals(complexNode1, complexNode2);
  }

  private ComplexNode createComplexNode(String node1) {
    ComplexNode complexNode1;
    List<Node> children1 = new ArrayList<>();
    children1.add(new SimpleNode("id", node1));
    complexNode1 = new ComplexNode("dataElement");
    complexNode1.setMetadata(false);
    complexNode1.setChildren(children1);
    return complexNode1;
  }
}
