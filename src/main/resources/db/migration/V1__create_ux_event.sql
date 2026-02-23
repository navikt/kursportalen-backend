create table if not exists ux_event (
    id bigserial primary key,
    user_id text,
    action text not null,
    path text not null,
    target text,
    metadata jsonb,
    created_at timestamptz not null default now()
);
