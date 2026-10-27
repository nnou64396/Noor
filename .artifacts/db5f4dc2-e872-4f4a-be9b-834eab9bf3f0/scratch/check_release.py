import urllib.request
import json

url = "https://api.github.com/repos/nnou64396/Noor/releases/tags/v1.1.4"
req = urllib.request.Request(
    url,
    headers={"User-Agent": "Noor-Audit-Script", "Accept": "application/vnd.github+json"}
)

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        print(f"Tag Name: {data.get('tag_name')}")
        print(f"Name: {data.get('name')}")
        print(f"Draft: {data.get('draft')}")
        print(f"Prerelease: {data.get('prerelease')}")
        print(f"Published At: {data.get('published_at')}")
        print("Assets:")
        for asset in data.get('assets', []):
            print(f"  - Name: {asset.get('name')}, Size: {asset.get('size')}, URL: {asset.get('browser_download_url')}")
except Exception as e:
    print(f"Error fetching release: {e}")
