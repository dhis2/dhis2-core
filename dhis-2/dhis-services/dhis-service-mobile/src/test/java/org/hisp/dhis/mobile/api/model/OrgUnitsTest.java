package org.hisp.dhis.mobile.api.model;

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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hisp.dhis.api.mobile.model.MobileOrgUnitLinks;
import org.hisp.dhis.api.mobile.model.OrgUnits;
import org.junit.Test;

public class OrgUnitsTest
{
    @Test
    public void testSerialization()
        throws IOException
    {
        MobileOrgUnitLinks unit = createOrgUnit();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream( baos );

        OrgUnits units = new OrgUnits();
        units.setOrgUnits( Arrays.asList( new MobileOrgUnitLinks[] { unit } ) );
        units.serializeVersion2_10( dos );
        dos.flush();

        OrgUnits units2 = new OrgUnits();
        units2.deSerialize( new DataInputStream( new ByteArrayInputStream( baos.toByteArray() ) ) );
        List<MobileOrgUnitLinks> unitList = units2.getOrgUnits();
        assertEquals( 1, unitList.size() );

        MobileOrgUnitLinks unit2 = unitList.get( 0 );
        assertEquals( unit.getName(), unit2.getName() );
        assertEquals( unit.getId(), unit2.getId() );
    }

    private MobileOrgUnitLinks createOrgUnit()
    {
        MobileOrgUnitLinks orgUnit = new MobileOrgUnitLinks();

        orgUnit.setId( 1 );
        orgUnit.setName( "name" );
        orgUnit.setDownloadAllUrl( "all" );
        orgUnit.setUpdateActivityPlanUrl( "activitiyplan" );
        orgUnit.setUploadFacilityReportUrl( "dataSets" );
        orgUnit.setDownloadFacilityReportUrl( "dataSetValue" );
        orgUnit.setUploadActivityReportUrl( "activities" );
        orgUnit.setUpdateDataSetUrl( "updateDataSets" );
        orgUnit.setChangeUpdateDataSetLangUrl( "changeLanguageDataSet" );
        orgUnit.setSearchUrl( "search" );
        orgUnit.setUpdateNewVersionUrl( "updateNewVersionUrl" );
        orgUnit.setSendFeedbackUrl( "sendFeedback" );
        orgUnit.setFindUserUrl( "findUser" );
        orgUnit.setSendMessageUrl( "sendMessage" );
        orgUnit.setDownloadMessageConversationUrl( "downloadMessageConversation" );
        orgUnit.setGetMessageUrl( "getMessage" );
        orgUnit.setReplyMessageUrl( "replyMessage" );
        orgUnit.setDownloadInterpretationUrl( "downloadInterpretation" );
        orgUnit.setPostInterpretationUrl( "postInterpretation" );
        orgUnit.setPostCommentUrl( "postComment" );
        orgUnit.setUpdateContactUrl( "updateContactForMobile" );
        orgUnit.setFindPatientUrl( "findPatient" );
        orgUnit.setRegisterPersonUrl( "registerPerson" );
        orgUnit.setUploadProgramStageUrl( "uploadProgramStage" );
        orgUnit.setEnrollProgramUrl( "enrollProgram" );
        orgUnit.setGetVariesInfoUrl( "getVariesInfo" );
        orgUnit.setAddRelationshipUrl( "addRelationship" );
        orgUnit.setDownloadAnonymousProgramUrl( "downloadAnonymousProgram" );
        orgUnit.setFindProgramUrl( "findProgram" );
        orgUnit.setFindPatientInAdvancedUrl( "findPatientInAdvanced" );
        orgUnit.setFindPatientsUrl( "findPatients" );
        orgUnit.setFindVisitScheduleUrl( "findVisitSchedule" );
        orgUnit.setFindLostToFollowUpUrl( "findLostToFollowUp" );
        orgUnit.setHandleLostToFollowUpUrl( "handleLostToFollowUp" );
        orgUnit.setGenerateRepeatableEventUrl( "generateRepeatableEvent" );
        orgUnit.setUploadSingleEventWithoutRegistration( "uploadSingleEventWithoutRegistration" );
        orgUnit.setCompleteProgramInstanceUrl( "completeProgramInstance" );
        orgUnit.setRegisterRelativeUrl( "registerRelative" );

        return orgUnit;
    }
}
