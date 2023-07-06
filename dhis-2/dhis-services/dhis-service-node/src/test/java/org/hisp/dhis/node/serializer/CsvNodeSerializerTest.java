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
package org.hisp.dhis.node.serializer;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.dataformat.csv.CsvWriteException;
import java.io.ByteArrayOutputStream;
import org.hisp.dhis.node.serializers.CsvNodeSerializer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.junit.jupiter.api.Test;

class CsvNodeSerializerTest {

  private final CsvNodeSerializer csvNodeSerializer = new CsvNodeSerializer();

  @Test
  void CsvFileIsWrittenWhenOnlySimpleNodesAreProvided() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    csvNodeSerializer.serialize(createCollectionWithSimpleNodes(), outputStream);

    String[] resultArray = outputStream.toString().split("\n");

    assertEquals(2, resultArray.length);
    assertEquals(
        "\"First simple node\",\"Second simple node\",\"Third simple node\"", resultArray[0]);
    assertEquals(
        "\"First value simple child\",\"Second value simple child\",\"Third value simple child\"",
        resultArray[1]);
  }

  @Test
  void CsvFileIsWrittenWhenAttributesAreProvided() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    csvNodeSerializer.serialize(createCollectionWithComplexNodes("attributes", true), outputStream);

    String[] resultArray = outputStream.toString().split("\n");

    assertEquals(2, resultArray.length);
    assertEquals(
        "\"First simple node\",\"Second simple node\",\"First attr\",\"Second attr\",\"Third attr\",\"Fourth attr\"",
        resultArray[0]);
    assertEquals(
        "\"First value simple child\",\"Second value simple child\",\"First attr value\",\"Second attr value\",\"Third attr value\",\"Fourth attr value\"",
        resultArray[1]);
  }

  @Test
  void CsvFileIsWrittenWhenSimpleNodesAreProvidedButAttributeComplexNodeIsNot() throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    csvNodeSerializer.serialize(
        createCollectionWithComplexNodes("enrollments", true), outputStream);

    String[] resultArray = outputStream.toString().split("\n");

    assertEquals(2, resultArray.length);
    assertEquals("\"First simple node\",\"Second simple node\"", resultArray[0]);
    assertEquals("\"First value simple child\",\"Second value simple child\"", resultArray[1]);
  }

  @Test
  void CsvFileIsNotWrittenWhenNoSimpleNodesNorAttributeComplexNodeAreProvided() {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Exception exception =
        assertThrows(
            CsvWriteException.class,
            () ->
                csvNodeSerializer.serialize(
                    createCollectionWithComplexNodes("enrollments", false), outputStream));

    String expectedMessage =
        "Schema specified that header line is to be written; but contains no column names";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  private RootNode createCollectionWithSimpleNodes() {
    CollectionNode collectionRootNode = new CollectionNode("Complex node");
    CollectionNode firstCollectionNode = new CollectionNode("First collection child");
    CollectionNode secondCollectionNode = new CollectionNode("Second collection child");
    secondCollectionNode.addChild(new SimpleNode("First simple node", "First value simple child"));
    secondCollectionNode.addChild(
        new SimpleNode("Second simple node", "Second value simple child"));
    secondCollectionNode.addChild(new SimpleNode("Third simple node", "Third value simple child"));

    firstCollectionNode.addChild(secondCollectionNode);
    collectionRootNode.addChild(firstCollectionNode);

    return new RootNode(collectionRootNode);
  }

  private RootNode createCollectionWithComplexNodes(
      String complexNodeName, boolean includeSimpleNodes) {
    CollectionNode collectionRootNode = new CollectionNode("Complex node");
    CollectionNode firstCollectionNode = new CollectionNode("First collection child");
    CollectionNode secondCollectionNode = new CollectionNode("Second collection child");

    ComplexNode attributesComplexNode = new ComplexNode(complexNodeName);
    ComplexNode attributeNodeChild = new ComplexNode("attribute child");
    attributeNodeChild.addChild(new SimpleNode("First attr", "First attr value"));
    attributeNodeChild.addChild(new SimpleNode("Second attr", "Second attr value"));
    attributeNodeChild.addChild(new SimpleNode("Third attr", "Third attr value"));
    attributeNodeChild.addChild(new SimpleNode("Fourth attr", "Fourth attr value"));

    attributesComplexNode.addChild(attributeNodeChild);

    if (includeSimpleNodes) {
      secondCollectionNode.addChild(
          new SimpleNode("First simple node", "First value simple child"));
      secondCollectionNode.addChild(
          new SimpleNode("Second simple node", "Second value simple child"));
    }

    secondCollectionNode.addChild(attributesComplexNode);
    firstCollectionNode.addChild(secondCollectionNode);
    collectionRootNode.addChild(firstCollectionNode);

    return new RootNode(collectionRootNode);
  }
}
