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

/**
 * The {@link GistProjection} controls how relations between
 * {@link org.hisp.dhis.common.IdentifiableObject}s are displayed.
 *
 * The intention is to provide guidance and protection so that request provide
 * useful information units while avoiding overly large responses that are slow
 * and resource intensive.
 *
 * @author Jan Bernitt
 */
public enum GistProjection
{
    /**
     * No actual type but used to represent the choice that the actual value
     * should be determined by program logic considering other relevant
     * metadata.
     */
    AUTO,

    NONE,

    /**
     * The relation is not shown as data but as a endpoint description that
     * allows to browse the data.
     */
    REF,

    SIZE,

    IS_EMPTY,

    IS_NOT_EMPTY,

    /**
     * The relation is shown as a list of UIDs. In addition an endpoint
     * description is given that allows to browse the full data objects
     * represented by the ids.
     */
    IDS,

    /**
     * Identical to {@link #IDS} except that each entry is still represented as
     * object with a single property {@code id}. This is mostly for easier
     * transition from existing APIs that usually return this type of ID lists.
     *
     * <pre>
     * { "id": UID }
     * </pre>
     *
     * (instead of plain UID)
     */
    ID_OBJECTS;

    public static GistProjection parse( String projection )
    {
        for ( GistProjection p : values() )
        {
            if ( p.name().replace( "_", "" ).equalsIgnoreCase(
                projection.replace( "-", "" ).replace( "+", "" ) ) )
            {
                return p;
            }
        }
        return AUTO;
    }
}
