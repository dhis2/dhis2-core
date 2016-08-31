package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class TrackedEntityInstanceStoreTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityInstanceStore teiStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    private TrackedEntityInstance teiA;
    private TrackedEntityInstance teiB;
    private TrackedEntityInstance teiC;
    private TrackedEntityInstance teiD;
    private TrackedEntityInstance teiE;
    private TrackedEntityInstance teiF;

    private TrackedEntityAttribute atA;
    private TrackedEntityAttribute atB;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;

    private Program prA;
    private Program prB;

    @Override
    public void setUpTest()
    {
        atA = createTrackedEntityAttribute( 'A' );
        atB = createTrackedEntityAttribute( 'B' );
        atB.setUnique( true );

        idObjectManager.save( atA );
        idObjectManager.save( atB );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouB );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );

        prA = createProgram( 'A', null, null );
        prB = createProgram( 'B', null, null );

        idObjectManager.save( prA );
        idObjectManager.save( prB );

        teiA = createTrackedEntityInstance( 'A', ouA );
        teiB = createTrackedEntityInstance( 'B', ouB );
        teiC = createTrackedEntityInstance( 'C', ouB );
        teiD = createTrackedEntityInstance( 'D', ouC );
        teiE = createTrackedEntityInstance( 'E', ouC );
        teiF = createTrackedEntityInstance( 'F', ouC );
    }

    @Test
    public void testTrackedEntityInstanceExists()
    {
        teiStore.save( teiA );
        teiStore.save( teiB );

        assertTrue( teiStore.exists( teiA.getUid() ) );
        assertTrue( teiStore.exists( teiB.getUid() ) );
        assertFalse( teiStore.exists( "aaaabbbbccc" ) );
        assertFalse( teiStore.exists( null ) );
    }

    @Test
    public void testAddGet()
    {
        int idA = teiStore.save( teiA );
        int idB = teiStore.save( teiB );

        assertNotNull( teiStore.get( idA ) );
        assertNotNull( teiStore.get( idB ) );
    }

    @Test
    public void testAddGetbyOu()
    {
        int idA = teiStore.save( teiA );
        int idB = teiStore.save( teiB );

        assertEquals( teiA.getName(), teiStore.get( idA ).getName() );
        assertEquals( teiB.getName(), teiStore.get( idB ).getName() );
    }

    @Test
    public void testDelete()
    {
        int idA = teiStore.save( teiA );
        int idB = teiStore.save( teiB );

        assertNotNull( teiStore.get( idA ) );
        assertNotNull( teiStore.get( idB ) );

        teiStore.delete( teiA );

        assertNull( teiStore.get( idA ) );
        assertNotNull( teiStore.get( idB ) );

        teiStore.delete( teiB );

        assertNull( teiStore.get( idA ) );
        assertNull( teiStore.get( idB ) );
    }

    @Test
    public void testGetAll()
    {
        teiStore.save( teiA );
        teiStore.save( teiB );

        assertTrue( equals( teiStore.getAll(), teiA, teiB ) );
    }

    @Test
    public void testQuery()
    {
        teiStore.save( teiA );
        teiStore.save( teiB );
        teiStore.save( teiC );
        teiStore.save( teiD );
        teiStore.save( teiE );
        teiStore.save( teiF );

        attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( atA, teiD, "Male" ) );
        attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( atA, teiE, "Male" ) );
        attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( atA, teiF, "Female" ) );

        programInstanceService.enrollTrackedEntityInstance( teiB, prA, new Date(), new Date(), ouB );
        programInstanceService.enrollTrackedEntityInstance( teiE, prA, new Date(), new Date(), ouB );

        // Get all

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        List<TrackedEntityInstance> teis = teiStore.getTrackedEntityInstances( params );

        assertEquals( 6, teis.size() );

        // Filter by attribute

        params = new TrackedEntityInstanceQueryParams();
        params.addFilter( new QueryItem( atA, QueryOperator.EQ, "Male", ValueType.TEXT, AggregationType.NONE, null ) );

        teis = teiStore.getTrackedEntityInstances( params );

        assertEquals( 2, teis.size() );
        assertTrue( teis.contains( teiD ) );
        assertTrue( teis.contains( teiE ) );

        // Filter by attribute

        params = new TrackedEntityInstanceQueryParams();
        params.addFilter( new QueryItem( atA, QueryOperator.EQ, "Female", ValueType.TEXT, AggregationType.NONE, null ) );

        teis = teiStore.getTrackedEntityInstances( params );

        assertEquals( 1, teis.size() );
        assertTrue( teis.contains( teiF ) );

        // Filter by selected org units

        params = new TrackedEntityInstanceQueryParams();
        params.addOrganisationUnit( ouB );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED );

        teis = teiStore.getTrackedEntityInstances( params );

        assertEquals( 2, teis.size() );
        assertTrue( teis.contains( teiB ) );
        assertTrue( teis.contains( teiC ) );

        // Filter by descendants org units

        params = new TrackedEntityInstanceQueryParams();
        params.addOrganisationUnit( ouB );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS );

        teis = teiStore.getTrackedEntityInstances( params );

        assertEquals( 5, teis.size() );
        assertTrue( teis.contains( teiB ) );
        assertTrue( teis.contains( teiC ) );
        assertTrue( teis.contains( teiD ) );
        assertTrue( teis.contains( teiE ) );
        assertTrue( teis.contains( teiF ) );

        // Filter by program enrollment

        params = new TrackedEntityInstanceQueryParams();
        params.setProgram( prA );

        teis = teiStore.getTrackedEntityInstances( params );

        assertEquals( 2, teis.size() );
        assertTrue( teis.contains( teiB ) );
        assertTrue( teis.contains( teiE ) );
    }
}
