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
package org.hisp.dhis.dxf2.events.importer.update.validation;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT )
class EventBasicCheckTest extends BaseValidationTest
{

    private ProgramStageInstanceBasicCheck rule;

    @BeforeEach
    void setUp()
    {
        rule = new ProgramStageInstanceBasicCheck();
    }

    @Test
    void failOnMissingProgramStageInstance()
    {
        when( workContext.getProgramStageInstanceMap() ).thenReturn( new HashMap<>() );
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event, "Event ID " + event.getEvent() + " doesn't point to valid event" );
    }

    @Test
    void failOnDeletedProgramStageInstance()
    {
        Map<String, Event> programStageInstanceMap = new HashMap<>();
        Event psi = new Event();
        psi.setDeleted( true );
        programStageInstanceMap.put( event.getEvent(), psi );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( programStageInstanceMap );
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
            "Event ID " + event.getEvent() + " was already used and/or deleted. This event can not be modified." );
    }

    @Test
    void failOnProgramStageInstanceAndInvalidImportOption()
    {
        Map<String, Event> programStageInstanceMap = new HashMap<>();
        Event psi = new Event();
        programStageInstanceMap.put( event.getEvent(), psi );
        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setImportStrategy( ImportStrategy.CREATE );
        when( workContext.getImportOptions() ).thenReturn( importOptions );
        when( workContext.getProgramStageInstanceMap() ).thenReturn( programStageInstanceMap );
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
            "Event ID " + event.getEvent() + " was already used and/or deleted. This event can not be modified." );
    }
}
