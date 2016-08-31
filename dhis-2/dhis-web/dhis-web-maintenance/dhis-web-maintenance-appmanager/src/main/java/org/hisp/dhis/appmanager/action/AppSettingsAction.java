package org.hisp.dhis.appmanager.action;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.List;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Saptarshi Purkayastha
 */
public class AppSettingsAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private AppManager appManager;
    
    @Autowired
    private LocationManager locationManager;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String appFolderPath;

    public String getAppFolderPath()
    {
        return appFolderPath;
    }

    private String appBaseUrl;

    public String getAppBaseUrl()
    {
        return appBaseUrl;
    }

    private List<App> appList;

    public List<App> getAppList()
    {
        return appList;
    }
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        appFolderPath = appManager.getAppFolderPath();

        if ( appFolderPath == null || appFolderPath.isEmpty() )
        {
            appFolderPath = locationManager.getExternalDirectoryPath() + AppManager.APPS_DIR;            
            appManager.setAppFolderPath( appFolderPath );
        }
        
        appBaseUrl = appManager.getAppBaseUrl();

        if ( appBaseUrl == null || appBaseUrl.isEmpty() )
        {
            String contextPath = ContextUtils.getContextPath( ServletActionContext.getRequest() );
            appBaseUrl = contextPath + AppManager.APPS_API_PATH;
            appManager.setAppBaseUrl( appBaseUrl );
        }
                
        appManager.reloadApps();

        appList = appManager.getApps();
        
        return SUCCESS;
    }
}
