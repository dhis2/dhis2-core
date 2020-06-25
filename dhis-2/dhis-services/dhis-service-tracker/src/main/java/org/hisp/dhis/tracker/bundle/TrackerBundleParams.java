package org.hisp.dhis.tracker.bundle;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.converter.TrackerBundleParamsConverter;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Used for setting up bundle parameters, closely modelled around {@see org.hisp.dhis.tracker.TrackerImportParams}
 * and is usually not directly used (rather created by using {@see org.hisp.dhis.tracker.TrackerImportParams#toTrackerBundleParams}.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize( converter = TrackerBundleParamsConverter.class )
public class TrackerBundleParams
{
    /**
     * User uid to use for import job.
     */
    @JsonProperty
    private String userId;

    /**
     * User to use for import job.
     */
    private User user;

    /**
     * Should import be imported or just validated.
     */
    @JsonProperty
    @Builder.Default
    private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * Should text pattern validation be skipped or not, default is not.
     */
    @JsonProperty
    private boolean skipTextPatternValidation;

    /**
     * Sets import strategy (create, update, etc).
     */
    @JsonProperty
    @Builder.Default
    private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE;

    /**
     * Identifiers to match metadata
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdentifierParams identifiers = new TrackerIdentifierParams();

    /**
     * Should import be treated as a atomic import (all or nothing).
     */
    @JsonProperty
    @Builder.Default
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Flush for every object or per type.
     */
    @JsonProperty
    @Builder.Default
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Validation mode to use, defaults to fully validated objects.
     */
    @JsonProperty
    @Builder.Default
    private ValidationMode validationMode = ValidationMode.FULL;

    /**
     * Give full report, or only include errors.
     */
    @JsonProperty
    @Builder.Default
    private TrackerBundleReportMode reportMode = TrackerBundleReportMode.ERRORS;

    /**
     * Tracked entities to import.
     */
    @JsonProperty
    @Builder.Default
    private List<TrackedEntity> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    @JsonProperty
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    @JsonProperty
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    /**
     * Relationships to import.
     */
    @JsonProperty
    @Builder.Default
    private List<Relationship> relationships = new ArrayList<>();

    @JsonProperty
    public String getUsername()
    {
        return User.username( user );
    }

    public TrackerBundle toTrackerBundle()
    {
        return TrackerBundle.builder()
            .user( user )
            .importMode( importMode )
            .importStrategy( importStrategy )
            .skipTextPatternValidation( skipTextPatternValidation )
            .flushMode( flushMode )
            .validationMode( validationMode )
            .reportMode( reportMode )
            .trackedEntities( trackedEntities )
            .enrollments( enrollments )
            .events( events )
            .relationships( relationships )
            .build();
    }

    public TrackerPreheatParams toTrackerPreheatParams()
    {
        return TrackerPreheatParams.builder()
            .userId( userId )
            .user( user )
            .identifiers( identifiers )
            .trackedEntities( trackedEntities )
            .enrollments( enrollments )
            .events( events )
            .relationships( relationships )
            .build();
    }
}
