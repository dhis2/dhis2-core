/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller;/*
                                        * Copyright (c) 2004-2021, University of Oslo
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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.metadatacache.MetadataCache;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping( "/temp" )
public class TempController
{

    @Autowired
    MetadataCache cache;

    @GetMapping( "" )
    @ResponseBody
    public String testCache(
        @RequestParam( name = "klass" ) String klass,
        @RequestParam( name = "uid" ) String uid )
    {
        switch ( klass )
        {
        case "DataElement":
            return cache.get( DataElement.class, uid ).toString();
        case "OrganisationUnit":
            return cache.get( OrganisationUnit.class, uid ).toString();
        default:
            return null;
        }
    }

    @GetMapping( "/expire" )
    @ResponseBody
    public String expireCache(
        @RequestParam String klass,
        @RequestParam String uid )
    {
        switch ( klass )
        {
        default:
            return null;
        }
    }

    @GetMapping( "/refresh" )
    @ResponseBody
    public String refreshCache(
        @RequestParam String klass,
        @RequestParam String uid )
    {
        switch ( klass )
        {
        default:
            return null;
        }
    }

    @GetMapping( "/bulk" )
    @ResponseBody
    public String refreshCache()
    {
        // cache.getAll( OrganisationUnit.class, Set.of( "O6uvpzGd5pu",
        // "O6uvpzGd5px", "ImspTQPwCqd", "DiszpKrYNg8" ) );

        return "OK";
    }
}
