/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.name;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class WildcardName implements JsonFilteringName
{

    private final String name;

    private final String rawName;

    private final Pattern pattern;

    public WildcardName( String name )
    {
        this.name = name;
        this.rawName = StringUtils.remove( this.name, '*' );
        this.pattern = buildPattern();
    }

    private Pattern buildPattern()
    {
        String[] search = { "*", "?" };
        String[] replace = { ".*", ".?" };

        return Pattern.compile( "^" + StringUtils.replaceEach( name, search, replace ) + "$" );
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getRawName()
    {
        return name;
    }

    @Override
    public int match( String name )
    {
        if ( pattern.matcher( name ).matches() )
        {
            return rawName.length() + 2;
        }

        return -1;
    }
}
