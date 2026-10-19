// Compatibility entry point. The maintained suite, pinned dependencies and
// reports now live under webclient/e2e. Build Maven and Angular before running.
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(new URL('../../../webclient/package.json', import.meta.url));
const child = spawn(process.execPath,
  [require.resolve('@playwright/test/cli'), 'test', ...process.argv.slice(2)],
  { cwd: fileURLToPath(new URL('../../../webclient/', import.meta.url)),
    stdio: 'inherit', windowsHide: true });
child.on('error', error => { console.error(error); process.exitCode = 1; });
child.on('exit', code => { process.exitCode = code ?? 1; });
