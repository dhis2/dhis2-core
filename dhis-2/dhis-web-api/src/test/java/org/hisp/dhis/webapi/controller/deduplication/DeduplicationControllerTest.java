/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.deduplication;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateQuery;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.DeduplicationController;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import com.google.common.collect.Lists;

@RunWith( MockitoJUnitRunner.class )
public class DeduplicationControllerTest
{
    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Mock
    private TrackerAccessManager trackerAccessManager;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private FieldFilterService fieldFilterService;

    @Mock
    private ContextService contextService;

    @Mock
    private User user;

    @Mock
    private TrackedEntityInstance trackedEntityInstanceA;

    @Mock
    private TrackedEntityInstance trackedEntityInstanceB;

    @InjectMocks
    private DeduplicationController deduplicationController;

    private static final String teiA = "trackedentA";

    private static final String teiB = "trackedentB";

    @Before
    public void setUpTest()
    {
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        lenient().when( trackedEntityInstanceA.getUid() ).thenReturn( teiA );
        lenient().when( trackedEntityInstanceB.getUid() ).thenReturn( teiB );

        lenient().when( trackedEntityInstanceService.getTrackedEntityInstance( teiA ) )
            .thenReturn( trackedEntityInstanceA );
        lenient().when( trackedEntityInstanceService.getTrackedEntityInstance( teiB ) )
            .thenReturn( trackedEntityInstanceB );

        lenient().when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceA ) ) ).thenReturn(
            Lists.newArrayList() );
        lenient().when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceB ) ) ).thenReturn(
            Lists.newArrayList() );
    }

    @Test
    public void getAllPotentialDuplicate()
    {
        PotentialDuplicateQuery potentialDuplicateQuery = new PotentialDuplicateQuery();

        deduplicationController.getAll( potentialDuplicateQuery, mock( HttpServletResponse.class ) );

        verify( deduplicationService ).getAllPotentialDuplicatesBy( potentialDuplicateQuery );
    }

    @Test
    public void getPotentialDuplicateNotFound()
    {
        when( deduplicationService.getPotentialDuplicateByUid( teiA ) ).thenReturn( null );

        try
        {
            deduplicationController.getPotentialDuplicate( teiA );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.NOT_FOUND.value() );
        }

        verify( deduplicationService ).getPotentialDuplicateByUid( teiA );
    }

    @Test
    public void getPotentialDuplicate()
        throws WebMessageException
    {
        when( deduplicationService.getPotentialDuplicateByUid( teiA ) )
            .thenReturn( new PotentialDuplicate( teiA, teiB ) );

        PotentialDuplicate pd = deduplicationController.getPotentialDuplicate( teiA );

        assertEquals( teiA, pd.getTeiA() );
        verify( deduplicationService ).getPotentialDuplicateByUid( teiA );
    }

    @Test
    public void postPotentialDuplicate()
        throws WebMessageException
    {
        Mockito.when( deduplicationService.exists( new PotentialDuplicate( teiA, teiB ) ) ).thenReturn( false );

        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );

        deduplicationController.postPotentialDuplicate( potentialDuplicate );

        verify( deduplicationService ).addPotentialDuplicate( potentialDuplicate );
        verify( trackedEntityInstanceService, times( 2 ) ).getTrackedEntityInstance( anyString() );
        verify( trackedEntityInstanceService ).getTrackedEntityInstance( teiA );
        verify( trackedEntityInstanceService ).getTrackedEntityInstance( teiB );
        verify( trackerAccessManager, times( 2 ) ).canRead( eq( user ), any( TrackedEntityInstance.class ) );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceB );
        verify( deduplicationService ).exists( potentialDuplicate );
    }

    @Test
    public void postPotentialDuplicateInvalidUid()
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( "invalid", "invalid1" ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.CONFLICT.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    public void postPotentialDuplicateInvalidUidTeiB()
    {
        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceA ) ) ).thenReturn(
            Lists.newArrayList() );

        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, "invalid" ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.CONFLICT.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    public void postPotentialDuplicateMissingRequiredTeis()
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( null, null ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.CONFLICT.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    public void postPotentialDuplicateOnlyOneTei()
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, null ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.CONFLICT.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    public void postPotentialDuplicateTeiNotFound()
    {
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiA ) )
            .thenReturn( null );

        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, teiB ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.NOT_FOUND.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    public void postPotentialDuplicateNoAccessToTeiA()
    {
        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceA ) ) ).thenReturn(
            Lists.newArrayList( "Error" ) );

        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, teiB ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.FORBIDDEN.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
    }

    @Test
    public void postPotentialDuplicateNoAccessToTeiB()
    {

        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceA ) ) ).thenReturn(
            Lists.newArrayList() );
        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceB ) ) ).thenReturn(
            Lists.newArrayList( "Error" ) );

        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, teiB ) );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.FORBIDDEN.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceB );
    }

    @Test
    public void postPotentialDuplicateAlreadyExists()
    {
        PotentialDuplicate pd = new PotentialDuplicate( teiA, teiB );

        when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceA ) ) ).thenReturn(
            Lists.newArrayList() );
        when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceB ) ) ).thenReturn(
            Lists.newArrayList() );

        when( deduplicationService.exists( pd ) ).thenReturn( true );

        try
        {
            deduplicationController.postPotentialDuplicate( pd );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.CONFLICT.value() );
        }

        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceB );
        verify( deduplicationService ).exists( pd );
    }

    @Test
    public void markPotentialDuplicateInvalid()
        throws WebMessageException
    {

        when( deduplicationService.getPotentialDuplicateByUid( teiA ) )
            .thenReturn( new PotentialDuplicate( teiA, teiB ) );

        deduplicationController.markPotentialDuplicateInvalid( teiA );

        ArgumentCaptor<PotentialDuplicate> pd = ArgumentCaptor.forClass( PotentialDuplicate.class );

        verify( deduplicationService ).updatePotentialDuplicate( pd.capture() );

        verify( deduplicationService ).getPotentialDuplicateByUid( teiA );
        verify( deduplicationService ).updatePotentialDuplicate( pd.getValue() );
        assertEquals( DeduplicationStatus.INVALID, pd.getValue().getStatus() );
    }

    @Test
    public void markPotentialDuplicateInvalidNotFound()
    {
        when( deduplicationService.getPotentialDuplicateByUid( teiA ) ).thenReturn( null );

        try
        {
            deduplicationController.markPotentialDuplicateInvalid( teiA );
        }
        catch ( WebMessageException e )
        {
            checkWebMessageException( e.getWebMessage(), HttpStatus.NOT_FOUND.value() );
        }

        verify( deduplicationService, times( 0 ) ).updatePotentialDuplicate( any() );
    }

    private void checkWebMessageException( WebMessage wm, int statusCode )
    {
        assertEquals( statusCode, wm.getHttpStatusCode().intValue() );
        assertEquals( Status.ERROR, wm.getStatus() );
    }
}