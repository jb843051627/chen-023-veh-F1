-- veh 重点营运车辆动态监控与报警处置管理 -- schema (chen-023)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_023

-- 册头：一册一行，进门只认这一道口子。active_key 仅在册码有效（未撤册）时等于册码，
-- 撤册后置 NULL——InnoDB 唯一索引允许多个 NULL，故同一册码撤后可重递，重递不覆旧册。
CREATE TABLE IF NOT EXISTS t_veh_alarm_book (
  id bigint NOT NULL COMMENT '主键',
  book_no varchar(64) NOT NULL COMMENT '月度报警处置核收册码',
  source_kind varchar(16) NOT NULL COMMENT '来路 AGENCY机构自录 CITY市级转递（两条来路一道门）',
  source_name varchar(128) DEFAULT NULL COMMENT '来路机构/地市名目',
  report_month varchar(7) DEFAULT NULL COMMENT '所报月度(YYYY-MM)',
  report_year int DEFAULT NULL COMMENT '进门所属报送年度（系统落，不让人填）',
  status int DEFAULT NULL COMMENT '册情形 0办理中 1核讫 2整册退回 3空册 4已撤销',
  nuclear_flag int DEFAULT '0' COMMENT '核讫标记 0未发 1已发（空册永不发）',
  total_rows int DEFAULT '0' COMMENT '册上原有行数（进门那一步点的）',
  stored_rows int DEFAULT '0' COMMENT '落进库里行数（办结那一回点算的快照）',
  returned_rows int DEFAULT '0' COMMENT '原样送回行数（办结那一回点算的快照）',
  row_snapshot longtext DEFAULT NULL COMMENT '办结时点的逐行结果快照(JSON)，供事后对账报差行',
  void_flag int DEFAULT '0' COMMENT '撤册标记 0在册 1已撤',
  active_key varchar(64) DEFAULT NULL COMMENT '有效册码=book_no，撤册置NULL，配合唯一键防二遍',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_alarm_book_active (active_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度报警处置核收册头';

-- 车辆名录：认车只认车辆营运代号；拿车牌对的、代号念不全的，按没对上办。
CREATE TABLE IF NOT EXISTS t_veh_truck (
  id bigint NOT NULL COMMENT '主键',
  item_code varchar(64) NOT NULL COMMENT '车辆营运代号（进门对车的唯一凭据）',
  plate_no varchar(32) DEFAULT NULL COMMENT '车牌号（只留档，不拿它对车）',
  truck_kind varchar(32) DEFAULT NULL COMMENT '车辆类别（客车/危货车等）',
  status int DEFAULT '0' COMMENT '名录情形 0在册 1注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_veh_truck_code (item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营运车辆代号名录';

CREATE TABLE IF NOT EXISTS t_veh_alarm_row (
  id bigint NOT NULL COMMENT '主键',
  book_id bigint DEFAULT NULL COMMENT '所属册头主键（行不许游到别册去）',
  batch_no varchar(64) DEFAULT NULL COMMENT '月度报警处置核收册码',
  row_no int DEFAULT NULL COMMENT '行次：进门时系统按落库先后排定，排定不再挪',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的营运车辆代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行仍未处置条数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本行采集报警条数',
  unit_kind varchar(32) DEFAULT NULL COMMENT '计量单位口径（条）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回',
  door_flag int DEFAULT NULL COMMENT '进门落点 1落进库里 2原样送回（进门那一步定盘，后日勾核不动它）',
  reject_reason varchar(255) DEFAULT NULL COMMENT '退回缘故（写在行上）',
  report_year int DEFAULT NULL COMMENT '进门所属报送年度',
  archived int DEFAULT '0' COMMENT '所在层 0现行名单 1往期（只朝旧册开）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_alarm_row_book (book_id),
  KEY idx_alarm_row_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报警处置核收行';

CREATE TABLE IF NOT EXISTS t_veh_bind_page (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '车辆联网清单页代号',
  node_no int DEFAULT NULL COMMENT '页所在层级序',
  site_id int DEFAULT NULL COMMENT '所属运输业户',
  site_no varchar(64) DEFAULT NULL COMMENT '所属业户代号',
  should_count int DEFAULT NULL COMMENT '应联项数',
  done_count int DEFAULT NULL COMMENT '已联项数',
  lack_count int DEFAULT NULL COMMENT '还差几项',
  content varchar(255) DEFAULT NULL COMMENT '随页交来的联网要件',
  status int DEFAULT NULL COMMENT '联网进展 0新录 1已联齐 2缺项',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆联网清单页';

CREATE TABLE IF NOT EXISTS t_veh_due_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '证照到期催办条目代号',
  due_at datetime DEFAULT NULL COMMENT '应开口那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '最可提前几日开口',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对业户的记要',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派出 2分不出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证照到期催办条目';

CREATE TABLE IF NOT EXISTS t_veh_ent (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '运输业户代号',
  site_name varchar(128) DEFAULT NULL COMMENT '业户全称',
  site_type varchar(32) DEFAULT NULL COMMENT '业户类别',
  road_name varchar(128) DEFAULT NULL COMMENT '属地那一路(省—市—县)',
  status int DEFAULT NULL COMMENT '名录情形 0在册 1已销户',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运输业户总名录';

CREATE TABLE IF NOT EXISTS t_veh_handle_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '严重报警处置单代号',
  stage int DEFAULT NULL COMMENT '当前步次 0..3（定级/知会/回告/归档）',
  status int DEFAULT NULL COMMENT '处置单走到的那一步 0未起 1在办 2已封尾',
  content varchar(255) DEFAULT NULL COMMENT '一段一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪步动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='严重报警处置单';

CREATE TABLE IF NOT EXISTS t_veh_move_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '移交执法核签单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（初核/复核/核准）',
  sign_mode int DEFAULT NULL COMMENT '同档算齐办法 0一名即可 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本档应落人数',
  sign_count int DEFAULT NULL COMMENT '本档已落印数',
  status int DEFAULT NULL COMMENT '核签情形 0在核 1已办结 2已挪回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='移交执法核签单';

CREATE TABLE IF NOT EXISTS t_veh_run_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '运行界次判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '界名',
  measure_kind varchar(32) DEFAULT NULL COMMENT '所量的是车速还是连续驾驶时长',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算数值(界册那一头的单位)',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线数值(界册那一头的单位)',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶数值(界册那一头的单位)',
  unit_kind varchar(32) DEFAULT NULL COMMENT '界册那一头的单位',
  eff_start datetime DEFAULT NULL COMMENT '起用那一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '界的情形 0在位 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运行界次判定线';

-- 初始档案数据：t_veh_ent 预置两条名录，id=0 在册、id=1 已销户，
-- 供界次判定与联网清单挂接时按在册口径取用。
INSERT IGNORE INTO t_veh_ent (id, site_no, site_name, site_type, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'YH00', '宁州顺达客运有限公司（道路旅客运输业户，在册）', '道路旅客运输业户', '宁川省—宁州市—临江县', 0, 0, 'seed', NOW()),
       (1, 'YH01', '临江通达危险货物运输站（停运后销户）', '危险货物运输业户', '宁川省—宁州市—临江县', 1, 0, 'seed', NOW());

-- 初始车辆营运代号名录：进门对车只认 item_code，plate_no 仅留档不得用于对车。
INSERT IGNORE INTO t_veh_truck (id, item_code, plate_no, truck_kind, status, del_flag, create_by, create_time)
VALUES (10, 'YY3201000001', '宁A10001', '道路旅客运输车辆', 0, 0, 'seed', NOW()),
       (11, 'YY3201000002', '宁A10002', '道路旅客运输车辆', 0, 0, 'seed', NOW()),
       (12, 'YY3201000003', '宁B20003', '危险货物运输车辆', 0, 0, 'seed', NOW()),
       (13, 'YY3201000004', '宁C30004', '危险货物运输车辆', 1, 0, 'seed', NOW());

