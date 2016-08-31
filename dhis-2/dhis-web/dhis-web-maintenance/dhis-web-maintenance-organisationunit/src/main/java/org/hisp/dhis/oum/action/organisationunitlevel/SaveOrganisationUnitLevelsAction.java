package org.hisp.dhis.oum.action.organisationunitlevel;

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

import static org.hisp.dhis.system.util.MathUtils.isInteger;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.util.ContextUtils;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class SaveOrganisationUnitLevelsAction
    implements Action
{
    private static final String LEVEL_PARAM_PREFIX = "level";
    private static final String OFFLINE_LEVELS_PARAM_PREFIX = "offline";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;
    
    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {        
        Map<String, String> params = ContextUtils.getParameterMap( ServletActionContext.getRequest() );
        
        Set<Integer> levels = new HashSet<>();
        
        for ( Entry<String, String> param : params.entrySet() )
        {
            String key = param.getKey();
            String value = param.getValue();
            
            if ( key != null && key.startsWith( LEVEL_PARAM_PREFIX ) )
            {
                if ( value != null && !value.isEmpty() )
                {
                    int level = Integer.parseInt( key.substring( LEVEL_PARAM_PREFIX.length(), key.length() ) );
                    
                    String offlineLevelsValue = params.get( OFFLINE_LEVELS_PARAM_PREFIX + level );
                    
                    Integer offlineLevels = isInteger( offlineLevelsValue ) ? Integer.parseInt( offlineLevelsValue ) : null;
                    
                    organisationUnitService.addOrUpdateOrganisationUnitLevel( new OrganisationUnitLevel( level, value, offlineLevels ) );
                    
                    levels.add( level );
                }
            }            
        }
        
        organisationUnitService.pruneOrganisationUnitLevels( levels );
        
        return SUCCESS;
    }
}
