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
package org.hisp.dhis.monitoring.metrics.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.springframework.beans.DirectFieldAccessor;

/**
 * @author Morten Svanæs
 */
public class HikariPoolMetadataAccessor extends AbstractPoolMetadata<HikariDataSource> {

  public HikariPoolMetadataAccessor(HikariDataSource dataSource) {
    super(dataSource);
  }

  @Override
  public Integer getActive() {
    try {
      return getHikariPool().getActiveConnections();
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public Integer getIdle() {
    try {
      return getHikariPool().getIdleConnections();
    } catch (Exception ex) {
      return null;
    }
  }

  private HikariPool getHikariPool() {
    return (HikariPool) new DirectFieldAccessor(getDataSource()).getPropertyValue("pool");
  }

  @Override
  public Integer getMax() {
    return getDataSource().getMaximumPoolSize();
  }

  @Override
  public Integer getMin() {
    return getDataSource().getMinimumIdle();
  }

  @Override
  public String getValidationQuery() {
    return getDataSource().getConnectionTestQuery();
  }

  @Override
  public Boolean getDefaultAutoCommit() {
    return getDataSource().isAutoCommit();
  }
}
