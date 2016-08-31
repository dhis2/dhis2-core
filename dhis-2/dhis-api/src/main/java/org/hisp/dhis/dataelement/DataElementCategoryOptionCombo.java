package org.hisp.dhis.dataelement;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Abyot Aselefew
 */
@JacksonXmlRootElement( localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementCategoryOptionCombo
    extends BaseDimensionalItemObject
{
    public static final String DEFAULT_NAME = "default";

    public static final String DEFAULT_TOSTRING = "(default)";

    /**
     * The category combo.
     */
    private DataElementCategoryCombo categoryCombo;

    /**
     * The category options.
     */
    @Scanned
    private Set<DataElementCategoryOption> categoryOptions = new HashSet<>();
    
    /**
     * Indicates whether to ignore data approval.
     */
    private boolean ignoreApproval;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementCategoryOptionCombo()
    {

    }
    
    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;

        int result = 1;

        result = prime * result + ((categoryCombo == null) ? 0 : categoryCombo.hashCode());
        result = prime * result + ((categoryOptions == null) ? 0 : categoryOptions.hashCode());

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !(object instanceof DataElementCategoryOptionCombo) )
        {
            return false;
        }

        final DataElementCategoryOptionCombo other = (DataElementCategoryOptionCombo) object;

        if ( categoryCombo == null )
        {
            if ( other.categoryCombo != null )
            {
                return false;
            }
        }
        else if ( !categoryCombo.equals( other.categoryCombo ) )
        {
            return false;
        }

        if ( categoryOptions == null )
        {
            if ( other.categoryOptions != null )
            {
                return false;
            }
        }
        else if ( !categoryOptions.equals( other.categoryOptions ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder( "[" + categoryCombo + ", [" );

        Iterator<DataElementCategoryOption> iterator = categoryOptions.iterator();

        while ( iterator.hasNext() )
        {
            DataElementCategoryOption dataElementCategoryOption = iterator.next();

            if ( dataElementCategoryOption != null )
            {
                builder.append( dataElementCategoryOption.toString() );
            }

            if ( iterator.hasNext() )
            {
                builder.append( ", " );
            }
        }

        return builder.append( "]]" ).toString();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptions.add( dataElementCategoryOption );
        dataElementCategoryOption.getCategoryOptionCombos().add( this );
    }

    public void removeDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptions.remove( dataElementCategoryOption );
        dataElementCategoryOption.getCategoryOptionCombos().remove( this );
    }

    public void removeAllCategoryOptions()
    {
        categoryOptions.clear();
    }

    /**
     * Tests whether two objects compare on a name basis. The default equals
     * method becomes unusable in the case of detached objects in conjunction
     * with persistence frameworks that put proxys on associated objects and
     * collections, since it tests the class type which will differ between the
     * proxy and the raw type.
     *
     * @param object the object to test for equality.
     * @return true if objects are equal, false otherwise.
     */
    public boolean equalsOnName( DataElementCategoryOptionCombo object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null || object.getCategoryCombo() == null || object.getCategoryOptions() == null )
        {
            return false;
        }

        if ( !categoryCombo.getName().equals( object.getCategoryCombo().getName() ) )
        {
            return false;
        }

        if ( categoryOptions.size() != object.getCategoryOptions().size() )
        {
            return false;
        }

        final Set<String> names1 = new HashSet<>();
        final Set<String> names2 = new HashSet<>();

        for ( DataElementCategoryOption option : categoryOptions )
        {
            names1.add( option.getName() );
        }

        for ( DataElementCategoryOption option : object.getCategoryOptions() )
        {
            names2.add( option.getName() );
        }

        return names1.equals( names2 );
    }

    public boolean isDefault()
    {
        return categoryCombo != null && DEFAULT_NAME.equals( categoryCombo.getName() );
    }

    /**
     * Creates a mapping between the category option combo identifier and name
     * for the given collection of elements.
     */
    @Deprecated
    public static Map<Integer, String> getCategoryOptionComboMap( Collection<DataElementCategoryOptionCombo> categoryOptionCombos )
    {
        Map<Integer, String> map = new HashMap<>();

        for ( DataElementCategoryOptionCombo coc : categoryOptionCombos )
        {
            map.put( coc.getId(), coc.getName() );
        }

        return map;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @Override
    public boolean isAutoGenerated()
    {
        return name != null && name.equals( DEFAULT_TOSTRING );
    }

    @Override
    public String getName()
    {
        if ( name != null )
        {
            return name;
        }
        
        StringBuilder builder = new StringBuilder();
        
        List<DataElementCategory> categories = this.categoryCombo.getCategories();
            
        for ( DataElementCategory category : categories )
        {
            List<DataElementCategoryOption> options = category.getCategoryOptions();
            
            optionLoop: for ( DataElementCategoryOption option : this.categoryOptions )
            {
                if ( options.contains( option ) )
                {
                    builder.append( option.getDisplayName() ).append( ", " );
                    
                    continue optionLoop;
                }
            }
        }
        
        builder.delete( Math.max( builder.length() - 2, 0 ), builder.length() );
        
        return StringUtils.substring( builder.toString(), 0, 255 );
    }

    @Override
    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public String getShortName()
    {
        return getName();
    }

    @Override
    public void setShortName( String shortName )
    {
        // Not supported
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementCategoryOption> getCategoryOptions()
    {
        return categoryOptions;
    }

    public void setCategoryOptions( Set<DataElementCategoryOption> categoryOptions )
    {
        this.categoryOptions = categoryOptions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isIgnoreApproval()
    {
        return ignoreApproval;
    }

    public void setIgnoreApproval( boolean ignoreApproval )
    {
        this.ignoreApproval = ignoreApproval;
    }
    
    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementCategoryOptionCombo categoryOptionCombo = (DataElementCategoryOptionCombo) other;

            if ( strategy.isReplace() )
            {
                categoryCombo = categoryOptionCombo.getCategoryCombo();
            }
            else if ( strategy.isMerge() )
            {
                categoryCombo = categoryOptionCombo.getCategoryCombo() == null ? categoryCombo : categoryOptionCombo.getCategoryCombo();
            }

            removeAllCategoryOptions();

            for ( DataElementCategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
            {
                addDataElementCategoryOption( categoryOption );
            }
        }
    }
}
