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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

@NoArgsConstructor
@EqualsAndHashCode( exclude = { "identifier", "identifierName", "identifierClass" } )
class RelationshipCriteria extends PagingAndSortingCriteriaAdapter
{

    private String trackedEntity;

    @Setter
    private String enrollment;

    @Setter
    private String event;

    private String identifier;

    private String identifierName;

    private Class<?> identifierClass;

    public void setTei( String tei )
    {
        // this setter is kept for backwards-compatibility
        // query parameter 'tei' should still be allowed, but 'trackedEntity' is
        // preferred.
        this.trackedEntity = tei;
    }

    public void setTrackedEntity( String trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    public String getIdentifierParam()
        throws BadRequestException
    {
        if ( this.identifier != null )
        {
            return this.identifier;
        }

        int count = 0;
        if ( !StringUtils.isBlank( this.trackedEntity ) )
        {
            this.identifier = this.trackedEntity;
            this.identifierName = "trackedEntity";
            this.identifierClass = TrackedEntityInstance.class;
            count++;
        }
        if ( !StringUtils.isBlank( this.enrollment ) )
        {
            this.identifier = this.enrollment;
            this.identifierName = "enrollment";
            this.identifierClass = ProgramInstance.class;
            count++;
        }
        if ( !StringUtils.isBlank( this.event ) )
        {
            this.identifier = this.event;
            this.identifierName = "event";
            this.identifierClass = Event.class;
            count++;
        }

        if ( count == 0 )
        {
            throw new BadRequestException( "Missing required parameter 'trackedEntity', 'enrollment' or 'event'." );
        }
        else if ( count > 1 )
        {
            throw new BadRequestException(
                "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed." );
        }
        return this.identifier;
    }

    public String getIdentifierName()
        throws BadRequestException
    {
        if ( this.identifierName == null )
        {
            this.getIdentifierParam();
        }
        return this.identifierName;
    }

    public Class<?> getIdentifierClass()
        throws BadRequestException
    {
        if ( this.identifierClass == null )
        {
            this.getIdentifierParam();
        }
        return this.identifierClass;
    }
}
