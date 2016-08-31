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
public class RelationshipStoreTest
    extends DhisSpringTest
{
    @Autowired
    private RelationshipStore relationshipStore;

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
    public void testGetRelationshipByTypeEntityInstance()
    {
        relationshipStore.save( relationshipA );
        relationshipStore.save( relationshipB );

        Relationship relationship = relationshipStore.get( entityInstanceA, entityInstanceB, relationshipType );
        assertEquals( relationshipA, relationship );

        relationship = relationshipStore.get( entityInstanceC, entityInstanceD, relationshipType );
        assertEquals( relationshipB, relationship );
    }

    @Test
    public void testGetRelationshipsForEntityInstance()
    {
        relationshipStore.save( relationshipA );
        relationshipStore.save( relationshipC );

        List<Relationship> relationships = relationshipStore.getForTrackedEntityInstance( entityInstanceA );
        assertTrue( equals( relationships, relationshipA, relationshipC ) );
    }

    @Test
    public void testGetRelationshipsByRelationshipType()
    {
        relationshipStore.save( relationshipA );
        relationshipStore.save( relationshipC );

        List<Relationship> relationships = relationshipStore.getByRelationshipType( relationshipType );
        assertTrue( equals( relationships, relationshipA, relationshipC ) );
    }

}
