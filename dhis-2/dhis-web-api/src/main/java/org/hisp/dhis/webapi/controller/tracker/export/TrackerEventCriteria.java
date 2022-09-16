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

import static org.hisp.dhis.webapi.controller.event.webrequest.tracker.FieldTranslatorSupport.translate;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

/**
 * Represents query parameters sent to {@link TrackerEventsExportController}.
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Data
@NoArgsConstructor
class TrackerEventCriteria extends PagingAndSortingCriteriaAdapter
{
    private String program;

    private String programStage;

    private ProgramStatus programStatus;

    private Boolean followUp;

    private String trackedEntity;

    private String orgUnit;

    private OrganisationUnitSelectionMode ouMode;

    private AssignedUserSelectionMode assignedUserMode;

    private String assignedUser;

    private Date occurredAfter;

    private Date occurredBefore;

    private Date scheduledAfter;

    private Date scheduledBefore;

    private Date updatedAfter;

    private Date updatedBefore;

    private String updatedWithin;

    private Date enrollmentEnrolledBefore;

    private Date enrollmentEnrolledAfter;

    private Date enrollmentOccurredBefore;

    private Date enrollmentOccurredAfter;

    private EventStatus status;

    private String attributeCc;

    private String attributeCos;

    private boolean skipMeta;

    private String attachment;

    private boolean includeDeleted;

    private String event;

    private Boolean skipEventId;

    private Set<String> filter = new HashSet<>();

    private Set<String> filterAttributes = new HashSet<>();

    private Set<String> enrollments;

    private IdSchemes idSchemes = new IdSchemes();

    @Override
    public boolean isLegacy()
    {
        return false;
    }

    @Override
    public Optional<String> translateField( String dtoFieldName, boolean isLegacy )
    {
        return isLegacy ? translate( dtoFieldName, TrackerEventCriteria.LegacyDtoToEntityFieldTranslator.values() )
            : translate( dtoFieldName, TrackerEventCriteria.DtoToEntityFieldTranslator.values() );
    }

    /**
     * Dto to database field translator for new tracker Enrollment export
     * controller
     */
    @RequiredArgsConstructor
    private enum DtoToEntityFieldTranslator implements EntityNameSupplier
    {
        /**
         * this enum names must be the same as
         * org.hisp.dhis.tracker.domain.Event fields, just with different case
         *
         * example: org.hisp.dhis.tracker.domain.Event.updatedAtClient -->
         * UPDATED_AT_CLIENT
         */
        OCCURRED_AT( "eventDate" ),
        SCHEDULED_AT( "dueDate" ),
        CREATED_AT( "created" ),
        UPDATED_AT( "lastUpdated" ),
        COMPLETED_AT( "completedDate" );

        @Getter
        private final String entityName;

    }

    /**
     * Dto to database field translator for old tracker Enrollment export
     * controller
     */
    @RequiredArgsConstructor
    private enum LegacyDtoToEntityFieldTranslator implements EntityNameSupplier
    {
        /**
         * this enum names must be the same as org.hisp.dhis.dxf2.events.Event
         * fields, just with different case
         *
         * example: org.hisp.dhis.dxf2.events.Event.lastUpdated --> LAST_UPDATED
         */
        EVENT( "uid" );

        @Getter
        private final String entityName;

    }
}
