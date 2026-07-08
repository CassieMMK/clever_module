//package com.avalon.jpcap.domain;
//
//import com.avalon.jpcap.common.domain.BaseControllerDomain;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import javax.validation.constraints.NotBlank;
//import javax.validation.constraints.NotNull;
//
///**
// * 最高复活点VO
// *
// * @author DingHaoLun
// * @since 2023-01-30 21:24
// **/
//@Data
//@EqualsAndHashCode(callSuper = true)
//@AllArgsConstructor
//@NoArgsConstructor
//public class HighestRiseAgainVO extends BaseControllerDomain {
//
//    /**
//     * 最高关卡
//     */
//    @NotNull(message = "关卡不能为空")
//    private Integer highestTowerLevel;
//
//    /**
//     * 最高复活点坐标Json
//     */
//    @NotBlank(message = "坐标不能为空")
//    @NotNull(message = "坐标不能为空")
//    private String highestRiseAgainLoc;
//}