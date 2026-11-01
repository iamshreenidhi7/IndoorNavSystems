package com.indoornav.engine.algorithm;

import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.graph.NavigationGraphBuilder;
import com.indoornav.engine.model.Edge;
import com.indoornav.engine.model.Node;
import com.indoornav.engine.model.NodeType;
import com.indoornav.engine.result.RouteResult;
import com.indoornav.engine.result.RouteStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the Phase 3 engine test obligations listed in IMPLEMENTATION_PLAN.md
 * ("direct path, alternatives, disconnected graph, start equals destination,
 * unknown nodes, blocked edge and node, one-way edge, determinism") and
 * ARCHITECTURE.md Section 5.6.
 */
class DijkstraRoutingAlgorithmTest {

    private static final double DISTANCE_TOLERANCE = 1e-9;

    private final DijkstraRoutingAlgorithm algorithm = new DijkstraRoutingAlgorithm();

    private static Node node(long id) {
        return new Node(id, 1L, id, 0.0, NodeType.CORRIDOR_JUNCTION, true);
    }

    private static Edge edge(long id, long fromNodeId, long toNodeId, double distanceMeters, boolean bidirectional) {
        return new Edge(id, fromNodeId, toNodeId, "CORRIDOR", distanceMeters, null, true, bidirectional);
    }

    private static RoutingRequest request(long start, long destination) {
        return new RoutingRequest(start, destination, Set.of(), Set.of());
    }

    @Test
    void findsDirectPath() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1))
                .addNode(node(2))
                .addEdge(edge(100, 1, 2, 5.0, true))
                .build();

        RouteResult result = algorithm.findRoute(graph, request(1, 2));

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L, 2L), result.nodeIds());
        assertEquals(List.of(100L), result.edgeIds());
        assertEquals(5.0, result.distanceMeters(), DISTANCE_TOLERANCE);
    }

    @Test
    void choosesTheShorterOfTwoAlternativePaths() {
        // 1 -2 (dist 6) -> 4: total 10. 1 -3 (dist 4) -> 4 (dist 3): total 7, shorter.
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2)).addNode(node(3)).addNode(node(4))
                .addEdge(edge(10, 1, 2, 6.0, true))
                .addEdge(edge(20, 2, 4, 4.0, true))
                .addEdge(edge(30, 1, 3, 4.0, true))
                .addEdge(edge(40, 3, 4, 3.0, true))
                .build();

        RouteResult result = algorithm.findRoute(graph, request(1, 4));

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L, 3L, 4L), result.nodeIds());
        assertEquals(List.of(30L, 40L), result.edgeIds());
        assertEquals(7.0, result.distanceMeters(), DISTANCE_TOLERANCE);
    }

    @Test
    void disconnectedGraphReturnsNoRoute() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2)).addNode(node(3))
                .addEdge(edge(100, 1, 2, 5.0, true))
                .build(); // node 3 has no edges

        RouteResult result = algorithm.findRoute(graph, request(1, 3));

        assertEquals(RouteStatus.NO_ROUTE, result.status());
        assertEquals(List.of(), result.nodeIds());
        assertEquals(List.of(), result.edgeIds());
        assertEquals(0.0, result.distanceMeters());
    }

    @Test
    void startEqualToDestinationReturnsZeroDistanceRoute() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2))
                .addEdge(edge(100, 1, 2, 5.0, true))
                .build();

        RouteResult result = algorithm.findRoute(graph, request(1, 1));

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L), result.nodeIds());
        assertEquals(List.of(), result.edgeIds());
        assertEquals(0.0, result.distanceMeters());
    }

    @Test
    void unknownStartNodeThrows() {
        NavigationGraph graph = new NavigationGraphBuilder().addNode(node(1)).build();

        assertThrows(UnknownNodeException.class, () -> algorithm.findRoute(graph, request(999, 1)));
    }

    @Test
    void unknownDestinationNodeThrows() {
        NavigationGraph graph = new NavigationGraphBuilder().addNode(node(1)).build();

        assertThrows(UnknownNodeException.class, () -> algorithm.findRoute(graph, request(1, 999)));
    }

    @Test
    void blockedEdgeForcesTheLongerAlternativeRoute() {
        // Direct 1-2 (dist 5) is shortest; blocking it forces the longer 1-3-2 (dist 6).
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2)).addNode(node(3))
                .addEdge(edge(100, 1, 2, 5.0, true))
                .addEdge(edge(200, 1, 3, 3.0, true))
                .addEdge(edge(300, 3, 2, 3.0, true))
                .build();

        RoutingRequest blockedRequest = new RoutingRequest(1, 2, Set.of(), Set.of(100L));
        RouteResult result = algorithm.findRoute(graph, blockedRequest);

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L, 3L, 2L), result.nodeIds());
        assertEquals(6.0, result.distanceMeters(), DISTANCE_TOLERANCE);
    }

    @Test
    void blockedNodeForcesTheLongerAlternativeRoute() {
        // Via node 3 (dist 4) is shortest; blocking node 3 forces the direct edge (dist 10).
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2)).addNode(node(3))
                .addEdge(edge(100, 1, 3, 2.0, true))
                .addEdge(edge(200, 3, 2, 2.0, true))
                .addEdge(edge(300, 1, 2, 10.0, true))
                .build();

        RoutingRequest blockedRequest = new RoutingRequest(1, 2, Set.of(3L), Set.of());
        RouteResult result = algorithm.findRoute(graph, blockedRequest);

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L, 2L), result.nodeIds());
        assertEquals(List.of(300L), result.edgeIds());
        assertEquals(10.0, result.distanceMeters(), DISTANCE_TOLERANCE);
    }

    @Test
    void blockingTheOnlyConnectionReturnsNoRoute() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2))
                .addEdge(edge(100, 1, 2, 5.0, true))
                .build();

        RoutingRequest blockedRequest = new RoutingRequest(1, 2, Set.of(), Set.of(100L));
        RouteResult result = algorithm.findRoute(graph, blockedRequest);

        assertEquals(RouteStatus.NO_ROUTE, result.status());
    }

    @Test
    void oneWayEdgeCannotBeTraversedAgainstItsDirection() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2))
                .addEdge(edge(100, 1, 2, 5.0, false))
                .build();

        RouteResult forward = algorithm.findRoute(graph, request(1, 2));
        RouteResult backward = algorithm.findRoute(graph, request(2, 1));

        assertEquals(RouteStatus.FOUND, forward.status());
        assertEquals(5.0, forward.distanceMeters(), DISTANCE_TOLERANCE);
        assertEquals(RouteStatus.NO_ROUTE, backward.status());
    }

    @Test
    void tieBetweenEqualCostPathsIsBrokenTowardsTheLowerIntermediateNodeId() {
        // 1-2-4 and 1-3-4 both cost 6; the queue's node-id tie-break must
        // always resolve this the same way.
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2)).addNode(node(3)).addNode(node(4))
                .addEdge(edge(10, 1, 2, 3.0, true))
                .addEdge(edge(20, 2, 4, 3.0, true))
                .addEdge(edge(30, 1, 3, 3.0, true))
                .addEdge(edge(40, 3, 4, 3.0, true))
                .build();

        RouteResult result = algorithm.findRoute(graph, request(1, 4));

        assertEquals(List.of(1L, 2L, 4L), result.nodeIds());
        assertEquals(List.of(10L, 20L), result.edgeIds());
        assertEquals(6.0, result.distanceMeters(), DISTANCE_TOLERANCE);
    }

    @Test
    void repeatedRunsOnTheSameGraphAndRequestProduceTheIdenticalResult() {
        NavigationGraph graph = new NavigationGraphBuilder()
                .addNode(node(1)).addNode(node(2)).addNode(node(3)).addNode(node(4))
                .addEdge(edge(10, 1, 2, 3.0, true))
                .addEdge(edge(20, 2, 4, 3.0, true))
                .addEdge(edge(30, 1, 3, 3.0, true))
                .addEdge(edge(40, 3, 4, 3.0, true))
                .build();
        RoutingRequest tieRequest = request(1, 4);

        RouteResult first = algorithm.findRoute(graph, tieRequest);
        for (int i = 0; i < 10; i++) {
            assertEquals(first, algorithm.findRoute(graph, tieRequest));
        }
    }
}
