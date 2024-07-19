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
package org.hisp.dhis.test;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.utils.TestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(SpringExtension.class)
@Slf4j
public abstract class BaseSpringTest extends DhisConvenienceTest
    implements ApplicationContextAware {

  protected ApplicationContext applicationContext;

  @Autowired protected DbmsManager dbmsManager;

  @Autowired protected TransactionTemplate transactionTemplate;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /** Method to override. */
  protected void setUpTest() throws Exception {}

  /** Method to override. */
  protected void tearDownTest() throws Exception {}

  protected void nonTransactionalAfter() throws Exception {
    clearSecurityContext();
    tearDownTest();
    try {
      dbmsManager.clearSession();
    } catch (Exception e) {
      log.error("Failed to clear hibernate session, reason: {}", e.getMessage(), e);
    }
    transactionTemplate.execute(
        status -> {
          dbmsManager.emptyDatabase();
          return null;
        });
  }

  protected void integrationTestBeforeEach() throws Exception {
    TestUtils.executeStartupRoutines(applicationContext);
    setUpTest();
  }
}
