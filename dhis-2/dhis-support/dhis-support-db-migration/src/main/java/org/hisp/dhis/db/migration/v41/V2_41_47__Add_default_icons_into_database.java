/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.migration.v41;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.icon.DefaultIcon;
import org.hisp.dhis.icon.Icon;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zubair Asghar
 */
public class V2_41_47__Add_default_icons_into_database extends BaseJavaMigration {

  private static final Logger log =
      LoggerFactory.getLogger(V2_41_47__Add_default_icons_into_database.class);

  public static final ObjectMapper o = new ObjectMapper();

  public static final String INSERT_DEFAULT_ICONS =
      """
      INSERT INTO public.icon(
      iconkey, description, fileresourceid, keywords, created, lastupdated, createdby, custom)
      VALUES ( ?, ?, null, ?, now(), now(), null, false );
      """;

  @Override
  public void migrate(Context context) throws Exception {

    Map<String, Icon> defaultIconsMap =
        Arrays.stream(DefaultIcon.values())
            .map(this::createIconWithVariants)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Icon::getKey, Function.identity()));

    try (PreparedStatement ps = context.getConnection().prepareStatement(INSERT_DEFAULT_ICONS)) {

      for (Map.Entry<String, Icon> entry : defaultIconsMap.entrySet()) {
        ps.setString(1, entry.getValue().getKey());
        ps.setString(2, entry.getValue().getDescription());

        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(o.writeValueAsString(entry.getValue().getKeywords()));

        ps.setObject(3, jsonObject);

        ps.addBatch();
      }
      ps.executeBatch();

      log.info("Icons migration completed");

    } catch (SQLException e) {
      throw new FlywayException(e);
    }
  }

  private List<Icon> createIconWithVariants(DefaultIcon defaultIcon) {

    return DefaultIcon.VARIANTS.stream()
        .map(
            variant ->
                new Icon(
                    String.format("%s_%s", defaultIcon.getKey(), variant),
                    defaultIcon.getDescription(),
                    defaultIcon.getKeywords(),
                    false,
                    null))
        .toList();
  }
}
