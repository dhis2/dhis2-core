package org.hisp.dhis.fileresource;

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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Deletes any orphaned FileResources. Queries for non-assigned or failed-upload
 * FileResources and deletes them from the database and/or file store.
 *
 * @author Halvdan Hoem Grelland
 */
public class FileResourceCleanUpTask
    implements Runnable
{
    public static final String KEY_TASK = "fileResourceCleanupTask";
    
    private static final Log log = LogFactory.getLog( FileResourceCleanUpTask.class );

    @Autowired
    private FileResourceService fileResourceService;

    @Override
    public void run()
    {
        List<Pair<String, String>> deleted = new ArrayList<>();

        fileResourceService.getOrphanedFileResources()
            .forEach( fr -> {
                deleted.add( ImmutablePair.of( fr.getName(), fr.getUid() ) );
                fileResourceService.deleteFileResource( fr.getUid() );
            } );

        if ( !deleted.isEmpty() )
        {
            log.warn( "Deleted " + deleted.size() + " orphaned FileResources: " + prettyPrint( deleted ) );
        }
    }

    private String prettyPrint( List<Pair<String, String>> list )
    {
        if ( list.isEmpty() )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder( "[ " );

        list.forEach( pair -> sb.append( pair.getLeft() ).append( " , uid: " ).append( pair.getRight() ).append( ", " ) );

        sb.deleteCharAt( sb.lastIndexOf( "," ) ).append( "]" );

        return sb.toString();
    }
}
