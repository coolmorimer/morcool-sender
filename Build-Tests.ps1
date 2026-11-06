. "$PSScriptRoot\Build.Common.ps1"
New-Item -ItemType Directory -Force -Path "$taskRoot\build\qa\classes","$taskRoot\build\qa\dex" | Out-Null
New-Item -ItemType Directory -Force -Path "$taskRoot\tests\assets" | Out-Null
Copy-Item -LiteralPath "$taskRoot\tests\fixture.xlsx" -Destination "$taskRoot\tests\assets\fixture.xlsx"
RunChecked "$bt\aapt2.exe" @('link','-o',"$taskRoot\build\qa\unsigned.apk",'-I',$androidJar,'--manifest',"$taskRoot\tests\AndroidManifest.xml",'-A',"$taskRoot\tests\assets")
RunChecked "$jdk\bin\javac.exe" @('-encoding','UTF-8','--release','8','-classpath',"$androidJar;$taskRoot\build\classes",'-d',"$taskRoot\build\qa\classes","$taskRoot\tests\NextTests.java")
RunChecked "$jdk\bin\jar.exe" @('cf',"$taskRoot\build\qa\classes.jar",'-C',"$taskRoot\build\qa\classes",'.')
RunChecked "$bt\d8.bat" @('--lib',$androidJar,'--classpath',"$taskRoot\build\classes.jar",'--min-api','23','--output',"$taskRoot\build\qa\dex","$taskRoot\build\qa\classes.jar")
RunChecked "$jdk\bin\jar.exe" @('uf',"$taskRoot\build\qa\unsigned.apk",'-C',"$taskRoot\build\qa\dex",'classes.dex')
RunChecked "$bt\zipalign.exe" @('-f','4',"$taskRoot\build\qa\unsigned.apk","$taskRoot\build\qa\aligned.apk")
RunChecked "$bt\apksigner.bat" @('sign','--ks',$signingKey,'--ks-key-alias',$signingAlias,'--ks-pass',"file:$signingPasswordFile",'--out',"$taskRoot\build\qa\tests.apk","$taskRoot\build\qa\aligned.apk")

