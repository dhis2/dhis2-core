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

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.OutputStream;
import java.io.StringReader;

import static org.hisp.dhis.system.util.GeoUtils.replaceUnsafeSvgText;

@Controller
@RequestMapping
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class SvgConversionController
{
    @Autowired
    private ContextUtils contextUtils;

    @RequestMapping( value = "/svg.png", method = RequestMethod.POST, consumes = ContextUtils.CONTENT_TYPE_FORM_ENCODED )
    public void toPng( @RequestParam String svg, @RequestParam( required = false ) String filename, HttpServletResponse response )
        throws Exception
    {
        String name = filename != null ? (CodecUtils.filenameEncode( filename ) + ".png") : "file.png";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.NO_CACHE, name, true );

        convertToPng( svg, response.getOutputStream() );
    }

    @RequestMapping( value = "/svg.pdf", method = RequestMethod.POST, consumes = ContextUtils.CONTENT_TYPE_FORM_ENCODED )
    public void toPdf( @RequestParam String svg, @RequestParam( required = false ) String filename, HttpServletResponse response )
        throws Exception
    {
        String name = filename != null ? (CodecUtils.filenameEncode( filename ) + ".pdf") : "file.pdf";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.NO_CACHE, name, true );

        convertToPdf( svg, response.getOutputStream() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void convertToPng( String svg, OutputStream out )
        throws TranscoderException
    {
        svg = replaceUnsafeSvgText( svg );

        PNGTranscoder transcoder = new PNGTranscoder();

        transcoder.addTranscodingHint( ImageTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE );

        TranscoderInput input = new TranscoderInput( new StringReader( svg ) );

        TranscoderOutput output = new TranscoderOutput( out );

        transcoder.transcode( input, output );
    }

    private void convertToPdf( String svg, OutputStream out )
        throws TranscoderException
    {
        svg = replaceUnsafeSvgText( svg );

        PDFTranscoder transcoder = new PDFTranscoder();

        TranscoderInput input = new TranscoderInput( new StringReader( svg ) );

        TranscoderOutput output = new TranscoderOutput( out );

        transcoder.transcode( input, output );
    }
}
