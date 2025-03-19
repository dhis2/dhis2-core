/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.generator;

/** Provides basic methods required by test generator implementers. */
public interface Generator {
  /**
   * Defines the numbers of methods that each generated class will have. Classes, with a very large
   * amount of lines, are not allowed by the JVM.
   *
   * @return the numbers of test methods per class.
   */
  int getMaxTestsPerClass();

  /**
   * The action used for the respective suite of tests. ie:
   *
   * <p>"aggregate", "query", etc.
   *
   * @return the action.
   */
  String getAction();

  /**
   * Just a prefix for the generated class.
   *
   * @return the class prefix.
   */
  String getClassNamePrefix();

  /**
   * The file where the test scenarios will be read from.
   *
   * @return the file name.
   */
  String getScenarioFile();

  /**
   * Returns specific scenarios for test generation. If empty, ALL scenarios will be generated.
   *
   * @return the scenarios.
   */
  default String[] getScenarios() {
    return new String[] {};
  }

  /**
   * The folder where the input test scenarios are living.
   *
   * @return the file name.
   */
  default String getScenarioFolder() {
    return "scenarios";
  }

  /**
   * Just a comment to be added to the generated class at class level.
   *
   * @return the class comment.
   */
  String getTopClassComment();

  /**
   * The declaration of the test action. ie:
   *
   * <p>private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();
   *
   * @return the action to be used in the respective test class.
   */
  String getActionDeclaration();

  /**
   * The package where the test will live.
   *
   * @return the full package.
   */
  String getPackage();

  /** If "true", the request URL must not skip the metadata. */
  boolean assertMetaData();

  /** If "true", this will assert rows in the exact order based on the index. */
  boolean assertRowIndex();
}
