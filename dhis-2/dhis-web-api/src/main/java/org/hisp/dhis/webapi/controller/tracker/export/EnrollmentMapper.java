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
package org.hisp.dhis.webapi.controller.tracker.export;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.Note;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper( uses = {
    RelationshipMapper.class,
    AttributeMapper.class,
    InstantMapper.class,
    UserMapper.class } )
interface EnrollmentMapper extends ViewMapper<ProgramInstance, Enrollment>
{
    @Mapping( target = "enrollment", source = "uid" )
    @Mapping( target = "createdAt", source = "created" )
    @Mapping( target = "createdAtClient", source = "createdAtClient" )
    @Mapping( target = "updatedAt", source = "lastUpdated" )
    @Mapping( target = "updatedAtClient", source = "lastUpdatedAtClient" )
    @Mapping( target = "trackedEntity", source = "entityInstance.uid" )
    @Mapping( target = "program", source = "program.uid" )
    @Mapping( target = "orgUnit", source = "organisationUnit.uid" )
    @Mapping( target = "orgUnitName", source = "organisationUnit.name" )
    @Mapping( target = "enrolledAt", source = "enrollmentDate" )
    @Mapping( target = "occurredAt", source = "incidentDate" )
    @Mapping( target = "followUp", source = "followup" )
    @Mapping( target = "completedAt", source = "endDate" )
    @Mapping( target = "createdBy", source = "createdByUserInfo" )
    @Mapping( target = "updatedBy", source = "lastUpdatedByUserInfo" )
    @Mapping( target = "events", source = "programStageInstances" )
    @Mapping( target = "relationships", source = "relationshipItems" )
    @Mapping( target = "attributes", source = "entityInstance.trackedEntityAttributeValues" )
    @Mapping( target = "notes", source = "comments" )
    @Override
    Enrollment from( ProgramInstance programInstance );

    /**
     * Maps {@link ProgramInstance#getRelationshipItems()} to
     * {@link org.hisp.dhis.relationship.Relationship} which is then mapped by
     * {@link RelationshipMapper}.
     *
     */
    default Set<org.hisp.dhis.relationship.Relationship> map(
        Set<org.hisp.dhis.relationship.RelationshipItem> relationshipItems )
    {
        if ( relationshipItems == null )
        {
            return Set.of();
        }

        return relationshipItems.stream().map( org.hisp.dhis.relationship.RelationshipItem::getRelationship )
            .collect( Collectors.toSet() );
    }

    @Mapping( target = "event", source = "uid" )
    @Mapping( target = "program", source = "programInstance.program.uid" )
    @Mapping( target = "programStage", source = "programStage.uid" )
    @Mapping( target = "enrollment", source = "programInstance.uid" )
    @Mapping( target = "trackedEntity", source = "programInstance.entityInstance.uid" )
    @Mapping( target = "orgUnit", source = "organisationUnit.uid" )
    @Mapping( target = "orgUnitName", source = "organisationUnit.name" )
    @Mapping( target = "occurredAt", source = "executionDate" )
    @Mapping( target = "scheduledAt", source = "dueDate" )
    @Mapping( target = "followup", source = "programInstance.followup" )
    @Mapping( target = "createdAt", source = "created" )
    @Mapping( target = "createdAtClient", source = "createdAtClient" )
    @Mapping( target = "updatedAt", source = "lastUpdated" )
    @Mapping( target = "updatedAtClient", source = "lastUpdatedAtClient" )
    @Mapping( target = "attributeOptionCombo", source = "attributeOptionCombo.uid" )
    @Mapping( target = "attributeCategoryOptions", source = "attributeOptionCombo.categoryOptions" )
    @Mapping( target = "completedAt", source = "completedDate" )
    @Mapping( target = "createdBy", source = "createdByUserInfo" )
    @Mapping( target = "updatedBy", source = "lastUpdatedByUserInfo" )
    @Mapping( target = "relationships", source = "relationshipItems" )
    @Mapping( target = "dataValues", source = "eventDataValues" )
    @Mapping( target = "notes", source = "comments" )
    Event from( org.hisp.dhis.program.ProgramStageInstance event );

    @Mapping( target = "note", source = "uid" )
    @Mapping( target = "storedAt", source = "created" )
    @Mapping( target = "value", source = "commentText" )
    @Mapping( target = "createdBy", source = "lastUpdatedBy" )
    @Mapping( target = "storedBy", source = "creator" )
    Note from( org.hisp.dhis.trackedentitycomment.TrackedEntityComment comment );

    // NOTE: right now we only support categoryOptionComboIdScheme on export. If we were to add a categoryOptionIdScheme
    // we could not simply export the UIDs.
    default String from( Set<CategoryOption> categoryOptions )
    {
        if ( categoryOptions == null || categoryOptions.isEmpty() )
        {
            return "";
        }

        return categoryOptions.stream()
            .map( CategoryOption::getUid )
            .collect( Collectors.joining( ";" ) );
    }

    @Mapping( target = "createdAt", source = "created" )
    @Mapping( target = "updatedAt", source = "lastUpdated" )
    @Mapping( target = "createdBy", source = "createdByUserInfo" )
    @Mapping( target = "updatedBy", source = "lastUpdatedByUserInfo" )
    DataValue from( org.hisp.dhis.eventdatavalue.EventDataValue dataValue );
}
