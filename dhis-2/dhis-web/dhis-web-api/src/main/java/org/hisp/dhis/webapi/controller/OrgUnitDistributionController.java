package org.hisp.dhis.webapi.controller;

import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionParams;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionServiceV2;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class OrgUnitDistributionController
{
    private static final String RESOURCE_PATH = "/orgUnitDistribution";

    @Autowired
    private OrgUnitDistributionServiceV2 distributionService;

    @Autowired
    private ContextUtils contextUtils;

    @RequestMapping( value = RESOURCE_PATH, method = RequestMethod.GET, produces = { "application/json" } )
    public @ResponseBody Grid getJson(
        @RequestParam Set<String> ou,
        @RequestParam Set<String> ougs,
        @RequestParam( required = false ) Set<String> filter,
        DhisApiVersion apiVersion,
        HttpServletResponse response ) throws Exception
    {
        OrgUnitDistributionParams params = distributionService.getParams( ougs, ougs );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.RESPECT_SYSTEM_SETTING );

        return distributionService.getOrgUnitDistribution( params );
    }
}
