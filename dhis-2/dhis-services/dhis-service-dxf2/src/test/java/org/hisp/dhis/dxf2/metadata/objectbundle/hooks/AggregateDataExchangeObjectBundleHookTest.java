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

import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import java.util.List;

import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.Api;
import org.hisp.dhis.dataexchange.aggregate.Filter;
import org.hisp.dhis.dataexchange.aggregate.Source;
import org.hisp.dhis.dataexchange.aggregate.SourceRequest;
import org.hisp.dhis.dataexchange.aggregate.Target;
import org.hisp.dhis.dataexchange.aggregate.TargetRequest;
import org.hisp.dhis.dataexchange.aggregate.TargetType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
public class AggregateDataExchangeObjectBundleHookTest
{
    @Mock
    private ObjectBundle objectBundle;

    @InjectMocks
    private AggregateDataExchangeObjectBundleHook objectBundleHook;

    @Test
    void testValidateSuccess()
    {
        AggregateDataExchange exchange = getAggregateDataExchange( 'A' );

        assertIsEmpty( objectBundleHook.validate( exchange, objectBundle ) );
    }

    private AggregateDataExchange getAggregateDataExchange( char uniqueChar )
    {
        SourceRequest sourceRequest = new SourceRequest();
        sourceRequest.getDx().addAll( List.of( "LrDpG50RAU9", "uR5HCiJhQ1w" ) );
        sourceRequest.getPe().addAll( List.of( "202201", "202202" ) );
        sourceRequest.getOu().addAll( List.of( "G9BuXqtNeeb", "jDgiLmYwPDm" ) );
        sourceRequest.getFilters().addAll( List.of(
            new Filter().setDimension( "MuTwGW0BI4o" ).setItems( List.of( "v9oULMMdmzE", "eJHJ0bfDCEO" ) ),
            new Filter().setDimension( "dAOgE7mgysJ" ).setItems( List.of( "rbE2mZX86AA", "XjOFfrPwake" ) ) ) );

        Source source = new Source()
            .setRequests( List.of( sourceRequest ) );

        Api api = new Api()
            .setUrl( "https://play.dhis2.org/demo" )
            .setUsername( "admin" )
            .setPassword( "district" );

        Target target = new Target()
            .setApi( api )
            .setType( TargetType.EXTERNAL )
            .setRequest( new TargetRequest() );

        AggregateDataExchange exchange = new AggregateDataExchange();
        exchange.setAutoFields();
        exchange.setName( "DataExchange" + uniqueChar );
        exchange.setSource( source );
        exchange.setTarget( target );
        return exchange;
    }
}
