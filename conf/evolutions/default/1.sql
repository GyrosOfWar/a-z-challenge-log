# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "GAME_TO_USER" ("GAME_ID" BIGINT NOT NULL,"USER_ID64" BIGINT NOT NULL);
create table "GAMES" ("MATCH_ID" BIGINT NOT NULL PRIMARY KEY,"DATE" BIGINT NOT NULL,"HERO_ID" INTEGER NOT NULL,"KILLS" INTEGER NOT NULL,"DEATHS" INTEGER NOT NULL,"ASSISTS" INTEGER NOT NULL,"GPM" INTEGER NOT NULL,"XPM" INTEGER NOT NULL,"WIN" BOOLEAN NOT NULL);
create table "HEROES" ("HERO_ID" INTEGER NOT NULL,"HERO_NAME" VARCHAR NOT NULL,"IMAGE_URL" VARCHAR NOT NULL);
create table "USERS" ("U_ID64" BIGINT NOT NULL PRIMARY KEY,"U_ID32" INTEGER NOT NULL,"U_NAME" VARCHAR NOT NULL);
alter table "GAME_TO_USER" add constraint "GAME_FK" foreign key("GAME_ID") references "GAMES"("MATCH_ID") on update NO ACTION on delete NO ACTION;
alter table "GAME_TO_USER" add constraint "USER_FK" foreign key("USER_ID64") references "USERS"("U_ID64") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "GAME_TO_USER" drop constraint "GAME_FK";
alter table "GAME_TO_USER" drop constraint "USER_FK";
drop table "GAME_TO_USER";
drop table "GAMES";
drop table "HEROES";
drop table "USERS";

