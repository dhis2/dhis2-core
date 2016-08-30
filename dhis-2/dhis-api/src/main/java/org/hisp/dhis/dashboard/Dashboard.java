package org.hisp.dhis.dashboard;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "dashboard", namespace = DxfNamespaces.DXF_2_0 )
public class Dashboard
    extends BaseIdentifiableObject
{
    public static final int MAX_ITEMS = 40;

    private List<DashboardItem> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Dashboard()
    {
    }

    public Dashboard( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Moves an item in the list. Returns true if the operation lead to a
     * modification of the list order. Returns false if there are no items,
     * the given position is out of bounds, the item is not present, if position
     * is equal to current item index or if attempting to move an item one
     * position to the right (pointless operation).
     *
     * @param uid      the uid of the item to move.
     * @param position the new index position of the item.
     * @return true if the operation lead to a modification of order, false otherwise.
     */
    public boolean moveItem( String uid, int position )
    {
        if ( items == null || position < 0 || position > items.size() )
        {
            return false; // No items or position out of bounds
        }

        int index = items.indexOf( new DashboardItem( uid ) );

        if ( index == -1 || index == position || (index + 1) == position )
        {
            return false; // Not found, already at position or pointless move
        }

        DashboardItem item = items.get( index );

        index = position < index ? (index + 1) : index; // New index after move

        items.add( position, item ); // Add item at position
        items.remove( index ); // Remove item at previous index

        return true;
    }

    /**
     * Returns the item with the given uid, or null if no item with the given
     * uid is present for this dashboard.
     *
     * @param uid the item identifier.
     * @return an item.
     */
    public DashboardItem getItemByUid( String uid )
    {
        int index = items.indexOf( new DashboardItem( uid ) );

        return index != -1 ? items.get( index ) : null;
    }

    /**
     * Returns an item from this dashboard of the given type which number of
     * content is less than max. Returns null if no item matches the criteria.
     *
     * @param type the type of content to return.
     * @return an item.
     */
    public DashboardItem getAvailableItemByType( DashboardItemType type )
    {
        for ( DashboardItem item : items )
        {
            if ( type.equals( item.getType() ) && item.getContentCount() < DashboardItem.MAX_CONTENT )
            {
                return item;
            }
        }

        return null;
    }

    /**
     * Indicates whether this dashboard has at least one item.
     */
    public boolean hasItems()
    {
        return items != null && !items.isEmpty();
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getItemCount()
    {
        return items == null ? 0 : items.size();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty( "dashboardItems" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dashboardItems", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dashboardItem", namespace = DxfNamespaces.DXF_2_0 )
    public List<DashboardItem> getItems()
    {
        return items;
    }

    public void setItems( List<DashboardItem> items )
    {
        this.items = items;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            Dashboard dashboard = (Dashboard) other;

            items.clear();
            items.addAll( dashboard.getItems() );
        }
    }
}
