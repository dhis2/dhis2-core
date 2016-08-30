package org.hisp.dhis.dxf2.gml;

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

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.impl.io.MalformedByteSequenceException;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.xml.sax.SAXParseException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Import geospatial data from GML documents and merge into OrganisationUnits.
 * <p>
 * The implementation is a pre-processing stage, using the general MetaDataImporter
 * as the import backend.
 * <p>
 * The process of importing GML, in short, entails the following:
 * <ol>
 * <li>Parse the GML payload and transform it into DXF2 format</li>
 * <li>Get the given identifiers (uid, code or name) from the parsed payload and fetch
 * the corresponding entities from the DB</li>
 * <li>Merge the geospatial data given in the input GML into DB entities</li>
 * <li>Serialize the MetaData payload containing the changes into DXF2, avoiding any magic
 * deletion managers, AOP, Hibernate object cache or transaction scope messing with the payload.
 * It is now essentially a perfect copy of the DB contents.</li>
 * <li>Deserialize the DXF2 payload into a MetaData object, which is now completely detached, and
 * feed this object into the MetaData importer.</li>
 * </ol>
 * <p>
 * Any failure during this process will be reported using the {@link Notifier}.
 *
 * @author Halvdan Hoem Grelland
 */
public class DefaultGmlImportService
    implements GmlImportService
{
    private static final Log log = LogFactory.getLog( DefaultGmlImportService.class );

    private static final String GML_TO_DXF_STYLESHEET = "gml/gml2dxf2.xsl";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private RenderService renderService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private MetadataImportService importService;

    @Autowired
    private Notifier notifier;

    // -------------------------------------------------------------------------
    // GmlImportService implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public void importGml( InputStream inputStream, MetadataImportParams importParams )
    {
        if ( !importParams.getImportStrategy().isUpdate() )
        {
            importParams.setImportStrategy( ImportStrategy.UPDATE );
            log.warn( "Changed GML import strategy to update. Only updates are supported." );
        }

        PreProcessingResult preProcessed = preProcessGml( inputStream );

        if ( preProcessed.isSuccess && preProcessed.dxf2MetaData != null )
        {
            Metadata metadata = dxf2ToMetaData( preProcessed.dxf2MetaData );
            importParams.addMetadata( schemaService.getMetadataSchemas(), metadata );
            importService.importMetadata( importParams );
        }
        else
        {
            Throwable throwable = preProcessed.throwable;

            notifier.notify( importParams.getTaskId(), NotificationLevel.ERROR, createNotifierErrorMessage( throwable ), false );
            log.error( "GML import failed: ", throwable );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private PreProcessingResult preProcessGml( InputStream inputStream )
    {
        InputStream dxfStream = null;
        Metadata metadata = null;

        try
        {
            dxfStream = transformGml( inputStream );
            metadata = renderService.fromXml( dxfStream, Metadata.class );
        }
        catch ( IOException | TransformerException e )
        {
            return PreProcessingResult.failure( e );
        }
        finally
        {
            IOUtils.closeQuietly( dxfStream );
        }

        Map<String, OrganisationUnit> uidMap = Maps.newHashMap(), codeMap = Maps.newHashMap(), nameMap = Maps.newHashMap();

        matchAndFilterOnIdentifiers( metadata.getOrganisationUnits(), uidMap, codeMap, nameMap );

        Map<String, OrganisationUnit> persistedUidMap = getMatchingPersistedOrgUnits( uidMap.keySet(), IdentifiableProperty.UID );
        Map<String, OrganisationUnit> persistedCodeMap = getMatchingPersistedOrgUnits( codeMap.keySet(), IdentifiableProperty.CODE );
        Map<String, OrganisationUnit> persistedNameMap = getMatchingPersistedOrgUnits( nameMap.keySet(), IdentifiableProperty.NAME );

        Iterator<OrganisationUnit> persistedIterator = Iterators.concat( persistedUidMap.values().iterator(),
            persistedCodeMap.values().iterator(), persistedNameMap.values().iterator() );

        while ( persistedIterator.hasNext() )
        {
            OrganisationUnit persisted = persistedIterator.next(), imported = null;

            if ( !Strings.isNullOrEmpty( persisted.getUid() ) && uidMap.containsKey( persisted.getUid() ) )
            {
                imported = uidMap.get( persisted.getUid() );
            }
            else if ( !Strings.isNullOrEmpty( persisted.getCode() ) && codeMap.containsKey( persisted.getCode() ) )
            {
                imported = codeMap.get( persisted.getCode() );
            }
            else if ( !Strings.isNullOrEmpty( persisted.getName() ) && nameMap.containsKey( persisted.getName() ) )
            {
                imported = nameMap.get( persisted.getName() );
            }

            if ( imported == null || imported.getCoordinates() == null || imported.getFeatureType() == null )
            {
                continue; // Failed to dereference a persisted entity for this org unit or geo data incomplete/missing, therefore ignore
            }

            mergeNonGeoData( persisted, imported );
        }

        String dxf2MetaData = metaDataToDxf2( metadata );

        if ( dxf2MetaData == null )
        {
            return PreProcessingResult.failure( new Exception( "GML import failed during pre-processing stage." ) );
        }

        return PreProcessingResult.success( dxf2MetaData );
    }

    // Basic holder for the return value of preProcessGml(InputStream)
    private static class PreProcessingResult
    {
        private boolean isSuccess;
        private String dxf2MetaData;
        private Throwable throwable;

        static PreProcessingResult success( String dxf2MetaData )
        {
            PreProcessingResult result = new PreProcessingResult();
            result.isSuccess = true;
            result.dxf2MetaData = dxf2MetaData;

            return result;
        }

        static PreProcessingResult failure( Throwable throwable )
        {
            PreProcessingResult result = new PreProcessingResult();
            result.isSuccess = false;
            result.throwable = throwable;

            return result;
        }
    }

    private InputStream transformGml( InputStream input )
        throws IOException, TransformerException
    {
        StreamSource gml = new StreamSource( input );
        StreamSource xsl = new StreamSource( new ClassPathResource( GML_TO_DXF_STYLESHEET ).getInputStream() );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        TransformerFactory.newInstance().newTransformer( xsl ).transform( gml, new StreamResult( output ) );

        xsl.getInputStream().close();
        gml.getInputStream().close();

        return new ByteArrayInputStream( output.toByteArray() );
    }

    private void matchAndFilterOnIdentifiers( List<OrganisationUnit> sourceList, Map<String, OrganisationUnit> uidMap, Map<String,
        OrganisationUnit> codeMap, Map<String, OrganisationUnit> nameMap )
    {
        for ( OrganisationUnit orgUnit : sourceList ) // Identifier Matching priority: uid, code, name
        {
            // Only matches if UID is actually in DB as an empty UID on input will be replaced by auto-generated value

            if ( !Strings.isNullOrEmpty( orgUnit.getUid() ) && idObjectManager.exists( OrganisationUnit.class, orgUnit.getUid() ) )
            {
                uidMap.put( orgUnit.getUid(), orgUnit );
            }
            else if ( !Strings.isNullOrEmpty( orgUnit.getCode() ) )
            {
                codeMap.put( orgUnit.getCode(), orgUnit );
            }
            else if ( !Strings.isNullOrEmpty( orgUnit.getName() ) )
            {
                nameMap.put( orgUnit.getName(), orgUnit );
            }
        }
    }

    private Map<String, OrganisationUnit> getMatchingPersistedOrgUnits( Collection<String> identifiers, final IdentifiableProperty property )
    {
        List<OrganisationUnit> orgUnits = idObjectManager.getObjects( OrganisationUnit.class, property, identifiers );
        return IdentifiableObjectUtils.getIdMap( orgUnits, IdScheme.from( property ) );
    }

    private void mergeNonGeoData( OrganisationUnit source, OrganisationUnit target )
    {
        String coordinates = target.getCoordinates();
        FeatureType featureType = target.getFeatureType();

        target.mergeWith( source, MergeMode.MERGE );

        target.setCoordinates( coordinates );
        target.setFeatureType( featureType );

        if ( source.getParent() != null )
        {
            OrganisationUnit parent = new OrganisationUnit();
            parent.setUid( source.getParent().getUid() );
            target.setParent( parent );
        }
    }

    private String metaDataToDxf2( Metadata metadata )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String dxf2 = null;

        try
        {
            renderService.toXml( baos, metadata );
            dxf2 = baos.toString();
        }
        catch ( IOException e )
        {
            // Gulp! Intentionally ignored
        }
        finally
        {
            IOUtils.closeQuietly( baos );
        }

        return dxf2;
    }

    private Metadata dxf2ToMetaData( String dxf2Content )
    {
        Metadata metadata;

        try
        {
            metadata = renderService.fromXml( dxf2Content, Metadata.class );
        }
        catch ( IOException e )
        {
            metadata = new Metadata();
        }

        return metadata;
    }

    private String createNotifierErrorMessage( Throwable throwable )
    {
        StringBuilder sb = new StringBuilder( "GML import failed: " );

        Throwable rootThrowable = ExceptionUtils.getRootCause( throwable );

        if ( rootThrowable instanceof SAXParseException )
        {
            SAXParseException e = (SAXParseException) rootThrowable;
            sb.append( e.getMessage() );

            if ( e.getLineNumber() >= 0 )
            {
                sb.append( " On line " ).append( e.getLineNumber() );

                if ( e.getColumnNumber() >= 0 )
                {
                    sb.append( " column " ).append( e.getColumnNumber() );
                }
            }
        }
        else if ( rootThrowable instanceof MalformedByteSequenceException )
        {
            sb.append( "Malformed GML file." );
        }
        else
        {
            sb.append( rootThrowable.getMessage() );
        }

        if ( sb.charAt( sb.length() - 1 ) != '.' )
        {
            sb.append( '.' );
        }

        return HtmlUtils.htmlEscape( sb.toString() );
    }
}
