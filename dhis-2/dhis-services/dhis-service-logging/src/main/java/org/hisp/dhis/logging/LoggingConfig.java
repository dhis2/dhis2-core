package org.hisp.dhis.logging;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "logging", namespace = DxfNamespaces.DXF_2_0 )
public class LoggingConfig
{
    private final LogLevel level;
    private final LogFormat format;
    private final boolean consoleEnabled;
    private final LogLevel consoleLevel;
    private final LogFormat consoleFormat;
    private final boolean fileEnabled;
    private final String fileName;
    private final LogLevel fileLevel;
    private final LogFormat fileFormat;

    public LoggingConfig( LogLevel level, LogFormat format, boolean consoleEnabled, LogLevel consoleLevel, LogFormat consoleFormat,
        boolean fileEnabled, String fileName, LogLevel fileLevel, LogFormat fileFormat )
    {
        this.level = level;
        this.format = format;
        this.consoleEnabled = consoleEnabled;
        this.consoleLevel = consoleLevel;
        this.consoleFormat = consoleFormat;
        this.fileEnabled = fileEnabled;
        this.fileName = fileName;
        this.fileLevel = fileLevel;
        this.fileFormat = fileFormat;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LogLevel getLevel()
    {
        return level;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LogFormat getFormat()
    {
        return format;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isConsoleEnabled()
    {
        return consoleEnabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LogLevel getConsoleLevel()
    {
        return consoleLevel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LogFormat getConsoleFormat()
    {
        return consoleFormat != null ? consoleFormat : format;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isFileEnabled()
    {
        return fileEnabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFileName()
    {
        return fileName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LogLevel getFileLevel()
    {
        return fileLevel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LogFormat getFileFormat()
    {
        return fileFormat;
    }

    public boolean isTextFormat()
    {
        return LogFormat.TEXT == format;
    }

    public boolean isJsonFormat()
    {
        return LogFormat.JSON == format;
    }
}
