package com.avalon.jpcap.service;

import com.avalon.jpcap.common.enums.MjMemberActivateEnum;
import com.avalon.jpcap.common.utils.DateUtils;
import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.converter.MjMemberConverter;
import com.avalon.jpcap.domain.MjMemberDto;
import com.avalon.jpcap.repository.po.MjMemberPO;
import com.avalon.jpcap.repository.service.RecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI绘图服务国内会员权益查询
 * 1、普通用户可以包月包年和购买积分
 * 2、VIP用户同理，且服务可插队到最前面
 *
 * 一个用户根据上述2种类型最多同时拥有2个服务。有vip会员的时候默认优先使用vip会员服务。
 * 用户有包年和包月套餐的时候先使用套餐，套餐过期后再使用积分。
 *
 * @author DingHaoLun
 * @since 2023-04-17 16:02
 **/
@Service
@Slf4j
public class MjMemberService {

    @Resource(name = "mjMemberRepositoryImpl")
    private RecordRepository<MjMemberPO,Long> mjMemberPOLongRecordRepository;

    /**
     * 查询用户会员等级及有效性
     * @param userId 用户id
     */
    public List<MjMemberDto> queryMember(Long userId){
        //通过用户id查询到用户的会员信息（积分、包月、VIP）
        List<MjMemberPO> pos = mjMemberPOLongRecordRepository.queryRecordListByCondition(userId);
        return MjMemberConverter.pos2Dtos(pos);
    }

    /**
     * 用户充值积分(先查后写，考虑并发问题）
     * @param userId 用户id
     * @param memberLevel 会员等级（普通，VIP）
     * @param credit 购买积分
     */
    public void addCredit(Long userId, Integer memberLevel, Integer credit){
        List<MjMemberPO> oldPos = mjMemberPOLongRecordRepository.queryRecordListByCondition(userId);
        if(CollectionUtils.isEmpty(oldPos)){
            //插入新数据
            MjMemberPO po = new MjMemberPO();
            po.setUserId(userId);
            po.setMemberLevel(memberLevel);
            po.setCredit(credit);
            po.setIsActivate(MjMemberActivateEnum.VALID.getCode());
            mjMemberPOLongRecordRepository.addRecord(po);
            return;
        }

        //过滤会员等级的数据进行操作
        List<MjMemberPO> filteredPo = oldPos.stream().filter(po -> memberLevel.equals(po.getMemberLevel())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(filteredPo)){
            //插入新数据
            MjMemberPO po = new MjMemberPO();
            po.setUserId(userId);
            po.setMemberLevel(memberLevel);
            po.setCredit(credit);
            po.setIsActivate(MjMemberActivateEnum.VALID.getCode());
            mjMemberPOLongRecordRepository.addRecord(po);
        }else{
            //不为空的话只有一条数据，更新这个数据
            MjMemberPO oldPo = filteredPo.get(0);
            Integer oldCredit = oldPo.getCredit()!=null?oldPo.getCredit():0;
            Integer newCredit = oldCredit + credit;

            MjMemberPO po = new MjMemberPO();
            po.setUserId(userId);
            po.setCredit(newCredit);
            mjMemberPOLongRecordRepository.updateRecord(po);
        }
    }

    /**
     * 用户购买包月包年等套餐
     * @param userId 用户id
     * @param memberLevel 购买会员等级（普通，VIP）
     * @param days 购买天数
     */
    public void buyContract(Long userId, Integer memberLevel, Integer days) throws Exception {
        List<MjMemberPO> oldPos = mjMemberPOLongRecordRepository.queryRecordListByCondition(userId);
        if(CollectionUtils.isEmpty(oldPos)){
            //插入新数据
            MjMemberPO po = new MjMemberPO();
            po.setUserId(userId);
            po.setMemberLevel(memberLevel);
            po.setExpireTime(DateUtils.dateCalculate(new Date(), Calendar.DAY_OF_MONTH, days));
            po.setIsActivate(MjMemberActivateEnum.VALID.getCode());
            mjMemberPOLongRecordRepository.addRecord(po);
            return;
        }

        //过滤会员等级的数据进行操作
        List<MjMemberPO> filteredPo = oldPos.stream().filter(po -> memberLevel.equals(po.getMemberLevel())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(filteredPo)){
            //插入新数据
            MjMemberPO po = new MjMemberPO();
            po.setUserId(userId);
            po.setMemberLevel(memberLevel);
            po.setExpireTime(DateUtils.dateCalculate(new Date(), Calendar.DAY_OF_MONTH, days));
            po.setIsActivate(MjMemberActivateEnum.VALID.getCode());
            mjMemberPOLongRecordRepository.addRecord(po);
        }else{
            //不为空的话只有一条数据，更新这个数据
            MjMemberPO oldPo = filteredPo.get(0);
            Date oldDate = oldPo.getExpireTime();
            Date newDate;
            if(oldDate==null || DateUtils.compareTime(new Date(),oldDate)>=0){
                //若没有过期时间或已过期，则直接从今天开始增加权益
                newDate = DateUtils.dateCalculate(new Date(), Calendar.DAY_OF_MONTH, days);
            } else {
                //尚未过期，继续续费
                newDate = DateUtils.dateCalculate(oldDate, Calendar.DAY_OF_MONTH, days);
            }
            log.info("用户id={}，续费后新的过期时间为{}", JsonUtils.toJson(userId),JsonUtils.toJson(newDate));
            MjMemberPO po = new MjMemberPO();
            po.setUserId(userId);
            po.setExpireTime(newDate);
            mjMemberPOLongRecordRepository.updateRecord(po);
        }
    }


}