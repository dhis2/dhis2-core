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
package org.hisp.dhis.dxf2.events.params;

import lombok.Value;
import lombok.With;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Luca Cambi
 */
@With
@Value
public class EnrollmentParams implements InstanceParams
{
    public static final EnrollmentParams TRUE = new EnrollmentParams( true, true, true, true, false, false );

    public static final EnrollmentParams FALSE = new EnrollmentParams( false, false, false, false, false, false );

    // Root is only relevant if it is a nested Param
    private final boolean includeRoot;

    private final boolean includeEvents;

    private final boolean includeRelationships;

    private final boolean includeAttributes;

    private final boolean includeDeleted;

    private final boolean dataSynchronizationQuery;

    @JsonProperty
    public boolean isIncludeRelationships()
    {
        return includeRelationships;
    }

    @JsonProperty
    public boolean isIncludeEvents()
    {
        return includeEvents;
    }

    @JsonProperty
    public boolean isIncludeAttributes()
    {
        return includeAttributes;
    }

    @Override
    public boolean isIncludeDeleted()
    {
        return includeDeleted;
    }

    @Override
    public boolean isDataSynchronizationQuery()
    {
        return dataSynchronizationQuery;
    }

    @Override
    public boolean isIncludeEnrollments()
    {
        return includeRoot;
    }

    @Override
    public boolean isIncludeProgramOwners()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "EnrollmentParams{" +
            "includeRelationships=" + includeRelationships +
            ", includeEvents=" + includeEvents +
            ", includeAttributes=" + includeAttributes +
            '}';
    }
}
