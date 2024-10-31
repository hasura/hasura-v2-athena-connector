#!/bin/sh

echo "waiting for sql server to come up"
sleep 20s
echo "running chinook database sql script"
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P Password123# -i chinook.sql
