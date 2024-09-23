package org.hisp.dhis.webapi.mvc.interceptor;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.setting.SystemSettingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class SystemSettingsInterceptor implements HandlerInterceptor {

  private final SystemSettingsService settingManager;

  @Override
  public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws Exception {
    settingManager.clearCurrentSettings();
    // Note: if settings are used in this request they will be initialised on first access
    return true;
  }

  @Override
  public void afterCompletion(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, Exception ex) throws Exception {
    settingManager.clearCurrentSettings();
  }
}
