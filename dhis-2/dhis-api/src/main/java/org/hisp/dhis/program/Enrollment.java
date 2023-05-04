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
import java.util.Objects;
import java.util.Set;

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Abyot Asalefew
 */
@Auditable( scope = AuditScope.TRACKER )
@JacksonXmlRootElement( localName = "enrollment", namespace = DxfNamespaces.DXF_2_0 )
public class Enrollment
    extends SoftDeletableObject
{
    private Date createdAtClient;

    private Date lastUpdatedAtClient;

    private ProgramStatus status = ProgramStatus.ACTIVE;

    @AuditAttribute
    private OrganisationUnit organisationUnit;

    private Date incidentDate;

    private Date enrollmentDate;

    private Date endDate;

    private UserInfoSnapshot createdByUserInfo;

    private UserInfoSnapshot lastUpdatedByUserInfo;

    @AuditAttribute
    private TrackedEntityInstance entityInstance;

    @AuditAttribute
    private Program program;

    private Set<Event> events = new HashSet<>();

    private Set<RelationshipItem> relationshipItems = new HashSet<>();

    private List<MessageConversation> messageConversations = new ArrayList<>();

    private Boolean followup = false;

    private List<TrackedEntityComment> comments = new ArrayList<>();

    private String completedBy;

    private Geometry geometry;

    private String storedBy;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Enrollment()
    {
    }

    public Enrollment( Date enrollmentDate, Date incidentDate, TrackedEntityInstance entityInstance,
        Program program )
    {
        this.enrollmentDate = enrollmentDate;
        this.incidentDate = incidentDate;
        this.entityInstance = entityInstance;
        this.program = program;
    }

    public Enrollment( Program program, TrackedEntityInstance entityInstance, OrganisationUnit organisationUnit )
    {
        this.program = program;
        this.entityInstance = entityInstance;
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
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Updated the bi-directional associations between this Enrollment and the
     * given entity instance and program.
     *
     * @param entityInstance the entity instance to enroll.
     * @param program the program to enroll the entity instance to.
     */
    public void enrollTrackedEntityInstance( TrackedEntityInstance entityInstance, Program program )
    {
        setEntityInstance( entityInstance );
        entityInstance.getEnrollments().add( this );

        setProgram( program );
    }

    public boolean isCompleted()
    {
        return this.status == ProgramStatus.COMPLETED;
    }

    // -------------------------------------------------------------------------
    // equals and hashCode
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();

        result = prime * result + ((incidentDate == null) ? 0 : incidentDate.hashCode());
        result = prime * result + ((enrollmentDate == null) ? 0 : enrollmentDate.hashCode());
        result = prime * result + ((entityInstance == null) ? 0 : entityInstance.hashCode());
        result = prime * result + ((program == null) ? 0 : program.hashCode());

        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        return this == obj || obj instanceof Enrollment && objectEquals( (Enrollment) obj );
    }

    private boolean objectEquals( Enrollment other )
    {
        return Objects.equals( incidentDate, other.incidentDate )
            && Objects.equals( enrollmentDate, other.enrollmentDate )
            && Objects.equals( entityInstance, other.entityInstance )
            && Objects.equals( program, other.program );
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
    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public Enrollment setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getIncidentDate()
    {
        return incidentDate;
    }

    public void setIncidentDate( Date incidentDate )
    {
        this.incidentDate = incidentDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEnrollmentDate()
    {
        return enrollmentDate;
    }

    public void setEnrollmentDate( Date enrollmentDate )
    {
        this.enrollmentDate = enrollmentDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
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
    public ProgramStatus getStatus()
    {
        return status;
    }

    public void setStatus( ProgramStatus status )
    {
        this.status = status;
    }

    @JsonProperty( "trackedEntityInstance" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityInstance getEntityInstance()
    {
        return entityInstance;
    }

    public void setEntityInstance( TrackedEntityInstance entityInstance )
    {
        this.entityInstance = entityInstance;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "events", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "event", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Event> getEvents()
    {
        return events;
    }

    public void setEvents( Set<Event> events )
    {
        this.events = events;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getFollowup()
    {
        return followup;
    }

    public void setFollowup( Boolean followup )
    {
        this.followup = followup;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "messageConversations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "messageConversation", namespace = DxfNamespaces.DXF_2_0 )
    public List<MessageConversation> getMessageConversations()
    {
        return messageConversations;
    }

    public void setMessageConversations( List<MessageConversation> messageConversations )
    {
        this.messageConversations = messageConversations;
    }

    @JsonProperty( "trackedEntityComments" )
    @JacksonXmlElementWrapper( localName = "trackedEntityComments", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntityComment", namespace = DxfNamespaces.DXF_2_0 )
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
    public Geometry getGeometry()
    {
        return geometry;
    }

    public void setGeometry( Geometry geometry )
    {
        this.geometry = geometry;
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

    @Override
    public String toString()
    {
        return "Enrollment{" +
            "id=" + id +
            ", uid='" + uid + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", created=" + created +
            ", lastUpdated=" + lastUpdated +
            ", status=" + status +
            ", organisationUnit=" + (organisationUnit != null ? organisationUnit.getUid() : "null") +
            ", incidentDate=" + incidentDate +
            ", enrollmentDate=" + enrollmentDate +
            ", entityInstance=" + (entityInstance != null ? entityInstance.getUid() : "null") +
            ", program=" + program +
            ", deleted=" + isDeleted() +
            ", storedBy='" + storedBy + '\'' +
            '}';
    }
}
