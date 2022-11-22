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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

@Component
public class DashboardObjectBundleHook extends AbstractObjectBundleHook<Dashboard>
{
    @Override
    public void preUpdate( Dashboard dashboard, Dashboard persistedObject, ObjectBundle bundle )
    {
        // Make sure all DashboardItem's objects are loaded into preheat even if
        // currentUser doesn't have read access.
        Map<Class<? extends IdentifiableObject>, Set<String>> mapUIDs = collectDashboardItemsUid( dashboard );

        if ( mapUIDs.isEmpty() )
        {
            return;
        }

        mapUIDs.forEach( ( klass, ids ) -> bundle.getPreheat().put( bundle.getPreheatIdentifier(),
            manager.getNoAcl( klass, ids ) ) );
    }

    /**
     * Collects and returns all uids from all dashboard items found in the given
     * "objects".
     *
     * @param dashboard the {@link Dashboard}
     * @return the set of dashboard items uid
     */
    private Map<Class<? extends IdentifiableObject>, Set<String>> collectDashboardItemsUid( Dashboard dashboard )
    {
        if ( isEmpty( dashboard.getItems() ) )
        {
            return Map.of();
        }

        Map<Class<? extends IdentifiableObject>, Set<String>> mapItemUIDs = new HashMap<>();

        dashboard.getItems().forEach( item -> extractAllDashboardItemsUids( mapItemUIDs, item ) );

        return mapItemUIDs;
    }

    /**
     * Extracts all uids from all objects associated with the given item.
     *
     * @param item the {@link DashboardItem}
     * @return the set of dashboard items uid
     */
    private void extractAllDashboardItemsUids( Map<Class<? extends IdentifiableObject>, Set<String>> mapItemUIDs,
        DashboardItem item )
    {
        if ( item != null )
        {
            if ( item.getEmbeddedItem() != null )
            {
                mapItemUIDs.computeIfAbsent( item.getEmbeddedItem().getClass(), key -> new HashSet<>() )
                    .add( item.getEmbeddedItem().getUid() );
            }

            if ( isNotEmpty( item.getResources() ) )
            {
                mapItemUIDs.computeIfAbsent( Document.class, key -> new HashSet<>() )
                    .addAll( item.getResources().stream().map( BaseIdentifiableObject::getUid ).collect( toSet() ) );
            }

            if ( isNotEmpty( item.getReports() ) )
            {
                mapItemUIDs.computeIfAbsent( Report.class, key -> new HashSet<>() )
                    .addAll( item.getReports().stream().map( BaseIdentifiableObject::getUid ).collect( toSet() ) );
            }

            if ( isNotEmpty( item.getUsers() ) )
            {
                mapItemUIDs.computeIfAbsent( User.class, key -> new HashSet<>() )
                    .addAll( item.getUsers().stream().map( BaseIdentifiableObject::getUid ).collect( toSet() ) );
            }
        }
    }
}
