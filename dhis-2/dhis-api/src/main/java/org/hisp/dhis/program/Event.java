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
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.User;
import org.locationtech.jts.geom.Geometry;

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
    private Enrollment enrollment;

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

    public Event( Enrollment enrollment, ProgramStage programStage )
    {
        this.enrollment = enrollment;
        this.programStage = programStage;
    }

    public Event( Enrollment enrollment, ProgramStage programStage,
        OrganisationUnit organisationUnit )
    {
        this( enrollment, programStage );
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

    public Enrollment getEnrollment()
    {
        return enrollment;
    }

    public void setEnrollment( Enrollment enrollment )
    {
        this.enrollment = enrollment;
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

    public UserInfoSnapshot getCreatedByUserInfo()
    {
        return createdByUserInfo;
    }

    public void setCreatedByUserInfo( UserInfoSnapshot createdByUserInfo )
    {
        this.createdByUserInfo = createdByUserInfo;
    }

    public UserInfoSnapshot getLastUpdatedByUserInfo()
    {
        return lastUpdatedByUserInfo;
    }

    public void setLastUpdatedByUserInfo( UserInfoSnapshot lastUpdatedByUserInfo )
    {
        this.lastUpdatedByUserInfo = lastUpdatedByUserInfo;
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

    public Event setOrganisationUnit( OrganisationUnit organisationUnit )
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

    public Set<EventDataValue> getEventDataValues()
    {
        return eventDataValues;
    }

    public void setEventDataValues( Set<EventDataValue> eventDataValues )
    {
        this.eventDataValues = eventDataValues;
    }

    public EventStatus getStatus()
    {
        return status;
    }

    public Event setStatus( EventStatus status )
    {
        this.status = status;
        return this;
    }

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

    public User getAssignedUser()
    {
        return assignedUser;
    }

    public void setAssignedUser( User assignedUser )
    {
        this.assignedUser = assignedUser;
    }

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
            ", enrollment=" + (enrollment != null ? enrollment.getUid() : null) +
            ", programStage=" + (programStage != null ? programStage.getUid() : null) +
            ", deleted=" + isDeleted() +
            ", storedBy='" + storedBy + '\'' +
            ", organisationUnit=" + (organisationUnit != null ? organisationUnit.getUid() : null) +
            ", status=" + status +
            ", lastSynchronized=" + lastSynchronized +
            '}';
    }
}
