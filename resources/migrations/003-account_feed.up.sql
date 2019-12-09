create table account_feed (
id uuid default uuid_generate_v4(),
version int4 not null,
order_id int8 not null,
insert_time int8 not null,
update_time int8,
account_id uuid not null,
feed_id uuid not null,
primary key(id),
unique(account_id, feed_id)
);