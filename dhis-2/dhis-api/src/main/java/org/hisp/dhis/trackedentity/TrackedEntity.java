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
package org.hisp.dhis.trackedentity;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Abyot Asalefew Gizaw
 */
@Getter
@Setter
@Auditable( scope = AuditScope.TRACKER )
public class TrackedEntity
    extends SoftDeletableObject
{
    private Date createdAtClient;

    private Date lastUpdatedAtClient;

    private Set<TrackedEntityAttributeValue> trackedEntityAttributeValues = new LinkedHashSet<>();

    private Set<RelationshipItem> relationshipItems = new HashSet<>();

    private Set<Enrollment> enrollments = new HashSet<>();

    private Set<TrackedEntityProgramOwner> programOwners = new HashSet<>();

    private boolean potentialDuplicate;

    @AuditAttribute
    private OrganisationUnit organisationUnit;

    @AuditAttribute
    private TrackedEntityType trackedEntityType;

    @Getter( )
    @AuditAttribute
    private Boolean inactive = false;

    private Geometry geometry;

    private Date lastSynchronized = new Date( 0 );

    private String storedBy;

    private UserInfoSnapshot createdByUserInfo;

    private UserInfoSnapshot lastUpdatedByUserInfo;

    public TrackedEntity()
    {
    }

    @Override
    public void setAutoFields()
    {
        super.setAutoFields();

        if ( createdAtClient == null )
        {
            createdAtClient = created;
        }

        if ( lastUpdatedAtClient == null )
        {
            lastUpdatedAtClient = lastUpdated;
        }
    }

    public void addAttributeValue( TrackedEntityAttributeValue attributeValue )
    {
        trackedEntityAttributeValues.add( attributeValue );
        attributeValue.setTrackedEntity( this );
    }

    public void removeAttributeValue( TrackedEntityAttributeValue attributeValue )
    {
        trackedEntityAttributeValues.remove( attributeValue );
        attributeValue.setTrackedEntity( null );
    }

    @Override
    public String toString()
    {
        return "TrackedEntity{" +
            "id=" + id +
            ", uid='" + uid + '\'' +
            ", name='" + name + '\'' +
            ", organisationUnit=" + organisationUnit +
            ", trackedEntityType=" + trackedEntityType +
            ", inactive=" + inactive +
            ", deleted=" + isDeleted() +
            ", lastSynchronized=" + lastSynchronized +
            '}';
    }
}
