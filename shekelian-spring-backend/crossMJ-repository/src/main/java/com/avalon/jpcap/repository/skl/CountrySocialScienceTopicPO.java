package com.avalon.jpcap.repository.skl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountrySocialScienceTopicPO {
    private Integer id;  // 主键
    private String topicName;  //国社科课题名称
    private Integer OrganizationId;  //发表课题的机关
    // .....其他列
}
