# --- !Ups

create table PROJECT (
  ID bigint(20) NOT NULL AUTO_INCREMENT,
  NAME varchar(255) NOT NULL,
  PRIMARY KEY (id)
);

create table STATUSMONITOR (
  ID bigint(20) NOT NULL AUTO_INCREMENT,
  PROJECTID bigint(20) NOT NULL,
  NAME varchar(255) NOT NULL,
  TYPENUM int(10) NOT NULL,
  URL varchar(255) NOT NUll,
  USERNAME varchar(255) NULL,
  PASSWORD varchar(255) NULL,
  ACTIVE boolean NOT NULL,
  KEEPHISTORY int(10) NOT NULL,
  UPDATEPERIOD int(10) NOT NULL,
  LASTQUERIED timestamp,
  LASTUPDATED timestamp,
  PRIMARY KEY (id),
  FOREIGN KEY (PROJECTID) REFERENCES PROJECT(ID)
);

create table STATUSVALUE (
  ID bigint(20) NOT NULL AUTO_INCREMENT,
  STATUSMONITORID bigint(20) NOT NULL,
  STATUSNUM int(10) NOT NULL,
  RETRIEVEDAT timestamp NOT NULL,
  VALUESJSON varchar(1000) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (STATUSMONITORID) REFERENCES STATUSMONITOR(ID)
);

alter table DISPLAY add column PROJECTID bigint(20);

# --- !Downs

alter table DISPLAY drop column PROJECTID bigint(20);

drop table STATUSVALUE;

drop table STATUSMONITOR;

drop table PROJECT;