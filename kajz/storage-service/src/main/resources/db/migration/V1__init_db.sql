CREATE TABLE file_uploads (
    id bigint not null primary key,
    owner_id uuid not null,
    filename varchar(255) not null,
    storage_path varchar(255) not null,
    mime_type varchar(255) not null,
    content_size bigint not null,
    e_tag varchar(255),
    access varchar(255) not null check ((access in ('PUBLIC','PRIVATE','PROTECTED'))),
    status varchar(255) not null check ((status in ('UPLOADING','UPLOADED','PROCESSING','VALIDATED','FAILED'))),
    metadata jsonb,
    completed_at timestampz,
    deleted boolean default false not null,
    created_at timestampz default current_timestamp not null,
    updated_at timestampz default current_timestamp not null,
    created_by varchar(50),
    updated_by varchar(50)
);