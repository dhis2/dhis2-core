package org.hisp.dhis.common;

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

import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "valueType", namespace = DxfNamespaces.DXF_2_0 )
public enum ValueType
{
    TEXT( String.class ),
    LONG_TEXT( String.class ),
    LETTER( String.class ),
    PHONE_NUMBER( String.class ),
    EMAIL( String.class ),
    BOOLEAN( Boolean.class ),
    TRUE_ONLY( Boolean.class ),
    DATE( Date.class ),
    DATETIME( Date.class ),
    NUMBER( Double.class ),
    UNIT_INTERVAL( Double.class ),
    PERCENTAGE( Double.class ),
    INTEGER( Integer.class ),
    INTEGER_POSITIVE( Integer.class ),
    INTEGER_NEGATIVE( Integer.class ),
    INTEGER_ZERO_OR_POSITIVE( Integer.class ),
    TRACKER_ASSOCIATE( TrackedEntityInstance.class ),
    USERNAME( String.class ),
    FILE_RESOURCE( String.class ),
    COORDINATE( String.class);

    public static final Set<ValueType> INTEGER_TYPES = Sets.newHashSet(
        INTEGER, INTEGER_POSITIVE, INTEGER_NEGATIVE, INTEGER_ZERO_OR_POSITIVE );

    public static final Set<ValueType> NUMERIC_TYPES = Sets.newHashSet(
        INTEGER, INTEGER_POSITIVE, INTEGER_NEGATIVE, INTEGER_ZERO_OR_POSITIVE, NUMBER, UNIT_INTERVAL, PERCENTAGE );

    public static final Set<ValueType> BOOLEAN_TYPES = Sets.newHashSet(
        BOOLEAN, TRUE_ONLY );
    
    public static final Set<ValueType> TEXT_TYPES = Sets.newHashSet( 
        TEXT, LONG_TEXT, LETTER, COORDINATE );
    
    public static final Set<ValueType> DATE_TYPES = Sets.newHashSet(
        DATE, DATETIME );
    
    private final Class<?> javaClass;

    private ValueType()
    {
        this.javaClass = null;
    }

    private ValueType( Class<?> javaClass )
    {
        this.javaClass = javaClass;
    }

    public Class<?> getJavaClass()
    {
        return javaClass;
    }

    public boolean isInteger()
    {
        return INTEGER_TYPES.contains( this );
    }

    public boolean isNumeric()
    {
        return NUMERIC_TYPES.contains( this );
    }
    
    public boolean isBoolean()
    {
        return BOOLEAN_TYPES.contains( this );
    }

    public boolean isText()
    {
        return TEXT_TYPES.contains( this );
    }

    public boolean isDate()
    {
        return DATE_TYPES.contains( this );
    }

    public boolean isFile()
    {
        return this == FILE_RESOURCE;
    }
    
    public boolean isCoordinate()
    {
    	return this == COORDINATE;
    }
}
