package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.SelectRequestDTO;
import com.avalon.jpcap.skl.request.SelectRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Component
public class SelectRequestConverter {
    public SelectRequestDTO convertToDto(SelectRequest request) {
        SelectRequestDTO converted = new SelectRequestDTO();

        // 处理paperName
        converted.setPaperName(StringUtils.hasText(request.getPaperName()) ?
                request.getPaperName().trim() : null);

        // 处理firstAuthor
        converted.setFirstAuthor(StringUtils.hasText(request.getFirstAuthor()) ?
                request.getFirstAuthor().trim() : null);

        // 处理organization
        converted.setOrganization(StringUtils.hasText(request.getOrganization()) ?
                request.getOrganization().trim() : null);

        // 处理publishYear
        if (StringUtils.hasText(request.getPublishYear())) {
            try {
                Integer year = Integer.parseInt(request.getPublishYear().trim());
                if (year < 1900 || year > 2100) {
                    throw new IllegalArgumentException("发表年份必须在1900-2100之间");
                }
                converted.setPublishYear(request.getPublishYear().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("发表年份必须是有效的数字");
            }
        } else {
            converted.setPublishYear(null);
        }

        // 处理researchField
        converted.setResearchField(StringUtils.hasText(request.getResearchField()) ?
                request.getResearchField().trim() : null);

        // 处理paperType
        String paperType = StringUtils.hasText(request.getPaperType()) ?
                request.getPaperType().trim().toUpperCase() : "CSCI";
        if (!Arrays.asList("CSCI", "SSCI&HCI", "ALL").contains(paperType)) {
            throw new IllegalArgumentException("论文类型必须是CSCI、SSCI、HCI或ALL");
        }
        converted.setPaperType(paperType);

        // 处理分页参数
        if (request.getPage() != null && request.getPage() < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        converted.setPage(request.getPage() != null ? request.getPage() : 0);

        if (request.getSize() != null) {
            if (request.getSize() <= 0) {
                throw new IllegalArgumentException("每页大小必须大于0");
            }
            if (request.getSize() > 100) {
                throw new IllegalArgumentException("每页大小不能超过100");
            }
            converted.setSize(request.getSize());
        } else {
            converted.setSize(10);
        }

        return converted;
    }
}