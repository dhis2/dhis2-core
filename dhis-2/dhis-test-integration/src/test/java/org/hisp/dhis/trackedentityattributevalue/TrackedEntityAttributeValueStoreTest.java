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
package org.hisp.dhis.trackedentityattributevalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityAttributeValueStoreTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private TrackedEntityAttributeValueStore attributeValueStore;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueAuditStore attributeValueAuditStore;

    @Autowired
    private RenderService renderService;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private TrackedEntityAttribute attributeC;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private TrackedEntityInstance entityInstanceC;

    private TrackedEntityInstance entityInstanceD;

    private TrackedEntityAttributeValue attributeValueA;

    private TrackedEntityAttributeValue attributeValueB;

    private TrackedEntityAttributeValue attributeValueC;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        entityInstanceA = createTrackedEntityInstance( organisationUnit );
        entityInstanceB = createTrackedEntityInstance( organisationUnit );
        entityInstanceC = createTrackedEntityInstance( organisationUnit );
        entityInstanceD = createTrackedEntityInstance( organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceC );
        entityInstanceService.addTrackedEntityInstance( entityInstanceD );
        attributeA = createTrackedEntityAttribute( 'A' );
        attributeB = createTrackedEntityAttribute( 'B' );
        attributeC = createTrackedEntityAttribute( 'C' );
        attributeService.addTrackedEntityAttribute( attributeA );
        attributeService.addTrackedEntityAttribute( attributeB );
        attributeService.addTrackedEntityAttribute( attributeC );
        attributeValueA = new TrackedEntityAttributeValue( attributeA, entityInstanceA, "A" );
        attributeValueA.setAutoFields();
        attributeValueB = new TrackedEntityAttributeValue( attributeB, entityInstanceA, "B" );
        attributeValueB.setAutoFields();
        attributeValueC = new TrackedEntityAttributeValue( attributeA, entityInstanceB, "C" );
        attributeValueC.setAutoFields();
    }

    @Test
    void testSaveTrackedEntityAttributeValue()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueB );
        assertNotNull( attributeValueStore.get( entityInstanceA, attributeA ) );
        assertNotNull( attributeValueStore.get( entityInstanceA, attributeA ) );
    }

    @Test
    void testDeleteTrackedEntityAttributeValueByEntityInstance()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueB );
        attributeValueStore.saveVoid( attributeValueC );
        assertNotNull( attributeValueStore.get( entityInstanceA, attributeA ) );
        assertNotNull( attributeValueStore.get( entityInstanceA, attributeB ) );
        assertNotNull( attributeValueStore.get( entityInstanceB, attributeA ) );
        attributeValueStore.deleteByTrackedEntityInstance( entityInstanceA );
        assertNull( attributeValueStore.get( entityInstanceA, attributeA ) );
        assertNull( attributeValueStore.get( entityInstanceA, attributeB ) );
        assertNotNull( attributeValueStore.get( entityInstanceB, attributeA ) );
        attributeValueStore.deleteByTrackedEntityInstance( entityInstanceB );
        assertNull( attributeValueStore.get( entityInstanceA, attributeA ) );
        assertNull( attributeValueStore.get( entityInstanceA, attributeB ) );
        assertNull( attributeValueStore.get( entityInstanceB, attributeA ) );
    }

    @Test
    void testGetTrackedEntityAttributeValue()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueC );
        assertEquals( attributeValueA, attributeValueStore.get( entityInstanceA, attributeA ) );
        assertEquals( attributeValueC, attributeValueStore.get( entityInstanceB, attributeA ) );
    }

    @Test
    void testGetByEntityInstance()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueB );
        attributeValueStore.saveVoid( attributeValueC );
        List<TrackedEntityAttributeValue> attributeValues = attributeValueStore.get( entityInstanceA );
        assertEquals( 2, attributeValues.size() );
        assertTrue( equals( attributeValues, attributeValueA, attributeValueB ) );
        attributeValues = attributeValueStore.get( entityInstanceB );
        assertEquals( 1, attributeValues.size() );
        assertTrue( equals( attributeValues, attributeValueC ) );
    }

    @Test
    void testGetTrackedEntityAttributeValuesbyEntityInstanceList()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueB );
        attributeValueStore.saveVoid( attributeValueC );
        List<TrackedEntityInstance> entityInstances = new ArrayList<>();
        entityInstances.add( entityInstanceA );
        entityInstances.add( entityInstanceB );
        List<TrackedEntityAttributeValue> attributeValues = attributeValueStore.get( entityInstances );
        assertEquals( 3, attributeValues.size() );
        assertTrue( equals( attributeValues, attributeValueA, attributeValueB, attributeValueC ) );
    }

    @Test
    void testSearchTrackedEntityAttributeValue()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueB );
        attributeValueStore.saveVoid( attributeValueC );
        List<TrackedEntityAttributeValue> attributeValues = attributeValueStore.searchByValue( attributeA, "A" );
        assertTrue( equals( attributeValues, attributeValueA ) );
    }

    @Test
    void testAudit()
    {
        attributeValueStore.saveVoid( attributeValueA );
        attributeValueStore.saveVoid( attributeValueB );
        TrackedEntityAttributeValueAudit auditA = new TrackedEntityAttributeValueAudit( attributeValueA,
            renderService.toJsonAsString( attributeValueA ), "userA", AuditType.UPDATE );
        TrackedEntityAttributeValueAudit auditB = new TrackedEntityAttributeValueAudit( attributeValueB,
            renderService.toJsonAsString( attributeValueB ), "userA", AuditType.UPDATE );
        attributeValueAuditStore.addTrackedEntityAttributeValueAudit( auditA );
        attributeValueAuditStore.addTrackedEntityAttributeValueAudit( auditB );
        assertEquals( 1,
            attributeValueAuditStore
                .getTrackedEntityAttributeValueAudits( Lists.newArrayList( attributeValueA.getAttribute() ),
                    Lists.newArrayList( attributeValueA.getEntityInstance() ), AuditType.UPDATE )
                .size() );
        assertEquals( 2,
            attributeValueAuditStore
                .getTrackedEntityAttributeValueAudits( null, Lists.newArrayList( entityInstanceA ), AuditType.UPDATE )
                .size() );
    }
}
