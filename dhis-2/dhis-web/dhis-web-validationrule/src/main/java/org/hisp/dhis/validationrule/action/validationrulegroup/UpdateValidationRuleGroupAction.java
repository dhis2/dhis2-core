package org.hisp.dhis.validationrule.action.validationrulegroup;

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

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;

import com.opensymphony.xwork2.Action;

/**
* @author Lars Helge Overland
* @version $Id$
*/
public class UpdateValidationRuleGroupAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }
    
    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }
    
    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }
    
    private Set<String> groupMembers;

    public void setGroupMembers( Set<String> groupMembers )
    {
        this.groupMembers = groupMembers;
    }

    private Set<String> userGroupsToAlert;

    public void setUserGroupsToAlert( Set<String> userGroupsToAlert )
    {
        this.userGroupsToAlert = userGroupsToAlert;
    }

    private boolean alertByOrgUnits;

    public void setAlertByOrgUnits( boolean alertByOrgUnits )
    {
        this.alertByOrgUnits = alertByOrgUnits;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        ValidationRuleGroup group = validationRuleService.getValidationRuleGroup( id );
        
        group.setName( StringUtils.trimToNull( name ) );
        group.setDescription( StringUtils.trimToNull( description ) );
        group.getMembers().clear();
        
        if ( groupMembers != null )
        {
            for ( String id : groupMembers )
            {
                group.getMembers().add( validationRuleService.getValidationRule( Integer.valueOf( id ) ) );
            }
        }
        
        group.getUserGroupsToAlert().clear();

        if ( userGroupsToAlert != null )
        {
            for ( String id : userGroupsToAlert )
            {
                group.getUserGroupsToAlert().add( userGroupService.getUserGroup( Integer.valueOf( id ) ) );
            }
        }

        group.setAlertByOrgUnits( alertByOrgUnits );

        validationRuleService.updateValidationRuleGroup( group );
        
        return SUCCESS;
    }
}
