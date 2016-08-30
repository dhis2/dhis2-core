package org.hisp.dhis.common;

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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.Date;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "valueType", namespace = DxfNamespaces.DXF_2_0 )
public enum ValueType
{
    TEXT( String.class, true ),
    LONG_TEXT( String.class, true ),
    LETTER( String.class, true ),
    PHONE_NUMBER( String.class, false ),
    EMAIL( String.class, false ),
    BOOLEAN( Boolean.class, true ),
    TRUE_ONLY( Boolean.class, true ),
    DATE( Date.class, false ),
    DATETIME( Date.class, false ),
    TIME( String.class, false ),
    NUMBER( Double.class, true ),
    UNIT_INTERVAL( Double.class, true ),
    PERCENTAGE( Double.class, true ),
    INTEGER( Integer.class, true ),
    INTEGER_POSITIVE( Integer.class, true ),
    INTEGER_NEGATIVE( Integer.class, true ),
    INTEGER_ZERO_OR_POSITIVE( Integer.class, true ),
    TRACKER_ASSOCIATE( TrackedEntityInstance.class, false ),
    USERNAME( String.class, false ),
    FILE_RESOURCE( String.class, false ),
    COORDINATE( String.class, true ),
    ORGANISATION_UNIT( OrganisationUnit.class, false );

    public static final Set<ValueType> INTEGER_TYPES = ImmutableSet.<ValueType>builder().add(
        INTEGER, INTEGER_POSITIVE, INTEGER_NEGATIVE, INTEGER_ZERO_OR_POSITIVE ).build();

    public static final Set<ValueType> NUMERIC_TYPES = ImmutableSet.<ValueType>builder().add(
        INTEGER, INTEGER_POSITIVE, INTEGER_NEGATIVE, INTEGER_ZERO_OR_POSITIVE, NUMBER, UNIT_INTERVAL, PERCENTAGE ).build();

    public static final Set<ValueType> BOOLEAN_TYPES = ImmutableSet.<ValueType>builder().add(
        BOOLEAN, TRUE_ONLY ).build();

    public static final Set<ValueType> TEXT_TYPES = ImmutableSet.<ValueType>builder().add(
        TEXT, LONG_TEXT, LETTER, COORDINATE, TIME ).build();

    public static final Set<ValueType> DATE_TYPES = ImmutableSet.<ValueType>builder().add(
        DATE, DATETIME ).build();
    
    private final Class<?> javaClass;
    
    private boolean aggregateable;

    private ValueType()
    {
        this.javaClass = null;
    }

    private ValueType( Class<?> javaClass, boolean aggregateable )
    {
        this.javaClass = javaClass;
        this.aggregateable = aggregateable;
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
    
    public boolean isAggregateable()
    {
        return aggregateable;
    }
}
