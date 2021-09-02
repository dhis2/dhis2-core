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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
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

    private ProgramInstanceService programInstanceService;

    public String getInvalidReferenceErrors( DeduplicationMergeParams params )
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
                return "Invalid attribute '" + tea + "'";
            }
        }

        for ( String rel : mergeObject.getRelationships() )
        {
            if ( original.getUid().equals( rel ) || !validRelationships.contains( rel ) )
            {
                return "Invalid relationship '" + rel + "'";
            }
        }

        Set<String> programs = programInstanceService.getProgramInstances( mergeObject.getEnrollments() )
            .stream()
            .map( e -> e.getProgram().getUid() )
            .collect( Collectors.toSet() );

        for ( ProgramInstance programInstance : original.getProgramInstances() )
        {
            if ( programs.contains( programInstance.getProgram().getUid() ) )
            {
                return "Invalid enrollment '" + programInstance.getUid() +
                    "'. Can not merge when both tracked entities are enrolled in the same program.";
            }
        }

        return null;
    }

    public MergeObject generateMergeObject( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        if ( !duplicate.getTrackedEntityType().equals( original.getTrackedEntityType() ) )
        {
            throw new PotentialDuplicateForbiddenException(
                "Potentical Duplicate does not have the same tracked entity type as the original" );
        }

        List<String> attributes = getMergeableAttributes( original, duplicate );

        List<String> relationships = getMergeableRelationships( original, duplicate );

        List<String> enrollments = getMergeableEnrollments( original, duplicate );

        return MergeObject.builder()
            .trackedEntityAttributes( attributes )
            .relationships( relationships )
            .enrollments( enrollments )
            .build();
    }

    public String getUserAccessErrors( TrackedEntityInstance original, TrackedEntityInstance duplicate,
        MergeObject mergeObject )
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null || !(user.isAuthorized( "ALL" ) || user.isAuthorized( "F_TRACKED_ENTITY_MERGE" )) )
        {
            return "Missing required authority for merging tracked entities.";
        }

        if ( !aclService.canDataWrite( user, original.getTrackedEntityType() ) ||
            !aclService.canDataWrite( user, duplicate.getTrackedEntityType() ) )
        {
            return "Missing data write access to Tracked Entity Type.";
        }

        List<RelationshipType> relationshipTypes = relationshipService
            .getRelationships( mergeObject.getRelationships() )
            .stream()
            .map( Relationship::getRelationshipType )
            .distinct()
            .collect( Collectors.toList() );

        if ( relationshipTypes.stream().anyMatch( rt -> !aclService.canDataWrite( user, rt ) ) )
        {
            return "Missing data write access to one or more Relationship Types.";
        }

        List<TrackedEntityAttribute> trackedEntityAttributes = trackedEntityAttributeService
            .getTrackedEntityAttributes( mergeObject.getTrackedEntityAttributes() );

        if ( trackedEntityAttributes.stream().anyMatch( attr -> !aclService.canDataWrite( user, attr ) ) )
        {
            return "Missing data write access to one or more Tracked Entity Attributes.";
        }

        List<ProgramInstance> enrollments = programInstanceService.getProgramInstances( mergeObject.getEnrollments() );

        if ( enrollments.stream().anyMatch( e -> !aclService.canDataWrite( user, e ) ) )
        {
            return "Missing data write access to one or more Programs.";
        }

        if ( !organisationUnitService.isInUserHierarchyCached( user, original.getOrganisationUnit() ) ||
            !organisationUnitService.isInUserHierarchyCached( user, duplicate.getOrganisationUnit() ) )
        {
            return "Missing access to organisation unit of one or both entities.";
        }

        return null;
    }

    private List<String> getMergeableAttributes( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        Map<String, String> existingTeavs = original.getTrackedEntityAttributeValues().stream()
            .collect( Collectors.toMap( teav -> teav.getAttribute().getUid(), TrackedEntityAttributeValue::getValue ) );

        List<String> attributes = new ArrayList<>();

        for ( TrackedEntityAttributeValue teav : duplicate.getTrackedEntityAttributeValues() )
        {
            String existingVal = existingTeavs.get( teav.getAttribute().getUid() );

            if ( existingVal != null )
            {
                if ( !existingVal.equals( teav.getValue() ) )
                {
                    throw new PotentialDuplicateConflictException(
                        "Potential Duplicate contains conflicting value and cannot be merged." );
                }
            }
            else
            {
                attributes.add( teav.getAttribute().getUid() );
            }
        }

        return attributes;
    }

    private List<String> getMergeableRelationships( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        List<String> relationships = new ArrayList<>();

        for ( RelationshipItem ri : duplicate.getRelationshipItems() )
        {
            TrackedEntityInstance from = ri.getRelationship().getFrom().getTrackedEntityInstance();
            TrackedEntityInstance to = ri.getRelationship().getTo().getTrackedEntityInstance();

            if ( (from != null && from.getUid().equals( original.getUid() ))
                || (to != null && to.getUid().equals( original.getUid() )) )
            {
                continue;
            }

            relationships.add( ri.getRelationship().getUid() );
        }

        return relationships;
    }

    private List<String> getMergeableEnrollments( TrackedEntityInstance original, TrackedEntityInstance duplicate )
    {
        List<String> enrollments = new ArrayList<>();

        Set<String> programs = original.getProgramInstances()
            .stream()
            .filter( e -> !e.isDeleted() )
            .map( e -> e.getProgram().getUid() )
            .collect( Collectors.toSet() );

        for ( ProgramInstance programInstance : duplicate.getProgramInstances() )
        {
            if ( programs.contains( programInstance.getProgram().getUid() ) )
            {
                throw new PotentialDuplicateConflictException(
                    "Potential Duplicate contains enrollments with the same program" +
                        " and cannot be merged." );
            }
            enrollments.add( programInstance.getUid() );
        }

        return enrollments;
    }
}
