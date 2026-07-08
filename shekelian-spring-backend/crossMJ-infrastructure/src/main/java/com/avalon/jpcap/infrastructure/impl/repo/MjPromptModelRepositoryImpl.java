package com.avalon.jpcap.infrastructure.impl.repo;

import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.infrastructure.mapper.PromptModelMapper;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.po.PromptModelPO;
import com.avalon.jpcap.repository.po.PromptModelQueryPO;
import com.avalon.jpcap.repository.service.RecordRepository;
import com.github.pagehelper.Page;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static com.avalon.jpcap.common.enums.ResultCodeEnum.PARAM_ERROR;

/**
 * @author DingHaoLun
 * @since 2023-05-15 14:31
 **/
@Repository
public class MjPromptModelRepositoryImpl implements RecordRepository<PromptModelPO, PromptModelQueryPO> {

    @Resource
    private PromptModelMapper mapper;

    @Override
    public Long addRecord(PromptModelPO po) {
        return mapper.insert(po);
    }

    @Override
    public Boolean updateRecord(PromptModelPO po) {
        if(Objects.isNull(po.getId())){
            throw new BusinessException(PARAM_ERROR.getCode(),"卡片记录id不能为空");
        }
        return mapper.updateById(po);
    }

    @Override
    public Boolean updateRecordList(List<PromptModelPO> promptModelPOS) {
        return null;
    }

    @Override
    public PromptModelPO queryRecordByCondition(PromptModelQueryPO queryPO) {
        if(Objects.isNull(queryPO.getId())){
            throw new BusinessException(PARAM_ERROR.getCode(),"卡片记录id不能为空");
        }
        return mapper.queryById(queryPO.getId());
    }

    @Override
    public List<PromptModelPO> queryRecordListByCondition(PromptModelQueryPO queryPO) {
        return mapper.queryPageByRank(queryPO.getUserId(), queryPO.getOrderByUsedTimesDesc(), queryPO.getOrderByLikeTimesDesc(),
                queryPO.getCurrentPage(), queryPO.getPageSize());
    }

    @Override
    public PagePO<PromptModelPO> queryPageRecordByCondition(PromptModelQueryPO queryPO) {
        Page<PromptModelPO> pageData = mapper.queryPageByRank(queryPO.getUserId(), queryPO.getOrderByUsedTimesDesc(), queryPO.getOrderByLikeTimesDesc(),
                queryPO.getCurrentPage(), queryPO.getPageSize());

        PagePO<PromptModelPO> pageResult = new PagePO<>();
        pageResult.setCurrentPage(pageData.getPageNum());
        pageResult.setPageSize(pageData.getPageSize());
        Long total = (Long) pageData.getTotal();
        pageResult.setTotalCount(total.intValue());
        pageResult.setData(pageData.getResult());

        return pageResult;
    }

    @Override
    public List<PromptModelPO> queryRecordListByIds(List<Long> Id) {
        //不提供
        return null;
    }
}