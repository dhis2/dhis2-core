package org.hisp.dhis.system.util;

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

import java.io.OutputStream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;

/**
 * Supports PDF, HMTL and XLS exports.
 * 
 * @author Lars Helge Overland
 */
public class JRExportUtils
{
    public static final String TYPE_XLS = "xls";
    public static final String TYPE_PDF = "pdf";
    
    /**
     * Export the provided JasperPrint the format given by type.
     *
     * @param type the type to export to. XLS, PDF and HTML are supported.
     * @param out the OutputStream to export to.
     * @param jasperPrint the JasperPrint to export.
     * @throws JRException on export failure.
     */
    public static void export( String type, OutputStream out, JasperPrint jasperPrint )
        throws JRException
    {
        if ( TYPE_XLS.equals( type ) )
        {
            SimpleXlsReportConfiguration config = new SimpleXlsReportConfiguration();
            
            config.setDetectCellType( true );
            config.setRemoveEmptySpaceBetweenRows( true );
            config.setRemoveEmptySpaceBetweenRows( true );
            config.setCollapseRowSpan( true );
            config.setWhitePageBackground( false );
            
            JRXlsExporter exporter = new JRXlsExporter();
            exporter.setExporterInput( new SimpleExporterInput( jasperPrint ) );
            exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( out ) );
            exporter.setConfiguration( config );
            exporter.exportReport();
        }
        else if ( TYPE_PDF.equals( type ) )
        {
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput( new SimpleExporterInput( jasperPrint ) );
            exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( out ) );
            exporter.exportReport();
        }
    }
}


