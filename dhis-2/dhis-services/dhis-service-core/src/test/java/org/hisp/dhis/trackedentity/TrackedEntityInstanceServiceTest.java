package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.junit.Assert.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
    private TrackedEntityTypeService typeService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private UserService _userService;

    private TrackedEntityInstance entityInstanceA1;

    private TrackedEntityInstance entityInstanceB1;

    private TrackedEntityAttribute entityInstanceAttribute;

    private OrganisationUnit organisationUnit;

    @Override
    public void setUpTest()
    {
        userService = _userService;
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

        TrackedEntityInstance teiA = entityInstanceService.getTrackedEntityInstance( idA );
        TrackedEntityInstance teiB = entityInstanceService.getTrackedEntityInstance( idB );

        assertNotNull( teiA );
        assertNotNull( teiB );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceA1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNotNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );

        entityInstanceService.deleteTrackedEntityInstance( entityInstanceB1 );

        assertNull( entityInstanceService.getTrackedEntityInstance( teiA.getUid() ) );
        assertNull( entityInstanceService.getTrackedEntityInstance( teiB.getUid() ) );
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

    @Test
    public void testSaveTrackedEntityInstanceWithAudit()
    {
        createAndInjectAdminUser();
        TrackedEntityType type = createTrackedEntityType( 'A' );
        type.setAllowAuditLog( true );
        typeService.addTrackedEntityType( type );
        entityInstanceA1.setTrackedEntityType( type );

        entityInstanceService.addTrackedEntityInstanceWithAudit( entityInstanceA1 );

        Mockito.verify( applicationEventPublisher ).publishEvent( Mockito.argThat( ( Audit audit ) -> audit != null && audit.getUid() != null && audit.getUid().equals( entityInstanceA1.getUid() ) ) );
    }

    @Test
    public void testDeleteTrackedEntityInstanceWithAudit()
    {
        createAndInjectAdminUser();
        TrackedEntityType type = createTrackedEntityType( 'A' );
        type.setAllowAuditLog( true );
        typeService.addTrackedEntityType( type );
        entityInstanceA1.setTrackedEntityType( type );

        entityInstanceService.addTrackedEntityInstance( entityInstanceA1 );
        // Audit event should not be published here
        Mockito.verify( applicationEventPublisher, Mockito.never() ).publishEvent( Audit.class );

        entityInstanceService.deleteTrackedEntityInstanceWithAudit( entityInstanceA1 );

        // Check if delete audit event is published
        Mockito.verify( applicationEventPublisher ).publishEvent( Mockito.argThat( (Audit audit) -> audit.getAuditType() == AuditType.DELETE ) );
    }

    @Configuration
    static class ApplicationEventPublisherConfig
    {
        @Bean
        @Primary
        public ApplicationEventPublisher applicationEventPublisher() {
            return Mockito.mock( ApplicationEventPublisher.class );
        }
    }

}
