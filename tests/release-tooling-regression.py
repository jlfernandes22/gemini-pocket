from pathlib import Path
import hashlib,re
root=Path('.')
expected='498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17'
assert hashlib.sha256((root/'gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest()==expected
props=(root/'gradle/wrapper/gradle-wrapper.properties').read_text()
assert 'distributionSha256Sum=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab' in props
for p in (root/'.github/workflows').glob('*.yml'):
 s=p.read_text()
 for ref in re.findall(r'uses:\s*([^\s#]+)',s):assert re.search(r'@[0-9a-f]{40}$',ref),ref
 assert 'sh gradlew --no-daemon' in s and expected in s
release=(root/'.github/workflows/release.yml').read_text()
assert 'fde3133f5fc9aa3ddaa429edf43bbc646b6f0d8e23928eca6fbc736d1dfcb206' in release
print('PASS: wrapper checksum, pinned Actions and release-certificate guards')

assert "--latest=false" in release and "group: promote-latest-release" in release
assert "sort -t- -k2,2n" in release
