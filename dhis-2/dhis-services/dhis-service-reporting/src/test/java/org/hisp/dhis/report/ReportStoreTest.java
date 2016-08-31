package org.hisp.dhis.report;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class ReportStoreTest
    extends DhisSpringTest
{
    @Resource(name="org.hisp.dhis.report.ReportStore")
    private GenericStore<Report> reportStore;
    
    @Autowired
    private ReportTableService reportTableService;
    
    private ReportTable reportTableA;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        reportTableA = new ReportTable();
        reportTableA.setName( "ReportTableA" );

        reportTableService.saveReportTable( reportTableA );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testSaveGet()
    {
        Report reportA = new Report( "ReportA", ReportType.JASPER_REPORT_TABLE, "DesignA", reportTableA );
        Report reportB = new Report( "ReportB", ReportType.JASPER_REPORT_TABLE, "DesignB", reportTableA );
        
        int idA = reportStore.save( reportA );
        int idB = reportStore.save( reportB );
        
        assertEquals( reportA, reportStore.get( idA ) );
        assertEquals( reportB, reportStore.get( idB ) );
    }

    @Test
    public void testSaveGetUpdate()
    {
        Report reportA = new Report( "ReportA", ReportType.JASPER_REPORT_TABLE, "DesignA", reportTableA );
        Report reportB = new Report( "ReportB", ReportType.JASPER_REPORT_TABLE, "DesignB", reportTableA );
        
        int idA = reportStore.save( reportA );
        int idB = reportStore.save( reportB );
        
        assertEquals( reportA, reportStore.get( idA ) );
        assertEquals( reportB, reportStore.get( idB ) );
        
        reportA.setDesignContent( "UpdatedDesignA" );
        reportB.setDesignContent( "UpdatedDesignB" );
        
        int updatedIdA = reportStore.save( reportA );
        int updatedIdB = reportStore.save( reportB );
        
        assertEquals( idA, updatedIdA );
        assertEquals( idB, updatedIdB );
        
        assertEquals( "UpdatedDesignA", reportStore.get( updatedIdA ).getDesignContent() );
        assertEquals( "UpdatedDesignB", reportStore.get( updatedIdB ).getDesignContent() );
    }

    @Test
    public void testDelete()
    {
        Report reportA = new Report( "ReportA", ReportType.JASPER_REPORT_TABLE, "DesignA", reportTableA );
        Report reportB = new Report( "ReportB", ReportType.JASPER_REPORT_TABLE, "DesignB", reportTableA );
        
        int idA = reportStore.save( reportA );
        int idB = reportStore.save( reportB );
        
        assertNotNull( reportStore.get( idA ) );
        assertNotNull( reportStore.get( idB ) );
        
        reportStore.delete( reportA );

        assertNull( reportStore.get( idA ) );
        assertNotNull( reportStore.get( idB ) );

        reportStore.delete( reportB );

        assertNull( reportStore.get( idA ) );
        assertNull( reportStore.get( idB ) );
    }

    @Test
    public void testGetAll()
    {
        Report reportA = new Report( "ReportA", ReportType.JASPER_REPORT_TABLE, "DesignA", reportTableA );
        Report reportB = new Report( "ReportB", ReportType.JASPER_REPORT_TABLE, "DesignB", reportTableA );
        
        reportStore.save( reportA );
        reportStore.save( reportB );
        
        List<Report> reports = reportStore.getAll();
        
        assertNotNull( reports );
        assertEquals( 2, reports.size() );
        assertTrue( reports.contains( reportA ) );
        assertTrue( reports.contains( reportB ) );        
    }
}
