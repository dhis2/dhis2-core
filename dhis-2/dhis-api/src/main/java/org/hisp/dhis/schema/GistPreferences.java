/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.schema;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public final class GistPreferences
{
    public static final GistPreferences DEFAULT = new GistPreferences( Flag.AUTO,
        emptyList(), GistProjection.AUTO, EnumSet.allOf( GistProjection.class ) );

    @Getter
    @JsonProperty
    private final Flag includeByDefault;

    /**
     * In case this is a collection property: optional list of fields that by
     * default are shown of the collection items in the API.
     *
     * An empty list represents no particular preferences and the list will be
     * determined by program logic evaluating the schema of the item type.
     */
    @Getter
    @JsonProperty
    private final List<String> fields;

    /**
     * In case this is a collection property: how *should* it be viewed as part
     * of its parent/owner object?
     */
    @Getter
    @JsonProperty
    private final GistProjection defaultProjection;

    /**
     * In case this is a collection property: how *can* it be viewed as part of
     * its parent/owner object?
     */
    @JsonProperty
    private final EnumSet<GistProjection> options;

    public boolean isOptions( GistProjection type )
    {
        return options.contains( type );
    }

    public GistPreferences withFields( String... fields )
    {
        return new GistPreferences( includeByDefault, asList( fields ), defaultProjection, options );
    }

    public enum Flag
    {
        FALSE,
        TRUE,
        AUTO
    }
}
