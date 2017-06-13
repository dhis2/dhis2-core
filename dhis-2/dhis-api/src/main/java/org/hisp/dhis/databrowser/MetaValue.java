package org.hisp.dhis.databrowser;


/*
 * Copyright (c) 2004-2017, University of Oslo
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

/**
 * @author joakibj
 * 
 * Class to hold Metadata for DataBrowserTable. All fields are optional but name
 * should not be null for proper behavior.
 */
public class MetaValue
{
    /**
     * Id for row/column
     */
    private Integer id;

    /**
     * Name for row/column
     */
    private String name;

    /**
     * Any other metadata associated to id or name
     */
    private String metaValue = "";

    public MetaValue()
    {
    }

    public MetaValue( String name )
    {
        this.name = name;
    }

    public MetaValue( String name, String metaValue )
    {
        this.name = name;
        this.metaValue = metaValue;
    }

    public MetaValue( Integer id, String name )
    {
        this.id = id;
        this.name = name;
    }

    public MetaValue( Integer id, String name, String metaValue )
    {
        this.id = id;
        this.name = name;
        this.metaValue = metaValue;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getMetaValue()
    {
        return metaValue;
    }

    public void setMetaValue( String metaValue )
    {
        this.metaValue = metaValue;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null )
        {
            return false;
        }

        if ( !(o instanceof MetaValue) )
        {
            return false;
        }

        final MetaValue other = (MetaValue) o;

        return name.equals( other.getName() );
    }
    
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
    
    @Override
    public String toString()
    {
        return name;
    }
}
