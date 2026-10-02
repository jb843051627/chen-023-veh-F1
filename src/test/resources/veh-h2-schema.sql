-- H2（MySQL 兼容口径）测试库：列与 doc/schema/veh.sql 逐列对齐，仅用于 JUnit。
CREATE TABLE t_veh_alarm_book (
  id BIGINT NOT NULL PRIMARY KEY,
  book_no VARCHAR(64) NOT NULL,
  source_kind VARCHAR(16) NOT NULL,
  source_name VARCHAR(128),
  report_month VARCHAR(7),
  report_year INT,
  status INT,
  nuclear_flag INT DEFAULT 0,
  total_rows INT DEFAULT 0,
  stored_rows INT DEFAULT 0,
  returned_rows INT DEFAULT 0,
  row_snapshot CLOB,
  void_flag INT DEFAULT 0,
  active_key VARCHAR(64),
  del_flag INT DEFAULT 0,
  create_by VARCHAR(64),
  create_time DATETIME,
  update_by VARCHAR(64),
  update_time DATETIME,
  remark VARCHAR(500),
  CONSTRAINT uk_alarm_book_active UNIQUE (active_key)
);

CREATE TABLE t_veh_truck (
  id BIGINT NOT NULL PRIMARY KEY,
  item_code VARCHAR(64) NOT NULL,
  plate_no VARCHAR(32),
  truck_kind VARCHAR(32),
  status INT DEFAULT 0,
  del_flag INT DEFAULT 0,
  create_by VARCHAR(64),
  create_time DATETIME,
  update_by VARCHAR(64),
  update_time DATETIME,
  remark VARCHAR(500),
  CONSTRAINT uk_veh_truck_code UNIQUE (item_code)
);

CREATE TABLE t_veh_alarm_row (
  id BIGINT NOT NULL PRIMARY KEY,
  book_id BIGINT,
  batch_no VARCHAR(64),
  row_no INT,
  item_code VARCHAR(64),
  qty DECIMAL(12,2),
  hours_num DECIMAL(12,2),
  unit_kind VARCHAR(32),
  status INT,
  door_flag INT,
  reject_reason VARCHAR(255),
  report_year INT,
  archived INT DEFAULT 0,
  del_flag INT DEFAULT 0,
  create_by VARCHAR(64),
  create_time DATETIME,
  update_by VARCHAR(64),
  update_time DATETIME,
  remark VARCHAR(500)
);
CREATE INDEX idx_alarm_row_book ON t_veh_alarm_row (book_id);
CREATE INDEX idx_alarm_row_batch ON t_veh_alarm_row (batch_no);

INSERT INTO t_veh_truck (id, item_code, plate_no, truck_kind, status, del_flag)
VALUES (10, 'YY3201000001', '宁A10001', '道路旅客运输车辆', 0, 0),
       (11, 'YY3201000002', '宁A10002', '道路旅客运输车辆', 0, 0),
       (12, 'YY3201000003', '宁B20003', '危险货物运输车辆', 0, 0),
       (13, 'YY3201000004', '宁C30004', '危险货物运输车辆', 1, 0);
