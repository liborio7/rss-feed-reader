create table feed_item (
id uuid default uuid_generate_v4(),
version int4 not null,
order_id int8 not null,
insert_time int8 not null,
update_time int8,
feed_id uuid not null,
title varchar not null,
link varchar not null,
pub_time int8 not null,
description varchar not null,
primary key(id)
);