package org.hisp.dhis.relationship;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class RelationshipServiceTest
    extends DhisSpringTest
{
    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    private RelationshipType relationshipType;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private TrackedEntityInstance entityInstanceC;

    private TrackedEntityInstance entityInstanceD;

    private Relationship relationshipA;

    private Relationship relationshipB;

    private Relationship relationshipC;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        entityInstanceB = createTrackedEntityInstance( 'B', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        entityInstanceC = createTrackedEntityInstance( 'C', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstanceC );

        entityInstanceD = createTrackedEntityInstance( 'D', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstanceD );

        relationshipType = createRelationshipType( 'A' );
        relationshipTypeService.addRelationshipType( relationshipType );

        relationshipA = new Relationship( entityInstanceA, relationshipType, entityInstanceB );
        relationshipB = new Relationship( entityInstanceC, relationshipType, entityInstanceD );
        relationshipC = new Relationship( entityInstanceA, relationshipType, entityInstanceC );
    }

    @Test
    public void testSaveRelationship()
    {
        int idA = relationshipService.addRelationship( relationshipA );
        int idB = relationshipService.addRelationship( relationshipB );

        assertNotNull( relationshipService.getRelationship( idA ) );
        assertNotNull( relationshipService.getRelationship( idB ) );
    }

    @Test
    public void testDeleteRelationship()
    {
        int idA = relationshipService.addRelationship( relationshipA );
        int idB = relationshipService.addRelationship( relationshipB );

        assertNotNull( relationshipService.getRelationship( idA ) );
        assertNotNull( relationshipService.getRelationship( idB ) );

        relationshipService.deleteRelationship( relationshipA );

        assertNull( relationshipService.getRelationship( idA ) );
        assertNotNull( relationshipService.getRelationship( idB ) );

        relationshipService.deleteRelationship( relationshipB );

        assertNull( relationshipService.getRelationship( idA ) );
        assertNull( relationshipService.getRelationship( idB ) );
    }

    @Test
    public void testUpdateRelationship()
    {
        int idA = relationshipService.addRelationship( relationshipA );

        assertNotNull( relationshipService.getRelationship( idA ) );

        relationshipA.setEntityInstanceA( entityInstanceC );
        relationshipService.updateRelationship( relationshipA );

        assertEquals( relationshipA, relationshipService.getRelationship( idA ) );
    }

    @Test
    public void testGetRelationshipById()
    {
        int idA = relationshipService.addRelationship( relationshipA );
        int idB = relationshipService.addRelationship( relationshipB );

        assertEquals( relationshipA, relationshipService.getRelationship( idA ) );
        assertEquals( relationshipB, relationshipService.getRelationship( idB ) );
    }

    @Test
    public void testGetRelationshipByTypeEntityInstance()
    {
        relationshipService.addRelationship( relationshipA );
        relationshipService.addRelationship( relationshipB );

        Relationship relationship = relationshipService.getRelationship( entityInstanceA, entityInstanceB,
            relationshipType );
        assertEquals( relationshipA, relationship );

        relationship = relationshipService.getRelationship( entityInstanceC, entityInstanceD, relationshipType );
        assertEquals( relationshipB, relationship );
    }

    @Test
    public void testGetRelationshipsForEntityInstance()
    {
        relationshipService.addRelationship( relationshipA );
        relationshipService.addRelationship( relationshipC );

        List<Relationship> relationships = relationshipService
            .getRelationshipsForTrackedEntityInstance( entityInstanceA );
        assertTrue( equals( relationships, relationshipA, relationshipC ) );
    }

}
