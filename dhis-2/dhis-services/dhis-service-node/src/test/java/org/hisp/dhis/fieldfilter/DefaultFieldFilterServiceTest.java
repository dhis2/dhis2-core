package org.hisp.dhis.fieldfilter;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeTransformer;
import org.hisp.dhis.node.transformers.PluckNodeTransformer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.DefaultSchemaService;
import org.hisp.dhis.schema.Jackson2PropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link DefaultFieldFilterService}.
 *
 * @author Volker Schmidt
 */
public class DefaultFieldFilterServiceTest
{
    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private AclService aclService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AttributeService attributeService;

    private DefaultFieldFilterService service;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        final Set<NodeTransformer> nodeTransformers = new HashSet<>();
        nodeTransformers.add( new PluckNodeTransformer() );

        final SchemaService schemaService = new DefaultSchemaService( new Jackson2PropertyIntrospectorService()
        {
            @Override
            protected Map<String, Property> getPropertiesFromHibernate( Class<?> klass )
            {
                return Collections.emptyMap();
            }
        }, sessionFactory );
        service = new DefaultFieldFilterService( new DefaultFieldParser(), schemaService, aclService, currentUserService, attributeService, nodeTransformers );
        service.init();
    }

    @Test
    public void baseIdentifiableIdOnly()
    {
        final OrganisationUnit ou1 = new OrganisationUnit();
        ou1.setUid( "abc1" );

        final OrganisationUnit ou2 = new OrganisationUnit();
        ou2.setUid( "abc2" );

        final CategoryOption option = new CategoryOption();
        option.setUid( "def1" );
        option.getOrganisationUnits().add( ou1 );
        option.getOrganisationUnits().add( ou2 );

        final FieldFilterParams params = new FieldFilterParams( Collections.singletonList( option ), Arrays.asList( "id", "organisationUnits" ) );
        final ComplexNode node = service.toComplexNode( params );

        Assert.assertEquals( "categoryOption", node.getName() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "id" ) instanceof SimpleNode );
        Assert.assertEquals( "def1", ( (SimpleNode) getNamedNode( node.getUnorderedChildren(), "id" ) ).getValue() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "organisationUnits" ) instanceof CollectionNode );

        final CollectionNode collectionNode = (CollectionNode) getNamedNode( node.getUnorderedChildren(), "organisationUnits" );
        Assert.assertEquals( 2, collectionNode.getUnorderedChildren().size() );
        final List<String> ouIds = new ArrayList<>();

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 0 ) instanceof ComplexNode );
        ComplexNode complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "organisationUnit", complexNode.getName() );
        Assert.assertEquals( 1, complexNode.getUnorderedChildren().size() );
        Assert.assertTrue( complexNode.getUnorderedChildren().get( 0 ) instanceof SimpleNode );
        SimpleNode simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "id", simpleNode.getName() );
        ouIds.add( String.valueOf( simpleNode.getValue() ) );

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 1 ) instanceof ComplexNode );
        complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get( 1 );
        Assert.assertEquals( "organisationUnit", complexNode.getName() );
        Assert.assertEquals( 1, complexNode.getUnorderedChildren().size() );
        Assert.assertTrue( complexNode.getUnorderedChildren().get( 0 ) instanceof SimpleNode );
        simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "id", simpleNode.getName() );
        ouIds.add( String.valueOf( simpleNode.getValue() ) );

        assertThat( ouIds, Matchers.containsInAnyOrder( "abc1", "abc2" ) );
    }

    @Test
    public void baseIdentifiableName()
    {
        final OrganisationUnit ou1 = new OrganisationUnit();
        ou1.setUid( "abc1" );
        ou1.setName( "OU 1" );

        final OrganisationUnit ou2 = new OrganisationUnit();
        ou2.setUid( "abc2" );
        ou2.setName( "OU 2" );

        final CategoryOption option = new CategoryOption();
        option.setUid( "def1" );
        option.getOrganisationUnits().add( ou1 );
        option.getOrganisationUnits().add( ou2 );

        final FieldFilterParams params = new FieldFilterParams( Collections.singletonList( option ), Arrays.asList( "id", "organisationUnits~pluck(name)[id,name]" ) );
        final ComplexNode node = service.toComplexNode( params );

        Assert.assertEquals( "categoryOption", node.getName() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "id" ) instanceof SimpleNode );
        Assert.assertEquals( "def1", ( (SimpleNode) getNamedNode( node.getUnorderedChildren(), "id" ) ).getValue() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "organisationUnits" ) instanceof CollectionNode );

        final CollectionNode collectionNode = (CollectionNode) getNamedNode( node.getUnorderedChildren(), "organisationUnits" );
        Assert.assertEquals( 2, collectionNode.getUnorderedChildren().size() );
        final List<String> ouIds = new ArrayList<>();

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 0 ) instanceof SimpleNode );
        SimpleNode simpleNode = (SimpleNode) collectionNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "name", simpleNode.getName() );
        ouIds.add( String.valueOf( simpleNode.getValue() ) );

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 1 ) instanceof SimpleNode );
        simpleNode = (SimpleNode) collectionNode.getUnorderedChildren().get( 1 );
        Assert.assertEquals( "name", simpleNode.getName() );
        ouIds.add( String.valueOf( simpleNode.getValue() ) );

        assertThat( ouIds, Matchers.containsInAnyOrder( "OU 1", "OU 2" ) );
    }

    @Test
    public void defaultClass()
    {
        final CategoryOption co1 = new CategoryOption();
        co1.setUid( "abc1" );

        final CategoryOption co2 = new CategoryOption();
        co2.setUid( "abc2" );
        co2.setName( "default" );

        final CategoryOption co3 = new CategoryOption();
        co3.setUid( "abc3" );

        final Category category = new Category();
        category.setUid( "def1" );
        category.getCategoryOptions().add( co1 );
        category.getCategoryOptions().add( co2 );
        category.getCategoryOptions().add( co3 );

        final FieldFilterParams params = new FieldFilterParams( Collections.singletonList( category ), Arrays.asList( "id", "categoryOptions" ) );
        params.setDefaults( Defaults.EXCLUDE );

        final ComplexNode node = service.toComplexNode( params );

        Assert.assertEquals( "category", node.getName() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "id" ) instanceof SimpleNode );
        Assert.assertEquals( "def1", ( (SimpleNode) getNamedNode( node.getUnorderedChildren(), "id" ) ).getValue() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "categoryOptions" ) instanceof CollectionNode );

        final CollectionNode collectionNode = (CollectionNode) getNamedNode( node.getUnorderedChildren(), "categoryOptions" );
        Assert.assertEquals( 2, collectionNode.getUnorderedChildren().size() );
        final List<String> coIds = new ArrayList<>();

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 0 ) instanceof ComplexNode );
        ComplexNode complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "categoryOption", complexNode.getName() );
        Assert.assertEquals( 1, complexNode.getUnorderedChildren().size() );
        Assert.assertTrue( complexNode.getUnorderedChildren().get( 0 ) instanceof SimpleNode );
        SimpleNode simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "id", simpleNode.getName() );
        coIds.add( String.valueOf( simpleNode.getValue() ) );

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 1 ) instanceof ComplexNode );
        complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get( 1 );
        Assert.assertEquals( "categoryOption", complexNode.getName() );
        Assert.assertEquals( 1, complexNode.getUnorderedChildren().size() );
        Assert.assertTrue( complexNode.getUnorderedChildren().get( 0 ) instanceof SimpleNode );
        simpleNode = (SimpleNode) complexNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "id", simpleNode.getName() );
        coIds.add( String.valueOf( simpleNode.getValue() ) );

        assertThat( coIds, Matchers.containsInAnyOrder( "abc1", "abc3" ) );
    }

    @Test
    public void baseIdentifiable()
    {
        final OrganisationUnit ou1 = new OrganisationUnit();
        ou1.setUid( "abc1" );
        ou1.setName( "Test 1" );

        final OrganisationUnit ou2 = new OrganisationUnit();
        ou2.setUid( "abc2" );
        ou2.setName( "Test 2" );

        final CategoryOption option = new CategoryOption();
        option.setUid( "def1" );
        option.getOrganisationUnits().add( ou1 );
        option.getOrganisationUnits().add( ou2 );

        final FieldFilterParams params = new FieldFilterParams( Collections.singletonList( option ), Arrays.asList( "id", "organisationUnits[id,name]" ) );
        final ComplexNode node = service.toComplexNode( params );

        Assert.assertEquals( "categoryOption", node.getName() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "id" ) instanceof SimpleNode );
        Assert.assertEquals( "def1", ( (SimpleNode) getNamedNode( node.getUnorderedChildren(), "id" ) ).getValue() );
        Assert.assertTrue( getNamedNode( node.getUnorderedChildren(), "organisationUnits" ) instanceof CollectionNode );

        final CollectionNode collectionNode = (CollectionNode) getNamedNode( node.getUnorderedChildren(), "organisationUnits" );
        Assert.assertEquals( 2, collectionNode.getUnorderedChildren().size() );
        final List<String> ouIds = new ArrayList<>();
        final List<String> ouNames = new ArrayList<>();

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 0 ) instanceof ComplexNode );
        ComplexNode complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get( 0 );
        Assert.assertEquals( "organisationUnit", complexNode.getName() );
        Assert.assertEquals( 2, complexNode.getUnorderedChildren().size() );
        Assert.assertTrue( getNamedNode( complexNode.getUnorderedChildren(), "id" ) instanceof SimpleNode );
        SimpleNode simpleNode = (SimpleNode) getNamedNode( complexNode.getUnorderedChildren(), "id" );
        Assert.assertEquals( "id", simpleNode.getName() );
        ouIds.add( String.valueOf( simpleNode.getValue() ) );
        Assert.assertTrue( getNamedNode( complexNode.getUnorderedChildren(), "name" ) instanceof SimpleNode );
        simpleNode = (SimpleNode) getNamedNode( complexNode.getUnorderedChildren(), "name" );
        Assert.assertEquals( "name", simpleNode.getName() );
        ouNames.add( String.valueOf( simpleNode.getValue() ) );

        Assert.assertTrue( collectionNode.getUnorderedChildren().get( 1 ) instanceof ComplexNode );
        complexNode = (ComplexNode) collectionNode.getUnorderedChildren().get( 1 );
        Assert.assertEquals( "organisationUnit", complexNode.getName() );
        Assert.assertEquals( 2, complexNode.getUnorderedChildren().size() );
        Assert.assertTrue( getNamedNode( complexNode.getUnorderedChildren(), "id" ) instanceof SimpleNode );
        simpleNode = (SimpleNode) getNamedNode( complexNode.getUnorderedChildren(), "id" );
        Assert.assertEquals( "id", simpleNode.getName() );
        ouIds.add( String.valueOf( simpleNode.getValue() ) );
        Assert.assertTrue( getNamedNode( complexNode.getUnorderedChildren(), "name" ) instanceof SimpleNode );
        simpleNode = (SimpleNode) getNamedNode( complexNode.getUnorderedChildren(), "name" );
        Assert.assertEquals( "name", simpleNode.getName() );
        ouNames.add( String.valueOf( simpleNode.getValue() ) );

        assertThat( ouIds, Matchers.containsInAnyOrder( "abc1", "abc2" ) );
        assertThat( ouNames, Matchers.containsInAnyOrder( "Test 1", "Test 2" ) );
    }

    private Node getNamedNode( @Nonnull Collection<? extends Node> nodes, @Nonnull String name )
    {
        return nodes.stream().filter( n -> name.equals( n.getName() ) ).findFirst().orElse( null );
    }
}
