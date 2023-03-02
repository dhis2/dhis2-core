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
package org.hisp.dhis.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerImportParams
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
    private final TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * IdSchemes to match metadata
     */
    @JsonProperty
    @Builder.Default
    private final TrackerIdSchemeParams idSchemes = new TrackerIdSchemeParams();

    /**
     * Sets import strategy (create, update, etc).
     */
    @JsonProperty
    @Builder.Default
    private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE_AND_UPDATE;

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
    private final FlushMode flushMode = FlushMode.AUTO;

    /**
     * Validation mode to use, defaults to fully validated objects.
     */
    @JsonProperty
    @Builder.Default
    private final ValidationMode validationMode = ValidationMode.FULL;

    /**
     * Should text pattern validation be skipped or not, default is not.
     */
    @JsonProperty
    @Builder.Default
    private final boolean skipPatternValidation = false;

    /**
     * Should side effects be skipped or not, default is not.
     */
    @JsonProperty
    @Builder.Default
    private final boolean skipSideEffects = false;

    /**
     * Should rule engine call be skipped or not, default is to skip.
     */
    @JsonProperty
    @Builder.Default
    private final boolean skipRuleEngine = false;

    /**
     * Name of file that was used for import (if available).
     */
    @JsonProperty
    @Builder.Default
    private final String filename = null;

    /**
     * Job configuration
     */
    private JobConfiguration jobConfiguration;

    @JsonProperty
    @Builder.Default
    private TrackerBundleReportMode reportMode = TrackerBundleReportMode.ERRORS;

    /**
     * Tracked entities to import.
     */
    @JsonProperty
    @Builder.Default
    private final List<TrackedEntity> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    @JsonProperty
    @Builder.Default
    private final List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    @JsonProperty
    @Builder.Default
    private final List<Event> events = new ArrayList<>();

    /**
     * Relationships to import.
     */
    @JsonProperty
    @Builder.Default
    private final List<Relationship> relationships = new ArrayList<>();

    public TrackerImportParams setUser( User user )
    {
        this.user = user;

        if ( user != null )
        {
            this.userId = user.getUid();
        }

        return this;
    }

    @JsonProperty
    public String getUsername()
    {
        return User.username( user );
    }

    @Override
    public String toString()
    {
        return Optional.ofNullable( this.getJobConfiguration() )
            .map( jobConfiguration -> JobType.TRACKER_IMPORT_JOB + " ( " + jobConfiguration.getUid() + " )" )
            .orElse( JobType.TRACKER_IMPORT_JOB.toString() );
    }

    public String userStartInfo()
    {
        return this + " started by "
            + this.getUsername() + " ( " + this.userId + " )";
    }
}
