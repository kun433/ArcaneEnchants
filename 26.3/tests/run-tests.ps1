# 离线断言：不启动服务端，只验证附魔定义与配置是否符合设计
# Usage: powershell -ExecutionPolicy Bypass -File tests\run-tests.ps1
#
# 前置：先跑 build.ps1，测试使用 build\ 里的类文件

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$JdkDir = $env:ARCANE_JDK
if ([string]::IsNullOrWhiteSpace($JdkDir)) { $JdkDir = 'C:\Program Files\Java\jdk-25' }
$Javac = Join-Path $JdkDir 'bin\javac.exe'
$Java = Join-Path $JdkDir 'bin\java.exe'

$Classes = Join-Path $Root 'build'
if (-not (Test-Path $Classes)) { throw "build\ not found, run build.ps1 first" }

$Out = Join-Path $Root 'tests\build'
if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null

$sources = @(Get-ChildItem (Join-Path $Root 'tests') -Filter *.java | ForEach-Object { $_.FullName })
& $Javac '-encoding' 'UTF-8' '--release' '25' '-nowarn' '-cp' $Classes '-d' $Out @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$Resources = Join-Path $Root 'resources'
& $Java '-cp' "$Out;$Classes" 'DesignTest' $Resources
if ($LASTEXITCODE -ne 0) { throw "design test failed with exit code $LASTEXITCODE" }
