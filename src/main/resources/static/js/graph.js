async function load() {
    try {
        const r = await fetch('api/graph/build');
        const data = await r.json();

        if (!data || !data.nodes || data.nodes.length === 0) {
            // Show empty state
            const svg = d3.select('#svg');
            const width = document.getElementById('graph-container').clientWidth;
            const height = document.getElementById('graph-container').clientHeight || 600;
            svg.append("text")
                .attr("x", width / 2)
                .attr("y", height / 2)
                .attr("text-anchor", "middle")
                .style("fill", "#64748b")
                .text("暂无数据，请先添加笔记");
            return;
        }

        const container = document.getElementById('graph-container');
        const width = container.clientWidth;
        const height = container.clientHeight || 600;

        const svg = d3.select('#svg')
            .attr('width', width)
            .attr('height', height)
            .call(d3.zoom().on("zoom", (event) => {
                g.attr("transform", event.transform);
            }));

        svg.selectAll("*").remove();

        const g = svg.append("g");

        const simulation = d3.forceSimulation(data.nodes)
            .force('link', d3.forceLink(data.links).id(d => d.id).distance(100))
            .force('charge', d3.forceManyBody().strength(-300))
            .force('center', d3.forceCenter(width / 2, height / 2))
            .force('collide', d3.forceCollide(30));

        const link = g.append('g')
            .selectAll('line')
            .data(data.links)
            .enter().append('line')
            .attr('stroke', '#cbd5e1')
            .attr('stroke-width', 2)
            .attr('stroke-opacity', 0.6);

        const node = g.append('g')
            .selectAll('g')
            .data(data.nodes)
            .enter().append('g')
            .call(drag(simulation))
            .on('click', (event, d) => {
                showSidebar(d);
                highlightNode(d, node, link);
                event.stopPropagation();
            });

        // Reset highlight on background click
        svg.on('click', () => {
            closeSidebar();
            resetHighlight(node, link);
        });

        node.append('circle')
            .attr('r', d => 5 + Math.sqrt(d.score || 1) * 3) // Dynamic radius
            .attr('fill', d => '#6366f1') // Indigo-500
            .attr('stroke', '#fff')
            .attr('stroke-width', 2)
            .attr('class', 'node-circle')
            .style('cursor', 'pointer')
            .style('transition', 'all 0.3s ease');

        node.append('text')
            .text(d => d.name)
            .attr('x', d => 8 + Math.sqrt(d.score || 1) * 3)
            .attr('y', 4)
            .style('font-size', '12px')
            .style('font-weight', '500')
            .style('fill', '#334155') // Slate-700
            .style('pointer-events', 'none')
            .style('text-shadow', '0 1px 2px rgba(255,255,255,0.8)');

        node.append('title')
            .text(d => d.name);

        simulation.on('tick', () => {
            link
                .attr('x1', d => d.source.x)
                .attr('y1', d => d.source.y)
                .attr('x2', d => d.target.x)
                .attr('y2', d => d.target.y);

            node
                .attr('transform', d => `translate(${d.x},${d.y})`);
        });

    } catch (e) {
        console.error("Graph load failed", e);
    }
}

function highlightNode(selectedNode, nodes, links) {
    // Find neighbors
    const neighbors = new Set();
    neighbors.add(selectedNode.id);
    links.each(function (d) {
        if (d.source.id === selectedNode.id) neighbors.add(d.target.id);
        if (d.target.id === selectedNode.id) neighbors.add(d.source.id);
    });

    // Dim all
    nodes.style('opacity', 0.2);
    links.style('opacity', 0.1);

    // Highlight selected and neighbors
    nodes.filter(d => neighbors.has(d.id))
        .style('opacity', 1)
        .select('circle')
        .attr('fill', d => d.id === selectedNode.id ? '#ef4444' : '#6366f1')
        .attr('r', d => (5 + Math.sqrt(d.score || 1) * 3) * (d.id === selectedNode.id ? 1.2 : 1));

    links.filter(d => d.source.id === selectedNode.id || d.target.id === selectedNode.id)
        .style('opacity', 0.8)
        .attr('stroke', '#6366f1')
        .attr('stroke-width', 3);
}

function resetHighlight(nodes, links) {
    nodes.style('opacity', 1)
        .select('circle')
        .attr('fill', '#6366f1')
        .attr('r', d => 5 + Math.sqrt(d.score || 1) * 3);

    links.style('opacity', 0.6)
        .attr('stroke', '#cbd5e1')
        .attr('stroke-width', 2);
}

async function showSidebar(node) {
    const sidebar = document.getElementById('sidebar');
    const title = document.getElementById('sidebar-title');
    const content = document.getElementById('sidebar-content');

    sidebar.classList.add('active');
    title.innerText = `"${node.name}" 相关笔记`;
    content.innerHTML = '<div class="text-center mt-5"><div class="spinner-border text-primary"></div></div>';

    try {
        const response = await fetch(`api/notes/search?q=${encodeURIComponent(node.name)}`);
        const notes = await response.json();

        if (notes.length === 0) {
            content.innerHTML = '<div class="empty-state"><i class="fas fa-search mb-3" style="font-size: 2rem;"></i><p>未找到相关笔记</p></div>';
            return;
        }

        content.innerHTML = notes.map(n => `
            <div class="note-item" onclick="location.href='notes'">
                <div class="note-title">${n.title || '无标题'}</div>
                <div class="note-preview">${n.text ? n.text.substring(0, 100) + '...' : '暂无内容'}</div>
                <div class="text-muted small mt-2">
                    <i class="far fa-clock me-1"></i>${new Date(n.createdAt).toLocaleDateString()}
                </div>
            </div>
        `).join('');

    } catch (e) {
        content.innerHTML = '<div class="text-center text-danger mt-5">加载失败</div>';
    }
}

function closeSidebar() {
    document.getElementById('sidebar').classList.remove('active');
}


function drag(sim) {
    function dragstarted(event) {
        if (!event.active) sim.alphaTarget(0.3).restart();
        event.subject.fx = event.subject.x;
        event.subject.fy = event.subject.y;
    }

    function dragged(event) {
        event.subject.fx = event.x;
        event.subject.fy = event.y;
    }

    function dragended(event) {
        if (!event.active) sim.alphaTarget(0);
        event.subject.fx = null;
        event.subject.fy = null;
    }

    return d3.drag()
        .on('start', dragstarted)
        .on('drag', dragged)
        .on('end', dragended);
}

// Initial load
load();

// Handle resize
window.addEventListener('resize', () => {
    // Ideally we re-run simulation or update center force
    // For now just reload to keep it simple or we can update center
    // load(); 
});
