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
package org.hisp.dhis.trackedentity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityAttributeStoreTest extends TransactionalIntegrationTest
{

    @Autowired
    private TrackedEntityAttributeService attributeService;

    private TrackedEntityAttribute attributeW;

    private TrackedEntityAttribute attributeY;

    private TrackedEntityAttribute attributeZ;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private ProgramService programService;

    private final static int A = 65;

    private final static int T = 85;

    private Program programB;

    @Override
    public void setUpTest()
    {
        attributeW = createTrackedEntityAttribute( 'W' );
        attributeY = createTrackedEntityAttribute( 'Y' );
        attributeZ = createTrackedEntityAttribute( 'Z', ValueType.NUMBER );
        List<TrackedEntityAttribute> attributesA = new ArrayList<>();
        attributesA.add( attributeW );
        attributesA.add( attributeY );
        Program program = createProgram( 'A' );
        programService.addProgram( program );
        TrackedEntityType trackedEntityTypeA = createTrackedEntityType( 'A' );
        trackedEntityTypeA.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeA );
        TrackedEntityType trackedEntityTypeB = createTrackedEntityType( 'B' );
        trackedEntityTypeB.setPublicAccess( AccessStringHelper.FULL );
        trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeB );
        // Create 20 Tracked Entity Attributes (named A .. O)
        IntStream.range( A, T ).mapToObj( i -> Character.toString( (char) i ) ).forEach( c -> attributeService
            .addTrackedEntityAttribute( createTrackedEntityAttribute( c.charAt( 0 ), ValueType.TEXT ) ) );
        // Transform the Tracked Entity Attributes into a List of
        // TrackedEntityTypeAttribute
        List<TrackedEntityTypeAttribute> teatList = IntStream.range( A, T )
            .mapToObj( i -> Character.toString( (char) i ) )
            .map( s -> new TrackedEntityTypeAttribute( trackedEntityTypeA,
                attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
            .collect( Collectors.toList() );
        // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type A
        trackedEntityTypeA.setTrackedEntityTypeAttributes( teatList.subList( 0, 10 ) );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeA );
        // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type B
        trackedEntityTypeB.setTrackedEntityTypeAttributes( teatList.subList( 10, 20 ) );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeB );
        programB = createProgram( 'B' );
        programService.addProgram( programB );
        List<ProgramTrackedEntityAttribute> pteaList = IntStream.range( A, T )
            .mapToObj( i -> Character.toString( (char) i ) ).map( s -> new ProgramTrackedEntityAttribute( programB,
                attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
            .collect( Collectors.toList() );
        programB.setProgramAttributes( pteaList );
        programService.updateProgram( programB );
    }

    @Test
    void testSaveTrackedEntityAttribute()
    {
        long idA = attributeService.addTrackedEntityAttribute( attributeW );
        long idB = attributeService.addTrackedEntityAttribute( attributeY );
        assertNotNull( attributeService.getTrackedEntityAttribute( idA ) );
        assertNotNull( attributeService.getTrackedEntityAttribute( idB ) );
    }

    @Test
    void testDeleteTrackedEntityAttribute()
    {
        long idA = attributeService.addTrackedEntityAttribute( attributeW );
        long idB = attributeService.addTrackedEntityAttribute( attributeY );
        assertNotNull( attributeService.getTrackedEntityAttribute( idA ) );
        assertNotNull( attributeService.getTrackedEntityAttribute( idB ) );
        attributeService.deleteTrackedEntityAttribute( attributeW );
        assertNull( attributeService.getTrackedEntityAttribute( idA ) );
        assertNotNull( attributeService.getTrackedEntityAttribute( idB ) );
        attributeService.deleteTrackedEntityAttribute( attributeY );
        assertNull( attributeService.getTrackedEntityAttribute( idA ) );
        assertNull( attributeService.getTrackedEntityAttribute( idB ) );
    }

    @Test
    void testUpdateTrackedEntityAttribute()
    {
        long idA = attributeService.addTrackedEntityAttribute( attributeW );
        assertNotNull( attributeService.getTrackedEntityAttribute( idA ) );
        attributeW.setName( "B" );
        attributeService.updateTrackedEntityAttribute( attributeW );
        assertEquals( "B", attributeService.getTrackedEntityAttribute( idA ).getName() );
    }

    @Test
    void testGetTrackedEntityAttributeById()
    {
        long idA = attributeService.addTrackedEntityAttribute( attributeW );
        long idB = attributeService.addTrackedEntityAttribute( attributeY );
        assertEquals( attributeW, attributeService.getTrackedEntityAttribute( idA ) );
        assertEquals( attributeY, attributeService.getTrackedEntityAttribute( idB ) );
    }

    @Test
    void testGetTrackedEntityAttributeByUid()
    {
        attributeW.setUid( "uid" );
        attributeService.addTrackedEntityAttribute( attributeW );
        assertEquals( attributeW, attributeService.getTrackedEntityAttribute( "uid" ) );
    }

    @Test
    void testGetTrackedEntityAttributeByName()
    {
        long idA = attributeService.addTrackedEntityAttribute( attributeW );
        assertNotNull( attributeService.getTrackedEntityAttribute( idA ) );
        assertEquals( attributeW.getName(),
            attributeService.getTrackedEntityAttributeByName( "AttributeW" ).getName() );
    }

    @Test
    void testGetAllTrackedEntityAttributes()
    {
        attributeService.addTrackedEntityAttribute( attributeW );
        attributeService.addTrackedEntityAttribute( attributeY );
        List<TrackedEntityAttribute> teas = attributeService.getAllTrackedEntityAttributes();
        assertThat( teas, hasItem( attributeW ) );
        assertThat( teas, hasItem( attributeY ) );
    }

    @Test
    void testGetTrackedEntityAttributesByDisplayOnVisitSchedule()
    {
        attributeW.setDisplayOnVisitSchedule( true );
        attributeY.setDisplayOnVisitSchedule( true );
        attributeZ.setDisplayOnVisitSchedule( false );
        attributeService.addTrackedEntityAttribute( attributeW );
        attributeService.addTrackedEntityAttribute( attributeY );
        attributeService.addTrackedEntityAttribute( attributeZ );
        List<TrackedEntityAttribute> attributes = attributeService
            .getTrackedEntityAttributesByDisplayOnVisitSchedule( true );
        assertEquals( 2, attributes.size() );
        assertTrue( attributes.contains( attributeW ) );
        assertTrue( attributes.contains( attributeY ) );
        attributes = attributeService.getTrackedEntityAttributesByDisplayOnVisitSchedule( false );
        assertEquals( 21, attributes.size() );
        assertTrue( attributes.contains( attributeZ ) );
    }

    @Test
    void verifyGetTrackedEntityAttributesByTrackedEntityTypes()
    {
        Set<TrackedEntityAttribute> trackedEntityAttributes = attributeService
            .getTrackedEntityAttributesByTrackedEntityTypes();
        assertThat( trackedEntityAttributes, hasSize( 20 ) );
    }

    @Test
    void verifyGetTrackedEntityAttributesByProgram()
    {
        Map<Program, Set<TrackedEntityAttribute>> trackedEntityAttributes = attributeService
            .getTrackedEntityAttributesByProgram();
        assertThat( trackedEntityAttributes.size(), is( 1 ) );
        assertThat( trackedEntityAttributes.get( programB ), hasSize( 20 ) );
    }
}
