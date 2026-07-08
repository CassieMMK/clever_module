package com.avalon.jpcap.repository.po;

import com.avalon.jpcap.common.enums.MjCrossRankResultEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author DingHaoLun
 * @since 2023-04-19 15:25
 **/
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class MjProcessPO {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 队列流名称
     */
    private String streamKey;

    /**
     * 队列stream流中的Id
     */
    private String messageId;

    /**
     * 排队排名
     * 0 为处理，1为前面还有1人排队。依此类推
     * -1 为目前没有执行中的（上一个已完成）
     */
    private Integer rank;

    /**以下是和mj请求有关的参数 Start*/
    /**
     * 请求类型
     * @see com.avalon.jpcap.common.enums.MjDiscordSendTypeEnum
     */
    private Integer type;

    /**
     * 请求体的prompt
     */
    private String prompt;
    /**以下是和mj请求有关的参数 End*/

    /**
     * 是否现在有请求在排队中
     * @return
     */
    public Boolean inRanking(){
        if(     MjCrossRankResultEnum.RANK_RESULT_SUCCESS.getCode().equals(this.getRank()) ||
                MjCrossRankResultEnum.RANK_RESULT_FAIL.getCode().equals(this.getRank())){
            return false;
        }
        //null或者排队中
        return true;
    }
}