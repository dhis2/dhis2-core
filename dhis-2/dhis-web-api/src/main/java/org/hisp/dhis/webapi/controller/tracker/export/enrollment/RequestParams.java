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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;

/**
 * Represents query parameters sent to {@link EnrollmentsExportController}.
 */
@OpenApi.Property
@Data
@NoArgsConstructor
public class RequestParams extends PagingAndSortingCriteriaAdapter
{
    static final String DEFAULT_FIELDS_PARAM = "*,!relationships,!events,!attributes";

    /**
     * Semicolon-delimited list of organisation unit UIDs.
     *
     * @deprecated use {@link #orgUnits} instead which is comma instead of
     *             semicolon separated.
     */
    @Deprecated( since = "2.41" )
    @OpenApi.Property( { UID[].class, OrganisationUnit.class } )
    private String orgUnit;

    @OpenApi.Property( { UID[].class, OrganisationUnit.class } )
    private Set<UID> orgUnits = new HashSet<>();

    private OrganisationUnitSelectionMode ouMode;

    @OpenApi.Property( { UID.class, Program.class } )
    private String program;

    private ProgramStatus programStatus;

    private Boolean followUp;

    private Date updatedAfter;

    private String updatedWithin;

    private Date enrolledAfter;

    private Date enrolledBefore;

    @OpenApi.Property( { UID.class, TrackedEntityType.class } )
    private String trackedEntityType;

    @OpenApi.Property( { UID.class, TrackedEntity.class } )
    private String trackedEntity;

    /**
     * Semicolon-delimited list of enrollment UIDs.
     *
     * @deprecated use {@link #enrollments} instead which is comma instead of
     *             semicolon separated.
     */
    @Deprecated( since = "2.41" )
    @OpenApi.Property( { UID[].class, Enrollment.class } )
    private String enrollment;

    @OpenApi.Property( { UID[].class, Enrollment.class } )
    private Set<UID> enrollments = new HashSet<>();

    private boolean includeDeleted;

    @OpenApi.Property( value = String[].class )
    private List<FieldPath> fields = FieldFilterParser.parse( DEFAULT_FIELDS_PARAM );
}
