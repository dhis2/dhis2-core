package org.hisp.dhis.api.mobile.model;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "orgUnit" )
public class MobileOrgUnitLinks
    implements DataStreamSerializable
{
    public static final double currentVersion = 2.11;

    private String clientVersion;

    private int id;

    private String name;

    private String downloadAllUrl;

    private String updateActivityPlanUrl;

    private String uploadFacilityReportUrl;

    private String downloadFacilityReportUrl;

    private String uploadActivityReportUrl;

    private String updateDataSetUrl;

    private String changeUpdateDataSetLangUrl;

    private String searchUrl;

    private String updateNewVersionUrl;

    private String sendFeedbackUrl;

    private String findUserUrl;

    private String sendMessageUrl;

    private String downloadMessageConversationUrl;

    private String getMessageUrl;

    private String replyMessageUrl;

    private String downloadInterpretationUrl;

    private String postInterpretationUrl;

    private String postCommentUrl;

    private String updateContactUrl;

    private String findPatientUrl;

    private String findPatientsUrl;

    private String uploadProgramStageUrl;

    private String enrollProgramUrl;

    private String registerPersonUrl;

    private String addRelationshipUrl;

    private String downloadAnonymousProgramUrl;

    private String findProgramUrl;

    private String getVariesInfoUrl;

    private String findPatientInAdvancedUrl;

    private String findVisitScheduleUrl;

    private String findLostToFollowUpUrl;

    private String handleLostToFollowUpUrl;

    private String generateRepeatableEventUrl;

    private String uploadSingleEventWithoutRegistration;

    private String completeProgramInstanceUrl;
    
    private String registerRelativeUrl;

    @XmlAttribute
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @XmlAttribute
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDownloadAllUrl()
    {
        return downloadAllUrl;
    }

    public void setDownloadAllUrl( String downloadAllUrl )
    {
        this.downloadAllUrl = downloadAllUrl;
    }

    public String getUploadFacilityReportUrl()
    {
        return uploadFacilityReportUrl;
    }

    public void setUploadFacilityReportUrl( String uploadFacilityReportUrl )
    {
        this.uploadFacilityReportUrl = uploadFacilityReportUrl;
    }

    public String getDownloadFacilityReportUrl()
    {
        return downloadFacilityReportUrl;
    }

    public void setDownloadFacilityReportUrl( String downloadFacilityReportUrl )
    {
        this.downloadFacilityReportUrl = downloadFacilityReportUrl;
    }

    public String getUploadActivityReportUrl()
    {
        return uploadActivityReportUrl;
    }

    public void setUploadActivityReportUrl( String uploadActivityReportUrl )
    {
        this.uploadActivityReportUrl = uploadActivityReportUrl;
    }

    public String getUpdateDataSetUrl()
    {
        return updateDataSetUrl;
    }

    public void setUpdateDataSetUrl( String updateDataSetUrl )
    {
        this.updateDataSetUrl = updateDataSetUrl;
    }

    public String getChangeUpdateDataSetLangUrl()
    {
        return changeUpdateDataSetLangUrl;
    }

    public void setChangeUpdateDataSetLangUrl( String changeUpdateDataSetLangUrl )
    {
        this.changeUpdateDataSetLangUrl = changeUpdateDataSetLangUrl;
    }

    public String getSearchUrl()
    {
        return searchUrl;
    }

    public void setSearchUrl( String searchUrl )
    {
        this.searchUrl = searchUrl;
    }

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    public String getUpdateActivityPlanUrl()
    {
        return updateActivityPlanUrl;
    }

    public void setUpdateActivityPlanUrl( String updateActivityPlanUrl )
    {
        this.updateActivityPlanUrl = updateActivityPlanUrl;
    }

    public String getUpdateNewVersionUrl()
    {
        return updateNewVersionUrl;
    }

    public void setUpdateNewVersionUrl( String updateNewVersionUrl )
    {
        this.updateNewVersionUrl = updateNewVersionUrl;
    }

    public String getSendFeedbackUrl()
    {
        return sendFeedbackUrl;
    }

    public void setSendFeedbackUrl( String sendFeedbackUrl )
    {
        this.sendFeedbackUrl = sendFeedbackUrl;
    }

    public String getFindUserUrl()
    {
        return findUserUrl;
    }

    public void setFindUserUrl( String findUserUrl )
    {
        this.findUserUrl = findUserUrl;
    }

    public String getSendMessageUrl()
    {
        return sendMessageUrl;
    }

    public void setSendMessageUrl( String sendMessageUrl )
    {
        this.sendMessageUrl = sendMessageUrl;
    }

    public String getDownloadMessageConversationUrl()
    {
        return downloadMessageConversationUrl;
    }

    public void setDownloadMessageConversationUrl( String downloadMessageConversationUrl )
    {
        this.downloadMessageConversationUrl = downloadMessageConversationUrl;
    }

    public String getGetMessageUrl()
    {
        return getMessageUrl;
    }

    public void setGetMessageUrl( String getMessageUrl )
    {
        this.getMessageUrl = getMessageUrl;
    }

    public String getReplyMessageUrl()
    {
        return replyMessageUrl;
    }

    public void setReplyMessageUrl( String replyMessageUrl )
    {
        this.replyMessageUrl = replyMessageUrl;
    }

    public String getDownloadInterpretationUrl()
    {
        return downloadInterpretationUrl;
    }

    public void setDownloadInterpretationUrl( String downloadInterpretationUrl )
    {
        this.downloadInterpretationUrl = downloadInterpretationUrl;
    }

    public String getPostInterpretationUrl()
    {
        return postInterpretationUrl;
    }

    public void setPostInterpretationUrl( String postInterpretationUrl )
    {
        this.postInterpretationUrl = postInterpretationUrl;
    }

  

    public String getPostCommentUrl()
    {
        return postCommentUrl;
    }

    public void setPostCommentUrl( String postCommentUrl )
    {
        this.postCommentUrl = postCommentUrl;
    }

    public String getUpdateContactUrl()
    {
        return updateContactUrl;
    }

    public void setUpdateContactUrl( String updateContactUrl )
    {
        this.updateContactUrl = updateContactUrl;
    }

    public String getFindPatientUrl()
    {
        return findPatientUrl;
    }

    public void setFindPatientUrl( String findPatientUrl )
    {
        this.findPatientUrl = findPatientUrl;
    }

    public String getFindPatientsUrl()
    {
        return findPatientsUrl;
    }

    public void setFindPatientsUrl( String findPatientsUrl )
    {
        this.findPatientsUrl = findPatientsUrl;
    }

    public String getUploadProgramStageUrl()
    {
        return uploadProgramStageUrl;
    }

    public void setUploadProgramStageUrl( String uploadProgramStageUrl )
    {
        this.uploadProgramStageUrl = uploadProgramStageUrl;
    }

    public String getEnrollProgramUrl()
    {
        return enrollProgramUrl;
    }

    public void setEnrollProgramUrl( String enrollProgramUrl )
    {
        this.enrollProgramUrl = enrollProgramUrl;
    }

    public String getRegisterPersonUrl()
    {
        return registerPersonUrl;
    }

    public void setRegisterPersonUrl( String registerPersonUrl )
    {
        this.registerPersonUrl = registerPersonUrl;
    }

    public String getGetVariesInfoUrl()
    {
        return getVariesInfoUrl;
    }

    public void setGetVariesInfoUrl( String getVariesInfoUrl )
    {
        this.getVariesInfoUrl = getVariesInfoUrl;
    }

    public String getAddRelationshipUrl()
    {
        return addRelationshipUrl;
    }

    public void setAddRelationshipUrl( String addRelationshipUrl )
    {
        this.addRelationshipUrl = addRelationshipUrl;
    }

    public String getDownloadAnonymousProgramUrl()
    {
        return downloadAnonymousProgramUrl;
    }

    public void setDownloadAnonymousProgramUrl( String downloadAnonymousProgramUrl )
    {
        this.downloadAnonymousProgramUrl = downloadAnonymousProgramUrl;
    }

    public String getFindProgramUrl()
    {
        return findProgramUrl;
    }

    public void setFindProgramUrl( String findProgramUrl )
    {
        this.findProgramUrl = findProgramUrl;
    }

    public String getFindPatientInAdvancedUrl()
    {
        return findPatientInAdvancedUrl;
    }

    public void setFindPatientInAdvancedUrl( String findPatientInAdvancedUrl )
    {
        this.findPatientInAdvancedUrl = findPatientInAdvancedUrl;
    }

    public String getFindVisitScheduleUrl()
    {
        return findVisitScheduleUrl;
    }

    public void setFindVisitScheduleUrl( String findVisitScheduleUrl )
    {
        this.findVisitScheduleUrl = findVisitScheduleUrl;
    }

    public String getFindLostToFollowUpUrl()
    {
        return findLostToFollowUpUrl;
    }

    public void setFindLostToFollowUpUrl( String findLostToFollowUpUrl )
    {
        this.findLostToFollowUpUrl = findLostToFollowUpUrl;
    }

    public String getHandleLostToFollowUpUrl()
    {
        return handleLostToFollowUpUrl;
    }

    public void setHandleLostToFollowUpUrl( String handleLostToFollowUpUrl )
    {
        this.handleLostToFollowUpUrl = handleLostToFollowUpUrl;
    }

    public String getGenerateRepeatableEventUrl()
    {
        return generateRepeatableEventUrl;
    }

    public void setGenerateRepeatableEventUrl( String generateRepeatableEventUrl )
    {
        this.generateRepeatableEventUrl = generateRepeatableEventUrl;
    }

    public String getUploadSingleEventWithoutRegistration()
    {
        return uploadSingleEventWithoutRegistration;
    }

    public void setUploadSingleEventWithoutRegistration( String uploadSingleEventWithoutRegistration )
    {
        this.uploadSingleEventWithoutRegistration = uploadSingleEventWithoutRegistration;
    }

    public String getCompleteProgramInstanceUrl()
    {
        return completeProgramInstanceUrl;
    }

    public void setCompleteProgramInstanceUrl( String completeProgramInstanceUrl )
    {
        this.completeProgramInstanceUrl = completeProgramInstanceUrl;
    }

    public String getRegisterRelativeUrl()
    {
        return registerRelativeUrl;
    }

    public void setRegisterRelativeUrl( String registerRelativeUrl )
    {
        this.registerRelativeUrl = registerRelativeUrl;
    }

    @Override
    public void serialize( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( id );
        dataOutputStream.writeUTF( name );
        dataOutputStream.writeUTF( downloadAllUrl );
        dataOutputStream.writeUTF( updateActivityPlanUrl );
        dataOutputStream.writeUTF( uploadFacilityReportUrl );
        dataOutputStream.writeUTF( downloadFacilityReportUrl );
        dataOutputStream.writeUTF( uploadActivityReportUrl );
        dataOutputStream.writeUTF( updateDataSetUrl );
        dataOutputStream.writeUTF( changeUpdateDataSetLangUrl );
        dataOutputStream.writeUTF( searchUrl );
        dataOutputStream.writeUTF( updateNewVersionUrl );
        dataOutputStream.writeUTF( sendFeedbackUrl );
        dataOutputStream.writeUTF( findUserUrl );
        dataOutputStream.writeUTF( sendMessageUrl );
        dataOutputStream.writeUTF( downloadMessageConversationUrl );
        dataOutputStream.writeUTF( getMessageUrl );
        dataOutputStream.writeUTF( replyMessageUrl );
        dataOutputStream.writeUTF( downloadInterpretationUrl );
        dataOutputStream.writeUTF( postInterpretationUrl );
        dataOutputStream.writeUTF( postCommentUrl );
        dataOutputStream.writeUTF( updateContactUrl );
        dataOutputStream.writeUTF( findPatientUrl );
        dataOutputStream.writeUTF( registerPersonUrl );
        dataOutputStream.writeUTF( uploadProgramStageUrl );
        dataOutputStream.writeUTF( enrollProgramUrl );
        dataOutputStream.writeUTF( getVariesInfoUrl );
        dataOutputStream.writeUTF( addRelationshipUrl );
        dataOutputStream.writeUTF( downloadAnonymousProgramUrl );
        dataOutputStream.writeUTF( findProgramUrl );
        dataOutputStream.writeUTF( findPatientInAdvancedUrl );
        dataOutputStream.writeUTF( findPatientsUrl );
        dataOutputStream.writeUTF( findVisitScheduleUrl );
        dataOutputStream.writeUTF( findLostToFollowUpUrl );
        dataOutputStream.writeUTF( handleLostToFollowUpUrl );
        dataOutputStream.writeUTF( generateRepeatableEventUrl );
        dataOutputStream.writeUTF( uploadSingleEventWithoutRegistration );
        dataOutputStream.writeUTF( completeProgramInstanceUrl );
        dataOutputStream.writeUTF( registerRelativeUrl );
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        id = dataInputStream.readInt();
        name = dataInputStream.readUTF();
        downloadAllUrl = dataInputStream.readUTF();
        updateActivityPlanUrl = dataInputStream.readUTF();
        uploadFacilityReportUrl = dataInputStream.readUTF();
        downloadFacilityReportUrl = dataInputStream.readUTF();
        uploadActivityReportUrl = dataInputStream.readUTF();
        updateDataSetUrl = dataInputStream.readUTF();
        changeUpdateDataSetLangUrl = dataInputStream.readUTF();
        searchUrl = dataInputStream.readUTF();
        updateNewVersionUrl = dataInputStream.readUTF();
        sendFeedbackUrl = dataInputStream.readUTF();
        findUserUrl = dataInputStream.readUTF();
        sendMessageUrl = dataInputStream.readUTF();
        downloadMessageConversationUrl = dataInputStream.readUTF();
        getMessageUrl = dataInputStream.readUTF();
        replyMessageUrl = dataInputStream.readUTF();
        downloadInterpretationUrl = dataInputStream.readUTF();
        postInterpretationUrl = dataInputStream.readUTF();
        postCommentUrl = dataInputStream.readUTF();
    }

    @Override
    public void serializeVersion2_8( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( this.id );
        dataOutputStream.writeUTF( this.name );
        dataOutputStream.writeUTF( this.downloadAllUrl );
        dataOutputStream.writeUTF( this.updateActivityPlanUrl );
        dataOutputStream.writeUTF( this.uploadFacilityReportUrl );
        dataOutputStream.writeUTF( this.downloadFacilityReportUrl );
        dataOutputStream.writeUTF( this.uploadActivityReportUrl );
        dataOutputStream.writeUTF( this.updateDataSetUrl );
        dataOutputStream.writeUTF( this.changeUpdateDataSetLangUrl );
        dataOutputStream.writeUTF( this.searchUrl );
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( this.id );
        dataOutputStream.writeUTF( this.name );
        dataOutputStream.writeUTF( this.downloadAllUrl );
        dataOutputStream.writeUTF( this.updateActivityPlanUrl );
        dataOutputStream.writeUTF( this.uploadFacilityReportUrl );
        dataOutputStream.writeUTF( this.uploadActivityReportUrl );
        dataOutputStream.writeUTF( this.updateDataSetUrl );
        dataOutputStream.writeUTF( this.changeUpdateDataSetLangUrl );
        dataOutputStream.writeUTF( this.searchUrl );
        dataOutputStream.writeUTF( this.updateNewVersionUrl );
    }

    @Override
    public void serializeVersion2_10( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( id );
        dataOutputStream.writeUTF( name );
        dataOutputStream.writeUTF( downloadAllUrl );
        dataOutputStream.writeUTF( updateActivityPlanUrl );
        dataOutputStream.writeUTF( uploadFacilityReportUrl );
        dataOutputStream.writeUTF( downloadFacilityReportUrl );
        dataOutputStream.writeUTF( uploadActivityReportUrl );
        dataOutputStream.writeUTF( updateDataSetUrl );
        dataOutputStream.writeUTF( changeUpdateDataSetLangUrl );
        dataOutputStream.writeUTF( searchUrl );
        dataOutputStream.writeUTF( updateNewVersionUrl );
        dataOutputStream.writeUTF( sendFeedbackUrl );
        dataOutputStream.writeUTF( findUserUrl );
        dataOutputStream.writeUTF( sendMessageUrl );
        dataOutputStream.writeUTF( downloadMessageConversationUrl );
        dataOutputStream.writeUTF( getMessageUrl );
        dataOutputStream.writeUTF( replyMessageUrl );
        dataOutputStream.writeUTF( downloadInterpretationUrl );
        dataOutputStream.writeUTF( postInterpretationUrl );
        dataOutputStream.writeUTF( postCommentUrl );
    }
}
