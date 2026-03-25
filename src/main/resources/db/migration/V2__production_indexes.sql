alter table messages add constraint uk_messages_client_message unique (chat_room_id, sender_id, client_message_id);
create index idx_message_status_message on message_status(message_id);
create index idx_user_status on users(status);
