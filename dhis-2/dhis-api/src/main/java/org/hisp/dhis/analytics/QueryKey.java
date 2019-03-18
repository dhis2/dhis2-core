package org.hisp.dhis.analytics;

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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author Lars Helge Overland
 */
public class QueryKey
{
    private static final char SEPARATOR = '-';

    List<String> keyComponents = new ArrayList<>();

    public QueryKey()
    {
    }

    /**
     * Adds a component to this key. Null values are included.
     *
     * @param keyComponent the key component.
     */
    public QueryKey add( Object keyComponent )
    {
        this.keyComponents.add( String.valueOf( keyComponent ) );
        return this;
    }

    /**
     * Adds a component to this key. Null values are omitted.
     *
     * @param keyComponent the key component.
     */
    public QueryKey addIgnoreNull( Object keyComponent )
    {
        if ( keyComponent != null )
        {
            this.keyComponents.add( String.valueOf( keyComponent ) );
        }

        return this;
    }

    /**
     * Adds a component to this key if the given object is not null, provided
     * by the given object.
     *
     * @param object the object to check for null.
     * @param keySupplier the supplier of the key component.
     */
    public QueryKey addIgnoreNull( Object object, Supplier<String> keySupplier )
    {
        if ( object != null )
        {
            this.addIgnoreNull( keySupplier.get() );
        }

        return this;
    }

    /**
     * Returns a plain text key based on the components of this key. Use
     * {@link QueryKey#build()} to obtain a shorter and more usable key.
     */
    public String asPlainKey()
    {
        return StringUtils.join( keyComponents, SEPARATOR );
    }

    /**
     * Returns a 40-character unique key. The key is a SHA-1 hash of
     * the components of this key.
     */
    public String build()
    {
        String key = StringUtils.join( keyComponents, SEPARATOR );
        return DigestUtils.sha1Hex( key );
    }

    /**
     * Equal to {@link QueryKey#build()}.
     */
    @Override
    public String toString()
    {
        return build();
    }
}
