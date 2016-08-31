package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityAuditStoreTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityAuditStore auditStore;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private TrackedEntityAudit auditA;

    private TrackedEntityAudit auditB;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private int entityInstanceAId;

    private Date today;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceAId = entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        entityInstanceB = createTrackedEntityInstance( 'B', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        DateTime testDate = DateTime.now();
        testDate.withTimeAtStartOfDay();
        today = testDate.toDate();

        auditA = new TrackedEntityAudit( entityInstanceA, "test", today, TrackedEntityAudit.MODULE_ENTITY_INSTANCE_DASHBOARD );
        auditB = new TrackedEntityAudit( entityInstanceB, "test", today, TrackedEntityAudit.MODULE_ENTITY_INSTANCE_DASHBOARD );
    }

    @Test
    public void testGetEntityInstanceAuditsByEntityInstance()
    {
        auditStore.save( auditA );
        auditStore.save( auditB );

        List<TrackedEntityAudit> entityInstanceAudits = auditStore.get( entityInstanceA );
        assertEquals( 1, entityInstanceAudits.size() );
        assertTrue( entityInstanceAudits.contains( auditA ) );

        entityInstanceAudits = auditStore.get( entityInstanceB );
        assertEquals( 1, entityInstanceAudits.size() );
        assertTrue( entityInstanceAudits.contains( auditB ) );
    }

    @Test
    public void testGetAuditByModule()
    {
        auditStore.save( auditA );
        auditStore.save( auditB );

        TrackedEntityAudit entityInstanceAudit = auditStore.get( entityInstanceAId, "test", today,
            TrackedEntityAudit.MODULE_ENTITY_INSTANCE_DASHBOARD );
        assertEquals( auditA, entityInstanceAudit );
    }
}
