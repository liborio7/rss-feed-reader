create table feed (
id uuid,
version int not null,
order_id bigint not null,
insert_time timestamp not null,
update_time timestamp,
link varchar not null unique,
primary key(id)
);