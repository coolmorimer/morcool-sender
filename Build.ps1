param([switch]$Qa)
. "$PSScriptRoot\Build.Common.ps1"
New-Item -ItemType Directory -Force -Path "$taskRoot\build\classes","$taskRoot\build\dex","$taskRoot\build\gen","$taskRoot\keys" | Out-Null
RunChecked "$bt\aapt2.exe" @('compile','--dir',"$taskRoot\res",'-o',"$taskRoot\build\res.zip")
$buildManifest=Join-Path $taskRoot 'build\AndroidManifest.xml'
$manifestText=Get-Content -LiteralPath "$taskRoot\AndroidManifest.xml" -Raw
if($Qa){$manifestText=$manifestText.Replace('android:networkSecurityConfig="@xml/network_security"','').Replace('android:usesCleartextTraffic="false"','android:usesCleartextTraffic="true"').Replace('android:label="Morcool Sender"','android:label="Morcool Sender QA"')}
Set-Content -LiteralPath $buildManifest -Encoding utf8 -Value $manifestText
RunChecked "$bt\aapt2.exe" @('link','-o',"$taskRoot\build\unsigned.apk",'-I',$androidJar,'--manifest',$buildManifest,'--java',"$taskRoot\build\gen",'-A',"$taskRoot\assets",'--auto-add-overlay',"$taskRoot\build\res.zip")
$flagPath=Join-Path $taskRoot 'build\gen\ru\morcool\sender\BuildFlags.java'
New-Item -ItemType Directory -Force -Path (Split-Path $flagPath) | Out-Null
$qaLiteral=if($Qa){'true'}else{'false'}
Set-Content -LiteralPath $flagPath -Encoding utf8 -Value "package ru.morcool.sender; public final class BuildFlags { public static final boolean QA=$qaLiteral; }"
$sources=@(Get-ChildItem "$taskRoot\src","$taskRoot\build\gen" -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
RunChecked "$jdk\bin\javac.exe" (@('-encoding','UTF-8','--release','8','-classpath',$androidJar,'-d',"$taskRoot\build\classes")+$sources)
RunChecked "$jdk\bin\jar.exe" @('cf',"$taskRoot\build\classes.jar",'-C',"$taskRoot\build\classes",'.')
RunChecked "$bt\d8.bat" @('--lib',$androidJar,'--min-api','23','--output',"$taskRoot\build\dex","$taskRoot\build\classes.jar")
RunChecked "$jdk\bin\jar.exe" @('uf',"$taskRoot\build\unsigned.apk",'-C',"$taskRoot\build\dex",'classes.dex')
RunChecked "$bt\zipalign.exe" @('-f','4',"$taskRoot\build\unsigned.apk","$taskRoot\build\aligned.apk")
Initialize-LocalSigning
RunChecked "$bt\apksigner.bat" @('sign','--ks',$signingKey,'--ks-key-alias',$signingAlias,'--ks-pass',"file:$signingPasswordFile",'--out',"$taskRoot\Morcool-Sender-Next.apk","$taskRoot\build\aligned.apk")
RunChecked "$bt\apksigner.bat" @('verify','--verbose',"$taskRoot\Morcool-Sender-Next.apk")


if($Qa){Copy-Item -LiteralPath "$taskRoot\Morcool-Sender-Next.apk" -Destination "$taskRoot\Morcool-Sender-Next-QA.apk" -Force}
