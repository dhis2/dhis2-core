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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import org.hisp.dhis.program.Event;
import org.hisp.dhis.webapi.controller.tracker.export.DataValueMapper;
import org.hisp.dhis.webapi.controller.tracker.export.NoteMapper;
import org.hisp.dhis.webapi.controller.tracker.export.UserMapper;
import org.hisp.dhis.webapi.controller.tracker.export.relationship.RelationshipMapper;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.ViewMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper( uses = {
    DataValueMapper.class,
    CategoryOptionMapper.class,
    InstantMapper.class,
    NoteMapper.class,
    RelationshipMapper.class,
    UserMapper.class } )
public interface EventMapper extends ViewMapper<Event, org.hisp.dhis.webapi.controller.tracker.view.Event>
{
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
    @Mapping( target = "dataValues", source = "eventDataValues" )
    @Mapping( target = "relationships", source = "relationshipItems" )
    @Mapping( target = "notes", source = "comments" )
    org.hisp.dhis.webapi.controller.tracker.view.Event from( Event event );
}
