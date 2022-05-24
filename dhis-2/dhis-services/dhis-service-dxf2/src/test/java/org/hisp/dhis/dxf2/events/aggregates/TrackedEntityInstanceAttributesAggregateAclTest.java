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
package org.hisp.dhis.dxf2.events.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.security.acl.AccessStringHelper.DATA_READ;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityInstanceAttributesAggregateAclTest extends TrackerTest
{

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    private User superUser;

    private User nonSuperUser;

    @Test
    void verifyTeiCantBeAccessedNoPublicAccessOnTrackedEntityType()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityTypeA );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 0 ) );
    }

    @Test
    void verifyTeiCanBeAccessedWhenDATA_READPublicAccessOnTrackedEntityType()
    {
        final String tetUid = CodeGenerator.generateUid();
        doInTransaction( () -> {
            injectSecurityContext( superUser );
            TrackedEntityType trackedEntityTypeZ = createTrackedEntityType( 'Z' );
            trackedEntityTypeZ.setUid( tetUid );
            trackedEntityTypeZ.setName( "TrackedEntityTypeZ" + trackedEntityTypeZ.getUid() );
            trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeZ );
            // When saving the trackedEntityType using addTrackedEntityType, the
            // public access value is ignored
            // therefore we need to update the previously saved TeiType
            final TrackedEntityType trackedEntityType = trackedEntityTypeService
                .getTrackedEntityType( trackedEntityTypeZ.getUid() );
            trackedEntityType.setPublicAccess( DATA_READ );
            trackedEntityTypeService.updateTrackedEntityType( trackedEntityType );
            this.persistTrackedEntityInstance( ImmutableMap.of( "trackedEntityType", trackedEntityType ) );
            this.persistTrackedEntityInstance( ImmutableMap.of( "trackedEntityType", trackedEntityType ) );
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );
        final TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType( tetUid );
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setTrackedEntityType( trackedEntityType );
        queryParams.setIncludeAllAttributes( true );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances( queryParams, params, false, true );
        assertThat( trackedEntityInstances, hasSize( 2 ) );
    }

    protected void setUserAuthorityToNonSuper( User user )
    {
        UserRole userRole = new UserRole();
        userRole.setName( "UserRole_" + user.getUsername() );
        userRole.setUid( CodeGenerator.generateUid() );
        userRole.setAuthorities( new HashSet<>( Collections.singletonList( "user" ) ) );
        user.setUserRoles( Sets.newHashSet( userRole ) );
    }
}
