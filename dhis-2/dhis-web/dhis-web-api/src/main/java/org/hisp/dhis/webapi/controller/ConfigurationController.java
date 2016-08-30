package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( "/configuration" )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class ConfigurationController
{
    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DhisConfigurationProvider config;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET )
    public @ResponseBody Configuration getConfiguration( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @ResponseStatus( value = HttpStatus.OK )
    @RequestMapping( value = "/systemId", method = RequestMethod.GET )
    public @ResponseBody String getSystemId( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getSystemId();
    }

    @RequestMapping( value = "/feedbackRecipients", method = RequestMethod.GET )
    public @ResponseBody UserGroup getFeedbackRecipients( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getFeedbackRecipients();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/feedbackRecipients", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setFeedbackRecipients( @RequestBody String uid )
        throws NotFoundException
    {
        UserGroup group = identifiableObjectManager.get( UserGroup.class, uid );

        if ( group == null )
        {
            throw new NotFoundException( "User group", uid );
        }

        Configuration config = configurationService.getConfiguration();

        config.setFeedbackRecipients( group );

        configurationService.setConfiguration( config );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/feedbackRecipients", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeFeedbackRecipients()
    {
        Configuration config = configurationService.getConfiguration();

        config.setFeedbackRecipients( null );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/offlineOrganisationUnitLevel", method = RequestMethod.GET )
    public @ResponseBody OrganisationUnitLevel getOfflineOrganisationUnitLevel( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getOfflineOrganisationUnitLevel();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/offlineOrganisationUnitLevel", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setOfflineOrganisationUnitLevel( @RequestBody String uid )
        throws NotFoundException
    {
        OrganisationUnitLevel organisationUnitLevel = identifiableObjectManager.get( OrganisationUnitLevel.class, uid );

        if ( organisationUnitLevel == null )
        {
            throw new NotFoundException( "Organisation unit level", uid );
        }

        Configuration config = configurationService.getConfiguration();

        config.setOfflineOrganisationUnitLevel( organisationUnitLevel );

        configurationService.setConfiguration( config );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/offlineOrganisationUnitLevel", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeOfflineOrganisationUnitLevel()
    {
        Configuration config = configurationService.getConfiguration();

        config.setOfflineOrganisationUnitLevel( null );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/infrastructuralIndicators", method = RequestMethod.GET )
    public @ResponseBody IndicatorGroup getInfrastructuralIndicators( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getInfrastructuralIndicators();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/infrastructuralIndicators", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setInfrastructuralIndicators( @RequestBody String uid )
        throws NotFoundException
    {
        IndicatorGroup group = identifiableObjectManager.get( IndicatorGroup.class, uid );

        if ( group == null )
        {
            throw new NotFoundException( "Indicator group", uid );
        }

        Configuration config = configurationService.getConfiguration();

        config.setInfrastructuralIndicators( group );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/infrastructuralDataElements", method = RequestMethod.GET )
    public @ResponseBody DataElementGroup getInfrastructuralDataElements( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getInfrastructuralDataElements();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/infrastructuralDataElements", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setInfrastructuralDataElements( @RequestBody String uid )
        throws NotFoundException
    {
        DataElementGroup group = identifiableObjectManager.get( DataElementGroup.class, uid );

        if ( group == null )
        {
            throw new NotFoundException( "Data element group", uid );
        }

        Configuration config = configurationService.getConfiguration();

        config.setInfrastructuralDataElements( group );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/infrastructuralPeriodType", method = RequestMethod.GET )
    public @ResponseBody BaseIdentifiableObject getInfrastructuralPeriodType( Model model, HttpServletRequest request )
    {
        String name = configurationService.getConfiguration().getInfrastructuralPeriodTypeDefaultIfNull().getName();
        return new BaseIdentifiableObject( name, name, name );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/infrastructuralPeriodType", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setInfrastructuralPeriodType( @RequestBody String name )
        throws NotFoundException
    {
        PeriodType periodType = PeriodType.getPeriodTypeByName( name );

        if ( periodType == null )
        {
            throw new NotFoundException( "Period type", name );
        }

        Configuration config = configurationService.getConfiguration();

        periodType = periodService.reloadPeriodType( periodType );

        config.setInfrastructuralPeriodType( periodType );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/selfRegistrationRole", method = RequestMethod.GET )
    public @ResponseBody UserAuthorityGroup getSelfRegistrationRole( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getSelfRegistrationRole();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/selfRegistrationRole", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setSelfRegistrationRole( @RequestBody String uid )
        throws NotFoundException
    {
        UserAuthorityGroup userGroup = identifiableObjectManager.get( UserAuthorityGroup.class, uid );

        if ( userGroup == null )
        {
            throw new NotFoundException( "User authority group", uid );
        }

        Configuration config = configurationService.getConfiguration();

        config.setSelfRegistrationRole( userGroup );

        configurationService.setConfiguration( config );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/selfRegistrationRole", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeSelfRegistrationRole()
    {
        Configuration config = configurationService.getConfiguration();

        config.setSelfRegistrationRole( null );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/selfRegistrationOrgUnit", method = RequestMethod.GET )
    public @ResponseBody OrganisationUnit getSelfRegistrationOrgUnit( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getSelfRegistrationOrgUnit();
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/selfRegistrationOrgUnit", method = RequestMethod.POST )
    public void setSelfRegistrationOrgUnit( @RequestBody String uid )
        throws NotFoundException
    {
        OrganisationUnit orgunit = identifiableObjectManager.get( OrganisationUnit.class, uid );

        if ( orgunit == null )
        {
            throw new NotFoundException( "Organisation unit", uid );
        }

        Configuration config = configurationService.getConfiguration();

        config.setSelfRegistrationOrgUnit( orgunit );

        configurationService.setConfiguration( config );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/selfRegistrationOrgUnit", method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeSelfRegistrationOrgUnit()
    {
        Configuration config = configurationService.getConfiguration();

        config.setSelfRegistrationOrgUnit( null );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/remoteServerUrl", method = RequestMethod.GET )
    public @ResponseBody String getRemoteServerUrl( Model model, HttpServletRequest request )
    {
        return (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL );
    }

    @RequestMapping( value = "/remoteServerUsername", method = RequestMethod.GET )
    public @ResponseBody String getRemoteServerUsername( Model model, HttpServletRequest request )
    {
        return (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
    }

    @RequestMapping( value = "/corsWhitelist", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody Set<String> getCorsWhitelist( Model model, HttpServletRequest request )
    {
        return configurationService.getConfiguration().getCorsWhitelist();
    }

    @SuppressWarnings( "unchecked" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_SYSTEM_SETTING')" )
    @RequestMapping( value = "/corsWhitelist", method = RequestMethod.POST, consumes = "application/json" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void setCorsWhitelist( @RequestBody String input )
        throws IOException
    {
        Set<String> corsWhitelist = renderService.fromJson( input, Set.class );

        Configuration config = configurationService.getConfiguration();

        config.setCorsWhitelist( corsWhitelist );

        configurationService.setConfiguration( config );
    }

    @RequestMapping( value = "/systemReadOnlyMode", method = RequestMethod.GET )
    public @ResponseBody boolean getSystemReadOnlyMode( Model model, HttpServletRequest request )
    {
        return config.isReadOnlyMode();
    }
}
