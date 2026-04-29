#!/bin/bash
# Auto-import NanoOrbit schema from Data Pump dump on first startup

DUMP_FILE="/opt/oracle/dump/vls_full.dmp"
MARKER="/opt/oracle/oradata/.import_done"

# Skip if already imported
if [ -f "$MARKER" ]; then
    echo "Schema already imported, skipping."
    exit 0
fi

# Check dump file exists
if [ ! -f "$DUMP_FILE" ]; then
    echo "ERROR: Dump file not found at $DUMP_FILE"
    exit 1
fi

echo "Creating Oracle directory for import..."
sqlplus -s SYSTEM/NanoOrbit_2026_VLS@FREEPDB1 <<SQL
CREATE OR REPLACE DIRECTORY dump_dir AS '/opt/oracle/dump';
GRANT READ, WRITE ON DIRECTORY dump_dir TO VLS_ADMIN;
EXIT
SQL

echo "Importing NanoOrbit schema..."
impdp VLS_ADMIN/Admin_VLS_2026@FREEPDB1 \
    SCHEMAS=VLS_ADMIN \
    DIRECTORY=dump_dir \
    DUMPFILE=vls_full.dmp \
    LOGFILE=vls_admin_import.log \
    TABLE_EXISTS_ACTION=REPLACE

EXIT_CODE=$?
# Exit code 5 = completed with warnings (privilege errors are expected)
if [ $EXIT_CODE -eq 0 ] || [ $EXIT_CODE -eq 5 ]; then
    echo "Import completed successfully."
    touch "$MARKER"
else
    echo "Import failed (exit code $EXIT_CODE). Check /opt/oracle/dump/vls_admin_import.log"
    exit 1
fi
