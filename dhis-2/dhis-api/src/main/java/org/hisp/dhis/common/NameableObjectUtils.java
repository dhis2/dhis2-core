package org.hisp.dhis.common;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public class NameableObjectUtils
{
    /**
     * Returns a list of NameableObjects.
     *
     * @param objects the NameableObjects to include in the list.
     * @return a list of NameableObjects.
     */
    public static List<NameableObject> getList( NameableObject... objects )
    {
        List<NameableObject> list = new ArrayList<>();

        if ( objects != null )
        {
            Collections.addAll( list, objects );
        }

        return list;
    }

    /**
     * Returns a list with erasure NameableObject based on the given collection.
     *
     * @param collection the collection.
     * @return a list of NameableObjects.
     */
    public static List<NameableObject> asList( Collection<? extends NameableObject> collection )
    {
        List<NameableObject> list = new ArrayList<>();
        list.addAll( collection );
        return list;
    }

    /**
     * Returns a list of BaseNameableObjects based on the given list of NameableObjects.
     * 
     * @param objects the list of NameableObjects.
     * @return a list of BaseNameableObejcts.
     */
    public static List<NameableObject> getAsNameableObjects( List<? extends NameableObject> objects )
    {
        List<NameableObject> list = new ArrayList<>();
        
        for ( NameableObject object : objects )
        {
            if ( object != null )
            {
                list.add( new BaseNameableObject( object ) );
            }
        }
        
        return list;
    }
    
    /**
     * Returns a mapping between the UID and the nameable objects.
     *
     * @param objects the nameable objects.
     * @return mapping between the UID and the nameable objects.
     */
    public static Map<String, NameableObject> getUidObjectMap( List<? extends NameableObject> objects )
    {
        Map<String, NameableObject> map = new HashMap<>();

        if ( objects != null )
        {
            for ( NameableObject object : objects )
            {
                map.put( object.getUid(), object );
            }
        }

        return map;
    }

    /**
     * Returns a mapping between the UID and the property defined by the given
     * display property.
     *
     * @param objects the objects.
     * @param displayProperty the property to use as value.
     * @return mapping between the uid and the property of the given objects.
     */
    public static Map<String, String> getUidDisplayPropertyMap( Collection<? extends NameableObject> objects, DisplayProperty displayProperty )
    {
        Map<String, String> map = new HashMap<>();

        if ( objects != null )
        {
            for ( NameableObject object : objects )
            {
                map.put( object.getUid(), object.getDisplayProperty( displayProperty ) );
            }
        }

        return map;
    }
        
    /**
     * Returns a copy of the given list. Returns an empty list if the argument is null.
     * 
     * @param objects the objects.
     * @param a list.
     */
    public static <T extends NameableObject> List<T> getCopyNullSafe( List<T> objects )
    {
        return objects != null ? new ArrayList<>( objects ) : new ArrayList<>();
    }
}
