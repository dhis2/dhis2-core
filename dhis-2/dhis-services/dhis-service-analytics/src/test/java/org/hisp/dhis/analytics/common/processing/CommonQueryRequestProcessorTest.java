/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_LIMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.Test;

class CommonQueryRequestProcessorTest
{
    private SystemSettingManager systemSettingManager = mock( SystemSettingManager.class );

    private final CommonQueryRequestProcessor commonQueryRequestProcessor = new CommonQueryRequestProcessor(
        systemSettingManager );

    @Test
    void testPaginationPagingTruePageSizeHigherThanMaxLimit()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 1000 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPaging( true )
            .withPageSize( 10000 );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertEquals( 1000, processed.getPageSize() );
        assertTrue( processed.isPaging() );
    }

    @Test
    void testPaginationPagingTruePageSizeLowerThanMaxLimit()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 1000 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPaging( true )
            .withPageSize( 100 );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertEquals( 100, processed.getPageSize() );
        assertTrue( processed.isPaging() );
    }

    @Test
    void testUnlimitedMaxLimit0()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 0 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertFalse( processed.isPaging() );
    }

    @Test
    void testUnlimitedIgnoreLimit()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 100 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withIgnoreLimit( true )
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertFalse( processed.isPaging() );
    }

    @Test
    void testPagingFalseAndPageSizeGreaterThanMaxLimit()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 100 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPageSize( 150 )
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertFalse( processed.isPaging() );
        assertFalse( processed.isIgnoreLimit() );
        assertEquals( 100, processed.getPageSize() );
    }

    @Test
    void testPagingFalseAndPageSizeLowerThanMaxLimit()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 100 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPageSize( 50 )
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertFalse( processed.isPaging() );
        assertFalse( processed.isIgnoreLimit() );
        assertEquals( 100, processed.getPageSize() );
    }

    @Test
    void testPagingFalseAndNoMaxLimit()
    {
        int unlimited = 0;

        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( unlimited );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPageSize( 50 )
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertFalse( processed.isPaging() );
        assertTrue( processed.isIgnoreLimit() );
        assertEquals( 50, processed.getPageSize() );
    }

    @Test
    void testPagingTrueAndNoMaxLimit()
    {
        int unlimited = 0;

        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( unlimited );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPageSize( 50 )
            .withPaging( true );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertTrue( processed.isPaging() );
        assertFalse( processed.isIgnoreLimit() );
        assertEquals( 50, processed.getPageSize() );
    }

    @Test
    void testProgramStatusWrongFormat()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withProgramStatus( Set.of( "COMPLETED" ) );
        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> commonQueryRequestProcessor.process( request ) );

        assertEquals( "parameters programStatus/enrollmentStatus must be of the form: [programUid].[ENROLLMENT_STATUS]",
            exception.getMessage() );
    }

    @Test
    void testEnrollmentStatusWrongFormat()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withEnrollmentStatus( Set.of( "COMPLETED" ) );
        IllegalQueryException exception = assertThrows( IllegalQueryException.class,
            () -> commonQueryRequestProcessor.process( request ) );

        assertEquals( "parameters programStatus/enrollmentStatus must be of the form: [programUid].[ENROLLMENT_STATUS]",
            exception.getMessage() );
    }

    @Test
    void testEventStatusWrongFormat()
    {
        for ( String eventStatus : List.of( "programUid.ACTIVE", "COMPLETED" ) )
        {
            CommonQueryRequest request = new CommonQueryRequest()
                .withEventStatus( Set.of( eventStatus ) );
            IllegalQueryException exception = assertThrows( IllegalQueryException.class,
                () -> commonQueryRequestProcessor.process( request ) );
            assertEquals( "parameter eventStatus must be of the form: [programUid].[programStageUid].[EVENT_STATUS]",
                exception.getMessage() );
        }
    }

    @Test
    void testProgramStatusWrongEnum()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withProgramStatus( Set.of( "programUid.WRONG_PROGRAM_STATUS" ) );
        IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
            () -> commonQueryRequestProcessor.process( request ) );

        assertEquals( "No enum constant org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus.WRONG_PROGRAM_STATUS",
            exception.getMessage() );
    }

    @Test
    void testEnrollmentStatusStatusWrongEnum()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withEnrollmentStatus( Set.of( "programUid.WRONG_PROGRAM_STATUS" ) );
        IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
            () -> commonQueryRequestProcessor.process( request ) );

        assertEquals( "No enum constant org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus.WRONG_PROGRAM_STATUS",
            exception.getMessage() );
    }

    @Test
    void testEventStatusWrongEnum()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withEventStatus( Set.of( "programUid.programStageUid.WRONG_EVENT_STATUS" ) );
        IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
            () -> commonQueryRequestProcessor.process( request ) );

        assertEquals( "No enum constant org.hisp.dhis.event.EventStatus.WRONG_EVENT_STATUS", exception.getMessage() );
    }

    @Test
    void testProgramStatusOK()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withProgramStatus( Set.of( "programUid.COMPLETED" ) );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );

        String parsedDimension = processed.getDimension().iterator().next();

        assertEquals( "programUid.ENROLLMENT_STATUS:COMPLETED", parsedDimension );
    }

    @Test
    void testEnrollmentStatusOK()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withEnrollmentStatus( Set.of( "programUid.COMPLETED" ) );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );

        String parsedDimension = processed.getDimension().iterator().next();

        assertEquals( "programUid.ENROLLMENT_STATUS:COMPLETED", parsedDimension );
    }

    @Test
    void testEventStatusOK()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withEventStatus( (Set.of( "programUid.programStageUid.COMPLETED" )) );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );

        String parsedDimension = processed.getDimension().iterator().next();

        assertEquals( "programUid.programStageUid.EVENT_STATUS:COMPLETED", parsedDimension );
    }
}
