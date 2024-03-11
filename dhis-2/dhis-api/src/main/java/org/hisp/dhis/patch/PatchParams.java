package org.hisp.dhis.patch;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PatchParams
{
    private final Object source;

    private final Object target;

    private final JsonNode jsonNode;

    /**
     * Ignore properties that are not owned by the source class.
     */
    private boolean ignoreTransient;

    public PatchParams( Object source, Object target )
    {
        this.source = source;
        this.target = target;
        this.jsonNode = null;
    }

    public PatchParams( JsonNode jsonNode )
    {
        this.source = null;
        this.target = null;
        this.jsonNode = jsonNode;
    }

    public Object getSource()
    {
        return source;
    }

    public Object getTarget()
    {
        return target;
    }

    public JsonNode getJsonNode()
    {
        return jsonNode;
    }

    public boolean haveJsonNode()
    {
        return jsonNode != null;
    }

    public boolean isIgnoreTransient()
    {
        return ignoreTransient;
    }

    public PatchParams setIgnoreTransient( boolean ignoreTransient )
    {
        this.ignoreTransient = ignoreTransient;
        return this;
    }
}
