alter table outbox_event
    modify id BINARY(16) not null;