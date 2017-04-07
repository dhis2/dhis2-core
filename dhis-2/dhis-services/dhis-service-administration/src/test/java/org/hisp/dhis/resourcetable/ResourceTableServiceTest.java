package org.hisp.dhis.resourcetable;

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

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class ResourceTableServiceTest
    extends DhisTest
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;
    
    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private DataSetService dataSetService;
    
    @Override
    public void setUpTest()
    {
        PeriodType pt = new MonthlyPeriodType();
        
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        
        DataElementGroup degA = createDataElementGroup( 'A' );
        DataElementGroup degB = createDataElementGroup( 'B' );
        
        degA.addDataElement( deA );
        degB.addDataElement( deB );
        
        idObjectManager.save( degA );
        idObjectManager.save( degB );
        
        DataElementGroupSet degsA = createDataElementGroupSet( 'A' );
        
        degsA.addDataElementGroup( degA );
        degsA.addDataElementGroup( degB );
        
        idObjectManager.save( degsA );
        
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C' );
        
        ouB.setParent( ouA );
        ouC.setParent( ouA );
        ouA.getChildren().add( ouB );
        ouA.getChildren().add( ouC );
        
        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
        
        DataSet dsA = createDataSet( 'A', pt );
        DataSet dsB = createDataSet( 'B', pt );
        
        dsA.addDataSetElement( deA );
        dsB.addDataSetElement( deA );
        dsA.addOrganisationUnit( ouA );
        dsB.addOrganisationUnit( ouA );
        
        dataSetService.addDataSet( dsA );
        dataSetService.addDataSet( dsB );
    }
    
    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
    
    @Test
    public void testGenerateAllResourceTables()
    {
        resourceTableService.generateOrganisationUnitStructures();
        resourceTableService.generateDataSetOrganisationUnitCategoryTable();
        resourceTableService.generateCategoryOptionComboNames();
        resourceTableService.generateDataElementGroupSetTable();
        resourceTableService.generateIndicatorGroupSetTable();
        resourceTableService.generateOrganisationUnitGroupSetTable();
        resourceTableService.generateCategoryTable();
        resourceTableService.generateDataElementTable();
        resourceTableService.generatePeriodTable();
        resourceTableService.generateDatePeriodTable();
        resourceTableService.generateDataElementCategoryOptionComboTable();
    }
}
