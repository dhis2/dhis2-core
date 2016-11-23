package org.hisp.dhis.query.planner;

/*
 * Copyright (c) 2004-2016, University of Oslo
 *  All rights reserved.
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

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;

import java.util.Arrays;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryPath
{
    private final String name;

    private final boolean persisted;

    private String[] alias = new String[]{};

    private static final Joiner PATH_JOINER = Joiner.on( "." );

    public QueryPath( String name, boolean persisted )
    {
        this.name = name;
        this.persisted = persisted;
    }

    public QueryPath( String name, boolean persisted, String[] alias )
    {
        this( name, persisted );
        this.alias = alias;
    }

    public String getName()
    {
        return name;
    }

    public String getPath()
    {
        return haveAlias() ? name : PATH_JOINER.join( alias ) + "." + name;
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public String[] getAlias()
    {
        return alias;
    }

    public boolean haveAlias()
    {
        return alias.length > 0;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "name", name )
            .add( "persisted", persisted )
            .add( "alias", Arrays.toString( alias ) )
            .toString();
    }
}
