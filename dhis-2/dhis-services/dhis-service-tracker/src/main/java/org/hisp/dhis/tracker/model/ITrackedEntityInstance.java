package org.hisp.dhis.tracker.model;

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

import java.util.Date;
import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public class ITrackedEntityInstance
{
    private Long id;

    private String uid;

    private String code;

    private Date created;

    private Date createdAtClient;

    private Date lastUpdatedAtClient;

    private Date lastUpdated;

    private User lastUpdatedBy;

    private Set<TrackedEntityAttributeValue> trackedEntityAttributeValues;

    private Set<RelationshipItem> relationshipItems;

    private Set<ProgramInstance> programInstances;

    private Set<TrackedEntityProgramOwner> programOwners;

    private OrganisationUnit organisationUnit;

    private TrackedEntityType trackedEntityType;

    private Boolean inactive;

    private Geometry geometry;

    private Date lastSynchronized;

    private String storedBy;

    private Set<UserGroupAccess> userGroupAccesses;

    private Set<UserAccess> userAccesses;

    private boolean deleted;

    // FIXME use a mapper
    public ITrackedEntityInstance( TrackedEntityInstance tei )
    {
        this.id = tei.getId();
        this.code = tei.getCode();
        this.uid = tei.getUid();
        this.deleted = tei.isDeleted();
        this.trackedEntityAttributeValues = tei.getTrackedEntityAttributeValues();
        this.createdAtClient = tei.getCreatedAtClient();
        this.geometry = tei.getGeometry();
        this.inactive = tei.isInactive();
        this.lastSynchronized = tei.getLastSynchronized();
        this.lastUpdatedAtClient = tei.getLastUpdatedAtClient();
        // HibernateUtils.initializeProxy( tei.getOrganisationUnit() );
        this.organisationUnit = tei.getOrganisationUnit();
        this.programInstances = tei.getProgramInstances();
        this.programOwners = tei.getProgramOwners();
        this.relationshipItems = tei.getRelationshipItems();
        this.storedBy = tei.getStoredBy();
        this.trackedEntityType = tei.getTrackedEntityType();
        this.userGroupAccesses = tei.getUserGroupAccesses();
        this.userAccesses = tei.getUserAccesses();
        this.created = tei.getCreated();
        this.lastUpdated = tei.getLastUpdated();
        this.lastUpdatedBy = tei.getLastUpdatedBy();
    }

    // FIXME use mapper
    public TrackedEntityInstance toTrackedEntityInstance() {

        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.setId( this.id );
        tei.setCode( this.code );
        tei.setUid( this.uid );
        tei.setDeleted( this.deleted );
        tei.setTrackedEntityAttributeValues( this.trackedEntityAttributeValues );
        tei.setCreatedAtClient( this.createdAtClient );
        tei.setGeometry( this.geometry );
        tei.setInactive( this.inactive );
        tei.setLastSynchronized( this.lastSynchronized );
        tei.setLastUpdatedAtClient( this.lastUpdatedAtClient );
        tei.setOrganisationUnit( this.organisationUnit );
        tei.setProgramInstances( this.programInstances );
        tei.setProgramOwners( this.programOwners );
        tei.setRelationshipItems( this.relationshipItems );
        tei.setStoredBy( this.storedBy );
        tei.setTrackedEntityType( this.trackedEntityType );
        tei.setUserGroupAccesses( this.userGroupAccesses );
        tei.setUserAccesses( this.userAccesses );
        tei.setCreated( this.created );
        tei.setLastUpdated( this.lastUpdated );
        tei.setLastUpdatedBy( this.lastUpdatedBy );

        return tei;
    }

    public Date getCreatedAtClient()
    {
        return createdAtClient;
    }

    public Date getLastUpdatedAtClient()
    {
        return lastUpdatedAtClient;
    }

    public Set<TrackedEntityAttributeValue> getTrackedEntityAttributeValues()
    {
        return trackedEntityAttributeValues;
    }

    public Set<RelationshipItem> getRelationshipItems()
    {
        return relationshipItems;
    }

    public Set<ProgramInstance> getProgramInstances()
    {
        return programInstances;
    }

    public Set<TrackedEntityProgramOwner> getProgramOwners()
    {
        return programOwners;
    }

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public TrackedEntityType getTrackedEntityType()
    {
        return trackedEntityType;
    }

    public Boolean getInactive()
    {
        return inactive;
    }

    public Geometry getGeometry()
    {
        return geometry;
    }

    public Date getLastSynchronized()
    {
        return lastSynchronized;
    }

    public String getStoredBy()
    {
        return storedBy;
    }

    public Long getId()
    {
        return id;
    }

    public Set<UserGroupAccess> getUserGroupAccesses()
    {
        return userGroupAccesses;
    }

    public Set<UserAccess> getUserAccesses()
    {
        return userAccesses;
    }

    public String getUid()
    {
        return uid;
    }

    public String getCode()
    {
        return code;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public Date getCreated()
    {
        return created;
    }

    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public User getLastUpdatedBy()
    {
        return lastUpdatedBy;
    }
}
