package org.hisp.dhis.dxf2.adx;

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

import java.util.Set;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.staxwax.factory.XMLFactory;
import org.hisp.staxwax.reader.XMLReader;
import org.hisp.staxwax.writer.XMLWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.util.XMLChar;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dataelement.CategoryComboMap;
import org.hisp.dhis.dataelement.CategoryComboMap.CategoryComboMapException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Date;
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
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.period.PeriodService;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

/**
 * @author bobj
 */
public class DefaultAdxDataService
    implements AdxDataService
{
    private static final Log log = LogFactory.getLog( DefaultAdxDataService.class );

    private static final int TOTAL_MINUTES_TO_WAIT = 5;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private Notifier notifier;

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    @Override
    public DataExportParams getFromUrl( Set<String> dataSets, Set<String> periods, Date startDate, Date endDate,
        Set<String> organisationUnits, boolean includeChildren, boolean includeDeleted, Date lastUpdated, Integer limit, IdSchemes outputIdSchemes )
    {
        DataExportParams params = new DataExportParams();

        if ( dataSets != null )
        {
            params.getDataSets().addAll( identifiableObjectManager.getByCode( DataSet.class, dataSets ) );
        }

        if ( periods != null && !periods.isEmpty() )
        {
            params.getPeriods().addAll( periodService.reloadIsoPeriods( new ArrayList<>( periods ) ) );
        }
        else if ( startDate != null && endDate != null )
        {
            params.setStartDate( startDate );
            params.setEndDate( endDate );
        }

        if ( organisationUnits != null )
        {
            params.getOrganisationUnits().addAll( identifiableObjectManager.getByCode( OrganisationUnit.class, organisationUnits ) );
        }

        params.setIncludeChildren( includeChildren );
        params.setIncludeDeleted( includeDeleted );
        params.setLastUpdated( lastUpdated );
        params.setLimit( limit );
        params.setOutputIdSchemes( outputIdSchemes );

        return params;
    }

    @Override
    public void writeDataValueSet( DataExportParams params, OutputStream out )
        throws AdxException
    {
        dataValueSetService.decideAccess( params );
        dataValueSetService.validate( params );

        XMLWriter adxWriter = XMLFactory.getXMLWriter( out );

        adxWriter.openElement( AdxDataService.ROOT );
        adxWriter.writeAttribute( "xmlns", AdxDataService.NAMESPACE );

        for ( DataSet dataSet : params.getDataSets() )
        {
            AdxDataSetMetadata metadata = new AdxDataSetMetadata( dataSet );

            DataElementCategoryCombo categoryCombo = dataSet.getCategoryCombo();

            for ( DataElementCategoryOptionCombo aoc : categoryCombo.getOptionCombos() )
            {
                Map<String, String> attributeDimensions = metadata.getExplodedCategoryAttributes(aoc.getId());

                for ( OrganisationUnit orgUnit : params.getOrganisationUnits() )
                {
                    for ( Period period : params.getPeriods() )
                    {
                        adxWriter.openElement( AdxDataService.GROUP );
                        adxWriter.writeAttribute( AdxDataService.DATASET, dataSet.getCode() );
                        adxWriter.writeAttribute( AdxDataService.PERIOD, AdxPeriod.serialize( period ) );
                        adxWriter.writeAttribute( AdxDataService.ORGUNIT, orgUnit.getCode() );

                        for ( String attribute : attributeDimensions.keySet() )
                        {
                            adxWriter.writeAttribute( attribute, attributeDimensions.get( attribute ) );
                        }

                        for ( DataValue dv : dataValueService.getDataValues( orgUnit, period, dataSet.getDataElements(), aoc ) )
                        {
                            adxWriter.openElement( AdxDataService.DATAVALUE );

                            adxWriter.writeAttribute( AdxDataService.DATAELEMENT, dv.getDataElement().getCode() );

                            DataElementCategoryOptionCombo coc = dv.getCategoryOptionCombo();

                            Map<String, String> categoryDimensions = metadata.getExplodedCategoryAttributes(coc.getId());

                            for ( String attribute : categoryDimensions.keySet() )
                            {
                                adxWriter.writeAttribute( attribute, categoryDimensions.get( attribute ) );
                            }

                            if ( dv.getDataElement().getValueType().isNumeric() )
                            {
                                adxWriter.writeAttribute( AdxDataService.VALUE, dv.getValue() );
                            }
                            else
                            {
                                adxWriter.writeAttribute( AdxDataService.VALUE, "0" );
                                adxWriter.openElement( AdxDataService.ANNOTATION );
                                adxWriter.writeCharacters( dv.getValue() );
                                adxWriter.closeElement(); // ANNOTATION
                            }
                            adxWriter.closeElement(); // DATAVALUE
                        }
                        adxWriter.closeElement(); // GROUP
                    }
                }
            }
        }

        adxWriter.closeElement(); // ADX

        adxWriter.closeWriter();
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSet( InputStream in, ImportOptions importOptions, TaskId id )
    {
        try
        {
            in = StreamUtils.wrapAndCheckCompressionFormat( in );
            return saveDataValueSetInternal( in, importOptions, id );
        }
        catch ( IOException ex )
        {
            log.warn( "Import failed: " + DebugUtils.getStackTrace( ex ) );
            return new ImportSummary( ImportStatus.ERROR, "ADX import failed" );
        }
    }

    private ImportSummary saveDataValueSetInternal( InputStream in, ImportOptions importOptions, TaskId id )
    {
        notifier.clear( id ).notify( id, "ADX parsing process started" );

        ImportOptions adxImportOptions = ObjectUtils.firstNonNull( importOptions, ImportOptions.getDefaultImportOptions() )
            .instance().setNotificationLevel( NotificationLevel.OFF );

        // Get import options
        IdScheme dataSetIdScheme = importOptions.getIdSchemes().getDataSetIdScheme();
        IdScheme dataElementIdScheme = importOptions.getIdSchemes().getDataElementIdScheme();

        // Create meta-data maps
        CachingMap<String, DataSet> dataSetMap = new CachingMap<>();
        CachingMap<String, DataElement> dataElementMap = new CachingMap<>();

        // Get meta-data maps
        IdentifiableObjectCallable<DataSet> dataSetCallable = new IdentifiableObjectCallable<>(
            identifiableObjectManager, DataSet.class, dataSetIdScheme, null );
        IdentifiableObjectCallable<DataElement> dataElementCallable = new IdentifiableObjectCallable<>(
            identifiableObjectManager, DataElement.class, dataElementIdScheme, null );

        // Heat cache
        if ( importOptions.isPreheatCacheDefaultFalse() )
        {
            dataSetMap.load( identifiableObjectManager.getAll( DataSet.class ), o -> o.getPropertyValue( dataSetIdScheme ) );
            dataElementMap.load( identifiableObjectManager.getAll( DataElement.class ), o -> o.getPropertyValue( dataElementIdScheme ) );
        }

        XMLReader adxReader = XMLFactory.getXMLReader( in );

        ImportSummary importSummary;

        adxReader.moveToStartElement( AdxDataService.ROOT, AdxDataService.NAMESPACE );

        ExecutorService executor = Executors.newSingleThreadExecutor();

        // For Async runs, give the DXF import a different notification task ID so it doesn't conflict with notifications from this level.
        TaskId dxfTaskId = ( id == null ) ? null : new TaskId( TaskCategory.DATAVALUE_IMPORT_INTERNAL, id.getUser() );

        int groupCount = 0;

        try ( PipedOutputStream pipeOut = new PipedOutputStream() )
        {
            Future<ImportSummary> futureImportSummary = executor.submit( new AdxPipedImporter(
                dataValueSetService, adxImportOptions, dxfTaskId, pipeOut, sessionFactory ) );
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter dxfWriter = factory.createXMLStreamWriter( pipeOut );

            List<ImportConflict> adxConflicts = new LinkedList<>();

            dxfWriter.writeStartDocument( "1.0" );
            dxfWriter.writeStartElement( "dataValueSet" );
            dxfWriter.writeDefaultNamespace( "http://dhis2.org/schema/dxf/2.0" );

            notifier.notify( id, "Starting to import ADX data groups." );

            while ( adxReader.moveToStartElement( AdxDataService.GROUP, AdxDataService.NAMESPACE ) )
            {
                notifier.update( id, "Importing ADX data group: " + groupCount );

                // note this returns conflicts which are detected at ADX level
                adxConflicts.addAll( parseAdxGroupToDxf( adxReader, dxfWriter, adxImportOptions,
                    dataSetMap, dataSetCallable, dataElementMap, dataElementCallable) );
                groupCount++;
            }

            dxfWriter.writeEndElement(); // end dataValueSet
            dxfWriter.writeEndDocument();

            pipeOut.flush();

            importSummary = futureImportSummary.get( TOTAL_MINUTES_TO_WAIT, TimeUnit.MINUTES );
            importSummary.getConflicts().addAll( adxConflicts );
            importSummary.getImportCount().incrementIgnored( adxConflicts.size() );
        }
        catch ( AdxException ex )
        {
            importSummary = new ImportSummary();
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setDescription( "Data set import failed within group number: " + groupCount );
            importSummary.getConflicts().add( ex.getImportConflict() );
            notifier.update( id, NotificationLevel.ERROR, "ADX data import done", true );
            log.warn( "Import failed: " + DebugUtils.getStackTrace( ex ) );
        }
        catch ( IOException | XMLStreamException | InterruptedException | ExecutionException | TimeoutException ex )
        {
            importSummary = new ImportSummary();
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setDescription( "Data set import failed within group number: " + groupCount );
            notifier.update( id, NotificationLevel.ERROR, "ADX data import done", true );
            log.warn( "Import failed: " + DebugUtils.getStackTrace( ex ) );
        }

        executor.shutdown();

        notifier.update( id, INFO, "ADX data import done", true ).addTaskSummary( id, importSummary );

        ImportCount c = importSummary.getImportCount();
        log.info( "ADX data import done, imported: " + c.getImported() + ", updated: " + c.getUpdated() + ", deleted: " + c.getDeleted() + ", ignored: " + c.getIgnored() );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    private List<ImportConflict> parseAdxGroupToDxf( XMLReader adxReader, XMLStreamWriter dxfWriter, ImportOptions importOptions,
        CachingMap<String, DataSet> dataSetMap, IdentifiableObjectCallable<DataSet> dataSetCallable,
        CachingMap<String, DataElement> dataElementMap, IdentifiableObjectCallable<DataElement> dataElementCallable )
        throws XMLStreamException, AdxException
    {
        List<ImportConflict> adxConflicts = new LinkedList<>();

        IdScheme categoryOptionComboIdScheme = importOptions.getIdSchemes().getCategoryOptionComboIdScheme();

        Map<String, String> groupAttributes = adxReader.readAttributes();

        if ( !groupAttributes.containsKey( AdxDataService.PERIOD ) )
        {
            throw new AdxException( AdxDataService.PERIOD + " attribute is required on 'group'" );
        }

        if ( !groupAttributes.containsKey( AdxDataService.ORGUNIT ) )
        {
            throw new AdxException( AdxDataService.ORGUNIT + " attribute is required on 'group'" );
        }

        // translate ADX period to DXF
        String periodStr = groupAttributes.get( AdxDataService.PERIOD );
        groupAttributes.remove( AdxDataService.PERIOD );
        Period period = AdxPeriod.parse( periodStr );
        groupAttributes.put( AdxDataService.PERIOD, period.getIsoDate() );

        // process ADX group attributes
        if ( !groupAttributes.containsKey( AdxDataService.ATTOPTCOMBO )
            && groupAttributes.containsKey( AdxDataService.DATASET ) )
        {
            log.debug( "No attribute option combo present, check data set for attribute category combo" );

            String dataSetStr = trimToNull( groupAttributes.get( AdxDataService.DATASET ) );
            final DataSet dataSet = dataSetMap.get( dataSetStr, dataSetCallable.setId( dataSetStr ) );

            if ( dataSet == null )
            {
                throw new AdxException( "No data set matching " + dataSetCallable.getIdScheme().name().toLowerCase()
                    + " '" + groupAttributes.get( AdxDataService.DATASET ) + "'" );
            }

            groupAttributes.put( AdxDataService.DATASET, dataSet.getUid() );
            DataElementCategoryCombo attributeCombo = dataSet.getCategoryCombo();
            convertAttributesToDxf( groupAttributes, AdxDataService.ATTOPTCOMBO, attributeCombo, categoryOptionComboIdScheme );
        }

        // process the dataValues
        while ( adxReader.moveToStartElement( AdxDataService.DATAVALUE, AdxDataService.GROUP ) )
        {
            try
            {
                parseADXDataValueToDxf( adxReader, dxfWriter, groupAttributes, importOptions,
                    dataElementMap, dataElementCallable );
            }
            catch ( AdxException ex )
            {
                adxConflicts.add( ex.getImportConflict() );

                log.info( "ADX data value conflict: " + ex.getImportConflict() );
            }
        }

        return adxConflicts;
    }

    private void parseADXDataValueToDxf( XMLReader adxReader, XMLStreamWriter dxfWriter,
        Map<String, String> groupAttributes, ImportOptions importOptions,
        CachingMap<String, DataElement> dataElementMap, IdentifiableObjectCallable<DataElement> dataElementCallable )
        throws XMLStreamException, AdxException
    {
        Map<String, String> dvAttributes = adxReader.readAttributes();

        log.debug( "Processing data value: " + dvAttributes );

        if ( !dvAttributes.containsKey( AdxDataService.DATAELEMENT ) )
        {
            throw new AdxException( AdxDataService.DATAELEMENT + " attribute is required on 'dataValue'" );
        }

        if ( !dvAttributes.containsKey( AdxDataService.VALUE ) )
        {
            throw new AdxException( AdxDataService.VALUE + " attribute is required on 'dataValue'" );
        }

        IdScheme categoryOptionComboIdScheme = importOptions.getIdSchemes().getCategoryOptionComboIdScheme();

        String dataElementStr = trimToNull( dvAttributes.get( AdxDataService.DATAELEMENT ) );
        final DataElement dataElement = dataElementMap.get( dataElementStr, dataElementCallable.setId( dataElementStr ) );

        if ( dataElement == null )
        {
            throw new AdxException( "No data element matching " + dataElementCallable.getIdScheme().name().toLowerCase()
                + " '" + dataElementStr + "'" );
        }

        // process ADX data value attributes
        if ( !dvAttributes.containsKey( AdxDataService.CATOPTCOMBO ) )
        {
            log.debug( "No category option combo present" );

            //TODO expand to allow for category combos part of DataSetElements.

            DataElementCategoryCombo categoryCombo = dataElement.getDataElementCategoryCombo();

            convertAttributesToDxf( dvAttributes, AdxDataService.CATOPTCOMBO, categoryCombo, categoryOptionComboIdScheme );
        }

        // if data element type is not numeric we need to pick out the
        // 'annotation' element
        if ( !dataElement.getValueType().isNumeric() )
        {
            adxReader.moveToStartElement( AdxDataService.ANNOTATION, AdxDataService.DATAVALUE );

            if ( adxReader.isStartElement( AdxDataService.ANNOTATION ) )
            {
                String textValue = adxReader.getElementValue();
                dvAttributes.put( AdxDataService.VALUE, textValue );
            }
            else
            {
                throw new AdxException( dvAttributes.get( AdxDataService.DATAELEMENT ),
                    "DataElement expects text annotation" );
            }
        }

        log.debug( "Processing data value as DXF: " + dvAttributes );

        dxfWriter.writeStartElement( "dataValue" );

        // write the group attributes through to DXF stream
        for ( String attribute : groupAttributes.keySet() )
        {
            dxfWriter.writeAttribute( attribute, groupAttributes.get( attribute ) );
        }

        // pass through the remaining attributes to DXF
        for ( String attribute : dvAttributes.keySet() )
        {
            dxfWriter.writeAttribute( attribute, dvAttributes.get( attribute ) );
        }

        dxfWriter.writeEndElement(); // dataValue
    }

    private Map<String, DataElementCategory> getCodeCategoryMap( DataElementCategoryCombo categoryCombo )
        throws AdxException
    {
        Map<String, DataElementCategory> categoryMap = new HashMap<>();

        List<DataElementCategory> categories = categoryCombo.getCategories();

        for ( DataElementCategory category : categories )
        {
            String categoryCode = category.getCode();

            if ( categoryCode == null || !XMLChar.isValidName( categoryCode ) )
            {
                throw new AdxException(
                    "Category code for " + category.getName() + " is missing or invalid: " + categoryCode );
            }

            categoryMap.put( category.getCode(), category );
        }

        return categoryMap;
    }

    private DataElementCategoryOptionCombo getCatOptComboFromAttributes( Map<String, String> attributes,
        DataElementCategoryCombo catcombo, IdentifiableProperty scheme )
        throws AdxException
    {
        CategoryComboMap catcomboMap;

        try
        {
            catcomboMap = new CategoryComboMap( catcombo, scheme );
        }
        catch ( CategoryComboMapException ex )
        {
            log.info( "Failed to create category combo map from: " + catcombo );
            throw new AdxException( ex.getMessage() );
        }

        String compositeIdentifier = StringUtils.EMPTY;

        for ( DataElementCategory category : catcomboMap.getCategories() )
        {
            String categoryCode = category.getCode();

            if ( categoryCode == null )
            {
                throw new AdxException( "No category matching: " + categoryCode );
            }

            String catAttribute = attributes.get( categoryCode );

            if ( catAttribute == null )
            {
                throw new AdxException( "Missing required attribute from category combo: " + categoryCode );
            }

            compositeIdentifier += "\"" + catAttribute + "\"";
        }

        DataElementCategoryOptionCombo catOptionCombo = catcomboMap.getCategoryOptionCombo( compositeIdentifier );

        if ( catOptionCombo == null )
        {
            throw new AdxException( "Invalid attributes:" + attributes );
        }

        return catOptionCombo;
    }

    private void convertAttributesToDxf( Map<String, String> attributes, String optionComboName, DataElementCategoryCombo catCombo,
        IdScheme idScheme )
        throws AdxException
    {
        log.debug( "ADX attributes: " + attributes );

        if ( catCombo.isDefault() )
        {
            return;
        }

        Map<String, DataElementCategory> categoryMap = getCodeCategoryMap( catCombo );

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
                throw new AdxException(
                    "Category combo " + catCombo.getName() + " must have " + categoryMap.get( category ).getName() );
            }
        }

        DataElementCategoryOptionCombo catOptCombo = getCatOptComboFromAttributes( attributeOptions, catCombo, idScheme.getIdentifiableProperty() );

        attributes.put( optionComboName, catOptCombo.getPropertyValue( idScheme ) );

        log.debug( "DXF attributes: " + attributes );
    }
}
