package org.hisp.dhis.dxf2.importsummary;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class ImportSummariesTest
{
    @Test
    public void testAddImportSummary()
    {
        ImportSummaries summaries = new ImportSummaries();
        
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.SUCCESS, "Great success",
                new ImportCount( 4, 2, 1, 2 ) ) );
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.WARNING, "Ouch warning",
                new ImportCount( 1, 2, 3, 0 ) ) );
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.SUCCESS, "Great failure",
                new ImportCount( 0, 0, 4, 3 ) ) );
        
        assertEquals( 5, summaries.getImported() );
        assertEquals( 4, summaries.getUpdated() );
        assertEquals( 8, summaries.getIgnored() );
        assertEquals( 5, summaries.getDeleted() );        
    }
    
    @Test
    public void testGetImportStatus()
    {
        ImportSummaries summaries = new ImportSummaries();
        
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.SUCCESS, "Great success",
                new ImportCount( 4, 2, 1, 2 ) ) );
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.WARNING, "Ouch warning",
                new ImportCount( 1, 2, 3, 0 ) ) );
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.ERROR, "Great failure",
                new ImportCount( 0, 0, 4, 3 ) ) );
        
        assertEquals( ImportStatus.ERROR, summaries.getStatus() );
        
        summaries = new ImportSummaries();
        
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.SUCCESS, "Great success",
                new ImportCount( 4, 2, 1, 2 ) ) );
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.WARNING, "Ouch warning",
                new ImportCount( 1, 2, 3, 0 ) ) );
        
        assertEquals( ImportStatus.WARNING, summaries.getStatus() );

        summaries = new ImportSummaries();
        
        summaries.addImportSummary( 
            new ImportSummary( ImportStatus.SUCCESS, "Great success",
                new ImportCount( 4, 2, 1, 2 ) ) );

        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );

        summaries = new ImportSummaries();

        assertEquals( ImportStatus.SUCCESS, summaries.getStatus() );
    }
}
