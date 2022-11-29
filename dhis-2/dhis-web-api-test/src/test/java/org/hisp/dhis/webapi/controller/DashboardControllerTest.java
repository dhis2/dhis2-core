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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClient.Body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private AclService aclService;

    @Test
    void testUpdateWithNonAccessibleItems()
    {
        POST( "/metadata", Body( "dashboard/create_dashboard.json" ) ).content( HttpStatus.OK );

        User userA = userService.getUser( "XThzKnyzeYW" );

        Dashboard dashboard = manager.get( Dashboard.class, "f1OijtLnf8a" );
        assertNotNull( dashboard );
        assertEquals( 1, dashboard.getItems().size() );

        Visualization visualization = dashboard.getItems().stream()
            .filter( item -> item.getUid().equals( "KnmKNIFiAwC" ) ).findFirst().get().getVisualization();
        assertNotNull( visualization );
        assertFalse( aclService.canRead( userA, visualization ) );

        switchContextToUser( userA );
        assertTrue( aclService.canUpdate( userA, dashboard ) );
        assertTrue( aclService.canRead( userA, dashboard ) );
        System.out.println( "dashboard.getSharing() = " + dashboard.getSharing() );

        // UserA update dashboard without read access permission to
        // Visualization.
        HttpResponse response = PUT( "/dashboards/f1OijtLnf8a", Body( "dashboard/update_dashboard.json" ) );
        JsonResponse jsonResponse = response.content( HttpStatus.OK );
        dashboard = manager.get( Dashboard.class, "f1OijtLnf8a" );
        assertEquals( 2, dashboard.getItems().size() );
        visualization = dashboard.getItems().stream().filter( item -> item.getUid().equals( "KnmKNIFiAwC" ) )
            .findFirst().get().getVisualization();
        assertNotNull( visualization );
        assertEquals( "gyYXi0rXAIc", visualization.getUid() );
    }
}
