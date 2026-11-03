# ArcaneEnchants build script
# Usage: powershell -ExecutionPolicy Bypass -File build.ps1
#
# Requires: JDK 21 (C:\Program Files\Java\jdk-21 by default, override with ARCANE_JDK)
# Compile classpath: lib\compile\*.jar (Paper API + Adventure + Guava)
# lib\ is gitignored; nothing from lib is bundled into the output jar.

$ErrorActionPreference = 'Stop'

$Root    = Split-Path -Parent $MyInvocation.MyCommand.Path
$Version = '1.0.0'
$MC      = '1.21.11'
$Name    = "ArcaneEnchants-$Version-$MC.jar"

$JdkDir = $env:ARCANE_JDK
if ([string]::IsNullOrWhiteSpace($JdkDir)) { $JdkDir = 'C:\Program Files\Java\jdk-21' }
$Javac  = Join-Path $JdkDir 'bin\javac.exe'
$JarTool = Join-Path $JdkDir 'bin\jar.exe'

if (-not (Test-Path $Javac)) { throw "javac not found: $Javac (set ARCANE_JDK)" }

$Src = Join-Path $Root 'src'
$Res = Join-Path $Root 'resources'
$Out = Join-Path $Root 'build'
$Dist = Join-Path $Root 'dist'

$libDir = $env:ARCANE_LIBS
if ([string]::IsNullOrWhiteSpace($libDir)) { $libDir = Join-Path $Root 'lib\compile' }
if (-not (Test-Path $libDir)) { throw "compile libs not found: $libDir (set ARCANE_LIBS)" }

# 可选的 server jar 目录：只用于补齐 brigadier / adventure-logger 等编译期类型，不打进产物
$serverLibDir = $env:ARCANE_SERVER_LIBS
if ([string]::IsNullOrWhiteSpace($serverLibDir)) { $serverLibDir = Join-Path $Root 'lib\server' }

$libJars = @(Get-ChildItem $libDir -Filter *.jar | ForEach-Object { $_.FullName })
if (Test-Path $serverLibDir) {
    $libJars += @(Get-ChildItem $serverLibDir -Filter *.jar | ForEach-Object { $_.FullName })
}
if ($libJars.Count -eq 0) { throw "No jars found in $libDir" }
$Classpath = $libJars -join ';'

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null
if (-not (Test-Path $Dist)) { New-Item -ItemType Directory -Path $Dist | Out-Null }

$sources = @(Get-ChildItem $Src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) { throw 'No java sources found' }

$argsList = @('-J-Duser.language=en', '-J-Duser.country=US', '-encoding', 'UTF-8', '--release', '21', '-nowarn', '-cp', $Classpath, '-d', $Out) + $sources
& $Javac @argsList
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Copy-Item (Join-Path $Res '*') $Out -Recurse -Force

$outJar = Join-Path $Dist $Name
if (Test-Path $outJar) { Remove-Item $outJar -Force }
& $JarTool --create --file $outJar -C $Out .
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }

$classes = (Get-ChildItem $Out -Recurse -Filter *.class | Measure-Object).Count
$size = [math]::Round((Get-Item $outJar).Length / 1KB, 1)
Write-Host ""
Write-Host "OK  $outJar"
Write-Host "    classes: $classes    size: ${size} KB"
Write-Host "    JDK:     $JdkDir"
Write-Host "    libs:    $libDir"
