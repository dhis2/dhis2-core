package org.hisp.dhis.trackedentity.action.programindicatorgroup;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.program.ProgramIndicatorGroup;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Viet Nguyen
 */
public class ValidateProgramIndicatorGroupAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    private Integer id;

    private String name;
    
    private String message;

    private I18n i18n;

    // -------------------------------------------------------------------------
    // Getter && Setter
    // -------------------------------------------------------------------------

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }
    
    public void setId( Integer id )
    {
        this.id = id;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        name = name.trim();

        ProgramIndicatorGroup match = programIndicatorService.getProgramIndicatorGroupByName( name );

        if ( match != null && (id == null || match.getId() != id.intValue()) )
        {
            message = i18n.getString( "name_in_use" );

            return INPUT;
        }
        
        message = i18n.getString( "everything_is_ok" );

        return SUCCESS;
    }
}
