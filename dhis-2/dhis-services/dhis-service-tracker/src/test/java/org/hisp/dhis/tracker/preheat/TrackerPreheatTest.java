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
package org.hisp.dhis.tracker.preheat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerPreheatTest extends DhisConvenienceTest
{

    @Test
    void testAllEmpty()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        assertTrue( preheat.isEmpty() );
        assertTrue( preheat.getAll( Program.class ).isEmpty() );
    }

    @Test
    void testPreheatCategoryOptionComboUsingSet()
    {

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        aoc.setCode( "ABC" );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .build();
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setIdSchemes( identifierParams );

        assertFalse( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        assertNull( preheat.getCategoryOptionCombo( categoryCombo, options ) );
        assertNull( preheat.getCategoryOptionCombo( "ABC" ) );

        preheat.putCategoryOptionCombo( categoryCombo, options, aoc );

        assertTrue( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( aoc, preheat.getCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( aoc, preheat.getCategoryOptionCombo( "ABC" ),
            "option combo should also be stored in the preheat map" );
    }

    @Test
    void testPreheatCategoryOptionComboUsingString()
    {

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        TrackerPreheat preheat = new TrackerPreheat();
        TrackerIdSchemeParams identifiers = new TrackerIdSchemeParams();
        preheat.setIdSchemes( identifiers );

        String optionsString = concatCategoryOptions( identifiers.getCategoryOptionComboIdScheme(), options );
        assertFalse( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( MetadataIdentifier.ofUid( null ),
            preheat.getCategoryOptionComboIdentifier( categoryCombo, optionsString ) );
        assertNull( preheat.getCategoryOptionCombo( aoc.getUid() ) );

        preheat.putCategoryOptionCombo( categoryCombo, options, aoc );

        assertTrue( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( identifiers.toMetadataIdentifier( aoc ),
            preheat.getCategoryOptionComboIdentifier( categoryCombo, optionsString ) );
        assertEquals( aoc, preheat.getCategoryOptionCombo( aoc.getUid() ),
            "option combo should also be stored in the preheat map" );
    }

    @Test
    void testPreheatCategoryOptionComboUsingStringIsOrderIndependent()
    {

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();
        assertEquals( 2, aoc.getCategoryOptions().size() );
        Iterator<CategoryOption> it = aoc.getCategoryOptions().iterator();
        CategoryOption option1 = it.next();
        CategoryOption option2 = it.next();

        TrackerPreheat preheat = new TrackerPreheat();
        TrackerIdSchemeParams identifiers = new TrackerIdSchemeParams();
        preheat.setIdSchemes( identifiers );

        preheat.putCategoryOptionCombo( categoryCombo, options, aoc );

        assertEquals( identifiers.toMetadataIdentifier( aoc ),
            preheat.getCategoryOptionComboIdentifier( categoryCombo, option1.getUid() + ";" + option2.getUid() ) );
        assertEquals( identifiers.toMetadataIdentifier( aoc ),
            preheat.getCategoryOptionComboIdentifier( categoryCombo, option2.getUid() + ";" + option1.getUid() ) );
    }

    @Test
    void testPreheatCategoryOptionCombosAllowNullValues()
    {

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        TrackerPreheat preheat = new TrackerPreheat();
        TrackerIdSchemeParams identifiers = new TrackerIdSchemeParams();
        preheat.setIdSchemes( identifiers );

        preheat.putCategoryOptionCombo( categoryCombo, options, null );

        assertTrue( preheat.containsCategoryOptionCombo( categoryCombo, options ) );
        String optionsString = concatCategoryOptions( identifiers.getCategoryOptionComboIdScheme(), options );
        assertNull( preheat.getCategoryOptionCombo( categoryCombo, options ) );
        assertEquals( MetadataIdentifier.ofUid( null ),
            preheat.getCategoryOptionComboIdentifier( categoryCombo, optionsString ) );
        assertNull( preheat.getCategoryOptionCombo( aoc.getUid() ),
            "option combo should not be added to preheat map if null" );
    }

    @Test
    void testPutAndGetByUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();
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
        TrackerPreheat preheat = new TrackerPreheat();
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setCode( "CODE1" );
        DataElement de2 = new DataElement( "dataElementB" );
        de2.setCode( "CODE2" );

        preheat.put( TrackerIdSchemeParam.CODE, de1 );
        preheat.put( TrackerIdSchemeParam.CODE, de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.get( DataElement.class, de1.getCode() ), is( de1 ) );
        assertThat( preheat.get( DataElement.class, de2.getCode() ), is( de2 ) );
    }

    @Test
    void testPutAndGetByName()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setName( "DATA_ELEM1" );
        DataElement de2 = new DataElement( "dataElementB" );
        de2.setName( "DATA_ELEM2" );

        preheat.put( TrackerIdSchemeParam.NAME, de1 );
        preheat.put( TrackerIdSchemeParam.NAME, de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.get( DataElement.class, de1.getName() ), is( de1 ) );
        assertThat( preheat.get( DataElement.class, de2.getName() ), is( de2 ) );
    }

    @Test
    void testPutAndGetByAttribute()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        Attribute attribute = new Attribute();
        attribute.setAutoFields();
        AttributeValue attributeValue = new AttributeValue( "value1" );
        attributeValue.setAttribute( attribute );
        DataElement de1 = new DataElement( "dataElementA" );
        de1.setAttributeValues( Collections.singleton( attributeValue ) );

        preheat.put(
            TrackerIdSchemeParam.builder().idScheme( TrackerIdScheme.ATTRIBUTE ).attributeUid( attribute.getUid() )
                .build(),
            de1 );

        assertEquals( 1, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.get( DataElement.class, "value1" ), is( notNullValue() ) );
    }

    @Test
    void testPutAndGetDataElementByCode()
    {
        TrackerPreheat preheat = new TrackerPreheat();
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
        assertThat( preheat.get( DataElement.class, de1.getCode() ), is( de1 ) );
        assertThat( preheat.get( DataElement.class, de2.getCode() ), is( de2 ) );
    }

    @Test
    void testPutAndGetDataElementByName()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setIdSchemes( TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE )
            .dataElementIdScheme( TrackerIdSchemeParam.NAME )
            .build() );
        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );

        preheat.put( de1 );
        preheat.put( de2 );

        assertEquals( 2, preheat.getAll( DataElement.class ).size() );
        assertThat( preheat.get( DataElement.class, de1.getName() ), is( de1 ) );
        assertThat( preheat.get( DataElement.class, de2.getName() ), is( de2 ) );
    }

    @Test
    void testPutAndGetProgramByCode()
    {
        TrackerPreheat preheat = new TrackerPreheat();
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
        TrackerPreheat preheat = new TrackerPreheat();
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
        TrackerPreheat preheat = new TrackerPreheat();

        Attribute attribute = new Attribute();
        attribute.setAutoFields();
        attribute.setName( "best" );
        preheat.put( TrackerIdSchemeParam.builder()
            .idScheme( TrackerIdScheme.NAME )
            .build(), attribute );

        DataElement de1 = new DataElement( "dataElementA" );
        de1.setAttributeValues( Collections.singleton( new AttributeValue( "value1", attribute ) ) );
        preheat.put( TrackerIdSchemeParam.builder()
            .idScheme( TrackerIdScheme.ATTRIBUTE )
            .attributeUid( attribute.getUid() )
            .build(), de1 );

        assertEquals( attribute, preheat.get( Attribute.class, MetadataIdentifier.ofName( "best" ) ) );
        assertEquals( de1,
            preheat.get( DataElement.class, MetadataIdentifier.ofAttribute( attribute.getUid(), "value1" ) ) );
    }

    @Test
    void testGetByMetadataIdentifierGivenNull()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        assertNull( preheat.get( Attribute.class, (MetadataIdentifier) null ) );
    }

    @Test
    void testPutUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();
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
        assertEquals( de1.getUid(), preheat.get( DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    void testPutCode()
    {
        TrackerPreheat preheat = new TrackerPreheat();
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
        assertEquals( de1.getCode(), preheat.get( DataElement.class, de1.getCode() ).getCode() );
        assertEquals( de2.getCode(), preheat.get( DataElement.class, de2.getCode() ).getCode() );
        assertEquals( de3.getCode(), preheat.get( DataElement.class, de3.getCode() ).getCode() );
    }

    @Test
    void testPutCollectionUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );
        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();
        preheat.put( TrackerIdSchemeParam.UID, Lists.newArrayList( de1, de2, de3 ) );
        assertFalse( preheat.isEmpty() );
        assertEquals( de1.getUid(), preheat.get( DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    void testReferenceInvalidation()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        // Create root TEI
        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.setUid( CodeGenerator.generateUid() );
        List<TrackedEntityInstance> teiList = new ArrayList<>();
        teiList.add( tei );
        List<String> allEntities = new ArrayList<>();
        allEntities.add( CodeGenerator.generateUid() );
        preheat.putTrackedEntities( teiList, allEntities );
        // Create 2 Enrollments, where TEI is parent
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.generateUid() );
        List<ProgramInstance> psList = new ArrayList<>();
        psList.add( programInstance );
        List<Enrollment> allPs = new ArrayList<>();
        allPs.add( new Enrollment()
        {

            {
                String uid = CodeGenerator.generateUid();
                setEnrollment( uid );
                setTrackedEntity( allEntities.get( 0 ) );
            }
        } );
        allPs.add( new Enrollment()
        {

            {
                String uid = CodeGenerator.generateUid();
                setEnrollment( uid );
                setTrackedEntity( allEntities.get( 0 ) );
            }
        } );
        preheat.putEnrollments( psList, allPs );
        // Create 4 Enrollments, where TEI is parent
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setUid( CodeGenerator.generateUid() );
        List<ProgramStageInstance> psiList = new ArrayList<>();
        psiList.add( psi );
        List<Event> allEvents = new ArrayList<>();
        allEvents.add( new Event()
        {

            {
                String uid = CodeGenerator.generateUid();
                setEvent( uid );
                setEnrollment( allPs.get( 0 ).getEnrollment() );
            }
        } );
        allEvents.add( new Event()
        {

            {
                String uid = CodeGenerator.generateUid();
                setEvent( uid );
                setEnrollment( allPs.get( 0 ).getEnrollment() );
            }
        } );
        allEvents.add( new Event()
        {

            {
                String uid = CodeGenerator.generateUid();
                setEvent( uid );
                setEnrollment( allPs.get( 1 ).getEnrollment() );
            }
        } );
        allEvents.add( new Event()
        {

            {
                String uid = CodeGenerator.generateUid();
                setEvent( uid );
                setEnrollment( allPs.get( 1 ).getEnrollment() );
            }
        } );
        preheat.putEvents( psiList, allEvents );
        preheat.createReferenceTree();
        Optional<ReferenceTrackerEntity> reference = preheat.getReference( allEvents.get( 0 ).getUid() );
        assertThat( reference.get().getUid(), is( allEvents.get( 0 ).getUid() ) );
        assertThat( reference.get().getParentUid(), is( allPs.get( 0 ).getUid() ) );
        Optional<ReferenceTrackerEntity> reference2 = preheat.getReference( allEvents.get( 1 ).getUid() );
        assertThat( reference2.get().getUid(), is( allEvents.get( 1 ).getUid() ) );
        assertThat( reference2.get().getParentUid(), is( allPs.get( 0 ).getUid() ) );
        Optional<ReferenceTrackerEntity> reference3 = preheat.getReference( allEvents.get( 2 ).getUid() );
        assertThat( reference3.get().getUid(), is( allEvents.get( 2 ).getUid() ) );
        assertThat( reference3.get().getParentUid(), is( allPs.get( 1 ).getUid() ) );
        Optional<ReferenceTrackerEntity> reference4 = preheat.getReference( allEvents.get( 3 ).getUid() );
        assertThat( reference4.get().getUid(), is( allEvents.get( 3 ).getUid() ) );
        assertThat( reference4.get().getParentUid(), is( allPs.get( 1 ).getUid() ) );
    }

    private String concatCategoryOptions( TrackerIdSchemeParam idSchemeParam, Set<CategoryOption> options )
    {
        return options.stream()
            .map( idSchemeParam::getIdentifier )
            .collect( Collectors.joining( ";" ) );
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
