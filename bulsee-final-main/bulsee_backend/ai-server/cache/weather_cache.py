from datetime import datetime, timedelta
from threading import Lock

_cache_store = {}
_lock = Lock()

def get_cached_data(key):
    with _lock:
        if key in _cache_store:
            entry = _cache_store[key]
            if entry["expire"] > datetime.now():
                return entry["data"]
            else:
                del _cache_store[key]
    return None

def save_to_cache(key, data, duration_minutes=10):
    with _lock:
        expire_time = datetime.now() + timedelta(minutes=duration_minutes)
        _cache_store[key] = {
            "data": data,
            "expire": expire_time
        }