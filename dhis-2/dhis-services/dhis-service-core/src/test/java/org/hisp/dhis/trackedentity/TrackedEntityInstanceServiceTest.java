package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

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

    private TrackedEntityInstance entityInstanceA1;

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

        entityInstanceA1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceB1 = createTrackedEntityInstance( organisationUnit );
        entityInstanceB1.setUid( "UID-B1" );
    }

    @Test
    public void testSaveTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );
    }

    @Test
    public void testDeleteTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( idB ) );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertTrue( entityInstanceService.getTrackedEntityInstance( idA ) == null );
        assertFalse( entityInstanceService.getTrackedEntityInstance( idB ) == null );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB1 );

        assertTrue( entityInstanceService.getTrackedEntityInstance( idA ) == null );
        assertTrue( entityInstanceService.getTrackedEntityInstance( idB ) == null );
    }

    @Test
    public void testUpdateTrackedEntityInstance()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );

        assertNotNull( entityInstanceService.getTrackedEntityInstance( idA ) );

        entityInstanceA1.setName( "B" );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA1 );

        assertEquals( "B", entityInstanceService.getTrackedEntityInstance( idA ).getName() );
    }

    @Test
    public void testGetTrackedEntityInstanceById()
    {
        long idA = entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        long idB = entityInstanceService.addTrackedEntityInstance( entityInstanceB1 );

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

}
