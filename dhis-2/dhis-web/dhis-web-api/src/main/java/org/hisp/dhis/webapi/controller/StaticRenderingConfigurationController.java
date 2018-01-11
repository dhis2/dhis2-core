package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.configuration.ObjectValueTypeRenderingOption;
import org.hisp.dhis.dxf2.configuration.StaticRenderingConfiguration;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Set;

@Controller
@RequestMapping( value = "/staticConfiguration/" )
@ApiVersion( DhisApiVersion.DEFAULT )
public class StaticRenderingConfigurationController
{

    @RequestMapping( value = "renderingOptions", method = RequestMethod.GET )
    public @ResponseBody Set<ObjectValueTypeRenderingOption> getMapping()
    {
        return StaticRenderingConfiguration.RENDERING_OPTIONS_MAPPING;
    }

}
