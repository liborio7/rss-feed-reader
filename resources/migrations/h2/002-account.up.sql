create table account (
id uuid,
version int4 not null,
order_id int8 not null,
insert_time timestamp not null,
update_time timestamp,
username varchar,
chat_id int4 not null unique,
primary key(id)
);