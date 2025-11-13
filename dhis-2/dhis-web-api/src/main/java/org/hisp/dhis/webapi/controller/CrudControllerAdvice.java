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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.createWebMessage;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.mergeReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.objectReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import io.github.classgraph.ClassGraph;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.ServletException;
import java.beans.PropertyEditorSupport;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalException;
import org.hisp.dhis.dataexchange.client.Dhis2ClientException;
import org.hisp.dhis.dxf2.metadata.MetadataExportException;
import org.hisp.dhis.dxf2.metadata.MetadataImportException;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.dxf2.webmessage.responses.ErrorReportsWebMessageResponse;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fieldfilter.FieldFilterException;
import org.hisp.dhis.query.QueryException;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.SchemaPathException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;
import org.hisp.dhis.system.util.HttpUtils;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateForbiddenException;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.exception.MetadataImportConflictException;
import org.hisp.dhis.webapi.controller.exception.MetadataSyncException;
import org.hisp.dhis.webapi.controller.exception.MetadataVersionException;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.controller.tracker.imports.IdSchemeParamEditor;
import org.hisp.dhis.webapi.security.apikey.ApiTokenAuthenticationException;
import org.hisp.dhis.webapi.security.apikey.ApiTokenError;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@ControllerAdvice
public class CrudControllerAdvice {
  // Add sensitive exceptions into this array
  private static final Class<?>[] SENSITIVE_EXCEPTIONS = {
    BadSqlGrammarException.class,
    org.hibernate.QueryException.class,
    DataAccessResourceFailureException.class
  };

  private static final String GENERIC_ERROR_MESSAGE =
      "An unexpected error has occured. Please contact your system administrator";

  private final List<Class<?>> enumClasses;

  public CrudControllerAdvice() {
    this.enumClasses =
        new ClassGraph()
            .acceptPackages("org.hisp.dhis")
            .enableClassInfo()
            .scan()
            .getAllClasses()
            .getEnums()
            .loadClasses();
  }

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Date.class, new FromTextPropertyEditor(DateUtils::parseDate));
    binder.registerCustomEditor(
        IdentifiableProperty.class, new FromTextPropertyEditor(String::toUpperCase));
    this.enumClasses.forEach(c -> binder.registerCustomEditor(c, new ConvertEnum(c)));
    binder.registerCustomEditor(TrackerIdSchemeParam.class, new IdSchemeParamEditor());
  }

  @ExceptionHandler
  @ResponseBody
  public WebMessage badSqlGrammarException(BadSqlGrammarException ex) {
    return Optional.of(ex)
        .map(BadSqlGrammarException::getSQLException)
        .map(WebMessageUtils::createWebMessage)
        .orElse(defaultExceptionHandler(ex));
  }

  @ExceptionHandler(org.hisp.dhis.feedback.BadRequestException.class)
  @ResponseBody
  public WebMessage badRequestException(org.hisp.dhis.feedback.BadRequestException ex) {
    WebMessage message = badRequest(ex.getMessage(), ex.getCode());
    if (!ex.getErrorReports().isEmpty()) {
      message.setResponse(new ErrorReportsWebMessageResponse(ex.getErrorReports()));
    }
    return message;
  }

  @ExceptionHandler(org.hisp.dhis.feedback.ConflictException.class)
  @ResponseBody
  public WebMessage conflictException(org.hisp.dhis.feedback.ConflictException ex) {
    if (ex.getObjectReport() != null) {
      return objectReport(ex.getObjectReport());
    }

    if (ex.getMergeReport() != null) {
      return mergeReport(ex.getMergeReport());
    }
    return conflict(ex.getMessage(), ex.getCode()).setDevMessage(ex.getDevMessage());
  }

  @ExceptionHandler(org.hisp.dhis.feedback.ForbiddenException.class)
  @ResponseBody
  public WebMessage forbiddenException(org.hisp.dhis.feedback.ForbiddenException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.FORBIDDEN, ex.getCode());
  }

  @ExceptionHandler(org.hisp.dhis.feedback.NotFoundException.class)
  @ResponseBody
  public WebMessage notFoundException(org.hisp.dhis.feedback.NotFoundException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.NOT_FOUND, ex.getCode());
  }

  @ExceptionHandler(org.hisp.dhis.feedback.HiddenNotFoundException.class)
  @ResponseBody
  public WebMessage hiddenNotFoundException(org.hisp.dhis.feedback.HiddenNotFoundException ex) {
    return createWebMessage(Status.OK, HttpStatus.OK);
  }

  @ExceptionHandler(RestClientException.class)
  @ResponseBody
  public WebMessage restClientExceptionHandler(RestClientException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(IllegalQueryException.class)
  @ResponseBody
  public WebMessage illegalQueryExceptionHandler(IllegalQueryException ex) {
    return conflict(ex.getMessage(), ex.getErrorCode());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseBody
  public WebMessage methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    Class<?> requiredType = ex.getRequiredType();
    PathVariable pathVariableAnnotation =
        ex.getParameter().getParameterAnnotation(PathVariable.class);
    String field = ex.getName();
    Object value = ex.getValue();
    boolean isPathVariable = pathVariableAnnotation != null;
    String notValidValueMessage = getNotValidValueMessage(value, field, isPathVariable);

    String customErrorMessage;
    if (requiredType == null) {
      customErrorMessage = ex.getMessage();
    } else if (requiredType.isEnum()) {
      customErrorMessage = getEnumErrorMessage(requiredType);
    } else if (requiredType.isPrimitive()) {
      customErrorMessage = getGenericFieldErrorMessage(requiredType.getSimpleName());
    } else if (ex.getCause() instanceof IllegalArgumentException) {
      customErrorMessage = ex.getCause().getMessage();
    } else if (ex.getCause() instanceof ConversionFailedException conversionException) {
      notValidValueMessage =
          getConversionErrorMessage(value, field, conversionException, isPathVariable);
      customErrorMessage = "";
    } else {
      customErrorMessage = getGenericFieldErrorMessage(requiredType.getSimpleName());
    }

    return badRequest(getFormattedBadRequestMessage(notValidValueMessage, customErrorMessage));
  }

  @ExceptionHandler(TypeMismatchException.class)
  @ResponseBody
  public WebMessage handleTypeMismatchException(TypeMismatchException ex) {
    Class<?> requiredType = ex.getRequiredType();
    String field = ex.getPropertyName();
    Object value = ex.getValue();
    String notValidValueMessage = getNotValidValueMessage(value, field);

    String customErrorMessage;
    if (requiredType == null) {
      customErrorMessage = ex.getMessage();
    } else if (requiredType.isEnum()) {
      customErrorMessage = getEnumErrorMessage(requiredType);
    } else if (requiredType.isPrimitive()) {
      customErrorMessage = getGenericFieldErrorMessage(requiredType.getSimpleName());
    } else if (ex.getCause() instanceof IllegalArgumentException) {
      customErrorMessage = ex.getCause().getMessage();
    } else if (ex.getCause() instanceof ConversionFailedException conversionException) {
      notValidValueMessage = getConversionErrorMessage(value, field, conversionException, false);
      customErrorMessage = "";
    } else {
      customErrorMessage = getGenericFieldErrorMessage(requiredType.getSimpleName());
    }

    return badRequest(getFormattedBadRequestMessage(notValidValueMessage, customErrorMessage));
  }

  private String getEnumErrorMessage(Class<?> requiredType) {
    String validValues =
        StringUtils.join(
            Arrays.stream(requiredType.getEnumConstants())
                .map(Objects::toString)
                .collect(Collectors.toList()),
            ", ");
    return MessageFormat.format("Valid values are: [{0}]", validValues);
  }

  private String getGenericFieldErrorMessage(String fieldType) {
    return MessageFormat.format("It should be of type {0}", fieldType);
  }

  private static String getNotValidValueMessage(Object value, String field) {
    return getNotValidValueMessage(value, field, false);
  }

  private static String getNotValidValueMessage(
      Object value, String field, boolean isPathVariable) {
    if (value == null || (value instanceof String stringValue && stringValue.isEmpty())) {
      return MessageFormat.format("{0} cannot be empty.", field);
    }
    if (isPathVariable) {
      return String.format("Value '%s' is not valid for path parameter %s.", value, field);
    }
    return String.format("Value '%s' is not valid for parameter %s.", value, field);
  }

  private String getFormattedBadRequestMessage(Object value, String field, String customMessage) {
    return getNotValidValueMessage(value, field) + " " + customMessage;
  }

  private String getFormattedBadRequestMessage(String fieldErrorMessage, String customMessage) {
    if (StringUtils.isEmpty(customMessage)) {
      return fieldErrorMessage;
    }
    return fieldErrorMessage + " " + customMessage;
  }

  private static String getConversionErrorMessage(
      Object rootValue, String field, ConversionFailedException ex, boolean isPathVariable) {
    Object invalidValue = ex.getValue();
    if (TypeDescriptor.valueOf(String.class).equals(ex.getSourceType())
        && (invalidValue != null && ((String) invalidValue).contains(","))
        && (rootValue != null && rootValue.getClass().isArray())) {
      return "You likely repeated request parameter '"
          + field
          + "' and used multiple comma-separated values within at least one of its values. Choose"
          + " one of these approaches. "
          + ex.getCause().getMessage();
    }

    return getNotValidValueMessage(invalidValue, field, isPathVariable)
        + " "
        + ex.getCause().getMessage();
  }

  /**
   * A BindException wraps possible errors happened trying to bind all the request parameters to a
   * binding object. Errors could be simple conversion failures (Trying to convert a 'TEXT' to an
   * Integer ) or validation errors create by hibernate-validator framework. Currently, we are not
   * using such framework hence only conversion errors can happen.
   *
   * <p>Only first error is returned to the client in order to be consistent in the way BAD_REQUEST
   * responses are displayed.
   */
  @ExceptionHandler(BindException.class)
  @ResponseBody
  public WebMessage handleBindException(BindException ex) {
    FieldError fieldError = ex.getFieldError();

    if (fieldError != null && fieldError.contains(TypeMismatchException.class)) {
      return handleTypeMismatchException(fieldError.unwrap(TypeMismatchException.class));
    }

    if (fieldError != null) {
      return badRequest(
          getFormattedBadRequestMessage(
              fieldError.getRejectedValue(), fieldError.getField(), ex.getMessage()));
    }

    return badRequest(ex.getMessage());
  }

  @ExceptionHandler(Dhis2ClientException.class)
  @ResponseBody
  public WebMessage dhis2ClientExceptionHandler(Dhis2ClientException ex) {
    return conflict(ex.getMessage(), ex.getErrorCode());
  }

  @ExceptionHandler(QueryRuntimeException.class)
  @ResponseBody
  public WebMessage queryRuntimeExceptionHandler(QueryRuntimeException ex) {
    return conflict(ex.getMessage(), ex.getErrorCode());
  }

  @ExceptionHandler(DeleteNotAllowedException.class)
  @ResponseBody
  public WebMessage deleteNotAllowedExceptionHandler(DeleteNotAllowedException ex) {
    return conflict(ex.getMessage(), ex.getErrorCode());
  }

  @ExceptionHandler(InvalidIdentifierReferenceException.class)
  @ResponseBody
  public WebMessage invalidIdentifierReferenceExceptionHandler(
      InvalidIdentifierReferenceException ex) {
    return conflict(ex.getMessage());
  }

  @ExceptionHandler({DataApprovalException.class})
  @ResponseBody
  public WebMessage dataApprovalExceptionHandler(Exception ex) {
    return conflict(ex.getMessage());
  }

  @ExceptionHandler({
    JsonParseException.class,
    MetadataImportException.class,
    MetadataExportException.class
  })
  @ResponseBody
  public WebMessage jsonParseExceptionHandler(Exception ex) {
    return conflict(ex.getMessage());
  }

  @ExceptionHandler({QueryParserException.class, QueryException.class})
  @ResponseBody
  public WebMessage queryExceptionHandler(Exception ex) {
    return conflict(ex.getMessage());
  }

  @ExceptionHandler(FieldFilterException.class)
  @ResponseBody
  public WebMessage fieldFilterExceptionHandler(FieldFilterException ex) {
    return conflict(ex.getMessage());
  }

  @ExceptionHandler(NotAuthenticatedException.class)
  @ResponseBody
  public WebMessage notAuthenticatedExceptionHandler(NotAuthenticatedException ex) {
    return unauthorized(ex.getMessage());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseBody
  public WebMessage constraintViolationExceptionHandler(ConstraintViolationException ex) {
    return error(getExceptionMessage(ex));
  }

  @ExceptionHandler(PersistenceException.class)
  @ResponseBody
  public WebMessage persistenceExceptionHandler(PersistenceException ex) {
    String helpfulMessage = getHelpfulMessage(ex);
    return conflict(helpfulMessage);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseBody
  public WebMessage accessDeniedExceptionHandler(AccessDeniedException ex) {
    return forbidden(ex.getMessage());
  }

  @ExceptionHandler(WebMessageException.class)
  @ResponseBody
  public ResponseEntity<WebMessage> webMessageExceptionHandler(WebMessageException ex) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>(
        ex.getWebMessage(), headers, ex.getWebMessage().getHttpStatusCode());
  }

  @ExceptionHandler(HttpStatusCodeException.class)
  @ResponseBody
  public WebMessage httpStatusCodeExceptionHandler(HttpStatusCodeException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpUtils.resolve(ex.getStatusCode()));
  }

  @ExceptionHandler(HttpClientErrorException.class)
  @ResponseBody
  public WebMessage httpClientErrorExceptionHandler(HttpClientErrorException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpUtils.resolve(ex.getStatusCode()));
  }

  @ExceptionHandler(HttpServerErrorException.class)
  @ResponseBody
  public WebMessage httpServerErrorExceptionHandler(HttpServerErrorException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpUtils.resolve(ex.getStatusCode()));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseBody
  public WebMessage httpRequestMethodNotSupportedExceptionHandler(
      HttpRequestMethodNotSupportedException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  @ResponseBody
  public WebMessage httpMediaTypeNotAcceptableExceptionHandler(
      HttpMediaTypeNotAcceptableException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  @ResponseBody
  public WebMessage httpMediaTypeNotSupportedExceptionHandler(
      HttpMediaTypeNotSupportedException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(ServletException.class)
  public void servletExceptionHandler(ServletException ex) throws ServletException {
    throw ex;
  }

  @ExceptionHandler({SchemaPathException.class, JsonPatchException.class})
  @ResponseBody
  public WebMessage handleBadRequest(Exception ex) {
    return badRequest(ex.getMessage());
  }

  /**
   * Handles {@link IllegalArgumentException} and logs the stack trace to standard error. {@link
   * IllegalArgumentException} is used in DHIS 2 application code but also by various frameworks to
   * indicate programming errors, so stack trace must be printed and not swallowed.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseBody
  public WebMessage illegalArgumentExceptionHandler(IllegalArgumentException ex) {
    log.error(IllegalArgumentException.class.getName(), ex);
    return badRequest(ex.getMessage());
  }

  /**
   * Handles {@link RuntimeJsonMappingException} and logs the stack trace. {@link
   * RuntimeJsonMappingException} is used in DHIS 2 application code but also by various frameworks
   * to indicate parsing errors, so stack trace must be printed and not swallowed.
   */
  @ExceptionHandler(RuntimeJsonMappingException.class)
  @ResponseBody
  public WebMessage runtimeJsonMappingExceptionHandler(RuntimeJsonMappingException ex) {
    log.error(RuntimeJsonMappingException.class.getName(), ex);
    return badRequest(ex.getMessage());
  }

  /**
   * Handles {@link IllegalStateException} and logs the stack trace to standard error. {@link
   * IllegalStateException} is used in DHIS 2 application code but also by various frameworks to
   * indicate programming errors, so stack trace must be printed and not swallowed.
   */
  @ExceptionHandler(IllegalStateException.class)
  @ResponseBody
  public WebMessage illegalStateExceptionHandler(IllegalStateException ex) {
    log.error(IllegalStateException.class.getName(), ex);
    return conflict(ex.getMessage());
  }

  @ExceptionHandler(MetadataVersionException.class)
  @ResponseBody
  public WebMessage handleMetaDataVersionException(
      MetadataVersionException metadataVersionException) {
    return error(metadataVersionException.getMessage());
  }

  @ExceptionHandler(MetadataSyncException.class)
  @ResponseBody
  public WebMessage handleMetaDataSyncException(MetadataSyncException ex) {
    return error(ex.getMessage());
  }

  @ExceptionHandler(DhisVersionMismatchException.class)
  @ResponseBody
  public WebMessage handleDhisVersionMismatchException(
      DhisVersionMismatchException versionMismatchException) {
    return forbidden(versionMismatchException.getMessage());
  }

  @ExceptionHandler(MetadataImportConflictException.class)
  @ResponseBody
  public WebMessage handleMetadataImportConflictException(MetadataImportConflictException ex) {
    if (ex.getMetadataSyncSummary() == null) {
      return conflict(ex.getMessage());
    }
    return conflict(null).setResponse(ex.getMetadataSyncSummary());
  }

  @ExceptionHandler(OAuth2AuthenticationException.class)
  @ResponseBody
  public WebMessage handleOAuth2AuthenticationException(OAuth2AuthenticationException ex) {
    OAuth2Error error = ex.getError();
    if (error instanceof BearerTokenError bearerTokenError) {
      HttpStatus status = ((BearerTokenError) error).getHttpStatus();

      return createWebMessage(
          bearerTokenError.getErrorCode(), bearerTokenError.getDescription(), Status.ERROR, status);
    }
    return unauthorized(ex.getMessage());
  }

  @ExceptionHandler(ApiTokenAuthenticationException.class)
  @ResponseBody
  public WebMessage handleApiTokenAuthenticationException(ApiTokenAuthenticationException ex) {
    ApiTokenError apiTokenError = ex.getError();
    if (apiTokenError != null) {
      return createWebMessage(
          apiTokenError.getDescription(), Status.ERROR, apiTokenError.getHttpStatus());
    }
    return unauthorized(ex.getMessage());
  }

  @ExceptionHandler(TwoFactorAuthenticationException.class)
  @ResponseBody
  public WebMessage handleTwoFactorAuthenticationException(TwoFactorAuthenticationException ex) {
    return unauthorized(ex.getMessage());
  }

  @ExceptionHandler(BadCredentialsException.class)
  @ResponseBody
  public WebMessage handleBadCredentialsException(BadCredentialsException ex) {
    return unauthorized(ex.getMessage());
  }

  @ExceptionHandler({PotentialDuplicateConflictException.class})
  @ResponseBody
  public WebMessage handlePotentialDuplicateConflictRequest(Exception ex) {
    return conflict(ex.getMessage());
  }

  @ExceptionHandler({PotentialDuplicateForbiddenException.class})
  @ResponseBody
  public WebMessage handlePotentialDuplicateForbiddenRequest(Exception ex) {
    return forbidden(ex.getMessage());
  }

  @ExceptionHandler(org.hisp.dhis.feedback.BadGatewayException.class)
  @ResponseBody
  public WebMessage badGatewayException(org.hisp.dhis.feedback.BadGatewayException ex) {
    return createWebMessage(ex.getMessage(), Status.ERROR, HttpStatus.BAD_GATEWAY);
  }

  /**
   * Catches default exception and send back to user, but re-throws internally so it still ends up
   * in server logs.
   */
  @ResponseBody
  @ExceptionHandler(Exception.class)
  public WebMessage defaultExceptionHandler(Exception ex) {
    log.error(Exception.class.getName(), ex);
    return error(getExceptionMessage(ex));
  }

  /**
   * Exception handler handling {@link UID} instantiation errors (from {@link String} to {@link
   * UID}) received in web requests. The error message is checked to see if it contains 'UID' & ';'
   * so it can be formatted more nicely for client consumption, otherwise too much extraneous
   * exception info is included. See e2e {@link IndicatorTypeMergeTest#testInvalidSourceUid} for
   * example response expected.
   *
   * @param ex exception
   * @return web message
   */
  @ResponseBody
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public WebMessage handleHttpMessageNotReadableExceptionHandler(
      HttpMessageNotReadableException ex) {
    String message = "HttpMessageNotReadableException exception has no message";
    String exMessage = ex.getMessage();
    if (exMessage != null) {
      message = exMessage;
    }

    if (message.contains("UID") && message.contains(";")) {
      message = message.substring(0, message.indexOf(';'));
    }
    return badRequest(message);
  }

  private String getExceptionMessage(Exception ex) {
    boolean isMessageSensitive = false;

    String message = ex.getMessage();

    if (isSensitiveException(ex)) {
      isMessageSensitive = true;
    }

    if (ex.getCause() != null) {
      message = ex.getCause().getMessage();

      if (isSensitiveException(ex.getCause())) {
        isMessageSensitive = true;
      }
    }

    if (isMessageSensitive) {
      message = GENERIC_ERROR_MESSAGE;
    }
    return message;
  }

  private boolean isSensitiveException(Throwable e) {
    for (Class<?> exClass : SENSITIVE_EXCEPTIONS) {
      if (exClass.isAssignableFrom(e.getClass())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Simple adapter to {@link PropertyEditorSupport} that allows to use lambda {@link Function}s to
   * convert value from its text representation.
   */
  private static final class FromTextPropertyEditor extends PropertyEditorSupport {
    private final Function<String, Object> fromText;

    private FromTextPropertyEditor(Function<String, Object> fromText) {
      this.fromText = fromText;
    }

    @Override
    public void setAsText(String text) {
      setValue(fromText.apply(text));
    }
  }

  private static final class ConvertEnum<T extends Enum<T>> extends PropertyEditorSupport {
    private final Class<T> enumClass;

    private ConvertEnum(Class<T> enumClass) {
      this.enumClass = enumClass;
    }

    @Override
    public void setAsText(String text) {
      Enum<T> enumValue = EnumUtils.getEnumIgnoreCase(enumClass, text);

      if (enumValue == null) {
        throw new IllegalArgumentException(
            MessageFormat.format(" Cannot convert {0} to {1}", text, enumClass));
      }

      setValue(enumValue);
    }
  }

  /**
   * {@link PersistenceException}s can have deeply-nested root causes and may have a very vague
   * message, which may not be very helpful. This method checks if a more detailed, user-friendly
   * message is available and returns it if found.
   *
   * <p>For example, instead of returning: <b><i>"Could not execute statement"</i></b> , potentially
   * returning: <b><i>"duplicate key value violates unique constraint "minmaxdataelement_unique_key"
   * Detail: Key (sourceid, dataelementid, categoryoptioncomboid)=(x, y, z) already exists"</i></b>.
   *
   * @param ex exception to check
   * @return detailed message or original exception message
   */
  @Nullable
  public static String getHelpfulMessage(PersistenceException ex) {
    Throwable cause = ex.getCause();

    if (cause != null) {
      Throwable rootCause = cause.getCause();
      if (rootCause != null) {
        return rootCause.getMessage();
      }
    }
    return ex.getMessage();
  }
}
