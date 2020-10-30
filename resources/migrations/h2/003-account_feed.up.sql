create table account_feed (
id uuid,
version int4 not null,
order_id int8 not null,
insert_time timestamp not null,
update_time timestamp,
account_id uuid not null,
feed_id uuid not null,
primary key(id),
unique(account_id, feed_id)
);