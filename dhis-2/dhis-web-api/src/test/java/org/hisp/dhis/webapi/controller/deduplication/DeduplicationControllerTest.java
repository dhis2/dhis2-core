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
package org.hisp.dhis.webapi.controller.deduplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.deduplication.PotentialDuplicateCriteria;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.ConflictException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.exception.OperationNotAllowedException;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class DeduplicationControllerTest
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

    private static final String uid = "uid";

    @BeforeEach
    void setUpTest()
    {
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        when( trackedEntityInstanceA.getUid() ).thenReturn( teiA );
        when( trackedEntityInstanceB.getUid() ).thenReturn( teiB );
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiA ) )
            .thenReturn( trackedEntityInstanceA );
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiB ) )
            .thenReturn( trackedEntityInstanceB );
        when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceA ) ) )
            .thenReturn( Lists.newArrayList() );
        when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceB ) ) )
            .thenReturn( Lists.newArrayList() );
    }

    @Test
    void getAllPotentialDuplicate()
        throws BadRequestException
    {
        PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
        deduplicationController.getPotentialDuplicates( criteria, mock( HttpServletResponse.class ), null );
        verify( deduplicationService ).getPotentialDuplicates( criteria );
    }

    @Test
    void getPotentialDuplicateNotFound()
    {
        when( deduplicationService.getPotentialDuplicateByUid( uid ) ).thenReturn( null );
        assertThrows( NotFoundException.class, () -> deduplicationController.getPotentialDuplicateById( uid ) );
    }

    @Test
    void getPotentialDuplicateByUid()
        throws NotFoundException
    {
        when( deduplicationService.getPotentialDuplicateByUid( uid ) )
            .thenReturn( new PotentialDuplicate( teiA, teiB ) );
        PotentialDuplicate pd = deduplicationController.getPotentialDuplicateById( uid );
        assertEquals( teiA, pd.getOriginal() );
        verify( deduplicationService ).getPotentialDuplicateByUid( uid );
    }

    @Test
    void postPotentialDuplicate()
        throws OperationNotAllowedException,
        ConflictException,
        NotFoundException,
        BadRequestException,
        PotentialDuplicateConflictException
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
    void postPotentialDuplicateInvalidUid()
        throws PotentialDuplicateConflictException
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( "invalid", "invalid1" ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof BadRequestException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    void postPotentialDuplicateInvalidUidTeiB()
        throws PotentialDuplicateConflictException
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, "invalid" ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof BadRequestException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    void postPotentialDuplicateMissingRequiredTeis()
        throws PotentialDuplicateConflictException
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( null, null ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof BadRequestException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    void postPotentialDuplicateOnlyOneTei()
        throws PotentialDuplicateConflictException
    {
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, null ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof BadRequestException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    void postPotentialDuplicateTeiNotFound()
        throws PotentialDuplicateConflictException
    {
        when( trackedEntityInstanceService.getTrackedEntityInstance( teiA ) ).thenReturn( null );
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, teiB ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof NotFoundException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
    }

    @Test
    void postPotentialDuplicateNoAccessToTeiA()
        throws PotentialDuplicateConflictException
    {
        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceA ) ) )
            .thenReturn( Lists.newArrayList( "Error" ) );
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, teiB ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof OperationNotAllowedException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
    }

    @Test
    void postPotentialDuplicateNoAccessToTeiB()
        throws PotentialDuplicateConflictException
    {
        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceA ) ) )
            .thenReturn( Lists.newArrayList() );
        when( trackerAccessManager.canRead( Mockito.any(), eq( trackedEntityInstanceB ) ) )
            .thenReturn( Lists.newArrayList( "Error" ) );
        try
        {
            deduplicationController.postPotentialDuplicate( new PotentialDuplicate( teiA, teiB ) );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof OperationNotAllowedException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceB );
    }

    @Test
    void postPotentialDuplicateAlreadyExists()
        throws PotentialDuplicateConflictException
    {
        PotentialDuplicate pd = new PotentialDuplicate( teiA, teiB );
        when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceA ) ) ).thenReturn( Lists.newArrayList() );
        when( trackerAccessManager.canRead( any(), eq( trackedEntityInstanceB ) ) ).thenReturn( Lists.newArrayList() );
        when( deduplicationService.exists( pd ) ).thenReturn( true );
        try
        {
            deduplicationController.postPotentialDuplicate( pd );
        }
        catch ( OperationNotAllowedException | ConflictException | NotFoundException | BadRequestException
            | PotentialDuplicateConflictException e )
        {
            assertTrue( e instanceof ConflictException );
        }
        verify( deduplicationService, times( 0 ) ).addPotentialDuplicate( any() );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceA );
        verify( trackerAccessManager ).canRead( user, trackedEntityInstanceB );
        verify( deduplicationService ).exists( pd );
    }

    @Test
    void updatePotentialDuplicateInvalidNotFound()
    {
        when( deduplicationService.getPotentialDuplicateByUid( uid ) ).thenReturn( null );
        assertThrows( NotFoundException.class,
            () -> deduplicationController.updatePotentialDuplicate( uid, DeduplicationStatus.INVALID ) );
    }

    @Test
    void shouldUpdatePotentialDuplicate()
        throws NotFoundException,
        BadRequestException
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        when( deduplicationService.getPotentialDuplicateByUid( uid ) ).thenReturn( potentialDuplicate );
        deduplicationController.updatePotentialDuplicate( uid, DeduplicationStatus.INVALID );
        ArgumentCaptor<PotentialDuplicate> potentialDuplicateArgumentCaptor = ArgumentCaptor
            .forClass( PotentialDuplicate.class );
        verify( deduplicationService ).updatePotentialDuplicate( potentialDuplicateArgumentCaptor.capture() );
        assertEquals( DeduplicationStatus.INVALID, potentialDuplicateArgumentCaptor.getValue().getStatus() );
    }

    @Test
    void shouldThrowUpdatePotentialDuplicateMergedStatusDb()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        potentialDuplicate.setStatus( DeduplicationStatus.MERGED );
        when( deduplicationService.getPotentialDuplicateByUid( uid ) ).thenReturn( potentialDuplicate );
        assertThrows( BadRequestException.class,
            () -> deduplicationController.updatePotentialDuplicate( uid, DeduplicationStatus.INVALID ) );
    }

    @Test
    void shouldThrowUpdatePotentialDuplicateMergeRequest()
    {
        PotentialDuplicate potentialDuplicate = new PotentialDuplicate( teiA, teiB );
        when( deduplicationService.getPotentialDuplicateByUid( uid ) ).thenReturn( potentialDuplicate );
        assertThrows( BadRequestException.class,
            () -> deduplicationController.updatePotentialDuplicate( uid, DeduplicationStatus.MERGED ) );
    }
}
