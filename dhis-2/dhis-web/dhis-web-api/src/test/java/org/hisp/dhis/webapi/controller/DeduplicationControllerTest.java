package org.hisp.dhis.webapi.controller;

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

import static org.junit.Assert.assertEquals;

import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

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

    @InjectMocks
    private DeduplicationController controller;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUpTest()
    {
    }

    @Test
    public void getPotentialDuplicateNotFound()
    {
        Mockito.when( deduplicationService.getPotentialDuplicateByUid( Mockito.eq( "0" ) ) ).thenReturn( null );

        try
        {
            controller.getPotentialDuplicate( "0" );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Not Found", wm.getHttpStatus() );
            assertEquals( 404, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "No potentialDuplicate records found with id '0'.", wm.getMessage() );
        }

    }

    @Test
    public void getPotentialDuplicate()
        throws WebMessageException
    {
        Mockito.when( deduplicationService.getPotentialDuplicateByUid( Mockito.eq( "1" ) ) )
            .thenReturn( new PotentialDuplicate( "teiA" ) );

        PotentialDuplicate pd = controller.getPotentialDuplicate( "1" );

        assertEquals( "teiA", pd.getTeiA() );
    }

    @Test
    public void postPotentialDuplicate()
        throws WebMessageException
    {
        PotentialDuplicate pd = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );

        TrackedEntityInstance teiA = new TrackedEntityInstance();
        TrackedEntityInstance teiB = new TrackedEntityInstance();

        teiA.setUid( "ABCDEFGHIJ1" );
        teiB.setUid( "ABCDEFGHIJ2" );

        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ1" ) ) )
            .thenReturn( teiA );
        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ2" ) ) )
            .thenReturn( teiB );

        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiA ) ) ).thenReturn(
            Lists.newArrayList() );
        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiB ) ) ).thenReturn(
            Lists.newArrayList() );

        Mockito.when( deduplicationService.exists( pd ) ).thenReturn( false );

        Mockito.when( deduplicationService.addPotentialDuplicate( Mockito.any( PotentialDuplicate.class ) ) )
            .thenReturn( 1L );

        controller.postPotentialDuplicate( new PotentialDuplicate( "ABCDEFGHIJ1" ) );
    }

    @Test
    public void postPotentialDuplicateMissingRequiredPropertyTeiA()
    {
        try
        {
            controller.postPotentialDuplicate( new PotentialDuplicate() );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Conflict", wm.getHttpStatus() );
            assertEquals( 409, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "Missing required property 'teiA'", wm.getMessage() );
        }
    }

    @Test
    public void postPotentialDuplicateInvalidUid()
    {
        try
        {
            controller.postPotentialDuplicate( new PotentialDuplicate( "invalid" ) );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Conflict", wm.getHttpStatus() );
            assertEquals( 409, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "'invalid' is not valid value for property 'teiA'", wm.getMessage() );
        }
    }

    @Test
    public void postPotentialDuplicateInvalidTei()
    {
        try
        {
            controller.postPotentialDuplicate( new PotentialDuplicate( "ABCDEFGHIJ0" ) );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Conflict", wm.getHttpStatus() );
            assertEquals( 409, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "No tracked entity instance found with id 'ABCDEFGHIJ0'.", wm.getMessage() );
        }
    }

    @Test
    public void postPotentialDuplicateNoAccessToTeiA()
    {
        TrackedEntityInstance teiA = new TrackedEntityInstance();

        teiA.setUid( "ABCDEFGHIJ1" );

        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ1" ) ) )
            .thenReturn( teiA );
        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiA ) ) ).thenReturn(
            Lists.newArrayList( "Error" ) );

        try
        {
            controller.postPotentialDuplicate( new PotentialDuplicate( "ABCDEFGHIJ1" ) );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Forbidden", wm.getHttpStatus() );
            assertEquals( 403, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "You don't have read access to 'ABCDEFGHIJ1'.", wm.getMessage() );
        }
    }

    @Test
    public void postPotentialDuplicateInvalidUidTeiB()
    {
        TrackedEntityInstance teiA = new TrackedEntityInstance();

        teiA.setUid( "ABCDEFGHIJ1" );

        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ1" ) ) )
            .thenReturn( teiA );
        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiA ) ) ).thenReturn(
            Lists.newArrayList() );

        try
        {
            controller.postPotentialDuplicate( new PotentialDuplicate( "ABCDEFGHIJ1", "invalid" ) );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Conflict", wm.getHttpStatus() );
            assertEquals( 409, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "'invalid' is not valid value for property 'teiB'", wm.getMessage() );
        }
    }

    @Test
    public void postPotentialDuplicateNoAccessToTeiB()
    {
        TrackedEntityInstance teiA = new TrackedEntityInstance();
        TrackedEntityInstance teiB = new TrackedEntityInstance();

        teiA.setUid( "ABCDEFGHIJ1" );
        teiB.setUid( "ABCDEFGHIJ2" );

        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ1" ) ) )
            .thenReturn( teiA );
        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ2" ) ) )
            .thenReturn( teiB );

        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiA ) ) ).thenReturn(
            Lists.newArrayList() );
        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiB ) ) ).thenReturn(
            Lists.newArrayList( "Error" ) );

        try
        {
            controller.postPotentialDuplicate( new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" ) );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Forbidden", wm.getHttpStatus() );
            assertEquals( 403, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "You don't have read access to 'ABCDEFGHIJ2'.", wm.getMessage() );
        }
    }

    @Test
    public void postPotentialDuplicateAlreadyExists()
    {
        PotentialDuplicate pd = new PotentialDuplicate( "ABCDEFGHIJ1", "ABCDEFGHIJ2" );

        TrackedEntityInstance teiA = new TrackedEntityInstance();
        TrackedEntityInstance teiB = new TrackedEntityInstance();

        teiA.setUid( "ABCDEFGHIJ1" );
        teiB.setUid( "ABCDEFGHIJ2" );

        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ1" ) ) )
            .thenReturn( teiA );
        Mockito.when( trackedEntityInstanceService.getTrackedEntityInstance( Mockito.eq( "ABCDEFGHIJ2" ) ) )
            .thenReturn( teiB );

        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiA ) ) ).thenReturn(
            Lists.newArrayList() );
        Mockito.when( trackerAccessManager.canRead( Mockito.any(), Mockito.eq( teiB ) ) ).thenReturn(
            Lists.newArrayList() );

        Mockito.when( deduplicationService.exists( pd ) ).thenReturn( true );

        try
        {
            controller.postPotentialDuplicate( pd );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Conflict", wm.getHttpStatus() );
            assertEquals( 409, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "'ABCDEFGHIJ1' and 'ABCDEFGHIJ2' is already marked as a potential duplicate",
                wm.getMessage() );
        }
    }

    @Test
    public void markPotentialDuplicateInvalid()
        throws WebMessageException
    {
        PotentialDuplicate pd = new PotentialDuplicate( "teiA" );
        Mockito.when( deduplicationService.getPotentialDuplicateByUid( Mockito.eq( "1" ) ) ).thenReturn( pd );

        controller.markPotentialDuplicateInvalid( "1" );

        Mockito.verify( deduplicationService ).markPotentialDuplicateInvalid( Mockito.eq( pd ) );
    }

    @Test
    public void markPotentialDuplicateInvalidNotFound()
    {
        Mockito.when( deduplicationService.getPotentialDuplicateByUid( Mockito.eq( "0" ) ) ).thenReturn( null );

        try
        {
            controller.markPotentialDuplicateInvalid( "0" );
        }
        catch ( WebMessageException e )
        {
            WebMessage wm = e.getWebMessage();
            assertEquals( "Not Found", wm.getHttpStatus() );
            assertEquals( 404, wm.getHttpStatusCode().intValue() );
            assertEquals( Status.ERROR, wm.getStatus() );
            assertEquals( "No potentialDuplicate records found with id '0'.", wm.getMessage() );
        }

    }
}