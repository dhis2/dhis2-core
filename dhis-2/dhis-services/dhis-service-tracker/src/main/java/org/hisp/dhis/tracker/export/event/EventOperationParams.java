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
package org.hisp.dhis.tracker.export.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;

@Getter
@Builder( toBuilder = true )
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class EventOperationParams
{
    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_PAGE_SIZE = 50;

    private String programUid;

    private String programStageUid;

    private ProgramStatus programStatus;

    private ProgramType programType;

    private Boolean followUp;

    private String orgUnitUid;

    private OrganisationUnitSelectionMode orgUnitSelectionMode;

    private AssignedUserSelectionMode assignedUserMode;

    private Set<String> assignedUsers;

    private String trackedEntityUid;

    private Date startDate;

    private Date endDate;

    private EventStatus eventStatus;

    private Date updatedAfter;

    private Date updatedBefore;

    /**
     * The last updated duration filter.
     */
    private String updatedWithin;

    private Date scheduledAfter;

    private Date scheduledBefore;

    private Date enrollmentEnrolledBefore;

    private Date enrollmentEnrolledAfter;

    private Date enrollmentOccurredBefore;

    private Date enrollmentOccurredAfter;

    private String attributeCategoryCombo;

    @Builder.Default
    private Set<String> attributeCategoryOptions = Collections.emptySet();

    private CategoryOptionCombo categoryOptionCombo;

    @Builder.Default
    private IdSchemes idSchemes = new IdSchemes();

    private Integer page;

    private Integer pageSize;

    private boolean totalPages;

    private boolean skipPaging;

    private boolean includeRelationships;

    @Builder.Default
    private List<OrderParam> orders = new ArrayList<>();

    @Builder.Default
    private List<OrderCriteria> attributeOrders = new ArrayList<>();

    private boolean includeAttributes;

    private boolean includeAllDataElements;

    @Builder.Default
    private Set<String> events = new HashSet<>();

    private Boolean skipEventId;

    /**
     * Filters for the response.
     */
    @Builder.Default
    private Set<String> filters = new HashSet<>();

    @Builder.Default
    private Set<String> filterAttributes = new HashSet<>();

    private boolean includeDeleted;

    private Set<String> accessiblePrograms;

    private Set<String> accessibleProgramStages;

    private boolean synchronizationQuery;

    /**
     * Indicates a point in the time used to decide the data that should not be
     * synchronized
     */
    private Date skipChangedBefore;

    private Set<String> enrollments;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isPaging()
    {
        return page != null || pageSize != null;
    }

    public int getPageWithDefault()
    {
        return page != null && page > 0 ? page : DEFAULT_PAGE;
    }

    public int getPageSizeWithDefault()
    {
        return pageSize != null && pageSize >= 0 ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Sets paging properties to default values.
     */
    public void setDefaultPaging()
    {
        this.page = DEFAULT_PAGE;
        this.pageSize = DEFAULT_PAGE_SIZE;
        this.skipPaging = false;
    }

    /**
     * Indicates whether this parameters specifies a last updated start date.
     */
    public boolean hasUpdatedAtStartDate()
    {
        return updatedAfter != null;
    }

    /**
     * Indicates whether this parameters specifies a last updated end date.
     */
    public boolean hasUpdatedAtEndDate()
    {
        return updatedBefore != null;
    }

    /**
     * Indicates whether this parameters has a UpdatedAtDuration filter.
     */
    public boolean hasUpdatedAtDuration()
    {
        return updatedWithin != null;
    }

}