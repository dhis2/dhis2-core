package org.hisp.dhis.mobile.action;

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

import com.opensymphony.xwork2.Action;

/**
 * @author Nguyen Kim Lai
 */
public class J2meClientUpdateAction
    implements Action
{

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------
/*
    private double version;

    public void setVersion( double version )
    {
        this.version = version;
    }

    private String autoUpdate;

    public void setAutoUpdate( String autoUpdate )
    {
        this.autoUpdate = autoUpdate;
    }*/

    @Override
    public String execute()
        throws Exception
    {

        //TO DO: reimplement using SystemSetting
        
        // List<TrackedEntityMobileSetting> list;
        //
        // list = (List<TrackedEntityMobileSetting>)
        // mobileSettingService.getCurrentSetting();
        //
        // if( list.size() == 0 )
        // {
        // trackedEntityMobileSetting = new TrackedEntityMobileSetting();
        // }
        // else
        // {
        // trackedEntityMobileSetting = list.get( 0 );
        // }
        // if ( this.version != 0 )
        // {
        // trackedEntityMobileSetting.setVersionToUpdate( this.version );
        // }
        // if ( autoUpdate != null && autoUpdate.equals( "yes" ) )
        // {
        // trackedEntityMobileSetting.setAutoUpdateClient( true );
        // }
        //
        // if ( autoUpdate != null && autoUpdate.equals( "no" ) )
        // {
        // trackedEntityMobileSetting.setAutoUpdateClient( false );
        // }
        //
        // mobileSettingService.saveTrackedEntityMobileSetting(
        // this.trackedEntityMobileSetting );
        return SUCCESS;
    }
}
