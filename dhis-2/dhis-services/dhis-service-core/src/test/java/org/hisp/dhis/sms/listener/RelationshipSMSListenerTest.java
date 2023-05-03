/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.models.RelationshipSmsSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class RelationshipSMSListenerTest
    extends
    CompressionSMSListenerTest
{

    @Mock
    private UserService userService;

    @Mock
    private IncomingSmsService incomingSmsService;

    @Mock
    private MessageSender smsSender;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    private ProgramService programService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EventService eventService;

    @Mock
    private IdentifiableObjectManager identifiableObjectManager;

    private User user;

    private OutboundMessageResponse response = new OutboundMessageResponse();

    private IncomingSms updatedIncomingSms;

    private String message = "";

    // Needed for this test

    RelationshipSMSListener subject;

    private IncomingSms incomingSmsRelationship;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    private Enrollment enrollment;

    private RelationshipType relationshipType;

    @BeforeEach
    public void initTest()
        throws SmsCompressionException
    {
        subject = new RelationshipSMSListener( incomingSmsService, smsSender, userService, trackedEntityTypeService,
            trackedEntityAttributeService, programService, organisationUnitService, categoryService, dataElementService,
            eventService, relationshipService, relationshipTypeService, trackedEntityInstanceService,
            programInstanceService, identifiableObjectManager );

        setUpInstances();

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( smsSender.isConfigured() ).thenReturn( true );
        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = (String) invocation.getArguments()[1];
            return response;
        } );

        when( relationshipTypeService.getRelationshipType( anyString() ) ).thenReturn( relationshipType );
        when( programInstanceService.getProgramInstance( anyString() ) ).thenReturn( enrollment );

        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    void testRelationship()
    {
        subject.receive( incomingSmsRelationship );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    private void setUpInstances()
        throws SmsCompressionException
    {
        user = makeUser( "U" );
        user.setPhoneNumber( ORIGINATOR );

        enrollment = new Enrollment();
        enrollment.setAutoFields();

        relationshipType = new RelationshipType();
        relationshipType.setAutoFields();
        RelationshipConstraint relConstraint = new RelationshipConstraint();
        relConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        relationshipType.setToConstraint( relConstraint );
        relationshipType.setFromConstraint( relConstraint );

        incomingSmsRelationship = createSMSFromSubmission( createRelationshipSubmission() );
    }

    private RelationshipSmsSubmission createRelationshipSubmission()
    {
        RelationshipSmsSubmission subm = new RelationshipSmsSubmission();

        subm.setUserId( user.getUid() );
        subm.setRelationshipType( relationshipType.getUid() );
        subm.setRelationship( "uf3svrmpzOj" );
        subm.setFrom( enrollment.getUid() );
        subm.setTo( enrollment.getUid() );
        subm.setSubmissionId( 1 );

        return subm;
    }
}
