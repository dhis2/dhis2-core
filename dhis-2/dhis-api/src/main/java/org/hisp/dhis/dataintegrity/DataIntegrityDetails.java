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
package org.hisp.dhis.dataintegrity;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.hisp.dhis.common.IdentifiableObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * The result data of a {@link DataIntegrityCheck#getRunDetailsCheck()} run.
 *
 * @author Jan Bernitt
 */
@Getter
@AllArgsConstructor
public class DataIntegrityDetails implements Serializable
{
    @JsonUnwrapped
    private final DataIntegrityCheck check;

    @JsonProperty
    private final Date startTime;

    @JsonProperty
    private final Date finishedTime;

    @JsonProperty
    private final String error;

    @JsonProperty
    private final List<DataIntegrityIssue> issues;

    @Getter
    @AllArgsConstructor
    public static final class DataIntegrityIssue implements Serializable
    {
        @JsonProperty
        private final String id;

        @JsonProperty
        private final String name;

        @JsonProperty
        private final String comment;

        @JsonProperty
        private final List<String> refs;

        public static DataIntegrityIssue toIssue( IdentifiableObject obj )
        {
            return toIssue( obj, null, null );
        }

        public static DataIntegrityIssue toIssue( IdentifiableObject obj, String comment )
        {
            return toIssue( obj, comment, null );
        }

        public static DataIntegrityIssue toIssue( IdentifiableObject obj,
            Collection<? extends IdentifiableObject> refs )
        {
            return toIssue( obj, null, refs );
        }

        public static DataIntegrityIssue toIssue( IdentifiableObject obj, String comment,
            Collection<? extends IdentifiableObject> refs )
        {
            return new DataIntegrityIssue( obj.getUid(), issueName( obj ), comment,
                refs == null || refs.isEmpty() ? List.of() : toRefsList( refs.stream() ) );
        }

        public static List<String> toRefsList( Stream<? extends IdentifiableObject> refs )
        {
            return refs.map( DataIntegrityIssue::issueName )
                .sorted()
                .collect( toUnmodifiableList() );
        }

        public static String issueName( IdentifiableObject object )
        {
            String displayName = object.getDisplayName();
            String uid = object.getUid();
            return displayName == null
                ? uid
                : displayName + ":" + uid;
        }
    }

}
