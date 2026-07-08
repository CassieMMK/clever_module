package com.avalon.jpcap.service;

import com.avalon.jpcap.common.domain.PageDTO;
import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.converter.MjPromptModelConverter;
import com.avalon.jpcap.domain.PromptModelDto;
import com.avalon.jpcap.domain.PromptModelQueryDto;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.po.PromptModelPO;
import com.avalon.jpcap.repository.po.PromptModelQueryPO;
import com.avalon.jpcap.repository.service.RecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.avalon.jpcap.common.enums.ResultCodeEnum.PARAM_ERROR;

/**
 * mj社区、模型分享等相关的服务
 *
 * @author DingHaoLun
 * @since 2023-05-15 13:48
 **/
@Service
@Slf4j
public class MjCommunityService {

    /**
     * 提示词封装模板 repo服务
     */
    @Resource(name = "mjPromptModelRepositoryImpl")
    private RecordRepository<PromptModelPO, PromptModelQueryPO> promptModelRepository;


    /**
     * 插入 或 更新卡片内容
     */
    public void savePromptModel(PromptModelDto dto){
        PromptModelPO po = MjPromptModelConverter.dto2Po(dto);
        if(po.getId()==null){
            promptModelRepository.addRecord(po);
        }else{
            //先查询userId是否归属这个Id，防止越权
            PromptModelPO queryResultPo = promptModelRepository.queryRecordByCondition(new PromptModelQueryPO().setId(po.getId()));
            if(!queryResultPo.getUserId().equals(dto.getUserId())){
                throw new BusinessException(PARAM_ERROR.getCode(), "您没有此卡片的编辑权限");
            }
            promptModelRepository.updateRecord(po);
        }
    }

    /**
     * 卡片使用次数+1 或 点赞次数+1
     */
    //TODO


    /**
     * 分页查询卡片
     */
    public PageDTO<PromptModelDto> queryPage(PromptModelQueryDto queryDto){
        PromptModelQueryPO queryPO = MjPromptModelConverter.dto2Po(queryDto);
        PagePO<PromptModelPO> pagePO = promptModelRepository.queryPageRecordByCondition(queryPO);

        PageDTO<PromptModelDto> pageDTO = new PageDTO<>();
        pageDTO.setPageSize(queryDto.getPageSize());
        pageDTO.setCurrentPage(queryDto.getCurrentPage());
        if(pagePO!=null && CollectionUtils.isNotEmpty(pagePO.getData())){
            List<PromptModelDto> dtos = pagePO.getData().stream().map(po -> {
                PromptModelDto dto = MjPromptModelConverter.po2Dto(po);
                return dto;
            }).collect(Collectors.toList());

            pageDTO.setData(dtos);
            pageDTO.setTotalCount(pagePO.getTotalCount());
        }
        return pageDTO;
    }
}