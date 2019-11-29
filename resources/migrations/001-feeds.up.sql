create table feeds (
id uuid default uuid_generate_v4(),
version int4 not null,
insert_time int8 not null,
update_time int8,
title varchar not null,
link varchar not null,
description varchar,
primary key(id)
);