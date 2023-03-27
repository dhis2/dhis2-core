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
package org.hisp.dhis.programstageworkinglist;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.EventDataFilter;
import org.hisp.dhis.trackedentityfilter.AttributeValueFilter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the filtering/sorting criteria to be used when querying program
 * stage working lists.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProgramStageQueryCriteria implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Property indicating which event status types to filter
     */
    @JsonProperty
    private EventStatus eventStatus;

    /**
     * Property to filter events based on their created dates
     */
    @JsonProperty
    private DateFilterPeriod eventCreatedAt;

    /**
     * Property to filter events based on their occurred dates
     */
    @JsonProperty
    private DateFilterPeriod eventOccurredAt;

    /**
     * Property to filter events based on their scheduled dates
     */
    @JsonProperty
    private DateFilterPeriod eventScheduledAt;

    /**
     * Property indicating which enrollment status types to filter
     */
    @JsonProperty
    private ProgramStatus enrollmentStatus;

    /**
     * Property to filter events based on their enrolment dates
     */
    @JsonProperty
    private DateFilterPeriod enrolledAt;

    /**
     * Property to filter events based on enrollment incident dates
     */
    @JsonProperty
    private DateFilterPeriod enrollmentOccurredAt;

    /**
     * Property which contains the required field ordering along with its
     * direction (asc/desc)
     */
    @JsonProperty
    private String order;

    /**
     * Property which contains the order of output columns
     */
    @JsonProperty
    @Builder.Default
    private List<String> displayColumnOrder = Collections.emptyList();

    /**
     * Property indicating the OU for the filter.
     */
    @JsonProperty
    private String orgUnit;

    /**
     * Property indicating the OU selection mode for the event filter
     */
    @JsonProperty
    private OrganisationUnitSelectionMode ouMode;

    /**
     * Property indicating the assigned user selection mode for the event
     * filter.
     */
    @JsonProperty
    private AssignedUserSelectionMode assignedUserMode;

    /**
     * Property which contains the required assigned user ids to be used in the
     * event filter.
     */
    @JsonProperty
    @Builder.Default
    private Set<String> assignedUsers = Collections.emptySet();

    /**
     * Property which contains the filters to be used when querying events.
     */
    @JsonProperty
    @Builder.Default
    private List<EventDataFilter> dataFilters = Collections.emptyList();

    /**
     * Property to filter tracked entity instances based on tracked entity
     * attribute values
     */
    @JsonProperty
    @Builder.Default
    private List<AttributeValueFilter> attributeValueFilters = Collections.emptyList();
}
