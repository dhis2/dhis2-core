package org.hisp.dhis.commons.util;

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

/**
 * Utility class with methods for managing file system paths.
 * 
 * @author Torgeir Lorange Ostby
 */
public class PathUtils
{
    private static final char PACKAGE_SEPARATOR = '.';

    private static final char FILE_SEPARATOR = '/';

    /**
     * Converts the fully qualified class name to a UNIX path.
     * @param clazz the class, separated by '.'.
     * @return the UNIX path, separated by '/'.
     */
    public static String getClassPath( Class<?> clazz )
    {
        return clazz.getName().replace( PACKAGE_SEPARATOR, FILE_SEPARATOR );
    }

    /**
     * Converts the fully qualified class name to a UNIX path.
     * @param clazzName the class name, separated by '.'.
     * @return the UNIX path, separated by '/'.
     */
    public static String getClassPath( String clazzName )
    {
        return clazzName.replace( PACKAGE_SEPARATOR, FILE_SEPARATOR );
    }

    /**
     * Gets the parent path of the given UNIX path.
     * @param path the path.
     * @return the parent path.
     */
    public static String getParent( String path )
    {
        int i = path.lastIndexOf( FILE_SEPARATOR );

        if ( i == -1 )
        {
            return null;
        }

        return path.substring( 0, i );
    }

    /**
     * Adds a child to the given UNIX path.
     * @param parent the parent path.
     * @param child the child path to add.
     * @return the path with the child appended.
     */
    public static String addChild( String parent, String child )
    {
        if ( parent.endsWith( String.valueOf( FILE_SEPARATOR ) ) )
        {
            return parent + child;
        }
        else
        {
            return parent + FILE_SEPARATOR + child;
        }
    }
}
