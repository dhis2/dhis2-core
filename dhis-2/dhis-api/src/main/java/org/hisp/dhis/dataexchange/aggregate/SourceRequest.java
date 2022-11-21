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
package org.hisp.dhis.dataexchange.aggregate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@Accessors( chain = true )
public class SourceRequest
    implements Serializable
{
    /**
     * Name of source request, max 50 characters.
     */
    @JsonProperty
    private String name;

    /**
     * Optional UID reference to a visualization.
     */
    @JsonProperty
    private String visualization;

    /**
     * Data dimension item identifiers.
     */
    @JsonProperty
    private List<String> dx = new ArrayList<>();

    /**
     * ISO period identifiers.
     */
    @JsonProperty
    private List<String> pe = new ArrayList<>();

    /**
     * Org unit identifiers.
     */
    @JsonProperty
    private List<String> ou = new ArrayList<>();

    /**
     * Request filters.
     */
    @JsonProperty
    private List<Filter> filters = new ArrayList<>();

    /**
     * Input identifier scheme.
     */
    @JsonProperty
    private String inputIdScheme;

    /**
     * Output data element identifier scheme.
     */
    @JsonProperty
    private String outputDataElementIdScheme;

    /**
     * Output org unit identifier scheme.
     */
    @JsonProperty
    private String outputOrgUnitIdScheme;

    /**
     * Output identifier scheme.
     */
    @JsonProperty
    private String outputIdScheme;
}
