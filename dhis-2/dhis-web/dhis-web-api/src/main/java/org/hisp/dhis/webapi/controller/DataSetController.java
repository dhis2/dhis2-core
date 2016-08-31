package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.common.DisplayDensity;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.metadata.ExportService;
import org.hisp.dhis.dxf2.metadata.MetaData;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.webapi.utils.FormUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.view.ClassPathUriResolver;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.hisp.dhis.webapi.webdomain.form.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ExportService exportService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private PeriodService periodService;

    // -------------------------------------------------------------------------
    // Controller
    // -------------------------------------------------------------------------

    @RequestMapping( produces = "application/dsd+xml" )
    public void getStructureDefinition( @RequestParam Map<String, String> parameters, HttpServletResponse response )
        throws IOException, TransformerException
    {
        WebOptions options = filterMetadataOptions();

        MetaData metaData = exportService.getMetaData( options );

        InputStream input = new ByteArrayInputStream( JacksonUtils.toXmlWithViewAsString( metaData, ExportView.class ).getBytes( "UTF-8" ) );

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
    
    @ResponseStatus( HttpStatus.NO_CONTENT )
    @RequestMapping( value = "/{uid}/version", method = RequestMethod.POST )
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

    @RequestMapping( value = "/{uid}/dataValueSet", method = RequestMethod.GET )
    public @ResponseBody RootNode getDvs( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "orgUnitIdScheme", defaultValue = "ID", required = false ) String orgUnitIdScheme,
        @RequestParam( value = "dataElementIdScheme", defaultValue = "ID", required = false ) String dataElementIdScheme,
        @RequestParam( value = "period", defaultValue = "", required = false ) String period,
        @RequestParam( value = "orgUnit", defaultValue = "", required = false ) List<String> orgUnits,
        @RequestParam( value = "comment", defaultValue = "true", required = false ) boolean comment,
        HttpServletResponse response ) throws IOException, WebMessageException
    {
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
        @RequestParam( required = false ) boolean metaData, HttpServletResponse response ) throws IOException, WebMessageException
    {
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

        Form form = getForm( dataSets, ou, pe, metaData );

        renderService.toJson( response.getOutputStream(), form );
    }

    @RequestMapping( value = "/{uid}/form", method = RequestMethod.GET, produces = { "application/xml", "text/xml" } )
    public void getFormXml(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String orgUnit,
        @RequestParam( value = "pe", required = false ) String period,
        @RequestParam( required = false ) boolean metaData, HttpServletResponse response ) throws IOException, WebMessageException
    {
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

        Form form = getForm( dataSets, ou, pe, metaData );

        renderService.toXml( response.getOutputStream(), form );
    }

    private Form getForm( List<DataSet> dataSets, OrganisationUnit ou, Period pe, boolean metaData )
    {
        DataSet dataSet = dataSets.get( 0 );

        i18nService.internationalise( dataSet );
        i18nService.internationalise( dataSet.getDataElements() );
        i18nService.internationalise( dataSet.getSections() );

        Form form = FormUtils.fromDataSet( dataSets.get( 0 ), metaData );

        if ( ou != null && pe != null )
        {
            i18nService.internationalise( ou );

            List<DataValue> dataValues = dataValueService.getDataValues( ou, pe, dataSets.get( 0 ).getDataElements() );

            FormUtils.fillWithDataValues( form, dataValues );
        }

        return form;
    }

    @RequestMapping( value = { "/{uid}/customDataEntryForm", "/{uid}/form" }, method = { RequestMethod.PUT, RequestMethod.POST }, consumes = "text/html" )
    @PreAuthorize( "hasRole('ALL')" )
    public void updateCustomDataEntryForm( @PathVariable( "uid" ) String uid,
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

    /**
     * Select only the meta-data required to describe form definitions.
     *
     * @return the filtered options.
     */
    private WebOptions filterMetadataOptions()
    {
        WebOptions options = new WebOptions( new HashMap<>() );
        options.setAssumeTrue( false );
        options.addOption( "categoryOptionCombos", "true" );
        options.addOption( "dataElements", "true" );
        options.addOption( "dataSets", "true" );
        return options;
    }
}
