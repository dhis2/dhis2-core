/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.category;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalEmbeddedObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "categoryDimension", namespace = DxfNamespaces.DXF_2_0 )
public class CategoryDimension
    implements DimensionalEmbeddedObject
{
    private int id;

    private Category dimension;

    private List<CategoryOption> items = new ArrayList<>();

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty( "category" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "category", namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.REFERENCE, required = Property.Value.TRUE )
    public Category getDimension()
    {
        return dimension;
    }

    public void setDimension( Category dimension )
    {
        this.dimension = dimension;
    }

    @JsonProperty( "categoryOptions" )
    @JacksonXmlElementWrapper( localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.REFERENCE, required = Property.Value.TRUE )
    public List<CategoryOption> getItems()
    {
        return items;
    }

    public void setItems( List<CategoryOption> items )
    {
        this.items = items;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "CategoryDimension{" );
        sb.append( "id=" ).append( id );
        sb.append( ", dimension=" ).append( dimension );
        sb.append( ", items=" ).append( items );
        sb.append( '}' );
        return sb.toString();
    }
}
