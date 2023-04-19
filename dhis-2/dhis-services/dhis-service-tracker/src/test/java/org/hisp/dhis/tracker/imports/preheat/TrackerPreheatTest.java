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
package org.hisp.dhis.tracker.imports.preheat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerPreheatTest extends DhisConvenienceTest
{

    private TrackerPreheat preheat;

    @BeforeEach
    void setUp()
    {
        preheat = new TrackerPreheat();
    }

    @Test
    void testAllEmpty()
    {
        assertTrue( preheat.isEmpty() );
        assertTrue( preheat.getAll( Program.class ).isEmpty() );
    }

    @Test
    void testPreheatCategoryOptionCombo()
    {

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        aoc.setCode( "ABC" );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .build();
        preheat.setIdSchemes( identifierParams );

        assertFalse( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        Set<MetadataIdentifier> optionIds = categoryOptionIds( identifierParams, options );
        assertEquals( MetadataIdentifier.EMPTY_CODE,
            preheat.getCategoryOptionComboIdentifier( categoryCombo, optionIds ) );
        assertNull( preheat.getCategoryOptionCombo( categoryCombo, options ) );
        assertNull( preheat.getCategoryOptionCombo( "ABC" ) );

        preheat.putCategoryOptionCombo( categoryCombo, options, aoc );

        assertTrue( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( identifierParams.toMetadataIdentifier( aoc ),
            preheat.getCategoryOptionComboIdentifier( categoryCombo, optionIds ) );
        assertEquals( aoc, preheat.getCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( aoc, preheat.getCategoryOptionCombo( "ABC" ),
            "option combo should also be stored in the preheat map" );
    }

    @Test
    void testPreheatCategoryOptionCombosAllowNullValues()
    {

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        TrackerIdSchemeParams identifiers = new TrackerIdSchemeParams();
        preheat.setIdSchemes( identifiers );

        preheat.putCategoryOptionCombo( categoryCombo, options, null );

        assertTrue( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        Set<MetadataIdentifier> optionIds = categoryOptionIds( identifiers, options );
        assertEquals( MetadataIdentifier.EMPTY_UID,
            preheat.getCategoryOptionComboIdentifier( categoryCombo, optionIds ) );
        assertNull( preheat.getCategoryOptionCombo( categoryCombo, options ) );
        assertNull( preheat.getCategoryOptionCombo( aoc.getUid() ),
            "option combo should not be added to preheat map if null" );
    }

    @Test
    void testPutAndGetByUid()
    {
        assertTrue( preheat.getAll( Program.class ).isEmpty() );
        assertTrue( preheat.isEmpty() );

        DataElement de1 = new DataElement( "dataElementA" );
        de1.setUid( CodeGenerator.generateUid() );
        DataElement de2 = new DataElement( "dataElementB" );
        de2.setUid( CodeGenerator.generateUid() );

        preheat.put( TrackerIdSchemeParam.UID, de1 );
        preheat.put( TrackerIdSchemeParam.UID, de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
    }

    @Test
    void testPutAndGetByCode()
    {
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setCode( "CODE1" );
        DataElement de2 = new DataElement( "dataElementB" );
        de2.setCode( "CODE2" );

        preheat.put( TrackerIdSchemeParam.CODE, de1 );
        preheat.put( TrackerIdSchemeParam.CODE, de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofCode( de1.getCode() ) ), is( de1 ) );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofCode( de2.getCode() ) ), is( de2 ) );
    }

    @Test
    void testPutAndGetByName()
    {
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setName( "DATA_ELEM1" );
        DataElement de2 = new DataElement( "dataElementB" );
        de2.setName( "DATA_ELEM2" );

        preheat.put( TrackerIdSchemeParam.NAME, de1 );
        preheat.put( TrackerIdSchemeParam.NAME, de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofName( de1.getName() ) ), is( de1 ) );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofName( de2.getName() ) ), is( de2 ) );
    }

    @Test
    void testPutAndGetByAttribute()
    {
        Attribute attribute = new Attribute();
        attribute.setAutoFields();
        AttributeValue attributeValue = new AttributeValue( "value1" );
        attributeValue.setAttribute( attribute );
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setAttributeValues( Collections.singleton( attributeValue ) );

        preheat.put(
            TrackerIdSchemeParam.ofAttribute( attribute.getUid() ),
            de1 );

        assertEquals( 1, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofUid( "value1" ) ), is( notNullValue() ) );
    }

    @Test
    void testPutAndGetDataElementByCode()
    {
        preheat.setIdSchemes( TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.UID )
            .dataElementIdScheme( TrackerIdSchemeParam.CODE )
            .build() );
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setCode( "CODE1" );
        DataElement de2 = new DataElement( "dataElementB" );
        de2.setCode( "CODE2" );

        preheat.put( de1 );
        preheat.put( de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofCode( de1.getCode() ) ), is( de1 ) );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofCode( de2.getCode() ) ), is( de2 ) );
    }

    @Test
    void testPutAndGetDataElementByName()
    {
        preheat.setIdSchemes( TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .dataElementIdScheme( TrackerIdSchemeParam.NAME )
            .build() );
        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );

        preheat.put( de1 );
        preheat.put( de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofName( de1.getName() ) ), is( de1 ) );
        assertThat( preheat.getDataElement( MetadataIdentifier.ofName( de2.getName() ) ), is( de2 ) );
    }

    @Test
    void testPutAndGetProgramByCode()
    {
        preheat.setIdSchemes( TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.UID )
            .programIdScheme( TrackerIdSchemeParam.CODE )
            .build() );
        Program p1 = new Program();
        p1.setCode( "p1" );
        Program p2 = new Program();
        p2.setCode( "p2" );

        preheat.put( p1 );
        preheat.put( p2 );

        assertEquals( 2, preheat.getAll( Program.class ).size() );
        assertThat( preheat.get( Program.class, p1.getCode() ), is( p1 ) );
        assertThat( preheat.get( Program.class, p2.getCode() ), is( p2 ) );
    }

    @Test
    void testPutAndGetProgramByName()
    {
        preheat.setIdSchemes( TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .programIdScheme( TrackerIdSchemeParam.NAME )
            .build() );
        Program p1 = new Program( "p1" );
        Program p2 = new Program( "p2" );

        preheat.put( p1 );
        preheat.put( p2 );

        assertEquals( 2, preheat.getAll( Program.class ).size() );
        assertThat( preheat.get( Program.class, p1.getName() ), is( p1 ) );
        assertThat( preheat.get( Program.class, p2.getName() ), is( p2 ) );
    }

    @Test
    void testGetByMetadataIdentifier()
    {
        Attribute attribute = new Attribute();
        attribute.setAutoFields();
        attribute.setName( "best" );
        preheat.put( TrackerIdSchemeParam.NAME, attribute );

        DataElement de1 = new DataElement( "dataElementA" );
        de1.setAttributeValues( Collections.singleton( new AttributeValue( "value1", attribute ) ) );
        preheat.put( TrackerIdSchemeParam.ofAttribute( attribute.getUid() ), de1 );

        assertEquals( attribute, preheat.get( Attribute.class, MetadataIdentifier.ofName( "best" ) ) );
        assertEquals( de1,
            preheat.getDataElement( MetadataIdentifier.ofAttribute( attribute.getUid(), "value1" ) ) );
    }

    @Test
    void testGetByMetadataIdentifierGivenNull()
    {
        assertNull( preheat.get( Attribute.class, (MetadataIdentifier) null ) );
    }

    @Test
    void testPutUid()
    {
        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );
        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();
        preheat.put( TrackerIdSchemeParam.UID, de1 );
        preheat.put( TrackerIdSchemeParam.UID, de2 );
        preheat.put( TrackerIdSchemeParam.UID, de3 );
        assertFalse( preheat.isEmpty() );
        assertEquals( de1.getUid(), preheat.getDataElement( MetadataIdentifier.ofUid( de1.getUid() ) ).getUid() );
        assertEquals( de2.getUid(), preheat.getDataElement( MetadataIdentifier.ofUid( de2.getUid() ) ).getUid() );
        assertEquals( de3.getUid(), preheat.getDataElement( MetadataIdentifier.ofUid( de3.getUid() ) ).getUid() );
    }

    @Test
    void testPutCode()
    {
        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );
        de1.setAutoFields();
        de1.setCode( "Code1" );
        de2.setAutoFields();
        de2.setCode( "Code2" );
        de3.setAutoFields();
        de3.setCode( "Code3" );
        preheat.put( TrackerIdSchemeParam.CODE, de1 );
        preheat.put( TrackerIdSchemeParam.CODE, de2 );
        preheat.put( TrackerIdSchemeParam.CODE, de3 );
        assertFalse( preheat.isEmpty() );
        assertEquals( de1.getCode(), preheat.getDataElement( MetadataIdentifier.ofCode( de1.getCode() ) ).getCode() );
        assertEquals( de2.getCode(), preheat.getDataElement( MetadataIdentifier.ofCode( de2.getCode() ) ).getCode() );
        assertEquals( de3.getCode(), preheat.getDataElement( MetadataIdentifier.ofCode( de3.getCode() ) ).getCode() );
    }

    @Test
    void testPutCollectionUid()
    {
        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );
        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();
        preheat.put( TrackerIdSchemeParam.UID, Lists.newArrayList( de1, de2, de3 ) );
        assertFalse( preheat.isEmpty() );
        assertEquals( de1.getUid(), preheat.getDataElement( MetadataIdentifier.ofUid( de1.getUid() ) ).getUid() );
        assertEquals( de2.getUid(), preheat.getDataElement( MetadataIdentifier.ofUid( de2.getUid() ) ).getUid() );
        assertEquals( de3.getUid(), preheat.getDataElement( MetadataIdentifier.ofUid( de3.getUid() ) ).getUid() );
    }

    @Test
    void testExistsTrackedEntity()
    {
        assertFalse( preheat.exists( TrackerType.TRACKED_ENTITY, "uid" ) );

        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.setUid( "uid" );
        preheat.putTrackedEntities( List.of( tei ) );

        assertTrue( preheat.exists( TrackerType.TRACKED_ENTITY, "uid" ) );
        assertTrue( preheat.exists( TrackedEntity.builder().trackedEntity( "uid" ).build() ) );
    }

    @Test
    void testExistsEnrollment()
    {
        assertFalse( preheat.exists( TrackerType.ENROLLMENT, "uid" ) );

        ProgramInstance pi = new ProgramInstance();
        pi.setUid( "uid" );
        preheat.putEnrollments( List.of( pi ) );

        assertTrue( preheat.exists( TrackerType.ENROLLMENT, "uid" ) );
    }

    @Test
    void testExistsEvent()
    {
        assertFalse( preheat.exists( TrackerType.EVENT, "uid" ) );

        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setUid( "uid" );
        preheat.putEvents( List.of( psi ) );

        assertTrue( preheat.exists( TrackerType.EVENT, "uid" ) );
    }

    @Test
    void testExistsRelationship()
    {
        assertFalse( preheat.exists( TrackerType.RELATIONSHIP, "uid" ) );

        org.hisp.dhis.relationship.Relationship relationship = new org.hisp.dhis.relationship.Relationship();
        relationship.setUid( "uid" );
        preheat.putRelationship( relationship );

        assertTrue( preheat.exists( TrackerType.RELATIONSHIP, "uid" ) );
    }

    @Test
    void testExistsFailsOnNullType()
    {
        assertThrows( NullPointerException.class, () -> preheat.exists( null, "uid" ) );
    }

    private Set<MetadataIdentifier> categoryOptionIds( TrackerIdSchemeParams params, Set<CategoryOption> options )
    {
        return options.stream()
            .map( params::toMetadataIdentifier )
            .collect( Collectors.toSet() );
    }

    private CategoryCombo categoryCombo()
    {
        char uniqueIdentifier = 'A';
        CategoryOption co1 = createCategoryOption( uniqueIdentifier );
        CategoryOption co2 = createCategoryOption( uniqueIdentifier );
        Category ca1 = createCategory( uniqueIdentifier, co1, co2 );
        CategoryOption co3 = createCategoryOption( uniqueIdentifier );
        Category ca2 = createCategory( uniqueIdentifier, co3 );
        CategoryCombo cc = createCategoryCombo( uniqueIdentifier, ca1, ca2 );
        cc.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        CategoryOptionCombo aoc1 = createCategoryOptionCombo( cc, co1, co3 );
        CategoryOptionCombo aoc2 = createCategoryOptionCombo( cc, co2, co3 );
        cc.setOptionCombos( Sets.newHashSet( aoc1, aoc2 ) );
        return cc;
    }

    private CategoryOptionCombo firstCategoryOptionCombo( CategoryCombo categoryCombo )
    {
        assertNotNull( categoryCombo.getOptionCombos() );
        assertFalse( categoryCombo.getOptionCombos().isEmpty() );

        return categoryCombo.getSortedOptionCombos().get( 0 );
    }
}
