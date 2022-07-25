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
package org.hisp.dhis.dxf2.events;

import lombok.Value;
import lombok.With;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@With
@Value
public class TrackedEntityInstanceParams
{
    public static final TrackedEntityInstanceParams TRUE = new TrackedEntityInstanceParams( true, true, true, true,
        false, false );

    public static final TrackedEntityInstanceParams FALSE = new TrackedEntityInstanceParams( false, false, false,
        false, false, false );

    public static final TrackedEntityInstanceParams DATA_SYNCHRONIZATION = new TrackedEntityInstanceParams( true, true,
        true, true, true, true );

    private final boolean includeRelationships;

    private final boolean includeEnrollments;

    private final boolean includeEvents;

    private final boolean includeProgramOwners;

    private final boolean includeDeleted;

    private final boolean dataSynchronizationQuery;

    @JsonProperty
    public boolean isIncludeRelationships()
    {
        return includeRelationships;
    }

    @JsonProperty
    public boolean isIncludeEnrollments()
    {
        return includeEnrollments;
    }

    @JsonProperty
    public boolean isIncludeEvents()
    {
        return includeEvents;
    }

    @JsonProperty
    public boolean isIncludeProgramOwners()
    {
        return includeProgramOwners;
    }

    @JsonProperty
    public boolean isIncludeDeleted()
    {
        return includeDeleted;
    }

    @JsonProperty
    public boolean isDataSynchronizationQuery()
    {
        return dataSynchronizationQuery;
    }

    @Override
    public String toString()
    {
        return "TrackedEntityInstanceParams{" +
            "includeRelationships=" + includeRelationships +
            ", includeEnrollments=" + includeEnrollments +
            ", includeEvents=" + includeEvents +
            ", includeProgramOwners=" + includeProgramOwners +
            ", includeDeleted=" + includeDeleted +
            ", dataSynchronizationQuery=" + dataSynchronizationQuery +
            '}';
    }
}
