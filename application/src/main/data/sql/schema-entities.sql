--
-- Copyright © 2016-2018 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--


CREATE TABLE IF NOT EXISTS admin_settings (
    id varchar(31) NOT NULL CONSTRAINT admin_settings_pkey PRIMARY KEY,
    json_value varchar,
    key varchar(255)
);

CREATE TABLE IF NOT EXISTS alarm (
    id varchar(31) NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    ack_ts bigint,
    clear_ts bigint,
    additional_info varchar,
    end_ts bigint,
    originator_id varchar(31),
    originator_type integer,
    propagate boolean,
    severity varchar(255),
    start_ts bigint,
    status varchar(255),
    tenant_id varchar(31),
    type varchar(255),
    alarm_count bigint
);

CREATE TABLE IF NOT EXISTS asset (
    id varchar(31) NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id varchar(31),
    name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id varchar(31) NOT NULL CONSTRAINT audit_log_pkey PRIMARY KEY,
    tenant_id varchar(31),
    customer_id varchar(31),
    entity_id varchar(31),
    entity_type varchar(255),
    entity_name varchar(255),
    user_id varchar(31),
    user_name varchar(255),
    action_type varchar(255),
    action_data varchar(1000000),
    action_status varchar(255),
    action_failure_details varchar(1000000)
);

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_type varchar(255),
  entity_id varchar(31),
  attribute_type varchar(255),
  attribute_key varchar(255),
  bool_v boolean,
  str_v varchar(10000000),
  long_v bigint,
  dbl_v double precision,
  last_update_ts bigint,
  CONSTRAINT attribute_kv_unq_key UNIQUE (entity_type, entity_id, attribute_type, attribute_key)
);

CREATE TABLE IF NOT EXISTS component_descriptor (
    id varchar(31) NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    actions varchar(255),
    clazz varchar UNIQUE,
    configuration_descriptor varchar,
    name varchar(255),
    scope varchar(255),
    search_text varchar(255),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS customer (
    id varchar(31) NOT NULL CONSTRAINT customer_pkey PRIMARY KEY,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    search_text varchar(255),
    state varchar(255),
    tenant_id varchar(31),
    title varchar(255),
    zip varchar(255)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id varchar(31) NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    configuration varchar(10000000),
    assigned_customers varchar(1000000),
    search_text varchar(255),
    tenant_id varchar(31),
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS device (
    id varchar(31) NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id varchar(31),
    type varchar(255),
    name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS device_credentials (
    id varchar(31) NOT NULL CONSTRAINT device_credentials_pkey PRIMARY KEY,
    credentials_id varchar,
    credentials_type varchar(255),
    credentials_value varchar,
    device_id varchar(31)
);

CREATE TABLE IF NOT EXISTS event (
    id varchar(31) NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    body varchar,
    entity_id varchar(31),
    entity_type varchar(255),
    event_type varchar(255),
    event_uid varchar(255),
    tenant_id varchar(31),
    CONSTRAINT event_unq_key UNIQUE (tenant_id, entity_type, entity_id, event_type, event_uid)
);

CREATE TABLE IF NOT EXISTS relation (
    from_id varchar(31),
    from_type varchar(255),
    to_id varchar(31),
    to_type varchar(255),
    relation_type_group varchar(255),
    relation_type varchar(255),
    additional_info varchar,
    CONSTRAINT relation_unq_key UNIQUE (from_id, from_type, relation_type_group, relation_type, to_id, to_type)
);

CREATE TABLE IF NOT EXISTS tb_user (
    id varchar(31) NOT NULL CONSTRAINT tb_user_pkey PRIMARY KEY,
    additional_info varchar,
    authority varchar(255),
    customer_id varchar(31),
    email varchar(255) UNIQUE,
    first_name varchar(255),
    last_name varchar(255),
    search_text varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS tenant (
    id varchar(31) NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    region varchar(255),
    search_text varchar(255),
    state varchar(255),
    title varchar(255),
    zip varchar(255)
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id varchar(31) NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    activate_token varchar(255) UNIQUE,
    enabled boolean,
    password varchar(255),
    reset_token varchar(255) UNIQUE,
    user_id varchar(31) UNIQUE
);

CREATE TABLE IF NOT EXISTS widget_type (
    id varchar(31) NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    alias varchar(255),
    bundle_alias varchar(255),
    descriptor varchar(1000000),
    name varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id varchar(31) NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    alias varchar(255),
    search_text varchar(255),
    tenant_id varchar(31),
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS rule_chain (
    id varchar(31) NOT NULL CONSTRAINT rule_chain_pkey PRIMARY KEY,
    additional_info varchar,
    configuration varchar(10000000),
    name varchar(255),
    first_rule_node_id varchar(31),
    root boolean,
    debug_mode boolean,
    search_text varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS rule_node (
    id varchar(31) NOT NULL CONSTRAINT rule_node_pkey PRIMARY KEY,
    rule_chain_id varchar(31),
    additional_info varchar,
    configuration varchar(10000000),
    type varchar(255),
    name varchar(255),
    debug_mode boolean,
    search_text varchar(255)
);

CREATE TABLE IF NOT EXISTS entity_view (
    id varchar(31) NOT NULL CONSTRAINT entity_view_pkey PRIMARY KEY,
    entity_id varchar(31),
    entity_type varchar(255),
    tenant_id varchar(31),
    customer_id varchar(31),
    type varchar(255),
    name varchar(255),
    keys varchar(10000000),
    start_ts bigint,
    end_ts bigint,
    search_text varchar(255),
    additional_info varchar
);
CREATE TABLE IF NOT EXISTS task (
    id varchar(31) NOT NULL CONSTRAINT task_pkey PRIMARY KEY,
	tenant_id varchar(31),
	customer_id varchar(31),
	process_user_id varchar(31),
	asset_id varchar(31),
	task_status varchar(31),
	task_kind varchar(31),
    originator_id varchar(31),
    originator_type varchar(255),
    name varchar(255),
	start_ts bigint,
	end_ts bigint,
	ack_ts bigint,
	clear_ts bigint,
	search_text varchar(255),
    additional_info varchar
);
CREATE TABLE IF NOT EXISTS patrol_record (
    id varchar(31) NOT NULL PRIMARY KEY,
	tenant_id varchar(31),
	customer_id varchar(31),
	originator_id varchar(31),
    originator_type varchar(255),
    recode_type varchar(255),
    info varchar
);
CREATE TABLE IF NOT EXISTS warnings_event_record (
  id varchar(31) NOT NULL PRIMARY KEY,
	tenant_id varchar(31),
	customer_id varchar(31),
	user_id varchar(31),
	asset_id varchar(31),
	record_ts bigint,
  record_type varchar(255),
  info varchar
);
CREATE TABLE IF NOT EXISTS ts_hour_value_statistic (
    entity_type varchar(255) NOT NULL,
    entity_id varchar(31) NOT NULL,
    ts bigint NOT NULL,
    customer_id varchar(31),
	tenant_id varchar(31),
    CONSTRAINT ts_hour_value_statistic_unq_key UNIQUE (entity_type, entity_id, ts)
);
CREATE OR REPLACE VIEW vassetattrkv AS
 SELECT attribute_kv.entity_type,
    attribute_kv.entity_id,
    attribute_kv.attribute_type,
    attribute_kv.attribute_key,
    attribute_kv.bool_v,
    attribute_kv.str_v,
    attribute_kv.long_v,
    attribute_kv.dbl_v,
    attribute_kv.last_update_ts,
    asset.id,
    asset.additional_info,
    asset.customer_id,
    asset.name,
    asset.search_text,
    asset.tenant_id,
    asset.type
   FROM attribute_kv,
    asset
  WHERE attribute_kv.entity_id::text = asset.id::text;



CREATE OR REPLACE VIEW device_attributes AS
 SELECT attribute_kv.entity_id,
    max(
        CASE attribute_kv.attribute_key
            WHEN 'ip'::text THEN attribute_kv.str_v
            ELSE NULL::character varying
        END::text) AS IP,
    max(
        CASE attribute_kv.attribute_key
            WHEN 'channel'::text THEN attribute_kv.str_v
            ELSE NULL::character varying
        END::text) AS channel,
    max(
        CASE attribute_kv.attribute_key
            WHEN 'measureid'::text THEN attribute_kv.str_v
            ELSE NULL::character varying
        END::text) AS measureid,
    max(
        CASE attribute_kv.attribute_key
            WHEN 'moniteritem'::text THEN attribute_kv.str_v
            ELSE NULL::character varying
        END::text) AS moniteritem,
	max(
	CASE attribute_kv.attribute_key
		WHEN 'devicename'::text THEN attribute_kv.str_v
		ELSE NULL::character varying
	END::text) AS devicename,
	max(
	CASE attribute_kv.attribute_key
		WHEN 'description'::text THEN attribute_kv.str_v
		ELSE NULL::character varying
	END::text) AS description,
	bool_or(
	CASE attribute_kv.attribute_key
		WHEN 'active'::text THEN attribute_kv.bool_v
		ELSE false::boolean
	END::boolean) AS active,
	max(
	CASE attribute_kv.attribute_key
		WHEN 'lastConnectTime'::text THEN attribute_kv.long_v
		ELSE null::bigint
	END::bigint) AS lastConnectTime	,
	max(
	CASE attribute_kv.attribute_key
		WHEN 'lastDisconnectTime'::text THEN attribute_kv.long_v
		ELSE null::bigint
	END::bigint) AS lastDisconnectTime
   FROM attribute_kv
  WHERE attribute_kv.entity_type::text = 'DEVICE'::text
  GROUP BY attribute_kv.entity_id;

CREATE OR REPLACE VIEW device_alarms AS
select asset.id as asset_id,alarm.severity,device_attributes.entity_id as device_id,device_attributes.moniteritem from asset
inner join relation on asset.id=relation.from_id and relation.to_type='DEVICE'
inner join alarm on relation.to_id=alarm.originator_id and alarm.status='ACTIVE_UNACK'
inner join device_attributes on alarm.originator_id=device_attributes.entity_id;



CREATE OR REPLACE VIEW deviceattrkv AS
 SELECT attribute_kv.entity_type,
    attribute_kv.entity_id,
    attribute_kv.attribute_type,
    attribute_kv.attribute_key,
    attribute_kv.bool_v,
    attribute_kv.str_v,
    attribute_kv.long_v,
    attribute_kv.dbl_v,
    attribute_kv.last_update_ts,
    device.additional_info,
    device.customer_id,
    device.type,
    device.name,
    device.search_text,
    device.tenant_id,
    device.id
   FROM attribute_kv,
    device
  WHERE attribute_kv.entity_id::text = device.id::text;

CREATE OR REPLACE VIEW asset_attributes AS
  SELECT attribute_kv.entity_id,
    max(
        CASE attribute_kv.attribute_key
            WHEN 'areaDivide'::text THEN attribute_kv.str_v
            ELSE NULL::character varying
        END::text) AS areadivide,
	max(
	  CASE attribute_kv.attribute_key
		  WHEN 'basicInfo'::text THEN attribute_kv.str_v
		  ELSE NULL::character varying
	  END::text) AS basicInfo,
	max(
	  CASE attribute_kv.attribute_key
		  WHEN 'cardInfo'::text THEN attribute_kv.str_v
		  ELSE NULL::character varying
	  END::text) AS cardInfo,
	max(
	  CASE attribute_kv.attribute_key
		  WHEN 'structureInfo'::text THEN attribute_kv.str_v
		  ELSE NULL::character varying
	  END::text) AS structureInfo
  FROM attribute_kv
  WHERE attribute_kv.entity_type::text = 'ASSET'::text
  GROUP BY attribute_kv.entity_id;