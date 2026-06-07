#!/bin/bash
DB="/home/ubuntu/mc-archive-data/mcarchive.db"
sqlite3 "$DB" "ALTER TABLE users ADD COLUMN trusted_devices TEXT;" 2>/dev/null
sqlite3 "$DB" "ALTER TABLE users ADD COLUMN device_verify_code VARCHAR(10);" 2>/dev/null
sqlite3 "$DB" "ALTER TABLE users ADD COLUMN device_verify_expiry TIMESTAMP;" 2>/dev/null
echo "DB migration done"
sudo pkill -9 -f "mc-archive-hub" 2>/dev/null
sleep 2
cd /home/ubuntu/mc-archive-hub
nohup java -jar mc-archive-hub-1.0.0.jar --server.port=8080 --spring.datasource.url="jdbc:sqlite:$DB" --app.upload.dir="/home/ubuntu/mc-archive-uploads" --app.base-url="http://43.128.141.158:8080" --spring.devtools.add-properties=false >> /home/ubuntu/mc-archive.log 2>&1 &
echo "Restarted. Verify login after 15s."
