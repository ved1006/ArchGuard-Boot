
// ─── CONFIG ───────────────────────────────────────────────────────────────────

const COLORS = {
    CONTROLLER: "#3b82f6",   // blue
    SERVICE: "#22c55e",   // green
    REPOSITORY: "#f59e0b",   // orange
    ENTITY: "#ef4444",   // red
    DTO: "#a855f7",   // purple
    EXCEPTION: "#ec4899",   // pink
    OTHER: "#64748b"
};

const NODE_RADIUS = {
    CONTROLLER: 12,
    SERVICE: 10,
    REPOSITORY: 9,
    ENTITY: 8,
    DTO: 6,
    EXCEPTION: 5,
    OTHER: 5
};

// Prefixes for noise suppression
const NOISE_PREFIXES = ["java.lang.", "java.util."];
const PRIMITIVES = new Set(["int", "long", "double", "float", "boolean", "short", "byte", "char", "void", "String", "Integer", "Long", "Double", "Float", "Boolean"]);

// Important structural edge types
const KEPT_EDGE_TYPES = new Set(["FIELD_INJECTION", "CONSTRUCTOR_INJECTION", "METHOD_CALL", "INHERITANCE"]);

// ─── STATE ────────────────────────────────────────────────────────────────────

let rawData = null;
let currentNodes = [];
let currentLinks = [];

// ─── D3 SETUP ─────────────────────────────────────────────────────────────────

const svgEl = document.getElementById("graph-svg");
const svg = d3.select(svgEl);
const gRoot = svg.append("g");

const zoom = d3.zoom()
    .scaleExtent([0.05, 5])
    .on("zoom", e => gRoot.attr("transform", e.transform));
svg.call(zoom);

let simulation = d3.forceSimulation();

// ─── DOM HANDLES ──────────────────────────────────────────────────────────────

const overlay = document.getElementById("loading-overlay");
const emptyState = document.getElementById("empty-state");
const nodeDetailEl = document.getElementById("node-detail");
const tooltipEl = document.getElementById("tooltip");

// ─── EVENT LISTENERS ──────────────────────────────────────────────────────────

document.getElementById("btn-generate").addEventListener("click", generate);
document.getElementById("detail-close").addEventListener("click", () => {
    nodeDetailEl.classList.add("hidden");
    resetHighlight();
});
document.getElementById("toggle-external").addEventListener("change", () => rawData && render());
document.getElementById("toggle-labels").addEventListener("change", () => rawData && render());
svg.on("click", () => { nodeDetailEl.classList.add("hidden"); resetHighlight(); });

// ─── FETCH ────────────────────────────────────────────────────────────────────

async function generate() {
    overlay.classList.remove("hidden");
    emptyState.classList.add("hidden");
    try {
        const res = await fetch("/graph", { method: "POST" });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        rawData = await res.json();
        render();
    } catch (err) {
        console.error("Graph generation failed:", err);
        overlay.classList.add("hidden");
        emptyState.classList.remove("hidden");
    }
}

// ─── FILTER LOGIC ─────────────────────────────────────────────────────────────

function filterData(data, showExternal) {
    const keptLayers = new Set(["CONTROLLER", "SERVICE", "REPOSITORY", "ENTITY", "DTO", "EXCEPTION"]);

    // 1️⃣ FILTERING (CRITICAL — DO THIS FIRST)
    const nodes = data.nodes.filter(n => {
        // REMOVE java.lang.*, java.util.*
        if (NOISE_PREFIXES.some(p => n.id.startsWith(p))) return false;
        // REMOVE primitives
        if (PRIMITIVES.has(n.label)) return false;

        // REMOVE noise: java.util, java.lang, primitives
        if (n.id.includes("java.util") || n.id.includes("java.lang")) return false;

        // Step 1: Specific layer check
        // If it's a known architecture layer, definitely keep it
        if (keptLayers.has(n.layer)) return true;

        // RELAXED: If it's an INTERNAL node (not external), keep it if it's likely part of the app
        if (n.layer === "OTHER" || n.layer === "EXTERNAL") {
            return true;
        }

        return false;
    });

    const nodeIndex = new Map(nodes.map(n => [n.id, n]));

    // 2️⃣ KEEP ONLY IMPORTANT EDGES
    const links = data.edges.filter(e => {
        if (e.isCycleEdge) {
            return nodeIndex.has(e.source) && nodeIndex.has(e.target);
        }

        if (!KEPT_EDGE_TYPES.has(e.type)) return false;
        if (!nodeIndex.has(e.source)) return false;
        if (!nodeIndex.has(e.target)) return false;

        const sourceNode = nodeIndex.get(e.source);
        const targetNode = nodeIndex.get(e.target);

        // 3️⃣ RELAX METHOD CALL FILTER: Keep if both ends are kept layers, OR if it's a flow from Controller/Service
        if (e.type === "METHOD_CALL") {
            const tl = targetNode.layer;
            const sl = sourceNode.layer;
            if (tl === "SERVICE" || tl === "REPOSITORY" || tl === "ENTITY") return true;
            if (sl === "CONTROLLER" || sl === "SERVICE") return true;
            return false;
        }

        return true;
    });

    // Step 4: Final Cleanup - Remove 'OTHER' nodes if they are completely orphaned
    const linkedNodeIds = new Set();
    links.forEach(l => {
        linkedNodeIds.add(typeof l.source === 'object' ? l.source.id : l.source);
        linkedNodeIds.add(typeof l.target === 'object' ? l.target.id : l.target);
    });

    const finalNodes = nodes.filter(n => {
        if (keptLayers.has(n.layer)) return true;
        return linkedNodeIds.has(n.id);
    });

    return { nodes: finalNodes, links };
}

// ─── RENDER ───────────────────────────────────────────────────────────────────

function render() {
    const showExternal = document.getElementById("toggle-external").checked;
    const showLabels = document.getElementById("toggle-labels").checked;

    const { nodes, links } = filterData(rawData, showExternal);
    currentNodes = nodes;
    currentLinks = links;

    // 🔟 DEBUG CHECK (MANDATORY)
    console.log("Nodes:", nodes.length);
    console.log("Edges:", links.length);

    // Update UI Stats
    document.getElementById("stat-internal").innerText = rawData.stats.internalNodes;
    document.getElementById("stat-external").innerText = rawData.stats.externalNodes;
    document.getElementById("stat-edges").innerText = links.length;
    document.getElementById("stats-panel").classList.remove("hidden");
    document.getElementById("legend-panel").classList.remove("hidden");
    document.getElementById("edge-legend-panel").classList.remove("hidden");
    document.getElementById("filter-panel").classList.remove("hidden");

    gRoot.selectAll("*").remove();

    const width = svgEl.clientWidth || window.innerWidth - 320;
    const height = svgEl.clientHeight || window.innerHeight;

    // Arrowheads
    const defs = svg.append("defs");
    defs.append("marker")
        .attr("id", "arrowhead")
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 24) // Adjusted for slightly larger nodes
        .attr("refY", 0)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto")
        .append("path")
        .attr("d", "M0,-5L10,0L0,5")
        .attr("fill", "#cbd5e1"); // Brighter for dark mode visibility

    defs.append("marker")
        .attr("id", "arrowhead-violation")
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 24)
        .attr("refY", 0)
        .attr("markerWidth", 7)
        .attr("markerHeight", 7)
        .attr("orient", "auto")
        .append("path")
        .attr("d", "M0,-5L10,0L0,5")
        .attr("fill", "#ff4444");

    // 8️⃣ EDGE STYLING & 9️⃣ HIGHLIGHT VIOLATIONS
    const linkSel = gRoot.append("g")
        .selectAll("line")
        .data(links)
        .enter().append("line")
        .attr("stroke", e => e.isViolation ? "#ff4444" : "#475569")
        .attr("stroke-width", e => {
            if (e.isViolation) return 4;
            return (e.type === "FIELD_INJECTION") ? 3 : 2;
        })
        .attr("stroke-opacity", 0.9)
        .attr("marker-end", e => e.isViolation ? "url(#arrowhead-violation)" : "url(#arrowhead)");

    // Nodes
    const nodeSel = gRoot.append("g")
        .selectAll("circle")
        .data(nodes)
        .enter().append("circle")
        .attr("r", n => (NODE_RADIUS[n.layer] || 5) * 1.5) // Increased size
        .attr("fill", n => COLORS[n.layer] || COLORS.OTHER)
        .attr("stroke", "#ffffff66")
        .attr("stroke-width", 2)
        .call(makeDrag())
        .on("mouseover", onHover)
        .on("mouseout", onHoverOut)
        .on("click", onNodeClick);

    // Labels
    let labelsNode = null;
    if (showLabels) {
        labelsNode = gRoot.append("g")
            .attr("class", "labels")
            .selectAll("g")
            .data(nodes)
            .enter().append("g");

        // Halo text
        labelsNode.append("text")
            .attr("text-anchor", "middle")
            .attr("dy", n => -((NODE_RADIUS[n.layer] || 5) * 1.5) - 8)
            .attr("stroke", "#0f172a")
            .attr("stroke-width", 4)
            .attr("paint-order", "stroke fill")
            .style("font-size", "12px")
            .style("font-weight", "800")
            .text(n => n.label);

        // Main text
        labelsNode.append("text")
            .attr("text-anchor", "middle")
            .attr("dy", n => -((NODE_RADIUS[n.layer] || 5) * 1.5) - 8)
            .style("font-size", "12px")
            .style("font-weight", "800")
            .style("fill", "#f8fafc")
            .text(n => n.label);
    }

    // 4️⃣ LAYER-BASED LAYOUT & 5️⃣ ADD LINK FORCE
    simulation.stop();
    simulation = d3.forceSimulation(nodes)
        .velocityDecay(0.6) // Stiffen the graph
        .force("link", d3.forceLink(links).id(d => d.id).distance(150)) // More breathing room
        .force("charge", d3.forceManyBody().strength(-600))
        .force("collide", d3.forceCollide(d => (NODE_RADIUS[d.layer] || 5) * 1.5 + 25))
        .force("x", d3.forceX(width / 2).strength(0.12))
        .force("y", d3.forceY(n => {
            switch (n.layer) {
                case "CONTROLLER": return height * 0.15;
                case "SERVICE": return height * 0.45;
                case "REPOSITORY": return height * 0.75;
                case "ENTITY": return height * 0.88;
                case "DTO": return height * 0.92;
                default: return height * 0.95;
            }
        }).strength(0.7))
        .on("tick", () => {
            linkSel
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);

            nodeSel
                .attr("cx", d => d.x)
                .attr("cy", d => d.y);

            if (labelsNode) {
                labelsNode.attr("transform", d => `translate(${d.x},${d.y})`);
            }
        });

    updateInsights(nodes, links);
    overlay.classList.add("hidden");
}

function updateInsights(nodes, links) {
    const panel = document.getElementById("insights-panel");
    const content = document.getElementById("insights-content");
    panel.classList.remove("hidden");

    const violations = links.filter(l => l.isViolation).length;
    const internal = nodes.filter(n => n.layer !== 'EXTERNAL').length;

    let html = "";

    if (violations > 0) {
        html += `<div class="insight-item insight-error">Found ${violations} architectural violations (Controller accessing Repository directly).</div>`;
    } else {
        html += `<div class="insight-item insight-ok">Architecture follows the standard 3-tier layering pattern.</div>`;
    }

    if (nodes.some(n => n.layer === 'SERVICE') && nodes.some(n => n.layer === 'CONTROLLER')) {
        html += `<div class="insight-item insight-ok">Clear separation between UI endpoints and business logic.</div>`;
    }

    const maxDegreeNode = nodes.reduce((max, n) => {
        const degree = links.filter(l => l.source.id === n.id || l.target.id === n.id).length;
        return degree > max.degree ? { name: n.label, degree } : max;
    }, { name: "None", degree: 0 });

    if (maxDegreeNode.degree > 0) {
        html += `<div class="insight-item"><strong>${maxDegreeNode.name}</strong> is the central hub of this architecture.</div>`;
    }

    html += `<div class="insight-item">Conclusion: System is ${violations > 0 ? 'slightly disorganized' : 'well-structured'} with ${internal} core components.</div>`;

    content.innerHTML = html;
}

// ─── INTERACTIONS ─────────────────────────────────────────────────────────────

function onHover(event, d) {
    // Step 5 Hover Info
    const tid = typeof d.id === 'object' ? d.id : d.id;
    const incoming = currentLinks.filter(l => (typeof l.target === 'object' ? l.target.id : l.target) === tid).length;
    const outgoing = currentLinks.filter(l => (typeof l.source === 'object' ? l.source.id : l.source) === tid).length;

    tooltipEl.classList.remove("hidden");
    tooltipEl.innerHTML = `
        <strong>Class: ${d.label}</strong><br>
        Layer: ${d.layer}<br>
        Incoming: ${incoming}<br>
        Outgoing: ${outgoing}
    `;
    tooltipEl.style.left = (event.pageX + 10) + "px";
    tooltipEl.style.top = (event.pageY - 10) + "px";
}

function onHoverOut() { tooltipEl.classList.add("hidden"); }

function onNodeClick(event, d) {
    event.stopPropagation();
    const neighborIds = new Set();
    const neighborNames = [];

    currentLinks.forEach(l => {
        const sid = typeof l.source === 'object' ? l.source.id : l.source;
        const tid = typeof l.target === 'object' ? l.target.id : l.target;

        if (sid === d.id) {
            neighborIds.add(tid);
            const targetNode = currentNodes.find(n => n.id === tid);
            if (targetNode) neighborNames.push(targetNode.label);
        }
        if (tid === d.id) {
            neighborIds.add(sid);
            const sourceNode = currentNodes.find(n => n.id === sid);
            if (sourceNode) neighborNames.push(sourceNode.label);
        }
    });

    gRoot.selectAll("circle")
        .transition().duration(200)
        .style("opacity", n => (n.id === d.id || neighborIds.has(n.id)) ? 1 : 0.1);

    gRoot.selectAll("line")
        .transition().duration(200)
        .style("opacity", l => {
            const sid = typeof l.source === 'object' ? l.source.id : l.source;
            const tid = typeof l.target === 'object' ? l.target.id : l.target;
            return (sid === d.id || tid === d.id) ? 1 : 0.05;
        });

    const incoming = currentLinks.filter(l => (typeof l.target === 'object' ? l.target.id : l.target) === d.id).length;
    const outgoing = currentLinks.filter(l => (typeof l.source === 'object' ? l.source.id : l.source) === d.id).length;

    nodeDetailEl.classList.remove("hidden");
    document.getElementById("detail-name").innerText = d.label;
    document.getElementById("detail-fqn").innerText = d.id;
    document.getElementById("detail-layer").innerText = d.layer;
    document.getElementById("detail-in").innerText = incoming;
    document.getElementById("detail-out").innerText = outgoing;
}

function resetHighlight() {
    gRoot.selectAll("circle").transition().duration(200).style("opacity", 1);
    gRoot.selectAll("line").transition().duration(200).style("opacity", 0.8);
}

function makeDrag() {
    return d3.drag()
        .on("start", (event, d) => {
            if (!event.active) simulation.alphaTarget(0.3).restart();
            d.fx = d.x; d.fy = d.y;
        })
        .on("drag", (event, d) => { d.fx = event.x; d.fy = event.y; })
        .on("end", (event, d) => {
            if (!event.active) simulation.alphaTarget(0);
            d.fx = null; d.fy = null;
        });
}

window.addEventListener("resize", () => { if (rawData) render(); });
