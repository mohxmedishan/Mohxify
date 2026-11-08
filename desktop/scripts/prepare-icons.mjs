import { readFile, writeFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));
const source = path.resolve(here, "../src-tauri/icons/icon.png.b64");
const output = path.resolve(here, "../src-tauri/icons/icon.png");
const encoded = (await readFile(source, "utf8")).replace(/\s+/g, "");
await writeFile(output, Buffer.from(encoded, "base64"));
const result = spawnSync("npx", ["tauri", "icon", output], {
  cwd: path.resolve(here, ".."),
  stdio: "inherit",
  shell: process.platform === "win32"
});
if (result.status !== 0) process.exit(result.status ?? 1);
