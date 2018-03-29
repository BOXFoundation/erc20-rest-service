package fm.castbox.wallet.exception;

import fm.castbox.wallet.dto.GeneralResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@EnableWebMvc
@RestControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    // logger.error("400 Status Code", ex);
    BindingResult result = ex.getBindingResult();
    GeneralResponse bodyOfResponse = new GeneralResponse(result.getAllErrors(),
        "Invalid" + result.getObjectName());
    return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatus status,
      WebRequest request) {
    // logger.error("400 Status Code", ex);
    BindingResult result = ex.getBindingResult();
    GeneralResponse bodyOfResponse = new GeneralResponse(result.getAllErrors(),
        "Invalid" + result.getObjectName());
    return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler({UserNotExistException.class})
  public ResponseEntity<Object> handleCaptchaException(UserNotExistException ex,
      WebRequest request) {
    return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST,
        request);
  }

}
