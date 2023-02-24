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

import static org.hisp.dhis.dxf2.events.Param.ATTRIBUTES;
import static org.hisp.dhis.dxf2.events.Param.DELETED;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_ATTRIBUTES;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_EVENTS;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_EVENTS_RELATIONSHIPS;
import static org.hisp.dhis.dxf2.events.Param.ENROLLMENTS_RELATIONSHIPS;
import static org.hisp.dhis.dxf2.events.Param.EVENTS;
import static org.hisp.dhis.dxf2.events.Param.EVENTS_RELATIONSHIPS;
import static org.hisp.dhis.dxf2.events.Param.PROGRAM_OWNERS;
import static org.hisp.dhis.dxf2.events.Param.RELATIONSHIPS;
import static org.hisp.dhis.dxf2.events.Param.fromFieldPath;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
public class TrackedEntityInstanceParams extends Params
{
    public static final Set<Param> ALL = EnumSet.of( RELATIONSHIPS, ATTRIBUTES, PROGRAM_OWNERS, DELETED,
        ENROLLMENTS, ENROLLMENTS_EVENTS, ENROLLMENTS_RELATIONSHIPS, ENROLLMENTS_ATTRIBUTES,
        ENROLLMENTS_EVENTS_RELATIONSHIPS,
        EVENTS,
        EVENTS_RELATIONSHIPS );

    private TrackedEntityInstanceParams( Set<Param> paramsSet )
    {
        super( paramsSet );
    }

    /**
     * Create a {@link EnrollmentParams} instance filtering by the prefix
     * {@link Param#ENROLLMENTS}. We need to set the {@link Param#DELETED}
     * because we only have a generic includeDeleted from the request
     */
    public EnrollmentParams getEnrollmentParams()
    {
        return EnrollmentParams.builder().empty()
            .with( this.params.stream()
                .filter(
                    p -> p.getPrefix().isPresent() && p.getPrefix().get() == ENROLLMENTS )
                .map( p -> fromFieldPath( p.getField() ) )
                .collect( Collectors.toCollection( () -> EnumSet.noneOf( Param.class ) ) ),
                true )
            .with( DELETED, this.params.contains( DELETED ) ).build();
    }

    public static ParamsBuilder<TrackedEntityInstanceParams> builder()
    {
        return new ParamsBuilder<>()
        {
            @Override
            public ParamsBuilder<TrackedEntityInstanceParams> all()
            {
                this.params = EnumSet.copyOf( ALL );
                return this;
            }

            @Override
            public TrackedEntityInstanceParams build()
            {
                return new TrackedEntityInstanceParams( this.params );
            }
        };
    }
}
