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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.DhisConvenienceTest.getAggregateDataExchange;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.Api;
import org.hisp.dhis.dataexchange.aggregate.Source;
import org.hisp.dhis.dataexchange.aggregate.Target;
import org.hisp.dhis.dataexchange.aggregate.TargetType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Lars Helge Overland
 */
@ExtendWith( MockitoExtension.class )
public class AggregateDataExchangeObjectBundleHookTest
{
    @Mock
    private ObjectBundle objectBundle;

    @InjectMocks
    private AggregateDataExchangeObjectBundleHook objectBundleHook;

    @Test
    void testValidateSuccessA()
    {
        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );

        assertIsEmpty( objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testValidateSuccessB()
    {
        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.getTarget().getApi().setAccessToken( "d2pat_fjx18dy0iB6nJybPxGSVsoagGtrXMAVn1162422595" );
        exchange.getTarget().getApi().setUsername( null );
        exchange.getTarget().getApi().setPassword( null );

        assertIsEmpty( objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testMissingSourceRequests()
    {
        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.setSource( new Source() );

        assertErrorCode( ErrorCode.E6302, objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testMissingSourceDxItems()
    {
        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.getSource().getRequests().get( 0 ).setDx( new ArrayList<>() );

        assertErrorCode( ErrorCode.E6303, objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testMissingTargetType()
    {
        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.setTarget( new Target() );

        assertErrorCode( ErrorCode.E4000, objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testMissingTargetApi()
    {
        Target target = new Target();
        target.setType( TargetType.EXTERNAL );

        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.setTarget( target );

        assertErrorCode( ErrorCode.E6304, objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testMissingTargetApiUrl()
    {
        Target target = new Target();
        target.setType( TargetType.EXTERNAL );
        target.setApi( new Api() );

        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.setTarget( target );

        assertErrorCode( ErrorCode.E4000, objectBundleHook.validate( exchange, objectBundle ) );
    }

    @Test
    void testMissingTargetApiAuthentication()
    {
        Api api = new Api();
        api.setUrl( "https://play.dhis2.org/demo" );
        api.setUsername( "admin" );

        Target target = new Target();
        target.setType( TargetType.EXTERNAL );
        target.setApi( api );

        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );
        exchange.setTarget( target );

        assertErrorCode( ErrorCode.E6305, objectBundleHook.validate( exchange, objectBundle ) );
    }

    /**
     * Asserts that the error code is present in the list of error reports.
     *
     * @param errorCode the {@link ErrorCode}.
     * @param errorReports the list of {@link ErrorReport}.
     */
    private void assertErrorCode( ErrorCode errorCode, List<ErrorReport> errorReports )
    {
        assertTrue( errorReports.stream().anyMatch( r -> errorCode.equals( r.getErrorCode() ) ),
            String.format( "Error reports expected to contain error code: '%s', %s", errorCode, errorReports ) );
    }
}
