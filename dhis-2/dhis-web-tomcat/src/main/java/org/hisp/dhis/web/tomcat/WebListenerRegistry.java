
package org.hisp.dhis.web.tomcat;

public interface WebListenerRegistry {

  /**
   * Adds web listeners that will be registered with the servlet container.
   * @param webListenerClassNames the class names of the web listeners
   */
  void addWebListeners(String... webListenerClassNames);

}
