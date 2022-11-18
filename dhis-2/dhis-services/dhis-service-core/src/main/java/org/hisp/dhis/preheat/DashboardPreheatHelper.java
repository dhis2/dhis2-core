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
package org.hisp.dhis.preheat;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;

/**
 * This class encapsulates all necessary logic to handle the specific sharing
 * settings requirements for Dashboard and its items.
 *
 * @author maikel arabori
 */
class DashboardPreheatHelper
{
    private DashboardPreheatHelper()
    {
        throw new UnsupportedOperationException( "helper" );
    }

    /**
     * Collects and returns all uids from all dashboard items found in the given
     * "objects".
     *
     * @param objects the map that contains lists of {@link IdentifiableObject}
     * @return the set of dashboard items uid
     */
    static Set<String> collectDashboardItemsUid( Set<Class<? extends IdentifiableObject>> types,
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        boolean hasDashboardClass = types.contains( Dashboard.class );

        if ( isEmpty( objects ) || !hasDashboardClass )
        {
            return emptySet();
        }

        Set<String> itemsUid = new HashSet<>();

        defaultIfNull( objects.get( Dashboard.class ), emptyList() )
            .forEach( obj -> itemsUid.addAll( collectDashboardItemsUid( (Dashboard) obj ) ) );

        return itemsUid;
    }

    /**
     * Collects and returns all uids from all dashboard items found in the given
     * "objects".
     *
     * @param dashboard the {@link Dashboard}
     * @return the set of dashboard items uid
     */
    private static Set<String> collectDashboardItemsUid( Dashboard dashboard )
    {
        Set<String> itemsUid = new HashSet<>();

        if ( isNotEmpty( dashboard.getItems() ) )
        {
            for ( DashboardItem item : dashboard.getItems() )
            {
                itemsUid.addAll( extractAllDashboardItemsUids( item ) );
            }
        }

        return itemsUid;
    }

    /**
     * Extracts all uids from all objects associated with the given item.
     *
     * @param item the {@link DashboardItem}
     * @return the set of dashboard items uid
     */
    private static Set<String> extractAllDashboardItemsUids( DashboardItem item )
    {
        Set<String> itemsUid = new HashSet<>();

        if ( item != null )
        {
            if ( item.getEmbeddedItem() != null )
            {
                itemsUid.add( item.getEmbeddedItem().getUid() );
            }

            if ( isNotEmpty( item.getResources() ) )
            {
                itemsUid.addAll(
                    item.getResources().stream().map( BaseIdentifiableObject::getUid ).collect( toSet() ) );
            }

            if ( isNotEmpty( item.getReports() ) )
            {
                itemsUid
                    .addAll( item.getReports().stream().map( BaseIdentifiableObject::getUid ).collect( toSet() ) );
            }

            if ( isNotEmpty( item.getUsers() ) )
            {
                itemsUid
                    .addAll( item.getUsers().stream().map( BaseIdentifiableObject::getUid ).collect( toSet() ) );
            }
        }

        return itemsUid;
    }
}
