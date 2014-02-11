#!/bin/bash
# Note: This script is tested on Linux environment only. It should work on any Unix platform but is not tested.

# command line arguments
zookeeper=$1
rowcount=$3
now=$(date  +'%d%m%H%M%S')
echo "$now"
table="index_test_$now"

# helper variable and functions
ddl="ddl.sql"
data="data.csv"
qry="query.sql"
statements=""

# Phoenix client jar. To generate new jars: $ mvn package -DskipTests
current_dir=$(cd $(dirname $0);pwd)
phoenix_jar_path="$current_dir/../target"
phoenix_client_jar=$(find $phoenix_jar_path/phoenix-*-client.jar)
testjar="$phoenix_jar_path/phoenix-*-tests.jar"

# HBase configuration folder path (where hbase-site.xml reside) for HBase/Phoenix client side property override
hbase_config_path="$current_dir"

execute="java -cp "$hbase_config_path:$phoenix_client_jar" -Dlog4j.configuration=file:$current_dir/log4j.properties com.salesforce.phoenix.util.PhoenixRuntime -t $table $1 "
function usage {
	echo "Index performance script arguments not specified. Usage: idxtest.sh <zookeeper> [N|M|I]"
	echo "Second argument option details [N|M|I]  (N: No Index, M: Mutable Index, I: Immutable Index)"
	echo "Example: idxtest.sh localhost M"
	exit
}

function cleartempfiles {
	delfile $ddl
	delfile $data
}
function delfile {
	if [ -f $1 ]; then rm $1 ;fi;
}

# Create Table DDL
noindex="CREATE TABLE IF NOT EXISTS $table (HOST CHAR(2) NOT NULL,DOMAIN VARCHAR NOT NULL, FEATURE VARCHAR NOT NULL, DATE DATE NOT NULL, 
USAGE.CORE BIGINT,USAGE.DB BIGINT,STATS.ACTIVE_VISITOR INTEGER CONSTRAINT PK PRIMARY KEY (HOST, DOMAIN, FEATURE, DATE));"

mutableindex="CREATE TABLE IF NOT EXISTS $table (HOST CHAR(2) NOT NULL,DOMAIN VARCHAR NOT NULL, FEATURE VARCHAR NOT NULL, DATE DATE NOT NULL, 
USAGE.CORE BIGINT,USAGE.DB BIGINT,STATS.ACTIVE_VISITOR INTEGER CONSTRAINT PK PRIMARY KEY (HOST, DOMAIN, FEATURE, DATE));CREATE INDEX IDX_ON_LOAD_$table ON $table (CORE,DB,ACTIVE_VISITOR);"

immutableindex="CREATE TABLE IF NOT EXISTS $table (HOST CHAR(2) NOT NULL,DOMAIN VARCHAR NOT NULL, FEATURE VARCHAR NOT NULL, DATE DATE NOT NULL, 
USAGE.CORE BIGINT,USAGE.DB BIGINT,STATS.ACTIVE_VISITOR INTEGER CONSTRAINT PK PRIMARY KEY (HOST, DOMAIN, FEATURE, DATE)) IMMUTABLE_ROWS=true;CREATE INDEX IDX_ON_LOAD_$table ON $table (CORE,DB,ACTIVE_VISITOR);"


# generate and upsert data
clear
echo "Phoenix Index Evaluation Script 1.0";echo "-----------------------------------------"
if [ -z "$2" ] 
then usage; fi;

echo ""; echo "Creating table..."

if [ $2 == "N" ];then
	echo $noindex > $ddl; 
elif  [ $2 == "M" ];then
	echo $mutableindex > $ddl; 
elif  [ $2 == "I" ];then
	echo $immutableindex > $ddl; 
else
	echo "Correct second argument not specified!"
	usage
fi


$execute "$ddl"

echo ""; echo "Generating and upserting data..."
java -jar $testjar $rowcount
echo ""; 
$execute $data

# clear temporary files
cleartempfiles
