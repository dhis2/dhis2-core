package org.hisp.dhis.webportal.menu.action;

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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webportal.module.Module;
import org.hisp.dhis.webportal.module.ModuleManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetModulesAction
    implements Action
{
    @Autowired
    private ModuleManager manager;

    @Autowired
    private CurrentUserService currentUserService;
    
    private List<Module> modules;
    
    public List<Module> getModules()
    {
        return modules;
    }

    @Override
    public String execute()
        throws Exception
    {
        String contextPath = ContextUtils.getContextPath( ServletActionContext.getRequest() );
        
        modules = manager.getAccessibleMenuModulesAndApps( contextPath );

        User user = currentUserService.getCurrentUser();
        
        if ( user != null && user.getApps() != null && !user.getApps().isEmpty() )
        {
            final List<String> userApps = user.getApps();
            
            Collections.sort( modules, new Comparator<Module>()
            {
                @Override
                public int compare( Module m1, Module m2 )
                {
                    int i1 = userApps.indexOf( m1.getName() );
                    int i2 = userApps.indexOf( m2.getName() );

                    i1 = i1 == -1 ? 9999 : i1;
                    i2 = i2 == -1 ? 9999 : i2;
                    
                    return Integer.valueOf( i1 ).compareTo( Integer.valueOf( i2 ) );
                }
            } );
        }
        
        return SUCCESS;
    }
}
