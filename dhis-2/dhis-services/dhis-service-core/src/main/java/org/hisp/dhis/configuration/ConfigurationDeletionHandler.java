package org.hisp.dhis.configuration;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.lang3.StringUtils;

import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ConfigurationDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ConfigurationService configService;
    
    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    protected String getClassName()
    {
        return Configuration.class.getSimpleName();
    }

    @Override
    public String allowDeleteUserGroup( UserGroup userGroup )
    {
        UserGroup feedbackRecipients = configService.getConfiguration().getFeedbackRecipients();
        
        return ( feedbackRecipients != null && feedbackRecipients.equals( userGroup ) ) ? StringUtils.EMPTY : null;
    }

    @Override
    public String allowDeleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        DataElementGroup infrastructuralDataElements = configService.getConfiguration().getInfrastructuralDataElements();
        
        return ( infrastructuralDataElements != null && infrastructuralDataElements.equals( dataElementGroup ) ) ? StringUtils.EMPTY : null;
    }
    
    @Override
    public String allowDeleteIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        IndicatorGroup infrastructuralIndicators = configService.getConfiguration().getInfrastructuralIndicators();
        
        return ( infrastructuralIndicators != null && infrastructuralIndicators.equals( indicatorGroup ) ) ? StringUtils.EMPTY : null;
    }
    
    @Override
    public String allowDeleteOrganisationUnitLevel( OrganisationUnitLevel level )
    {
        OrganisationUnitLevel offlineLevel = configService.getConfiguration().getOfflineOrganisationUnitLevel();
        
        return ( offlineLevel != null && offlineLevel.equals( level ) ) ? StringUtils.EMPTY : null;
    }
    
    @Override
    public String allowDeleteOrganisationUnit( OrganisationUnit organisationUnit )
    {
        OrganisationUnit selfRegOrgUnit = configService.getConfiguration().getSelfRegistrationOrgUnit();

        return ( selfRegOrgUnit != null && selfRegOrgUnit.equals( organisationUnit ) ) ? StringUtils.EMPTY : null;
    }

    @Override
    public String allowDeleteUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        UserAuthorityGroup selfRegRole = configService.getConfiguration().getSelfRegistrationRole();
        
        return ( selfRegRole != null && selfRegRole.equals( userAuthorityGroup ) ) ? StringUtils.EMPTY : null;
    }    
}
