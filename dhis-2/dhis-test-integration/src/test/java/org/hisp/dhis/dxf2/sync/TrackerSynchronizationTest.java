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
package org.hisp.dhis.dxf2.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 */
class TrackerSynchronizationTest extends NonTransactionalIntegrationTest
{
    @Autowired
    private UserService _userService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TrackedEntityInstanceService subject;

    private TrackedEntityInstanceQueryParams queryParams;

    private TrackedEntityInstanceParams params;

    private void prepareDataForTest()
    {
        TrackedEntityAttribute teaA = createTrackedEntityAttribute( 'a' );
        TrackedEntityAttribute teaB = createTrackedEntityAttribute( 'b' );
        teaB.setSkipSynchronization( true );
        manager.save( teaA );
        manager.save( teaB );
        TrackedEntityType tet = createTrackedEntityType( 'a' );
        TrackedEntityTypeAttribute tetaA = new TrackedEntityTypeAttribute( tet, teaA, true, false );
        TrackedEntityTypeAttribute tetaB = new TrackedEntityTypeAttribute( tet, teaB, true, false );
        tet.getTrackedEntityTypeAttributes().add( tetaA );
        tet.getTrackedEntityTypeAttributes().add( tetaB );
        manager.save( tet );
        OrganisationUnit ou = createOrganisationUnit( 'a' );
        manager.save( ou );
        TrackedEntityInstance tei = createTrackedEntityInstance( 'a', ou, teaA );
        tei.setTrackedEntityType( tet );
        TrackedEntityAttributeValue teavB = createTrackedEntityAttributeValue( 'b', tei, teaB );
        TrackedEntityAttributeValue teavA = createTrackedEntityAttributeValue( 'a', tei, teaA );
        manager.save( tei );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teavA );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( teavB );
        tei.getTrackedEntityAttributeValues().addAll( List.of( teavA, teavB ) );
        manager.update( tei );
    }

    @Override
    public void setUpTest()
    {
        userService = _userService;
        User user = createUserWithAuth( "userUID0001" );
        manager.save( user );
        prepareSyncParams();
        prepareDataForTest();
    }

    private void prepareSyncParams()
    {
        queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setIncludeDeleted( true );
        queryParams.setSynchronizationQuery( true );
        params = new TrackedEntityInstanceParams( false, false, false, false, true, true );
    }

    @Test
    void testSkipSyncFunctionality()
    {
        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> fetchedTeis = subject
            .getTrackedEntityInstances( queryParams, params, true, true );
        assertEquals( 1, fetchedTeis.get( 0 ).getAttributes().size() );
    }
}
