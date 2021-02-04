/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.events.importer.shared;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.event.EventStatus;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public class ImmutableEvent extends Event
{
    private Event event;

    public ImmutableEvent( Event event )
    {
        this.event = event;
    }

    @Override
    public String getUid()
    {
        return event.getUid();
    }

    @Override
    public String getEvent()
    {
        return event.getEvent();
    }

    @Override
    public EnrollmentStatus getEnrollmentStatus()
    {
        return event.getEnrollmentStatus();
    }

    @Override
    public EventStatus getStatus()
    {
        return event.getStatus();
    }

    @Override
    public String getProgram()
    {
        return event.getProgram();
    }

    @Override
    public String getProgramStage()
    {
        return event.getProgramStage();
    }

    @Override
    public String getEnrollment()
    {
        return event.getEnrollment();
    }

    @Override
    public String getOrgUnit()
    {
        return event.getOrgUnit();
    }

    @Override
    public String getOrgUnitName()
    {
        return event.getOrgUnitName();
    }

    @Override
    public String getTrackedEntityInstance()
    {
        return event.getTrackedEntityInstance();
    }

    @Override
    public String getEventDate()
    {
        return event.getEventDate();
    }

    @Override
    public String getDueDate()
    {
        return event.getDueDate();
    }

    @Override
    public String getStoredBy()
    {
        return event.getStoredBy();
    }

    @Override
    public Set<DataValue> getDataValues()
    {
        return event.getDataValues();
    }

    @Override
    public List<Note> getNotes()
    {
        return event.getNotes();
    }

    @Override
    public Boolean getFollowup()
    {
        return event.getFollowup();
    }

    @Override
    public String getCreated()
    {
        return event.getCreated();
    }

    @Override
    public String getLastUpdated()
    {
        return event.getLastUpdated();
    }

    @Override
    public String getCreatedAtClient()
    {
        return event.getCreatedAtClient();
    }

    @Override
    public String getLastUpdatedAtClient()
    {
        return event.getLastUpdatedAtClient();
    }

    @Override
    public String getAttributeOptionCombo()
    {
        return event.getAttributeOptionCombo();
    }

    @Override
    public String getAttributeCategoryOptions()
    {
        return event.getAttributeCategoryOptions();
    }

    @Override
    public String getCompletedBy()
    {
        return event.getCompletedBy();
    }

    @Override
    public String getCompletedDate()
    {
        return event.getCompletedDate();
    }

    @Override
    public Boolean isDeleted()
    {
        return event.isDeleted();
    }

    @Override
    public int getOptionSize()
    {
        return event.getOptionSize();
    }

    @Override
    public Set<Relationship> getRelationships()
    {
        return event.getRelationships();
    }

    @Override
    public Geometry getGeometry()
    {
        return event.getGeometry();
    }

    @Override
    public String getAssignedUser()
    {
        return event.getAssignedUser();
    }

    @Override
    public String getAssignedUserUsername()
    {
        return event.getAssignedUserUsername();
    }

    @Override
    public boolean equals( Object o )
    {
        return event.equals( o );
    }

    @Override
    public int hashCode()
    {
        return event.hashCode();
    }

    @Override
    public String toString()
    {
        return event.toString();
    }

    @Override
    public String getHref()
    {
        return event.getHref();
    }
}
