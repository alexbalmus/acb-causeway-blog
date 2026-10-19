// Serve the actual production bundle and proxy its API to the disposable backend.
import { createServer, request } from 'node:http';
import { readFileSync, statSync } from 'node:fs';
import { extname, resolve, sep } from 'node:path';

const root = resolve('dist/acb-blog-webclient/browser');
readFileSync(resolve(root, 'index.html')); // Fail immediately if the build is missing.
createServer((req, res) => {
  if (/^\/restful(?:\/|\?|$)/.test(req.url)) {
    const upstream = request({ hostname: '127.0.0.1', port: 18080,
      path: req.url, method: req.method, headers: req.headers }, incoming => {
      res.writeHead(incoming.statusCode, incoming.headers); incoming.pipe(res);
    });
    upstream.on('error', () => { res.writeHead(502); res.end(); });
    req.pipe(upstream);
    return;
  }
  try {
    const path = decodeURIComponent(new URL(req.url, 'http://127.0.0.1').pathname);
    let file = resolve(root, '.' + path);
    if (file !== root && !file.startsWith(root + sep)) { res.writeHead(403); res.end(); return; }
    try { if (!statSync(file).isFile()) file = resolve(root, 'index.html'); }
    catch { file = resolve(root, 'index.html'); }
    const types = { '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css', '.ico': 'image/x-icon' };
    res.setHeader('Content-Type', types[extname(file)] || 'application/octet-stream');
    res.end(readFileSync(file));
  } catch { res.writeHead(400); res.end(); }
}).listen(14200, '127.0.0.1');
