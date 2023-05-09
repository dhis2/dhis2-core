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
package org.hisp.dhis.webapi.controller.event.webrequest;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;

/**
 * @author Luciano Fiandesio
 */
@Data
@NoArgsConstructor
public class TrackedEntityInstanceCriteria extends PagingAndSortingCriteriaAdapter
{
    private String query;

    private Set<String> attribute;

    private Set<String> filter;

    /**
     * Semicolon-delimited list of Organizational Unit UIDs
     */
    private String ou;

    /**
     * Selection mode for the specified organisation units, default is
     * ACCESSIBLE.
     */
    private OrganisationUnitSelectionMode ouMode;

    /**
     * a Program UID for which instances in the response must be enrolled in.
     */
    private String program;

    /**
     * The {@see ProgramStatus} of the Tracked Entity Instance in the given
     * program.
     */
    private ProgramStatus programStatus;

    /**
     * Indicates whether the Tracked Entity Instance is marked for follow up for
     * the specified Program.
     */
    private Boolean followUp;

    /**
     * Start date for last updated.
     */
    private Date lastUpdatedStartDate;

    /**
     * End date for last updated.
     */
    private Date lastUpdatedEndDate;

    /**
     * The last updated duration filter.
     */
    private String lastUpdatedDuration;

    /**
     * The given Program start date.
     */
    private Date programStartDate;

    /**
     * The given Program end date.
     */
    private Date programEndDate;

    /**
     * Start date for enrollment in the given program.
     */
    private Date programEnrollmentStartDate;

    /**
     * End date for enrollment in the given program.
     */
    private Date programEnrollmentEndDate;

    /**
     * Start date for incident in the given program.
     */
    private Date programIncidentStartDate;

    /**
     * End date for incident in the given program.
     */
    private Date programIncidentEndDate;

    /**
     * Only returns Tracked Entity Instances of this type.
     */
    private String trackedEntityType;

    /**
     * Semicolon-delimited list of Tracked Entity Instance UIDs
     */
    private String trackedEntityInstance;

    /**
     * Selection mode for user assignment of events.
     */
    private AssignedUserSelectionMode assignedUserMode;

    /**
     * Semicolon-delimited list of user UIDs to filter based on events assigned
     * to the users.
     */
    private String assignedUser;

    /**
     * Program Stage UID, used for filtering TEIs based on the selected Program
     * Stage
     */
    private String programStage;

    /**
     * Status of any events in the specified program.
     */
    private EventStatus eventStatus;

    /**
     * Start date for Event for the given Program.
     */
    private Date eventStartDate;

    /**
     * End date for Event for the given Program.
     */
    private Date eventEndDate;

    /**
     * Indicates whether not to include meta data in the response.
     */
    private boolean skipMeta;

    /**
     * Indicates whether to include soft-deleted elements
     */
    private boolean includeDeleted;

    /**
     * Indicates whether to include all TEI attributes
     */
    private boolean includeAllAttributes;

    /**
     * Potential Duplicate value for TEI. If null, we don't check whether a TEI
     * is a potentialDuplicate or not
     */
    private Boolean potentialDuplicate;

    /**
     * The file name in case of exporting as file
     */
    private String attachment;

    public Set<String> getOrgUnits()
    {
        return ou != null ? TextUtils.splitToSet( ou, TextUtils.SEMICOLON ) : new HashSet<>();
    }

    public Set<String> getAssignedUsers()
    {
        Set<String> assignedUsers = new HashSet<>();

        if ( assignedUser != null && !assignedUser.isEmpty() )
        {
            assignedUsers = TextUtils.splitToSet( assignedUser, TextUtils.SEMICOLON ).stream()
                .filter( CodeGenerator::isValidUid ).collect( Collectors.toSet() );
        }

        return assignedUsers;
    }

    public boolean hasTrackedEntity()
    {
        return StringUtils.isNotEmpty( this.trackedEntityInstance );
    }

    public Set<String> getTrackedEntityInstances()
    {
        if ( hasTrackedEntity() )
        {
            return TextUtils.splitToSet( trackedEntityInstance, TextUtils.SEMICOLON );
        }
        return new HashSet<>();
    }
}
