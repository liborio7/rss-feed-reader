create table feed (
id uuid default uuid_generate_v4(),
version int4 not null,
order_id int8 not null,
insert_time int8 not null,
update_time int8,
link varchar not null unique,
primary key(id)
);