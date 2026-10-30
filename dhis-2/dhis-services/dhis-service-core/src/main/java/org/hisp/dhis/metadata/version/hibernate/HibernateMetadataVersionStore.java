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
package org.hisp.dhis.metadata.version.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.datastore.MetadataDatastoreService;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Implementation of MetadataVersionStore.
 *
 * @author aamerm
 */
@Repository("org.hisp.dhis.metadata.version.MetadataVersionStore")
public class HibernateMetadataVersionStore extends HibernateIdentifiableObjectStore<MetadataVersion>
    implements MetadataVersionStore {
  public HibernateMetadataVersionStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, MetadataVersion.class, aclService, false);
  }

  @Override
  public MetadataVersion getVersionByKey(long key) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder, newJpaParameters().addPredicate(root -> builder.equal(root.get("id"), key)));
  }

  @Override
  public MetadataVersion getVersionByName(String versionName) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("name"), versionName)));
  }

  @Override
  public MetadataVersion getCurrentVersion() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addOrder(root -> builder.desc(root.get("created")))
            .setMaxResults(1)
            .setCacheable(false));
  }

  @Override
  public List<MetadataVersion> getAllVersionsInBetween(Date startDate, Date endDate) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.between(root.get("created"), startDate, endDate)));
  }

  @Override
  public MetadataVersion getInitialVersion() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addOrder(root -> builder.asc(root.get("created")))
            .setMaxResults(1)
            .setCacheable(false));
  }

  @Override
  public boolean metadataVersionSnapshotExists(String versionName) {
    return Boolean.TRUE.equals(
        jdbcTemplate.query(
            "SELECT 1 FROM keyjsonvalue WHERE namespace = ? AND namespacekey = ? LIMIT 1",
            ResultSet::next,
            MetadataDatastoreService.METADATA_STORE_NS,
            versionName));
  }

  @Override
  public boolean streamMetadataVersionData(String versionName, OutputStream out)
      throws IOException {
    // The 'metadata' JSON key must match MetadataWrapper#getMetadata(); renaming the wrapper
    // field would silently break this query (no row found, snapshot reported missing).
    String sql =
        "SELECT jbvalue->>'metadata' FROM keyjsonvalue"
            + " WHERE namespace = ? AND namespacekey = ?";
    try {
      Boolean found =
          jdbcTemplate.query(
              sql,
              rs -> {
                if (!rs.next()) return false;
                try (InputStream in = rs.getBinaryStream(1)) {
                  if (in == null) return false;
                  in.transferTo(out);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
                return true;
              },
              MetadataDatastoreService.METADATA_STORE_NS,
              versionName);
      return Boolean.TRUE.equals(found);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
