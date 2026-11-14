package com.indoornav.engine.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.indoornav.engine.cost.ConnectionCost;
import com.indoornav.engine.cost.RoutingCostConfiguration;
import com.indoornav.engine.graph.NavigationGraph;
import com.indoornav.engine.model.ConnectionType;
import com.indoornav.engine.model.NavigationEdge;
import com.indoornav.engine.model.NavigationNode;
import com.indoornav.engine.model.NodeType;
import com.indoornav.engine.model.RoutePreference;
import com.indoornav.engine.model.RouteRequest;
import com.indoornav.engine.result.RouteResult;
import com.indoornav.engine.result.RouteStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RouteFinderTest {
    private static final double TOLERANCE = 1.0e-9;
    private final RouteFinder aStar = new AStarRouteFinder();
    private final RouteFinder dijkstra = new DijkstraRouteFinder();

    @Test
    void findsTheShortestDirectPath() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 3, 0, true)),
                List.of(edge(1, 1, 2, 3, ConnectionType.CORRIDOR, true, true)));

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 2, RoutePreference.SHORTEST), costs());

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L, 2L), result.nodeIds());
        assertEquals(List.of(1L), result.edgeIds());
        assertEquals(3.0, result.totalDistanceMetres(), TOLERANCE);
    }

    @Test
    void returnsNoRouteForDisconnectedNodes() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 3, 0, true)),
                List.of());

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 2, RoutePreference.SHORTEST), costs());

        assertEquals(RouteStatus.NO_ROUTE, result.status());
    }

    @Test
    void returnsAZeroDistanceRouteWhenStartEqualsDestination() {
        NavigationGraph graph = graph(List.of(node(1, 1, 0, 0, true)), List.of());

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 1, RoutePreference.SHORTEST), costs());

        assertEquals(RouteStatus.FOUND, result.status());
        assertEquals(List.of(1L), result.nodeIds());
        assertEquals(0.0, result.totalDistanceMetres(), TOLERANCE);
        assertEquals(0.0, result.totalEstimatedTimeSeconds(), TOLERANCE);
    }

    @Test
    void rejectsUnknownStartNodes() {
        NavigationGraph graph = graph(List.of(node(1, 1, 0, 0, true)), List.of());

        assertThrows(InvalidRouteRequestException.class,
                () -> aStar.findRoute(graph, RouteRequest.of(99, 1, RoutePreference.SHORTEST), costs()));
    }

    @Test
    void excludesBlockedEdgesAndNodes() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 1, 0, true), node(3, 1, 2, 0, true)),
                List.of(edge(1, 1, 2, 1, ConnectionType.CORRIDOR, true, true),
                        edge(2, 2, 3, 1, ConnectionType.CORRIDOR, true, true),
                        edge(3, 1, 3, 4, ConnectionType.CORRIDOR, true, true)));

        RouteRequest edgeBlocked = new RouteRequest(1, 3, RoutePreference.SHORTEST, java.util.Set.of(), java.util.Set.of(1L));
        RouteRequest nodeBlocked = new RouteRequest(1, 3, RoutePreference.SHORTEST, java.util.Set.of(2L), java.util.Set.of());

        assertEquals(List.of(3L), aStar.findRoute(graph, edgeBlocked, costs()).edgeIds());
        assertEquals(List.of(3L), aStar.findRoute(graph, nodeBlocked, costs()).edgeIds());
    }

    @Test
    void selectsDifferentShortestAndFastestRoutes() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 2, 0, true), node(3, 1, 0, 3, true), node(4, 1, 4, 0, true)),
                List.of(edge(1, 1, 2, 2, ConnectionType.STAIRS, false, true),
                        edge(2, 2, 4, 2, ConnectionType.STAIRS, false, true),
                        edge(3, 1, 3, 3, ConnectionType.CORRIDOR, true, true),
                        edge(4, 3, 4, 5, ConnectionType.CORRIDOR, true, true)));

        RouteResult shortest = aStar.findRoute(graph, RouteRequest.of(1, 4, RoutePreference.SHORTEST), costs());
        RouteResult fastest = aStar.findRoute(graph, RouteRequest.of(1, 4, RoutePreference.FASTEST), costs());

        assertEquals(List.of(1L, 2L), shortest.edgeIds());
        assertEquals(List.of(3L, 4L), fastest.edgeIds());
    }

    @Test
    void explicitEdgeTimeOverridesDerivedTimeForFastestRoutesAndMetrics() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 4, 0, true), node(3, 1, 0, 3, true)),
                List.of(edge(1, 1, 2, 4, ConnectionType.CORRIDOR, true, true),
                        edge(2, 1, 3, 3, ConnectionType.CORRIDOR, true, true),
                        edgeWithTime(3, 3, 2, 5, 0.5, ConnectionType.CORRIDOR, true, true)));

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 2, RoutePreference.FASTEST), costs());
        RouteResult dijkstraResult = dijkstra.findRoute(graph, RouteRequest.of(1, 2, RoutePreference.FASTEST), costs());

        assertEquals(List.of(2L, 3L), result.edgeIds());
        assertEquals(8.0, result.totalDistanceMetres(), TOLERANCE);
        assertEquals(3.5, result.totalEstimatedTimeSeconds(), TOLERANCE);
        assertEquals(dijkstraResult.totalEstimatedTimeSeconds(), result.totalEstimatedTimeSeconds(), TOLERANCE);
    }

    @Test
    void accessibleRoutesExcludeInaccessibleConnectionsAndNodes() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 1, 0, false), node(3, 1, 0, 2, true), node(4, 1, 2, 0, true)),
                List.of(edge(1, 1, 2, 1, ConnectionType.STAIRS, false, true),
                        edge(2, 2, 4, 1, ConnectionType.STAIRS, false, true),
                        edge(3, 1, 3, 2, ConnectionType.RAMP, true, true),
                        edge(4, 3, 4, 3, ConnectionType.RAMP, true, true)));

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 4, RoutePreference.ACCESSIBLE), costs());

        assertEquals(List.of(3L, 4L), result.edgeIds());
    }

    @Test
    void avoidStairsUsesStairsOnlyWhenNoAlternativeExists() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 1, 0, true), node(3, 1, 0, 3, true), node(4, 1, 4, 0, true)),
                List.of(edge(1, 1, 2, 1, ConnectionType.STAIRS, false, true),
                        edge(2, 2, 4, 3, ConnectionType.STAIRS, false, true),
                        edge(3, 1, 3, 3, ConnectionType.CORRIDOR, true, true),
                        edge(4, 3, 4, 5, ConnectionType.CORRIDOR, true, true)));

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 4, RoutePreference.AVOID_STAIRS), costs());

        assertEquals(List.of(3L, 4L), result.edgeIds());
        assertEquals(false, result.usesStairs());
    }

    @Test
    void avoidStairsFlagsStairsWhenTheyAreUnavoidable() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 2, 0, true)),
                List.of(edge(1, 1, 2, 2, ConnectionType.STAIRS, false, true)));

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 2, RoutePreference.AVOID_STAIRS), costs());

        assertEquals(List.of(1L), result.edgeIds());
        assertEquals(true, result.usesStairs());
    }

    @Test
    void reportsFloorTransitionsForVerticalConnections() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 2, 0, 0, true)),
                List.of(edge(1, 1, 2, 4, ConnectionType.ELEVATOR, true, true)),
                Map.of(1L, 1.0, 2L, 1.0));

        RouteResult result = aStar.findRoute(graph, RouteRequest.of(1, 2, RoutePreference.FASTEST), costs());

        assertEquals(1, result.floorTransitions().size());
        assertEquals(1L, result.floorTransitions().getFirst().fromFloorId());
        assertEquals(2L, result.floorTransitions().getFirst().toFloorId());
        assertEquals(ConnectionType.ELEVATOR, result.floorTransitions().getFirst().connectionType());
    }

    @Test
    void aStarAndDijkstraReturnEqualCostsForEveryPreference() {
        NavigationGraph graph = graph(
                List.of(node(1, 1, 0, 0, true), node(2, 1, 1, 0, true), node(3, 1, 0, 3, true),
                        node(4, 2, 0, 3, true), node(5, 2, 4, 0, true)),
                List.of(edge(1, 1, 2, 1, ConnectionType.CORRIDOR, true, true),
                        edge(2, 2, 5, 4, ConnectionType.STAIRS, false, true),
                        edge(3, 1, 3, 3, ConnectionType.CORRIDOR, true, true),
                        edge(4, 3, 4, 4, ConnectionType.ELEVATOR, true, true),
                        edge(5, 4, 5, 5, ConnectionType.CORRIDOR, true, true)),
                Map.of(1L, 1.0, 2L, 1.0));

        for (RoutePreference preference : RoutePreference.values()) {
            RouteRequest request = RouteRequest.of(1, 5, preference);
            RouteResult aStarResult = aStar.findRoute(graph, request, costs());
            RouteResult dijkstraResult = dijkstra.findRoute(graph, request, costs());
            assertEquals(dijkstraResult.status(), aStarResult.status());
            assertEquals(
                    routeCost(graph, dijkstraResult, preference),
                    routeCost(graph, aStarResult, preference),
                    TOLERANCE);
        }
    }

    @Test
    void aStarAndDijkstraAgreeAcrossDeterministicGeneratedGraphs() {
        Random random = new Random(42L);

        for (int graphNumber = 0; graphNumber < 100; graphNumber++) {
            NavigationGraph graph = generatedGraph(random);
            for (RoutePreference preference : RoutePreference.values()) {
                RouteRequest request = new RouteRequest(
                        1,
                        12,
                        preference,
                        random.nextBoolean() ? java.util.Set.of() : java.util.Set.of(6L),
                        random.nextBoolean() ? java.util.Set.of() : java.util.Set.of(3L));
                RouteResult aStarResult = aStar.findRoute(graph, request, costs());
                RouteResult dijkstraResult = dijkstra.findRoute(graph, request, costs());

                assertEquals(dijkstraResult.status(), aStarResult.status());
                assertEquals(
                        routeCost(graph, dijkstraResult, preference),
                        routeCost(graph, aStarResult, preference),
                        TOLERANCE);
            }
        }
    }

    @Test
    void equalCostRoutesUseTheSameStablePathRegardlessOfInputEdgeOrder() {
        List<NavigationNode> nodes = List.of(
                node(1, 1, 0, 0, true),
                node(2, 1, 1, 0, true),
                node(3, 1, 0, 1, true),
                node(4, 1, 1, 1, true));
        List<NavigationEdge> edges = List.of(
                edge(20, 1, 2, 1, ConnectionType.CORRIDOR, true, true),
                edge(21, 2, 4, 1, ConnectionType.CORRIDOR, true, true),
                edge(10, 1, 3, 1, ConnectionType.CORRIDOR, true, true),
                edge(11, 3, 4, 1, ConnectionType.CORRIDOR, true, true));
        List<NavigationEdge> reversedEdges = new ArrayList<>(edges);
        Collections.reverse(reversedEdges);
        RouteRequest request = RouteRequest.of(1, 4, RoutePreference.SHORTEST);

        RouteResult firstAStar = aStar.findRoute(graph(nodes, edges), request, costs());
        RouteResult secondAStar = aStar.findRoute(graph(nodes, reversedEdges), request, costs());
        RouteResult firstDijkstra = dijkstra.findRoute(graph(nodes, edges), request, costs());
        RouteResult secondDijkstra = dijkstra.findRoute(graph(nodes, reversedEdges), request, costs());

        assertEquals(List.of(20L, 21L), firstAStar.edgeIds());
        assertEquals(firstAStar.edgeIds(), secondAStar.edgeIds());
        assertEquals(firstDijkstra.edgeIds(), secondDijkstra.edgeIds());
        assertEquals(firstAStar.edgeIds(), firstDijkstra.edgeIds());
    }

    private NavigationGraph graph(List<NavigationNode> nodes, List<NavigationEdge> edges) {
        return graph(nodes, edges, Map.of(1L, 1.0));
    }

    private NavigationGraph graph(List<NavigationNode> nodes, List<NavigationEdge> edges, Map<Long, Double> scales) {
        return new NavigationGraph(nodes, edges, scales);
    }

    private NavigationNode node(long id, long floorId, double x, double y, boolean accessible) {
        return new NavigationNode(id, floorId, x, y, NodeType.CORRIDOR_JUNCTION, accessible);
    }

    private NavigationEdge edge(
            long id, long from, long to, double distance, ConnectionType type, boolean accessible, boolean bidirectional) {
        return edgeWithTime(id, from, to, distance, null, type, accessible, bidirectional);
    }

    private NavigationEdge edgeWithTime(
            long id,
            long from,
            long to,
            double distance,
            Double timeSeconds,
            ConnectionType type,
            boolean accessible,
            boolean bidirectional) {
        return new NavigationEdge(id, from, to, distance, timeSeconds, type, accessible, bidirectional);
    }

    private RoutingCostConfiguration costs() {
        return new RoutingCostConfiguration(
                Map.of(
                        ConnectionType.CORRIDOR, new ConnectionCost(1.0, 0.0),
                        ConnectionType.DOORWAY, new ConnectionCost(1.0, 0.0),
                        ConnectionType.RAMP, new ConnectionCost(1.0, 0.0),
                        ConnectionType.STAIRS, new ConnectionCost(0.2, 0.0),
                        ConnectionType.ELEVATOR, new ConnectionCost(1.0, 1.0)),
                10.0,
                2.0);
    }

    private NavigationGraph generatedGraph(Random random) {
        List<NavigationNode> nodes = new ArrayList<>();
        for (long id = 1; id <= 12; id++) {
            nodes.add(node(id, 1, id - 1, 0, true));
        }
        List<NavigationEdge> edges = new ArrayList<>();
        long edgeId = 1;
        for (long nodeId = 1; nodeId < 12; nodeId++) {
            edges.add(edge(edgeId++, nodeId, nodeId + 1, 1, randomConnectionType(random), true, true));
        }
        for (long fromNodeId = 1; fromNodeId <= 10; fromNodeId++) {
            long toNodeId = fromNodeId + 2 + random.nextInt(2);
            if (toNodeId <= 12) {
                edges.add(edge(
                        edgeId++,
                        fromNodeId,
                        toNodeId,
                        toNodeId - fromNodeId,
                        randomConnectionType(random),
                        random.nextBoolean(),
                        true));
            }
        }
        return graph(nodes, edges);
    }

    private ConnectionType randomConnectionType(Random random) {
        ConnectionType[] types = {
                ConnectionType.CORRIDOR,
                ConnectionType.DOORWAY,
                ConnectionType.RAMP,
                ConnectionType.STAIRS,
                ConnectionType.ELEVATOR
        };
        return types[random.nextInt(types.length)];
    }

    private double routeCost(NavigationGraph graph, RouteResult route, RoutePreference preference) {
        RoutingCostConfiguration configuration = costs();
        return route.edgeIds().stream()
                .map(graph::edge)
                .mapToDouble(edge -> traversalCost(edge, preference, configuration))
                .sum();
    }

    private double traversalCost(
            NavigationEdge edge, RoutePreference preference, RoutingCostConfiguration configuration) {
        if (preference == RoutePreference.SHORTEST) {
            return edge.distanceMetres();
        }
        double time = configuration.travelTimeSeconds(edge);
        if (!edge.connectionType().isStairs()) {
            return time;
        }
        return switch (preference) {
            case AVOID_STAIRS -> time * configuration.avoidStairsMultiplier();
            case PREFER_ELEVATOR -> time * configuration.preferElevatorStairsMultiplier();
            default -> time;
        };
    }
}
