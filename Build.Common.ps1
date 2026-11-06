$ErrorActionPreference='Stop'
$taskRoot=$PSScriptRoot
$localConfig=@{}
if(Test-Path -LiteralPath "$taskRoot\local.build.json"){
    $loadedConfig=Get-Content -LiteralPath "$taskRoot\local.build.json" -Raw | ConvertFrom-Json
    foreach($property in $loadedConfig.PSObject.Properties){$localConfig[$property.Name]=$property.Value}
}
function Choose-BuildValue([string]$environmentName,[string]$configName,[string]$fallback){
    $value=[Environment]::GetEnvironmentVariable($environmentName)
    if($value){return $value}
    if($localConfig.ContainsKey($configName) -and $localConfig[$configName]){return [string]$localConfig[$configName]}
    return $fallback
}
$sdk=Choose-BuildValue 'ANDROID_SDK_ROOT' 'SdkRoot' $env:ANDROID_HOME
if(!$sdk){$sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'}
$jdk=Choose-BuildValue 'JAVA_HOME' 'JdkHome' ''
if(!$jdk){$javacCommand=Get-Command javac.exe -ErrorAction SilentlyContinue;if($javacCommand){$jdk=Split-Path (Split-Path $javacCommand.Source)}}
if(!$jdk -or !(Test-Path -LiteralPath (Join-Path $jdk 'bin\javac.exe'))){throw 'Install JDK 17+ and set JAVA_HOME.'}
$env:JAVA_HOME=$jdk
$bt=Join-Path $sdk 'build-tools\36.0.0'
$androidJar=Join-Path $sdk 'platforms\android-36\android.jar'
if(!(Test-Path -LiteralPath $androidJar) -or !(Test-Path -LiteralPath "$bt\aapt2.exe")){throw 'Install Android SDK platform android-36 and build-tools 36.0.0; set ANDROID_SDK_ROOT.'}
function RunChecked([string]$exe,[string[]]$arguments){
    & $exe @arguments
    if($LASTEXITCODE -ne 0){throw "$exe failed: $LASTEXITCODE"}
}
$signingKey=Choose-BuildValue 'MORCOOL_KEYSTORE' 'Keystore' (Join-Path $taskRoot 'keys\morcool-sender.jks')
$signingPasswordFile=Choose-BuildValue 'MORCOOL_KEYSTORE_PASSWORD_FILE' 'PasswordFile' (Join-Path $taskRoot 'keys\signing-password.txt')
$signingAlias=Choose-BuildValue 'MORCOOL_KEY_ALIAS' 'KeyAlias' 'morcool-sender'
if(![IO.Path]::IsPathRooted($signingKey)){$signingKey=Join-Path $taskRoot $signingKey}
if(![IO.Path]::IsPathRooted($signingPasswordFile)){$signingPasswordFile=Join-Path $taskRoot $signingPasswordFile}
function Initialize-LocalSigning {
    if(Test-Path -LiteralPath $signingKey){
        if(!(Test-Path -LiteralPath $signingPasswordFile)){throw 'Existing keystore found. Set MORCOOL_KEYSTORE_PASSWORD_FILE to its private password file.'}
        return
    }
    New-Item -ItemType Directory -Force -Path (Split-Path $signingKey),(Split-Path $signingPasswordFile) | Out-Null
    if(!(Test-Path -LiteralPath $signingPasswordFile)){
        $random=[Security.Cryptography.RandomNumberGenerator]::Create()
        try{$passwordBytes=New-Object byte[] 32;$random.GetBytes($passwordBytes);$password=[Convert]::ToBase64String($passwordBytes)}finally{$random.Dispose()}
        Set-Content -LiteralPath $signingPasswordFile -Value $password -Encoding ascii -NoNewline
    }
    RunChecked "$jdk\bin\keytool.exe" @('-genkeypair','-keystore',$signingKey,'-storepass:file',$signingPasswordFile,'-alias',$signingAlias,'-keyalg','RSA','-keysize','3072','-validity','3650','-dname','CN=Morcool Sender Local Build')
}
