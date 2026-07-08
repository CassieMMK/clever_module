package com.avalon.jpcap.skl;

import com.avalon.jpcap.common.result.HttpResult;
import com.avalon.jpcap.infrastructure.skl.mapper.PersonMapper;
import com.avalon.jpcap.skl.DTO.*;
import com.avalon.jpcap.skl.converter.*;
import com.avalon.jpcap.skl.impl.ShekeServiceImpl;
import com.avalon.jpcap.skl.request.SelectRequest;
import com.avalon.jpcap.skl.request.TopNRequest;
import com.avalon.jpcap.skl.service.*;
import com.avalon.jpcap.skl.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@Validated
@RequestMapping("/sheke/dataview/")
@Api(tags = "社科联服务接口")
public class ShekeController {

    @Resource
    private ShekeService shekeService;

    @Autowired
    private ShekeServiceImpl shekeServiceImpl;

    @Autowired
    private CSCIService csciService;

    @Autowired
    private CSCIVOConverter csciVOConverter;

    @Autowired
    private SSCIService ssciService;

    @Autowired
    private SSCIVOConverter ssciVOConverter;

    @Autowired
    // 数据转换器
    private TotalNumsVOConverter totalNumsVOConverter;

    @Autowired
    private ProvinceTopicService provinceTopicService;

    @Autowired
    private ProvinceTopicRankConverter provinceTopicRankConverter;

    @Autowired
    private CountrySocialScienceAwareService countrySocialScienceAwareService;

    @Autowired
    private CountryTopicRankConverter countryTopicRankConverter;

    @Autowired
    private SocialScienceAwareService socialScienceAwareService;

    @Autowired
    private SocialScienceGroupService socialScienceGroupService;

    @Autowired
    private TopicCountByYearConverter topicCountByYearConverter;
    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private SelectService selectService;
    @Autowired
    private KeywordsVOConverter keywordsVOConverter;
    @Autowired
    private  SelectRequestConverter selectRequestConverter;
    @Autowired
    private  SelectVOConverter selectVOConverter;


    @GetMapping("/provin_topics")
    @Operation(summary = "省课题总数", description = "省课题总数")
    public HttpResult<TotalNumsVO> getProvincesTopicCount() {
        // 获取DTO数据
        TotalNumsDTO dto = provinceTopicService.countProvinceTopicsDto();
        // DTO-->VO
        TotalNumsVO vo = totalNumsVOConverter.convertToVO(dto);
        // 返回成功响应，包含转换后的数据
        return HttpResult.success(vo);

    }

    @GetMapping("/provin_topics_statistic_by_year")
    @Operation(summary = "历年省课题统计", description = "历年省课题统计(按年份)")
    public HttpResult<List<TopicCountByYearVO>> statisticProvinTopicsByYear() {
        // 获取DTO数据
        List<TopicCountByYearDTO> dtos = provinceTopicService.getTopicCountByYeardto();

        // 将DTO转换为VO
        List<TopicCountByYearVO> vos = topicCountByYearConverter.convertToVOList(dtos);

        return HttpResult.success(vos);
    }

    @GetMapping("/province_topic_top_org")
    @Operation(summary = "省课课题数量前五的机关单位", description = "获取课题数量最多的前五位机关单位")
    public HttpResult<List<ProvinceTopicTopOrganizationVO>> getTopOrgOfProviceTopic() {
        // 获取 DTO 数据
        List<ProvinceTopicInfoDTO> dtos = provinceTopicService.getProvincetop5InfoDto();

        // 将 DTO 转换为 VO
        List<ProvinceTopicTopOrganizationVO> vos = provinceTopicRankConverter.convertToVOList(dtos);

        // 返回成功响应，包含转换后的数据
        return HttpResult.success(vos);
    }


    @GetMapping("/country_topics")
    @Operation(summary = "国家社科课题总数", description = "国社科课题总数")
    public HttpResult<TotalNumsVO> getCountryTopicsCount() {
        // 获取DTO数据
        TotalNumsDTO dto = countrySocialScienceAwareService.countCountryTopicsDto();
        // DTO-->VO
        TotalNumsVO vo = totalNumsVOConverter.convertToVO(dto);
        // 返回成功响应，包含转换后的数据
        return HttpResult.success(vo);
    }

    @GetMapping("national_rewards_statistic")
    @ApiOperation("历年国社科奖统计")
    public HttpResult<List<TopicCountByYearVO>> nationalRewardsStatistic() {
        List<TopicCountByYearDTO> dto = countrySocialScienceAwareService.national_rewards_statistic();
        List<TopicCountByYearVO> vo = topicCountByYearConverter.convertToVOList(dto);
        return HttpResult.success(vo);
    }

    @GetMapping("/country_topic_top_org")
    @Operation(summary = "国社科获奖数量前五的机关单位", description = "获奖课题数量最多的前五位机关单位")
    public HttpResult<List<CountryTopicTopOrganizationVO>> getTopOrgOfCountryTopic() {
        // 获取 DTO 数据
        List<CountryTopicInfoDTO> dtos = countrySocialScienceAwareService.getCountrytop5InfoDto();

        // 将 DTO 转换为 VO
        List<CountryTopicTopOrganizationVO> vos = countryTopicRankConverter.convertToVOList(dtos);

        // 返回成功响应，包含转换后的数据
        return HttpResult.success(vos);
    }

    @GetMapping("/social_science_awares")
    @Operation(summary = "社会科学奖获奖总数", description = "社会科学奖获奖总数")
    public HttpResult<TotalNumsVO> getSocialScienceAwaresCount() {
        // 获取DTO数据
        TotalNumsDTO dto = socialScienceAwareService.countSocialScienceAwareDto();
        // DTO-->VO
        TotalNumsVO vo = totalNumsVOConverter.convertToVO(dto);
        // 返回成功响应，包含转换后的数据
        return HttpResult.success(vo);

    }

    @GetMapping("/sheke_teams_num")
    @Operation(summary = "四川社科团队数量", description = "四川社科团队数量")
    public HttpResult<TotalNumsVO> getSocialScienceGroupNums() {
        // 获取DTO数据
        TotalNumsDTO dto = socialScienceGroupService.countSocialScienceGroupDto();
        // DTO-->VO
        TotalNumsVO vo = totalNumsVOConverter.convertToVO(dto);
        // 返回成功响应，包含转换后的数据
        return HttpResult.success(vo);
    }

    @GetMapping("/SSCI_total_nums")
    @ApiOperation("SSCI和HCI论文数量")
    public HttpResult getSSCITotalNums() {
        TotalNumsDTO dto = shekeServiceImpl.ssciTotalNums();
        TotalNumsVO vo = totalNumsVOConverter.convertToVO(dto);
        return HttpResult.success(dto);
    }

    @GetMapping("/SSCI-top-authors")
    @Operation(summary = "SSCI论文数量前五的作者", description = "获取论文数量最多的前五位作者")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功获取数据",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CSCIAuthorVO.class)))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public HttpResult<List<SSCIAuthorVO>> getSSCITopAuthors() {
        List<SSCIInfoDTO> dtos = ssciService.getSSCItop5InfoDto();
        List<SSCIAuthorVO> vos = SSCIVOConverter.convertToVOList(dtos);
        return HttpResult.success(vos);
    }

    @GetMapping("/CSCI_total_nums")
    @ApiOperation("CSCI论文数量")
    public HttpResult getCSCITotalNums() {
        TotalNumsDTO dto = shekeServiceImpl.csciTotalNums();
        TotalNumsVO vo = totalNumsVOConverter.convertToVO(dto);
        return HttpResult.success(vo);
    }

    @GetMapping("/CSCI-top-authors")
    @Operation(summary = "CSCI论文数量前五的作者", description = "获取论文数量最多的前五位作者")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功获取数据",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CSCIAuthorVO.class)))),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public HttpResult<List<CSCIAuthorVO>> getCSCITopAuthors() {
        List<CSCIInfoDTO> dtos = csciService.getCSCItop5InfoDto();
        List<CSCIAuthorVO> vos = CSCIVOConverter.convertToVOList(dtos);
        return HttpResult.success(vos);
    }

    @PostMapping("/search")
    @ApiOperation(value = "论文查询", notes = "根据条件查询CSCI/SSCI论文")
    public HttpResult<SelectVO> searchPapers(@RequestBody SelectRequest request) {
        // 转换请求参数
        SelectRequestDTO convertedRequest = selectRequestConverter.convertToDto(request);

        // 调用Service查询
        SelectDTO resultDto = selectService.selectPapers(convertedRequest);

        // 转换为VO
        SelectVO resultVo = selectVOConverter.convertToVO(resultDto);

        return HttpResult.success(resultVo);
    }


    @GetMapping("/word_cloud_by_keywords_CSSCI")
    @ApiOperation("省课题词云分析（获取全部）")
    public HttpResult<List<KeywordsVO>> getWordCloudByKeywords() {
        List<KeywordsDTO> keywordDTOs = csciService.getKeywordStatistics();
        List<KeywordsVO> keywordsVOS = keywordsVOConverter.convertToVOList(keywordDTOs);
        return HttpResult.success(keywordsVOS);
    }

    /**
     * 新增的 POST 方法，用于获取 Top N 关键词
     */
    @PostMapping("/word_cloud_by_top_n_keywords_CSSCI") // 新的路径
    @ApiOperation("省课题词云分析(获取Top N)")
    public HttpResult<List<KeywordsVO>> getWordCloudByTopNKeywordsCSSCI(
            @Valid @RequestBody TopNRequest request // 使用 @Valid 触发 TopNRequest 中的校验
    ) {
        int n = request.getN();
        // logger.info("POST /word_cloud_by_top_n_keywords_CSSCI called with n={}", n);

        // 1. 调用服务层获取 Top N 的 DTO 列表
        List<KeywordsDTO> topNKeywordDTOs = csciService.getTopNKeywordStatistics(n);

        // 2. 将 DTO 列表转换为 VO 列表
        List<KeywordsVO> topNKeywordsVOS = keywordsVOConverter.convertToVOList(topNKeywordDTOs);

        // logger.info("Returning top {} keywords.", topNKeywordsVOS.size());
        // 3. 使用 HttpResult 包装并返回
        return HttpResult.success(topNKeywordsVOS);
    }

    @GetMapping("sheke_members_connection")
    @ApiOperation("社科人员关联分析")
    public HttpResult getShekeMembersConnection() {
        return null;
    }


}
