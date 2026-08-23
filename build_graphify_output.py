import os
import sys
import time
import json
from pathlib import Path
from collections import Counter
import networkx as nx

# Graphify modules
from graphify.build import build_from_json
from graphify.cluster import cluster, score_all
from graphify.analyze import god_nodes, surprising_connections, suggest_questions
from graphify.report import generate
from graphify.export import to_json, to_html

def main():
    root = Path('D:/MAPA').resolve()
    out_dir = root / 'GRAPHIFY'
    out_dir.mkdir(parents=True, exist_ok=True)
    
    print("=" * 60)
    print("Finalizando Graphify para D:\\MAPA")
    print("=" * 60)

    extract_path = out_dir / 'graph_extract.json'
    if extract_path.exists():
        print("1. Cargando extracción AST previamente guardada...")
        extraction = json.loads(extract_path.read_text(encoding='utf-8'))
    else:
        from graphify.extract import extract
        CODE_EXTS = {'.kt', '.java', '.cpp', '.h', '.hpp', '.c', '.cc', '.py', '.gradle', '.kts', '.xml', '.json', '.md', '.txt', '.sh', '.bat', '.ps1'}
        EXCLUDE_DIRS = {'.git', '.gradle', '.kotlin', '.idea', '.vscode', 'build', 'GRAPHIFY', 'graphify-out', 'BACKUP_OSMAND_MOTOR_20260822_193904', 'BACKUP_TXMAPS_STABLE_', 'BACKUP_TXMAPS_STABLE_20260822_193745', 'resources'}
        files_to_scan = []
        for dp, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS and not d.startswith('.')]
            for f in filenames:
                p = Path(dp) / f
                if p.suffix.lower() in CODE_EXTS and not f.startswith('logcat') and not f.endswith('.hprof') and not f.endswith('.log'):
                    files_to_scan.append(p)
        print(f"1. Extrayendo {len(files_to_scan)} archivos...")
        extraction = extract(files_to_scan, cache_root=root)
        extract_path.write_text(json.dumps(extraction, indent=2, ensure_ascii=False), encoding='utf-8')

    # 2. Construcción del Grafo
    print("2. Construyendo grafo de conocimiento (NetworkX)...")
    t0 = time.time()
    G = build_from_json(extraction, root=str(root), directed=False)
    print(f"   -> Grafo construido con {G.number_of_nodes()} nodos y {G.number_of_edges()} aristas en {time.time()-t0:.2f}s.")

    # 3. Detección de Comunidades / Clústeres
    print("3. Detectando comunidades y calculando cohesión...")
    communities = cluster(G)
    cohesion = score_all(G, communities)
    print(f"   -> Detectadas {len(communities)} comunidades arquitectónicas.")

    # 4. Análisis de Nodos Clave (God Nodes) y Conexiones Sorprendentes
    print("4. Analizando God Nodes y conexiones transversales...")
    gods = god_nodes(G, top_n=30)
    surprises = surprising_connections(G, communities, top_n=20)

    # 5. Generación de Etiquetas para Comunidades
    labels = {}
    for cid, node_ids in communities.items():
        tokens = []
        for nid in node_ids:
            data = G.nodes.get(nid, {})
            label = data.get('label', '')
            src = str(data.get('source_file', ''))
            if 'Team-Nacional-TX-Aragua' in src:
                tokens.append('TeamTX')
            elif 'txmaps' in src:
                tokens.append('TXMaps')
            elif 'OsmAnd' in src:
                tokens.append('OsmAnd')
            elif 'core' in src:
                tokens.append('CoreNative')
            
            words = [w for w in label.replace('_', ' ').replace('/', ' ').split() if len(w) > 2]
            tokens.extend(words[:2])
            
        common = Counter(tokens).most_common(2)
        if common:
            label_str = " / ".join([w[0] for w in common])
            labels[cid] = f"C{cid}: {label_str}"
        else:
            labels[cid] = f"Comunidad {cid}"

    questions = suggest_questions(G, communities, labels)

    # 6. Exportar graph.json
    print("5. Exportando graph.json...")
    to_json(G, communities, str(out_dir / 'graph.json'))

    # 7. Generar GRAPH_REPORT.md
    print("6. Generando GRAPH_REPORT.md...")
    detection_dummy = {
        'total_files': len(extraction.get('nodes', [])),
        'total_words': 1000000,
        'files': {'code': []}
    }
    tokens_dummy = {'input': 0, 'output': 0}
    report = generate(
        G, communities, cohesion, labels, gods, surprises,
        detection_dummy, tokens_dummy, str(root), suggested_questions=questions
    )
    (out_dir / 'GRAPH_REPORT.md').write_text(report, encoding='utf-8')

    # 8. Exportar HTML Interactivo Global (vis.js con node_limit=5000 para agregación automática de macro-comunidades)
    print("7. Generando visualizador interactivo HTML global (graph.html)...")
    to_html(G, communities, str(out_dir / 'graph.html'), community_labels=labels, node_limit=5000)

    # 9. Generar Sub-Grafo Detallado de Team-TX & TXMaps (nodo a nodo)
    print("8. Generando sub-grafo detallado de Team-TX & TXMaps (graph_team_tx.html)...")
    try:
        tx_nodes = [
            nid for nid, data in G.nodes(data=True)
            if 'Team-Nacional-TX-Aragua' in str(data.get('source_file', '')) or 'txmaps' in str(data.get('source_file', ''))
        ]
        if tx_nodes:
            subG = G.subgraph(tx_nodes).copy()
            sub_comm = cluster(subG)
            sub_labels = {cid: labels.get(cid, f"C{cid}") for cid in sub_comm}
            to_html(subG, sub_comm, str(out_dir / 'graph_team_tx.html'), community_labels=sub_labels, node_limit=5000)
            print(f"   -> graph_team_tx.html generado con {subG.number_of_nodes()} nodos detallados.")
    except Exception as e:
        print("   -> Sub-grafo Team-TX skipped:", e)

    # 10. Generar Sub-Grafo de Architectural Hubs (Top Conexiones y Vecinos)
    print("9. Generando sub-grafo de Hubs Arquitectónicos (graph_hubs.html)...")
    try:
        hub_node_ids = set([g.get('id') for g in gods if g.get('id') in G])
        neighbor_nodes = set(hub_node_ids)
        for hid in hub_node_ids:
            neighbor_nodes.update(G.neighbors(hid))
        
        # Limitar a máx 3000 nodos para visualización fluida
        selected_nodes = list(neighbor_nodes)[:3000]
        hubG = G.subgraph(selected_nodes).copy()
        hub_comm = cluster(hubG)
        hub_labels = {cid: labels.get(cid, f"C{cid}") for cid in hub_comm}
        to_html(hubG, hub_comm, str(out_dir / 'graph_hubs.html'), community_labels=hub_labels, node_limit=5000)
        print(f"   -> graph_hubs.html generado con {hubG.number_of_nodes()} nodos clave.")
    except Exception as e:
        print("   -> Sub-grafo Hubs skipped:", e)

    # 11. Generar Dashboard index.html Premium
    print("10. Creando portal index.html interactivo...")
    create_portal_index(out_dir, G, communities, labels, gods, surprises, report)

    print("=" * 60)
    print("¡PROCESO COMPLETADO EXITOSAMENTE!")
    print(f"Archivos listos en: {out_dir}")
    print("  - index.html            (Panel de control y visor central)")
    print("  - graph.html            (Grafo interactivo de macro-comunidades)")
    print("  - graph_team_tx.html    (Grafo detallado de Team-TX & TXMaps)")
    print("  - graph_hubs.html       (Grafo de nodos centrales y dependencias)")
    print("  - GRAPH_REPORT.md       (Reporte técnico completo)")
    print("  - graph.json            (Base de datos completa del grafo)")
    print("=" * 60)


def create_portal_index(out_dir, G, communities, labels, gods, surprises, report_md):
    gods_html = "".join([
        f"<li class='god-item'><span class='god-name'>{g.get('label', g.get('id', ''))}</span> "
        f"<span class='badge'>{g.get('degree', 0)} conexiones</span> "
        f"<small class='god-src'>{g.get('source_file', '')}</small></li>"
        for g in gods[:20]
    ])

    comm_html = "".join([
        f"<div class='comm-card'><strong>{labels.get(cid, f'C{cid}')}</strong> "
        f"<span class='comm-count'>{len(members):,} nodos</span></div>"
        for cid, members in sorted(communities.items(), key=lambda x: len(x[1]), reverse=True)[:18]
    ])

    surprises_html = "".join([
        f"<li class='surprise-item'><code>{s.get('source', '')}</code> &harr; <code>{s.get('target', '')}</code> "
        f"<span class='badge-purple'>{s.get('relation', 'cross-module')}</span></li>"
        for s in surprises[:12]
    ])

    html = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Graphify - MapaBase & Team TX Aragua</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>
:root {{
    --bg: #090d16;
    --card: #111827;
    --card-hover: #1f293d;
    --accent: #3b82f6;
    --accent-glow: rgba(59, 130, 246, 0.35);
    --cyan: #06b6d4;
    --purple: #a855f7;
    --text: #f8fafc;
    --text-muted: #94a3b8;
    --border: #1e293b;
}}
* {{ margin: 0; padding: 0; box-sizing: border-box; }}
body {{
    font-family: 'Outfit', sans-serif;
    background-color: var(--bg);
    color: var(--text);
    line-height: 1.6;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}}
header {{
    background: linear-gradient(135deg, #0b1120 0%, #1e1b4b 100%);
    padding: 2.2rem 2rem;
    border-bottom: 1px solid var(--border);
    box-shadow: 0 10px 30px rgba(0,0,0,0.6);
}}
.header-container {{
    max-width: 1400px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 1.5rem;
}}
.title-group h1 {{
    font-size: 2.2rem;
    font-weight: 700;
    background: linear-gradient(90deg, #60a5fa, #38bdf8, #c084fc);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: -0.02em;
}}
.title-group p {{
    color: var(--text-muted);
    font-size: 1rem;
    margin-top: 0.3rem;
}}
.action-buttons {{
    display: flex;
    gap: 0.8rem;
    flex-wrap: wrap;
}}
.btn {{
    padding: 0.75rem 1.4rem;
    border-radius: 10px;
    font-weight: 600;
    text-decoration: none;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.9rem;
    cursor: pointer;
}}
.btn-primary {{
    background: linear-gradient(135deg, #2563eb, #0284c7);
    color: white;
    box-shadow: 0 4px 15px var(--accent-glow);
}}
.btn-primary:hover {{
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(59, 130, 246, 0.6);
}}
.btn-secondary {{
    background: var(--card);
    color: var(--text);
    border: 1px solid var(--border);
}}
.btn-secondary:hover {{
    background: var(--card-hover);
    border-color: var(--cyan);
}}
.container {{
    max-width: 1400px;
    margin: 2rem auto;
    padding: 0 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 2rem;
    flex: 1;
    width: 100%;
}}
.metrics-row {{
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1.2rem;
}}
.metric-box {{
    background: var(--card);
    border: 1px solid var(--border);
    padding: 1.5rem;
    border-radius: 14px;
    position: relative;
    overflow: hidden;
}}
.metric-box::before {{
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; height: 3px;
    background: linear-gradient(90deg, var(--accent), var(--cyan));
}}
.metric-val {{
    font-size: 2.2rem;
    font-weight: 700;
    color: #38bdf8;
    font-family: 'JetBrains Mono', monospace;
}}
.metric-lbl {{
    font-size: 0.85rem;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
}}
.grid-3col {{
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 1.5rem;
}}
@media (max-width: 1100px) {{
    .grid-3col {{ grid-template-columns: 1fr; }}
}}
.panel {{
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
}}
.panel-title {{
    font-size: 1.2rem;
    font-weight: 600;
    margin-bottom: 1rem;
    color: #f8fafc;
    border-bottom: 1px solid var(--border);
    padding-bottom: 0.6rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
}}
.god-list, .surprise-list {{
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 0.7rem;
    max-height: 440px;
    overflow-y: auto;
    padding-right: 0.5rem;
}}
.god-item, .surprise-item {{
    background: rgba(15, 23, 42, 0.5);
    border: 1px solid var(--border);
    padding: 0.75rem 0.9rem;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
}}
.god-name {{
    font-weight: 600;
    color: #e2e8f0;
    font-family: 'JetBrains Mono', monospace;
    font-size: 0.9rem;
    word-break: break-all;
}}
.badge {{
    align-self: flex-start;
    background: rgba(59, 130, 246, 0.2);
    color: #60a5fa;
    padding: 0.15rem 0.5rem;
    border-radius: 6px;
    font-size: 0.75rem;
    font-weight: 600;
}}
.badge-purple {{
    align-self: flex-start;
    background: rgba(168, 85, 247, 0.2);
    color: #c084fc;
    padding: 0.15rem 0.5rem;
    border-radius: 6px;
    font-size: 0.75rem;
    font-weight: 600;
}}
.god-src {{
    color: var(--text-muted);
    font-size: 0.75rem;
    word-break: break-all;
}}
.comm-grid {{
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.6rem;
    max-height: 440px;
    overflow-y: auto;
    padding-right: 0.5rem;
}}
.comm-card {{
    background: rgba(15, 23, 42, 0.5);
    border: 1px solid var(--border);
    padding: 0.8rem;
    border-radius: 8px;
    font-size: 0.85rem;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}}
.comm-count {{
    color: var(--cyan);
    font-size: 0.78rem;
    font-weight: 600;
    margin-top: 0.3rem;
}}
.viewer-section {{
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 1.5rem;
}}
.viewer-tabs {{
    display: flex;
    gap: 0.5rem;
    margin-bottom: 1rem;
}}
.tab-btn {{
    padding: 0.6rem 1.2rem;
    background: rgba(15, 23, 42, 0.6);
    border: 1px solid var(--border);
    color: var(--text-muted);
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    font-family: inherit;
    font-size: 0.9rem;
    transition: all 0.2s ease;
}}
.tab-btn.active {{
    background: #2563eb;
    color: white;
    border-color: #2563eb;
}}
.iframe-container {{
    width: 100%;
    height: 720px;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid var(--border);
}}
iframe {{
    width: 100%;
    height: 100%;
    border: none;
}}
footer {{
    text-align: center;
    padding: 2rem;
    color: var(--text-muted);
    font-size: 0.85rem;
    border-top: 1px solid var(--border);
    margin-top: 3rem;
}}
</style>
</head>
<body>

<header>
  <div class="header-container">
    <div class="title-group">
      <h1>MapaBase Knowledge Graph</h1>
      <p>Mapeo relacional de archivos, clases, m&eacute;todos y ramas (D:\MAPA)</p>
    </div>
    <div class="action-buttons">
      <a href="graph_team_tx.html" target="_blank" class="btn btn-primary">
        &#128065; Ver Team-TX Detallado
      </a>
      <a href="graph_hubs.html" target="_blank" class="btn btn-secondary">
        &#127760; Ver Nodos Hubs
      </a>
      <a href="graph.html" target="_blank" class="btn btn-secondary">
        &#129513; Ver Macro-Comunidades
      </a>
      <a href="GRAPH_REPORT.md" target="_blank" class="btn btn-secondary">
        &#128196; Reporte MD
      </a>
    </div>
  </div>
</header>

<main class="container">
  <div class="metrics-row">
    <div class="metric-box">
      <div class="metric-val">{G.number_of_nodes():,}</div>
      <div class="metric-lbl">Total Nodos Analizados</div>
    </div>
    <div class="metric-box">
      <div class="metric-val">{G.number_of_edges():,}</div>
      <div class="metric-lbl">Relaciones & Conexiones</div>
    </div>
    <div class="metric-box">
      <div class="metric-val">{len(communities):,}</div>
      <div class="metric-lbl">Comunidades Detectadas</div>
    </div>
    <div class="metric-box">
      <div class="metric-val">{len(gods)}</div>
      <div class="metric-lbl">Hubs Arquitect&oacute;nicos</div>
    </div>
  </div>

  <div class="grid-3col">
    <div class="panel">
      <div class="panel-title">
        <span>&#128392; Nodos Centrales (Hubs)</span>
        <small style="color:var(--text-muted);font-size:0.75rem">M&aacute;s conexiones</small>
      </div>
      <ul class="god-list">
        {gods_html}
      </ul>
    </div>

    <div class="panel">
      <div class="panel-title">
        <span>&#129513; Comunidades Principales</span>
        <small style="color:var(--text-muted);font-size:0.75rem">M&oacute;dulos agrupados</small>
      </div>
      <div class="comm-grid">
        {comm_html}
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">
        <span>&#128260; Conexiones Clave</span>
        <small style="color:var(--text-muted);font-size:0.75rem">Enlaces modulares</small>
      </div>
      <ul class="surprise-list">
        {surprises_html}
      </ul>
    </div>
  </div>

  <section class="viewer-section">
    <div class="panel-title">
      <span>&#128065; Visualizador de Grafos Interactivo</span>
      <div class="viewer-tabs">
        <button class="tab-btn active" onclick="loadGraph('graph_team_tx.html', this)">Team-TX Detallado</button>
        <button class="tab-btn" onclick="loadGraph('graph_hubs.html', this)">Hubs Centrales</button>
        <button class="tab-btn" onclick="loadGraph('graph.html', this)">Macro-Comunidades</button>
      </div>
    </div>
    <div class="iframe-container">
      <iframe id="graph-frame" src="graph_team_tx.html"></iframe>
    </div>
  </section>
</main>

<footer>
  <p>Generado con Graphify para el proyecto <code>D:\MAPA</code> &bull; Carpeta de salida: <code>D:\MAPA\GRAPHIFY</code></p>
</footer>

<script>
function loadGraph(url, btn) {{
  document.getElementById('graph-frame').src = url;
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
}}
</script>

</body>
</html>
"""
    (out_dir / 'index.html').write_text(html, encoding='utf-8')

if __name__ == '__main__':
    main()
