create table users (
    id binary(16) primary key,
    username varchar(50) not null unique,
    display_name varchar(100) not null,
    password_hash varchar(255) not null,
    status varchar(20) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table user_devices (
    id binary(16) primary key,
    user_id binary(16) not null,
    device_id varchar(100) not null,
    public_key text not null,
    key_algorithm varchar(50) not null,
    key_version int not null,
    last_seen_at datetime(6),
    created_at datetime(6) not null,
    constraint fk_user_devices_user foreign key (user_id) references users(id)
);

create table chat_rooms (
    id binary(16) primary key,
    type varchar(20) not null,
    title varchar(255),
    created_by binary(16) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_chat_rooms_created_by foreign key (created_by) references users(id)
);

create table chat_members (
    id binary(16) primary key,
    chat_room_id binary(16) not null,
    user_id binary(16) not null,
    role varchar(20) not null,
    muted_until datetime(6),
    joined_at datetime(6) not null,
    left_at datetime(6),
    unique key uk_chat_member (chat_room_id, user_id),
    constraint fk_chat_members_chat_room foreign key (chat_room_id) references chat_rooms(id),
    constraint fk_chat_members_user foreign key (user_id) references users(id)
);

create table messages (
    id binary(16) primary key,
    chat_room_id binary(16) not null,
    sender_id binary(16) not null,
    type varchar(20) not null,
    cipher_text longtext not null,
    encrypted_key longtext not null,
    nonce varchar(255) not null,
    algorithm varchar(50) not null,
    metadata json,
    client_message_id varchar(100),
    created_at datetime(6) not null,
    edited_at datetime(6),
    deleted_at datetime(6),
    constraint fk_messages_chat_room foreign key (chat_room_id) references chat_rooms(id),
    constraint fk_messages_sender foreign key (sender_id) references users(id)
);

create table message_status (
    id binary(16) primary key,
    message_id binary(16) not null,
    user_id binary(16) not null,
    status varchar(20) not null,
    status_at datetime(6) not null,
    unique key uk_message_status (message_id, user_id),
    constraint fk_message_status_message foreign key (message_id) references messages(id),
    constraint fk_message_status_user foreign key (user_id) references users(id)
);

create table message_attachments (
    id binary(16) primary key,
    message_id binary(16) not null,
    file_name varchar(255) not null,
    content_type varchar(100) not null,
    file_size bigint not null,
    storage_key varchar(500) not null,
    checksum varchar(255),
    created_at datetime(6) not null,
    constraint fk_message_attachments_message foreign key (message_id) references messages(id)
);

create table user_blocks (
    id binary(16) primary key,
    blocker_id binary(16) not null,
    blocked_id binary(16) not null,
    created_at datetime(6) not null,
    unique key uk_user_blocks (blocker_id, blocked_id),
    constraint fk_user_blocks_blocker foreign key (blocker_id) references users(id),
    constraint fk_user_blocks_blocked foreign key (blocked_id) references users(id)
);

create index idx_messages_chat_created_at on messages(chat_room_id, created_at desc);
create index idx_message_status_user_status on message_status(user_id, status);
create index idx_chat_members_user on chat_members(user_id);
create index idx_user_devices_user on user_devices(user_id);
create index idx_messages_client_message_id on messages(client_message_id);
