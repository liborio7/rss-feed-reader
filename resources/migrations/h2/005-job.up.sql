create table job (
id uuid,
version int4 not null,
order_id int8 not null,
insert_time timestamp not null,
update_time timestamp,
name varchar not null unique,
execution_payload blob,
last_execution_payload blob,
last_execution_ms int4,
description varchar,
enabled boolean not null default true,
locked boolean not null default false,
primary key(id)
);