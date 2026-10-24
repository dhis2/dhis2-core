/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dataexchange.analytics;

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.Api;
import org.hisp.dhis.dataexchange.aggregate.Source;
import org.hisp.dhis.dataexchange.aggregate.SourceRequest;
import org.hisp.dhis.dataexchange.aggregate.Target;
import org.hisp.dhis.dataexchange.aggregate.TargetType;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests how target API secrets of an imported aggregate data exchange are stored. */
@Transactional
class AggregateDataExchangeImportTest extends PostgresIntegrationTestBase {
  private static final String PASSWORD = "Secret-pwd-123";

  private static final String ACCESS_TOKEN = "d2pat_secretToken0123456789abcdef0123";

  @Autowired private MetadataImportService importService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired
  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private PooledPBEStringEncryptor encryptor;

  @Test
  void testImportedTargetApiSecretsAreStoredEncryptedAndDecryptToTheSecret() {
    AggregateDataExchange basicAuth =
        exchange(
            "AdeBasic001",
            new Api().setUrl("https://a.example.org").setUsername("admin").setPassword(PASSWORD));
    AggregateDataExchange tokenAuth =
        exchange(
            "AdeToken001", new Api().setUrl("https://b.example.org").setAccessToken(ACCESS_TOKEN));

    ImportReport report =
        importService.importMetadata(
            new MetadataImportParams()
                .setImportMode(ObjectBundleMode.COMMIT)
                .setImportStrategy(ImportStrategy.CREATE),
            new MetadataObjects().addObjects(List.of(basicAuth, tokenAuth)));
    assertEquals(Status.OK, report.getStatus());
    entityManager.flush();

    String storedPassword = selectTargetApiValue("AdeBasic001", "password");
    assertNotEquals(PASSWORD, storedPassword);
    assertEquals(PASSWORD, encryptor.decrypt(storedPassword));

    String storedToken = selectTargetApiValue("AdeToken001", "accessToken");
    assertNotEquals(ACCESS_TOKEN, storedToken);
    assertEquals(ACCESS_TOKEN, encryptor.decrypt(storedToken));
  }

  private static AggregateDataExchange exchange(String uid, Api api) {
    Source source =
        new Source()
            .setRequests(
                List.of(
                    new SourceRequest()
                        .setName("Request")
                        .setDx(List.of("LrDpG50RAU9"))
                        .setPe(List.of("202201"))
                        .setOu(List.of("G9BuXqtNeeb"))));
    AggregateDataExchange exchange =
        new AggregateDataExchange()
            .setSource(source)
            .setTarget(new Target().setType(TargetType.EXTERNAL).setApi(api));
    exchange.setUid(uid);
    exchange.setName("Exchange " + uid);
    return exchange;
  }

  private String selectTargetApiValue(String uid, String key) {
    return jdbcTemplate.queryForObject(
        "select target->'api'->>? from aggregatedataexchange where uid = ?",
        String.class,
        key,
        uid);
  }
}
