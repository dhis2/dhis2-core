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
package org.hisp.dhis.commons.jackson.jsonpatch;

import lombok.Getter;

import org.hisp.dhis.commons.jackson.jsonpatch.operations.AddOperation;
import org.hisp.dhis.commons.jackson.jsonpatch.operations.RemoveOperation;
import org.hisp.dhis.commons.jackson.jsonpatch.operations.ReplaceOperation;
import org.hisp.dhis.commons.jackson.jsonpatch.operations.TestOperation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonPointer;

/**
 * @author Morten Olav Hansen
 */
@Getter
@JsonSubTypes( {
    @JsonSubTypes.Type( name = "add", value = AddOperation.class ),
    @JsonSubTypes.Type( name = "remove", value = RemoveOperation.class ),
    @JsonSubTypes.Type( name = "replace", value = ReplaceOperation.class ),
    @JsonSubTypes.Type( name = "test", value = TestOperation.class ),
} )
@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, property = "op" )
public abstract class JsonPatchOperation
    implements Patch
{
    protected final String op;

    protected final JsonPointer path;

    public JsonPatchOperation( final String op, final JsonPointer path )
    {
        this.op = op;
        this.path = path;
    }
}
