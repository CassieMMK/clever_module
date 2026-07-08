package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.SelectDTO;
import com.avalon.jpcap.skl.vo.PaginationVO;
import com.avalon.jpcap.skl.vo.SelectVO;
import org.springframework.stereotype.Component;

@Component
public class SelectVOConverter {

    public SelectVO convertToVO(SelectDTO dto) {
        if (dto == null) {
            return null;
        }

        SelectVO vo = new SelectVO();
        vo.setName("论文查询结果");

        // 转换CSCI结果 - 增加空值检查
        if (dto.getCsciResult() != null && !dto.getCsciResult().getRecords().isEmpty()) {
            vo.setCsciList(CSCIVOConverter.convertList(dto.getCsciResult().getRecords()));
        }

        // 转换SSCI结果 - 增加空值检查
        if (dto.getSsciResult() != null && !dto.getSsciResult().getRecords().isEmpty()) {
            vo.setSsciList(SSCIVOConverter.convertList(dto.getSsciResult().getRecords()));
        }

        // 设置分页信息 - 增加空值检查
        if (dto.getPagination() != null) {
            PaginationVO pagination = new PaginationVO(
                    dto.getPagination().getCurrent(),
                    dto.getPagination().getPageSize(),
                    dto.getPagination().getTotal()
            );
            vo.setPagination(pagination);
        }

        return vo;
    }
}
