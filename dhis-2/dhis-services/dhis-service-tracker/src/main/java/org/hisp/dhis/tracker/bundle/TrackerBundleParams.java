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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerBundleParams
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
     * Give full report, or only include errors.
     */
    private TrackerBundleReportMode reportMode = TrackerBundleReportMode.ERRORS;

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

    public TrackerBundleParams()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUserId()
    {
        return userId;
    }

    public TrackerBundleParams setUserId( String userId )
    {
        this.userId = userId;
        return this;
    }

    public User getUser()
    {
        return user;
    }

    public TrackerBundleParams setUser( User user )
    {
        this.user = user;
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

    public TrackerBundleParams setImportMode( TrackerBundleMode importMode )
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

    public TrackerBundleParams setIdentifier( TrackerIdentifier identifier )
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

    public TrackerBundleParams setImportStrategy( TrackerImportStrategy importStrategy )
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

    public TrackerBundleParams setAtomicMode( org.hisp.dhis.tracker.AtomicMode atomicMode )
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

    public TrackerBundleParams setFlushMode( FlushMode flushMode )
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

    public TrackerBundleParams setValidationMode( ValidationMode validationMode )
    {
        this.validationMode = validationMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackerBundleReportMode getReportMode()
    {
        return reportMode;
    }

    public TrackerBundleParams setReportMode( TrackerBundleReportMode reportMode )
    {
        this.reportMode = reportMode;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntity> getTrackedEntities()
    {
        return trackedEntities;
    }

    public TrackerBundleParams setTrackedEntities( List<TrackedEntity> trackedEntities )
    {
        this.trackedEntities = trackedEntities;
        return this;
    }

    @JsonSetter
    public TrackerBundleParams setTrackedEntityInstances( List<TrackedEntity> trackedEntities )
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

    public TrackerBundleParams setEnrollments( List<Enrollment> enrollments )
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

    public TrackerBundleParams setEvents( List<Event> events )
    {
        this.events = events;
        return this;
    }

    public TrackerBundle toTrackerBundle()
    {
        return new TrackerBundle()
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

    public TrackerPreheatParams toTrackerPreheatParams()
    {
        return new TrackerPreheatParams()
            .setUserId( userId )
            .setUser( user )
            .setTrackedEntities( trackedEntities )
            .setEnrollments( enrollments )
            .setEvents( events );
    }
}
