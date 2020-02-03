package org.hisp.dhis.tracker;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "trackerImportParams", namespace = DxfNamespaces.DXF_2_0 )
public class TrackerImportParams
{
    /**
     * User uid to use for import job.
     */
    private String userId;

    /**
     * User to use for import job.
     */

    private User user;

    /**
     * Should import be imported or just validated.
     */
    private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * What identifiers to match on.
     */
    private TrackerIdentifier identifier = TrackerIdentifier.UID;

    /**
     * Sets import strategy (create, update, etc).
     */
    private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE;

    /**
     * Should import be treated as a atomic import (all or nothing).
     */
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Flush for every object or per type.
     */
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Validation mode to use, defaults to fully validated objects.
     */
    private ValidationMode validationMode = ValidationMode.FULL;

    /**
     * Name of file that was used for import (if available).
     */
    private String filename;

    /**
     * Give full report, or only include errors.
     */
    private TrackerBundleReportMode reportMode = TrackerBundleReportMode.ERRORS;

    /**
     * Job id to use for threaded imports.
     */
    private JobConfiguration jobConfiguration;

    /**
     * Tracked entities to import.
     */
    private List<TrackedEntity> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    private List<Event> events = new ArrayList<>();

    public TrackerImportParams()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUserId()
    {
        return userId;
    }

    public TrackerImportParams setUserId( String userId )
    {
        this.userId = userId;
        return this;
    }

    public User getUser()
    {
        return user;
    }

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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUsername()
    {
        return user != null ? user.getUsername() : "system-process";
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackerBundleMode getImportMode()
    {
        return importMode;
    }

    public TrackerImportParams setImportMode( TrackerBundleMode importMode )
    {
        this.importMode = importMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackerIdentifier getIdentifier()
    {
        return identifier;
    }

    public TrackerImportParams setIdentifier( TrackerIdentifier identifier )
    {
        this.identifier = identifier;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackerImportStrategy getImportStrategy()
    {
        return importStrategy;
    }

    public TrackerImportParams setImportStrategy( TrackerImportStrategy importStrategy )
    {
        this.importStrategy = importStrategy;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AtomicMode getAtomicMode()
    {
        return atomicMode;
    }

    public TrackerImportParams setAtomicMode( AtomicMode atomicMode )
    {
        this.atomicMode = atomicMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public TrackerImportParams setFlushMode( FlushMode flushMode )
    {
        this.flushMode = flushMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValidationMode getValidationMode()
    {
        return validationMode;
    }

    public TrackerImportParams setValidationMode( ValidationMode validationMode )
    {
        this.validationMode = validationMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFilename()
    {
        return filename;
    }

    public TrackerImportParams setFilename( String filename )
    {
        this.filename = filename;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackerBundleReportMode getReportMode()
    {
        return reportMode;
    }

    public TrackerImportParams setReportMode( TrackerBundleReportMode reportMode )
    {
        this.reportMode = reportMode;
        return this;
    }

    public JobConfiguration getJobConfiguration()
    {
        return jobConfiguration;
    }

    public TrackerImportParams setJobConfiguration( JobConfiguration jobConfiguration )
    {
        this.jobConfiguration = jobConfiguration;
        return this;
    }

    public boolean hasJobConfiguration()
    {
        return jobConfiguration != null;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntity> getTrackedEntities()
    {
        return trackedEntities;
    }

    public TrackerImportParams setTrackedEntities( List<TrackedEntity> trackedEntities )
    {
        this.trackedEntities = trackedEntities;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<Enrollment> getEnrollments()
    {
        return enrollments;
    }

    public TrackerImportParams setEnrollments( List<Enrollment> enrollments )
    {
        this.enrollments = enrollments;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<Event> getEvents()
    {
        return events;
    }

    public TrackerImportParams setEvents( List<Event> events )
    {
        this.events = events;
        return this;
    }

    public TrackerBundleParams toTrackerBundleParams()
    {
        return new TrackerBundleParams()
            .setUser( user )
            .setImportMode( importMode )
            .setImportStrategy( importStrategy )
            .setAtomicMode( atomicMode )
            .setFlushMode( flushMode )
            .setValidationMode( validationMode )
            .setReportMode( reportMode )
            .setTrackedEntities( trackedEntities )
            .setEnrollments( enrollments )
            .setEvents( events );
    }
}
