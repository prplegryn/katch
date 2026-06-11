import html
import json
import os
import re
import time
import urllib.request
from http.cookiejar import MozillaCookieJar
from urllib.parse import unquote

from yt_dlp import YoutubeDL
from yt_dlp.extractor.common import InfoExtractor


MEDIA_URL_RE = re.compile(
    r"https?:\\?/\\?/[^\"'<>\s]+?(?:\.mp4|\.m3u8|\.mpd)(?:\?[^\"'<>\s]*)?",
    re.IGNORECASE,
)


def probe(url, cookie_path, user_agent):
    opts = _ydl_options(cookie_path, user_agent)
    opts.update(
        {
            "skip_download": True,
            "extract_flat": False,
            "noplaylist": False,
        }
    )
    with YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)
        result = _summarize_info(info)
        result["formats"].extend(
            _probe_page_source(
                ydl=ydl,
                url=result.get("webpage_url") or url,
                cookie_path=cookie_path,
                user_agent=user_agent,
                video_id=result.get("id") or "xiaohongshu",
            )
        )
        result["format_count"] = len(result["formats"])
        return json.dumps(result, ensure_ascii=False)


def download(url, format_id, direct_url, output_dir, cookie_path, user_agent, callback):
    os.makedirs(output_dir, exist_ok=True)
    before = set(_files_in(output_dir))
    started = time.time()

    def hook(event):
        status = event.get("status") or ""
        downloaded = int(event.get("downloaded_bytes") or 0)
        total = int(event.get("total_bytes") or event.get("total_bytes_estimate") or 0)
        speed = float(event.get("speed") or 0.0)
        eta = float(event.get("eta") or 0.0)
        percent = 100.0 if status == "finished" else (downloaded / total * 100.0 if total else 0.0)
        label = status or "running"
        try:
            callback.onProgress(float(percent), downloaded, total, speed, eta, label)
        except Exception:
            pass

    opts = _ydl_options(cookie_path, user_agent)
    opts.update(
        {
            "outtmpl": os.path.join(output_dir, "%(title).160B [%(id)s] %(format_id)s.%(ext)s"),
            "progress_hooks": [hook],
            "noplaylist": True,
            "windowsfilenames": True,
            "retries": 10,
            "fragment_retries": 10,
            "continuedl": True,
        }
    )

    target_url = direct_url if _should_download_direct(format_id, direct_url) else url
    if target_url == url:
        opts["format"] = format_id

    with YoutubeDL(opts) as ydl:
        ydl.download([target_url])

    after = set(_files_in(output_dir))
    files = sorted(after - before)
    if not files:
        files = sorted(after, key=lambda item: os.path.getmtime(item), reverse=True)[:1]

    try:
        callback.onProgress(100.0, 0, 0, 0.0, 0.0, "finished")
    except Exception:
        pass

    return json.dumps(
        {
            "files": files,
            "elapsed": round(time.time() - started, 3),
        },
        ensure_ascii=False,
    )


def _ydl_options(cookie_path, user_agent):
    opts = {
        "quiet": True,
        "no_warnings": True,
        "ignoreerrors": False,
        "http_headers": {
            "User-Agent": user_agent,
            "Referer": "https://www.xiaohongshu.com/",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.6",
        },
    }
    if cookie_path and os.path.exists(cookie_path):
        opts["cookiefile"] = cookie_path
    return opts


def _summarize_info(info):
    root = _first_video_info(info)
    formats = []
    if root is info:
        formats.extend(_summarize_formats(root.get("formats") or [], "yt-dlp", None))
    else:
        for index, entry in enumerate(_entries(info)):
            formats.extend(
                _summarize_formats(
                    entry.get("formats") or [],
                    "yt-dlp-entry",
                    "entry-%03d" % index,
                )
            )

    if not formats:
        formats.extend(_summarize_formats(root.get("formats") or [], "yt-dlp", None))

    return {
        "id": root.get("id") or info.get("id") or "",
        "title": root.get("title") or info.get("title") or "小红书内容",
        "webpage_url": root.get("webpage_url") or info.get("webpage_url") or "",
        "extractor": root.get("extractor") or info.get("extractor") or "",
        "thumbnail": root.get("thumbnail") or info.get("thumbnail"),
        "duration": root.get("duration") or info.get("duration"),
        "formats": formats,
    }


def _entries(info):
    entries = info.get("entries") or []
    return [entry for entry in entries if isinstance(entry, dict)]


def _first_video_info(info):
    if isinstance(info, dict) and info.get("formats"):
        return info
    for entry in _entries(info):
        if entry.get("formats"):
            return entry
    return info


def _summarize_formats(formats, source, prefix):
    output = []
    for index, item in enumerate(formats):
        if not isinstance(item, dict):
            continue
        copied = _format_payload(item)
        original_id = copied.get("format_id") or "format-%03d" % index
        copied["format_id"] = "%s-%s" % (prefix, original_id) if prefix else original_id
        copied["akatcha_source"] = source
        output.append(copied)
    return output


def _format_payload(item):
    keys = [
        "format_id",
        "format",
        "format_note",
        "ext",
        "resolution",
        "width",
        "height",
        "fps",
        "vcodec",
        "acodec",
        "dynamic_range",
        "filesize",
        "filesize_approx",
        "tbr",
        "protocol",
        "url",
    ]
    return {key: item.get(key) for key in keys if key in item and item.get(key) is not None}


def _probe_page_source(ydl, url, cookie_path, user_agent, video_id):
    try:
        page = _fetch_text(url, cookie_path, user_agent)
    except Exception:
        return []

    page = page.replace("\\u002F", "/").replace("\\u002f", "/").replace("\\/", "/")
    urls = []
    for match in MEDIA_URL_RE.finditer(page):
        cleaned = _clean_url(match.group(0))
        if cleaned:
            urls.append(cleaned)

    formats = []
    extractor = InfoExtractor(ydl)
    extractor._downloader = ydl
    for index, media_url in enumerate(urls[:400]):
        lower = media_url.lower().split("?", 1)[0]
        try:
            if lower.endswith(".m3u8"):
                nested = extractor._extract_m3u8_formats(
                    media_url,
                    video_id,
                    ext="mp4",
                    m3u8_id="page-source-%03d" % index,
                    fatal=False,
                    headers=_headers(user_agent),
                )
                for item in nested:
                    payload = _format_payload(item)
                    payload["format_id"] = payload.get("format_id") or "page-source-%03d" % index
                    payload["akatcha_source"] = "page-manifest"
                    formats.append(payload)
            elif lower.endswith(".mpd"):
                nested = extractor._extract_mpd_formats(
                    media_url,
                    video_id,
                    mpd_id="page-source-%03d" % index,
                    fatal=False,
                )
                for item in nested:
                    payload = _format_payload(item)
                    payload["format_id"] = payload.get("format_id") or "page-source-%03d" % index
                    payload["akatcha_source"] = "page-manifest"
                    formats.append(payload)
            else:
                formats.append(
                    {
                        "format_id": "page-source-%03d" % index,
                        "format": "page source mp4",
                        "format_note": "page source",
                        "ext": "mp4",
                        "protocol": "https",
                        "url": media_url,
                        "akatcha_source": "page-source",
                    }
                )
        except Exception:
            formats.append(
                {
                    "format_id": "page-source-%03d" % index,
                    "format": "page source media",
                    "format_note": "page source fallback",
                    "url": media_url,
                    "akatcha_source": "page-source",
                }
            )
    return formats


def _fetch_text(url, cookie_path, user_agent):
    opener = urllib.request.build_opener()
    if cookie_path and os.path.exists(cookie_path):
        jar = MozillaCookieJar()
        jar.load(cookie_path, ignore_discard=True, ignore_expires=True)
        opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    request = urllib.request.Request(url, headers=_headers(user_agent))
    with opener.open(request, timeout=20) as response:
        data = response.read(6_000_000)
    return data.decode("utf-8", "replace")


def _headers(user_agent):
    return {
        "User-Agent": user_agent,
        "Referer": "https://www.xiaohongshu.com/",
        "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.6",
    }


def _clean_url(candidate):
    text = candidate.replace("\\/", "/")
    text = html.unescape(unquote(text))
    text = text.strip().strip('"').strip("'")
    if not text.startswith(("http://", "https://")):
        return None
    return text


def _should_download_direct(format_id, direct_url):
    if not direct_url:
        return False
    return (
        format_id.startswith("page-source")
        or format_id.startswith("entry-")
        or format_id.startswith("page-")
    )


def _files_in(path):
    output = []
    for root, _, files in os.walk(path):
        for name in files:
            output.append(os.path.join(root, name))
    return output
