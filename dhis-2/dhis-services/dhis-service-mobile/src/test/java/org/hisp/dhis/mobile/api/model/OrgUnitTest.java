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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.hisp.dhis.api.mobile.model.MobileOrgUnitLinks;
import org.junit.Test;

public class OrgUnitTest
{
    @Test
    public void testSerialization()
        throws IOException
    {
        MobileOrgUnitLinks unit = new MobileOrgUnitLinks();

        unit.setId( 1 );
        unit.setName( "name" );
        unit.setDownloadAllUrl( "downloadAllUrl" );
        unit.setDownloadFacilityReportUrl( "downloadFacilityReportUrl" );
        unit.setUpdateActivityPlanUrl( "updateActivityPlanUrl" );
        unit.setUploadFacilityReportUrl( "uploadFacilityReportUrl" );
        unit.setUploadActivityReportUrl( "uploadActivityReportUrl" );
        unit.setUpdateDataSetUrl( "updateDataSetUrl" );
        unit.setChangeUpdateDataSetLangUrl( "changeUpdateDataSetLangUrl" );
        unit.setSearchUrl( "search" );
        unit.setUpdateNewVersionUrl( "" );
        unit.setSendFeedbackUrl( "sendFeedbackUrl" );
        unit.setFindUserUrl( "findUserUrl" );
        unit.setSendMessageUrl( "sendMessageUrl" );
        unit.setDownloadMessageConversationUrl( "downloadMessageConversationUrl" );
        unit.setGetMessageUrl( "getMessageUrl" );
        unit.setReplyMessageUrl( "replyMessageUrl" );
        unit.setDownloadInterpretationUrl( "downloadInterpretationUrl" );
        unit.setPostInterpretationUrl( "postInterpretationUrl" );
        unit.setPostCommentUrl( "postCommentUrl" );
        unit.setUpdateContactUrl( "updateContactUrl" );
        unit.setFindPatientUrl( "findPatientUrl" );
        unit.setUploadProgramStageUrl( "uploadProgramStageUrl" );
        unit.setEnrollProgramUrl( "enrollProgramUrl" );
        unit.setRegisterPersonUrl( "registerPersonUrl" );
        unit.setGetVariesInfoUrl( "getVariesInfoUrl" );
        unit.setAddRelationshipUrl( "addRelationshipUrl" );
        unit.setDownloadAnonymousProgramUrl( "downloadAnonymousProgramUrl" );
        unit.setFindProgramUrl( "findProgramUrl" );
        unit.setFindPatientInAdvancedUrl( "findPatientInAdvanced" );
        unit.setFindPatientsUrl( "findPatientsUrl" );
        unit.setFindVisitScheduleUrl( "findVisitSchedule" );
        unit.setFindLostToFollowUpUrl( "findLostToFollowUpUrl" );
        unit.setHandleLostToFollowUpUrl( "handleLostToFollowUpUrl" );
        unit.setGenerateRepeatableEventUrl( "generateRepeatableEventUrl" );
        unit.setUploadSingleEventWithoutRegistration( "uploadSingleEventWithoutRegistration" );
        unit.setCompleteProgramInstanceUrl( "completeProgramInstance" );
        unit.setRegisterRelativeUrl( "registerRelativeUrl" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream( baos );
        unit.serialize( dos );
        dos.flush();
        MobileOrgUnitLinks unit2 = new MobileOrgUnitLinks();
        unit2.deSerialize( new DataInputStream( new ByteArrayInputStream( baos.toByteArray() ) ) );

        assertEquals( unit.getName(), unit2.getName() );
        assertEquals( unit.getId(), unit2.getId() );
    }
}
