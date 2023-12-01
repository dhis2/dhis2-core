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

import org.hisp.dhis.node.AbstractNodeTest;
import org.hisp.dhis.schema.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ComplexNode}.
 *
 * @author Volker Schmidt
 */
class ComplexNodeTest {

  @Test
  void createSingleChild() {
    final Property property = new Property(AbstractNodeTest.TestClass.class);
    property.setName("tests");
    property.setNamespace("testUri");
    final SimpleNode simpleNode = new SimpleNode("id", "My Test");
    final ComplexNode testNode = new ComplexNode(property, simpleNode);
    Assertions.assertEquals("tests", testNode.getName());
    Assertions.assertEquals("testUri", testNode.getNamespace());
    Assertions.assertEquals(AbstractNodeTest.TestClass.class, testNode.getProperty().getKlass());
    Assertions.assertEquals(1, testNode.getUnorderedChildren().size());
    Assertions.assertSame(simpleNode, testNode.getUnorderedChildren().get(0));
  }
}
