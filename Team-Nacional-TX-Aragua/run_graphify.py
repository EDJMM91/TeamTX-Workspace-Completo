import sys, json, os, glob
from pathlib import Path
from multiprocessing import freeze_support

if __name__ == '__main__':
    freeze_support()
    # Run detect
    from graphify.detect import detect
    root = Path('.')
    os.makedirs('graphify-out', exist_ok=True)
    detect_res = detect(root)
    Path('graphify-out/.graphify_detect.json').write_text(json.dumps(detect_res, ensure_ascii=False), encoding="utf-8")

    # Extract code files
    from graphify.extract import collect_files, extract
    code_files = []
    for f in detect_res.get('files', {}).get('code', []):
        code_files.extend(collect_files(Path(f)) if Path(f).is_dir() else [Path(f)])

    ast_result = {'nodes':[],'edges':[],'input_tokens':0,'output_tokens':0}
    if code_files:
        ast_result = extract(code_files, cache_root=root)
    Path('graphify-out/.graphify_ast.json').write_text(json.dumps(ast_result, indent=2, ensure_ascii=False), encoding="utf-8")

    # Fast path semantic
    Path('graphify-out/.graphify_semantic.json').write_text(json.dumps({'nodes':[],'edges':[],'hyperedges':[],'input_tokens':0,'output_tokens':0}), encoding='utf-8')

    # Merge
    ast = ast_result
    sem = {'nodes':[],'edges':[],'hyperedges':[],'input_tokens':0,'output_tokens':0}
    seen = {n['id'] for n in ast['nodes']}
    merged_nodes = list(ast['nodes'])
    for n in sem['nodes']:
        if n['id'] not in seen:
            merged_nodes.append(n)
            seen.add(n['id'])
    merged_edges = ast['edges'] + sem['edges']
    merged_hyperedges = sem.get('hyperedges', [])
    merged = {
        'nodes': merged_nodes,
        'edges': merged_edges,
        'hyperedges': merged_hyperedges,
        'input_tokens': sem.get('input_tokens', 0),
        'output_tokens': sem.get('output_tokens', 0),
    }
    Path('graphify-out/.graphify_extract.json').write_text(json.dumps(merged, indent=2, ensure_ascii=False), encoding="utf-8")

    # Build and report
    from graphify.build import build_from_json
    from graphify.cluster import cluster, score_all
    from graphify.analyze import god_nodes, surprising_connections, suggest_questions
    from graphify.report import generate
    from graphify.export import to_json
    from graphify.diagnostics import diagnose_extraction, format_diagnostic_report

    G = build_from_json(merged, root=str(root.resolve()), directed=False)
    if G.number_of_nodes() == 0:
        print('ERROR: Graph is empty')
        sys.exit(1)
    communities = cluster(G)
    cohesion = score_all(G, communities)
    tokens = {'input': 0, 'output': 0}
    gods = god_nodes(G)
    surprises = surprising_connections(G, communities)
    labels = {cid: 'Community ' + str(cid) for cid in communities}
    questions = suggest_questions(G, communities, labels)

    to_json(G, communities, 'graphify-out/graph.json')
    report = generate(G, communities, cohesion, labels, gods, surprises, detect_res, tokens, str(root.resolve()), suggested_questions=questions)
    Path('graphify-out/GRAPH_REPORT.md').write_text(report, encoding="utf-8")

    print(f'Graph complete! {G.number_of_nodes()} nodes.')
