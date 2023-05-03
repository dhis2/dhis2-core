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
package org.hisp.dhis.deduplication.hibernate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.deduplication.PotentialDuplicateStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PotentialDuplicateRemoveTrackedEntityTest extends TransactionalIntegrationTest
{

    @Autowired
    private PotentialDuplicateStore potentialDuplicateStore;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramService programService;

    @Test
    void shouldDeleteTrackedEntityInstance()
    {
        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttribute );
        TrackedEntityInstance trackedEntityInstance = createTei( trackedEntityAttribute );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance.getUid() ) );
        removeTrackedEntity( trackedEntityInstance );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance.getUid() ) );
    }

    @Test
    void shouldDeleteTeiAndAttributeValues()
    {
        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeService.addTrackedEntityAttribute( trackedEntityAttribute );
        TrackedEntityInstance trackedEntityInstance = createTei( trackedEntityAttribute );
        trackedEntityInstance.getTrackedEntityAttributeValues()
            .forEach( trackedEntityAttributeValueService::addTrackedEntityAttributeValue );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance.getUid() ) );
        removeTrackedEntity( trackedEntityInstance );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstance.getUid() ) );
        assertNull( trackedEntityAttributeValueService.getTrackedEntityAttributeValue( trackedEntityInstance,
            trackedEntityAttribute ) );
    }

    @Test
    void shouldDeleteRelationShips()
    {
        OrganisationUnit ou = createOrganisationUnit( "OU_A" );
        organisationUnitService.addOrganisationUnit( ou );
        TrackedEntityInstance original = createTrackedEntityInstance( ou );
        TrackedEntityInstance duplicate = createTrackedEntityInstance( ou );
        TrackedEntityInstance control1 = createTrackedEntityInstance( ou );
        TrackedEntityInstance control2 = createTrackedEntityInstance( ou );
        trackedEntityInstanceService.addTrackedEntityInstance( original );
        trackedEntityInstanceService.addTrackedEntityInstance( duplicate );
        trackedEntityInstanceService.addTrackedEntityInstance( control1 );
        trackedEntityInstanceService.addTrackedEntityInstance( control2 );
        RelationshipType relationshipType = createRelationshipType( 'A' );
        relationshipTypeService.addRelationshipType( relationshipType );
        Relationship relationship1 = createTeiToTeiRelationship( original, control1, relationshipType );
        Relationship relationship2 = createTeiToTeiRelationship( control2, control1, relationshipType );
        Relationship relationship3 = createTeiToTeiRelationship( duplicate, control2, relationshipType );
        Relationship relationship4 = createTeiToTeiRelationship( control1, duplicate, relationshipType );
        Relationship relationship5 = createTeiToTeiRelationship( control1, original, relationshipType );
        long relationShip1 = relationshipService.addRelationship( relationship1 );
        long relationShip2 = relationshipService.addRelationship( relationship2 );
        long relationShip3 = relationshipService.addRelationship( relationship3 );
        long relationShip4 = relationshipService.addRelationship( relationship4 );
        long relationShip5 = relationshipService.addRelationship( relationship5 );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( original.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( duplicate.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( control1.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( control2.getUid() ) );
        removeTrackedEntity( duplicate );
        assertNull( relationshipService.getRelationship( relationShip3 ) );
        assertNull( relationshipService.getRelationship( relationShip4 ) );
        assertNotNull( relationshipService.getRelationship( relationShip1 ) );
        assertNotNull( relationshipService.getRelationship( relationShip2 ) );
        assertNotNull( relationshipService.getRelationship( relationShip5 ) );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( duplicate.getUid() ) );
    }

    @Test
    void shouldDeleteEnrollments()
    {
        OrganisationUnit ou = createOrganisationUnit( "OU_A" );
        organisationUnitService.addOrganisationUnit( ou );
        TrackedEntityInstance original = createTrackedEntityInstance( ou );
        TrackedEntityInstance duplicate = createTrackedEntityInstance( ou );
        TrackedEntityInstance control1 = createTrackedEntityInstance( ou );
        TrackedEntityInstance control2 = createTrackedEntityInstance( ou );
        trackedEntityInstanceService.addTrackedEntityInstance( original );
        trackedEntityInstanceService.addTrackedEntityInstance( duplicate );
        trackedEntityInstanceService.addTrackedEntityInstance( control1 );
        trackedEntityInstanceService.addTrackedEntityInstance( control2 );
        Program program = createProgram( 'A' );
        programService.addProgram( program );
        Enrollment enrollment1 = createProgramInstance( program, original, ou );
        Enrollment enrollment2 = createProgramInstance( program, duplicate, ou );
        Enrollment enrollment3 = createProgramInstance( program, control1, ou );
        Enrollment enrollment4 = createProgramInstance( program, control2, ou );
        programInstanceService.addProgramInstance( enrollment1 );
        programInstanceService.addProgramInstance( enrollment2 );
        programInstanceService.addProgramInstance( enrollment3 );
        programInstanceService.addProgramInstance( enrollment4 );
        original.getEnrollments().add( enrollment1 );
        duplicate.getEnrollments().add( enrollment2 );
        control1.getEnrollments().add( enrollment3 );
        control2.getEnrollments().add( enrollment4 );
        trackedEntityInstanceService.updateTrackedEntityInstance( original );
        trackedEntityInstanceService.updateTrackedEntityInstance( duplicate );
        trackedEntityInstanceService.updateTrackedEntityInstance( control1 );
        trackedEntityInstanceService.updateTrackedEntityInstance( control2 );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( original.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( duplicate.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( control1.getUid() ) );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( control2.getUid() ) );
        removeTrackedEntity( duplicate );
        assertNull( programInstanceService.getProgramInstance( enrollment2.getUid() ) );
        assertNotNull( programInstanceService.getProgramInstance( enrollment1.getUid() ) );
        assertNotNull( programInstanceService.getProgramInstance( enrollment3.getUid() ) );
        assertNotNull( programInstanceService.getProgramInstance( enrollment4.getUid() ) );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( duplicate.getUid() ) );
    }

    private TrackedEntityInstance createTei( TrackedEntityAttribute trackedEntityAttribute )
    {
        OrganisationUnit ou = createOrganisationUnit( "OU_A" );
        organisationUnitService.addOrganisationUnit( ou );
        TrackedEntityInstance trackedEntityInstance = createTrackedEntityInstance( 'T', ou, trackedEntityAttribute );
        trackedEntityInstanceService.addTrackedEntityInstance( trackedEntityInstance );
        return trackedEntityInstance;
    }

    private void removeTrackedEntity( TrackedEntityInstance trackedEntityInstance )
    {
        potentialDuplicateStore.removeTrackedEntity( trackedEntityInstance );
    }
}
