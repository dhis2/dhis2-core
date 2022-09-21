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
package org.hisp.dhis.dxf2.events.importer;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.dxf2.events.importer.EventTestUtils.createBaseEvent;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public abstract class BasePreProcessTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    protected WorkContext workContext;

    @Mock
    protected ServiceDelegator serviceDelegator;

    @Mock
    protected ProgramInstanceStore programInstanceStore;

    protected Event event;

    protected Program program;

    @Before
    public void superSetUp()
    {
        event = createBaseEvent();
        program = createProgram( 'P' );

        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );

        when( workContext.getProgramsMap() ).thenReturn( programMap );
        when( workContext.getImportOptions() ).thenReturn( ImportOptions.getDefaultImportOptions() );
        when( workContext.getServiceDelegator() ).thenReturn( serviceDelegator );

        when( serviceDelegator.getProgramInstanceStore() ).thenReturn( programInstanceStore );
    }

}
