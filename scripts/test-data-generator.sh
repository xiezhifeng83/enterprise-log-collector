#!/bin/bash

# Test Data Generator Script
# Generates mock log files with transaction IDs for testing

set -e

OUTPUT_DIR="${1:-./test-logs}"
NUM_SERVERS="${2:-3}"
LINES_PER_FILE="${3:-1000}"

echo "Generating test log data..."
echo "Output directory: $OUTPUT_DIR"
echo "Number of servers: $NUM_SERVERS"
echo "Lines per file: $LINES_PER_FILE"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Transaction ID pool
TRANSACTION_IDS=(
    "TXN20251018001"
    "TXN20251018002"
    "TXN20251018003"
    "TXN20251018004"
    "TXN20251018005"
)

# Log levels
LOG_LEVELS=("INFO" "DEBUG" "WARN" "ERROR")

# Generate logs for each server
for ((server=1; server<=NUM_SERVERS; server++)); do
    SERVER_DIR="$OUTPUT_DIR/server${server}"
    mkdir -p "$SERVER_DIR"

    # Generate main application log
    APP_LOG="$SERVER_DIR/application.log"
    echo "Generating $APP_LOG..."

    > "$APP_LOG"  # Clear file

    for ((i=1; i<=LINES_PER_FILE; i++)); do
        # Generate timestamp
        TIMESTAMP=$(date -u +"%Y-%m-%d %H:%M:%S")

        # Random log level (80% INFO, 10% DEBUG, 7% WARN, 3% ERROR)
        RAND=$((RANDOM % 100))
        if [ $RAND -lt 80 ]; then
            LEVEL="INFO"
        elif [ $RAND -lt 90 ]; then
            LEVEL="DEBUG"
        elif [ $RAND -lt 97 ]; then
            LEVEL="WARN"
        else
            LEVEL="ERROR"
        fi

        # Random transaction ID (60% of logs have transaction IDs)
        TXN_ID=""
        if [ $((RANDOM % 100)) -lt 60 ]; then
            TXN_ID=${TRANSACTION_IDS[$((RANDOM % ${#TRANSACTION_IDS[@]}))]}
        fi

        # Generate log message
        if [ -n "$TXN_ID" ]; then
            if [ "$LEVEL" == "ERROR" ]; then
                echo "[$TIMESTAMP] [$LEVEL] [server-${server}] Transaction $TXN_ID failed with error: Database connection timeout" >> "$APP_LOG"
            elif [ "$LEVEL" == "WARN" ]; then
                echo "[$TIMESTAMP] [$LEVEL] [server-${server}] Transaction $TXN_ID took longer than expected: 5234ms" >> "$APP_LOG"
            else
                MESSAGES=(
                    "Processing transaction $TXN_ID - step 1: validation"
                    "Processing transaction $TXN_ID - step 2: authorization"
                    "Processing transaction $TXN_ID - step 3: execution"
                    "Transaction $TXN_ID completed successfully"
                    "Transaction $TXN_ID started from gateway"
                )
                MSG=${MESSAGES[$((RANDOM % ${#MESSAGES[@]}))]}
                echo "[$TIMESTAMP] [$LEVEL] [server-${server}] $MSG" >> "$APP_LOG"
            fi
        else
            GENERIC_MESSAGES=(
                "Health check completed"
                "Cache refresh initiated"
                "Background task executed"
                "Metrics collected and published"
                "Configuration reloaded"
            )
            MSG=${GENERIC_MESSAGES[$((RANDOM % ${#GENERIC_MESSAGES[@]}))]}
            echo "[$TIMESTAMP] [$LEVEL] [server-${server}] $MSG" >> "$APP_LOG"
        fi
    done

    # Generate ledger log
    LEDGER_LOG="$SERVER_DIR/ledger.log"
    echo "Generating $LEDGER_LOG..."

    > "$LEDGER_LOG"  # Clear file

    for ((i=1; i<=500; i++)); do
        TIMESTAMP=$(date -u +"%Y-%m-%d %H:%M:%S")
        TXN_ID=${TRANSACTION_IDS[$((RANDOM % ${#TRANSACTION_IDS[@]}))]}

        LEDGER_MESSAGES=(
            "Ledger entry created for transaction $TXN_ID: amount=\$1234.56"
            "Balance updated for transaction $TXN_ID: new_balance=\$5678.90"
            "Transaction $TXN_ID recorded in ledger at $(date +%s)"
        )
        MSG=${LEDGER_MESSAGES[$((RANDOM % ${#LEDGER_MESSAGES[@]}))]}
        echo "[$TIMESTAMP] [INFO] [ledger-${server}] $MSG" >> "$LEDGER_LOG"
    done

    echo "  Created logs for server${server}"
done

echo ""
echo "✅ Test log data generation complete!"
echo ""
echo "Generated files:"
find "$OUTPUT_DIR" -name "*.log" -exec echo "  {}" \;
echo ""
echo "Total lines: $((NUM_SERVERS * (LINES_PER_FILE + 500)))"
echo ""
echo "Transaction IDs used:"
for TXN in "${TRANSACTION_IDS[@]}"; do
    echo "  $TXN"
done
echo ""
echo "To use these logs for testing:"
echo "  1. Configure SSH access to test servers in the application"
echo "  2. Point log_file_paths to the generated .log files"
echo "  3. Run the collection service"
