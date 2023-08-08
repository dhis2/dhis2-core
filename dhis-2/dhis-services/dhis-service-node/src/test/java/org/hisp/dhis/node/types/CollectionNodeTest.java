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
package org.hisp.dhis.node.types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CollectionNodeTest}.
 *
 * @author Volker Schmidt
 */
class CollectionNodeTest {

  @Test
  void createEmpty() {
    final CollectionNode collectionNode = new CollectionNode("tests", 0);
    assertEquals("tests", collectionNode.getName());
    assertEquals(0, collectionNode.getUnorderedChildren().size());
  }

  @Test
  void createNonEmpty() {
    final CollectionNode collectionNode = new CollectionNode("tests", 10);
    assertEquals("tests", collectionNode.getName());
    assertEquals(0, collectionNode.getUnorderedChildren().size());
  }

  @Test
  void getChildren() {
    final CollectionNode collectionNode = new CollectionNode("tests", 0);
    final SimpleNode simpleNode1 =
        new SimpleNode("id", "My Test 1") {

          @Override
          public int getOrder() {
            return 10;
          }
        };
    final SimpleNode simpleNode2 =
        new SimpleNode("id", "My Test 2") {

          @Override
          public int getOrder() {
            return 5;
          }
        };
    final SimpleNode simpleNode3 =
        new SimpleNode("id", "My Test 3") {

          @Override
          public int getOrder() {
            return 15;
          }
        };
    collectionNode.addChild(simpleNode1);
    collectionNode.addChild(simpleNode2);
    collectionNode.addChild(simpleNode3);
    assertThat(
        collectionNode.getChildren(), Matchers.contains(simpleNode2, simpleNode1, simpleNode3));
  }

  @Test
  void getEmptyChildren() {
    final CollectionNode collectionNode = new CollectionNode("tests", 0);
    assertEquals(0, collectionNode.getChildren().size());
  }

  @Test
  void getSingleChildren() {
    final CollectionNode collectionNode = new CollectionNode("tests", 0);
    final SimpleNode simpleNode1 = new SimpleNode("id", "My Test 1");
    collectionNode.addChild(simpleNode1);
    assertThat(collectionNode.getChildren(), Matchers.contains(simpleNode1));
  }
}
