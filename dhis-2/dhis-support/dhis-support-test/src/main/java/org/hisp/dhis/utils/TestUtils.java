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
package org.hisp.dhis.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;
import org.hisp.dhis.BaseSpringTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class TestUtils {
  public static void executeStartupRoutines(ApplicationContext applicationContext)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    String id = "org.hisp.dhis.system.startup.StartupRoutineExecutor";

    if (applicationContext.containsBean(id)) {
      Object object = applicationContext.getBean(id);
      Method method = object.getClass().getMethod("executeForTesting");
      method.invoke(object);
    }
  }

  public static void executeIntegrationTestDataScript(
      Class<? extends BaseSpringTest> currentClass, JdbcTemplate jdbcTemplate) throws SQLException {
    IntegrationTestData annotation = currentClass.getAnnotation(IntegrationTestData.class);

    if (annotation != null) {
      ScriptUtils.executeSqlScript(
          Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection(),
          new EncodedResource(new ClassPathResource(annotation.path()), StandardCharsets.UTF_8));

      // Not very thread safe?!
      BaseSpringTest.dataInit = true;
    }
  }
}
