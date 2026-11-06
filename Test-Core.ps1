. "$PSScriptRoot\Build.Common.ps1"
$pure=Join-Path $taskRoot 'build\pure'
New-Item -ItemType Directory -Force -Path $pure | Out-Null
$mainNames=@('Core','Sheets','CompletionWatch','SkipPolicy','AccessState','Channels')
$testNames=@('CoreTest','CompletionWatchTest','SkipPolicyTest','AccessStateTest','ChannelsTest','ChannelIdentityTest')
$sources=@($mainNames | ForEach-Object {Join-Path $taskRoot "src\ru\morcool\sender\$_.java"})+@($testNames | ForEach-Object {Join-Path $taskRoot "tests\$_.java"})
RunChecked "$jdk\bin\javac.exe" (@('-encoding','UTF-8','--release','8','-d',$pure)+$sources)
foreach($name in $testNames){
    $arguments=@('-cp',$pure,$name)
    if($name -eq 'CoreTest'){$arguments+=Join-Path $taskRoot 'tests\fixture.xlsx'}
    RunChecked "$jdk\bin\java.exe" $arguments
}

$nodeCommand=Get-Command node -ErrorAction SilentlyContinue
if($nodeCommand){RunChecked $nodeCommand.Source @((Join-Path $taskRoot 'tests\WebAddressEvidenceTest.js'))}else{Write-Warning 'Node.js not found: web-address regressions were not run.'}
