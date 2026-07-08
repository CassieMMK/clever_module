-- 学科信息表
CREATE TABLE discipline_info
(
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '学科ID，主键，自增',
    disci_name VARCHAR(255) NOT NULL COMMENT '学科名称'
);

-- 省课题类别表
CREATE TABLE province_topic_category
(
    id       INT PRIMARY KEY AUTO_INCREMENT COMMENT '省课题类别ID，主键，自增',
    type     VARCHAR(255) NOT NULL COMMENT '课题类型',
    category VARCHAR(255) NOT NULL COMMENT '课题类别',
    score    INT          NOT NULL COMMENT '权重分数'
);

-- 机关单位信息表
CREATE TABLE organization_info
(
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '机关单位ID，主键，自增',
    unit_name    VARCHAR(255) NOT NULL COMMENT '单位名称',
    unit_address VARCHAR(255) NOT NULL COMMENT '单位地址'
);

-- 人员信息表
CREATE TABLE person_info
(
    id     INT PRIMARY KEY AUTO_INCREMENT COMMENT '人员ID，主键，自增',
    org_id INT          NOT NULL COMMENT '单位ID，外键关联organization_info表的id',
    name   VARCHAR(255) NOT NULL COMMENT '人员姓名',
    FOREIGN KEY (org_id) REFERENCES organization_info (id)
);

-- 省课题信息表
CREATE TABLE province_topic_info
(
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '省课题信息ID，主键，自增',
    name          VARCHAR(255) NOT NULL COMMENT '课题名称',
    header_id     INT          NOT NULL COMMENT '负责人ID，外键关联person_info表的id',
    topic_id      INT          NOT NULL COMMENT '课题类别ID，外键关联province_topic_category表的id',
    discipline_id INT          NOT NULL COMMENT '学科ID，外键关联discipline_info表的id',
    org_id        INT          NOT NULL COMMENT '机关单位ID，外键关联organization_info表的id',
    create_time   INT          NOT NULL COMMENT '创建年份',
    start_time    INT          NOT NULL COMMENT '开始年份',
    end_time      INT          NOT NULL COMMENT '结束年份',
    FOREIGN KEY (header_id) REFERENCES person_info (id),
    FOREIGN KEY (topic_id) REFERENCES province_topic_category (id),
    FOREIGN KEY (discipline_id) REFERENCES discipline_info (id),
    FOREIGN KEY (org_id) REFERENCES organization_info (id)
);

-- 出版单位信息表
CREATE TABLE publish_unit_info
(
    id               INT PRIMARY KEY AUTO_INCREMENT COMMENT '出版单位ID，主键，自增',
    pub_unit_name    VARCHAR(255) COMMENT '单位名称',
    pub_unit_address VARCHAR(255) COMMENT '单位地址'
);

-- 社科学术奖等级表
CREATE TABLE social_science_aware_level
(
    id    INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科学术奖等级ID，主键，自增',
    level VARCHAR(50) COMMENT '获奖等级',
    score INT COMMENT '权重分数'
);

-- 社科团队信息表
CREATE TABLE social_science_group_info
(
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科团队ID，主键，自增',
    org_id        INT COMMENT '单位ID',
    type_id       INT COMMENT '团队类型ID',
    person_id     INT COMMENT '负责人ID',
    discipline_id INT COMMENT '学科ID',
    group_name    VARCHAR(255) COMMENT '团队名称',
    start_time    INT COMMENT '开始时间，存储为整数',
    end_time      INT COMMENT '结束时间，存储为整数',
    other_type    VARCHAR(255) COMMENT '其他类型',
    score         INT COMMENT '权重分数'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='社科团队信息表';

-- 社科团队类型表
CREATE TABLE social_science_group_type
(
    id        INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科团队类型ID，主键，自增',
    type_name VARCHAR(255) COMMENT '类型名称'
);

-- 社科学术奖信息表
CREATE TABLE social_science_aware_info
(
    id              INT PRIMARY KEY AUTO_INCREMENT COMMENT '社科学术奖信息ID，主键，自增',
    org_id          INT COMMENT '人员单位ID',
    pub_unit_id     INT COMMENT '出版单位ID',
    level_id        INT COMMENT '获奖等级ID',
    person_id       INT COMMENT '负责人ID',
    discipline_id   INT COMMENT '学科ID',
    obj_name        VARCHAR(255) COMMENT '项目名称',
    session         INT COMMENT '届次',
    document_number VARCHAR(255) COMMENT '档号'
);

-- 创建国家社科奖信息表
CREATE TABLE national_social_science_aware_info
(
    id            INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增',
    org_id        INT COMMENT '人员单位ID',
    type_id       INT COMMENT '获奖类型ID',
    categorie_id  INT COMMENT '获奖类别ID',
    person_id     INT COMMENT '负责人ID',
    discipline_id INT COMMENT '学科ID',
    obj_name      VARCHAR(255) COMMENT '项目名称',
    score         INT COMMENT '分数',
    year          INT COMMENT '获奖年份',
    start_time    INT COMMENT '开始时间',
    end_time      INT COMMENT '结束时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='国家社科奖信息表';

-- 创建国社科奖类型表
CREATE TABLE nat_social_science_group_type
(
    id        INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增',
    type_name VARCHAR(255) COMMENT '类型名称'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='国社科奖类型表';

-- 创建国社科奖类别表
CREATE TABLE nat_social_science_group_categorie
(
    id             INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增',
    categorie_name VARCHAR(255) COMMENT '类别名称'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='国社科奖类别表';

-- 创建CSSCI论文数据库表

CREATE TABLE CSSCI_info
(
    id              INT PRIMARY KEY COMMENT '主键',
    first_author_id INT COMMENT '一作',
    journal_id      INT COMMENT '期刊',
    organization    MEDIUMTEXT COMMENT '机构',
    addr_id         VARCHAR(50) COMMENT '地址代号',
    discipline_id   VARCHAR(50) COMMENT '学科代号',
    clc_id          MEDIUMTEXT COMMENT 'clc',
    author_list     MEDIUMTEXT COMMENT '作者列表',
    obj_name        MEDIUMTEXT COMMENT '论文名',
    year            VARCHAR(50) COMMENT '年份',
    keywords        MEDIUMTEXT COMMENT '关键词',
    funding         MEDIUMTEXT COMMENT '基金方',
    sno_id          VARCHAR(50) COMMENT '序号',
    score           VARCHAR(50) COMMENT '分数（暂定含义）',
    research_direct VARCHAR(50) COMMENT '研究方向',
    thesis_type     VARCHAR(50) COMMENT '论文主题'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='CSSCI论文表';

-- 创建SSCI_AND_HCI论文表

CREATE TABLE SSCI_AND_HCI_info
(
    id                  INT PRIMARY KEY COMMENT '主键',
    first_author_id     INT COMMENT '一作',
    co_author_id        INT COMMENT '通讯作者',
    journal_id          INT COMMENT '期刊',
    thesis_type_id      INT COMMENT '主题类型',
    organization        MEDIUMTEXT COMMENT '机构',
    addr                MEDIUMTEXT COMMENT '地址',
    author_list         MEDIUMTEXT COMMENT '作者列表',
    obj_name            MEDIUMTEXT COMMENT '论文名',
    year                INT COMMENT '年份',
    keywords            MEDIUMTEXT COMMENT '关键词',
    full_paper_link     MEDIUMTEXT COMMENT '全文引用路径',
    funding             MEDIUMTEXT COMMENT '基金方',
    days_use_count_180  INT COMMENT '180引用',
    days_use_count      INT COMMENT '总引用',
    highly_cited_status INT COMMENT '是否高频引用',
    research_areas      VARCHAR(500) COMMENT '研究主题'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSCI和HCI信息表';


-- 创建SSCI_AND_HC_thesis_type_info表
CREATE TABLE SSCI_AND_HC_thesis_type_info
(
    id   INT PRIMARY KEY COMMENT '主键',
    type VARCHAR(255) COMMENT '主题类型，关联SSCI表thesis_type_id'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='SSCI和HCI论文类型信息表';

-- 创建journal_info表
CREATE TABLE journal_info
(
    id           INT PRIMARY KEY COMMENT '主键',
    journal_name VARCHAR(255) COMMENT '期刊名'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='期刊信息表';

-- 创建paper_person_info表

CREATE TABLE paper_person_info
(
    id     INT PRIMARY KEY COMMENT '主键',
    org_id INT COMMENT '人员的机构',
    name   VARCHAR(255) COMMENT '人员名称'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='论文作者表';