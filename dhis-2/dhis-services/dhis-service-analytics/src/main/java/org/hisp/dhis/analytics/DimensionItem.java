package org.hisp.dhis.analytics;

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

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.commons.collection.CollectionUtils;

/**
 * @author Lars Helge Overland
 */
public class DimensionItem
{
    private String dimension;
    
    private DimensionalItemObject item;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DimensionItem( String dimension, DimensionalItemObject item )
    {
        this.dimension = dimension;
        this.item = item;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    public String getDimension()
    {
        return dimension;
    }

    public void setDimension( String dimension )
    {
        this.dimension = dimension;
    }

    public DimensionalItemObject getItem()
    {
        return item;
    }

    public void setItem( DimensionalItemObject item )
    {
        this.item = item;
    }

    // -------------------------------------------------------------------------
    // Static methods
    // -------------------------------------------------------------------------

    /**
     * Returns a string key for dimension items in the given list. The key is 
     * a concatenation of the dimension items separated by the dimension separator.
     * If no items are given or items is null, an empty string is returned.
     */
    public static String asItemKey( List<DimensionItem> items )
    {
        StringBuilder builder = new StringBuilder();
        
        if ( items != null && !items.isEmpty() )
        {
            for ( DimensionItem item : items )
            {
                builder.append( item.getItem().getDimensionItem() ).append( DIMENSION_SEP );
            }
            
            builder.deleteCharAt( builder.length() - 1 );
        }
        
        return builder.toString();
    }

    /**
     * Returns an array of identifiers of the dimension items in the given list.
     * If no items are given or items are null, an empty array is returned.
     */
    public static String[] getItemIdentifiers( List<DimensionItem> items )
    {
        List<String> itemUids = new ArrayList<>();
        
        if ( items != null && !items.isEmpty() )
        {
            for ( DimensionItem item : items )
            {
                itemUids.add( item != null ? item.getItem().getDimensionItem() : null );
            }
        }
        
        return itemUids.toArray( CollectionUtils.STRING_ARR );
    }

    /**
     * Returns the period dimension item object from the given list of
     * dimension items. If no items are given, items are null or there are no 
     * period dimension, null is returned.
     */
    public static DimensionalItemObject getPeriodItem( List<DimensionItem> items )
    {
        if ( items != null && !items.isEmpty() )
        {
            for ( DimensionItem item : items )
            {
                if ( DimensionalObject.PERIOD_DIM_ID.equals( item.getDimension() ) )
                {
                    return item.getItem();
                }
            }
        }
        
        return null;
    }

    /**
     * Returns the organisation unit dimension item object from the given list of
     * dimension items. If no items are given, items are null or there are no 
     * period dimension, null is returned.
     */
    public static DimensionalItemObject getOrganisationUnitItem( List<DimensionItem> items )
    {
        if ( items != null && !items.isEmpty() )
        {
            for ( DimensionItem item : items )
            {
                if ( DimensionalObject.ORGUNIT_DIM_ID.equals( item.getDimension() ) )
                {
                    return item.getItem();
                }
            }
        }
        
        return null;
    }
    
    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( dimension == null ) ? 0 : dimension.hashCode() );
        result = prime * result + ( ( item == null ) ? 0 : item.hashCode() );
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
        
        if ( getClass() != object.getClass() )
        {
            return false;
        }
        
        DimensionItem other = (DimensionItem) object;
        
        if ( dimension == null )
        {
            if ( other.dimension != null )
            {
                return false;
            }
        }
        else if ( !dimension.equals( other.dimension ) )
        {
            return false;
        }
        
        if ( item == null )
        {
            if ( other.item != null )
            {
                return false;
            }
        }
        else if ( !item.equals( other.item ) )
        {
            return false;
        }
        
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + dimension + ", " + item + "]";
    }
}
