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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 */
public class ShowUpdateValidationRuleGroupFormAction
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

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private ValidationRuleGroup validationRuleGroup;

    public ValidationRuleGroup getValidationRuleGroup()
    {
        return validationRuleGroup;
    }

    private List<ValidationRule> availableValidationRules = new ArrayList<>();

    public List<ValidationRule> getAvailableValidationRules()
    {
        return availableValidationRules;
    }

    private List<ValidationRule> groupMembers = new ArrayList<>();

    public List<ValidationRule> getGroupMembers()
    {
        return groupMembers;
    }

    private List<UserGroup> availableUserGroupsToAlert = new ArrayList<>();
    
    public List<UserGroup> getAvailableUserGroupsToAlert()
    {
        return availableUserGroupsToAlert;
    }

    private List<UserGroup> userGroupsToAlert = new ArrayList<>();

    public List<UserGroup> getUserGroupsToAlert()
    {
        return userGroupsToAlert;
    }

    private boolean alertByOrgUnits;

    public boolean getAlertByOrgUnits()
    {
        return alertByOrgUnits;
    }
   
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        validationRuleGroup = validationRuleService.getValidationRuleGroup( id );

        groupMembers = new ArrayList<>( validationRuleGroup.getMembers() );

        Collections.sort( groupMembers, IdentifiableObjectNameComparator.INSTANCE );
        
        availableUserGroupsToAlert = new ArrayList<>( userGroupService.getAllUserGroups() );

        userGroupsToAlert = new ArrayList<>( validationRuleGroup.getUserGroupsToAlert() );
        
        Collections.sort( userGroupsToAlert, IdentifiableObjectNameComparator.INSTANCE );

        alertByOrgUnits = validationRuleGroup.isAlertByOrgUnits();

        return SUCCESS;
    }
}
