/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummary;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntryFormService;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntryFormUtil;
import org.hisp.dhis.dxf2.pdfform.PdfFormFontSettings;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author James Chang <jamesbchang@gmail.com>
 */
@OpenApi.Tags( "ui" )
@Controller
@RequestMapping( value = "/pdfForm" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class PdfFormController
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private Notifier notifier;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private PdfDataEntryFormService pdfDataEntryFormService;

    @Autowired
    private ContextUtils contextUtils;

    // --------------------------------------------------------------------------
    // DataSet
    // --------------------------------------------------------------------------

    @GetMapping( "/dataSet/{dataSetUid}" )
    public void getFormPdfDataSet( @PathVariable String dataSetUid, HttpServletRequest request,
        HttpServletResponse response, OutputStream out )
        throws Exception
    {
        Document document = new Document();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance( document, baos );

        PdfFormFontSettings pdfFormFontSettings = new PdfFormFontSettings();

        PdfDataEntryFormUtil.setDefaultFooterOnDocument( document, request.getServerName(),
            pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_FOOTER ) );

        pdfDataEntryFormService.generatePDFDataEntryForm( document, writer, dataSetUid,
            PdfDataEntryFormUtil.DATATYPE_DATASET,
            PdfDataEntryFormUtil.getDefaultPageSize( PdfDataEntryFormUtil.DATATYPE_DATASET ),
            pdfFormFontSettings, i18nManager.getI18nFormat() );

        String fileName = dataSetService.getDataSet( dataSetUid ).getName() + " " +
            DateUtils.getMediumDateString() + ".pdf";

        contextUtils.configureResponse( response,
            ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.NO_CACHE, fileName, true );
        response.setContentLength( baos.size() );

        baos.writeTo( out );
    }

    @PostMapping( "/dataSet" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @ResponseBody
    public WebMessage sendFormPdfDataSet( HttpServletRequest request )
        throws Exception
    {
        JobConfiguration jobId = new JobConfiguration( "inMemoryDataValueImport",
            JobType.DATAVALUE_IMPORT, currentUserService.getCurrentUser().getUid(), true );

        notifier.clear( jobId );

        InputStream in = request.getInputStream();

        in = StreamUtils.wrapAndCheckCompressionFormat( in );

        ImportSummary summary = dataValueSetService.importDataValueSetPdf( in, ImportOptions.getDefaultImportOptions(),
            jobId );

        return importSummary( summary );
    }
}
