param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Tasks)
$ErrorActionPreference = 'Stop'
$sourceRoot = Split-Path -Parent $PSScriptRoot
if (!$Tasks) { $Tasks = @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug') }
$temporaryDrive = $null
if ($sourceRoot -match '[^\x00-\x7F]') {
    $temporaryDrive = @('P:', 'Q:', 'R:', 'S:') | Where-Object { !(Test-Path ($_ + '\')) } | Select-Object -First 1
    if (!$temporaryDrive) { throw 'No free drive letter for an ASCII build path.' }
    & subst $temporaryDrive $sourceRoot
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the temporary build drive.' }
    $sourceRoot = $temporaryDrive + '\'
}
Push-Location $sourceRoot
try {
    & .\gradlew.bat --no-daemon --no-configuration-cache --console=plain -Pandroid.overridePathCheck=true @Tasks
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
    if ($temporaryDrive) { & subst $temporaryDrive /D }
}
exit $gradleExitCode
