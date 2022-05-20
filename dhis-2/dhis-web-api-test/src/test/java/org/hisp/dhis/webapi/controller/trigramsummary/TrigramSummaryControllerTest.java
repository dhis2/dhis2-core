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
package org.hisp.dhis.webapi.controller.trigramsummary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.hisp.dhis.trigramsummary.TrigramSummary;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith( MockitoExtension.class )
class TrigramSummaryControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Mock
    private TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private AclService aclService;

    @Autowired
    private FieldFilterService fieldFilterService;

    private TrigramSummaryController controller;

    private OrganisationUnit orgUnit;

    private Program program;

    private TrackedEntityInstance tei;

    private ProgramInstance programInstance;

    private TrackedEntityAttribute teaA;

    private TrackedEntityAttribute teaB;

    private TrackedEntityAttribute teaC;

    private TrackedEntityAttribute teaD;

    private TrackedEntityAttribute teaE;

    private TrackedEntityAttribute teaF;

    ProgramTrackedEntityAttribute pteaA;

    ProgramTrackedEntityAttribute pteaB;

    TrackedEntityTypeAttribute tetaA;

    TrackedEntityTypeAttribute tetaB;

    TrackedEntityType trackedEntityType;

    @BeforeEach
    void setUp()
    {
        controller = new TrigramSummaryController( trackedEntityAttributeService, trackedEntityAttributeTableManager,
            contextService, aclService, fieldFilterService );
        orgUnit = createOrganisationUnit( 'A' );
        manager.save( orgUnit );

        program = createProgram( 'A' );
        manager.save( program );

        teaA = createTrackedEntityAttribute( 'A' );
        teaB = createTrackedEntityAttribute( 'B' );
        teaC = createTrackedEntityAttribute( 'C' );
        teaD = createTrackedEntityAttribute( 'D' );
        teaE = createTrackedEntityAttribute( 'E' );
        teaF = createTrackedEntityAttribute( 'F' );
        teaF.setUnique( true );
        manager.save( teaA );
        manager.save( teaB );
        manager.save( teaC );
        manager.save( teaD );
        manager.save( teaE );
        manager.save( teaF );

        trackedEntityType = createTrackedEntityType( 'A' );
        manager.save( trackedEntityType );

        tetaA = new TrackedEntityTypeAttribute();
        tetaA.setSearchable( true );
        tetaA.setTrackedEntityType( trackedEntityType );
        tetaA.setTrackedEntityAttribute( teaA );
        manager.save( tetaA );
        trackedEntityType.getTrackedEntityTypeAttributes().add( tetaA );

        tetaB = new TrackedEntityTypeAttribute();
        tetaB.setSearchable( false );
        tetaB.setTrackedEntityType( trackedEntityType );
        tetaB.setTrackedEntityAttribute( teaD );
        manager.save( tetaB );
        trackedEntityType.getTrackedEntityTypeAttributes().add( tetaB );

        manager.update( trackedEntityType );

        pteaA = new ProgramTrackedEntityAttribute();
        pteaA.setSearchable( true );
        pteaA.setAttribute( teaB );
        pteaA.setProgram( program );
        manager.save( pteaA );

        pteaB = new ProgramTrackedEntityAttribute();
        pteaB.setSearchable( false );
        pteaB.setAttribute( teaE );
        pteaB.setProgram( program );
        manager.save( pteaB );

        program.getProgramAttributes().add( pteaA );
        program.getProgramAttributes().add( pteaB );
        manager.update( program );

        tei = createTrackedEntityInstance( orgUnit );
        tei.setTrackedEntityType( trackedEntityType );
        manager.save( tei );

        programInstance = new ProgramInstance( program, tei, orgUnit );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        programInstance.setFollowup( true );
        manager.save( programInstance );
    }

    @Test
    void getTrigramIndexSummaryWhenNoIndexesAreCreated()
    {

        when( trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndexCreated() )
            .thenReturn( new ArrayList<>() );
        TrigramSummary trigramSummary = controller.getTrigramSummary( new HashMap<>() );

        assertNotNull( trigramSummary );

        Set<String> expected = Set.of( "\"AttributeA\"", "\"AttributeB\"", "\"AttributeF\"" );
        assertAttributeList( trigramSummary.getIndexableAttributes(), expected );

        Set<String> expected2 = Set.of();
        assertAttributeList( trigramSummary.getIndexedAttributes(), expected2 );

        Set<String> expected3 = Set.of();
        assertAttributeList( trigramSummary.getObsoleteIndexedAttributes(), expected3 );

    }

    @Test
    void getTrigramIndexSummaryWithOneIndexAlreadyCreated()
    {

        when( trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndexCreated() )
            .thenReturn( List.of( teaB.getId() ) );
        TrigramSummary trigramSummary = controller.getTrigramSummary( new HashMap<>() );

        assertNotNull( trigramSummary );

        Set<String> expected = Set.of( "\"AttributeA\"", "\"AttributeF\"" );
        assertAttributeList( trigramSummary.getIndexableAttributes(), expected );

        Set<String> expected2 = Set.of( "\"AttributeB\"" );
        assertAttributeList( trigramSummary.getIndexedAttributes(), expected2 );

        Set<String> expected3 = Set.of();
        assertAttributeList( trigramSummary.getObsoleteIndexedAttributes(), expected3 );

    }

    @Test
    void getTrigramIndexSummaryWithAnObsoleteIndex()
    {

        when( trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndexCreated() )
            .thenReturn( List.of( teaB.getId(), teaC.getId() ) );
        TrigramSummary trigramSummary = controller.getTrigramSummary( new HashMap<>() );

        assertNotNull( trigramSummary );

        Set<String> expected = Set.of( "\"AttributeA\"", "\"AttributeF\"" );
        assertAttributeList( trigramSummary.getIndexableAttributes(), expected );

        Set<String> expected2 = Set.of( "\"AttributeB\"" );
        assertAttributeList( trigramSummary.getIndexedAttributes(), expected2 );

        Set<String> expected3 = Set.of( "\"AttributeC\"" );
        assertAttributeList( trigramSummary.getObsoleteIndexedAttributes(), expected3 );

    }

    private static void assertAttributeList( List<ObjectNode> attributes, Set<String> expected )
    {
        assertEquals( expected.size(), attributes.size() );
        assertEquals( expected, attributes.stream().map( e -> e.get( "displayName" ).toString() )
            .collect( Collectors.toSet() ) );
    }

}