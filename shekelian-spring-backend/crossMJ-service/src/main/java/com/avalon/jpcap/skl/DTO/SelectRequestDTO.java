package com.avalon.jpcap.skl.DTO;

import lombok.Data;

@Data
public class SelectRequestDTO {
    private String paperName;
    private String firstAuthor;
    private String organization;
    private String publishYear;
    private String researchField;
    private String paperType; // 默认CSCI
    private Integer page = 0;         // 默认第0页
    private Integer size = 10;        // 默认每页10条
}
