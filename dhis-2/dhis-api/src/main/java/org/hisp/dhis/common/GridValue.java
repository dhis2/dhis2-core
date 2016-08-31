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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public class GridValue
{
    private Object value;
    
    private Map<Object, Object> attributes = new HashMap<>();

    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------

    public GridValue( Object value )
    {
        this.value = value;
    }
    
    public GridValue( Object value, Map<Object, Object> attributes )
    {
        this.value = value;
        this.attributes = attributes;
    }
    
    // ---------------------------------------------------------------------
    // Logic
    // ---------------------------------------------------------------------

    public void attr( Object attribute, Object value )
    {
        this.attributes.put( attribute, value );
    }
    
    public Object attr( Object attribute )
    {
        return this.attributes.get( attribute );
    }
    
    public boolean hasAttr( Object attribute )
    {
        return this.attributes.containsKey( attribute );
    }

    @Override
    public String toString()
    {
        return value != null ? value.toString() : null;
    }

    // ---------------------------------------------------------------------
    // Get and set methods
    // ---------------------------------------------------------------------

    public Object getValue()
    {
        return value;
    }

    public void setValue( Object value )
    {
        this.value = value;
    }

    public Map<Object, Object> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( Map<Object, Object> attributes )
    {
        this.attributes = attributes;
    }
}
