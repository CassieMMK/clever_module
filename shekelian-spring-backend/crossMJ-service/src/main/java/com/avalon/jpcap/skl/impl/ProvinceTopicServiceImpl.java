package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.OrganizationMapper;
import com.avalon.jpcap.infrastructure.skl.mapper.ProvinceTopicInfoMapper;
import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.repository.skl.OrganizationPO;
import com.avalon.jpcap.repository.skl.TopicCountByYearPO;
import com.avalon.jpcap.skl.DTO.ProvinceTopicInfoDTO;
import com.avalon.jpcap.skl.DTO.TopicCountByYearDTO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import com.avalon.jpcap.skl.converter.TopicCountByYearPOTODTOConverter;
import com.avalon.jpcap.skl.service.ProvinceTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public  class ProvinceTopicServiceImpl implements ProvinceTopicService {

    @Resource
    ProvinceTopicService provinceTopicService;
    //定义获取省课课题总数的的对象与方法
    //定义省课题对象
    @Autowired
    private ProvinceTopicInfoMapper provinceTopicInfoMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    /*
        1.返回省课题总数
         */
    @Override
    //这个注解可以让编译器帮你检查该方法是否真的重写了父类中的某个方法，从而避免一些潜在的错误。
    //final确保这个方法的行为不被子类改变，比如说不能让其他子类继承后改写这个方法：从返回总数改变为返回第一行
    public TotalNumsDTO countProvinceTopicsDto() {
        //1. 获取原始数据（备注：这里暂时不需要定义PO，因为没有必要，我统计的是行数，不属于某个表的列名）
        Integer count = provinceTopicInfoMapper.countProvinceTopics();

        //2. count 转 DTO （service层数据的形式应该已经转为dto）
        TotalNumsDTO totalNumsDTO = new TotalNumsDTO();
        totalNumsDTO.setCount(count != null ? count.intValue() : 0); // 确保 count 不为 null
        return totalNumsDTO;
    }

    /*
    2.历年省课题统计
     */
    @Override
    public List<TopicCountByYearDTO> getTopicCountByYeardto() {
        // 1. 获取原始PO数据（按年份统计的课题数量）
        List<TopicCountByYearPO> topicCounts = provinceTopicInfoMapper.getTopicCountByYear();
        // 输出日志，用于检查数据是否正确获取（可选）
        System.out.println("Topic Count PO List: " + topicCounts);

        // 2. 如果需要额外的数据处理，比如通过年份获取更多信息，
        //    可以在这里查询其他数据资源再封装成Map，用于转换过程中使用。
        //    示例中暂不需要额外查询的映射关系，故跳过此步。

        // 3. PO转换为DTO：调用 TopicCountByYearConverter 中的静态转换方法
        // 注意，这里的PO转DTO与在另一层的DTO转VO类名能不能一样，否则会出问题
        return TopicCountByYearPOTODTOConverter.convertToDTOList(topicCounts);
    }


    /*
     3.获取课题数量为前五的单位
     */
    // 定义根据id查名字的mapper对象使用
    @Override
    public List<ProvinceTopicInfoDTO> getProvincetop5InfoDto() {
        // 1. 获取原始PO数据
        List<OrganizationCountPO> organizationCounts = provinceTopicInfoMapper.findTop5OrganizationCounts();
        List<Integer> organizationIds = organizationCounts.stream()
                .map(OrganizationCountPO::getId)
                .collect(Collectors.toList());
        System.out.println("Org IDs:" + organizationIds);// 输出 authorIds，检查是否有值

        // 2. 批量查询单位信息PO (假设我们也需要获取单位名称等信息)
        Map<Integer, OrganizationPO> organizationNameMap = organizationMapper.selectUnit_NameByIDS(organizationIds);

        // 3. PO转换为DTO
        return organizationCounts.stream()
                .map(po -> convertToDTO(po, organizationNameMap))
                .collect(Collectors.toList());
    }

    private ProvinceTopicInfoDTO convertToDTO(OrganizationCountPO countPO, Map<Integer, OrganizationPO> nameMap) {
        ProvinceTopicInfoDTO dto = new ProvinceTopicInfoDTO();
        dto.setOrganizationId(countPO.getId());

        // 获取单位名称（假设我们可以从名称映射中获取）
        OrganizationPO organizationPO = nameMap != null ? nameMap.get(countPO.getId()) : null;
        dto.setOrganizationName(organizationPO != null ? organizationPO.getUnit_name() : "未知单位");

        dto.setTopicCount(countPO.getTopicCount());
        return dto;
    }
}
