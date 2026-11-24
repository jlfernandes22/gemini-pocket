from pathlib import Path

sources = Path('app/src/main/java/com/geminipocket')
activity = (sources / 'MainActivity.java').read_text()
transport = (sources / 'ImageDownloads.java').read_text()
manifest = Path('app/src/main/AndroidManifest.xml').read_text()
assert 'web.setDownloadListener(' in activity and 'web!=downloadPage' in activity
assert 'ImageDownloads.SAVE_FILE' in activity and 'imageDownloads.result(result,data)' in activity
assert 'imageDownloads.start(downloadPage,url,agent,disposition,mime,length)' in activity
assert 'ImageDownloadPolicy.validText(in)' in transport and 'Intent.EXTRA_TITLE, name' in transport
assert 'conn.setInstanceFollowRedirects(false)' in transport
assert transport.index('!ImageDownloadPolicy.https(url)') < transport.index('getCookie(url)')
assert 'postWebMessage' in transport and 'Uri.parse(ImageDownloadPolicy.ORIGIN)' in transport
assert 'addJavascriptInterface' not in activity + transport
assert 'Intent.ACTION_CREATE_DOCUMENT' in transport and 'isDocumentUri(activity, destination)' in transport
assert 'WRITE_EXTERNAL_STORAGE' not in manifest and 'READ_MEDIA_IMAGES' not in manifest
print('PASS: isolated download wiring, restricted transfer, and permissionless save picker')
