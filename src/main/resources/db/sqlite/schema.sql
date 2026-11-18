CREATE TABLE IF NOT EXISTS channel_request (
    id TEXT PRIMARY KEY NOT NULL,
    subject TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
);
