# Personal Account Book Deployment Script
param(
    [string]$ServerIP = "49.235.161.106",
    [int]$ServerPort = 22,
    [string]$Username = "root",
    [string]$KeyPath = "$env:USERPROFILE\.ssh\id_rsa",
    [string]$RemoteDir = "/opt/apps/account-book",
    [int]$HostPort = 10013,
    [int]$ContainerPort = 8080,
    [string]$ContainerName = "account-book",
    [string]$ImageName = "account-book:latest"
)

$ErrorActionPreference = "Stop"

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    switch ($Level) {
        "ERROR" { Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor Red }
        "WARN"  { Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor Yellow }
        "SUCCESS" { Write-Host "[$timestamp] [$Level] $Message" -ForegroundColor Green }
        default { Write-Host "[$timestamp] [$Level] $Message" }
    }
}

Write-Log "========== Starting Deployment =========="
Write-Log "Server: ${ServerIP}:${ServerPort}"
Write-Log "Remote Directory: $RemoteDir"
Write-Log "Host Port: $HostPort"

# Phase 1: Check Docker on server
Write-Log "========== Phase 1: Server Pre-check =========="
Write-Log "Checking Docker status..."
try {
    $dockerVersion = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$Username@$ServerIP" "docker version --format '{{.Server.Version}}' 2>/dev/null"
    if ($LASTEXITCODE -eq 0) {
        Write-Log "Docker is running, version: $dockerVersion" "SUCCESS"
    } else {
        throw "Docker is not running"
    }
} catch {
    Write-Log "Docker check failed: $_" "ERROR"
    exit 1
}

# Check and create remote directory
Write-Log "Checking remote directory $RemoteDir..."
ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "mkdir -p $RemoteDir && chmod 755 $RemoteDir"
Write-Log "Directory ready" "SUCCESS"

# Check port 10013
Write-Log "Checking port $HostPort usage..."
$portCheckScript = 'non_container_pid=$(netstat -tlnp 2>/dev/null | grep ":'$HostPort' " | grep -v "docker-proxy" | awk "{print \$7}" | cut -d"/" -f1 | head -1); if [ -n "$non_container_pid" ] && [ "$non_container_pid" != "-" ]; then echo "NON_CONTAINER:$(cat /proc/$non_container_pid/comm 2>/dev/null || echo unknown)"; exit 0; fi; container_id=$(docker ps -q --filter "publish='$HostPort'" 2>/dev/null); if [ -n "$container_id" ]; then container_name=$(docker inspect --format="{{.Name}}" $container_id | sed "s/\///"); echo "CONTAINER:$container_name:$container_id"; else echo "FREE"; fi'
$portCheck = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "$portCheckScript"
Write-Log "Port check result: $portCheck"

if ($portCheck -match "^NON_CONTAINER:") {
    $processName = $portCheck.Split(":")[1]
    Write-Log "Port $HostPort is occupied by non-container process: $processName. Deployment failed!" "ERROR"
    exit 1
}

if ($portCheck -match "^CONTAINER:(.+):(.+)") {
    $existingContainer = $Matches[1]
    $existingContainerId = $Matches[2]
    Write-Log "Port $HostPort is occupied by container $existingContainer, stopping and removing..." "WARN"
    ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker stop $existingContainerId && docker rm $existingContainerId"
    Write-Log "Container $existingContainer stopped and removed" "SUCCESS"
}

# Check and remove existing container with same name
Write-Log "Checking for existing container $ContainerName..."
$existingContainerId = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker ps -aq --filter name=^/$ContainerName`$ 2>/dev/null"
if ($existingContainerId) {
    Write-Log "Found existing container, removing..." "WARN"
    ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker rm -f $ContainerName 2>/dev/null"
    Write-Log "Existing container removed" "SUCCESS"
}

# Phase 2: Clean old files and images
Write-Log "========== Phase 2: Clean old files and images =========="
Write-Log "Cleaning old files..."
ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "rm -f $RemoteDir/*.jar $RemoteDir/Dockerfile 2>/dev/null; echo 'Cleaned'"

Write-Log "Checking old images..."
$oldImageId = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker images --filter reference=$ImageName --format '{{.ID}}' 2>/dev/null"
if ($oldImageId) {
    Write-Log "Found old image, removing..." "WARN"
    ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker rmi -f $ImageName 2>/dev/null; echo 'Image removed'"
    Write-Log "Old image removed" "SUCCESS"
}

# Phase 3: Upload files
Write-Log "========== Phase 3: Upload files =========="
Write-Log "Uploading JAR file..."
scp -i $KeyPath -P $ServerPort -o StrictHostKeyChecking=no "target/personal-account-book-1.0.0.jar" "$Username@${ServerIP}:${RemoteDir}/"
Write-Log "JAR file uploaded" "SUCCESS"

Write-Log "Uploading Dockerfile..."
scp -i $KeyPath -P $ServerPort -o StrictHostKeyChecking=no "Dockerfile" "$Username@${ServerIP}:${RemoteDir}/"
Write-Log "Dockerfile uploaded" "SUCCESS"

# Phase 4: Build image and start container
Write-Log "========== Phase 4: Build image and start container =========="
Write-Log "Building Docker image..."
$buildResult = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "cd $RemoteDir && docker build -t $ImageName . 2>&1"
if ($LASTEXITCODE -ne 0) {
    Write-Log "Build failed: $buildResult" "ERROR"
    exit 1
}
Write-Log "Image built successfully" "SUCCESS"

Write-Log "Starting container..."
$runResult = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker run -d --name $ContainerName -p $HostPort`:$ContainerPort -v $RemoteDir/data:/app/data --restart unless-stopped $ImageName 2>&1"
if ($LASTEXITCODE -ne 0) {
    Write-Log "Container start failed: $runResult" "ERROR"
    exit 1
}

Start-Sleep -Seconds 3
$containerStatus = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker ps --filter name=$ContainerName --format '{{.Status}}'"
if ($containerStatus) {
    Write-Log "Container started, status: $containerStatus" "SUCCESS"
} else {
    Write-Log "Container failed to start" "ERROR"
    $logs = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker logs $ContainerName 2>&1"
    Write-Log "Container logs: $logs" "ERROR"
    exit 1
}

# Phase 5: Health check
Write-Log "========== Phase 5: Health Check =========="
Write-Log "Waiting 15 seconds for application to start..."
Start-Sleep -Seconds 15

$healthUrl = "http://${ServerIP}:${HostPort}/api/users"
Write-Log "Testing API: $healthUrl"

try {
    $response = Invoke-WebRequest -Uri $healthUrl -Method GET -TimeoutSec 10 -UseBasicParsing
    $statusCode = $response.StatusCode
    $content = $response.Content
    
    Write-Log "API returned status: $statusCode" "SUCCESS"
    
    if ($statusCode -eq 200) {
        Write-Log "========== Deployment Successful! ==========" "SUCCESS"
        Write-Log "API Endpoint: http://${ServerIP}:${HostPort}" "SUCCESS"
        Write-Log "Frontend Page: http://${ServerIP}:${HostPort}/index.html" "SUCCESS"
        Write-Log ""
        Write-Log "========== Test Cases ==========" "SUCCESS"
        Write-Log "1. User Registration:"
        Write-Log "   curl -X POST http://${ServerIP}:${HostPort}/api/users/register -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"123456\"}'"
        Write-Log ""
        Write-Log "2. User Login:"
        Write-Log "   curl -X POST http://${ServerIP}:${HostPort}/api/users/login -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"123456\"}'"
        Write-Log ""
        Write-Log "3. Get User List:"
        Write-Log "   curl http://${ServerIP}:${HostPort}/api/users"
        Write-Log ""
        Write-Log "4. Access Frontend:"
        Write-Log "   http://${ServerIP}:${HostPort}/index.html"
        Write-Log ""
        Write-Log "5. View Container Logs:"
        Write-Log "   ssh -i $KeyPath -p $ServerPort $Username@$ServerIP 'docker logs $ContainerName -f'"
    } else {
        Write-Log "API returned non-200 status" "WARN"
    }
} catch {
    Write-Log "Health check failed: $_" "ERROR"
    $logs = ssh -i $KeyPath -p $ServerPort -o StrictHostKeyChecking=no "$Username@$ServerIP" "docker logs $ContainerName --tail 50 2>&1"
    Write-Log "Container logs: $logs"
}
