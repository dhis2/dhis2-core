/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.association.jdbc;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.association.CategoryOptionOrganisationUnitAssociationsQueryBuilder;
import org.hisp.dhis.association.DataSetOrganisationUnitAssociationsQueryBuilder;
import org.hisp.dhis.association.ProgramOrganisationUnitAssociationsQueryBuilder;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class JdbcOrgUnitAssociationStoreConfig {

  @Bean
  public JdbcOrgUnitAssociationsStore jdbcProgramOrgUnitAssociationsStore(
      JdbcTemplate jdbcTemplate, CacheProvider cacheProvider, UserService userService) {
    return new JdbcOrgUnitAssociationsStore(
        jdbcTemplate,
        new ProgramOrganisationUnitAssociationsQueryBuilder(),
        cacheProvider.createProgramOrgUnitAssociationCache(),
        userService);
  }

  @Bean
  public JdbcOrgUnitAssociationsStore jdbcCategoryOptionOrgUnitAssociationsStore(
      JdbcTemplate jdbcTemplate, CacheProvider cacheProvider, UserService userService) {
    return new JdbcOrgUnitAssociationsStore(
        jdbcTemplate,
        new CategoryOptionOrganisationUnitAssociationsQueryBuilder(),
        cacheProvider.createCatOptOrgUnitAssociationCache(),
        userService);
  }

  @Bean
  public JdbcOrgUnitAssociationsStore jdbcDataSetOrgUnitAssociationsStore(
      JdbcTemplate jdbcTemplate, CacheProvider cacheProvider, UserService userService) {
    return new JdbcOrgUnitAssociationsStore(
        jdbcTemplate,
        new DataSetOrganisationUnitAssociationsQueryBuilder(),
        cacheProvider.createDataSetOrgUnitAssociationCache(),
        userService);
  }
}
