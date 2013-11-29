-- SQL DDL script
-- Generated file - do not edit

alter table "GAME_TO_USER" drop constraint "GAME_FK"

alter table "GAME_TO_USER" drop constraint "USER_FK"

drop table "GAME_TO_USER"

drop table "GAMES"

drop table "HEROES"

drop table "USERS"