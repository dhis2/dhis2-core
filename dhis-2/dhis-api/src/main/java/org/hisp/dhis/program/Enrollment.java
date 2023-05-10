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

import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Abyot Asalefew
 */
@Getter
@Setter
@Auditable( scope = AuditScope.TRACKER )
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
    private TrackedEntity trackedEntity;

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

    public Enrollment()
    {
    }

    public Enrollment( Date enrollmentDate, Date incidentDate, TrackedEntity trackedEntity,
        Program program )
    {
        this.enrollmentDate = enrollmentDate;
        this.incidentDate = incidentDate;
        this.trackedEntity = trackedEntity;
        this.program = program;
    }

    public Enrollment( Program program, TrackedEntity trackedEntity, OrganisationUnit organisationUnit )
    {
        this.program = program;
        this.trackedEntity = trackedEntity;
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

    /**
     * Updated the bidirectional associations between this Enrollment and the
     * given tracked entity and program.
     *
     * @param trackedEntity the tracked entity to enroll
     * @param program the program to enroll the tracked entity in
     */
    public void enrollTrackedEntity( TrackedEntity trackedEntity, Program program )
    {
        setTrackedEntity( trackedEntity );
        trackedEntity.getEnrollments().add( this );

        setProgram( program );
    }

    public boolean isCompleted()
    {
        return this.status == ProgramStatus.COMPLETED;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();

        result = prime * result + ((incidentDate == null) ? 0 : incidentDate.hashCode());
        result = prime * result + ((enrollmentDate == null) ? 0 : enrollmentDate.hashCode());
        result = prime * result + ((trackedEntity == null) ? 0 : trackedEntity.hashCode());
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
            && Objects.equals( trackedEntity, other.trackedEntity )
            && Objects.equals( program, other.program );
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
            ", entityInstance=" + (trackedEntity != null ? trackedEntity.getUid() : "null") +
            ", program=" + program +
            ", deleted=" + isDeleted() +
            ", storedBy='" + storedBy + '\'' +
            '}';
    }
}
