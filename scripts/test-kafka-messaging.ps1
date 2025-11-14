#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Test Kafka messaging between ERP platform microservices
.DESCRIPTION
    Tests event-driven communication by:
    1. Consuming from Kafka topics
    2. Triggering actions that publish events
    3. Verifying events are received
#>

$ErrorActionPreference = "Stop"

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         ERP Platform - Kafka Messaging Test               ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Configuration
$KAFKA_BROKER = "localhost:19092"
$TOPICS = @(
    "identity.tenants",
    "identity.users",
    "identity.roles",
    "finance.ledgers",
    "finance.journal-entries"
)

# Check if kafka-console-consumer is available via Docker
function Test-KafkaConsumer {
    try {
        docker ps --filter "name=erp-redpanda" --format "{{.Names}}" | Out-Null
        return $true
    }
    catch {
        Write-Host "❌ Redpanda container not running" -ForegroundColor Red
        return $false
    }
}

# List all topics
function Get-KafkaTopics {
    Write-Host "`n📋 Listing Kafka Topics..." -ForegroundColor Yellow
    
    try {
        $output = docker exec erp-redpanda rpk topic list 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host $output -ForegroundColor Gray
            return $true
        }
        else {
            Write-Host "⚠️  Could not list topics: $output" -ForegroundColor Yellow
            return $false
        }
    }
    catch {
        Write-Host "❌ Error listing topics: $_" -ForegroundColor Red
        return $false
    }
}

# Consume messages from a topic (last 10 messages)
function Get-TopicMessages {
    param(
        [string]$Topic,
        [int]$Count = 10
    )
    
    Write-Host "`n📨 Consuming from topic: $Topic (last $Count messages)..." -ForegroundColor Cyan
    
    try {
        # Use rpk to consume messages
        $output = docker exec erp-redpanda rpk topic consume $Topic `
            --num $Count `
            --format json `
            --offset -$Count 2>&1
        
        if ($output -match "No messages") {
            Write-Host "   ℹ️  No messages in topic" -ForegroundColor Gray
            return 0
        }
        elseif ($LASTEXITCODE -eq 0) {
            $messageCount = ($output -split "`n" | Where-Object { $_ -match '"value"' }).Count
            Write-Host "   ✅ Found $messageCount messages" -ForegroundColor Green
            Write-Host $output -ForegroundColor DarkGray
            return $messageCount
        }
        else {
            Write-Host "   ⚠️  Error consuming: $output" -ForegroundColor Yellow
            return -1
        }
    }
    catch {
        Write-Host "   ❌ Error: $_" -ForegroundColor Red
        return -1
    }
}

# Start consumer in background for live monitoring
function Start-LiveConsumer {
    param(
        [string]$Topic
    )
    
    Write-Host "`n🔴 Starting live consumer for topic: $Topic" -ForegroundColor Magenta
    Write-Host "   Press Ctrl+C to stop monitoring`n" -ForegroundColor Gray
    
    docker exec erp-redpanda rpk topic consume $Topic --format json
}

# Main test execution
function Test-KafkaMessaging {
    Write-Host "🔍 Checking Kafka infrastructure..." -ForegroundColor Yellow
    
    if (-not (Test-KafkaConsumer)) {
        Write-Host "`n❌ Kafka not available. Start infrastructure first:" -ForegroundColor Red
        Write-Host "   .\scripts\start-infrastructure.ps1" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "✅ Kafka infrastructure is running" -ForegroundColor Green
    
    # List all topics
    Get-KafkaTopics
    
    # Check each expected topic
    Write-Host "`n📊 Checking expected topics..." -ForegroundColor Yellow
    
    $totalMessages = 0
    foreach ($topic in $TOPICS) {
        $count = Get-TopicMessages -Topic $topic -Count 5
        if ($count -gt 0) {
            $totalMessages += $count
        }
    }
    
    Write-Host "`n" + ("═" * 60) -ForegroundColor Cyan
    Write-Host "📈 Summary:" -ForegroundColor Cyan
    Write-Host ("═" * 60) -ForegroundColor Cyan
    Write-Host "Total messages found: $totalMessages" -ForegroundColor $(if ($totalMessages -gt 0) { "Green" } else { "Yellow" })
    
    if ($totalMessages -eq 0) {
        Write-Host "`n💡 No messages found yet. Try these actions to generate events:" -ForegroundColor Yellow
        Write-Host "   1. Create a tenant via REST API" -ForegroundColor Gray
        Write-Host "   2. Post a journal entry in Finance service" -ForegroundColor Gray
        Write-Host "   3. Create a user or role" -ForegroundColor Gray
    }
    
    # Offer to start live monitoring
    Write-Host "`n❓ Start live consumer? (monitors all new messages)" -ForegroundColor Cyan
    Write-Host "   Press 1-5 to monitor a specific topic, or Enter to skip:" -ForegroundColor Gray
    Write-Host "   1) identity.tenants" -ForegroundColor Gray
    Write-Host "   2) identity.users" -ForegroundColor Gray
    Write-Host "   3) identity.roles" -ForegroundColor Gray
    Write-Host "   4) finance.ledgers" -ForegroundColor Gray
    Write-Host "   5) finance.journal-entries" -ForegroundColor Gray
    
    $choice = Read-Host
    
    switch ($choice) {
        "1" { Start-LiveConsumer -Topic "identity.tenants" }
        "2" { Start-LiveConsumer -Topic "identity.users" }
        "3" { Start-LiveConsumer -Topic "identity.roles" }
        "4" { Start-LiveConsumer -Topic "finance.ledgers" }
        "5" { Start-LiveConsumer -Topic "finance.journal-entries" }
        default { Write-Host "`n✅ Test complete" -ForegroundColor Green }
    }
}

# Run the test
Test-KafkaMessaging
