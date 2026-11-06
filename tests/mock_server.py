"""Loopback-only transport fixture. Never forwards a request to a messenger."""
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from collections import Counter
import json, time

counts = Counter()
PAGE = '''<!doctype html><html><meta charset="utf-8"><body>
<input type="search" class="input-search-input" placeholder="Поиск">
<div role="listitem" id="result" style="display:none">Demo Person\n@demo_person</div>
<div class="chat-info"><span class="peer-title"></span></div>
<div contenteditable="true" class="input-message-input" aria-label="Message"></div>
<button aria-label="Send">Send</button><script>
window.sentCount=0;
document.querySelector('input').addEventListener('input',()=>document.querySelector('#result').style.display='block');
document.querySelector('#result').onclick=()=>document.querySelector('.peer-title').textContent='Demo Person';
document.querySelector('button').onclick=()=>{window.sentCount++;document.querySelector('[contenteditable]').textContent='';};
</script></body></html>'''

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass
    def reply(self, code, body):
        data = json.dumps(body).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    def do_GET(self):
        if self.path == '/reset':
            counts.clear()
            return self.reply(200, {'reset': True})
        if self.path == '/stats':
            return self.reply(200, dict(counts))
        data = PAGE.encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    def do_POST(self):
        request = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))))
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        target = str(request.get('chat_id', query.get('chat_id', query.get('user_id', ['']))[0]))
        provider = parsed.path.strip('/')
        counts[provider + ':' + target] += 1
        if provider == 'max_bot' and self.headers.get('Authorization') != 'qa-max-token':
            return self.reply(401, {})
        if target == '1002':
            return self.reply(403, {'ok': False, 'error_code': 403})
        if target == '1003' and counts[provider + ':' + target] == 1:
            return self.reply(429, {'ok': False, 'parameters': {'retry_after': 1}})
        if target == '1008':
            return self.reply(429, {'ok': False, 'parameters': {'retry_after': 1}})
        if target == '1005':
            self.close_connection = True
            return
        if target == '1006':
            time.sleep(2)
        if provider == 'telegram_bot':
            return self.reply(200, {'ok': True, 'result': {'message_id': int(target or '1')}})
        return self.reply(200, {'message': {'body': {'mid': 'mock-' + target}}})

print('Sender QA fixture ready on loopback:8879', flush=True)
ThreadingHTTPServer(('127.0.0.1', 8879), Handler).serve_forever()
