CREATE TABLE admin_users
(
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE videos
(
    id          UUID PRIMARY KEY,
    name        TEXT    NOT NULL,
    created_at  TIMESTAMP        DEFAULT NOW(),
    uploaded_at TIMESTAMP NULL,
    status      TEXT    NOT NULL,
    is_premium  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE users
(
    id                 VARCHAR(255) PRIMARY KEY,
    name               TEXT         NOT NULL,
    password           VARCHAR(255) NOT null,
    subscription_level TEXT         NOT NULL DEFAULT 'STANDARD',
    subscription_till  TIMESTAMP NULL,
    created_at         TIMESTAMP             DEFAULT NOW(),
    updated_at         TIMESTAMP             DEFAULT NOW()
);


CREATE TABLE views
(
    video_id  UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    user_id   VARCHAR(255) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    viewed_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE likes
(
    video_id UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    user_id  VARCHAR(255) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    liked_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_like UNIQUE (video_id, user_id)
);

CREATE TABLE videos_temp
(
    id       UUID PRIMARY KEY REFERENCES videos (id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL
);

CREATE TABLE video_chunks
(
    id             UUID PRIMARY KEY,
    video_id       UUID REFERENCES videos (id) ON DELETE CASCADE,
    chunk_size     INT NOT NULL,
    start_position INT NOT NULL,
    end_position   INT NOT NULL
);

CREATE TABLE sessions
(
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(255)      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    accessed_at TIMESTAMP NOT NULL DEFAULT NOW()
);