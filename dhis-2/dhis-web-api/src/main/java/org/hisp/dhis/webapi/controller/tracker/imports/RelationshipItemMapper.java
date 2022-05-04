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
package org.hisp.dhis.webapi.controller.tracker.imports;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.RelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper( uses = {
    AttributeMapper.class,
    DataValueMapper.class,
    NoteMapper.class,
    InstantMapper.class,
    UserMapper.class
} )
interface RelationshipItemMapper
    extends DomainMapper<RelationshipItem, org.hisp.dhis.tracker.domain.RelationshipItem>
{
    org.hisp.dhis.tracker.domain.RelationshipItem from( RelationshipItem relationshipItem,
        @Context TrackerIdSchemeParams idSchemeParams );

    org.hisp.dhis.tracker.domain.RelationshipItem.TrackedEntity from( TrackedEntity trackedEntity,
        @Context TrackerIdSchemeParams idSchemeParams );

    org.hisp.dhis.tracker.domain.RelationshipItem.Enrollment from( Enrollment enrollment,
        @Context TrackerIdSchemeParams idSchemeParams );

    org.hisp.dhis.tracker.domain.RelationshipItem.Event from( Event event,
        @Context TrackerIdSchemeParams idSchemeParams );
}
