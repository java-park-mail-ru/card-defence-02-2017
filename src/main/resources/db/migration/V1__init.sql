create extension if not exists "uuid-ossp";

CREATE TABLE "public".users
(
    ID UUID DEFAULT uuid_generate_v4() PRIMARY KEY NOT NULL,
    username VARCHAR(300) NOT NULL,
    email VARCHAR(300) NOT NULL,
    password VARCHAR(300) NOT NULL
);
CREATE UNIQUE INDEX users_username_uindex ON "public".users (username);
CREATE UNIQUE INDEX users_lower_username_uindex ON "public".users (lower(username));

CREATE TABLE "public".sessions
(
    ID UUID DEFAULT uuid_generate_v4() PRIMARY KEY NOT NULL,
    username VARCHAR(300) NOT NULL,
    updated TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX sessions_username_uindex ON "public".sessions (username);
