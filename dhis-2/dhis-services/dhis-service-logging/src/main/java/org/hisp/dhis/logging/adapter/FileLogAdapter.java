package org.hisp.dhis.logging.adapter;

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

import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.logging.Log;
import org.hisp.dhis.logging.LogAdapter;
import org.hisp.dhis.logging.LoggingConfig;
import org.hisp.dhis.logging.LoggingManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class FileLogAdapter implements LogAdapter
{
    private final LocationManager locationManager;
    private final String logDirectory;

    public FileLogAdapter( LocationManager locationManager )
    {
        this.locationManager = locationManager;

        File externalDirectory = locationManager.getExternalDirectory();
        this.logDirectory = externalDirectory.getAbsolutePath() + "/logs";
    }

    @Override
    public void log( Log log, LoggingConfig config )
    {
        Path path = Paths.get( logDirectory, "dhis2.log" );
        String logText = logFormat( log, config );

        try
        {
            Files.write( path, logText.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND );
        }
        catch ( IOException ignored )
        {
        }
    }

    private String logFormat( Log log, LoggingConfig config )
    {
        switch ( config.getFileFormat() )
        {
            case TEXT:
                return log.toString();
            case JSON:
                return LoggingManager.toJson( log ) + "\n";
            default:
                return LoggingManager.toJson( log ) + "\n";
        }
    }
}
