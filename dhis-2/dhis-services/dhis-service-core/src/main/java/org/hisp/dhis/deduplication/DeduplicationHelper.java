/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.deduplication;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DeduplicationHelper
{
    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final RelationshipService relationshipService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final OrganisationUnitService organisationUnitService;

    public boolean hasInvalidReference( DeduplicationMergeParams params )
    {
        TrackedEntityInstance original = params.getOriginal();
        TrackedEntityInstance duplicate = params.getDuplicate();
        MergeObject mergeObject = params.getMergeObject();

        Set<String> validTrackedEntityAttributes = duplicate.getTrackedEntityAttributeValues().stream()
            .map( teav -> teav.getAttribute().getUid() ).collect( Collectors.toSet() );

        Set<String> validRelationships = duplicate.getRelationshipItems().stream()
            .map( rel -> rel.getTrackedEntityInstance().getUid() ).collect( Collectors.toSet() );

        for ( String tea : mergeObject.getTrackedEntityAttributes() )
        {
            if ( !validTrackedEntityAttributes.contains( tea ) )
            {
                return true;
            }
        }

        for ( String rel : mergeObject.getRelationships() )
        {
            if ( original.getUid().equals( rel ) || !validRelationships.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    public MergeObject generateMergeObject( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        return null;
    }

    public boolean hasUserAccess( TrackedEntityInstance original, TrackedEntityInstance duplicate,
        MergeObject mergeObject )
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null || !(user.isAuthorized( "ALL" ) || user.isAuthorized( "F_TRACKED_ENTITY_MERGE" )) )
        {
            return false;
        }

        if ( !aclService.canDataWrite( user, original.getTrackedEntityType() ) ||
            !aclService.canDataWrite( user, duplicate.getTrackedEntityType() ) )
        {
            return false;
        }

        List<RelationshipType> relationshipTypes = relationshipService
            .getRelationships( mergeObject.getRelationships() )
            .stream()
            .map( r -> r.getRelationshipType() )
            .distinct()
            .collect( Collectors.toList() );

        if ( relationshipTypes.stream().anyMatch( rt -> !aclService.canDataWrite( user, rt ) ) )
        {
            return false;
        }

        List<TrackedEntityAttribute> trackedEntityAttributes = trackedEntityAttributeService
            .getTrackedEntityAttributes( mergeObject.getTrackedEntityAttributes() );

        if ( trackedEntityAttributes.stream().anyMatch( attr -> !aclService.canDataWrite( user, attr ) ) )
        {
            return false;
        }

        if ( !organisationUnitService.isInUserHierarchyCached( user, original.getOrganisationUnit() ) ||
            !organisationUnitService.isInUserHierarchyCached( user, duplicate.getOrganisationUnit() ) )
        {
            return false;
        }

        return true;
    }
}
