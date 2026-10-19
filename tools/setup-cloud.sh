#!/usr/bin/env bash
set -euo pipefail
# Keep toolchains and caches outside the checkout. Never change source or lockfiles.
TASK_TOOLS=/workspace/android-tools
mkdir -p "$TASK_TOOLS/downloads" "$TASK_TOOLS/sdk/cmdline-tools" "$TASK_TOOLS/home" "$TASK_TOOLS/gradle-cache"
export ANDROID_HOME="$TASK_TOOLS/sdk"
export ANDROID_USER_HOME="$TASK_TOOLS/home"
export GRADLE_USER_HOME="$TASK_TOOLS/gradle-cache"
mkdir -p "$HOME/.android"
python3 - <<'PY'
import hashlib, os, shutil, pathlib, urllib.request, urllib.parse, xml.etree.ElementTree as E, zipfile, tarfile
root=pathlib.Path('/workspace/android-tools')
def get(url,path):
    with urllib.request.urlopen(url,timeout=120) as response, open(path,'wb') as output:
        import shutil
        shutil.copyfileobj(response,output)
def ensure(url,name):
    p=root/'downloads'/name
    if not p.exists(): get(url,p)
    return p
jdk=ensure('https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz','jdk.tar.gz')
jdk_hash=ensure('https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz.sha256.txt','jdk.sha256').read_text().split()[0]
if hashlib.sha256(jdk.read_bytes()).hexdigest()!=jdk_hash: raise SystemExit('JDK checksum mismatch')
shutil.copyfile('/etc/ssl/certs/java/cacerts',root/'java-cacerts')
if not (root/'jdk-21.0.8+9/bin/javac').exists():
    with tarfile.open(jdk) as t: t.extractall(root,filter='data')
gradle=ensure('https://services.gradle.org/distributions/gradle-8.13-bin.zip','gradle.zip')
checksum=ensure('https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256','gradle.sha256').read_text().strip()
if hashlib.sha256(gradle.read_bytes()).hexdigest()!=checksum: raise SystemExit('Gradle checksum mismatch')
cli=ensure('https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip','commandline.zip')
metadata=ensure('https://dl.google.com/android/repository/repository2-1.xml','repository.xml')
verified=False
for archive in E.parse(metadata).getroot().iter('archive'):
    url=archive.find('complete/url')
    if url is not None and url.text=='commandlinetools-linux-11076708_latest.zip':
        digest=archive.find('complete/checksum')
        verified=hashlib.new(digest.attrib.get('type','sha1'),cli.read_bytes()).hexdigest()==digest.text.strip()
if not verified: raise SystemExit('Android CLI checksum verification failed')
if not (root/'gradle-8.13/bin/gradle').exists():
    with zipfile.ZipFile(gradle) as z: z.extractall(root)
if not (root/'sdk/cmdline-tools/cmdline-tools/bin/sdkmanager').exists():
    with zipfile.ZipFile(cli) as z: z.extractall(root/'sdk/cmdline-tools')
for p in [root/'gradle-8.13/bin/gradle',*(root/'sdk/cmdline-tools/cmdline-tools/bin').iterdir()]: p.chmod(0o755)
# Java tools do not automatically read the platform's HTTPS_PROXY.
proxy=urllib.request.getproxies().get('https')
properties=root/'gradle-cache/gradle.properties'
if proxy:
    u=urllib.parse.urlsplit(proxy)
    if u.username or u.password: raise SystemExit('Authenticated proxy requires a supported Java credential binding')
    content=''.join(f'systemProp.{proto}.proxyHost={u.hostname}\nsystemProp.{proto}.proxyPort={u.port or 80}\n' for proto in ('http','https'))
    content+='systemProp.http.nonProxyHosts=localhost|127.*\n'
    # Dedicated generated cache file: preserve any unknown preexisting user configuration.
    if properties.exists() and properties.read_text()!=content:
        raise SystemExit('Existing Gradle proxy configuration differs; inspect before replacing')
    properties.write_text(content)
PY
export JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1 -Djavax.net.ssl.trustStore=$TASK_TOOLS/java-cacerts"
export JAVA_HOME="$TASK_TOOLS/jdk-21.0.8+9"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_EMULATOR_HOME="$TASK_TOOLS/home"
python3 - <<'PY'
import os, subprocess, pathlib
sdk=os.environ['ANDROID_HOME']
required=['platform-tools','platforms/android-35','build-tools/35.0.0']
if all((pathlib.Path(sdk)/p/'package.xml').is_file() for p in required):
    print('Required SDK packages already installed')
    raise SystemExit(0)
cmd=[sdk+'/cmdline-tools/cmdline-tools/bin/sdkmanager','--sdk_root='+sdk]
result=subprocess.run(cmd+['--licenses'],input='y\n'*20,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
if result.returncode: print(result.stdout); result.check_returncode()
subprocess.run(cmd+['platform-tools','platforms;android-35','build-tools;35.0.0'],check=True)
PY

if [ -f "$ANDROID_HOME/system-images/android-30/default/x86_64/package.xml" ] && [ ! -f "$ANDROID_USER_HOME/avd/nll-test30.ini" ]; then
    python3 - <<'PYAVD'
import os, subprocess, pathlib
subprocess.run([os.environ['ANDROID_HOME']+'/cmdline-tools/cmdline-tools/bin/avdmanager','create','avd','--name','nll-test30','--package','system-images;android-30;default;x86_64','--device','pixel'],input='no\n',text=True,check=True)
p=pathlib.Path(os.environ['ANDROID_USER_HOME'])/'avd/nll-test30.avd/config.ini'
values=dict(line.split('=',1) for line in p.read_text().splitlines() if '=' in line)
values.update({'hw.lcd.width':'720','hw.lcd.height':'1440','hw.lcd.density':'320','disk.dataPartition.size':'2G','showDeviceFrame':'no'})
p.write_text(''.join(k+'='+v+'\n' for k,v in values.items()))
PYAVD
fi
