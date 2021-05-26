param (
    [string]$url,
    [string]$username,
    [string]$password,
    [string]$certAcceptResponse,
    [string]$options
)

# $logfile = "c:\temp\svngit.log" # For tests
$logfile = New-TemporaryFile

# Throw if Powershell Core
if ($PSVersionTable.PSEdition -eq "core") {
    throw "This script is incompatible with PowerShell Core"
}

try {
    $command = "`{ $PSScriptRoot\gitsvn-wrapper.ps1 -url $url -username $username -password $password -certAcceptResponse $certAcceptResponse -logfile $logfile -options $options `}"

    Write-Host "Execution in progress. It may take some time before some output appears. Please wait..."
    Invoke-Expression "powershell -Command ${command}"
    Get-Content $logfile
}
finally {
    if (Test-Path $logfile) {
    Remove-Item $logfile
    }
}
