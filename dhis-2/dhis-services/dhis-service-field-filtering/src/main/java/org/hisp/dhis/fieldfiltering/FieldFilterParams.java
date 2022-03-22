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
package org.hisp.dhis.fieldfiltering;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen
 */
@Data
@Builder
public class FieldFilterParams<T>
{
    /**
     * Objects to apply filters on.
     */
    private final List<T> objects;

    @Builder.Default
    private final Set<String> filters = Collections.singleton( "*" );

    private User user;

    /**
     * Do not include sharing properties (user, sharing, publicAccess, etc).
     */
    private final boolean skipSharing;

    public static <O> FieldFilterParams<O> of( List<O> objects, List<String> filters )
    {
        return FieldFilterParams
            .<O> builder()
            .objects( objects )
            .filters( Collections.singleton( StringUtils.join( filters, "," ) ) )
            .build();
    }

    public static <O> FieldFilterParams<O> of( O object, List<String> filters )
    {
        return FieldFilterParams
            .<O> builder()
            .objects( Collections.singletonList( object ) )
            .filters( Collections.singleton( StringUtils.join( filters, "," ) ) )
            .build();
    }

    public FieldFilterParams<T> copyOf()
    {
        return new FieldFilterParams<>( objects, filters, user, skipSharing );
    }
}
