package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.schema.Property;

import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Order
{
    private boolean ascending;

    private boolean ignoreCase;

    private Property property;

    public Order( Property property, boolean ascending )
    {
        this.property = property;
        this.ascending = ascending;
    }

    public Order ignoreCase()
    {
        this.ignoreCase = true;
        return this;
    }

    public boolean isAscending()
    {
        return ascending;
    }

    public boolean isIgnoreCase()
    {
        return ignoreCase;
    }

    public Property getProperty()
    {
        return property;
    }

    public static Order asc( Property property )
    {
        return new Order( property, true );
    }

    public static Order desc( Property property )
    {
        return new Order( property, false );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( ascending, ignoreCase, property );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        final Order other = (Order) obj;

        return Objects.equals( this.ascending, other.ascending )
            && Objects.equals( this.ignoreCase, other.ignoreCase )
            && Objects.equals( this.property, other.property );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "ascending", ascending )
            .add( "ignoreCase", ignoreCase )
            .add( "property", property != null ? property.getName() : null )
            .toString();
    }
}
