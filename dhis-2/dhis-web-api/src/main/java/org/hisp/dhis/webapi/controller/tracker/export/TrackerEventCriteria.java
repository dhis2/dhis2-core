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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

/**
 * Represents query parameters sent to {@link TrackerEventsExportController}.
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Getter
@ToString
@EqualsAndHashCode
class TrackerEventCriteria extends PagingAndSortingCriteriaAdapter
{
    private final String program;

    private final String programStage;

    private final ProgramStatus programStatus;

    private final boolean followUp;

    private final String trackedEntity;

    private final String orgUnit;

    private final OrganisationUnitSelectionMode ouMode;

    private final AssignedUserSelectionMode assignedUserMode;

    private final String assignedUser;

    private final Date occurredAfter;

    private final Date occurredBefore;

    private final Date scheduledAfter;

    private final Date scheduledBefore;

    private final Date updatedAfter;

    private final Date updatedBefore;

    private final String updatedWithin;

    private final Date enrollmentEnrolledBefore;

    private final Date enrollmentEnrolledAfter;

    private final Date enrollmentOccurredBefore;

    private final Date enrollmentOccurredAfter;

    private final EventStatus status;

    private final String attributeCc;

    private final String attributeCos;

    private final boolean skipMeta;

    private final String attachment;

    private final boolean includeDeleted;

    private final String event;

    private final boolean skipEventId;

    private Set<String> filter = new HashSet<>();

    private Set<String> filterAttributes = new HashSet<>();

    private Set<String> enrollments = new HashSet<>();

    private final IdSchemes idSchemes = new IdSchemes();

    @Builder
    public TrackerEventCriteria( String program, String programStage, ProgramStatus programStatus, boolean followUp,
        String trackedEntity, String orgUnit, OrganisationUnitSelectionMode ouMode,
        AssignedUserSelectionMode assignedUserMode, String assignedUser, Date occurredAfter, Date occurredBefore,
        Date scheduledAfter, Date scheduledBefore, Date updatedAfter, Date updatedBefore, String updatedWithin,
        Set<String> enrollments, Date enrollmentEnrolledBefore, Date enrollmentEnrolledAfter,
        Date enrollmentOccurredBefore, Date enrollmentOccurredAfter, EventStatus status, String attributeCc,
        String attributeCos, boolean skipMeta, String attachment, boolean includeDeleted, String event,
        boolean skipEventId, Set<String> filter, Set<String> filterAttributes, Integer page, Integer pageSize,
        boolean totalPages, boolean skipPaging, List<OrderCriteria> order, boolean isLegacy )
    {
        // explicit constructor is necessary since this class extends another
        // class
        super( page, pageSize, totalPages, skipPaging, order, isLegacy );
        this.program = program;
        this.programStage = programStage;
        this.programStatus = programStatus;
        this.followUp = followUp;
        this.trackedEntity = trackedEntity;
        this.orgUnit = orgUnit;
        this.ouMode = ouMode;
        this.assignedUserMode = assignedUserMode;
        this.assignedUser = assignedUser;
        this.occurredAfter = occurredAfter;
        this.occurredBefore = occurredBefore;
        this.scheduledAfter = scheduledAfter;
        this.scheduledBefore = scheduledBefore;
        this.updatedAfter = updatedAfter;
        this.updatedBefore = updatedBefore;
        this.updatedWithin = updatedWithin;
        this.enrollmentEnrolledBefore = enrollmentEnrolledBefore;
        this.enrollmentEnrolledAfter = enrollmentEnrolledAfter;
        this.enrollmentOccurredBefore = enrollmentOccurredBefore;
        this.enrollmentOccurredAfter = enrollmentOccurredAfter;
        this.status = status;
        this.attributeCc = attributeCc;
        this.attributeCos = attributeCos;
        this.skipMeta = skipMeta;
        this.attachment = attachment;
        this.includeDeleted = includeDeleted;
        this.event = event;
        this.skipEventId = skipEventId;
        // necessary since @Builder.Default does not work with an explicit
        // constructor
        this.filter = Optional.ofNullable( filter ).orElse( this.filter );
        this.filterAttributes = Optional.ofNullable( filterAttributes ).orElse( this.filterAttributes );
        this.enrollments = Optional.ofNullable( enrollments ).orElse( this.enrollments );
    }

    public Set<String> getFilter()
    {
        return Collections.unmodifiableSet( this.filter );
    }

    public Set<String> getFilterAttributes()
    {
        return Collections.unmodifiableSet( this.filterAttributes );
    }

    public Set<String> getEnrollments()
    {
        return Collections.unmodifiableSet( this.enrollments );
    }

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
         * <br/>
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
         * fields, just with different case <br/>
         * example: org.hisp.dhis.dxf2.events.Event.lastUpdated --> LAST_UPDATED
         */
        EVENT( "uid" );

        @Getter
        private final String entityName;

    }
}
