create table job (
id uuid default uuid_generate_v4(),
version int4 not null,
order_id int8 not null,
insert_time timestamp not null,
update_time timestamp,
name varchar not null,
execution_payload json,
last_execution_payload json,
last_execution_ms int4,
description varchar,
enabled boolean not null default true,
locked boolean not null default false
primary key(id)
);

insert into job (version, order_id, insert_time, name, description)
values (0, extract(epoch from now()), now(), "feed_item", "Fetch feed items")