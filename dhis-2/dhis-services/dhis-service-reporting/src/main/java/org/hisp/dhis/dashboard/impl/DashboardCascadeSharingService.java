/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.dashboard.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.sharing.AbstractCascadeSharingService;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.visualization.VisualizationCascadeSharingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCascadeSharingService
    extends AbstractCascadeSharingService implements CascadeSharingService<Dashboard>
{
    private final IdentifiableObjectManager manager;

    private final VisualizationCascadeSharingService visualizationCascadeSharingService;

    public DashboardCascadeSharingService( @NonNull IdentifiableObjectManager manager,
        @NonNull VisualizationCascadeSharingService visualizationCascadeSharingService )
    {
        this.manager = manager;
        this.visualizationCascadeSharingService = visualizationCascadeSharingService;
    }

    @Override
    @Transactional
    public List<ErrorReport> cascadeSharing( Dashboard dashboard, CascadeSharingParameters parameters )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        dashboard.getItems().forEach( dashboardItem -> {
            switch ( dashboardItem.getType() )
            {
            case VISUALIZATION:
                mergeSharing( dashboard, dashboardItem.getVisualization() );
                errorReports.addAll(
                    visualizationCascadeSharingService.cascadeSharing( dashboardItem.getVisualization(), parameters ) );
                break;
            case MAP:
                errorReports.addAll( mergeSharing( dashboard, dashboardItem.getMap() ) );
                break;
            case REPORT_TABLE:
                break;
            case EVENT_REPORT:
                break;
            case REPORTS:
                break;
            case RESOURCES:
                break;
            case APP:
                break;
            default:
                break;
            }
        } );

        if ( !parameters.isDryRun() )
        {
            manager.update( dashboard );
        }

        return errorReports;
    }
}
