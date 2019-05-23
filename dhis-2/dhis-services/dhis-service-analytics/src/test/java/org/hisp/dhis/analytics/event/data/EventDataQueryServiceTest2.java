/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class EventDataQueryServiceTest2
{

    @Mock
    private ProgramService programService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private LegendSetService legendSetService;

    @Mock
    private DataQueryService dataQueryService;

    @Mock
    private QueryItemLocator queryItemLocator;

    @Mock
    private I18nManager i18nManager;

    @Mock
    private I18n i18n;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultEventDataQueryService subject;

    private final static String PROGRAM_UID = CodeGenerator.generateUid();

    private final static String PROGRAM_STAGE_UID = CodeGenerator.generateUid();
    
    private final static String RELATIONSHIP_TYPE_UID = CodeGenerator.generateUid();
    
    private final static String PROGRAM_INDICATOR_UID = CodeGenerator.generateUid();
    
    @Before
    public void setUp()
    {
        subject = new DefaultEventDataQueryService( programService, programStageService, dataElementService, queryItemLocator,
            attributeService, programIndicatorService, legendSetService, dataQueryService, i18nManager );

        when( i18nManager.getI18n() ).thenReturn( i18n );
        when( i18n.getString( "LAST_12_MONTHS" ) ).thenReturn( "Last 12 months" );
    }

    @Test
    public void t1()
    {
        Program program = createProgram( 'A' );
        program.setUid( PROGRAM_UID );

        ProgramStage ps = createProgramStage( 'B', program );

        when( programService.getProgram( PROGRAM_UID ) ).thenReturn( program );
        when( programStageService.getProgramStage( PROGRAM_STAGE_UID ) ).thenReturn( ps );

        EventDataQueryRequest request = EventDataQueryRequest.newBuilder()
            .dimension( Sets.newHashSet( RELATIONSHIP_TYPE_UID + "." + PROGRAM_INDICATOR_UID ) )
            .program( PROGRAM_UID )
            .stage( PROGRAM_STAGE_UID )
            .build();

        subject.getFromRequest( request );
    }

}
