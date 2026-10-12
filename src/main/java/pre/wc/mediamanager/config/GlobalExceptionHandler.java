package pre.wc.mediamanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pre.wc.mediamanager.common.BusinessException;
import pre.wc.mediamanager.common.Result;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.error(413, "上传文件超过大小限制");
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public Result<Void> handleBadRequest(Exception e) {
        return Result.error(400, "请求参数错误：" + e.getMessage());
    }

    /**
     * 用户快进视频时浏览器会主动关闭旧连接，服务端继续写流式响应会触发该异常。
     * 这属于正常现象，只记录 debug 日志，避免控制台大量 ERROR 堆栈。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnect(AsyncRequestNotUsableException e) {
        log.debug("Client closed streaming connection: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("Unhandled server exception", e);
        return Result.error(500, "服务器内部错误");
    }
}
