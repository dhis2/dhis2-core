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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.enrollment.RequestParams.DEFAULT_FIELDS_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT ) // common setup
@ExtendWith( MockitoExtension.class )
class EnrollmentRequestParamsMapperTest
{

    private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

    private static final String ORG_UNIT_2_UID = "TK4KA0IIWqa";

    private static final String PROGRAM_UID = "XhBYIraw7sv";

    private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

    private static final String TRACKED_ENTITY_UID = "DGbr8GHG4li";

    @Mock
    private EnrollmentFieldsParamMapper fieldsParamMapper;

    @InjectMocks
    private EnrollmentRequestParamsMapper mapper;

    @BeforeEach
    void setUp()
    {
        when(fieldsParamMapper.map(anyList())).thenReturn(EnrollmentParams.FALSE);
    }

    @Test
    void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
    {
        RequestParams requestParams = new RequestParams();

        mapper.map( requestParams );

        verify( fieldsParamMapper, times( 1 ) ).map( FieldFilterParser.parse( DEFAULT_FIELDS_PARAM ) );
    }

    @Test
    void testMappingOrgUnit()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setOrgUnit( ORG_UNIT_1_UID + ";" + ORG_UNIT_2_UID );
        requestParams.setProgram( UID.of( PROGRAM_UID ) );

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertContainsOnly( Set.of( ORG_UNIT_1_UID, ORG_UNIT_2_UID ), params.getOrgUnitUids() );
    }

    @Test
    void testMappingOrgUnits()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setOrgUnits( Set.of( UID.of( ORG_UNIT_1_UID ), UID.of( ORG_UNIT_2_UID ) ) );
        requestParams.setProgram( UID.of( PROGRAM_UID ) );

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertContainsOnly( Set.of( ORG_UNIT_1_UID, ORG_UNIT_2_UID ), params.getOrgUnitUids() );
    }

    @Test
    void testMappingProgram()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setProgram( UID.of( PROGRAM_UID ) );

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertEquals( PROGRAM_UID, params.getProgramUid() );
    }

    @Test
    void testMappingTrackedEntityType()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setTrackedEntityType( UID.of( TRACKED_ENTITY_TYPE_UID ) );

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertEquals( TRACKED_ENTITY_TYPE_UID, params.getTrackedEntityTypeUid() );
    }

    @Test
    void testMappingTrackedEntity()
    {
        RequestParams requestParams = new RequestParams();
        requestParams.setTrackedEntity( UID.of( TRACKED_ENTITY_UID ) );

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertEquals( TRACKED_ENTITY_UID, params.getTrackedEntityUid() );
    }

    @Test
    void testMappingOrderParams()
    {
        RequestParams requestParams = new RequestParams();
        OrderCriteria order1 = OrderCriteria.of( "field1", SortDirection.ASC );
        OrderCriteria order2 = OrderCriteria.of( "field2", SortDirection.DESC );
        requestParams.setOrder( List.of( order1, order2 ) );

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertEquals( List.of(
            new OrderParam( "field1", SortDirection.ASC ),
            new OrderParam( "field2", SortDirection.DESC ) ), params.getOrder() );
    }

    @Test
    void testMappingOrderParamsNoOrder()
    {
        RequestParams requestParams = new RequestParams();

        EnrollmentOperationParams params = mapper.map( requestParams );

        assertIsEmpty( params.getOrder() );
    }
}
