package com.avalon.jpcap.converter;

import com.avalon.jpcap.domain.MjMemberDto;
import com.avalon.jpcap.domain.old.MjMemberInfoVO;
import com.avalon.jpcap.domain.old.UserInfoVO;
import com.avalon.jpcap.security.domain.dto.UserInfoDTO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author DingHaoLun
 * @since 2023-03-23 20:51
 **/
public class UserInfoConverter {
    public static UserInfoVO dto2Vo(UserInfoDTO dto, List<MjMemberDto> mjMemberDtoList){
        UserInfoVO vo = new UserInfoVO();
        //用户基本信息
        vo.setNickname(dto.getNickName());
        vo.setAvatar(dto.getHeadImgUrl());
        //用户会员信息
        List<MjMemberInfoVO> mjMemberInfoVOList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(mjMemberDtoList)){
            mjMemberDtoList.stream().map(mjMemberDto -> {
                MjMemberInfoVO mjMemberInfoVO = new MjMemberInfoVO();
                if(Boolean.TRUE.equals(mjMemberDto.getIsActivate())){
                    mjMemberInfoVO.setIsMember(mjMemberDto.getIsActivate());
                    mjMemberInfoVO.setMemberLevel(mjMemberDto.getMemberLevel());
                    mjMemberInfoVO.setCredit(mjMemberInfoVO.getCredit());
                    mjMemberInfoVO.setExpireTime(mjMemberDto.getExpireTime());
                    mjMemberInfoVO.setExt(mjMemberDto.getExt());
                } else {
                    mjMemberInfoVO.setIsMember(Boolean.FALSE);
                }
                mjMemberInfoVOList.add(mjMemberInfoVO);
                return null;
            }).collect(Collectors.toList());
        }
        vo.setMjMemberInfoVOList(mjMemberInfoVOList);
        vo.setExt(dto.getExt());
        return vo;
    }

    public static UserInfoDTO vo2Dto(UserInfoVO vo, Long userId){
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(userId);
        dto.setNickName(vo.getNickname());
        dto.setHeadImgUrl(vo.getAvatar());
        dto.setExt(vo.getExt());

        return dto;
    }
}