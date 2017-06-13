package org.hisp.dhis.external.location;

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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public interface LocationManager
{
    /**
     * Gets an inputstream from a file relative to the external configuration directory 
     * location, which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set, if the file 
     * does not exists, or cannot be read by the application. The inputstream should
     * be closed by the client code after use.
     * 
     * @param fileName the name of the file to be read.
     */
    InputStream getInputStream( String fileName )
        throws LocationManagerException;
    
    /**
     * Gets an inputstream from a file relative to the external configuration directory 
     * location, which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set, if the file 
     * does not exists, or cannot be read by the application. The inputstream should
     * be closed by the client code after use.
     * 
     * @param fileName the name of the file to be read.
     */
    InputStream getInputStream( String fileName, String... directories )
        throws LocationManagerException;
    
    /**
     * Gets a file relative to the external configuration directory location,
     * which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set, if the file 
     * does not exists, or cannot be read by the application.
     * 
     * @param fileName the name of the file to be read.
     * @param directories the directories in the path relative to the external
     *        configuration directory in descending order.
     */
    File getFileForReading( String fileName )
        throws LocationManagerException;

    /**
     * Gets a file relative to the external configuration directory location,
     * which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set, if the file 
     * does not exists, or cannot be read by the application.
     * 
     * @param fileName the name of the file to be read.
     * @param directories the directories in the path relative to the external
     *        configuration directory in descending order.
     */
    File getFileForReading( String fileName, String... directories )
        throws LocationManagerException;
    
    /**
     * Gets a file relative to the external configuration directory location,
     * which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set. The method tries
     * to construct the directories passed as arguments if they do not already
     * exist, and trows a LocationManagerException if the process was unsuccessful.
     * 
     * @param fileName the name of the file to be written.
     */
    File getFileForWriting( String fileName )
        throws LocationManagerException;

    /**
     * Gets a file relative to the external configuration directory location,
     * which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set. The method tries
     * to construct the directories passed as arguments if they do not already
     * exist, and trows a LocationManagerException if the process was unsuccessful.
     * 
     * @param fileName the name of the file to be written.
     * @param directories the directories in the path relative to the external
     *        configuration directory.
     */
    File getFileForWriting( String fileName, String... directories )
        throws LocationManagerException;
    
    /**
     * Builds the directory structure defined by the given array of directories
     * relative to external configuration directory location. For instance calling
     * this method with "reporting", "excel", "temp" will create the directory
     * <external_config_dir>/reporting/excel/temp.
     * 
     * @param directories The directories to create.
     * @return a File representing the created directory.
     */
    File buildDirectory( String... directories )
        throws LocationManagerException;

    /**
     * Gets an outputstream from a file relative to the external configuration directory 
     * location, which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set. The outputstream
     * should be closed by the client code after use.
     * 
     * @param fileName the name of the file to be written.
     */
    OutputStream getOutputStream( String fileName )
        throws LocationManagerException;
    
    /**
     * Gets an outputstream from a file relative to the external configuration directory 
     * location, which is set through an environment variable. A LocationManagerException 
     * is thrown if the external directory location is not set. The method tries
     * to construct the directories passed as arguments if they do not already
     * exist, and trows a LocationManagerException if the process was unsuccessful. The 
     * outputstream should be closed by the client code after use.
     * 
     * @param fileName the name of the file to be written.
     * @param directories the directories in the path relative to the external
     *        configuration directory.
     */
    OutputStream getOutputStream( String fileName, String... directories )
        throws LocationManagerException;
    
    /**
     * Gets the external configuration directory. A LocationManagerException is 
     * thrown if the external directory location is not set.
     */
    File getExternalDirectory()
        throws LocationManagerException;

    /**
     * Gets the external configuration directory. A LocationManagerException is 
     * thrown if the external directory location is not set.
     */
    String getExternalDirectoryPath()
        throws LocationManagerException;
    
    /**
     * Indicates whether the external configuration directory is set, valid,
     * and writable.
     */
    boolean externalDirectorySet();
    
    /**
     * Gets the name of the environment variable used for defining the external
     * configuration directory.
     */
    String getEnvironmentVariable();
}
