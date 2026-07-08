package com.avalon.jpcap.repository.skl;

import lombok.Data;

//这个类用来存放按时间统计的结果
@Data
public class TopicCountByYearPO {
    private Integer year;  // 统计年份
    private Integer topicCount;     // 每年的课题数量
}
