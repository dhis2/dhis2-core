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
package org.hisp.dhis.program;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.User;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Abyot Asalefew
 */
@Auditable( scope = AuditScope.TRACKER )
public class Event
    extends SoftDeletableObject
{
    private Date createdAtClient;

    private Date lastUpdatedAtClient;

    @AuditAttribute
    private ProgramInstance programInstance;

    @AuditAttribute
    private ProgramStage programStage;

    private String storedBy;

    private UserInfoSnapshot createdByUserInfo;

    private UserInfoSnapshot lastUpdatedByUserInfo;

    private Date dueDate;

    private Date executionDate;

    @AuditAttribute
    private OrganisationUnit organisationUnit;

    @AuditAttribute
    private CategoryOptionCombo attributeOptionCombo;

    private List<MessageConversation> messageConversations = new ArrayList<>();

    private List<TrackedEntityComment> comments = new ArrayList<>();

    @AuditAttribute
    private Set<EventDataValue> eventDataValues = new HashSet<>();

    private Set<RelationshipItem> relationshipItems = new HashSet<>();

    @AuditAttribute
    private EventStatus status = EventStatus.ACTIVE;

    private String completedBy;

    private Date completedDate;

    private Date lastSynchronized = new Date( 0 );

    private Geometry geometry;

    private User assignedUser;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Event()
    {
    }

    public Event( ProgramInstance programInstance, ProgramStage programStage )
    {
        this.programInstance = programInstance;
        this.programStage = programStage;
    }

    public Event( ProgramInstance programInstance, ProgramStage programStage,
        OrganisationUnit organisationUnit )
    {
        this( programInstance, programStage );
        this.organisationUnit = organisationUnit;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getCreatedAtClient()
    {
        return createdAtClient;
    }

    public void setCreatedAtClient( Date createdAtClient )
    {
        this.createdAtClient = createdAtClient;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastUpdatedAtClient()
    {
        return lastUpdatedAtClient;
    }

    public void setLastUpdatedAtClient( Date lastUpdatedAtClient )
    {
        this.lastUpdatedAtClient = lastUpdatedAtClient;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public void setProgramInstance( ProgramInstance programInstance )
    {
        this.programInstance = programInstance;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserInfoSnapshot getCreatedByUserInfo()
    {
        return createdByUserInfo;
    }

    public void setCreatedByUserInfo( UserInfoSnapshot createdByUserInfo )
    {
        this.createdByUserInfo = createdByUserInfo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserInfoSnapshot getLastUpdatedByUserInfo()
    {
        return lastUpdatedByUserInfo;
    }

    public void setLastUpdatedByUserInfo( UserInfoSnapshot lastUpdatedByUserInfo )
    {
        this.lastUpdatedByUserInfo = lastUpdatedByUserInfo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCompletedBy()
    {
        return completedBy;
    }

    public void setCompletedBy( String completedBy )
    {
        this.completedBy = completedBy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( Date dueDate )
    {
        this.dueDate = dueDate;
    }

    @JsonProperty( "eventDate" )
    @JacksonXmlProperty( localName = "eventDate", namespace = DxfNamespaces.DXF_2_0 )
    public Date getExecutionDate()
    {
        return executionDate;
    }

    public void setExecutionDate( Date executionDate )
    {
        this.executionDate = executionDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCompleted()
    {
        return status == EventStatus.COMPLETED;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public Event setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
        return this;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public CategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getCompletedDate()
    {
        return completedDate;
    }

    public void setCompletedDate( Date completedDate )
    {
        this.completedDate = completedDate;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<MessageConversation> getMessageConversations()
    {
        return messageConversations;
    }

    public void setMessageConversations( List<MessageConversation> messageConversations )
    {
        this.messageConversations = messageConversations;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityComment> getComments()
    {
        return comments;
    }

    public void setComments( List<TrackedEntityComment> comments )
    {
        this.comments = comments;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<EventDataValue> getEventDataValues()
    {
        return eventDataValues;
    }

    public void setEventDataValues( Set<EventDataValue> eventDataValues )
    {
        this.eventDataValues = eventDataValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventStatus getStatus()
    {
        return status;
    }

    public Event setStatus( EventStatus status )
    {
        this.status = status;
        return this;
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

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "relationshipItems", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "relationshipItem", namespace = DxfNamespaces.DXF_2_0 )
    public Set<RelationshipItem> getRelationshipItems()
    {
        return relationshipItems;
    }

    public void setRelationshipItems( Set<RelationshipItem> relationshipItems )
    {
        this.relationshipItems = relationshipItems;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Geometry getGeometry()
    {
        return geometry;
    }

    public void setGeometry( Geometry geometry )
    {
        this.geometry = geometry;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getAssignedUser()
    {
        return assignedUser;
    }

    public void setAssignedUser( User assignedUser )
    {
        this.assignedUser = assignedUser;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCreatableInSearchScope()
    {
        return this.getStatus() == EventStatus.SCHEDULE && this.getEventDataValues().isEmpty()
            && this.getExecutionDate() == null;
    }

    @Override
    public String toString()
    {
        return "Event{" +
            "id=" + id +
            ", uid='" + uid + '\'' +
            ", name='" + name + '\'' +
            ", created=" + created +
            ", lastUpdated=" + lastUpdated +
            ", programInstance=" + (programInstance != null ? programInstance.getUid() : null) +
            ", programStage=" + (programStage != null ? programStage.getUid() : null) +
            ", deleted=" + isDeleted() +
            ", storedBy='" + storedBy + '\'' +
            ", organisationUnit=" + (organisationUnit != null ? organisationUnit.getUid() : null) +
            ", status=" + status +
            ", lastSynchronized=" + lastSynchronized +
            '}';
    }
}
