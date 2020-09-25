package org.hisp.dhis.webapi.documentation.controller;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.apache.http.HttpStatus;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.hisp.dhis.mapping.ImageFormat;
import org.hisp.dhis.mapping.MapLayerPosition;
import org.hisp.dhis.mapping.MapService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionGroupSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.program.*;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.hisp.dhis.webapi.documentation.common.ResponseDocumentation;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.program.ProgramIndicator.KEY_PROGRAM_VARIABLE;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public abstract class AbstractWebApiTest<T extends IdentifiableObject>
    extends DhisWebSpringTest
{
    protected Class<T> testClass;

    protected Schema schema;

    protected int createdStatus = HttpStatus.SC_CREATED;

    protected int updateStatus = HttpStatus.SC_OK;

    protected int deleteStatus = HttpStatus.SC_OK;

    @Override
    @SuppressWarnings( "unchecked" )
    public void setUpTest()
    {
        testClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        schema = schemaService.getSchema( testClass );
        setStatues();
    }

    protected void setStatues()
    {
    }

    @Test
    public void testGetAll() throws Exception
    {
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaultObjectMap = manager.getDefaults();
        IdentifiableObject defaultTestObject = defaultObjectMap.get( testClass );
        int valueToTest = defaultTestObject != null ? 5 : 4;

        manager.save( createTestObject( testClass, 'A' ) );
        manager.save( createTestObject( testClass, 'B' ) );
        manager.save( createTestObject( testClass, 'C' ) );
        manager.save( createTestObject( testClass, 'D' ) );
        MockHttpSession session = getSession( "ALL" );

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
        fieldDescriptors.addAll( ResponseDocumentation.pager() );
        fieldDescriptors.add( fieldWithPath( schema.getPlural() ).description( schema.getPlural() ) );

        mvc.perform( get( schema.getRelativeApiEndpoint() ).session( session ).accept( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$." + schema.getPlural() ).isArray() )
            .andExpect( jsonPath( "$." + schema.getPlural() + ".length()" ).value( valueToTest ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/all",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) )
            ) );
    }

    @Test
    public void testGetByIdOk() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        T object = createTestObject( testClass, 'A' );
        manager.save( object );
        Set<FieldDescriptor> fieldDescriptors = TestUtils.getFieldDescriptors( schema );

        mvc.perform( get( schema.getRelativeApiEndpoint() + "/{id}", object.getUid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.name" ).value( object.getName() ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/id",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) ) );
    }

    @Test
    public void testCreate() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        T object = createTestObject( testClass, 'A' );

        Set<FieldDescriptor> fieldDescriptors = TestUtils.getFieldDescriptors( schema );

        mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( TestUtils.convertObjectToJsonBytes( object ) ) )
            .andExpect( status().is( createdStatus ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/create",
                requestFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) )
            );
    }

    @Test
    public void testUpdate() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        T object = createTestObject( testClass, 'A' );
        manager.save( object );

        object.setHref( "updatedHref" );

        mvc.perform( put( schema.getRelativeApiEndpoint() + "/" + object.getUid() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( TestUtils.convertObjectToJsonBytes( object ) ) )
            .andExpect( status().is( updateStatus ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/update" ) );

    }

    @Test
    public void testDeleteByIdOk() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        T object = createTestObject( testClass, 'A' );
        manager.save( object );

        mvc.perform( delete( schema.getRelativeApiEndpoint() + "/{id}", object.getUid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().is( deleteStatus ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/delete" ) );

    }

    @Test
    public void testDeleteById404() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( schema.getRelativeApiEndpoint() + "/{id}", "deabcdefghA" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isNotFound() );
    }

    @SuppressWarnings( "unchecked" )
    protected T createTestObject( Class<?> clazz, char uniqueName, Object... params )
    {
        if ( DataElementGroup.class.isAssignableFrom( clazz ) )
        {
            return (T) createDataElementGroup( uniqueName );
        }
        else if ( CategoryCombo.class.isAssignableFrom( clazz ) )
        {
            return (T) createCategoryCombo( uniqueName, Arrays.copyOf( params, params.length, Category[].class ) );
        }
        else if ( CategoryOption.class.isAssignableFrom( clazz ) )
        {
            return (T) createCategoryOption( uniqueName );
        }
        else if ( DataElement.class.isAssignableFrom( clazz ) )
        {
            return (T) createDataElement( uniqueName );
        }
        else if ( Category.class.isAssignableFrom( clazz ) )
        {
            return (T) createCategory( uniqueName, Arrays.copyOf( params, params.length, CategoryOption[].class ) );
        }
        else if ( Program.class.isAssignableFrom( clazz ) )
        {
            OrganisationUnit organisationUnitA = createOrganisationUnit( uniqueName );
            manager.save( organisationUnitA );
            return (T) createProgram( uniqueName, new HashSet<>(), organisationUnitA );
        }
        else if ( DataElementOperand.class.isAssignableFrom( clazz ) )
        {
            DataElement deA = createDataElement( uniqueName );
            manager.save( deA );

            CategoryCombo cc = createCategoryCombo( uniqueName );
            CategoryOption co = createCategoryOption( uniqueName );
            manager.save( cc );
            manager.save( co );

            CategoryOptionCombo coc = createCategoryOptionCombo( cc, co );
            manager.save( coc );

            return (T) new DataElementOperand( deA, coc );
        }
        else if ( DataElementGroupSet.class.isAssignableFrom( clazz ) )
        {
            return (T) createDataElementGroupSet( uniqueName );
        }
        else if ( CategoryOptionCombo.class.isAssignableFrom( clazz ) )
        {
            CategoryCombo cc = createCategoryCombo( uniqueName );
            CategoryOption co = createCategoryOption( uniqueName );
            manager.save( cc );
            manager.save( co );

            return (T) createCategoryOptionCombo( cc, co );
        }
        else if ( CategoryOptionGroup.class.isAssignableFrom( clazz ) )
        {
            CategoryOption co = createCategoryOption( uniqueName );
            manager.save( co );
            return (T) createCategoryOptionGroup( uniqueName, co );
        }
        else if ( CategoryOptionGroupSet.class.isAssignableFrom( clazz ) )
        {
            return (T) createCategoryOptionGroupSet( uniqueName );
        }
        else if ( EventChart.class.isAssignableFrom( clazz ) )
        {
            Program prA = createProgram( uniqueName );
            manager.save( prA );

            EventChart ecA = new EventChart( "evc" + uniqueName );
            ecA.setProgram( prA );
            ecA.setType( ChartType.COLUMN );

            return (T) ecA;
        }
        else if ( EventReport.class.isAssignableFrom( clazz ) )
        {
            Program prA = createProgram( uniqueName );
            manager.save( prA );
            EventReport erA = new EventReport( "er" + uniqueName );
            erA.setProgram( prA );

            return (T) erA;
        }
        else if ( ProgramDataElementDimensionItem.class.isAssignableFrom( clazz ) )
        {
            Program prA = createProgram( uniqueName );
            manager.save( prA );

            DataElement deA = createDataElement( uniqueName );
            manager.save( deA );

            return (T) new ProgramDataElementDimensionItem( prA, deA );
        }
        else if ( ProgramIndicator.class.isAssignableFrom( clazz ) )
        {
            Program program = (Program) createTestObject( Program.class, uniqueName );
            manager.save( program );

            Constant constantA = createConstant( uniqueName, 7.0 );
            manager.save( constantA );

            String expressionA = "( " + KEY_PROGRAM_VARIABLE + "{" + ProgramIndicator.VAR_ENROLLMENT_DATE + "} - " + KEY_PROGRAM_VARIABLE + "{"
                    + ProgramIndicator.VAR_INCIDENT_DATE + "} )  / " + ProgramIndicator.KEY_CONSTANT + "{" + constantA.getUid() + "}";

            return (T) createProgramIndicator( uniqueName, program, expressionA, null );
        }
        else if ( Indicator.class.isAssignableFrom( clazz ) )
        {
            IndicatorType indicatorType = createIndicatorType( uniqueName );
            manager.save( indicatorType );
            return (T) createIndicator( uniqueName, indicatorType );
        }
        else if ( IndicatorGroup.class.isAssignableFrom( clazz ) )
        {
            return (T) createIndicatorGroup( uniqueName );
        }
        else if ( IndicatorGroupSet.class.isAssignableFrom( clazz ) )
        {
            return (T) createIndicatorGroupSet( uniqueName );
        }
        else if ( IndicatorType.class.isAssignableFrom( clazz ) )
        {
            return (T) createIndicatorType( uniqueName );
        }
        else if ( Legend.class.isAssignableFrom( clazz ) )
        {
            return (T) createLegend( uniqueName, 0d, 10d );
        }
        else if ( LegendSet.class.isAssignableFrom( clazz ) )
        {
            return (T) createLegendSet( uniqueName );
        }
        else if ( OrganisationUnit.class.isAssignableFrom( clazz ) )
        {
            return (T) createOrganisationUnit( uniqueName );
        }
        else if ( OrganisationUnitGroup.class.isAssignableFrom( clazz ) )
        {
            return (T) createOrganisationUnitGroup( uniqueName );
        }
        else if ( OrganisationUnitGroupSet.class.isAssignableFrom( clazz ) )
        {
            return (T) createOrganisationUnitGroupSet( uniqueName );
        }
        else if ( OrganisationUnitLevel.class.isAssignableFrom( clazz ) )
        {
            return (T) new OrganisationUnitLevel( uniqueName, "OrgLevel" + uniqueName );
        }
        else if ( org.hisp.dhis.mapping.Map.class.isAssignableFrom( clazz ) )
        {
            org.hisp.dhis.mapping.Map map = new org.hisp.dhis.mapping.Map();
            map.setName( "Map" + uniqueName );
            map.setDisplayName( "DisplayName" + uniqueName );
            map.setLatitude( 952175.62553525 );
            map.setLongitude( -1378543.6774686 );
            return (T) map;
        }
        else if ( ExternalMapLayer.class.isAssignableFrom( clazz ) )
        {
            ExternalMapLayer externalMapLayer = new ExternalMapLayer( "ExternalMapLayer" + uniqueName );
            externalMapLayer.setMapService( MapService.WMS );
            externalMapLayer.setUrl( "testUrl" );
            externalMapLayer.setImageFormat( ImageFormat.JPG );
            externalMapLayer.setMapLayerPosition( MapLayerPosition.BASEMAP );
            return (T) externalMapLayer;
        }
        else if ( OptionGroup.class.isAssignableFrom( clazz ) )
        {
            OptionGroup optionGroup = new OptionGroup( "OptionGroup" + uniqueName );
            optionGroup.setShortName( "Group" + uniqueName );
            return (T) optionGroup;
        }
        else if ( OptionGroupSet.class.isAssignableFrom( clazz ) )
        {
            return (T) new OptionGroupSet( "OptionGroupSet" + uniqueName );
        }
        else if ( Option.class.isAssignableFrom( clazz ) )
        {
            return (T) new Option( "Option" + uniqueName, "code" + uniqueName );
        }
        else if ( OptionSet.class.isAssignableFrom( clazz ))
        {
            return (T) new OptionSet( "OptionSet" +uniqueName, ValueType.TEXT );
        }
        else if ( ProgramTrackedEntityAttributeGroup.class.isAssignableFrom( clazz ) )
        {
            ProgramTrackedEntityAttributeGroup group = createProgramTrackedEntityAttributeGroup( uniqueName );

            Program pr = createProgram( 'A' );
            TrackedEntityAttribute tea = createTrackedEntityAttribute( 'A' );
            ProgramTrackedEntityAttribute attr = createProgramTrackedEntityAttribute( pr, tea );
            group.addAttribute( attr );

            return (T) group;
        }
        else if ( ProgramTrackedEntityAttribute.class.isAssignableFrom( clazz ))
        {
            Program pr = createProgram( 'A' );
            TrackedEntityAttribute tea = createTrackedEntityAttribute( 'A' );
            return (T) createProgramTrackedEntityAttribute( pr, tea );
        }
        else if ( ProgramDataElementDimensionItem.class.isAssignableFrom( clazz ) )
        {
            return (T) createProgramDataElement( uniqueName );
        }

        return null;
    }

}
