package org.hisp.dhis.dxf2.adx;

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

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.amplecode.staxwax.factory.XMLFactory;
import org.amplecode.staxwax.reader.XMLReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.util.XMLChar;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dataelement.CategoryComboMap;
import org.hisp.dhis.dataelement.CategoryComboMap.CategoryComboMapException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataExportParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.datavalueset.PipedImporter;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.period.Period;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author bobj
 */
public class DefaultADXDataService
    implements ADXDataService
{
    private static final Log log = LogFactory.getLog( DefaultADXDataService.class );

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final int TOTAL_MINUTES_TO_WAIT = 5;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    protected DataValueSetService dataValueSetService;

    @Autowired
    protected DataElementService dataElementService;

    @Autowired
    protected DataElementCategoryService categoryService;

    @Autowired
    protected DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    @Override
    public void getData( DataExportParams params, OutputStream out )
    {
        throw new UnsupportedOperationException( "ADX export not supported yet." );
    }

    @Override
    public ImportSummaries postData( InputStream in, ImportOptions importOptions )
    {
        XMLReader adxReader = XMLFactory.getXMLReader( in );

        ImportSummaries importSummaries = new ImportSummaries();

        adxReader.moveToStartElement( ADXDataService.ROOT, ADXDataService.NAMESPACE );

        ExecutorService executor = Executors.newSingleThreadExecutor();

        int count = 0;
        
        // submit each ADX group to DXF importer as a datavalueSet
        while ( adxReader.moveToStartElement( ADXDataService.GROUP, ADXDataService.NAMESPACE ) )
        {
            try ( PipedOutputStream pipeOut = new PipedOutputStream() )
            {
                Future<ImportSummary> futureImportSummary;
                futureImportSummary = executor.submit( new PipedImporter( dataValueSetService, importOptions, pipeOut ) );
                XMLOutputFactory factory = XMLOutputFactory.newInstance();
                XMLStreamWriter dxfWriter = factory.createXMLStreamWriter( pipeOut );
                
                // note this returns conflicts which are detected at ADX level
                List<ImportConflict> adxConflicts = parseADXGroupToDxf( adxReader, dxfWriter, importOptions );
                
                pipeOut.flush();
                
                ImportSummary summary = futureImportSummary.get( TOTAL_MINUTES_TO_WAIT, TimeUnit.MINUTES );
                
                // add ADX conflicts to the import summary
                for ( ImportConflict conflict : adxConflicts)
                {
                    summary.getConflicts().add( conflict );
                    summary.getImportCount().incrementIgnored();
                }
                importSummaries.addImportSummary( summary );
            }
            catch ( ADXException ex)
            {
                ImportSummary importSummary = new ImportSummary();
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.setDescription( "DataSet import failed for group number " + count );
                importSummary.getConflicts().add( ex.getImportConflict() );
                importSummaries.addImportSummary( importSummary );
                log.warn( "Import failed: " + ex );
            }
            catch ( IOException | XMLStreamException | InterruptedException | ExecutionException | TimeoutException ex )
            {
                ImportSummary importSummary = new ImportSummary();
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.setDescription( "DataSet import failed for group number " + count );
                importSummaries.addImportSummary( importSummary );
                log.warn( "Import failed: " + ex );
            }
            count++;
        }

        executor.shutdown();

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    private List<ImportConflict> parseADXGroupToDxf( XMLReader adxReader, XMLStreamWriter dxfWriter, ImportOptions importOptions )
        throws XMLStreamException, ADXException
    {
        List<ImportConflict> adxConflicts = new LinkedList<>();
        
        dxfWriter.writeStartDocument( "1.0" );
        dxfWriter.writeStartElement( "dataValueSet" );
        dxfWriter.writeDefaultNamespace( "http://dhis2.org/schema/dxf/2.0" );

        IdentifiableProperty dataElementIdScheme = importOptions.getDataElementIdScheme();

        Map<String, String> groupAttributes = readAttributes( adxReader );

        if ( !groupAttributes.containsKey( ADXDataService.PERIOD ) )
        {
            throw new ADXException( ADXDataService.PERIOD + " attribute is required on 'group'" );
        }

        if ( !groupAttributes.containsKey( ADXDataService.ORGUNIT ) )
        {
            throw new ADXException( ADXDataService.ORGUNIT + " attribute is required on 'group'" );
        }

        // translate ADX period to dxf2
        String periodStr = groupAttributes.get( ADXDataService.PERIOD );
        groupAttributes.remove( ADXDataService.PERIOD );
        Period period = ADXPeriod.parse( periodStr );
        groupAttributes.put( ADXDataService.PERIOD, period.getIsoDate());

        // process ADX group attributes
        if ( !groupAttributes.containsKey( ADXDataService.ATTOPTCOMBO )
            && groupAttributes.containsKey( ADXDataService.DATASET ) )
        {
            log.debug( "No attributeOptionCombo present.  Check dataSet for attribute categorycombo" );

            DataSet dataSet = identifiableObjectManager.getObject( DataSet.class, dataElementIdScheme,
                groupAttributes.get( ADXDataService.DATASET ) );
            
            if (dataSet == null)
            {
                throw new ADXException("No dataSet matching identifier: " + groupAttributes.get( ADXDataService.DATASET ));
            }
            groupAttributes.put( ADXDataService.DATASET, dataSet.getUid() );
            DataElementCategoryCombo attributeCombo = dataSet.getCategoryCombo();
            attributesToDXF( ADXDataService.ATTOPTCOMBO, attributeCombo, groupAttributes, dataElementIdScheme );
        }

        // write the remaining attributes through to DXF stream
        for ( String attribute : groupAttributes.keySet() )
        {
            dxfWriter.writeAttribute( attribute, groupAttributes.get( attribute ) );
        }

        // process the dataValues
        while ( adxReader.moveToStartElement( ADXDataService.DATAVALUE, ADXDataService.GROUP ) )
        {
            try 
            {
                parseADXDataValueToDxf( adxReader, dxfWriter, importOptions );
            }
            catch (ADXException ex)
            {
                adxConflicts.add( ex.getImportConflict() );
                log.info("ADX datavalue conflict: " + ex.getImportConflict());
            }
        }

        dxfWriter.writeEndElement();
        dxfWriter.writeEndDocument();
        
        return adxConflicts;
    }

    private void parseADXDataValueToDxf( XMLReader adxReader, XMLStreamWriter dxfWriter, ImportOptions importOptions )
        throws XMLStreamException, ADXException
    {
        Map<String, String> dvAttributes = readAttributes( adxReader );
        
        log.debug("Processing datavalue: " + dvAttributes );
        
        if ( !dvAttributes.containsKey( ADXDataService.DATAELEMENT ) )
        {
            throw new ADXException( ADXDataService.DATAELEMENT + " attribute is required on 'dataValue'" );
        }

        if ( !dvAttributes.containsKey( ADXDataService.VALUE ) )
        {
            throw new ADXException( ADXDataService.VALUE + " attribute is required on 'dataValue'" );
        }

        IdentifiableProperty dataElementIdScheme = importOptions.getDataElementIdScheme();

        DataElement dataElement = identifiableObjectManager.getObject( DataElement.class, dataElementIdScheme,dvAttributes.get( ADXDataService.DATAELEMENT));
        
        if ( dataElement == null )
        {
            throw new ADXException(dvAttributes.get( ADXDataService.DATAELEMENT), "No matching dataelement");
        }
        
        // process ADX data value attributes
        if ( !dvAttributes.containsKey( ADXDataService.CATOPTCOMBO ) )
        {
            log.debug( "No categoryOptionCombo present." );
            DataElementCategoryCombo categoryCombo = dataElement.getCategoryCombo();

            attributesToDXF( ADXDataService.CATOPTCOMBO, categoryCombo, dvAttributes, dataElementIdScheme );
        }
        
        // if data element type is not numeric we need to pick out the 'annotation' element
        if ( !dataElement.getValueType().isNumeric() )        
        {
            adxReader.moveToStartElement( ADXDataService.ANNOTATION, ADXDataService.DATAVALUE );
            if ( adxReader.isStartElement( ADXDataService.ANNOTATION ) )
            {
                String textValue = adxReader.getElementValue();
                dvAttributes.put( ADXDataService.VALUE, textValue );
            }
            else
            {
                throw new ADXException( dvAttributes.get( ADXDataService.DATAELEMENT),"Dataelement expects text annotation" );
            }
        }
        
        log.debug("Processing datavalue as DXF2: " + dvAttributes );
        
        dxfWriter.writeStartElement( "dataValue" );
        
        // pass through the remaining attributes to DXF
        for ( String attribute : dvAttributes.keySet() )
        {
            dxfWriter.writeAttribute( attribute, dvAttributes.get( attribute ) );
        }
        dxfWriter.writeEndElement();
    }

    private Map<String, DataElementCategory> createCategoryMap( DataElementCategoryCombo catcombo )
        throws ADXException
    {
        Map<String, DataElementCategory> categoryMap = new HashMap<>();

        List<DataElementCategory> categories = catcombo.getCategories();

        for ( DataElementCategory category : categories )
        {
            String categoryCode = category.getCode();
            
            if ( categoryCode == null || !XMLChar.isValidName( categoryCode ) )
            {
                throw new ADXException( "Category code for " + category.getName() + " is missing or invalid: "
                    + categoryCode );
            }
            
            categoryMap.put( category.getCode(), category );
        }

        return categoryMap;
    }

    private DataElementCategoryOptionCombo getCatOptComboFromAttributes( Map<String, String> attributes,
        DataElementCategoryCombo catcombo, IdentifiableProperty scheme )
        throws ADXException
    {
        CategoryComboMap catcomboMap;
        
        try
        {
            catcomboMap = new CategoryComboMap( catcombo, scheme );
            log.debug( catcomboMap.toString() );

        }
        catch ( CategoryComboMapException ex )
        {
            log.info( "Failed to create catcomboMap from " + catcombo );
            throw new ADXException( ex.getMessage() );
        }

        String compositeIdentifier = StringUtils.EMPTY;
        
        for ( DataElementCategory category : catcomboMap.getCategories() )
        {
            String catCode = category.getCode();
            
            if ( catCode == null )
            {
                throw new ADXException( "No category matching " + catCode );
            }
            
            String catAttribute = attributes.get( catCode );
            
            if ( catAttribute == null )
            {
                throw new ADXException( "Missing required attribute from catcombo: " + catCode );
            }
            
            compositeIdentifier += "\"" + catAttribute + "\"";
        }
        
        DataElementCategoryOptionCombo catoptcombo = catcomboMap.getCategoryOptionCombo( compositeIdentifier );

        if ( catoptcombo == null )
        {
            throw new ADXException( "Invalid attributes:" + attributes );
        }
        
        return catoptcombo;
    }

    private void attributesToDXF( String optionComboName, DataElementCategoryCombo catCombo,
        Map<String, String> attributes, IdentifiableProperty scheme )
        throws ADXException
    {
        log.debug( "adx attributes: " + attributes );

        if ( catCombo.isDefault() )
        {
            return;
        }
        
        Map<String, DataElementCategory> categoryMap = createCategoryMap( catCombo );

        Map<String, String> attributeOptions = new HashMap<>();
        
        for ( String category : categoryMap.keySet() )
        {
            if ( attributes.containsKey( category ) )
            {
                attributeOptions.put( category, attributes.get( category ) );
                attributes.remove( category );
            }
            else
            {
                throw new ADXException( "catcombo " + catCombo.getName() + " must have " + categoryMap.get( category ).getName() );
            }
        }

        DataElementCategoryOptionCombo catOptCombo = getCatOptComboFromAttributes( attributeOptions, catCombo, scheme );
        attributes.put( optionComboName, catOptCombo.getUid() );
            
        log.debug( "dxf attributes: " + attributes );
    }

    // TODO this should be part of staxwax library
    protected Map<String, String> readAttributes( XMLReader staxWaxReader )
    {
        Map<String, String> attributes = new HashMap<>();

        XMLStreamReader reader = staxWaxReader.getXmlStreamReader();

        if ( reader.getEventType() != START_ELEMENT )
        {
            throw new IllegalArgumentException( "Trying to retrieve attributes from non START_ELEMENT node" );
        }

        // Read attributes
        for ( int i = 0; i < reader.getAttributeCount(); i++ )
        {
            attributes.put( reader.getAttributeLocalName( i ), reader.getAttributeValue( i ) );
        }

        return attributes;
    }
}
