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
package org.hisp.dhis.node.transformers;

import java.util.Collections;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.schema.DefaultPropertyIntrospectorService;
import org.hisp.dhis.schema.DefaultSchemaService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.introspection.JacksonPropertyIntrospector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PluckNodeTransformer}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class PluckNodeTransformerTest {
  private final PluckNodeTransformer transformer = new PluckNodeTransformer();

  @Mock private SessionFactory sessionFactory;

  private SchemaService schemaService;

  private CollectionNode collectionNode;

  @BeforeEach
  public void setUp() {
    schemaService =
        new DefaultSchemaService(
            new DefaultPropertyIntrospectorService(new JacksonPropertyIntrospector()),
            sessionFactory);

    collectionNode = new CollectionNode("organisationUnits", 2);
    collectionNode.setNamespace("testUrn");
    collectionNode.setProperty(
        schemaService.getDynamicSchema(CategoryOption.class).getProperty("organisationUnits"));

    ComplexNode complexNode = new ComplexNode("organisationUnit");
    SimpleNode simpleNode =
        new SimpleNode(
            "id", schemaService.getDynamicSchema(Category.class).getProperty("id"), "abc1");
    complexNode.addChild(simpleNode);
    simpleNode =
        new SimpleNode(
            "name", schemaService.getDynamicSchema(Category.class).getProperty("id"), "OU 1");
    complexNode.addChild(simpleNode);
    collectionNode.addChild(complexNode);

    complexNode = new ComplexNode("organisationUnit");
    simpleNode =
        new SimpleNode(
            "id", schemaService.getDynamicSchema(Category.class).getProperty("id"), "abc2");
    complexNode.addChild(simpleNode);
    simpleNode =
        new SimpleNode(
            "name", schemaService.getDynamicSchema(Category.class).getProperty("id"), "OU 2");
    complexNode.addChild(simpleNode);
    collectionNode.addChild(complexNode);
  }

  @Test
  void name() {
    Assertions.assertEquals("pluck", transformer.name());
  }

  @Test
  void withoutArg() {
    Node result = transformer.transform(collectionNode, null);
    Assertions.assertTrue(result instanceof CollectionNode);

    CollectionNode collection = (CollectionNode) result;
    Assertions.assertEquals("organisationUnits", collection.getName());
    Assertions.assertEquals("testUrn", collection.getNamespace());
    Assertions.assertEquals(2, collection.getUnorderedChildren().size());

    Assertions.assertEquals("id", collection.getUnorderedChildren().get(0).getName());
    Assertions.assertTrue(collection.getUnorderedChildren().get(0) instanceof SimpleNode);
    Assertions.assertEquals(
        "abc1", ((SimpleNode) collection.getUnorderedChildren().get(0)).getValue());

    Assertions.assertEquals("id", collection.getUnorderedChildren().get(1).getName());
    Assertions.assertTrue(collection.getUnorderedChildren().get(1) instanceof SimpleNode);
    Assertions.assertEquals(
        "abc2", ((SimpleNode) collection.getUnorderedChildren().get(1)).getValue());
  }

  @Test
  void withArg() {
    Node result = transformer.transform(collectionNode, Collections.singletonList("name"));
    Assertions.assertTrue(result instanceof CollectionNode);

    CollectionNode collection = (CollectionNode) result;
    Assertions.assertEquals("organisationUnits", collection.getName());
    Assertions.assertEquals("testUrn", collection.getNamespace());
    Assertions.assertEquals(2, collection.getUnorderedChildren().size());

    Assertions.assertEquals("name", collection.getUnorderedChildren().get(0).getName());
    Assertions.assertTrue(collection.getUnorderedChildren().get(0) instanceof SimpleNode);
    Assertions.assertEquals(
        "OU 1", ((SimpleNode) collection.getUnorderedChildren().get(0)).getValue());

    Assertions.assertEquals("name", collection.getUnorderedChildren().get(1).getName());
    Assertions.assertTrue(collection.getUnorderedChildren().get(1) instanceof SimpleNode);
    Assertions.assertEquals(
        "OU 2", ((SimpleNode) collection.getUnorderedChildren().get(1)).getValue());
  }
}
