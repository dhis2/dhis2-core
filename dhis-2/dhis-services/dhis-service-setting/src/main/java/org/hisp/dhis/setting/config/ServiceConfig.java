/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.setting.config;

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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Luciano Fiandesio
 */
@Configuration("settingServiceConfig")
public class ServiceConfig {
  @Autowired private SystemSettingStore systemSettingStore;

  @Autowired
  @Qualifier("tripleDesStringEncryptor")
  private PBEStringEncryptor pbeStringEncryptor;

  @Bean("org.hisp.dhis.setting.SystemSettingManager")
  public DefaultSystemSettingManager defaultSystemSettingManager(
      CacheProvider cacheProvider, TransactionTemplate transactionTemplate) {

    List<String> flags = new ArrayList<>();
    flags.add("afghanistan");
    flags.add("africare");
    flags.add("akros");
    flags.add("aland_islands");
    flags.add("albania");
    flags.add("algeria");
    flags.add("american_samoa");
    flags.add("andorra");
    flags.add("angola");
    flags.add("anguilla");
    flags.add("antarctica");
    flags.add("antigua_and_barbuda");
    flags.add("argentina");
    flags.add("armenia");
    flags.add("aruba");
    flags.add("australia");
    flags.add("austria");
    flags.add("azerbaijan");
    flags.add("bahamas");
    flags.add("bahrain");
    flags.add("bangladesh");
    flags.add("barbados");
    flags.add("belarus");
    flags.add("belgium");
    flags.add("belize");
    flags.add("benin");
    flags.add("bermuda");
    flags.add("bhutan");
    flags.add("bolivia");
    flags.add("bosnia_and_herzegovina");
    flags.add("botswana");
    flags.add("bouvet_island");
    flags.add("brazil");
    flags.add("british_indian_ocean_territory");
    flags.add("british_virgin_islands");
    flags.add("brunei");
    flags.add("bulgaria");
    flags.add("burkina_faso");
    flags.add("burkina_faso_coat_of_arms");
    flags.add("burundi");
    flags.add("cambodia");
    flags.add("cameroon");
    flags.add("canada");
    flags.add("cape_verde");
    flags.add("caribbean_netherlands");
    flags.add("cayman_islands");
    flags.add("central_african_republic");
    flags.add("chad");
    flags.add("chile");
    flags.add("china");
    flags.add("christmas_island");
    flags.add("cidrz");
    flags.add("cocos_keeling_islands");
    flags.add("colombia");
    flags.add("comoros");
    flags.add("congo_brazzaville");
    flags.add("congo_kinshasa");
    flags.add("cook_islands");
    flags.add("cordaid");
    flags.add("costa_rica");
    flags.add("cote_d_ivoire_ivory_coast");
    flags.add("croatia");
    flags.add("cuba");
    flags.add("curacao");
    flags.add("cyprus");
    flags.add("czechia");
    flags.add("demoland");
    flags.add("denmark");
    flags.add("denmark");
    flags.add("djibouti");
    flags.add("dominica");
    flags.add("dominican_republic");
    flags.add("dr_congo");
    flags.add("ecowas");
    flags.add("ecuador");
    flags.add("east_africa_community");
    flags.add("egypt");
    flags.add("el_salvador");
    flags.add("engender_health");
    flags.add("england");
    flags.add("eritrea");
    flags.add("estonia");
    flags.add("eswatini_swaziland");
    flags.add("ethiopia");
    flags.add("equatorial_guinea");
    flags.add("european_union");
    flags.add("falkland_islands");
    flags.add("faroe_islands");
    flags.add("fhi360");
    flags.add("fiji");
    flags.add("finland");
    flags.add("forut");
    flags.add("france");
    flags.add("french_guiana");
    flags.add("french_polynesia");
    flags.add("french_southern_and_antarctic_lands");
    flags.add("gabon");
    flags.add("gambia");
    flags.add("georgia");
    flags.add("germany");
    flags.add("ghana");
    flags.add("gibraltar");
    flags.add("global_fund");
    flags.add("greece");
    flags.add("greenland");
    flags.add("grenada");
    flags.add("guadeloupe");
    flags.add("guam");
    flags.add("guatemala");
    flags.add("guernsey");
    flags.add("guinea");
    flags.add("guinea_bissau");
    flags.add("guyana");
    flags.add("haiti");
    flags.add("heard_island_and_mcdonald_islands");
    flags.add("honduras");
    flags.add("hong_kong");
    flags.add("hungary");
    flags.add("icap");
    flags.add("iceland");
    flags.add("ippf");
    flags.add("ima");
    flags.add("india");
    flags.add("indonesia");
    flags.add("irc");
    flags.add("iran");
    flags.add("iraq");
    flags.add("ireland");
    flags.add("isle_of_man");
    flags.add("israel");
    flags.add("italy");
    flags.add("ivory_coast");
    flags.add("jamaica");
    flags.add("japan");
    flags.add("jersey");
    flags.add("jhpiego");
    flags.add("jordan");
    flags.add("kazakhstan");
    flags.add("kenya");
    flags.add("kiribati");
    flags.add("kosovo");
    flags.add("kurdistan");
    flags.add("kuwait");
    flags.add("kyrgyzstan");
    flags.add("laos");
    flags.add("latvia");
    flags.add("lebanon");
    flags.add("lesotho");
    flags.add("liberia");
    flags.add("libya");
    flags.add("liechtenstein");
    flags.add("lithuania");
    flags.add("luxembourg");
    flags.add("macau");
    flags.add("madagascar");
    flags.add("malawi");
    flags.add("malaysia");
    flags.add("malta");
    flags.add("marshall_islands");
    flags.add("martinique");
    flags.add("mauritania");
    flags.add("mauritius");
    flags.add("maldives");
    flags.add("mayotte");
    flags.add("mexico");
    flags.add("micronesia");
    flags.add("moldova");
    flags.add("monaco");
    flags.add("mongolia");
    flags.add("montenegro");
    flags.add("montserrat");
    flags.add("morocco");
    flags.add("mozambique");
    flags.add("myanmar");
    flags.add("mali");
    flags.add("mhrp");
    flags.add("msf");
    flags.add("msh");
    flags.add("msh_white");
    flags.add("msi");
    flags.add("namibia");
    flags.add("nauru");
    flags.add("netherlands");
    flags.add("new_caledonia");
    flags.add("new_zealand");
    flags.add("nicaragua");
    flags.add("nepal");
    flags.add("niger");
    flags.add("nigeria");
    flags.add("niue");
    flags.add("norfolk_island");
    flags.add("north_korea");
    flags.add("north_macedonia");
    flags.add("northern_ireland");
    flags.add("northern_mariana_islands");
    flags.add("norway");
    flags.add("oman");
    flags.add("pakistan");
    flags.add("palau");
    flags.add("palestine");
    flags.add("palladium");
    flags.add("panama");
    flags.add("papua_new_guinea");
    flags.add("pepfar");
    flags.add("paraguay");
    flags.add("pathfinder");
    flags.add("philippines");
    flags.add("pitcairn_islands");
    flags.add("planned_parenthood");
    flags.add("peru");
    flags.add("poland");
    flags.add("portugal");
    flags.add("psi");
    flags.add("puerto_rico");
    flags.add("puntland");
    flags.add("qatar");
    flags.add("republic_of_the_congo");
    flags.add("reunion");
    flags.add("romania");
    flags.add("russia");
    flags.add("rwanda");
    flags.add("saint_barthelemy");
    flags.add("saint_helena_ascension_and_tristan_da_cunha");
    flags.add("saint_kitts_and_nevis");
    flags.add("saint_lucia");
    flags.add("saint_martin");
    flags.add("saint_pierre_and_miquelon");
    flags.add("saint_vincent_and_the_grenadines");
    flags.add("samoa");
    flags.add("san_marino");
    flags.add("sao_tome_and_principe");
    flags.add("saudi_arabia");
    flags.add("save_the_children");
    flags.add("scotland");
    flags.add("senegal");
    flags.add("serbia");
    flags.add("seychelles");
    flags.add("sierra_leone");
    flags.add("sierra_leone_coat_of_arms");
    flags.add("singapore");
    flags.add("sint_maarten");
    flags.add("slovakia");
    flags.add("slovenia");
    flags.add("solomon_islands");
    flags.add("somalia");
    flags.add("somaliland");
    flags.add("south_africa");
    flags.add("south_africa_department_of_health");
    flags.add("south_georgia");
    flags.add("south_korea");
    flags.add("south_sudan");
    flags.add("spain");
    flags.add("sri_lanka");
    flags.add("sudan");
    flags.add("suriname");
    flags.add("svalbard_and_jan_mayen");
    flags.add("swaziland");
    flags.add("sweden");
    flags.add("switzerland");
    flags.add("syria");
    flags.add("taiwan");
    flags.add("tajikistan");
    flags.add("tanzania");
    flags.add("thailand");
    flags.add("timor_leste");
    flags.add("republic_of_trinidad_and_tobago");
    flags.add("togo");
    flags.add("tokelau");
    flags.add("tonga");
    flags.add("trinidad_and_tobago");
    flags.add("tunisia");
    flags.add("turkey");
    flags.add("turkmenistan");
    flags.add("turks_and_caicos_islands");
    flags.add("tuvalu");
    flags.add("uganda");
    flags.add("ukraine");
    flags.add("united_arab_emirates");
    flags.add("united_kingdom");
    flags.add("united_nations");
    flags.add("united_states");
    flags.add("united_states_minor_outlying_islands");
    flags.add("united_states_virgin_islands");
    flags.add("uruguay");
    flags.add("usaid");
    flags.add("uzbekistan");
    flags.add("vatican_city_holy_see");
    flags.add("venezuela");
    flags.add("vietnam");
    flags.add("vanuatu");
    flags.add("wales");
    flags.add("wallis_and_futuna");
    flags.add("western_sahara");
    flags.add("yemen");
    flags.add("zambia");
    flags.add("zanzibar");
    flags.add("zimbabwe");
    flags.add("who");

    return new DefaultSystemSettingManager(
        systemSettingStore, pbeStringEncryptor, cacheProvider, flags, transactionTemplate);
  }
}
