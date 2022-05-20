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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ameen
 */
class TrackedEntityAttributeStoreIntegrationTest
    extends
    IntegrationTestBase
{
    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private ProgramService programService;

    private final static int A = 65;

    private final static int T = 85;

    private Program programB;

    private TrackedEntityAttribute attributeW;

    private TrackedEntityAttribute attributeY;

    private TrackedEntityAttribute attributeZ;

    @Override
    public void setUpTest()
    {

        attributeW = createTrackedEntityAttribute( 'W' );
        attributeW.setUnique( true );
        attributeY = createTrackedEntityAttribute( 'Y' );
        attributeY.setUnique( true );
        attributeZ = createTrackedEntityAttribute( 'Z', ValueType.NUMBER );

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

        // Setting searchable to true for 5 random tracked entity type
        // attributes
        TrackedEntityTypeAttribute teta = teatList.get( 0 );
        teta.setSearchable( true );
        teta = teatList.get( 4 );
        teta.setSearchable( true );
        teta = teatList.get( 9 );
        teta.setSearchable( true );
        teta = teatList.get( 14 );
        teta.setSearchable( true );
        teta = teatList.get( 19 );
        teta.setSearchable( true );

        // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type A
        trackedEntityTypeA.getTrackedEntityTypeAttributes().addAll( teatList.subList( 0, 10 ) );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeA );

        // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type B
        trackedEntityTypeB.getTrackedEntityTypeAttributes().addAll( teatList.subList( 10, 20 ) );
        trackedEntityTypeService.updateTrackedEntityType( trackedEntityTypeB );

        programB = createProgram( 'B' );
        programService.addProgram( programB );

        List<ProgramTrackedEntityAttribute> pteaList = IntStream.range( A, T )
            .mapToObj( i -> Character.toString( (char) i ) ).map( s -> new ProgramTrackedEntityAttribute( programB,
                attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
            .collect( Collectors.toList() );

        // Setting searchable to true for 5 random program tracked entity
        // attributes
        ProgramTrackedEntityAttribute ptea = pteaList.get( 0 );
        ptea.setSearchable( true );
        ptea = pteaList.get( 4 );
        ptea.setSearchable( true );
        ptea = pteaList.get( 9 );
        ptea.setSearchable( true );
        ptea = pteaList.get( 13 );
        ptea.setSearchable( true );
        ptea = pteaList.get( 18 );
        ptea.setSearchable( true );

        programB.getProgramAttributes().addAll( pteaList );
        programService.updateProgram( programB );

    }

    @Test
    void testGetAllIndexableAttributes()
    {
        attributeService.addTrackedEntityAttribute( attributeW );
        attributeService.addTrackedEntityAttribute( attributeY );
        attributeService.addTrackedEntityAttribute( attributeZ );

        Set<TrackedEntityAttribute> indexableAttributes = attributeService
            .getAllTrigramIndexableTrackedEntityAttributes();

        assertNotNull( indexableAttributes );
        assertEquals( indexableAttributes.size(), 9 );
        assertTrue( indexableAttributes.contains( attributeW ) );
        assertTrue( indexableAttributes.contains( attributeY ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'A' ) ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'E' ) ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'J' ) ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'N' ) ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'O' ) ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'S' ) ) );
        assertTrue(
            indexableAttributes.contains( attributeService.getTrackedEntityAttributeByName( "Attribute" + 'T' ) ) );
    }

    @Test
    void testCreateTrigramIndex()
    {
        attributeService.addTrackedEntityAttribute( attributeW );
        trackedEntityAttributeTableManager.createTrigramIndex( attributeW );
        assertFalse( jdbcTemplate.queryForList( "select * "
            + "from pg_indexes "
            + "where tablename= 'trackedentityattributevalue' and indexname like 'in_gin_teavalue_%'; " ).isEmpty() );
    }

    @Test
    void testTrigramIndexDetection()
    {
        attributeService.addTrackedEntityAttribute( attributeW );
        trackedEntityAttributeTableManager.createTrigramIndex( attributeW );

        List<Long> attributeIds = trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndexCreated();

        assertNotNull( attributeIds );
        assertTrue( attributeIds.contains( attributeW.getId() ) );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

}