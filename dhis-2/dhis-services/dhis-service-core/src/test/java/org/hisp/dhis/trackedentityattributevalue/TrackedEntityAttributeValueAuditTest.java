package org.hisp.dhis.trackedentityattributevalue;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditRepository;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TrackedEntityAttributeValueAuditTest extends IntegrationTestBase
{
    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuditObjectFactory auditObjectFactory;

    @Autowired
    private TrackedEntityAttributeValueAuditService attributeValueAuditService;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;


    private TrackedEntityAttributeValue attributeValueA;

    private TrackedEntityAttributeValue attributeValueB;

    String userName = "system";

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        entityInstanceA = createTrackedEntityInstance( organisationUnit );
        entityInstanceB = createTrackedEntityInstance( organisationUnit );

        attributeA = createTrackedEntityAttribute( 'A' );
        attributeB = createTrackedEntityAttribute( 'B' );

        attributeValueA = new TrackedEntityAttributeValue( attributeA, entityInstanceA, "A" );
        attributeValueB = new TrackedEntityAttributeValue( attributeB, entityInstanceA, "B" );
    }

    @Test
    public void testAddAttributeValueAudit()
    {
        AuditAttributes auditAttributesA = new AuditAttributes();
        auditAttributesA.put( "trackedEntityInstance", entityInstanceA.getUid() );
        auditAttributesA.put( "trackedEntityAttribute", attributeA.getUid() );

        attributeValueA = new TrackedEntityAttributeValue( attributeA, entityInstanceA, "A" );
        Audit auditA = Audit.builder().auditScope( AuditScope.TRACKER )
            .auditType( AuditType.CREATE )
            .data( auditObjectFactory.create( AuditScope.TRACKER, AuditType.CREATE, attributeValueA, userName ).toString() )
            .klass( TrackedEntityAttributeValue.class.getName() )
            .attributes( auditAttributesA )
            .createdAt( LocalDateTime.now() )
            .createdBy( userName )
            .build();
        auditRepository.save( auditA );

        AuditAttributes auditAttributesB = new AuditAttributes();
        auditAttributesA.put( "trackedEntityInstance", entityInstanceB.getUid() );
        auditAttributesA.put( "trackedEntityAttribute", attributeB.getUid() );
        Audit auditB = Audit.builder().auditScope( AuditScope.TRACKER )
            .auditType( AuditType.CREATE )
            .data( auditObjectFactory.create( AuditScope.TRACKER, AuditType.CREATE, attributeValueB, userName ).toString() )
            .klass( TrackedEntityAttributeValue.class.getName() )
            .attributes( auditAttributesB )
            .createdAt( LocalDateTime.now() )
            .createdBy( userName )
            .build();

        auditRepository.save( auditB );

        List<TrackedEntityAttributeValueAudit> check = attributeValueAuditService
            .getTrackedEntityAttributeValueAudits( Lists.newArrayList( attributeA ),
                Lists.newArrayList( entityInstanceA ), org.hisp.dhis.common.AuditType.CREATE );

        assertEquals( 1, check.size() );
        assertEquals( attributeA.getUid(), check.get( 0 ).getAttribute().getUid() );
    }

    public void testUpdateAttributeValueAudit()
    {
        AuditAttributes auditAttributesA = new AuditAttributes();
        auditAttributesA.put( "trackedEntityInstance", entityInstanceA.getUid() );
        auditAttributesA.put( "trackedEntityAttribute", attributeA.getUid() );

        attributeValueA = new TrackedEntityAttributeValue( attributeA, entityInstanceA, "A" );
        Audit auditA = Audit.builder().auditScope( AuditScope.TRACKER )
            .auditType( AuditType.CREATE )
            .data( auditObjectFactory.create( AuditScope.TRACKER, AuditType.CREATE, attributeValueA, userName ).toString() )
            .klass( TrackedEntityAttributeValue.class.getName() )
            .attributes( auditAttributesA )
            .createdAt( LocalDateTime.now() )
            .createdBy( userName )
            .build();

        auditRepository.save( auditA );

        AuditAttributes auditAttributesB = new AuditAttributes();
        auditAttributesA.put( "trackedEntityInstance", entityInstanceB.getUid() );
        auditAttributesA.put( "trackedEntityAttribute", attributeB.getUid() );
        Audit auditB = Audit.builder().auditScope( AuditScope.TRACKER )
            .auditType( AuditType.UPDATE )
            .data( auditObjectFactory.create( AuditScope.TRACKER, AuditType.CREATE, attributeValueB, userName ).toString() )
            .klass( TrackedEntityAttributeValue.class.getName() )
            .attributes( auditAttributesB )
            .createdAt( LocalDateTime.now() )
            .createdBy( userName )
            .build();

        auditRepository.save( auditB );

        List<TrackedEntityAttributeValueAudit> check = attributeValueAuditService
            .getTrackedEntityAttributeValueAudits( null,
                null, org.hisp.dhis.common.AuditType.UPDATE );

        assertEquals( 1, check.size() );
        assertEquals( attributeB.getUid(), check.get( 0 ).getAttribute().getUid() );
    }
}
