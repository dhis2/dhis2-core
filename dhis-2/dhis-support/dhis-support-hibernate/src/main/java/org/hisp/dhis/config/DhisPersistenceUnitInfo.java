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
package org.hisp.dhis.config;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DhisPersistenceUnitInfo implements PersistenceUnitInfo {

  private String persistenceUnitName;
  private String persistenceProviderClassName;
  private List<String> managedClassNames = new ArrayList<>();
  private List<URL> jarFileUrls = new ArrayList<>();
  private DataSource jtaDataSource;
  private DataSource nonJtaDataSource;
  private Properties properties = new Properties();
  private List<String> mappingFileNames = new ArrayList<>();
  private URL persistenceUnitRootUrl;
  private boolean excludeUnlistedClasses;
  private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;
  private ValidationMode validationMode = ValidationMode.AUTO;
  private String persistenceXMLSchemaVersion = "3.1.0";
  private PersistenceUnitTransactionType transactionType =
      PersistenceUnitTransactionType.RESOURCE_LOCAL;

  @Override
  public boolean excludeUnlistedClasses() {
    return false;
  }

  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  @Override
  public void addTransformer(ClassTransformer classTransformer) {}

  @Override
  public ClassLoader getNewTempClassLoader() {
    return null;
  }
}
