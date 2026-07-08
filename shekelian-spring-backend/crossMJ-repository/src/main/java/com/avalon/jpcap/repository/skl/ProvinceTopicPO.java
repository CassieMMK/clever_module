package com.avalon.jpcap.repository.skl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
//定义有参无参构造器
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceTopicPO {
    private Integer id;  // 主键
    private String topicName;  //社科课题名称
    private Integer OrganizationId;  //发表课题的机关
    private Integer create_time;
    // .....其他列
}
