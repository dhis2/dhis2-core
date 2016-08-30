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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import org.springframework.util.StringUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class IdScheme
{
    public static final IdScheme NULL = new IdScheme( null );
    public static final IdScheme ID = new IdScheme( IdentifiableProperty.ID );
    public static final IdScheme UID = new IdScheme( IdentifiableProperty.UID );
    public static final IdScheme UUID = new IdScheme( IdentifiableProperty.UUID );
    public static final IdScheme CODE = new IdScheme( IdentifiableProperty.CODE );
    public static final IdScheme NAME = new IdScheme( IdentifiableProperty.NAME );

    public static final ImmutableMap<IdentifiableProperty, IdScheme> IDPROPERTY_IDSCHEME_MAP = 
        ImmutableMap.<IdentifiableProperty, IdScheme>builder().
        put( IdentifiableProperty.ID, IdScheme.ID ).
        put( IdentifiableProperty.UID, IdScheme.UID ).
        put( IdentifiableProperty.UUID, IdScheme.UUID ).
        put( IdentifiableProperty.CODE, IdScheme.CODE ).
        put( IdentifiableProperty.NAME, IdScheme.NAME ).build();
    
    public static final String ATTR_ID_SCHEME_PREFIX = "ATTRIBUTE:";
    
    private IdentifiableProperty identifiableProperty;

    private String attribute;

    public static IdScheme from( IdScheme idScheme )
    {
        if ( idScheme == null )
        {
            return IdScheme.NULL;
        }

        return idScheme;
    }

    public static IdScheme from( String scheme )
    {
        if ( scheme == null )
        {
            return IdScheme.NULL;
        }

        if ( IdScheme.isAttribute( scheme ) )
        {
            return new IdScheme( IdentifiableProperty.ATTRIBUTE, scheme.substring( 10 ) );
        }

        return IdScheme.from( IdentifiableProperty.valueOf( scheme.toUpperCase() ) );
    }

    public static IdScheme from( IdentifiableProperty property )
    {
        if ( property == null )
        {
            return IdScheme.NULL;
        }
        
        return IDPROPERTY_IDSCHEME_MAP.containsKey( property ) ? 
            IDPROPERTY_IDSCHEME_MAP.get( property ) : new IdScheme( property );
    }

    private IdScheme( IdentifiableProperty identifiableProperty )
    {
        this.identifiableProperty = identifiableProperty;
    }

    private IdScheme( IdentifiableProperty identifiableProperty, String attribute )
    {
        this.identifiableProperty = identifiableProperty;
        this.attribute = attribute;
    }

    public IdentifiableProperty getIdentifiableProperty()
    {
        return identifiableProperty;
    }

    public String getIdentifiableString()
    {
        return identifiableProperty != null ? identifiableProperty.toString() : null;
    }

    public void setIdentifiableProperty( IdentifiableProperty identifiableProperty )
    {
        this.identifiableProperty = identifiableProperty;
    }

    public String getAttribute()
    {
        return attribute;
    }

    public void setAttribute( String attribute )
    {
        this.attribute = attribute;
    }

    public boolean is( IdentifiableProperty identifiableProperty )
    {
        return this.identifiableProperty == identifiableProperty;
    }

    public boolean isNull()
    {
        return null == this.identifiableProperty;
    }

    public boolean isNotNull()
    {
        return !isNull();
    }

    public boolean isAttribute()
    {
        return IdentifiableProperty.ATTRIBUTE == identifiableProperty && !StringUtils.isEmpty( attribute );
    }

    public static boolean isAttribute( String str )
    {
        return !StringUtils.isEmpty( str ) && str.toUpperCase().startsWith( ATTR_ID_SCHEME_PREFIX ) && str.length() == 21;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "identifiableProperty", identifiableProperty )
            .add( "attribute", attribute )
            .toString();
    }
}
