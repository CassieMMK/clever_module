package com.avalon.jpcap.infrastructure.impl.repo;

import com.avalon.jpcap.infrastructure.mapper.MjMemberMapper;
import com.avalon.jpcap.repository.po.MjMemberPO;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.service.RecordRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * mj会员落库服务
 *
 * @author DingHaoLun
 * @since 2023-04-17 16:12
 **/
@Repository
public class MjMemberRepositoryImpl implements RecordRepository<MjMemberPO, Long> {

    @Resource
    private MjMemberMapper mapper;

    @Override
    public Long addRecord(MjMemberPO mjMemberPO) {
        return mapper.insert(mjMemberPO);
    }

    @Override
    public Boolean updateRecord(MjMemberPO mjMemberPO) {
        mapper.update(mjMemberPO);
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateRecordList(List<MjMemberPO> mjMemberPOS) {
        return null;
    }

    @Override
    public MjMemberPO queryRecordByCondition(Long userId) {
        return null;
    }

    @Override
    public List<MjMemberPO> queryRecordListByCondition(Long userId) {
        return mapper.queryByUserId(userId);
    }

    @Override
    public PagePO<MjMemberPO> queryPageRecordByCondition(Long aLong) {
        return null;
    }

    @Override
    public List<MjMemberPO> queryRecordListByIds(List<Long> Id) {
        return null;
    }
}