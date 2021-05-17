# --- First database schema

# --- !Ups

set ignorecase true;

-- create table company (
--   id                        bigint not null,
--   name                      varchar(255) not null,
--   constraint pk_company primary key (id))
-- ;

-- create table computer (
--   id                        bigint not null,
--   name                      varchar(255) not null,
--   introduced                timestamp,
--   discontinued              timestamp,
--   company_id                bigint,
--   constraint pk_computer primary key (id))
-- ;

create table member (
    id  int not null,
    name varchar(255) not null,
    constraint pk_member primary key (id)
);

create table content (
    id int not null,
    title varchar(255) not null,
    content varchar(255) not null,
    member_id bigint,
    constraint  pk_content primary key (id)
);

create sequence member_seq start with 1000;

create sequence content_seq start with 1000;

alter table content add constraint fk_content_member foreign key (member_id) references member (id) on delete restrict on update restrict;
create index ix_content_member on content (member_id);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists member;

drop table if exists content;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists member_seq;

drop sequence if exists content_seq;

