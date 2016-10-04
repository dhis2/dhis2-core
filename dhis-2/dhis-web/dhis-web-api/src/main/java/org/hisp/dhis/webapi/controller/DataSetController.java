package org.hisp.dhis.webapi.controller;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.FormUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.view.ClassPathUriResolver;
import org.hisp.dhis.webapi.webdomain.form.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = DataSetSchemaDescriptor.API_ENDPOINT )
public class DataSetController
    extends AbstractCrudController<DataSet>
{
    public static final String DSD_TRANSFORM = "/templates/metadata2dsd.xsl";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    @Autowired
    private MetadataExportService exportService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private InputUtils inputUtils;

    // -------------------------------------------------------------------------
    // Controller
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    @RequestMapping( produces = "application/dsd+xml" )
    public void getStructureDefinition( @RequestParam Map<String, String> parameters, HttpServletResponse response )
        throws IOException, TransformerException
    {
        MetadataExportParams exportParams = filterMetadataOptions();

        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadataMap = exportService.getMetadata( exportParams );

        Metadata metadata = new Metadata();
        metadata.setDataElements( (List<DataElement>) metadataMap.get( DataElement.class ) );
        metadata.setDataSets( (List<DataSet>) metadataMap.get( DataSet.class ) );
        metadata.setCategoryOptionCombos( (List<DataElementCategoryOptionCombo>) metadataMap.get( DataElementCategoryOptionCombo.class ) );

        InputStream input = new ByteArrayInputStream( DefaultRenderService.getXmlMapper().writeValueAsString( metadata ).getBytes( "UTF-8" ) );

        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setURIResolver( new ClassPathUriResolver() );

        Transformer transformer = tf.newTransformer( new StreamSource( new ClassPathResource( DSD_TRANSFORM ).getInputStream() ) );

        transformer.transform( new StreamSource( input ), new StreamResult( response.getOutputStream() ) );
    }

    @RequestMapping( value = "/{uid}/version", method = RequestMethod.GET )
    public void getVersion( @PathVariable( "uid" ) String uid, @RequestParam Map<String, String> parameters,
        HttpServletResponse response ) throws Exception
    {
        DataSet dataSet = manager.get( DataSet.class, uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + uid ) );
        }

        Map<String, Integer> versionMap = new HashMap<>();
        versionMap.put( "version", dataSet.getVersion() );

        renderService.toJson( response.getOutputStream(), versionMap );
    }

    @RequestMapping( value = "/{uid}/version", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void bumpVersion( @PathVariable( "uid" ) String uid )
        throws Exception
    {
        DataSet dataSet = manager.get( DataSet.class, uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + uid ) );
        }

        dataSet.increaseVersion();

        dataSetService.updateDataSet( dataSet );
    }

    @RequestMapping( value = "/{uid}/categoryCombos", method = RequestMethod.GET )
    public @ResponseBody RootNode getCategoryCombinations( @PathVariable( "uid" ) String uid, HttpServletRequest request,
        TranslateParams translateParams, HttpServletResponse response )
        throws Exception
    {
        setUserContext( translateParams );
        DataSet dataSet = manager.get( DataSet.class, uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + uid ) );
        }

         List<DataElementCategoryCombo> categoryCombos = dataSet.getDataSetElements().stream().
            map( DataSetElement::getResolvedCategoryCombo ).distinct().collect( Collectors.toList() );

        Collections.sort( categoryCombos );

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( fieldFilterService.filter( DataElementCategoryCombo.class, categoryCombos, fields ) );

        return rootNode;
    }

    @RequestMapping( value = "/{uid}/dataValueSet", method = RequestMethod.GET )
    public @ResponseBody RootNode getDvs( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "orgUnitIdScheme", defaultValue = "ID", required = false ) String orgUnitIdScheme,
        @RequestParam( value = "dataElementIdScheme", defaultValue = "ID", required = false ) String dataElementIdScheme,
        @RequestParam( value = "period", defaultValue = "", required = false ) String period,
        @RequestParam( value = "orgUnit", defaultValue = "", required = false ) List<String> orgUnits,
        @RequestParam( value = "comment", defaultValue = "true", required = false ) boolean comment,
        TranslateParams translateParams, HttpServletResponse response ) throws IOException, WebMessageException
    {
        setUserContext( translateParams );
        List<DataSet> dataSets = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataSets.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + uid ) );
        }

        Period pe = periodService.getPeriod( period );

        return dataValueSetService.getDataValueSetTemplate( dataSets.get( 0 ), pe, orgUnits, comment, orgUnitIdScheme, dataElementIdScheme );
    }

    @RequestMapping( value = "/{uid}/form", method = RequestMethod.GET, produces = "application/json" )
    public void getFormJson(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnit,
        @RequestParam( value = "pe", required = false ) String period,
        @RequestParam( value = "categoryOptions", required = false ) String categoryOptions,
        @RequestParam( required = false ) boolean metaData,
        TranslateParams translateParams, HttpServletResponse response ) throws IOException, WebMessageException
    {
        setUserContext( translateParams );
        List<DataSet> dataSets = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataSets.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + uid ) );
        }

        OrganisationUnit ou = manager.get( OrganisationUnit.class, orgUnit );

        if ( ou == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Organisation unit does not exist: " + orgUnit ) );
        }

        Period pe = PeriodType.getPeriodFromIsoString( period );

        Form form = getForm( dataSets, ou, pe, categoryOptions, metaData );

        renderService.toJson( response.getOutputStream(), form );
    }

    @RequestMapping( value = "/{uid}/form", method = RequestMethod.GET, produces = { "application/xml", "text/xml" } )
    public void getFormXml(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnit,
        @RequestParam( value = "pe", required = false ) String period,
        @RequestParam( value = "catOpts", required = false ) String categoryOptions,
        @RequestParam( required = false ) boolean metaData,
        TranslateParams translateParams, HttpServletResponse response ) throws IOException, WebMessageException
    {
        setUserContext( translateParams );
        List<DataSet> dataSets = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataSets.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + uid ) );
        }

        OrganisationUnit ou = manager.get( OrganisationUnit.class, orgUnit );

        if ( ou == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Organisation unit does not exist: " + orgUnit ) );
        }

        Period pe = PeriodType.getPeriodFromIsoString( period );

        Form form = getForm( dataSets, ou, pe, categoryOptions, metaData );

        renderService.toXml( response.getOutputStream(), form );
    }

    private Form getForm( List<DataSet> dataSets, OrganisationUnit ou, Period pe, String categoryOptions, boolean metaData ) throws IOException
    {
        DataSet dataSet = dataSets.get( 0 );

        Form form = FormUtils.fromDataSet( dataSets.get( 0 ), metaData, null );


        Set<String> options = null;

        if ( StringUtils.isNotEmpty( categoryOptions ) && categoryOptions.startsWith( "[" ) && categoryOptions.endsWith( "]" ) )
        {
            String[] split = categoryOptions.substring( 1, categoryOptions.length() - 1 ).split( "," );

            options = new HashSet<>( Lists.newArrayList( split ) );
        }

        if ( ou != null && pe != null )
        {
            List<DataValue> dataValues;

            if ( options != null && !options.isEmpty() )
            {
                DataElementCategoryOptionCombo attrOptionCombo = inputUtils.getAttributeOptionCombo( dataSet.getCategoryCombo(), options, IdScheme.UID );
                dataValues = dataValueService.getDataValues( ou, pe, dataSets.get( 0 ).getDataElements(), attrOptionCombo );
            }
            else
            {
                dataValues = dataValueService.getDataValues( dataSets.get( 0 ).getDataElements(), Sets.newHashSet( pe ), Sets.newHashSet( ou ) );
            }

            FormUtils.fillWithDataValues( form, dataValues );
        }

        return form;
    }

    @RequestMapping( value = { "/{uid}/customDataEntryForm", "/{uid}/form" }, method = { RequestMethod.PUT, RequestMethod.POST }, consumes = "text/html" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateCustomDataEntryFormHtml( @PathVariable( "uid" ) String uid,
        @RequestBody String formContent,
        HttpServletResponse response ) throws Exception
    {
        DataSet dataSet = dataSetService.getDataSet( uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + uid ) );
        }

        DataEntryForm form = dataSet.getDataEntryForm();

        if ( form == null )
        {
            form = new DataEntryForm( dataSet.getName(), DisplayDensity.NORMAL, formContent );
            dataEntryFormService.addDataEntryForm( form );
            dataSet.setDataEntryForm( form );
        }
        else
        {
            form.setHtmlCode( formContent );
            dataEntryFormService.updateDataEntryForm( form );
        }

        dataSet.increaseVersion();
        dataSetService.updateDataSet( dataSet );
    }

    @RequestMapping( value = "/{uid}/form", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE )
    @ApiVersion( value = ApiVersion.Version.ALL, exclude = ApiVersion.Version.V23 )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateCustomDataEntryFormJson( @PathVariable( "uid" ) String uid, HttpServletRequest request ) throws WebMessageException
    {
        DataSet dataSet = dataSetService.getDataSet( uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + uid ) );
        }

        DataEntryForm form = dataSet.getDataEntryForm();
        DataEntryForm newForm;

        try
        {
            newForm = renderService.fromJson( request.getInputStream(), DataEntryForm.class );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Failed to parse request", e.getMessage() ) );
        }

        if ( form == null )
        {
            if ( !newForm.hasForm() )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Missing required parameter 'htmlCode'" ) );
            }

            newForm.setName( dataSet.getName() );
            dataEntryFormService.addDataEntryForm( newForm );
            dataSet.setDataEntryForm( newForm );
        }
        else
        {
            if ( newForm.getHtmlCode() != null )
            {
                form.setHtmlCode( dataEntryFormService.prepareDataEntryFormForSave( newForm.getHtmlCode() ) );
            }

            if ( newForm.getStyle() != null )
            {
                form.setStyle( newForm.getStyle() );
            }

            dataEntryFormService.updateDataEntryForm( form );
        }

        dataSet.increaseVersion();
        dataSetService.updateDataSet( dataSet );
    }

    @RequestMapping( value = "/{uid}/metadata", method = RequestMethod.GET )
    public @ResponseBody RootNode getDataSetWithDependencies( @PathVariable( "uid" ) String pvUid, HttpServletResponse response )
        throws WebMessageException, IOException
    {
        DataSet dataSet = dataSetService.getDataSet( pvUid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + pvUid ) );
        }

        return exportService.getMetadataWithDependenciesAsNode( dataSet );
    }

    /**
     * Select only the meta-data required to describe form definitions.
     *
     * @return the filtered options.
     */
    private MetadataExportParams filterMetadataOptions()
    {
        MetadataExportParams params = new MetadataExportParams();
        params.addQuery( Query.from( schemaService.getSchema( DataElement.class ) ) );
        params.addQuery( Query.from( schemaService.getSchema( DataSet.class ) ) );
        params.addQuery( Query.from( schemaService.getSchema( DataElementCategoryOptionCombo.class ) ) );

        return params;
    }
}
