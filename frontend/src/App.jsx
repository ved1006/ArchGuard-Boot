import { useEffect, useRef, useState } from "react";
import * as d3 from "d3";

const defaultRepoUrl = "https://github.com/ved1006/campusCore";

const layerColors = {
  CONTROLLER: "#f97316",
  SERVICE:    "#10b981",
  REPOSITORY: "#f59e0b",
  ENTITY:     "#ef4444",
  DTO:        "#3b82f6",
  EXCEPTION:  "#a855f7",
  OTHER:      "#6b7280",
  EXTERNAL:   "#4b5563",
};

const styles = `
  @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500&family=Syne:wght@600;700;800&family=DM+Sans:wght@300;400;500&display=swap');

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  :root {
    --bg:         #080c10;
    --surface:    #0e1419;
    --surface-2:  #141c24;
    --border:     rgba(255,255,255,0.07);
    --border-hi:  rgba(255,255,255,0.14);
    --text:       #e2e8f0;
    --muted:      #64748b;
    --accent:     #10b981;
    --accent-dim: rgba(16,185,129,0.12);
    --warn:       #f59e0b;
    --danger:     #ef4444;
    --danger-dim: rgba(239,68,68,0.10);
    --font-head:  'Syne', sans-serif;
    --font-body:  'DM Sans', sans-serif;
    --font-mono:  'IBM Plex Mono', monospace;
    --radius:     6px;
    --radius-lg:  10px;
  }

  body {
    background: var(--bg);
    color: var(--text);
    font-family: var(--font-body);
    font-size: 14px;
    line-height: 1.6;
    min-height: 100vh;
  }

  /* ── Layout ─────────────────────────────────── */
  .app { display: flex; flex-direction: column; min-height: 100vh; }

  .topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 28px;
    height: 52px;
    border-bottom: 1px solid var(--border);
    background: var(--surface);
    position: sticky;
    top: 0;
    z-index: 100;
  }

  .topbar-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    font-family: var(--font-head);
    font-size: 15px;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text);
  }

  .brand-icon {
    width: 26px; height: 26px;
    background: linear-gradient(135deg, #10b981, #3b82f6);
    border-radius: 5px;
    display: flex; align-items: center; justify-content: center;
    font-size: 13px;
  }

  .health-badge {
    font-family: var(--font-mono);
    font-size: 11px;
    padding: 3px 10px;
    border-radius: 20px;
    font-weight: 500;
    border: 1px solid transparent;
  }
  .health-ok      { color: var(--accent); border-color: rgba(16,185,129,0.3); background: var(--accent-dim); }
  .health-offline { color: var(--danger); border-color: rgba(239,68,68,0.3);  background: var(--danger-dim); }
  .health-checking{ color: var(--muted);  border-color: var(--border); }

  .workspace {
    display: grid;
    grid-template-columns: 320px 1fr;
    gap: 0;
    flex: 1;
    min-height: 0;
  }

  /* ── Sidebar ─────────────────────────────────── */
  .sidebar {
    border-right: 1px solid var(--border);
    background: var(--surface);
    padding: 24px 20px;
    display: flex;
    flex-direction: column;
    gap: 24px;
    overflow-y: auto;
  }

  .sidebar-section-label {
    font-family: var(--font-mono);
    font-size: 10px;
    font-weight: 500;
    letter-spacing: 0.1em;
    color: var(--muted);
    text-transform: uppercase;
    margin-bottom: 10px;
  }

  .repo-form { display: flex; flex-direction: column; gap: 8px; }

  .repo-form label {
    font-size: 12px;
    color: var(--muted);
    font-weight: 500;
  }

  .repo-form input {
    width: 100%;
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 9px 12px;
    color: var(--text);
    font-family: var(--font-mono);
    font-size: 12px;
    outline: none;
    transition: border-color 0.15s;
  }
  .repo-form input:focus { border-color: var(--accent); }
  .repo-form input::placeholder { color: var(--muted); }

  .run-btn {
    margin-top: 4px;
    padding: 10px 14px;
    background: var(--accent);
    color: #04120c;
    font-family: var(--font-body);
    font-size: 13px;
    font-weight: 600;
    border: none;
    border-radius: var(--radius);
    cursor: pointer;
    transition: opacity 0.15s, transform 0.1s;
    letter-spacing: 0.01em;
  }
  .run-btn:hover:not(:disabled) { opacity: 0.88; }
  .run-btn:active:not(:disabled) { transform: scale(0.98); }
  .run-btn:disabled { opacity: 0.4; cursor: not-allowed; }

  /* Stats grid */
  .stats-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .stat-card {
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 12px 14px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    position: relative;
    overflow: hidden;
  }
  .stat-card::before {
    content: '';
    position: absolute;
    top: 0; left: 0;
    width: 3px; height: 100%;
    background: var(--stat-accent, var(--accent));
  }

  .stat-val {
    font-family: var(--font-head);
    font-size: 22px;
    font-weight: 700;
    color: var(--text);
    line-height: 1;
  }
  .stat-lbl {
    font-size: 11px;
    color: var(--muted);
    font-weight: 400;
  }

  /* Error */
  .error-box {
    background: var(--danger-dim);
    border: 1px solid rgba(239,68,68,0.25);
    border-radius: var(--radius);
    padding: 10px 14px;
    color: #fca5a5;
    font-size: 13px;
  }

  /* ── Main content ───────────────────────────── */
  .content {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  /* Tab bar */
  .tab-bar {
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--border);
    background: var(--surface);
    padding: 0 24px;
    gap: 0;
  }

  .tab-btn {
    padding: 14px 18px;
    font-size: 13px;
    font-weight: 500;
    color: var(--muted);
    background: none;
    border: none;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    transition: color 0.15s, border-color 0.15s;
    letter-spacing: 0.01em;
    font-family: var(--font-body);
  }
  .tab-btn:hover { color: var(--text); }
  .tab-btn.active { color: var(--text); border-bottom-color: var(--accent); }

  .tab-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px; height: 18px;
    padding: 0 5px;
    background: var(--surface-2);
    border-radius: 10px;
    font-size: 10px;
    font-family: var(--font-mono);
    color: var(--muted);
    margin-left: 6px;
  }
  .tab-count.has-issues { background: rgba(239,68,68,0.15); color: #fca5a5; }

  .tab-spacer { margin-left: auto; }

  .timestamp {
    font-family: var(--font-mono);
    font-size: 11px;
    color: var(--muted);
  }

  /* Tab panels */
  .tab-panel { display: none; flex: 1; overflow-y: auto; padding: 20px 24px; }
  .tab-panel.active { display: flex; flex-direction: column; gap: 10px; }

  /* Empty state */
  .empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: var(--muted);
    font-size: 13px;
    padding: 48px;
    text-align: center;
  }
  .empty-icon {
    font-size: 32px;
    opacity: 0.3;
  }

  /* Meta strip */
  .meta-strip {
    display: flex;
    gap: 0;
    border: 1px solid var(--border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    background: var(--surface-2);
    margin-bottom: 4px;
  }
  .meta-item {
    flex: 1;
    padding: 11px 16px;
    border-right: 1px solid var(--border);
    display: flex;
    flex-direction: column;
    gap: 3px;
  }
  .meta-item:last-child { border-right: none; }
  .meta-key { font-size: 10px; color: var(--muted); font-family: var(--font-mono); text-transform: uppercase; letter-spacing: 0.06em; }
  .meta-val { font-size: 13px; font-weight: 500; font-family: var(--font-mono); color: var(--text); }

  /* Issues list */
  .issue-card {
    background: var(--surface-2);
    border: 1px solid var(--border);
    border-left: 3px solid var(--border-hi);
    border-radius: var(--radius);
    padding: 13px 16px;
    display: flex;
    flex-direction: column;
    gap: 5px;
    transition: border-color 0.15s;
  }
  .issue-card:hover { border-color: var(--border-hi); }
  .issue-card.sev-critical { border-left-color: var(--danger); }
  .issue-card.sev-high     { border-left-color: #f97316; }
  .issue-card.sev-medium   { border-left-color: var(--warn); }
  .issue-card.sev-low      { border-left-color: #3b82f6; }

  .issue-top {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .issue-rule {
    font-family: var(--font-mono);
    font-size: 11px;
    color: var(--text);
    font-weight: 500;
    background: var(--surface);
    padding: 2px 8px;
    border-radius: 4px;
    border: 1px solid var(--border);
  }
  .issue-sev {
    font-family: var(--font-mono);
    font-size: 10px;
    font-weight: 500;
    padding: 2px 8px;
    border-radius: 20px;
    letter-spacing: 0.05em;
    text-transform: uppercase;
  }
  .sev-badge-critical { background: rgba(239,68,68,0.15); color: #fca5a5; }
  .sev-badge-high     { background: rgba(249,115,22,0.15); color: #fdba74; }
  .sev-badge-medium   { background: rgba(245,158,11,0.15); color: #fde68a; }
  .sev-badge-low      { background: rgba(59,130,246,0.15); color: #93c5fd; }

  .issue-msg { font-size: 13px; color: var(--text); line-height: 1.5; }
  .issue-loc {
    font-family: var(--font-mono);
    font-size: 11px;
    color: var(--muted);
  }

  .no-issues {
    display: flex; align-items: center; gap: 10px;
    background: var(--accent-dim);
    border: 1px solid rgba(16,185,129,0.2);
    border-radius: var(--radius);
    padding: 14px 16px;
    color: var(--accent);
    font-size: 13px;
    font-weight: 500;
  }

  /* ── Graph panel ────────────────────────────── */
  .graph-tab-panel {
    display: none;
    flex-direction: column;
    flex: 1;
    padding: 0;
  }
  .graph-tab-panel.active { display: flex; }

  .graph-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 24px;
    border-bottom: 1px solid var(--border);
    flex-wrap: wrap;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    color: var(--muted);
    font-family: var(--font-mono);
  }
  .legend-dot {
    width: 8px; height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .graph-metrics {
    margin-left: auto;
    display: flex;
    gap: 20px;
  }
  .gm-item { display: flex; flex-direction: column; align-items: flex-end; }
  .gm-val { font-family: var(--font-mono); font-size: 13px; font-weight: 500; color: var(--text); }
  .gm-key { font-size: 10px; color: var(--muted); }

  .graph-area {
    flex: 1;
    position: relative;
    background:
      radial-gradient(ellipse at 30% 40%, rgba(16,185,129,0.04) 0%, transparent 60%),
      radial-gradient(ellipse at 70% 70%, rgba(59,130,246,0.04) 0%, transparent 60%),
      var(--bg);
  }

  .graph-svg {
    width: 100%;
    height: 100%;
    display: block;
  }

  .graph-tooltip {
    position: fixed;
    pointer-events: none;
    background: var(--surface-2);
    border: 1px solid var(--border-hi);
    border-radius: var(--radius);
    padding: 10px 14px;
    font-family: var(--font-mono);
    font-size: 11.5px;
    color: var(--text);
    line-height: 1.7;
    opacity: 0;
    transition: opacity 0.1s;
    z-index: 999;
    max-width: 280px;
    box-shadow: 0 8px 32px rgba(0,0,0,0.5);
  }
  .tooltip-layer {
    color: var(--muted);
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin-bottom: 2px;
  }
  .tooltip-name { color: var(--accent); font-weight: 500; font-size: 12px; }
  .tooltip-id   { color: var(--muted); font-size: 10px; margin-top: 2px; }

  /* Scrollbar */
  ::-webkit-scrollbar { width: 5px; }
  ::-webkit-scrollbar-track { background: transparent; }
  ::-webkit-scrollbar-thumb { background: var(--border-hi); border-radius: 10px; }

  /* Loading spinner */
  @keyframes spin { to { transform: rotate(360deg); } }
  .spinner {
    width: 14px; height: 14px;
    border: 2px solid var(--border);
    border-top-color: var(--accent);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
    display: inline-block;
  }
`;

/* ─── Dependency Graph ────────────────────────────────── */
function DependencyGraph({ graph }) {
  const svgRef = useRef(null);
  const tooltipRef = useRef(null);
  const wrapRef = useRef(null);

  useEffect(() => {
    if (!graph || !svgRef.current) return;

    const svg = d3.select(svgRef.current);
    const tooltip = d3.select(tooltipRef.current);
    const wrapper = wrapRef.current;
    const width  = wrapper?.clientWidth  || 800;
    const height = wrapper?.clientHeight || 560;

    svg.selectAll("*").remove();
    svg.attr("viewBox", `0 0 ${width} ${height}`);

    const nodes = graph.nodes.map(n => ({ ...n }));
    const links = graph.edges
      .filter(e => ["METHOD_CALL", "FIELD_INJECTION", "CONSTRUCTOR_INJECTION"].includes(e.type))
      .map(e => ({ ...e }));

    const simulation = d3.forceSimulation(nodes)
      .force("link", d3.forceLink(links).id(n => n.id).distance(120))
      .force("charge", d3.forceManyBody().strength(-350))
      .force("center", d3.forceCenter(width / 2, height / 2))
      .force("collision", d3.forceCollide().radius(n => n.layer === "CONTROLLER" ? 20 : 16));

    const root = svg.append("g");

    svg.call(d3.zoom().scaleExtent([0.3, 4]).on("zoom", e => {
      root.attr("transform", e.transform);
    }));

    // Arrow marker for directed edges
    svg.append("defs").append("marker")
      .attr("id", "arrow")
      .attr("viewBox", "0 -4 8 8")
      .attr("refX", 20).attr("refY", 0)
      .attr("markerWidth", 5).attr("markerHeight", 5)
      .attr("orient", "auto")
      .append("path")
      .attr("d", "M0,-4L8,0L0,4")
      .attr("fill", "rgba(100,116,139,0.5)");

    const linkSel = root.append("g").selectAll("line").data(links).join("line")
      .attr("stroke", e => e.isViolation ? "#ef4444" : "rgba(100,116,139,0.25)")
      .attr("stroke-width", e => e.isViolation ? 2.5 : 1.5)
      .attr("marker-end", "url(#arrow)");

    const nodeGroup = root.append("g").selectAll("g").data(nodes).join("g")
      .call(d3.drag()
        .on("start", (event, n) => { if (!event.active) simulation.alphaTarget(0.3).restart(); n.fx = n.x; n.fy = n.y; })
        .on("drag",  (event, n) => { n.fx = event.x; n.fy = event.y; })
        .on("end",   (event, n) => { if (!event.active) simulation.alphaTarget(0); n.fx = null; n.fy = null; })
      );

    // Glow rings for cycle nodes
    nodeGroup.filter(n => n.inCycle)
      .append("circle")
      .attr("r", n => n.layer === "CONTROLLER" ? 20 : 17)
      .attr("fill", "none")
      .attr("stroke", "#ef4444")
      .attr("stroke-width", 1.5)
      .attr("opacity", 0.4);

    nodeGroup.append("circle")
      .attr("r", n => n.layer === "CONTROLLER" ? 13 : 10)
      .attr("fill", n => layerColors[n.layer] || layerColors.OTHER)
      .attr("stroke", "#080c10")
      .attr("stroke-width", 2)
      .style("cursor", "pointer")
      .on("mousemove", (event, n) => {
        tooltip
          .style("opacity", 1)
          .style("left", `${event.clientX + 16}px`)
          .style("top",  `${event.clientY - 10}px`)
          .html(`
            <div class="tooltip-layer">${n.layer}</div>
            <div class="tooltip-name">${n.label}</div>
            <div class="tooltip-id">${n.id}</div>
          `);
      })
      .on("mouseleave", () => tooltip.style("opacity", 0));

    nodeGroup.append("text")
      .text(n => n.label)
      .attr("text-anchor", "middle")
      .attr("dy", -18)
      .attr("fill", "#94a3b8")
      .attr("font-size", 10)
      .attr("font-family", "'IBM Plex Mono', monospace")
      .attr("pointer-events", "none");

    simulation.on("tick", () => {
      linkSel
        .attr("x1", e => e.source.x).attr("y1", e => e.source.y)
        .attr("x2", e => e.target.x).attr("y2", e => e.target.y);
      nodeGroup.attr("transform", n => `translate(${n.x},${n.y})`);
    });

    return () => simulation.stop();
  }, [graph]);

  return (
    <div ref={wrapRef} style={{ flex: 1, position: "relative" }}>
      <svg ref={svgRef} className="graph-svg" />
      <div ref={tooltipRef} className="graph-tooltip" />
    </div>
  );
}

/* ─── Stat Card ──────────────────────────────────────── */
function StatCard({ label, value, accent }) {
  return (
    <div className="stat-card" style={{ "--stat-accent": accent }}>
      <span className="stat-val">{value}</span>
      <span className="stat-lbl">{label}</span>
    </div>
  );
}

/* ─── Issue Card ─────────────────────────────────────── */
function IssueCard({ issue }) {
  const sev = issue.severity.toLowerCase();
  return (
    <article className={`issue-card sev-${sev}`}>
      <div className="issue-top">
        <span className="issue-rule">{issue.ruleId}</span>
        <span className={`issue-sev sev-badge-${sev}`}>{issue.severity}</span>
      </div>
      <p className="issue-msg">{issue.message}</p>
      <span className="issue-loc">{issue.location}</span>
    </article>
  );
}

/* ─── App ────────────────────────────────────────────── */
export default function App() {
  const [repoUrl,  setRepoUrl]  = useState(defaultRepoUrl);
  const [analysis, setAnalysis] = useState(null);
  const [health,   setHealth]   = useState("checking");
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState("");
  const [tab,      setTab]      = useState("issues");

  useEffect(() => {
    fetch("/api/health")
      .then(r => r.json())
      .then(d => setHealth(d.status))
      .catch(() => setHealth("offline"));
  }, []);

  async function runAnalysis(e) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const r = await fetch("/api/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ repoUrl }),
      });
      const data = await r.json();
      if (!r.ok) throw new Error(data.message || "Analysis failed.");
      setAnalysis(data);
      setTab("issues");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  const summary  = analysis?.summary;
  const graph    = analysis?.graph;
  const issues   = analysis?.issues ?? [];
  const hasData  = !!analysis;
  const healthCls = health === "ok" ? "health-ok" : health === "offline" ? "health-offline" : "health-checking";

  return (
    <>
      <style>{styles}</style>
      <div className="app">
        {/* ── Top bar ── */}
        <header className="topbar">
          <div className="topbar-brand">
            <div className="brand-icon">⬡</div>
            ArchViz
          </div>
          <span className={`health-badge ${healthCls}`}>
            {health === "checking" ? "—" : health === "ok" ? "● API online" : "✕ offline"}
          </span>
        </header>

        <div className="workspace">
          {/* ── Sidebar ── */}
          <aside className="sidebar">
            <div>
              <p className="sidebar-section-label">Repository</p>
              <form className="repo-form" onSubmit={runAnalysis}>
                <label htmlFor="repoUrl">GitHub URL</label>
                <input
                  id="repoUrl"
                  value={repoUrl}
                  onChange={e => setRepoUrl(e.target.value)}
                  placeholder="https://github.com/owner/repo"
                  spellCheck={false}
                />
                <button className="run-btn" type="submit" disabled={loading}>
                  {loading ? <><span className="spinner" /> &nbsp;Analyzing…</> : "Run Analysis"}
                </button>
              </form>
              {error && <div className="error-box" style={{ marginTop: 10 }}>{error}</div>}
            </div>

            {summary && (
              <div>
                <p className="sidebar-section-label">Metrics</p>
                <div className="stats-grid">
                  <StatCard label="Quality Score"  value={summary.score.toFixed(1)}  accent="#10b981" />
                  <StatCard label="Issues"         value={summary.issues}             accent="#ef4444" />
                  <StatCard label="Java Files"     value={summary.javaFiles}          accent="#3b82f6" />
                  <StatCard label="Cycles"         value={summary.cycles}             accent="#a855f7" />
                  <StatCard label="Controllers"    value={summary.controllers}        accent="#f97316" />
                  <StatCard label="Services"       value={summary.services}           accent="#10b981" />
                </div>
              </div>
            )}
          </aside>

          {/* ── Main content ── */}
          <main className="content">
            {/* Tab bar */}
            <nav className="tab-bar">
              <button
                className={`tab-btn ${tab === "issues" ? "active" : ""}`}
                onClick={() => setTab("issues")}
              >
                Issues
                {hasData && (
                  <span className={`tab-count ${issues.length > 0 ? "has-issues" : ""}`}>
                    {issues.length}
                  </span>
                )}
              </button>
              <button
                className={`tab-btn ${tab === "graph" ? "active" : ""}`}
                onClick={() => setTab("graph")}
              >
                Graph
              </button>
              <span className="tab-spacer" />
              {analysis && (
                <span className="timestamp">{analysis.analyzedAt}</span>
              )}
            </nav>

            {/* Issues tab */}
            <div className={`tab-panel ${tab === "issues" ? "active" : ""}`}>
              {!hasData ? (
                <div className="empty">
                  <div className="empty-icon">◎</div>
                  <span>No analysis yet. Enter a repository URL and run analysis.</span>
                </div>
              ) : (
                <>
                  <div className="meta-strip">
                    <div className="meta-item">
                      <span className="meta-key">Repository</span>
                      <span className="meta-val" style={{ fontSize: 11 }}>{analysis.repoUrl}</span>
                    </div>
                    <div className="meta-item">
                      <span className="meta-key">Nodes</span>
                      <span className="meta-val">{graph.metrics.nodes}</span>
                    </div>
                    <div className="meta-item">
                      <span className="meta-key">Edges</span>
                      <span className="meta-val">{graph.metrics.edges}</span>
                    </div>
                    <div className="meta-item">
                      <span className="meta-key">Coupling</span>
                      <span className="meta-val">{graph.metrics.coupling.toFixed(2)}</span>
                    </div>
                    <div className="meta-item">
                      <span className="meta-key">Graph Score</span>
                      <span className="meta-val">{graph.score.toFixed(1)}</span>
                    </div>
                  </div>

                  {issues.length === 0 ? (
                    <div className="no-issues">✓ No issues found in the current ruleset.</div>
                  ) : (
                    issues.map((issue, i) => (
                      <IssueCard key={`${issue.ruleId}-${i}`} issue={issue} />
                    ))
                  )}
                </>
              )}
            </div>

            {/* Graph tab */}
            <div className={`graph-tab-panel ${tab === "graph" ? "active" : ""}`}>
              {!hasData || !graph ? (
                <div className="empty" style={{ flex: 1 }}>
                  <div className="empty-icon">⬡</div>
                  <span>Graph appears after the first successful analysis.</span>
                </div>
              ) : (
                <>
                  <div className="graph-toolbar">
                    {Object.entries(layerColors)
                      .filter(([l]) => l !== "EXTERNAL")
                      .map(([layer, color]) => (
                        <span key={layer} className="legend-item">
                          <span className="legend-dot" style={{ background: color }} />
                          {layer}
                        </span>
                      ))}
                    <div className="graph-metrics">
                      <div className="gm-item">
                        <span className="gm-val">{graph.metrics.nodes}</span>
                        <span className="gm-key">NODES</span>
                      </div>
                      <div className="gm-item">
                        <span className="gm-val">{graph.metrics.edges}</span>
                        <span className="gm-key">EDGES</span>
                      </div>
                      <div className="gm-item">
                        <span className="gm-val">{graph.metrics.coupling.toFixed(2)}</span>
                        <span className="gm-key">COUPLING</span>
                      </div>
                    </div>
                  </div>
                  <div className="graph-area">
                    <DependencyGraph graph={graph} />
                  </div>
                </>
              )}
            </div>
          </main>
        </div>
      </div>
    </>
  );
}