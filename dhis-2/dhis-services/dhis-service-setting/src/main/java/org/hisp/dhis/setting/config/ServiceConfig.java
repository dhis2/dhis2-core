package org.hisp.dhis.setting.config;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import java.util.List;

import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.setting.DefaultSystemSettingManager;
import org.hisp.dhis.setting.SystemSettingStore;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Luciano Fiandesio
 */
@Configuration("settingServiceConfig")
public class ServiceConfig
{
    @Autowired
    private SystemSettingStore systemSettingStore;

    @Autowired
    private CacheProvider cacheProvider;

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier( "tripleDesStringEncryptor" )
    private PBEStringEncryptor pbeStringEncryptor;

    @Bean( "org.hisp.dhis.setting.SystemSettingManager" )
    public DefaultSystemSettingManager defaultSystemSettingManager()
    {

        List<String> flags = new ArrayList<>();
        flags.add( "afghanistan" );
        flags.add( "africare" );
        flags.add( "akros" );
        flags.add( "algeria" );
        flags.add( "angola" );
        flags.add( "armenia" );
        flags.add( "bangladesh" );
        flags.add( "benin" );
        flags.add( "bhutan" );
        flags.add( "botswana" );
        flags.add( "burkina_faso" );
        flags.add( "burkina_faso_coat_of_arms" );
        flags.add( "burundi" );
        flags.add( "cambodia" );
        flags.add( "cameroon" );
        flags.add( "cape_verde" );
        flags.add( "chad" );
        flags.add( "china" );
        flags.add( "cidrz" );
        flags.add( "colombia" );
        flags.add( "congo_brazzaville" );
        flags.add( "congo_kinshasa" );
        flags.add( "cordaid" );
        flags.add( "demoland" );
        flags.add( "denmark" );
        flags.add( "ecowas" );
        flags.add( "ecuador" );
        flags.add( "east_africa_community" );
        flags.add( "egypt" );
        flags.add( "engender_health" );
        flags.add( "eritrea" );
        flags.add( "ethiopia" );
        flags.add( "equatorial_guinea" );
        flags.add( "fhi360" );
        flags.add( "forut" );
        flags.add( "gambia" );
        flags.add( "ghana" );
        flags.add( "global_fund" );
        flags.add( "grenada" );
        flags.add( "guatemala" );
        flags.add( "guinea" );
        flags.add( "guinea_bissau" );
        flags.add( "haiti" );
        flags.add( "honduras" );
        flags.add( "icap" );
        flags.add( "ippf" );
        flags.add( "ima" );
        flags.add( "india" );
        flags.add( "indonesia" );
        flags.add( "irc" );
        flags.add( "iran" );
        flags.add( "iraq" );
        flags.add( "ivory_coast" );
        flags.add( "jhpiego" );
        flags.add( "kenya" );
        flags.add( "kiribati" );
        flags.add( "kurdistan" );
        flags.add( "laos" );
        flags.add( "lesotho" );
        flags.add( "liberia" );
        flags.add( "madagascar" );
        flags.add( "malawi" );
        flags.add( "mauritania" );
        flags.add( "mauritius" );
        flags.add( "maldives" );
        flags.add( "mongolia" );
        flags.add( "mozambique" );
        flags.add( "myanmar" );
        flags.add( "mali" );
        flags.add( "mhrp" );
        flags.add( "msf" );
        flags.add( "msh" );
        flags.add( "msh_white" );
        flags.add( "msi" );
        flags.add( "namibia" );
        flags.add( "nicaragua" );
        flags.add( "nepal" );
        flags.add( "niger" );
        flags.add( "nigeria" );
        flags.add( "norway" );
        flags.add( "pakistan" );
        flags.add( "palestine" );
        flags.add( "palladium" );
        flags.add( "pepfar" );
        flags.add( "paraguay" );
        flags.add( "pathfinder" );
        flags.add( "philippines" );
        flags.add( "planned_parenthood" );
        flags.add( "peru" );
        flags.add( "psi" );
        flags.add( "puntland" );
        flags.add( "rwanda" );
        flags.add( "sao_tome_and_principe" );
        flags.add( "save_the_children" );
        flags.add( "senegal" );
        flags.add( "sierra_leone" );
        flags.add( "sierra_leone_coat_of_arms" );
        flags.add( "solomon_islands" );
        flags.add( "somalia" );
        flags.add( "somaliland" );
        flags.add( "south_africa" );
        flags.add( "south_africa_department_of_health" );
        flags.add( "south_sudan" );
        flags.add( "sri_lanka" );
        flags.add( "sudan" );
        flags.add( "swaziland" );
        flags.add( "sweden" );
        flags.add( "tajikistan" );
        flags.add( "tanzania" );
        flags.add( "timor_leste" );
        flags.add( "republic_of_trinidad_and_tobago" );
        flags.add( "togo" );
        flags.add( "tonga" );
        flags.add( "uganda" );
        flags.add( "ukraine" );
        flags.add( "usaid" );
        flags.add( "vietnam" );
        flags.add( "vanuatu" );
        flags.add( "zambia" );
        flags.add( "zanzibar" );
        flags.add( "zimbabwe" );
        flags.add( "who" );

        return new DefaultSystemSettingManager( systemSettingStore, pbeStringEncryptor,
            cacheProvider, environment, flags );
    }
}
