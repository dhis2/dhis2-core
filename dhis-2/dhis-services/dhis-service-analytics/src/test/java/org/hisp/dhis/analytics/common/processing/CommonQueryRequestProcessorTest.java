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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CommonQueryRequestProcessorTest
{
    private SystemSettingManager systemSettingManager = mock( SystemSettingManager.class );

    private final CommonQueryRequestProcessor commonQueryRequestProcessor = new CommonQueryRequestProcessor(
        systemSettingManager );

    @Test
    @Disabled( "behaviour has changed, this test doesn't make sense anymore" )
    void testEventDate()
    {
        CommonQueryRequest request = new CommonQueryRequest()
            .withEventDate( "IpHINAT79UW.LAST_YEAR" )
            .withIncidentDate( "LAST_MONTH" )
            .withEnrollmentDate( "2021-06-30" )
            .withLastUpdated( "TODAY" )
            .withScheduledDate( "YESTERDAY" );

        assertEquals(
            "pe:IpHINAT79UW.LAST_YEAR:EVENT_DATE;2021-06-30:ENROLLMENT_DATE;YESTERDAY:SCHEDULED_DATE;LAST_MONTH:INCIDENT_DATE;TODAY:LAST_UPDATED",
            commonQueryRequestProcessor.process( request ).getDimension().stream().findFirst().orElse( null ) );
    }

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
    void testPagingFalseMaxLimit()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 100 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPageSize( 150 )
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertTrue( processed.isPaging() );
        assertEquals( 100, processed.getPageSize() );
    }

    @Test
    void testPagingFalsePaging()
    {
        when( systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT ) ).thenReturn( 100 );
        CommonQueryRequest request = new CommonQueryRequest()
            .withPageSize( 50 )
            .withPaging( false );
        CommonQueryRequest processed = commonQueryRequestProcessor.process( request );
        assertTrue( processed.isPaging() );
        assertEquals( 50, processed.getPageSize() );
    }
}
