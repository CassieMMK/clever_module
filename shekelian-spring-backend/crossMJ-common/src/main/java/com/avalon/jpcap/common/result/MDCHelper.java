package com.avalon.jpcap.common.result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.UUID;
public class MDCHelper {

    /**
     * 字段定义
     */
    public static final String TRACE_ID = "X-trace-id";
    public static final String OPERATOR = "X-operator";
    public static final String IDENTITY = "X-identity";

    /**
     * 获取跟踪号
     *
     * @return 跟踪号
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 获取业务身份
     *
     * @return 业务身份
     */
    public static String getIdentity() {
        return MDC.get(IDENTITY);
    }

    /**
     * 获取操作人
     *
     * @return 操作人
     */
    public static String getOperator() {
        return MDC.get(OPERATOR);
    }

    /**
     * 清除上下文
     */
    public static void uninstall() {
        MDC.clear();
    }

    /**
     * 安装上下文到Slf4j的日志头中
     *
     * @param traceId  跟踪号ID
     * @param operator 操作人
     * @param identity 业务身份
     */
    public static void install(String traceId, String operator, String identity) {
        MDC.put(TRACE_ID,
                StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString().replace("-", "")));
        MDC.put(OPERATOR, StringUtils.defaultIfBlank(operator, ""));
        MDC.put(IDENTITY, StringUtils.defaultIfBlank(identity, ""));
    }
}