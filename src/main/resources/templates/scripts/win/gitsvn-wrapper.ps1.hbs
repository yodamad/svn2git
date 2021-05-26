param (
[string]$url,
[string]$username,
[string]$password,
[string]$certAcceptResponse,
[string]$logfile,
[string]$options
)

# Throw if Powershell Core
if ($PSVersionTable.PSEdition -eq "core") {
throw "This script is incompatible with PowerShell Core"
}

$pspath = "$pshome\powershell.exe"

# Load our C# code
Add-Type -Path "$PSScriptRoot\GitSvnCloneWrapper.cs"
$wrapper = New-Object GitSvnWrapper.GitSvnCloneWrapper -ArgumentList $pspath

# Execute
$result = $wrapper.Execute($url, $username, $password, $certAcceptResponse, $options)
$lastOutput = $wrapper.LastOutput
$lastError = $wrapper.LastError
$rc = $wrapper.ReturnCode # This is the rc from the git command

# Log the results
if ($logfile -ne "") {
"Execution Dump below" | Out-File -FilePath $logfile -Encoding utf8
$lastOutput | Out-File -FilePath $logfile -Encoding utf8  -Append
"----------------------" | Out-File -FilePath $logfile -Encoding utf8 -Append
"Execution Return Code: $result" | Out-File -FilePath $logfile -Encoding utf8 -Append
"Execution Error      : $lastError" | Out-File -FilePath $logfile -Encoding utf8  -Append
"Git Return Code      :" | Out-File -FilePath $logfile -Encoding utf8  -Append
# NB: we put the return code alone on the last line so that it is easy to retrieve and parse from the calling script
"$rc" | Out-File -FilePath $logfile -Encoding utf8  -Append
}
