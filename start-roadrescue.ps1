$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

function Write-Step {
  param([string]$Message)
  Write-Host ""
  Write-Host "==> $Message" -ForegroundColor Cyan
}

function Invoke-Npm {
  param([string[]]$Arguments)
  & npm.cmd @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "npm command failed: npm.cmd $($Arguments -join ' ')"
  }
}

function Sync-ServerEnv {
  if (Test-Path ".env") {
    Copy-Item ".env" "server/.env" -Force
  }
}

function Set-JavaHome {
  if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
    Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor DarkGray
    return
  }

  $preferredJdks = @(
    "C:\Program Files\Java\jdk-21.0.11",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Java\latest"
  )

  foreach ($jdkPath in $preferredJdks) {
    if (Test-Path $jdkPath) {
      $env:JAVA_HOME = $jdkPath
      $env:Path = "$env:JAVA_HOME\bin;$env:Path"
      Write-Host "Set JAVA_HOME: $env:JAVA_HOME" -ForegroundColor DarkGray
      return
    }
  }

  $detectedJdk = Get-ChildItem "C:\Program Files\Java" -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Select-Object -First 1

  if ($detectedJdk) {
    $env:JAVA_HOME = $detectedJdk.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Host "Set JAVA_HOME: $env:JAVA_HOME" -ForegroundColor DarkGray
    return
  }

  throw "Java JDK was not found. Install Java 21 LTS or set JAVA_HOME, then rerun start-roadrescue.ps1."
}

function Test-CommandAvailable {
  param([string]$CommandName)
  return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Ensure-EnvFile {
  if (-not (Test-Path ".env")) {
    Write-Step "Creating .env from .env.local.example"
    Copy-Item ".env.local.example" ".env"
    throw "Created .env from .env.local.example. Update DATABASE_URL and rerun the script."
  }
}

function Get-DotEnvValue {
  param([string]$Name)

  if (-not (Test-Path ".env")) {
    return $null
  }

  $line = Get-Content ".env" |
    Where-Object { $_ -match "^\s*$Name\s*=" -and $_ -notmatch "^\s*#" } |
    Select-Object -First 1

  if (-not $line) {
    return $null
  }

  return ($line -split "=", 2)[1].Trim().Trim('"').Trim("'")
}

function Assert-LocalDatabaseSafety {
  $databaseUrl = Get-DotEnvValue "DATABASE_URL"
  if (-not $databaseUrl) {
    throw "DATABASE_URL is missing from .env. Set it to a local development PostgreSQL database before running locally."
  }

  $allowRemote = Get-DotEnvValue "ALLOW_REMOTE_DATABASE"
  $isExplicitlyAllowed = $allowRemote -in @("1", "true", "TRUE", "yes", "YES")
  $looksProduction =
    $databaseUrl -match "schema=roadrescue_prod" -or
    $databaseUrl -match "neon\.tech" -or
    $databaseUrl -match "onrender\.com"

  if ($looksProduction -and -not $isExplicitlyAllowed) {
    throw @"
DATABASE_URL looks like a production or hosted database.
Refusing to start the local app so Flyway or test actions do not touch shared data.

For local development, update .env to use a local schema, for example:
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/roadrescue?schema=roadrescue_local

If you intentionally want to use the remote database, add this to .env:
ALLOW_REMOTE_DATABASE=true
"@
  }
}

function Test-PortListening {
  param([int]$Port)
  $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
  return $null -ne $connections
}

function Get-PortListenerProcessIds {
  param([int]$Port)
  @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique |
    Where-Object { $_ -and $_ -ne $PID })
}

function Get-RelatedRoadRescueProcessIds {
  param([int[]]$SeedProcessIds)

  try {
    $processSnapshot = @(Get-CimInstance Win32_Process -ErrorAction Stop)
  }
  catch {
    return @($SeedProcessIds | Where-Object { $_ -and $_ -ne $PID } | Sort-Object -Unique)
  }

  $orderedIds = New-Object System.Collections.Generic.List[int]

  foreach ($seedProcessId in $SeedProcessIds) {
    $lineage = New-Object System.Collections.Generic.List[int]
    $currentProcessId = [int]$seedProcessId

    for ($i = 0; $i -lt 16 -and $currentProcessId; $i++) {
      if ($currentProcessId -eq $PID) {
        break
      }

      $process = $processSnapshot | Where-Object { $_.ProcessId -eq $currentProcessId } | Select-Object -First 1
      if (-not $process) {
        break
      }

      $commandLine = [string]$process.CommandLine
      $isSeed = $currentProcessId -eq $seedProcessId
      $isRoadRescueDevProcess =
        $isSeed -or
        $commandLine -match [regex]::Escape($PSScriptRoot) -or
        $commandLine -match 'start-roadrescue|npm(\.cmd|-cli\.js)?.*run dev|npm-cli\.js.*run dev|concurrently|spring-boot:run|ng serve|mvn.*spring-boot:run'

      if (-not $isRoadRescueDevProcess) {
        break
      }

      $lineage.Add($currentProcessId)
      $currentProcessId = [int]$process.ParentProcessId
    }

    [array]::Reverse($lineage)
    foreach ($relatedProcessId in $lineage) {
      if (-not $orderedIds.Contains($relatedProcessId)) {
        $orderedIds.Add($relatedProcessId)
      }
    }
  }

  return @($orderedIds)
}

function Stop-ProcessesUsingPorts {
  param([int[]]$Ports)

  $listenerProcessIds = @()
  foreach ($port in $Ports) {
    $listenerProcessIds += Get-PortListenerProcessIds $port
  }

  $listenerProcessIds = @($listenerProcessIds | Sort-Object -Unique)
  if ($listenerProcessIds.Count -eq 0) {
    Write-Host "No existing RoadRescue listeners found on ports $($Ports -join ', ')." -ForegroundColor DarkGray
    return
  }

  $processIds = @(Get-RelatedRoadRescueProcessIds $listenerProcessIds)
  foreach ($processId in $processIds) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if (-not $process) {
      continue
    }

    Write-Host "Stopping existing process $($process.ProcessName) ($processId)..." -ForegroundColor Yellow
    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
  }
}

function Wait-PortsAvailable {
  param(
    [int[]]$Ports,
    [int]$TimeoutSeconds = 20
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  do {
    $busyPorts = @($Ports | Where-Object { Test-PortListening $_ })
    if ($busyPorts.Count -eq 0) {
      return
    }

    Start-Sleep -Milliseconds 500
  } while ((Get-Date) -lt $deadline)

  $stillBusy = @($Ports | Where-Object { Test-PortListening $_ })
  throw "Ports still in use after cleanup: $($stillBusy -join ', '). Close those processes manually and rerun start-roadrescue.ps1."
}

Write-Step "Closing existing RoadRescue instances"
Stop-ProcessesUsingPorts @(5000, 5173)
Wait-PortsAvailable @(5000, 5173)

Write-Step "Preparing RoadRescue local environment"
Ensure-EnvFile
Assert-LocalDatabaseSafety
Sync-ServerEnv
Set-JavaHome

if (-not (Test-CommandAvailable "mvn")) {
  throw "Maven was not found on PATH. Install Maven or add it to PATH, then rerun start-roadrescue.ps1."
}

if (-not (Test-Path "node_modules")) {
  Write-Step "Installing dependencies"
  Invoke-Npm @("install")
}
else {
  Write-Step "Dependencies already installed"
}

Write-Step "Starting RoadRescue locally"
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Green
Write-Host "Backend:  http://localhost:5000/api/health" -ForegroundColor Green
Write-Host "Database schema: roadrescue_local" -ForegroundColor Green
Write-Host "Backend:  Spring Boot + Flyway" -ForegroundColor Green
Write-Host "Frontend: Angular dev server" -ForegroundColor Green

Sync-ServerEnv
Invoke-Npm @("run", "dev")