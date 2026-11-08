import "./style.css";

const KEY = "mohxify.desktop.v1";
const MAX_BACKUP_CHARS = 2000000;
const MAX_LOADOUTS = 50;
const MAX_RULES_PER_LOADOUT = 10000;
const MAX_TOTAL_RULES = 20000;
let state = readState();
let toastTimer;
let synonymCycle = 0;

function readState() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY));
    if (saved && Array.isArray(saved.loadouts) && saved.loadouts.length) return saved;
  } catch {}
  return { activeId: "loadout-1", loadouts: [{ id: "loadout-1", name: "Loadout 1", rules: [] }] };
}
function active() { return state.loadouts.find(x => x.id === state.activeId) || state.loadouts[0]; }
function save() { localStorage.setItem(KEY, JSON.stringify(state)); render(); }
function notify(message) {
  const toast = document.querySelector("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2200);
}
function esc(value) {
  return String(value).replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
}
function caseLike(word, replacement) {
  const upper = word.length > 1 && word === word.toLocaleUpperCase();
  const title = word[0] && word[0] === word[0].toLocaleUpperCase() && word.slice(1) === word.slice(1).toLocaleLowerCase();
  if (upper) return replacement.toLocaleUpperCase();
  if (title) return replacement.charAt(0).toLocaleUpperCase() + replacement.slice(1);
  return replacement;
}
function transform(text, rules, cycle = 0) {
  const sorted = rules.filter(rule => rule.from.trim()).sort((a, b) => b.from.length - a.from.length);
  if (!sorted.length) return text;
  const alternatives = new Map();
  for (const rule of sorted) {
    const key = rule.from.toLocaleLowerCase();
    if (!alternatives.has(key)) alternatives.set(key, []);
    if (!alternatives.get(key).some(value => value.toLocaleLowerCase() === rule.to.toLocaleLowerCase())) alternatives.get(key).push(rule.to);
  }
  const escaped = sorted.map(rule => rule.from.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
  const pattern = new RegExp("(^|[^\\p{L}\\p{N}_])(" + escaped.join("|") + ")(?![\\p{L}\\p{N}_])", "giu");
  return text.replace(pattern, (match, prefix, word) => {
    const choices = alternatives.get(word.toLocaleLowerCase());
    if (!choices || !choices.length) return match;
    return prefix + caseLike(word, choices[cycle % choices.length]);
  });
}
function unwrap(value) {
  let result = value.trim(), changed = true;
  while (changed && result.length >= 2) {
    changed = false;
    const first = result[0], last = result[result.length - 1];
    if ((first === "[" && last === "]") || (first === '"' && last === '"') || (first === "'" && last === "'") || (first === "`" && last === "`")) {
      result = result.slice(1, -1).trim();
      changed = true;
    }
  }
  return result;
}
function parseRules(raw) {
  const entries = raw.split(/\r?\n/).filter(line => !line.trimStart().startsWith("#")).join("\n").split(/[;\n]+/);
  const rules = [], seen = new Set();
  for (const rawEntry of entries) {
    const entry = rawEntry.trim();
    if (!entry) continue;
    const colon = entry.indexOf(":");
    if (colon < 0) continue;
    const from = unwrap(entry.slice(0, colon));
    const to = unwrap(entry.slice(colon + 1));
    if (!from || /\s/.test(from) || !to) continue;
    const key = from.toLocaleLowerCase() + "\u0000" + to.toLocaleLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    rules.push({ from, to });
  }
  return rules;
}
function serializeRules(rules) { return rules.map(rule => rule.from + ": " + rule.to + ";").join("\n") + (rules.length ? "\n" : ""); }
function download(name, data, type = "text/plain;charset=utf-8") {
  const url = URL.createObjectURL(new Blob([data], { type }));
  const link = document.createElement("a");
  link.href = url; link.download = name; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function setView(name) {
  document.querySelectorAll(".view").forEach(view => view.classList.add("hidden"));
  document.querySelector("#view-" + name).classList.remove("hidden");
  document.querySelectorAll(".nav-item").forEach(button => button.classList.toggle("active", button.dataset.view === name));
  document.querySelector("#page-title").textContent = ({ editor: "Text editor", loadouts: "Loadouts", rules: "Rule library" })[name];
  if (name === "rules") renderRules();
  if (name === "loadouts") renderLoadouts();
}
function render() {
  document.querySelector("#active-loadout-label").textContent = active().name;
  renderRules(); renderLoadouts();
}
function renderRules() {
  const root = document.querySelector("#rules-list");
  if (!root) return;
  const query = document.querySelector("#rule-search").value.toLowerCase();
  const rows = active().rules.filter(rule => (rule.from + " " + rule.to).toLowerCase().includes(query));
  root.innerHTML = rows.map(rule => {
    const index = active().rules.indexOf(rule);
    return '<div class="rule-row"><input aria-label="Original" data-index="' + index + '" data-field="from" value="' + esc(rule.from) + '"><input aria-label="Replacement" data-index="' + index + '" data-field="to" value="' + esc(rule.to) + '"><button class="delete-rule" data-delete="' + index + '" title="Delete">×</button></div>';
  }).join("") || '<div class="rules-footer">No matching rules. Add a rule or change your search.</div>';
  document.querySelector("#rules-count").textContent = rows.length + " of " + active().rules.length + " rules";
}
function renderLoadouts() {
  const root = document.querySelector("#loadout-list");
  if (!root) return;
  root.innerHTML = state.loadouts.map(loadout =>
    '<article class="loadout-card ' + (loadout.id === state.activeId ? "selected" : "") + '"><h3>' + esc(loadout.name) + (loadout.id === state.activeId ? " ✓" : "") + '</h3><p>' + loadout.rules.length + ' replacement rules</p><div class="card-actions"><button class="secondary" data-activate="' + esc(loadout.id) + '">' + (loadout.id === state.activeId ? "Active" : "Use loadout") + '</button><button class="subtle-btn" data-rename="' + esc(loadout.id) + '">Rename</button>' + (loadout.id !== "loadout-1" ? '<button class="subtle-btn" data-remove="' + esc(loadout.id) + '">Delete</button>' : "") + '</div></article>'
  ).join("");
}
function updateCounts() {
  document.querySelector("#input-count").textContent = document.querySelector("#input-text").value.length + " characters";
  document.querySelector("#output-count").textContent = document.querySelector("#output-text").value.length + " characters";
}
function runTransform() {
  synonymCycle = 0;
  document.querySelector("#output-text").value = transform(document.querySelector("#input-text").value, active().rules, synonymCycle);
  updateCounts();
  notify("Text transformed.");
}
document.querySelectorAll(".nav-item").forEach(button => button.addEventListener("click", () => setView(button.dataset.view)));
document.querySelector("#transform-btn").addEventListener("click", runTransform);
document.querySelector("#input-text").addEventListener("input", updateCounts);
document.querySelector("#clear-input").addEventListener("click", () => {
  document.querySelector("#input-text").value = "";
  document.querySelector("#output-text").value = "";
  updateCounts();
});
document.querySelector("#cycle-btn").addEventListener("click", () => {
  if (!active().rules.some(rule => active().rules.filter(item => item.from.toLocaleLowerCase() === rule.from.toLocaleLowerCase()).length > 1)) return notify("This loadout has no synonyms to cycle.");
  synonymCycle++;
  document.querySelector("#output-text").value = transform(document.querySelector("#input-text").value, active().rules, synonymCycle);
  updateCounts();
  notify("Showing the next synonym set.");
});
document.querySelector("#copy-btn").addEventListener("click", async () => {
  const value = document.querySelector("#output-text").value;
  if (!value) return notify("Nothing to copy yet.");
  try { await navigator.clipboard.writeText(value); notify("Copied to clipboard."); }
  catch { notify("Clipboard access unavailable."); }
});
document.querySelector("#paste-btn").addEventListener("click", async () => {
  try { document.querySelector("#input-text").value = await navigator.clipboard.readText(); updateCounts(); }
  catch { notify("Clipboard access unavailable."); }
});
document.querySelector("#new-loadout").addEventListener("click", () => {
  const name = prompt("Name your new loadout:");
  if (!name || !name.trim()) return;
  const item = { id: crypto.randomUUID(), name: name.trim(), rules: [] };
  state.loadouts.push(item); state.activeId = item.id; save(); notify("Loadout created.");
});
document.querySelector("#loadout-list").addEventListener("click", event => {
  const button = event.target.closest("button");
  if (!button) return;
  if (button.dataset.activate) { state.activeId = button.dataset.activate; save(); notify("Active loadout changed."); }
  if (button.dataset.rename) {
    const loadout = state.loadouts.find(item => item.id === button.dataset.rename);
    const name = prompt("Rename loadout:", loadout.name);
    if (name && name.trim()) { loadout.name = name.trim(); save(); }
  }
  if (button.dataset.remove) {
    if (!confirm("Delete this loadout and its rules?")) return;
    state.loadouts = state.loadouts.filter(item => item.id !== button.dataset.remove);
    if (state.activeId === button.dataset.remove) state.activeId = state.loadouts[0].id;
    save(); notify("Loadout deleted.");
  }
});
document.querySelector("#add-rule").addEventListener("click", () => {
  const from = prompt("Original word (single word):");
  if (!from) return;
  const to = prompt("Replacement:");
  if (!to) return;
  if (/\s/.test(from.trim())) return notify("Originals must be a single word, like the Android app.");
  active().rules.push({ from: from.trim(), to: to.trim() }); save(); notify("Rule added.");
});
document.querySelector("#rule-search").addEventListener("input", renderRules);
document.querySelector("#rules-list").addEventListener("change", event => {
  const input = event.target.closest("input[data-index]");
  if (!input) return;
  const value = input.value.trim();
  if (!value || (input.dataset.field === "from" && /\s/.test(value))) { notify("Use a non-empty single-word original."); renderRules(); return; }
  active().rules[Number(input.dataset.index)][input.dataset.field] = value;
  save();
});
document.querySelector("#rules-list").addEventListener("click", event => {
  const button = event.target.closest("[data-delete]");
  if (!button) return;
  active().rules.splice(Number(button.dataset.delete), 1); save(); notify("Rule removed.");
});
document.querySelector("#export-rules").addEventListener("click", () => download("mohxify-" + active().name.toLowerCase().replace(/[^a-z0-9]+/g, "-") + ".txt", serializeRules(active().rules)));
document.querySelector("#import-rules").addEventListener("click", () => document.querySelector("#rules-file").click());
document.querySelector("#rules-file").addEventListener("change", async event => {
  const file = event.target.files[0];
  if (!file) return;
  const rules = parseRules(await file.text());
  if (!rules.length) { notify("No valid rules found."); event.target.value = ""; return; }
  const merge = confirm("Import " + rules.length + " rules? OK merges; Cancel replaces this loadout.");
  active().rules = merge ? active().rules.concat(rules) : rules;
  save(); event.target.value = ""; notify(rules.length + " rules imported.");
});
document.querySelector("#export-backup").addEventListener("click", () => {
  const backup = { format: "mohxify-loadouts-v1", activeLoadoutId: state.activeId, loadouts: state.loadouts };
  download("mohxify-backup.json", JSON.stringify(backup, null, 2), "application/json");
});
document.querySelector("#import-backup").addEventListener("click", () => document.querySelector("#backup-file").click());
document.querySelector("#backup-file").addEventListener("change", async event => {
  const file = event.target.files[0];
  if (!file) return;
  try {
    if (file.size > MAX_BACKUP_CHARS) throw Error("too large");
    const backup = JSON.parse(await file.text());
    if (backup.format !== "mohxify-loadouts-v1" || !Array.isArray(backup.loadouts) || backup.loadouts.length < 1 || backup.loadouts.length > MAX_LOADOUTS) throw Error("invalid format");
    const ids = new Set(), names = new Set();
    let total = 0;
    for (const loadout of backup.loadouts) {
      if (typeof loadout.id !== "string" || !loadout.id.trim() || loadout.id.length > 128 || typeof loadout.name !== "string" || !loadout.name.trim() || loadout.name.length > 60 || !Array.isArray(loadout.rules) || loadout.rules.length > MAX_RULES_PER_LOADOUT) throw Error("invalid loadout");
      if (ids.has(loadout.id) || names.has(loadout.name.toLocaleLowerCase())) throw Error("duplicate loadout");
      ids.add(loadout.id); names.add(loadout.name.toLocaleLowerCase());
      for (const rule of loadout.rules) {
        if (!rule || typeof rule.from !== "string" || typeof rule.to !== "string" || !rule.from.trim() || rule.from.length > 64 || /\s/.test(rule.from) || !rule.to.trim() || rule.to.length > 256 || rule.from.toLocaleLowerCase() === rule.to.toLocaleLowerCase()) throw Error("invalid rule");
      }
      total += loadout.rules.length;
      if (total > MAX_TOTAL_RULES) throw Error("too many rules");
    }
    if (!ids.has(backup.activeLoadoutId)) throw Error("invalid active loadout");
    if (!confirm("Replace all current loadouts with this Mohxify backup?")) return;
    state = { loadouts: backup.loadouts, activeId: backup.activeLoadoutId };
    save(); notify("Backup restored.");
  } catch { notify("That is not a valid Mohxify backup."); }
  finally { event.target.value = ""; }
});
document.querySelector("#theme-toggle").addEventListener("click", () => document.body.classList.toggle("light"));
document.addEventListener("keydown", event => {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter" && !document.querySelector("#view-editor").classList.contains("hidden")) { event.preventDefault(); runTransform(); }
});
render(); updateCounts();