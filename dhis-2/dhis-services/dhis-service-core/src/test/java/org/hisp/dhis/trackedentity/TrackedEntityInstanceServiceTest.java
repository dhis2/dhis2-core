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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityInstanceServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    private TrackedEntityInstance entityInstanceA1;

    private TrackedEntityInstance entityInstanceA3;

    private TrackedEntityInstance entityInstanceB1;

    private TrackedEntityAttribute entityInstanceAttribute;

    private OrganisationUnit organisationUnit;

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        OrganisationUnit organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        entityInstanceAttribute = createTrackedEntityAttribute( 'A' );
        attributeService.addTrackedEntityAttribute( entityInstanceAttribute );

        entityInstanceA1 = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceA3 = createTrackedEntityInstance( 'A', organisationUnit, entityInstanceAttribute );
        entityInstanceB1 = createTrackedEntityInstance( 'B', organisationUnit );
        entityInstanceB1.setUid( "UID-B1" );
    }

    @Test
    public void testSaveTrackedEntityInstance()
    {
        int idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        int idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testDeleteTrackedEntityInstance()
    {
        int idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        int idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testUpdateTrackedEntityInstance()
    {
        int idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );

        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );

        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }

    @Test
    public void testGetTrackedEntityInstanceById()
    {
        int idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        int idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( idA ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testGetTrackedEntityInstanceByUid()
    {
        entityInstanceA1.setUid( "A1" );
        entityInstanceB1.setUid( "B1" );

        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertEquals( entityInstanceA1, entityInstanceService.getTrackedEntityInstance( "A1" ) );
        assertEquals( entityInstanceB1, entityInstanceService.getTrackedEntityInstance( "B1" ) );
    }

    @Test
    public void testCreateTrackedEntityInstanceAndRelative()
    {
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        RelationshipType relationshipType = createRelationshipType( 'A' );
        int relationshipTypeId = relationshipTypeService.addRelationshipType( relationshipType );

        TrackedEntityAttributeValue attributeValue = createTrackedEntityAttributeValue( 'A', entityInstanceA1,
            entityInstanceAttribute );
        Set<TrackedEntityAttributeValue> entityInstanceAttributeValues = new HashSet<>();
        entityInstanceAttributeValues.add( attributeValue );

        int idA = entityInstanceService.createTrackedEntityInstance( entityInstanceA1, entityInstanceB1.getUid(),
            relationshipTypeId, entityInstanceAttributeValues );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
    }

    @Test
    public void testUpdateTrackedEntityInstanceAndRelative()
    {
        entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        RelationshipType relationshipType = createRelationshipType( 'A' );
        int relationshipTypeId = relationshipTypeService.addRelationshipType( relationshipType );

        entityInstanceA3.setName( "B" );
        TrackedEntityAttributeValue attributeValue = createTrackedEntityAttributeValue( 'A', entityInstanceA3,
            entityInstanceAttribute );
        Set<TrackedEntityAttributeValue> entityInstanceAttributeValues = new HashSet<>();
        entityInstanceAttributeValues.add( attributeValue );
        int idA = entityInstanceService.createTrackedEntityInstance( entityInstanceA3, entityInstanceB1.getUid(),
            relationshipTypeId, entityInstanceAttributeValues );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );

        attributeValue.setValue( "AttributeB" );
        List<TrackedEntityAttributeValue> attributeValues = new ArrayList<>();
        attributeValues.add( attributeValue );

        entityInstanceService.updateTrackedEntityInstance( entityInstanceA3, entityInstanceB1.getUid(),
            relationshipTypeId, attributeValues, new ArrayList<>(),
            new ArrayList<>() );
        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }
}
