package org.hisp.dhis.api.mobile.controller;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hisp.dhis.api.mobile.NotAllowedException;
import org.hisp.dhis.api.mobile.model.DataStreamSerializable;
import org.hisp.dhis.api.mobile.model.MobileOrgUnitLinks;
import org.hisp.dhis.api.mobile.model.OrgUnits;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping( value = "/mobile" )
public class MobileClientController
    extends AbstractMobileController
{
    @Autowired
    private CurrentUserService currentUserService;

    @RequestMapping( method = RequestMethod.GET )
    @ResponseBody
    public OrgUnits getOrgUnitsForUser2_8( HttpServletRequest request )
        throws NotAllowedException
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw NotAllowedException.NO_USER;
        }

        Collection<OrganisationUnit> units = user.getOrganisationUnits();

        List<MobileOrgUnitLinks> unitList = new ArrayList<>();
        for ( OrganisationUnit unit : units )
        {
            unitList.add( getOrgUnit( unit, request ) );
        }
        OrgUnits orgUnits = new OrgUnits( unitList );
        orgUnits.setClientVersion( DataStreamSerializable.TWO_POINT_EIGHT );
        return orgUnits;
    }

    @RequestMapping( method = RequestMethod.GET, value = "/{version:.+}" )
    @ResponseBody
    public OrgUnits getOrgUnitsForUser( HttpServletRequest request, @PathVariable String version )
        throws NotAllowedException
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw NotAllowedException.NO_USER;
        }

        Collection<OrganisationUnit> units = user.getOrganisationUnits();

        List<MobileOrgUnitLinks> unitList = new ArrayList<>();
        for ( OrganisationUnit unit : units )
        {
            unitList.add( getOrgUnit( unit, request ) );
        }
        OrgUnits orgUnits = new OrgUnits( unitList );
        orgUnits.setClientVersion( version );
        return orgUnits;
    }

    @RequestMapping( method = RequestMethod.GET, value = "/{version}/LWUIT" )
    @ResponseBody
    public org.hisp.dhis.api.mobile.model.LWUITmodel.OrgUnits getOrgUnitsForUserLWUIT( HttpServletRequest request, @PathVariable String version )
        throws NotAllowedException
    {
        User user = currentUserService.getCurrentUser();

        if ( user == null )
        {
            throw NotAllowedException.NO_USER;
        }

        Collection<OrganisationUnit> units = user.getOrganisationUnits();
        
        List<org.hisp.dhis.api.mobile.model.LWUITmodel.MobileOrgUnitLinks> unitList = new ArrayList<>();
        for ( OrganisationUnit unit : units )
        {
            unitList.add( getTrackerOrgUnit( unit, request ) );
        }
        org.hisp.dhis.api.mobile.model.LWUITmodel.OrgUnits orgUnits = new org.hisp.dhis.api.mobile.model.LWUITmodel.OrgUnits( unitList );
        orgUnits.setClientVersion( version );
        return orgUnits;
    }

    private org.hisp.dhis.api.mobile.model.LWUITmodel.MobileOrgUnitLinks getTrackerOrgUnit( OrganisationUnit unit,
        HttpServletRequest request )
    {
        org.hisp.dhis.api.mobile.model.LWUITmodel.MobileOrgUnitLinks orgUnit = new org.hisp.dhis.api.mobile.model.LWUITmodel.MobileOrgUnitLinks();

        orgUnit.setId( unit.getId() );
        orgUnit.setName( unit.getShortName() );

        orgUnit.setDownloadAllUrl( getUrl( request, unit.getId(), "all" ) );
        orgUnit.setUpdateActivityPlanUrl( getUrl( request, unit.getId(), "activitiyplan" ) );
        orgUnit.setUploadFacilityReportUrl( getUrl( request, unit.getId(), "dataSets" ) );
        orgUnit.setDownloadFacilityReportUrl( getUrl( request, unit.getId(), "dataSetValue" ) );
        orgUnit.setUploadActivityReportUrl( getUrl( request, unit.getId(), "activities" ) );
        orgUnit.setUpdateDataSetUrl( getUrl( request, unit.getId(), "updateDataSets" ) );
        orgUnit.setChangeUpdateDataSetLangUrl( getUrl( request, unit.getId(), "changeLanguageDataSet" ) );
        orgUnit.setSearchUrl( getUrl( request, unit.getId(), "search" ) );
        orgUnit.setUpdateNewVersionUrl( getUrl( request, unit.getId(), "updateNewVersionUrl" ) );
        orgUnit.setSendFeedbackUrl( getUrl( request, unit.getId(), "sendFeedback" ) );
        orgUnit.setFindUserUrl( getUrl( request, unit.getId(), "findUser" ) );
        orgUnit.setSendMessageUrl( getUrl( request, unit.getId(), "sendMessage" ) );
        orgUnit.setDownloadMessageConversationUrl( getUrl( request, unit.getId(), "downloadMessageConversation" ) );
        orgUnit.setGetMessageUrl( getUrl( request, unit.getId(), "getMessage" ) );
        orgUnit.setReplyMessageUrl( getUrl( request, unit.getId(), "replyMessage" ) );
        orgUnit.setDownloadInterpretationUrl( getUrl( request, unit.getId(), "downloadInterpretation" ) );
        orgUnit.setPostInterpretationUrl( getUrl( request, unit.getId(), "postInterpretation" ) );
        orgUnit.setPostCommentUrl( getUrl( request, unit.getId(), "postComment" ) );
        orgUnit.setUpdateContactUrl( getUrl( request, unit.getId(), "updateContactForMobile" ) );
        orgUnit.setFindPatientUrl( getUrl( request, unit.getId(), "findPatient" ) );
        orgUnit.setRegisterPersonUrl( getUrl( request, unit.getId(), "registerPerson" ) );
        orgUnit.setUploadProgramStageUrl( getUrl( request, unit.getId(), "uploadProgramStage" ) );
        orgUnit.setEnrollProgramUrl( getUrl( request, unit.getId(), "enrollProgram" ) );
        orgUnit.setGetVariesInfoUrl( getUrl( request, unit.getId(), "getVariesInfo" ) );
        orgUnit.setAddRelationshipUrl( getUrl( request, unit.getId(), "addRelationship" ) );
        orgUnit.setDownloadAnonymousProgramUrl( getUrl( request, unit.getId(), "downloadAnonymousProgram" ) );
        orgUnit.setFindProgramUrl( getUrl( request, unit.getId(), "findProgram" ) );
        orgUnit.setFindPatientInAdvancedUrl( getUrl( request, unit.getId(), "findPatientInAdvanced" ) );
        orgUnit.setFindPatientsUrl( getUrl( request, unit.getId(), "findPatients" ) );
        orgUnit.setFindVisitScheduleUrl( getUrl( request, unit.getId(), "findVisitSchedule" ) );
        orgUnit.setFindLostToFollowUpUrl( getUrl( request, unit.getId(), "findLostToFollowUp" ) );
        orgUnit.setHandleLostToFollowUpUrl( getUrl( request, unit.getId(), "handleLostToFollowUp" ) );
        orgUnit.setGenerateRepeatableEventUrl( getUrl( request, unit.getId(), "generateRepeatableEvent" ) );
        orgUnit.setUploadSingleEventWithoutRegistration( getUrl( request, unit.getId(),
            "uploadSingleEventWithoutRegistration" ) );
        orgUnit.setCompleteProgramInstanceUrl( getUrl( request, unit.getId(), "completeProgramInstance" ) );
        orgUnit.setRegisterRelativeUrl( getUrl( request, unit.getId(), "registerRelative" ) );

        // generate URL for download new version
        String full = UrlUtils.buildFullRequestUrl( request );
        String root = full.substring( 0, full.length() - UrlUtils.buildRequestUrl( request ).length() );
        String updateNewVersionUrl = root + "/dhis-web-api-mobile/updateClient.action";
        orgUnit.setUpdateNewVersionUrl( updateNewVersionUrl );

        return orgUnit;
    }

    private MobileOrgUnitLinks getOrgUnit( OrganisationUnit unit, HttpServletRequest request )
    {
        MobileOrgUnitLinks orgUnit = new MobileOrgUnitLinks();

        orgUnit.setId( unit.getId() );
        orgUnit.setName( unit.getShortName() );

        orgUnit.setDownloadAllUrl( getUrl( request, unit.getId(), "all" ) );
        orgUnit.setUpdateActivityPlanUrl( getUrl( request, unit.getId(), "activitiyplan" ) );
        orgUnit.setUploadFacilityReportUrl( getUrl( request, unit.getId(), "dataSets" ) );
        orgUnit.setDownloadFacilityReportUrl( getUrl( request, unit.getId(), "dataSetValue" ) );
        orgUnit.setUploadActivityReportUrl( getUrl( request, unit.getId(), "activities" ) );
        orgUnit.setUpdateDataSetUrl( getUrl( request, unit.getId(), "updateDataSets" ) );
        orgUnit.setChangeUpdateDataSetLangUrl( getUrl( request, unit.getId(), "changeLanguageDataSet" ) );
        orgUnit.setSearchUrl( getUrl( request, unit.getId(), "search" ) );
        orgUnit.setUpdateNewVersionUrl( getUrl( request, unit.getId(), "updateNewVersionUrl" ) );
        orgUnit.setSendFeedbackUrl( getUrl( request, unit.getId(), "sendFeedback" ) );
        orgUnit.setFindUserUrl( getUrl( request, unit.getId(), "findUser" ) );
        orgUnit.setSendMessageUrl( getUrl( request, unit.getId(), "sendMessage" ) );
        orgUnit.setDownloadMessageConversationUrl( getUrl( request, unit.getId(), "downloadMessageConversation" ) );
        orgUnit.setGetMessageUrl( getUrl( request, unit.getId(), "getMessage" ) );
        orgUnit.setReplyMessageUrl( getUrl( request, unit.getId(), "replyMessage" ) );
        orgUnit.setDownloadInterpretationUrl( getUrl( request, unit.getId(), "downloadInterpretation" ) );
        orgUnit.setPostInterpretationUrl( getUrl( request, unit.getId(), "postInterpretation" ) );
        orgUnit.setPostCommentUrl( getUrl( request, unit.getId(), "postComment" ) );
        orgUnit.setUpdateContactUrl( getUrl( request, unit.getId(), "updateContactForMobile" ) );
        orgUnit.setFindPatientUrl( getUrl( request, unit.getId(), "findPatient" ) );
        orgUnit.setRegisterPersonUrl( getUrl( request, unit.getId(), "registerPerson" ) );
        orgUnit.setUploadProgramStageUrl( getUrl( request, unit.getId(), "uploadProgramStage" ) );
        orgUnit.setEnrollProgramUrl( getUrl( request, unit.getId(), "enrollProgram" ) );
        orgUnit.setGetVariesInfoUrl( getUrl( request, unit.getId(), "getVariesInfo" ) );
        orgUnit.setAddRelationshipUrl( getUrl( request, unit.getId(), "addRelationship" ) );
        orgUnit.setDownloadAnonymousProgramUrl( getUrl( request, unit.getId(), "downloadAnonymousProgram" ) );
        orgUnit.setFindProgramUrl( getUrl( request, unit.getId(), "findProgram" ) );
        orgUnit.setFindPatientInAdvancedUrl( getUrl( request, unit.getId(), "findPatientInAdvanced" ) );
        orgUnit.setFindPatientsUrl( getUrl( request, unit.getId(), "findPatients" ) );
        orgUnit.setFindVisitScheduleUrl( getUrl( request, unit.getId(), "findVisitSchedule" ) );
        orgUnit.setFindLostToFollowUpUrl( getUrl( request, unit.getId(), "findLostToFollowUp" ) );
        orgUnit.setHandleLostToFollowUpUrl( getUrl( request, unit.getId(), "handleLostToFollowUp" ) );
        orgUnit.setGenerateRepeatableEventUrl( getUrl( request, unit.getId(), "generateRepeatableEvent" ) );
        orgUnit.setUploadSingleEventWithoutRegistration( getUrl( request, unit.getId(),
            "uploadSingleEventWithoutRegistration" ) );
        orgUnit.setCompleteProgramInstanceUrl( getUrl( request, unit.getId(), "completeProgramInstance" ) );
        orgUnit.setRegisterRelativeUrl( getUrl( request, unit.getId(), "registerRelative" ) );

        // generate URL for download new version
        String full = UrlUtils.buildFullRequestUrl( request );
        String root = full.substring( 0, full.length() - UrlUtils.buildRequestUrl( request ).length() );
        String updateNewVersionUrl = root + "/dhis-web-api-mobile/updateClient.action";
        orgUnit.setUpdateNewVersionUrl( updateNewVersionUrl );

        return orgUnit;
    }

    private static String getUrl( HttpServletRequest request, int id, String path )
    {
        String url = UrlUtils.buildFullRequestUrl( request );
        if ( url.endsWith( "/" ) )
        {
            url = url + "orgUnits/" + id + "/" + path;
        }
        else
        {
            url = url + "/orgUnits/" + id + "/" + path;
        }
        return url;
    }
}
