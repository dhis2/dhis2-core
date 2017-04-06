package org.hisp.dhis.dxf2.events;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackedEntityInstanceParams
{
    public static final TrackedEntityInstanceParams TRUE = new TrackedEntityInstanceParams( true, true, true );

    public static final TrackedEntityInstanceParams FALSE = new TrackedEntityInstanceParams( false, false, false );

    private boolean includeRelationships;

    private boolean includeEnrollments;

    private boolean includeEvents;

    public TrackedEntityInstanceParams()
    {
    }

    public TrackedEntityInstanceParams( boolean includeRelationships, boolean includeEnrollments, boolean includeEvents )
    {
        this.includeRelationships = includeRelationships;
        this.includeEnrollments = includeEnrollments;
        this.includeEvents = includeEvents;
    }

    @JsonProperty
    public boolean isIncludeRelationships()
    {
        return includeRelationships;
    }

    public void setIncludeRelationships( boolean includeRelationships )
    {
        this.includeRelationships = includeRelationships;
    }

    @JsonProperty
    public boolean isIncludeEnrollments()
    {
        return includeEnrollments;
    }

    public void setIncludeEnrollments( boolean includeEnrollments )
    {
        this.includeEnrollments = includeEnrollments;
    }

    @JsonProperty
    public boolean isIncludeEvents()
    {
        return includeEvents;
    }

    public void setIncludeEvents( boolean includeEvents )
    {
        this.includeEvents = includeEvents;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "includeRelationships", includeRelationships )
            .add( "includeEnrollments", includeEnrollments )
            .add( "includeEvents", includeEvents )
            .toString();
    }
}
