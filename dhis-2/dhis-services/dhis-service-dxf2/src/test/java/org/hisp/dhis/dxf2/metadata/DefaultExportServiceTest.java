package org.hisp.dhis.dxf2.metadata;

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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.schema.SchemaService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author bobj
 */
public class DefaultExportServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private MetadataExportService exportService;

    @Autowired
    private SchemaService schemaService;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataSet dsA;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private Period peA;

    private Period peB;

    @Override
    public void setUpTest()
    {
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        dsA = DhisConvenienceTest.createDataSet( 'A', new MonthlyPeriodType() );
        ouA = DhisConvenienceTest.createOrganisationUnit( 'A' );
        ouB = DhisConvenienceTest.createOrganisationUnit( 'B' );
        peA = DhisConvenienceTest.createPeriod( DhisConvenienceTest.getDate( 2012, 1, 1 ), DhisConvenienceTest.getDate( 2012, 1, 31 ) );
        peB = DhisConvenienceTest.createPeriod( DhisConvenienceTest.getDate( 2012, 2, 1 ), DhisConvenienceTest.getDate( 2012, 2, 29 ) );

        deA.setUid( "f7n9E0hX8qk" );
        deB.setUid( "Ix2HsbDMLea" );
        deC.setUid( "eY5ehpbEsB7" );
        dsA.setUid( "pBOMPrpg1QX" );
        ouA.setUid( "DiszpKrYNg8" );
        ouB.setUid( "BdfsJfj87js" );

        deA.setCode( "DE_A" );
        deB.setCode( "DE_B" );
        deC.setCode( "DE_C" );
        dsA.setCode( "DS_A" );
        ouA.setCode( "OU_A" );
        ouB.setCode( "OU_B" );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataSetService.addDataSet( dsA );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        periodService.addPeriod( peA );
        periodService.addPeriod( peB );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void exportMetaDataTest() throws IOException, XPathExpressionException
    {
        MetadataExportParams params = new MetadataExportParams();
        params.addQuery( Query.from( schemaService.getSchema( DataElement.class ) ) );
        params.addQuery( Query.from( schemaService.getSchema( OrganisationUnit.class ) ) );

        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadataMap = exportService.getMetadata( params );

        Metadata metadata = new Metadata();
        metadata.setDataElements( (List<DataElement>) metadataMap.get( DataElement.class ) );
        metadata.setOrganisationUnits( (List<OrganisationUnit>) metadataMap.get( OrganisationUnit.class ) );

        String metaDataXml = DefaultRenderService.getXmlMapper().writeValueAsString( metadata );

        assertEquals( "1", xpathTest( "count(//d:organisationUnits)", metaDataXml ) );
        assertEquals( "2", xpathTest( "count(//d:organisationUnit)", metaDataXml ) );
        assertEquals( "3", xpathTest( "count(//d:dataElement)", metaDataXml ) );
        assertEquals( "DE_A", xpathTest( "//d:dataElement[@name='DataElementA']/@code", metaDataXml ) );
    }
}
