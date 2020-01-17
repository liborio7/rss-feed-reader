create table feed_item (
id uuid default uuid_generate_v4(),
version int4 not null,
order_id int8 not null,
insert_time timestamp not null,
update_time timestamp,
feed_id uuid not null,
title varchar not null,
link varchar not null unique,
pub_time timestamp not null,
description varchar not null,
primary key(id)
);