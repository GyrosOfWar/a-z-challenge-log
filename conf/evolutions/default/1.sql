# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "GAMES" ("MATCH_ID" BIGINT NOT NULL,"HERO_ID" INTEGER NOT NULL,"DATE" BIGINT NOT NULL);
create table "HEROES" ("HERO_ID" INTEGER NOT NULL,"HERO_NAME" VARCHAR NOT NULL,"IMAGE_URL" VARCHAR NOT NULL);
create table "USERS" ("U_ID64" BIGINT NOT NULL PRIMARY KEY,"U_ID32" INTEGER NOT NULL,"U_NAME" VARCHAR NOT NULL);

# --- !Downs

drop table "GAMES";
drop table "HEROES";
drop table "USERS";

