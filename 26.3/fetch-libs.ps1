# ArcaneEnchants 26.3 依赖下载脚本
# Usage: powershell -ExecutionPolicy Bypass -File fetch-libs.ps1
#
# lib\ 目录不进仓库（.gitignore），克隆后跑一次本脚本即可获得编译所需全部 jar：
#   lib\compile\  Paper API 26.3 + Adventure 5.2 + Guava + brigadier + jetbrains annotations
# 不需要服务端 jar：26.3 的 Paper API pom 已把 brigadier 与 adventure-text-logger-slf4j 列为 compile 依赖。
#
# 可用 ARCANE_LIBS 覆盖目标目录。

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$libDir = $env:ARCANE_LIBS
if ([string]::IsNullOrWhiteSpace($libDir)) { $libDir = Join-Path $Root 'lib\compile' }
New-Item -ItemType Directory -Path $libDir -Force | Out-Null

$Paper   = 'https://repo.papermc.io/repository/maven-public'
$Central = 'https://repo1.maven.org/maven2'
$Api     = '26.3.build.241-stable'

$files = [ordered]@{
    "paper-api-$Api.jar"                  = "$Paper/io/papermc/paper/paper-api/$Api/paper-api-$Api.jar"
    'adventure-api-5.2.0.jar'             = "$Central/net/kyori/adventure-api/5.2.0/adventure-api-5.2.0.jar"
    'adventure-key-5.2.0.jar'             = "$Central/net/kyori/adventure-key/5.2.0/adventure-key-5.2.0.jar"
    'adventure-text-minimessage-5.2.0.jar' = "$Central/net/kyori/adventure-text-minimessage/5.2.0/adventure-text-minimessage-5.2.0.jar"
    'adventure-text-serializer-plain-5.2.0.jar' = "$Central/net/kyori/adventure-text-serializer-plain/5.2.0/adventure-text-serializer-plain-5.2.0.jar"
    'adventure-text-logger-slf4j-5.2.0.jar' = "$Central/net/kyori/adventure-text-logger-slf4j/5.2.0/adventure-text-logger-slf4j-5.2.0.jar"
    'examination-api-1.3.0.jar'           = "$Central/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar"
    'guava-33.6.0-jre.jar'                = "$Central/com/google/guava/guava/33.6.0-jre/guava-33.6.0-jre.jar"
    'bungeecord-chat-1.21.jar'            = "$Paper/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"
    'brigadier-1.3.11.jar'                = "$Paper/com/mojang/brigadier/1.3.11/brigadier-1.3.11.jar"
    'annotations-26.0.2.jar'              = "$Central/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar"
}

foreach ($name in $files.Keys) {
    $target = Join-Path $libDir $name
    if (Test-Path $target) {
        Write-Host "skip  $name"
        continue
    }
    Write-Host "fetch $name"
    Invoke-WebRequest -Uri $files[$name] -OutFile $target -UseBasicParsing
    if (-not (Test-Path $target) -or (Get-Item $target).Length -eq 0) {
        throw "download failed: $name"
    }
}

Write-Host ""
Write-Host "OK  $libDir"
Get-ChildItem $libDir -Filter *.jar | ForEach-Object { Write-Host ("    {0,-48} {1,8:N0} bytes" -f $_.Name, $_.Length) }
