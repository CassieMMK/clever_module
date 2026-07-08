package com.avalon.jpcap.skl.service;

import com.avalon.jpcap.skl.DTO.SSCIInfoDTO;

import java.util.List;

public interface SSCIService {
    /*
     **查询排名前五的SSCI和HCI作者
     */
    List<SSCIInfoDTO> getSSCItop5InfoDto();
}
