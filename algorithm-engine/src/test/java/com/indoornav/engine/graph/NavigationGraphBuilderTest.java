package com.indoornav.engine.graph;

import com.indoornav.engine.model.Edge;
import com.indoornav.engine.model.FloorScale;
import com.indoornav.engine.model.Node;
import com.indoornav.engine.model.NodeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationGraphBuilderTest {

    private static Node node(long id) {
        return new Node(id, 1L, id, 0.0, NodeType.CORRIDOR_JUNCTION, true);
    }

    @Test
    void buildsGraphWithKnownNodesAndEdges() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1))
                .addNode(node(2))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, true))
                .addFloorScale(new FloorScale(1L, 0.05))
                .build();

        assertTrue(graph.containsNode(1L));
        assertTrue(graph.containsNode(2L));
        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertEquals(0.05, graph.findFloorScale(1L).orElseThrow().metresPerUnit());
    }

    @Test
    void bidirectionalEdgeAppearsInBothEndpointsAdjacency() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1))
                .addNode(node(2))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, true))
                .build();

        assertEquals(List.of(100L), edgeIdsOf(graph.outgoingEdges(1L)));
        assertEquals(List.of(100L), edgeIdsOf(graph.outgoingEdges(2L)));
    }

    @Test
    void oneWayEdgeOnlyAppearsAtItsFromNode() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1))
                .addNode(node(2))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, false))
                .build();

        assertEquals(List.of(100L), edgeIdsOf(graph.outgoingEdges(1L)));
        assertTrue(graph.outgoingEdges(2L).isEmpty());
    }

    @Test
    void outgoingEdgesAreOrderedByAscendingEdgeId() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1))
                .addNode(node(2))
                .addNode(node(3))
                .addEdge(new Edge(200L, 1L, 3L, "CORRIDOR", 5.0, null, true, true))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, true))
                .build();

        assertEquals(List.of(100L, 200L), edgeIdsOf(graph.outgoingEdges(1L)));
    }

    @Test
    void nodeWithNoEdgesHasEmptyAdjacency() {
        NavigationGraph graph = new NavigationGraphBuilder().addNode(node(1)).build();

        assertTrue(graph.outgoingEdges(1L).isEmpty());
    }

    @Test
    void rejectsDuplicateNodeId() {
        NavigationGraphBuilder builder = new NavigationGraphBuilder().addNode(node(1));

        assertThrows(GraphConstructionException.class, () -> builder.addNode(node(1)));
    }

    @Test
    void rejectsDuplicateEdgeId() {
        NavigationGraphBuilder builder = new NavigationGraphBuilder()
                .addNode(node(1))
                .addNode(node(2))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, true));

        assertThrows(GraphConstructionException.class,
                () -> builder.addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 3.0, null, true, true)));
    }

    @Test
    void rejectsDuplicateFloorScale() {
        NavigationGraphBuilder builder = new NavigationGraphBuilder().addFloorScale(new FloorScale(1L, 0.05));

        assertThrows(GraphConstructionException.class, () -> builder.addFloorScale(new FloorScale(1L, 0.06)));
    }

    @Test
    void rejectsEdgeWithUnknownFromNode() {
        NavigationGraphBuilder builder = new NavigationGraphBuilder()
                .addNode(node(2))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, true));

        assertThrows(GraphConstructionException.class, builder::build);
    }

    @Test
    void rejectsEdgeWithUnknownToNode() {
        NavigationGraphBuilder builder = new NavigationGraphBuilder()
                .addNode(node(1))
                .addEdge(new Edge(100L, 1L, 2L, "CORRIDOR", 5.0, null, true, true));

        assertThrows(GraphConstructionException.class, builder::build);
    }

    private static List<Long> edgeIdsOf(List<Edge> edges) {
        return edges.stream().map(Edge::id).toList();
    }
}
