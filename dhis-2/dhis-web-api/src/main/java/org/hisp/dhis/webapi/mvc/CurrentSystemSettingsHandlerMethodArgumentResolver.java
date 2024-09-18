package org.hisp.dhis.webapi.mvc;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.annotation.Nonnull;

@Component
@RequiredArgsConstructor
public class CurrentSystemSettingsHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  private final SystemSettingsProvider settingsProvider;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType() == SystemSettings.class;
  }

  @Override
  public SystemSettings resolveArgument(
      @Nonnull MethodParameter parameter,
      ModelAndViewContainer container,
      @Nonnull NativeWebRequest request,
      WebDataBinderFactory binderFactory) {
    return settingsProvider.getCurrentSettings();
  }
}
