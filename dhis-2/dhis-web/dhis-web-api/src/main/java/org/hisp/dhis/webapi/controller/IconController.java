package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconData;
import org.hisp.dhis.icon.IconService;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Kristian Wærstad
 */
@Controller
@RequestMapping( value = IconSchemaDescriptor.API_ENDPOINT )
public class IconController
{
    private static final String ICON_PATH = "/SVGs";

    @Autowired
    private IconService iconService;

    @Autowired
    private ContextService contextService;

    @RequestMapping( method = RequestMethod.GET )
    public @ResponseBody
    List<IconData> getIcons( @RequestParam( required = false ) Collection<String> keywords )
    {
        Collection<IconData> icons;

        if ( keywords == null )
        {
            icons = iconService.getIcons();
        }
        else
        {
            icons = iconService.getIcons( keywords );
        }

        return icons.stream()
            .map( data -> data.setReference( String.format( "%s%s/%s.%s", contextService.getContextPath(), ICON_PATH, data.getKey(), Icon.SUFFIX ) ) )
            .collect( Collectors.toList() );
    }

    @RequestMapping( value="/keywords", method = RequestMethod.GET )
    public @ResponseBody
    Collection<String> getKeywords()
    {
        return iconService.getKeywords();
    }

    @RequestMapping( value="/{iconKey}", method = RequestMethod.GET )
    public @ResponseBody
    IconData getIcon( @PathVariable String iconKey ) throws WebMessageException
    {
        Optional<IconData> icon = iconService.getIcon( iconKey );

        if ( !icon.isPresent() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( String.format( "Icon not found: '%s", iconKey ) ) );
        }

        return icon.get();
    }
}
