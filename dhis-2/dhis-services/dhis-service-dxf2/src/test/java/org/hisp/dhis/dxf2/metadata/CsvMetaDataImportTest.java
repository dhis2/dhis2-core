package org.hisp.dhis.dxf2.metadata;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class CsvMetaDataImportTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private OptionService optionService;
    
    @Autowired
    private CsvImportService csvImportService;
    
    @Autowired
    private ImportService importService;
    
    private InputStream input;
    
    private final ImportOptions importOptions = new ImportOptions().setImportStrategy( ImportStrategy.NEW_AND_UPDATES );
    
    @Test
    public void testDataElementImport()
        throws Exception
    {        
        input = new ClassPathResource( "metadata/dataElements.csv" ).getInputStream();
        
        MetaData metaData = csvImportService.fromCsv( input, DataElement.class );
        
        assertEquals( 2, metaData.getDataElements().size() );
        
        ImportSummary summary = importService.importMetaData( null, metaData, importOptions );
        
        assertEquals( 2, summary.getImportCount().getImported() );
        
        Collection<DataElement> dataElements = dataElementService.getAllDataElements();
        
        assertEquals( 2, dataElements.size() );
    }
    
    @Test
    public void testOptionSetImport()
        throws Exception
    {
        input = new ClassPathResource( "metadata/optionSets.csv" ).getInputStream();

        MetaData metaData = csvImportService.fromCsv( input, OptionSet.class );
        
        assertEquals( 4, metaData.getOptionSets().size() );
        assertEquals( 3, metaData.getOptionSets().get( 0 ).getOptions().size() );
        assertEquals( 3, metaData.getOptionSets().get( 1 ).getOptions().size() );
        assertEquals( 3, metaData.getOptionSets().get( 2 ).getOptions().size() );
        assertEquals( 3, metaData.getOptionSets().get( 3 ).getOptions().size() );

        ImportSummary summary = importService.importMetaData( null, metaData, importOptions );

        assertEquals( 16, summary.getImportCount().getImported() );
        
        List<OptionSet> optionSets = new ArrayList<>( optionService.getAllOptionSets() );
        
        assertEquals( 4, optionSets.size() );
        assertEquals( 3, optionSets.get( 0 ).getOptions().size() );
        assertEquals( 3, optionSets.get( 1 ).getOptions().size() );
        assertEquals( 3, optionSets.get( 2 ).getOptions().size() );
        assertEquals( 3, optionSets.get( 3 ).getOptions().size() );
    }
}
