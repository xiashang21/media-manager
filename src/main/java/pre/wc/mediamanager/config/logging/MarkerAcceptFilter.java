package pre.wc.mediamanager.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logback 1.5 移除了内置 MarkerFilter，这里保留按业务/安全标记拆分日志文件的能力。
 */
public class MarkerAcceptFilter extends Filter<ILoggingEvent> {

    private String marker;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted() || marker == null || event.getMarker() == null) {
            return FilterReply.DENY;
        }
        return marker.equals(event.getMarker().getName())
                ? FilterReply.ACCEPT : FilterReply.DENY;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
        start();
    }
}
