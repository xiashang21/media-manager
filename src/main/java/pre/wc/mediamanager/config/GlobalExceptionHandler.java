package pre.wc.mediamanager.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pre.wc.mediamanager.common.BusinessException;
import pre.wc.mediamanager.common.ForbiddenException;
import pre.wc.mediamanager.common.Result;
import pre.wc.mediamanager.common.UnauthorizedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Marker BUSINESS = MarkerFactory.getMarker("BUSINESS");
    private static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn(BUSINESS, "业务异常 status={} message={}", e.getStatus(), e.getMessage());
        return response(HttpStatus.valueOf(e.getStatus()), e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Void>> handleUnauthorized(UnauthorizedException e) {
        log.warn(SECURITY, "鉴权失败 message={}", e.getMessage());
        return response(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result<Void>> handleForbidden(ForbiddenException e) {
        log.warn(SECURITY, "越权访问被拒绝 message={}", e.getMessage());
        return response(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn(BUSINESS, "上传文件超过大小限制");
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "上传文件超过大小限制");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数错误" : error.getDefaultMessage())
                .orElse("请求参数错误");
        log.warn(BUSINESS, "参数校验失败 message={}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        String message = e.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数错误" : error.getDefaultMessage())
                .orElse("请求参数错误");
        log.warn(BUSINESS, "参数绑定失败 message={}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        log.warn(BUSINESS, "请求参数错误 message={}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST, "请求参数错误：" + e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn(BUSINESS, "请求体格式错误 message={}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        log.warn(BUSINESS, "不支持的 Content-Type message={}", e.getMessage());
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请使用正确的 Content-Type");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleConflict(DataIntegrityViolationException e) {
        log.warn(BUSINESS, "数据约束冲突 message={}", e.getMostSpecificCause().getMessage());
        return response(HttpStatus.CONFLICT, "数据冲突，请检查是否重复提交");
    }

    /**
     * 用户快进视频时浏览器会主动关闭旧连接，服务端继续写流式响应会触发该异常。
     * 这属于正常现象，只记录 debug 日志，避免控制台大量 ERROR 堆栈。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnect(AsyncRequestNotUsableException e) {
        log.debug("客户端关闭了流式连接: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAll(Exception e) {
        log.error("系统异常 type={} message={}", e.getClass().getSimpleName(), e.getMessage(), e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    private ResponseEntity<Result<Void>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }
}
