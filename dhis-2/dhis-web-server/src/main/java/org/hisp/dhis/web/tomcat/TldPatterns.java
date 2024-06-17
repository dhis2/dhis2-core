/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.web.tomcat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>TLD skip and scan patterns used by Spring Boot.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class TldPatterns {

  static final Set<String> TOMCAT_SKIP;

  static {
    // Same as Tomcat
    Set<String> skipPatterns = new LinkedHashSet<>();
    skipPatterns.add("annotations-api.jar");
    skipPatterns.add("ant-junit*.jar");
    skipPatterns.add("ant-launcher*.jar");
    skipPatterns.add("ant*.jar");
    skipPatterns.add("asm-*.jar");
    skipPatterns.add("aspectj*.jar");
    skipPatterns.add("bcel*.jar");
    skipPatterns.add("biz.aQute.bnd*.jar");
    skipPatterns.add("bootstrap.jar");
    skipPatterns.add("catalina-ant.jar");
    skipPatterns.add("catalina-ha.jar");
    skipPatterns.add("catalina-ssi.jar");
    skipPatterns.add("catalina-storeconfig.jar");
    skipPatterns.add("catalina-tribes.jar");
    skipPatterns.add("catalina.jar");
    skipPatterns.add("cglib-*.jar");
    skipPatterns.add("cobertura-*.jar");
    skipPatterns.add("commons-beanutils*.jar");
    skipPatterns.add("commons-codec*.jar");
    skipPatterns.add("commons-collections*.jar");
    skipPatterns.add("commons-compress*.jar");
    skipPatterns.add("commons-daemon.jar");
    skipPatterns.add("commons-dbcp*.jar");
    skipPatterns.add("commons-digester*.jar");
    skipPatterns.add("commons-fileupload*.jar");
    skipPatterns.add("commons-httpclient*.jar");
    skipPatterns.add("commons-io*.jar");
    skipPatterns.add("commons-lang*.jar");
    skipPatterns.add("commons-logging*.jar");
    skipPatterns.add("commons-math*.jar");
    skipPatterns.add("commons-pool*.jar");
    skipPatterns.add("derby-*.jar");
    skipPatterns.add("dom4j-*.jar");
    skipPatterns.add("easymock-*.jar");
    skipPatterns.add("ecj-*.jar");
    skipPatterns.add("el-api.jar");
    skipPatterns.add("geronimo-spec-jaxrpc*.jar");
    skipPatterns.add("h2*.jar");
    skipPatterns.add("ha-api-*.jar");
    skipPatterns.add("hamcrest-*.jar");
    skipPatterns.add("hibernate*.jar");
    skipPatterns.add("httpclient*.jar");
    skipPatterns.add("icu4j-*.jar");
    skipPatterns.add("jakartaee-migration-*.jar");
    skipPatterns.add("jasper-el.jar");
    skipPatterns.add("jasper.jar");
    skipPatterns.add("jaspic-api.jar");
    skipPatterns.add("jaxb-*.jar");
    skipPatterns.add("jaxen-*.jar");
    skipPatterns.add("jaxws-rt-*.jar");
    skipPatterns.add("jdom-*.jar");
    skipPatterns.add("jetty-*.jar");
    skipPatterns.add("jmx-tools.jar");
    skipPatterns.add("jmx.jar");
    skipPatterns.add("jsp-api.jar");
    skipPatterns.add("jstl.jar");
    skipPatterns.add("jta*.jar");
    skipPatterns.add("junit-*.jar");
    skipPatterns.add("junit.jar");
    skipPatterns.add("log4j*.jar");
    skipPatterns.add("mail*.jar");
    skipPatterns.add("objenesis-*.jar");
    skipPatterns.add("oraclepki.jar");
    skipPatterns.add("org.hamcrest.core_*.jar");
    skipPatterns.add("org.junit_*.jar");
    skipPatterns.add("oro-*.jar");
    skipPatterns.add("servlet-api-*.jar");
    skipPatterns.add("servlet-api.jar");
    skipPatterns.add("slf4j*.jar");
    skipPatterns.add("taglibs-standard-spec-*.jar");
    skipPatterns.add("tagsoup-*.jar");
    skipPatterns.add("tomcat-api.jar");
    skipPatterns.add("tomcat-coyote.jar");
    skipPatterns.add("tomcat-dbcp.jar");
    skipPatterns.add("tomcat-i18n-*.jar");
    skipPatterns.add("tomcat-jdbc.jar");
    skipPatterns.add("tomcat-jni.jar");
    skipPatterns.add("tomcat-juli-adapters.jar");
    skipPatterns.add("tomcat-juli.jar");
    skipPatterns.add("tomcat-util-scan.jar");
    skipPatterns.add("tomcat-util.jar");
    skipPatterns.add("tomcat-websocket.jar");
    skipPatterns.add("tools.jar");
    skipPatterns.add("unboundid-ldapsdk-*.jar");
    skipPatterns.add("websocket-api.jar");
    skipPatterns.add("websocket-client-api.jar");
    skipPatterns.add("wsdl4j*.jar");
    skipPatterns.add("xercesImpl.jar");
    skipPatterns.add("xml-apis.jar");
    skipPatterns.add("xmlParserAPIs-*.jar");
    skipPatterns.add("xmlParserAPIs.jar");
    skipPatterns.add("xom-*.jar");
    TOMCAT_SKIP = Collections.unmodifiableSet(skipPatterns);
  }

  private static final Set<String> ADDITIONAL_SKIP;

  static {
    // Additional typical for Spring Boot applications
    Set<String> skipPatterns = new LinkedHashSet<>();
    skipPatterns.add("antlr-*.jar");
    skipPatterns.add("aopalliance-*.jar");
    skipPatterns.add("aspectjweaver-*.jar");
    skipPatterns.add("classmate-*.jar");
    skipPatterns.add("ehcache-core-*.jar");
    skipPatterns.add("hsqldb-*.jar");
    skipPatterns.add("jackson-annotations-*.jar");
    skipPatterns.add("jackson-core-*.jar");
    skipPatterns.add("jackson-databind-*.jar");
    skipPatterns.add("jandex-*.jar");
    skipPatterns.add("javassist-*.jar");
    skipPatterns.add("jboss-logging-*.jar");
    skipPatterns.add("jboss-transaction-api_*.jar");
    skipPatterns.add("jcl-over-slf4j-*.jar");
    skipPatterns.add("jdom-*.jar");
    skipPatterns.add("jul-to-slf4j-*.jar");
    skipPatterns.add("logback-classic-*.jar");
    skipPatterns.add("logback-core-*.jar");
    skipPatterns.add("rome-*.jar");
    skipPatterns.add("spring-aop-*.jar");
    skipPatterns.add("spring-aspects-*.jar");
    skipPatterns.add("spring-beans-*.jar");
    skipPatterns.add("spring-boot-*.jar");
    skipPatterns.add("spring-core-*.jar");
    skipPatterns.add("spring-context-*.jar");
    skipPatterns.add("spring-data-*.jar");
    skipPatterns.add("spring-expression-*.jar");
    skipPatterns.add("spring-jdbc-*.jar,");
    skipPatterns.add("spring-orm-*.jar");
    skipPatterns.add("spring-oxm-*.jar");
    skipPatterns.add("spring-tx-*.jar");
    skipPatterns.add("snakeyaml-*.jar");
    skipPatterns.add("tomcat-embed-core-*.jar");
    skipPatterns.add("tomcat-embed-logging-*.jar");
    skipPatterns.add("tomcat-embed-el-*.jar");
    skipPatterns.add("validation-api-*.jar");
    ADDITIONAL_SKIP = Collections.unmodifiableSet(skipPatterns);
  }

  static final Set<String> DEFAULT_SKIP;

  static {
    Set<String> skipPatterns = new LinkedHashSet<>();
    skipPatterns.addAll(TOMCAT_SKIP);
    skipPatterns.addAll(ADDITIONAL_SKIP);
    DEFAULT_SKIP = Collections.unmodifiableSet(skipPatterns);
  }

  static final Set<String> TOMCAT_SCAN;

  static {
    Set<String> scanPatterns = new LinkedHashSet<>();
    scanPatterns.add("log4j-taglib*.jar");
    scanPatterns.add("log4j-jakarta-web*.jar");
    scanPatterns.add("log4javascript*.jar");
    scanPatterns.add("slf4j-taglib*.jar");
    TOMCAT_SCAN = Collections.unmodifiableSet(scanPatterns);
  }

  static final Set<String> DEFAULT_SCAN;

  static {
    Set<String> scanPatterns = new LinkedHashSet<>();
    scanPatterns.addAll(TOMCAT_SCAN);
    DEFAULT_SCAN = Collections.unmodifiableSet(scanPatterns);
  }

  private TldPatterns() {}
}
