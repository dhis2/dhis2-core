package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.Geometry;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
public class ProgramStageInstance
    extends BaseIdentifiableObject
{
    private Date createdAtClient;

    private Date lastUpdatedAtClient;

    private ProgramInstance programInstance;

    private ProgramStage programStage;

    private boolean deleted;

    private String storedBy;

    private Date dueDate;

    private Date executionDate;

    private OrganisationUnit organisationUnit;

    private CategoryOptionCombo attributeOptionCombo;

    private List<MessageConversation> messageConversations = new ArrayList<>();

    private List<TrackedEntityComment> comments = new ArrayList<>();

    private Set<TrackedEntityDataValue> dataValues = new HashSet<>();

    private Set<RelationshipItem> relationshipItems = new HashSet<>();

    private EventStatus status = EventStatus.ACTIVE;

    private String completedBy;

    private Date completedDate;

    private Date lastSynchronized = new Date( 0 );

    private Geometry geometry;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramStageInstance()
    {
        this.deleted = false;
    }

    public ProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage )
    {
        this.programInstance = programInstance;
        this.programStage = programStage;
        this.deleted = false;
    }

    @Override
    public void setAutoFields()
    {
        super.setAutoFields();

        if ( createdAtClient == null )
        {
            createdAtClient = created;
        }

        lastUpdatedAtClient = lastUpdated;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Date getCreatedAtClient()
    {
        return createdAtClient;
    }

    public void setCreatedAtClient( Date createdAtClient )
    {
        this.createdAtClient = createdAtClient;
    }

    public Date getLastUpdatedAtClient()
    {
        return lastUpdatedAtClient;
    }

    public void setLastUpdatedAtClient( Date lastUpdatedAtClient )
    {
        this.lastUpdatedAtClient = lastUpdatedAtClient;
    }

    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public void setProgramInstance( ProgramInstance programInstance )
    {
        this.programInstance = programInstance;
    }

    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }

    public String getCompletedBy()
    {
        return completedBy;
    }

    public void setCompletedBy( String completedBy )
    {
        this.completedBy = completedBy;
    }

    public Date getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( Date dueDate )
    {
        this.dueDate = dueDate;
    }

    public Date getExecutionDate()
    {
        return executionDate;
    }

    public void setExecutionDate( Date executionDate )
    {
        this.executionDate = executionDate;
    }

    public boolean isCompleted()
    {
        return status == EventStatus.COMPLETED;
    }

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public ProgramStageInstance setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
        return this;
    }

    public CategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    public Date getCompletedDate()
    {
        return completedDate;
    }

    public void setCompletedDate( Date completedDate )
    {
        this.completedDate = completedDate;
    }

    public ProgramStageInstance setStatus( EventStatus status )
    {
        this.status = status;
        return this;
    }

    public List<MessageConversation> getMessageConversations()
    {
        return messageConversations;
    }

    public void setMessageConversations( List<MessageConversation> messageConversations )
    {
        this.messageConversations = messageConversations;
    }

    public List<TrackedEntityComment> getComments()
    {
        return comments;
    }

    public void setComments( List<TrackedEntityComment> comments )
    {
        this.comments = comments;
    }

    public Set<TrackedEntityDataValue> getDataValues()
    {
        return dataValues;
    }

    public void setDataValues( Set<TrackedEntityDataValue> dataValues )
    {
        this.dataValues = dataValues;
    }

    public EventStatus getStatus()
    {
        return status;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted( boolean deleted )
    {
        this.deleted = deleted;
    }

    @JsonIgnore
    public Date getLastSynchronized()
    {
        return lastSynchronized;
    }

    public void setLastSynchronized( Date lastSynchronized )
    {
        this.lastSynchronized = lastSynchronized;
    }

    public Set<RelationshipItem> getRelationshipItems()
    {
        return relationshipItems;
    }

    public void setRelationshipItems( Set<RelationshipItem> relationshipItems )
    {
        this.relationshipItems = relationshipItems;
    }

    public Geometry getGeometry()
    {
        return geometry;
    }

    public void setGeometry( Geometry geometry )
    {
        this.geometry = geometry;
    }

    public boolean isCreatableInSearchScope()
    {
        return this.getStatus() == EventStatus.SCHEDULE && this.getDataValues().isEmpty() && this.getExecutionDate() == null;
    }
    
    @Override public String toString()
    {
        return "ProgramStageInstance{" +
            "id=" + id +
            ", uid='" + uid + '\'' +
            ", name='" + name + '\'' +
            ", created=" + created +
            ", lastUpdated=" + lastUpdated +
            ", displayName='" + displayName + '\'' +
            ", programInstance=" + programInstance.getUid() +
            ", programStage=" + programStage.getUid() +
            ", deleted=" + deleted +
            ", storedBy='" + storedBy + '\'' +
            ", organisationUnit=" + organisationUnit.getUid() +
            ", status=" + status +
            ", lastSynchronized=" + lastSynchronized +
            '}';
    }
}
