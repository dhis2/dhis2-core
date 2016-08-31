package org.hisp.dhis.eventchart;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jan Henrik Overland
 */
public class EventChartServiceTest
    extends DhisSpringTest
{
    @Autowired
    private EventChartService eventChartService;

    @Autowired
    private ProgramService programService;

    private Program prA;

    @Override
    public void setUpTest()
    {
        prA = createProgram( 'A', null, null );
        programService.addProgram( prA );
    }

    @Test
    public void testSaveGet()
    {
        EventChart ecA = new EventChart( "ecA" );
        ecA.setProgram( prA );
        ecA.setType( ChartType.COLUMN );
        EventChart ecB = new EventChart( "ecB" );
        ecB.setProgram( prA );
        ecB.setType( ChartType.COLUMN );
        EventChart ecC = new EventChart( "ecC" );
        ecC.setProgram( prA );
        ecC.setType( ChartType.COLUMN );

        int idA = eventChartService.saveEventChart( ecA );
        int idB = eventChartService.saveEventChart( ecB );
        int idC = eventChartService.saveEventChart( ecC );

        assertEquals( "ecA", eventChartService.getEventChart( idA ).getName() );
        assertEquals( "ecB", eventChartService.getEventChart( idB ).getName() );
        assertEquals( "ecC", eventChartService.getEventChart( idC ).getName() );
    }

    @Test
    public void testDelete()
    {
        EventChart ecA = new EventChart( "ecA" );
        ecA.setProgram( prA );
        ecA.setType( ChartType.COLUMN );
        EventChart ecB = new EventChart( "ecB" );
        ecB.setProgram( prA );
        ecB.setType( ChartType.COLUMN );
        EventChart ecC = new EventChart( "ecC" );
        ecC.setProgram( prA );
        ecC.setType( ChartType.COLUMN );

        int idA = eventChartService.saveEventChart( ecA );
        int idB = eventChartService.saveEventChart( ecB );
        int idC = eventChartService.saveEventChart( ecC );

        assertNotNull( eventChartService.getEventChart( idA ) );
        assertNotNull( eventChartService.getEventChart( idB ) );
        assertNotNull( eventChartService.getEventChart( idC ) );

        eventChartService.deleteEventChart( ecA );

        assertNull( eventChartService.getEventChart( idA ) );
        assertNotNull( eventChartService.getEventChart( idB ) );
        assertNotNull( eventChartService.getEventChart( idC ) );

        eventChartService.deleteEventChart( ecB );

        assertNull( eventChartService.getEventChart( idA ) );
        assertNull( eventChartService.getEventChart( idB ) );
        assertNotNull( eventChartService.getEventChart( idC ) );

        eventChartService.deleteEventChart( ecC );

        assertNull( eventChartService.getEventChart( idA ) );
        assertNull( eventChartService.getEventChart( idB ) );
        assertNull( eventChartService.getEventChart( idC ) );
    }
}
