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
package org.hisp.dhis.fieldfilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.NoOpCache;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeTransformer;
import org.hisp.dhis.node.transformers.PluckNodeTransformer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.DefaultPropertyIntrospectorService;
import org.hisp.dhis.schema.DefaultSchemaService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.introspection.JacksonPropertyIntrospector;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultFieldFilterService}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class DefaultFieldFilterServiceTest {
  @Mock private SessionFactory sessionFactory;

  @Mock private AclService aclService;

  @Mock private CurrentUserService currentUserService;

  @Mock private AttributeService attributeService;

  @Mock private UserGroupService userGroupService;

  @Mock private UserService userService;

  private DefaultFieldFilterService service;

  @BeforeEach
  public void setUp() {
    final Set<NodeTransformer> nodeTransformers = new HashSet<>();
    nodeTransformers.add(new PluckNodeTransformer());

    final SchemaService schemaService =
        new DefaultSchemaService(
            new DefaultPropertyIntrospectorService(new JacksonPropertyIntrospector()),
            sessionFactory);

    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.createPropertyTransformerCache()).thenReturn(new NoOpCache<>());
    service =
        new DefaultFieldFilterService(
            new DefaultFieldParser(),
            schemaService,
            aclService,
            currentUserService,
            attributeService,
            cacheProvider,
            userGroupService,
            userService,
            nodeTransformers);
    service.init();
  }

  @Test
  void baseIdentifiableIdOnly() {
    final OrganisationUnit ou1 = new OrganisationUnit();
    ou1.setUid("abc1");

    final OrganisationUnit ou2 = new OrganisationUnit();
    ou2.setUid("abc2");

    final CategoryOption option = new CategoryOption();
    option.setUid("def1");
    option.getOrganisationUnits().add(ou1);
    option.getOrganisationUnits().add(ou2);

    final FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(option), Arrays.asList("id", "organisationUnits"));
    final ComplexNode node = service.toComplexNode(params);

    Assertions.assertEquals("categoryOption", node.getName());
    Assertions.assertTrue(getNamedNode(node.getUnorderedChildren(), "id") instanceof SimpleNode);
    Assertions.assertEquals(
        "def1", ((SimpleNode) getNamedNode(node.getUnorderedChildren(), "id")).getValue());
    Assertions.assertTrue(
        getNamedNode(node.getUnorderedChildren(), "organisationUnits") instanceof CollectionNode);

    final CollectionNode collectionNode =
        (CollectionNode) getNamedNode(node.getUnorderedChildren(), "organisationUnits");
    Assertions.assertEquals(2, collectionNode.getUnorderedChildren().size());
    final List<String> ouIds = new ArrayList<>();

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(0) instanceof ComplexNode);
    ComplexNode complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("organisationUnit", complexNode.getName());
    Assertions.assertEquals(1, complexNode.getUnorderedChildren().size());
    Assertions.assertTrue(complexNode.getUnorderedChildren().get(0) instanceof SimpleNode);
    SimpleNode simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("id", simpleNode.getName());
    ouIds.add(String.valueOf(simpleNode.getValue()));

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(1) instanceof ComplexNode);
    complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get(1);
    Assertions.assertEquals("organisationUnit", complexNode.getName());
    Assertions.assertEquals(1, complexNode.getUnorderedChildren().size());
    Assertions.assertTrue(complexNode.getUnorderedChildren().get(0) instanceof SimpleNode);
    simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("id", simpleNode.getName());
    ouIds.add(String.valueOf(simpleNode.getValue()));

    assertThat(ouIds, Matchers.containsInAnyOrder("abc1", "abc2"));
  }

  @Test
  void baseIdentifiableName() {
    final OrganisationUnit ou1 = new OrganisationUnit();
    ou1.setUid("abc1");
    ou1.setName("OU 1");

    final OrganisationUnit ou2 = new OrganisationUnit();
    ou2.setUid("abc2");
    ou2.setName("OU 2");

    final CategoryOption option = new CategoryOption();
    option.setUid("def1");
    option.getOrganisationUnits().add(ou1);
    option.getOrganisationUnits().add(ou2);

    final FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(option),
            Arrays.asList("id", "organisationUnits~pluck(name)[id,name]"));
    final ComplexNode node = service.toComplexNode(params);

    Assertions.assertEquals("categoryOption", node.getName());
    Assertions.assertTrue(getNamedNode(node.getUnorderedChildren(), "id") instanceof SimpleNode);
    Assertions.assertEquals(
        "def1", ((SimpleNode) getNamedNode(node.getUnorderedChildren(), "id")).getValue());
    Assertions.assertTrue(
        getNamedNode(node.getUnorderedChildren(), "organisationUnits") instanceof CollectionNode);

    final CollectionNode collectionNode =
        (CollectionNode) getNamedNode(node.getUnorderedChildren(), "organisationUnits");
    Assertions.assertEquals(2, collectionNode.getUnorderedChildren().size());
    final List<String> ouIds = new ArrayList<>();

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(0) instanceof SimpleNode);
    SimpleNode simpleNode = (SimpleNode) collectionNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("name", simpleNode.getName());
    ouIds.add(String.valueOf(simpleNode.getValue()));

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(1) instanceof SimpleNode);
    simpleNode = (SimpleNode) collectionNode.getUnorderedChildren().get(1);
    Assertions.assertEquals("name", simpleNode.getName());
    ouIds.add(String.valueOf(simpleNode.getValue()));

    assertThat(ouIds, Matchers.containsInAnyOrder("OU 1", "OU 2"));
  }

  @Test
  void defaultClass() {
    final CategoryOption co1 = new CategoryOption();
    co1.setUid("abc1");

    final CategoryOption co2 = new CategoryOption();
    co2.setUid("abc2");
    co2.setName("default");

    final CategoryOption co3 = new CategoryOption();
    co3.setUid("abc3");

    final Category category = new Category();
    category.setUid("def1");
    category.getCategoryOptions().add(co1);
    category.getCategoryOptions().add(co2);
    category.getCategoryOptions().add(co3);

    final FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(category), Arrays.asList("id", "categoryOptions"));
    params.setDefaults(Defaults.EXCLUDE);

    final ComplexNode node = service.toComplexNode(params);

    Assertions.assertEquals("category", node.getName());
    Assertions.assertTrue(getNamedNode(node.getUnorderedChildren(), "id") instanceof SimpleNode);
    Assertions.assertEquals(
        "def1", ((SimpleNode) getNamedNode(node.getUnorderedChildren(), "id")).getValue());
    Assertions.assertTrue(
        getNamedNode(node.getUnorderedChildren(), "categoryOptions") instanceof CollectionNode);

    final CollectionNode collectionNode =
        (CollectionNode) getNamedNode(node.getUnorderedChildren(), "categoryOptions");
    Assertions.assertEquals(2, collectionNode.getUnorderedChildren().size());
    final List<String> coIds = new ArrayList<>();

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(0) instanceof ComplexNode);
    ComplexNode complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("categoryOption", complexNode.getName());
    Assertions.assertEquals(1, complexNode.getUnorderedChildren().size());
    Assertions.assertTrue(complexNode.getUnorderedChildren().get(0) instanceof SimpleNode);
    SimpleNode simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("id", simpleNode.getName());
    coIds.add(String.valueOf(simpleNode.getValue()));

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(1) instanceof ComplexNode);
    complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get(1);
    Assertions.assertEquals("categoryOption", complexNode.getName());
    Assertions.assertEquals(1, complexNode.getUnorderedChildren().size());
    Assertions.assertTrue(complexNode.getUnorderedChildren().get(0) instanceof SimpleNode);
    simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("id", simpleNode.getName());
    coIds.add(String.valueOf(simpleNode.getValue()));

    assertThat(coIds, Matchers.containsInAnyOrder("abc1", "abc3"));
  }

  @Test
  void baseIdentifiable() {
    final OrganisationUnit ou1 = new OrganisationUnit();
    ou1.setUid("abc1");
    ou1.setName("Test 1");

    final OrganisationUnit ou2 = new OrganisationUnit();
    ou2.setUid("abc2");
    ou2.setName("Test 2");

    final CategoryOption option = new CategoryOption();
    option.setUid("def1");
    option.getOrganisationUnits().add(ou1);
    option.getOrganisationUnits().add(ou2);

    final FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(option), Arrays.asList("id", "organisationUnits[id,name]"));
    final ComplexNode node = service.toComplexNode(params);

    Assertions.assertEquals("categoryOption", node.getName());
    Assertions.assertTrue(getNamedNode(node.getUnorderedChildren(), "id") instanceof SimpleNode);
    Assertions.assertEquals(
        "def1", ((SimpleNode) getNamedNode(node.getUnorderedChildren(), "id")).getValue());
    Assertions.assertTrue(
        getNamedNode(node.getUnorderedChildren(), "organisationUnits") instanceof CollectionNode);

    final CollectionNode collectionNode =
        (CollectionNode) getNamedNode(node.getUnorderedChildren(), "organisationUnits");
    Assertions.assertEquals(2, collectionNode.getUnorderedChildren().size());
    final List<String> ouIds = new ArrayList<>();
    final List<String> ouNames = new ArrayList<>();

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(0) instanceof ComplexNode);
    ComplexNode complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get(0);
    Assertions.assertEquals("organisationUnit", complexNode.getName());
    Assertions.assertEquals(2, complexNode.getUnorderedChildren().size());
    Assertions.assertTrue(
        getNamedNode(complexNode.getUnorderedChildren(), "id") instanceof SimpleNode);
    SimpleNode simpleNode = (SimpleNode) getNamedNode(complexNode.getUnorderedChildren(), "id");
    Assertions.assertEquals("id", simpleNode.getName());
    ouIds.add(String.valueOf(simpleNode.getValue()));
    Assertions.assertTrue(
        getNamedNode(complexNode.getUnorderedChildren(), "name") instanceof SimpleNode);
    simpleNode = (SimpleNode) getNamedNode(complexNode.getUnorderedChildren(), "name");
    Assertions.assertEquals("name", simpleNode.getName());
    ouNames.add(String.valueOf(simpleNode.getValue()));

    Assertions.assertTrue(collectionNode.getUnorderedChildren().get(1) instanceof ComplexNode);
    complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get(1);
    Assertions.assertEquals("organisationUnit", complexNode.getName());
    Assertions.assertEquals(2, complexNode.getUnorderedChildren().size());
    Assertions.assertTrue(
        getNamedNode(complexNode.getUnorderedChildren(), "id") instanceof SimpleNode);
    simpleNode = (SimpleNode) getNamedNode(complexNode.getUnorderedChildren(), "id");
    Assertions.assertEquals("id", simpleNode.getName());
    ouIds.add(String.valueOf(simpleNode.getValue()));
    Assertions.assertTrue(
        getNamedNode(complexNode.getUnorderedChildren(), "name") instanceof SimpleNode);
    simpleNode = (SimpleNode) getNamedNode(complexNode.getUnorderedChildren(), "name");
    Assertions.assertEquals("name", simpleNode.getName());
    ouNames.add(String.valueOf(simpleNode.getValue()));

    assertThat(ouIds, Matchers.containsInAnyOrder("abc1", "abc2"));
    assertThat(ouNames, Matchers.containsInAnyOrder("Test 1", "Test 2"));
  }

  private Node getNamedNode(@Nonnull Collection<? extends Node> nodes, @Nonnull String name) {
    return nodes.stream().filter(n -> name.equals(n.getName())).findFirst().orElse(null);
  }
}
