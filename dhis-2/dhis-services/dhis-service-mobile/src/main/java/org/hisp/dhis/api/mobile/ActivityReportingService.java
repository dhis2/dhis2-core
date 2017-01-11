package org.hisp.dhis.api.mobile;

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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.api.mobile.model.ActivityPlan;
import org.hisp.dhis.api.mobile.model.ActivityValue;
import org.hisp.dhis.api.mobile.model.Interpretation;
import org.hisp.dhis.api.mobile.model.Message;
import org.hisp.dhis.api.mobile.model.MessageConversation;
import org.hisp.dhis.api.mobile.model.PatientAttribute;
import org.hisp.dhis.api.mobile.model.User;
import org.hisp.dhis.api.mobile.model.LWUITmodel.LostEvent;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Notification;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Patient;
import org.hisp.dhis.api.mobile.model.LWUITmodel.PatientList;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Program;
import org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship;
import org.hisp.dhis.organisationunit.OrganisationUnit;

public interface ActivityReportingService
{
    ActivityPlan getCurrentActivityPlan( OrganisationUnit unit, String localeString );

    ActivityPlan getAllActivityPlan( OrganisationUnit unit, String localeString );

    void saveActivityReport( OrganisationUnit unit, ActivityValue activityValue, Integer programStageSectionId )
        throws NotAllowedException;

    Patient findPatient( String patientId )
        throws NotAllowedException;

    PatientList findPatients( String patientIds )
        throws NotAllowedException;

    String findPatientInAdvanced( String keyword, int orgUnitId, int programId )
        throws NotAllowedException;

    String saveProgramStage( ProgramStage programStage, int patientId, int orgUnitId )
        throws NotAllowedException;

    Patient enrollProgram( String enrollInfo,
        List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage> mobileProgramStageList, Date incidentDate )
        throws NotAllowedException;

    public String completeProgramInstance( int programId )
        throws NotAllowedException;

    List<org.hisp.dhis.trackedentity.TrackedEntityAttribute> getPatientAtts( String programId );

    List<PatientAttribute> getAttsForMobile();

    List<PatientAttribute> getPatientAttributesForMobile( String programId );

    Patient addRelationship( Relationship enrollmentRelationship, int orgUnitId )
        throws NotAllowedException;

    Program getAllProgramByOrgUnit( int orgUnitId, String programType )
        throws NotAllowedException;

    Program findProgram( String programInfo )
        throws NotAllowedException;

    Patient savePatient( Patient patient, int orgUnitId, String programId )
        throws NotAllowedException;

    Patient updatePatient( Patient patient, int orgUnitId, String programId )
        throws NotAllowedException;

    String findLostToFollowUp( int orgUnitId, String programId )
        throws NotAllowedException;

    Notification handleLostToFollowUp( LostEvent lostEvent )
        throws NotAllowedException;

    Patient generateRepeatableEvent( int orgUnitId, String eventInfo )
        throws NotAllowedException;

    String saveSingleEventWithoutRegistration( ProgramStage programStage, int orgUnitId )
        throws NotAllowedException;

    String sendFeedback( Message message )
        throws NotAllowedException;

    Collection<User> findUser( String keyword )
        throws NotAllowedException;

    String findVisitSchedule( int orgUnitId, int programId, String info )
        throws NotAllowedException;

    String sendMessage( Message message )
        throws NotAllowedException;

    Collection<MessageConversation> downloadMessageConversation()
        throws NotAllowedException;

    Collection<Message> getMessage( String conversationId )
        throws NotAllowedException;

    String replyMessage( Message message )
        throws NotAllowedException;

    Interpretation getInterpretation( String uId )
        throws NotAllowedException;

    String postInterpretation( String data )
        throws NotAllowedException;

    String postInterpretationComment( String data )
        throws NotAllowedException;
    
    Patient registerRelative( Patient patient, int orgUnitId, String programId )
        throws NotAllowedException;
}
