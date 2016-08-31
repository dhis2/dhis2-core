package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Simple class for caching of object type. Mainly for usage in speeding up Operator type lookup.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Type
{
    private boolean isString;

    private boolean isChar;

    private boolean isByte;

    private boolean isNumber;

    private boolean isInteger;

    private boolean isFloat;

    private boolean isDouble;

    private boolean isBoolean;

    private boolean isEnum;

    private boolean isDate;

    private boolean isCollection;

    private boolean isList;

    private boolean isSet;

    private boolean isNull;

    public Type( Object object )
    {
        isNull = object == null;
        isString = String.class.isInstance( object );
        isChar = Character.class.isInstance( object );
        isByte = Byte.class.isInstance( object );
        isNumber = Number.class.isInstance( object );
        isInteger = Integer.class.isInstance( object );
        isFloat = Float.class.isInstance( object );
        isDouble = Double.class.isInstance( object );
        isBoolean = Boolean.class.isInstance( object );
        isEnum = Enum.class.isInstance( object );
        isDate = Date.class.isInstance( object );
        isCollection = Collection.class.isInstance( object );
        isList = List.class.isInstance( object );
        isSet = Set.class.isInstance( object );
    }

    public boolean isNull()
    {
        return isNull;
    }

    public boolean isString()
    {
        return isString;
    }

    public boolean isChar()
    {
        return isChar;
    }

    public boolean isByte()
    {
        return isByte;
    }

    public boolean isNumber()
    {
        return isNumber;
    }

    public boolean isInteger()
    {
        return isInteger;
    }

    public boolean isFloat()
    {
        return isFloat;
    }

    public boolean isDouble()
    {
        return isDouble;
    }

    public boolean isBoolean()
    {
        return isBoolean;
    }

    public boolean isEnum()
    {
        return isEnum;
    }

    public boolean isDate()
    {
        return isDate;
    }

    public boolean isCollection()
    {
        return isCollection;
    }

    public boolean isList()
    {
        return isList;
    }

    public boolean isSet()
    {
        return isSet;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "isString", isString )
            .add( "isChar", isChar )
            .add( "isByte", isByte )
            .add( "isNumber", isNumber )
            .add( "isInteger", isInteger )
            .add( "isFloat", isFloat )
            .add( "isDouble", isDouble )
            .add( "isBoolean", isBoolean )
            .add( "isEnum", isEnum )
            .add( "isDate", isDate )
            .add( "isCollection", isCollection )
            .add( "isList", isList )
            .add( "isSet", isSet )
            .add( "isNull", isNull )
            .toString();
    }
}
