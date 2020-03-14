create table feed (
id uuid default uuid_generate_v4(),
version int4 not null,
order_id int8 not null,
insert_time timestamp not null,
update_time timestamp,
link varchar not null unique,
primary key(id)
);