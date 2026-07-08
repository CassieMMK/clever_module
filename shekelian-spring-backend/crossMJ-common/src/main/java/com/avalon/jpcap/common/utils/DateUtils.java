package com.avalon.jpcap.common.utils;

import com.avalon.jpcap.common.enums.ResultCodeEnum;
import com.avalon.jpcap.common.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
//import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * 时间日期工具
 *
 * @author DingHaoLun
 * @since 2023-04-17 19:08
 **/
@Slf4j
public class DateUtils {

    public static final String ISO_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATE = "yyyy-MM-dd";

    /**
     * 某个日期时间加减
     * @param field: @see java.util.Calendar
     * @param num: 正负数字，代表日期加减
     */
    public static Date dateCalculate(Date date, int field, int num) throws Exception {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(date);
        rightNow.add(field, num);
        Date dt1 = rightNow.getTime();
        return dt1;
    }

    public static int compareTime(Date a, Date b){
        /**
         * 若 a在b时间之后>0,
         * 若 a在b时间之前<0,
         * 若时间一样 = 0
         */
        return a.compareTo(b);
    }



    public static String toIsoDateTime(Date time) {
        return toStr("yyyy-MM-dd HH:mm:ss", time);
    }

    public static String toIsoDate(Date time) {
        return toStr("yyyy-MM-dd", time);
    }

    public static String toStr(String pattern, Date date) {
        return !Objects.isNull(pattern) && !Objects.isNull(date) ? (new SimpleDateFormat(pattern)).format(date) : null;
    }

    public static Date parseDate(String dateStr) {
        return parseDate(dateStr, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date parseDate(String dateStr, String pattern) {
        if (!StringUtils.isBlank(dateStr) && !StringUtils.isBlank(pattern)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                return sdf.parse(dateStr);
            } catch (Exception var3) {
                log.error("解析日期异常,dateStr：" + dateStr, var3);
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "解析日期异常");
            }
        } else {
            return null;
        }
    }

    public static Date localDateTime2Date(LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return null;
        } else {
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
            return Date.from(zonedDateTime.toInstant());
        }
    }
}