/*
 * Maze User Documentation
 *
 * Press 'm' to enter manual gameplay mode
 *  - (Press 'escape' to exit mode and use arrow keys to move)
 * Press 'd' to toggle to depth first search mode
 * Press 'b' to toggle to breadth first search mode
 * Press 'r' to reset and create new maze
 *
 * Current mode and instructions are indicated in the game
 */


import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.*;
import tester.Tester;

import java.awt.*;
import java.util.Queue;
import java.util.*;

// to represent a vertex in the maze
class Vertex {
  int x;
  int y;

  // constructor
  Vertex(int x, int y) {
    this.x = x;
    this.y = y;
  }

  // draw the vertex
  WorldImage drawVertex(Color cellColor, int cellSize) {
    return new RectangleImage(cellSize - 2, cellSize - 2,
        OutlineMode.SOLID, cellColor);
  }
}


// to represent an edge in the maze
class Edge {
  Vertex source;
  Vertex destination;
  int weight;

  // constructor
  Edge(Vertex v1, Vertex v2, int weight) {
    this.source = v1;
    this.destination = v2;
    this.weight = weight;
  }

  // determine if this edge contains the given vertex
  boolean hasVertex(Vertex v) {
    return this.source.equals(v) || this.destination.equals(v);
  }
}

// represents a Maze game 
class MazeWorld extends World {
  // Maze world dimensions
  int cellSize;
  int width;
  int height;

  // maze world fields
  ArrayList<ArrayList<Vertex>> vertices;
  HashMap<Vertex, Vertex> representatives;
  ArrayList<Edge> edges;

  // random number generator
  Random rand;

  // depth first search fields
  HashMap<Vertex, Edge> cameFromEdgeDfs;
  ArrayList<Vertex> dfsPath;
  ArrayList<Vertex> dfsVisited;
  Stack<Vertex> dfsWorklist;
  boolean animatingDfs;
  boolean reconstructDfs;
  boolean finalStateDfs;
  boolean solvingDfs;

  // breadth first search fields
  HashMap<Vertex, Edge> cameFromEdgeBfs;
  ArrayList<Vertex> bfsPath;
  ArrayList<Vertex> bfsVisited;
  Queue<Vertex> bfsWorklist;
  boolean animatingBfs;
  boolean reconstructBfs;
  boolean finalStateBfs;
  boolean solvingBfs;

  // manual player fields
  Vertex player;
  ArrayList<Vertex> playerPathAnimator;
  ArrayList<Vertex> playerPath;
  boolean playerReconstruct;
  boolean finalStatePlayer;
  boolean manualGameplay;
  boolean playerWon;

  // maze world constructor
  MazeWorld(int width, int height, Random rand) {
    // Maze world setup
    // initialize the maze world dimensions
    this.width = width;
    this.height = height;
    this.cellSize = 600 / this.height;
    this.rand = rand;
    this.vertices = new ArrayList<ArrayList<Vertex>>();
    this.representatives = new HashMap<Vertex, Vertex>();
    this.edges = new ArrayList<Edge>();
    // Initialize the vertices
    for (int i = 0; i < this.width; i++) {
      this.vertices.add(new ArrayList<Vertex>());
      for (int j = 0; j < this.height; j++) {
        Vertex v = new Vertex(i, j);
        this.vertices.get(i).add(v);
        this.representatives.put(v, v);
      }
    }
    // initialize the edges
    this.edges = this.kruskals();

    // initialize the dfs, bfs, and player fields
    // depth first search fields
    this.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    this.dfsPath = new ArrayList<Vertex>();
    this.dfsPath.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.dfsVisited = new ArrayList<Vertex>();
    this.dfsWorklist = new Stack<Vertex>();
    this.animatingDfs = false;
    this.reconstructDfs = false;
    this.finalStateDfs = false;
    this.solvingDfs = false;
    // breadth first search fields
    this.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    this.bfsPath = new ArrayList<Vertex>();
    this.bfsPath.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.bfsVisited = new ArrayList<Vertex>();
    this.bfsWorklist = new LinkedList<Vertex>();
    this.animatingBfs = false;
    this.reconstructBfs = false;
    this.finalStateBfs = false;
    this.solvingBfs = false;
    // manual player fields
    this.playerPathAnimator = new ArrayList<Vertex>();
    this.playerPathAnimator.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.playerPath = new ArrayList<Vertex>();
    this.player = this.vertices.get(0).get(0);
    this.playerPath.add(this.player);
    this.playerReconstruct = false;
    this.finalStatePlayer = false;
    this.manualGameplay = false;
    this.playerWon = false;
  }

  // initialize the edges of the maze
  ArrayList<Edge> kruskals() {
    // track the edges in the tree
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    // all edges in graph, sorted by edge weights
    ArrayList<Edge> worklist = this.generateWorklist();

    // initialize every node's representative to itself
    while (this.moreThanOneTree()) {
      // find the next cheapest edge
      Edge cheapestEdge = worklist.get(0);
      // if the source and destination are in the same tree, remove the edge
      if (this.find(cheapestEdge.source).equals(this.find(cheapestEdge.destination))) {
        worklist.remove(0);
      } else {
        // otherwise, record the edge and update the representatives
        edgesInTree.add(cheapestEdge);
        if (this.representatives.get(cheapestEdge.source).equals(cheapestEdge.source)) {
          this.representatives.put(cheapestEdge.source, cheapestEdge.destination);
        } else {
          this.representatives.put(this.find(cheapestEdge.source),
              this.find(this.find(cheapestEdge.destination)));
        }
        worklist.remove(0);
      }
    }
    // return the edges in the tree
    return edgesInTree;
  }

  // find the representative of the given vertex
  // assuming that the graph is not cyclic
  Vertex find(Vertex v) {
    if (this.representatives.get(v).equals(v)) {
      return v;
    } else {
      return this.find(this.representatives.get(v));
    }
  }

  // generate the sorted worklist of edges
  ArrayList<Edge> generateWorklist() {
    ArrayList<Edge> worklist = new ArrayList<Edge>();
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        if (i == 0 && j == 0) {
          continue;
        } else {
          if (i > 0 && i < this.width) {
            worklist.add(new Edge(this.vertices.get(i).get(j),
                this.vertices.get(i - 1).get(j),
                this.rand.nextInt(1000)));
          }
          if (j > 0 && j < this.height) {
            worklist.add(new Edge(this.vertices.get(i).get(j),
                this.vertices.get(i).get(j - 1),
                this.rand.nextInt(1000)));
          }
        }
      }
    }
    return this.sortEdges(worklist);
  }

  // sort the edges of the maze
  ArrayList<Edge> sortEdges(ArrayList<Edge> edges) {
    Comparator<Edge> comp = new CompareEdge();
    for (int i = 0; i < edges.size() - 1; i++) {
      for (int j = i + 1; j < edges.size(); j++) {
        if (comp.compare(edges.get(i), edges.get(j)) > 0) {
          Edge temp = edges.get(i);
          edges.set(i, edges.get(j));
          edges.set(j, temp);
        }
      }
    }
    return edges;
  }

  // determine if there is more than one tree
  boolean moreThanOneTree() {
    int numTrees = 0;
    for (Vertex v : this.representatives.keySet()) {
      if (this.representatives.get(v).equals(v)) {
        numTrees++;
      }
    }
    return numTrees > 1;
  }

  // draw the open paths of the maze
  void drawEdge(Edge edge, WorldScene ws) {
    Color c = Color.WHITE;
    if (this.dfsVisited.contains(edge.source) && this.dfsVisited.contains(edge.destination)
        && this.animatingDfs) {
      if (this.dfsPath.contains(edge.source) || this.dfsPath.contains(edge.destination)) {
        c = Color.BLUE;
      } else {
        c = Color.CYAN;
      }
    } else if (this.bfsVisited.contains(edge.source) && this.bfsVisited.contains(edge.destination)
        && this.animatingBfs) {
      if (this.bfsPath.contains(edge.source) || this.bfsPath.contains(edge.destination)) {
        c = Color.MAGENTA;
      } else {
        c = Color.PINK;
      }
    } else if ((this.dfsPath.contains(edge.source) || this.dfsPath.contains(edge.destination))
        && this.animatingDfs) {
      c = Color.BLUE;
    } else if ((this.bfsPath.contains(edge.source) || this.bfsPath.contains(edge.destination))
        && this.animatingBfs) {
      c = Color.MAGENTA;
    } else if (this.playerPathAnimator.contains(edge.source)
        && this.playerPathAnimator.contains(edge.destination)) {
      c = Color.ORANGE;
    }
    if (edge.source.x == edge.destination.x) {
      Vertex v = (edge.source.y > edge.destination.y) ? edge.source : edge.destination;
      ws.placeImageXY(new RectangleImage(cellSize - 2, 2,
              OutlineMode.SOLID, c),
          (v.x * cellSize) + cellSize / 2,
          v.y * cellSize);
    } else {
      Vertex v = (edge.source.x > edge.destination.x) ? edge.source : edge.destination;
      ws.placeImageXY(new RectangleImage(2, cellSize - 2,
              OutlineMode.SOLID, c),
          v.x * cellSize,
          (v.y * cellSize) + cellSize / 2);
    }
  }

  // determine if the given vertex is in a cycle
  // FOR TESTING PURPOSES ONLY
  boolean containsCycle(Vertex start, Vertex current) {
    if (this.representatives.get(current).equals(current)) {
      return false;
    }
    if (this.representatives.get(current).equals(start)) {
      return true;
    } else {
      return this.containsCycle(start, this.representatives.get(current));
    }
  }

  // depth first search to find the shortest path from the start to the end
  void dfs() {
    // get the next vertex from the top of the worklist
    Vertex next = this.dfsWorklist.pop();

    // if next has already been visited, discard it
    if (this.dfsVisited.contains(next)) {
      return;
    } else if (next.equals(this.vertices.get(this.width - 1).get(this.height - 1))) {
      // reconstruct the path from the end to the start to show the shortest path
      this.solvingDfs = false;
      this.reconstructDfs = true;
    } else {
      // get edges of next
      ArrayList<Edge> nextEdges = new ArrayList<Edge>();
      for (Edge e : this.edges) {
        if (e.hasVertex(next)) {
          nextEdges.add(e);
        }
      }
      // for each neighbor n of next
      for (Edge e : nextEdges) {
        Vertex neighbor = (e.source.equals(next)) ? e.destination : e.source;
        // add n to the worklist
        this.dfsWorklist.push(neighbor);
        // record the edge from next to n in the cameFromEdge map
        this.cameFromEdgeDfs.putIfAbsent(neighbor, e);
      }
    }

    // update the visited vertices
    this.dfsVisited.add(next);
  }

  // breadth first search to find the shortest path from the start to the end
  void bfs() {
    // get the next vertex from the head of the queue
    Vertex next = this.bfsWorklist.remove();

    // if next has already been visited, discard it
    if (this.bfsVisited.contains(next)) {
      return;
    } else if (next.equals(this.vertices.get(this.width - 1).get(this.height - 1))) {
      // reconstruct the path from the end to the start to show the shortest path
      this.solvingBfs = false;
      this.reconstructBfs = true;
    } else {
      // get edges of next
      ArrayList<Edge> nextEdges = new ArrayList<Edge>();
      for (Edge e : this.edges) {
        if (e.hasVertex(next)) {
          nextEdges.add(e);
        }
      }
      // for each neighbor n of next
      for (Edge e : nextEdges) {
        Vertex neighbor = (e.source.equals(next)) ? e.destination : e.source;
        // add n to the worklist
        this.bfsWorklist.add(neighbor);
        // record the edge from next to n in the cameFromEdge map
        this.cameFromEdgeBfs.putIfAbsent(neighbor, e);
      }
    }

    //update the visited vertices
    this.bfsVisited.add(next);
  }

  // reconstruct the path from the end to the start to show the shortest path (DFS)
  void reconstructDfsPath(Vertex current) {
    if (!current.equals(this.vertices.get(0).get(0))) {
      Edge e = this.cameFromEdgeDfs.get(current);
      Vertex neighbor = (e.source.equals(current)) ? e.destination : e.source;
      this.dfsPath.add(neighbor);
    } else {
      this.reconstructDfs = false;
      this.finalStateDfs = true;
    }
  }

  // reconstruct the path from the end to the start to show the shortest path (BFS)
  void reconstructBfsPath(Vertex current) {
    if (!current.equals(this.vertices.get(0).get(0))) {
      Edge e = this.cameFromEdgeBfs.get(current);
      Vertex neighbor = (e.source.equals(current)) ? e.destination : e.source;
      this.bfsPath.add(neighbor);
    } else {
      this.reconstructBfs = false;
      this.finalStateBfs = true;
    }
  }

  // reconstruct the path from the end to the start to show the shortest path (Player)
  void reconstructPlayerPath() {
    if (this.playerPath.size() == 0) {
      this.playerReconstruct = false;
      this.finalStatePlayer = true;
      return;
    }
    this.playerPathAnimator.add(this.playerPath.remove(this.playerPath.size() - 1));
  }

  // move player up
  void moveUp() {
    if (this.player.y > 0) {
      Vertex next = this.vertices.get(this.player.x).get(this.player.y - 1);
      for (Edge e : this.edges) {
        if (e.hasVertex(this.player) && e.hasVertex(next)) {
          this.player = next;
          this.playerPath.add(this.player);
          break;
        }
      }
      if (this.player.equals(this.vertices.get(this.width - 1).get(this.height - 1))) {
        this.playerReconstruct = true;
        this.playerWon = true;
      }
    }
  }

  // move player down
  void moveDown() {
    if (this.player.y < this.height - 1) {
      Vertex next = this.vertices.get(this.player.x).get(this.player.y + 1);
      for (Edge e : this.edges) {
        if (e.hasVertex(this.player) && e.hasVertex(next)) {
          this.player = next;
          this.playerPath.add(this.player);
          break;
        }
      }
      if (this.player.equals(this.vertices.get(this.width - 1).get(this.height - 1))) {
        this.playerReconstruct = true;
        this.playerWon = true;
      }
    }
  }

  // move player left
  void moveLeft() {
    if (this.player.x > 0) {
      Vertex next = this.vertices.get(this.player.x - 1).get(this.player.y);
      for (Edge e : this.edges) {
        if (e.hasVertex(this.player) && e.hasVertex(next)) {
          this.player = next;
          this.playerPath.add(this.player);
          break;
        }
      }
      if (this.player.equals(this.vertices.get(this.width - 1).get(this.height - 1))) {
        this.playerReconstruct = true;
        this.playerWon = true;
      }
    }
  }

  // move player right
  void moveRight() {
    if (this.player.x < this.width - 1) {
      Vertex next = this.vertices.get(this.player.x + 1).get(this.player.y);
      for (Edge e : this.edges) {
        if (e.hasVertex(this.player) && e.hasVertex(next)) {
          this.player = next;
          this.playerPath.add(this.player);
          break;
        }
      }
      if (this.player.equals(this.vertices.get(this.width - 1).get(this.height - 1))) {
        this.playerReconstruct = true;
        this.playerWon = true;
      }
    }
  }

  // reset entire world
  void reset() {
    // reset maze fields
    this.vertices = new ArrayList<ArrayList<Vertex>>();
    this.representatives = new HashMap<Vertex, Vertex>();
    for (int i = 0; i < this.width; i++) {
      this.vertices.add(new ArrayList<Vertex>());
      for (int j = 0; j < this.height; j++) {
        Vertex v = new Vertex(i, j);
        this.vertices.get(i).add(v);
        this.representatives.put(v, v);
      }
    }
    this.edges = this.kruskals();

    // reset dfs, bfs, and player fields
    // depth first search fields
    this.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    this.dfsPath = new ArrayList<Vertex>();
    this.dfsPath.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.dfsVisited = new ArrayList<Vertex>();
    this.dfsWorklist = new Stack<Vertex>();
    this.animatingDfs = false;
    this.reconstructDfs = false;
    this.finalStateDfs = false;
    this.solvingDfs = false;
    // breadth first search fields
    this.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    this.bfsPath = new ArrayList<Vertex>();
    this.bfsPath.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.bfsVisited = new ArrayList<Vertex>();
    this.bfsWorklist = new LinkedList<Vertex>();
    this.animatingBfs = false;
    this.reconstructBfs = false;
    this.finalStateBfs = false;
    this.solvingBfs = false;
    // manual player fields
    this.playerPathAnimator = new ArrayList<Vertex>();
    this.playerPathAnimator.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.playerPath = new ArrayList<Vertex>();
    this.player = this.vertices.get(0).get(0);
    this.playerPath.add(this.player);
    this.playerReconstruct = false;
    this.finalStatePlayer = false;
    this.manualGameplay = false;
    this.playerWon = false;
  }

  // reset player world only to exit manual gameplay
  void resetPlayer() {
    // reset manual player fields
    this.playerPathAnimator = new ArrayList<Vertex>();
    this.playerPathAnimator.add(this.vertices.get(this.width - 1).get(this.height - 1));
    this.playerPath = new ArrayList<Vertex>();
    this.player = this.vertices.get(0).get(0);
    this.playerPath.add(this.player);
    this.playerReconstruct = false;
    this.finalStatePlayer = false;
    this.manualGameplay = false;
    this.playerWon = false;
  }

  // on key event, handle accordingly
  public void onKeyEvent(String key) {
    if (key.equals("d")) {
      // if already solving, toggle animation
      if (this.animatingDfs || this.animatingBfs) {
        this.animatingDfs = true;
        this.animatingBfs = false;
        return;
      }
      // setup for dfs and bfs if not already solving
      // depth first search starts
      this.animatingDfs = true;

      // dfs
      this.solvingDfs = true;
      this.dfsWorklist = new Stack<Vertex>();
      this.dfsWorklist.push(this.vertices.get(0).get(0));
      this.dfsVisited = new ArrayList<Vertex>();
      // bfs
      this.solvingBfs = true;
      this.bfsWorklist = new LinkedList<Vertex>();
      this.bfsWorklist.add(this.vertices.get(0).get(0));
      this.bfsVisited = new ArrayList<Vertex>();
    } else if (key.equals("b")) {
      // if already solving, toggle animation
      if (this.animatingDfs || this.animatingBfs) {
        this.animatingBfs = true;
        this.animatingDfs = false;
        return;
      }
      // setup for dfs and bfs if not already solving
      // breadth first search starts
      this.animatingBfs = true;

      // bfs
      this.solvingBfs = true;
      this.bfsWorklist = new LinkedList<Vertex>();
      this.bfsWorklist.add(this.vertices.get(0).get(0));
      this.bfsVisited = new ArrayList<Vertex>();
      // dfs
      this.solvingDfs = true;
      this.dfsWorklist = new Stack<Vertex>();
      this.dfsWorklist.push(this.vertices.get(0).get(0));
      this.dfsVisited = new ArrayList<Vertex>();
    } else if (key.equals("m") && !animatingDfs && !animatingBfs) {
      this.manualGameplay = true;
    } else if (key.equals("up") && this.manualGameplay && !this.finalStatePlayer) {
      this.moveUp();
    } else if (key.equals("down") && this.manualGameplay && !this.finalStatePlayer) {
      this.moveDown();
    } else if (key.equals("left") && this.manualGameplay && !this.finalStatePlayer) {
      this.moveLeft();
    } else if (key.equals("right") && this.manualGameplay && !this.finalStatePlayer) {
      this.moveRight();
    } else if (key.equals("escape")) {
      this.resetPlayer();
    } else if (key.equals("r")) {
      this.reset();
    }
  }

  // on tick
  public void onTick() {
    if (this.solvingDfs) {
      this.dfs();
    }
    if (this.solvingBfs) {
      this.bfs();
    }
    if (this.reconstructDfs) {
      this.reconstructDfsPath(this.dfsPath.get(this.dfsPath.size() - 1));
    }
    if (this.reconstructBfs) {
      this.reconstructBfsPath(this.bfsPath.get(this.bfsPath.size() - 1));
    }
    if (this.playerReconstruct) {
      this.reconstructPlayerPath();
    }
  }

  // draw the world
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.width * this.cellSize,
        this.height * this.cellSize + 200);

    // create white background behind black maze background
    ws.placeImageXY(new RectangleImage(this.width * cellSize,
            this.height * cellSize + 200,
            OutlineMode.SOLID, Color.WHITE), this.width * cellSize / 2 + 100,
        this.height * cellSize / 2 + 100);

    // create black background
    ws.placeImageXY(new RectangleImage(this.width * cellSize,
            this.height * cellSize,
            OutlineMode.SOLID, Color.BLACK), this.width * cellSize / 2,
        this.height * cellSize / 2);

    // draw maze vertices
    for (int x = 0; x < this.vertices.size(); x++) {
      for (int y = 0; y < this.vertices.get(x).size(); y++) {
        if (this.player.equals(this.vertices.get(x).get(y))
            && this.manualGameplay) { // color manual player
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.ORANGE, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        } else if (this.dfsPath.contains(this.vertices.get(x).get(y))
            && (this.reconstructDfs || this.finalStateDfs)
            && this.animatingDfs) { // color dfs path reconstruction
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.BLUE, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        } else if (this.bfsPath.contains(this.vertices.get(x).get(y))
            && (this.reconstructBfs || this.finalStateBfs)
            && this.animatingBfs) { // color bfs path reconstruction
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.MAGENTA, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        } else if (this.playerPathAnimator.contains(this.vertices.get(x).get(y))
            && (this.playerReconstruct || this.finalStatePlayer)) {
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.ORANGE, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        } else if (this.dfsVisited.contains(this.vertices.get(x).get(y))
            && this.animatingDfs) { // color dfs visited vertices
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.CYAN, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        } else if (this.bfsVisited.contains(this.vertices.get(x).get(y))
            && this.animatingBfs) { // color bfs visited vertices
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.PINK, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        } else { // color blank cells
          ws.placeImageXY(this.vertices.get(x).get(y).drawVertex(Color.WHITE, this.cellSize),
              (this.vertices.get(x).get(y).x * cellSize) + cellSize / 2,
              (this.vertices.get(x).get(y).y * cellSize) + cellSize / 2);
        }
      }
    }

    // draw the edges of the maze
    for (Edge e : this.edges) {
      this.drawEdge(e, ws);
    }

    // highlight the beginning and ending of the maze
    if (!(this.player.equals(this.vertices.get(0).get(0)) && this.manualGameplay)) {
      ws.placeImageXY(this.vertices.get(0).get(0)
              .drawVertex(Color.GREEN, this.cellSize),
          (this.vertices.get(0).get(0).x * cellSize) + cellSize / 2,
          (this.vertices.get(0).get(0).y * cellSize) + cellSize / 2);
    }
    if (!(this.player.equals(this.vertices.get(this.width - 1).get(this.height - 1))
        && this.manualGameplay)) {
      ws.placeImageXY(this.vertices.get(this.width - 1).get(this.height - 1)
              .drawVertex(Color.RED, this.cellSize),
          (this.vertices.get(this.width - 1).get(this.height - 1).x * cellSize) + cellSize / 2,
          (this.vertices.get(this.width - 1).get(this.height - 1).y * cellSize) + cellSize / 2);
    }

    // display the current mode
    if (this.animatingDfs) {
      ws.placeImageXY(new TextImage("Mode: DFS", 15, FontStyle.BOLD, Color.BLACK),
          60, this.height * this.cellSize + 20);
    } else if (this.animatingBfs) {
      ws.placeImageXY(new TextImage("Mode: BFS", 15, FontStyle.BOLD, Color.BLACK),
          60, this.height * this.cellSize + 20);
    } else if (this.manualGameplay) {
      ws.placeImageXY(new TextImage("Mode: Manual", 15, FontStyle.BOLD, Color.BLACK),
          60, this.height * this.cellSize + 20);
    } else {
      ws.placeImageXY(new TextImage("Mode: None", 15, FontStyle.BOLD, Color.BLACK),
          60, this.height * this.cellSize + 20);
    }


    // write the winning message under the maze if the player has won
    if (this.playerWon) {
      ws.placeImageXY(new TextImage("You won!", (this.width * this.cellSize) / 20,
              FontStyle.BOLD, Color.GREEN),
          this.width * this.cellSize / 2, this.height * this.cellSize + 70);
      ws.placeImageXY(new TextImage("You only needed " + this.playerPathAnimator.size() + " moves!",
              (this.width * this.cellSize) / 30, FontStyle.BOLD, Color.GREEN),
          this.width * this.cellSize / 2, this.height * this.cellSize + 120);
      ws.placeImageXY(new TextImage("Press 'esc' and then 'd' or 'b' to see the shortest path",
              (this.width * this.cellSize) / 30, FontStyle.BOLD, Color.GREEN),
          this.width * this.cellSize / 2, this.height * this.cellSize + 165);
    } else if (this.finalStateDfs && this.animatingDfs) {
      ws.placeImageXY(new TextImage("The shortest path is " + this.dfsPath.size() + " moves long",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.BLUE),
          this.width * this.cellSize / 2, this.height * this.cellSize + 65);
      ws.placeImageXY(new TextImage("Depth First Search took",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.CYAN),
          this.width * this.cellSize / 2, this.height * this.cellSize + 100);
      ws.placeImageXY(new TextImage((this.dfsVisited.size() - this.dfsPath.size())
              + " wrong moves before solving the maze",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.CYAN),
          this.width * this.cellSize / 2, this.height * this.cellSize + 135);
    } else if (this.finalStateBfs && this.animatingBfs) {
      ws.placeImageXY(new TextImage("The shortest path is " + this.bfsPath.size() + " moves long",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.MAGENTA),
          this.width * this.cellSize / 2, this.height * this.cellSize + 65);
      ws.placeImageXY(new TextImage("Breadth First Search took",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.PINK),
          this.width * this.cellSize / 2, this.height * this.cellSize + 100);
      ws.placeImageXY(new TextImage((this.bfsVisited.size() - this.bfsPath.size())
              + " wrong moves before solving the maze",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.PINK),
          this.width * this.cellSize / 2, this.height * this.cellSize + 135);
    } else {
      // write instructions in the bottom left under the maze
      if (!this.manualGameplay) {
        ws.placeImageXY(new TextImage("Press 'm' to enter manual gameplay",
                (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.BLACK),
            this.width * this.cellSize / 2, this.height * this.cellSize + 45);
        ws.placeImageXY(new TextImage("Press 'd'/'b' to toggle between depth/breadth first search",
                (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.BLACK),
            this.width * this.cellSize / 2, this.height * this.cellSize + 85);
      } else {
        ws.placeImageXY(new TextImage("Press 'esc' to exit manual gameplay",
                (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.BLACK),
            this.width * this.cellSize / 2, this.height * this.cellSize + 45);
        ws.placeImageXY(new TextImage("Use arrow keys to move through maze",
                (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.BLACK),
            this.width * this.cellSize / 2, this.height * this.cellSize + 85);
      }
      ws.placeImageXY(new TextImage("Press 'r' to reset and create new maze",
              (this.width * this.cellSize) / 50, FontStyle.BOLD, Color.BLACK),
          this.width * this.cellSize / 2, this.height * this.cellSize + 125);
    }

    return ws;
  }
}


// compare two edges to sort from least to greatest
class CompareEdge implements Comparator<Edge> {
  // compare two edges by their weight
  public int compare(Edge e1, Edge e2) {
    if (e1.weight < e2.weight) {
      return -1;
    } else if (e1.weight > e2.weight) {
      return 1;
    } else {
      return 0;
    }
  }
}

class ExamplesMaze {
  // test big bang and the makeScene method
  void testMazeBigBang(Tester t) {
    MazeWorld maze = new MazeWorld(50, 25, new Random());
    maze.bigBang(maze.cellSize * maze.width, maze.cellSize * maze.height + 200, 0.001);
  }

  // TESTS FOR THE VERTEX CLASS
  Vertex v1;
  Vertex v2;
  Vertex v3;
  Vertex v4;
  Vertex v5;

  // initialize vertices
  void initVertices() {
    this.v1 = new Vertex(0, 0);
    this.v2 = new Vertex(1, 0);
    this.v3 = new Vertex(0, 1);
    this.v4 = new Vertex(1, 1);
    this.v5 = new Vertex(2, 1);
  }

  // test Vertex constructor
  void testVertexConstructor(Tester t) {
    this.initVertices();
    t.checkExpect(this.v1.x, 0);
    t.checkExpect(this.v1.y, 0);
    t.checkExpect(this.v2.x, 1);
    t.checkExpect(this.v2.y, 0);
    t.checkExpect(this.v3.x, 0);
    t.checkExpect(this.v3.y, 1);
    t.checkExpect(this.v4.x, 1);
    t.checkExpect(this.v4.y, 1);
    t.checkExpect(this.v5.x, 2);
    t.checkExpect(this.v5.y, 1);
  }

  // test Vertex drawVertex method
  void testDrawVertex(Tester t) {
    this.initVertices();
    t.checkExpect(this.v1.drawVertex(Color.WHITE, 10),
        new RectangleImage(8, 8, OutlineMode.SOLID, Color.WHITE));
    t.checkExpect(this.v2.drawVertex(Color.BLUE, 20),
        new RectangleImage(18, 18, OutlineMode.SOLID, Color.BLUE));
    t.checkExpect(this.v3.drawVertex(Color.RED, 30),
        new RectangleImage(28, 28, OutlineMode.SOLID, Color.RED));
    t.checkExpect(this.v4.drawVertex(Color.GREEN, 40),
        new RectangleImage(38, 38, OutlineMode.SOLID, Color.GREEN));
    t.checkExpect(this.v5.drawVertex(Color.YELLOW, 50),
        new RectangleImage(48, 48, OutlineMode.SOLID, Color.YELLOW));
  }


  // TESTS FOR THE EDGE CLASS
  Edge e1;
  Edge e2;
  Edge e3;
  Edge e4;
  Edge e5;

  // initialize edges
  void initEdges() {
    this.initVertices();
    this.e1 = new Edge(this.v1, this.v2, 1);
    this.e2 = new Edge(this.v1, this.v3, 2);
    this.e3 = new Edge(this.v2, this.v4, 3);
    this.e4 = new Edge(this.v3, this.v4, 4);
    this.e5 = new Edge(this.v4, this.v5, 5);
  }

  // test Edge constructor
  void testEdgeConstructor(Tester t) {
    this.initEdges();
    t.checkExpect(this.e1.source, this.v1);
    t.checkExpect(this.e1.destination, this.v2);
    t.checkExpect(this.e1.weight, 1);
    t.checkExpect(this.e2.source, this.v1);
    t.checkExpect(this.e2.destination, this.v3);
    t.checkExpect(this.e2.weight, 2);
    t.checkExpect(this.e3.source, this.v2);
    t.checkExpect(this.e3.destination, this.v4);
    t.checkExpect(this.e3.weight, 3);
    t.checkExpect(this.e4.source, this.v3);
    t.checkExpect(this.e4.destination, this.v4);
    t.checkExpect(this.e4.weight, 4);
    t.checkExpect(this.e5.source, this.v4);
    t.checkExpect(this.e5.destination, this.v5);
    t.checkExpect(this.e5.weight, 5);
  }

  // test hasVertex method
  void testHasVertex(Tester t) {
    this.initEdges();

    t.checkExpect(this.e1.hasVertex(this.v1), true);
    t.checkExpect(this.e1.hasVertex(this.v2), true);
    t.checkExpect(this.e1.hasVertex(this.v3), false);
    t.checkExpect(this.e1.hasVertex(this.v4), false);
    t.checkExpect(this.e1.hasVertex(this.v5), false);

    t.checkExpect(this.e2.hasVertex(this.v1), true);
    t.checkExpect(this.e2.hasVertex(this.v2), false);
    t.checkExpect(this.e2.hasVertex(this.v3), true);
    t.checkExpect(this.e2.hasVertex(this.v4), false);
    t.checkExpect(this.e2.hasVertex(this.v5), false);

    t.checkExpect(this.e3.hasVertex(this.v1), false);
    t.checkExpect(this.e3.hasVertex(this.v2), true);
    t.checkExpect(this.e3.hasVertex(this.v3), false);
    t.checkExpect(this.e3.hasVertex(this.v4), true);
    t.checkExpect(this.e3.hasVertex(this.v5), false);

    t.checkExpect(this.e4.hasVertex(this.v1), false);
    t.checkExpect(this.e4.hasVertex(this.v2), false);
    t.checkExpect(this.e4.hasVertex(this.v3), true);
    t.checkExpect(this.e4.hasVertex(this.v4), true);
    t.checkExpect(this.e4.hasVertex(this.v5), false);

    t.checkExpect(this.e5.hasVertex(this.v1), false);
    t.checkExpect(this.e5.hasVertex(this.v2), false);
    t.checkExpect(this.e5.hasVertex(this.v3), false);
    t.checkExpect(this.e5.hasVertex(this.v4), true);
    t.checkExpect(this.e5.hasVertex(this.v5), true);
  }

  // TEST THE COMPARE EDGE CLASS
  CompareEdge ce = new CompareEdge();

  void testCompare(Tester t) {
    this.initEdges();
    t.checkExpect(this.ce.compare(this.e1, this.e1), 0);
    t.checkExpect(this.ce.compare(this.e1, this.e2), -1);
    t.checkExpect(this.ce.compare(this.e1, this.e3), -1);
    t.checkExpect(this.ce.compare(this.e1, this.e4), -1);
    t.checkExpect(this.ce.compare(this.e1, this.e5), -1);

    t.checkExpect(this.ce.compare(this.e2, this.e1), 1);
    t.checkExpect(this.ce.compare(this.e2, this.e2), 0);
    t.checkExpect(this.ce.compare(this.e2, this.e3), -1);
    t.checkExpect(this.ce.compare(this.e2, this.e4), -1);
    t.checkExpect(this.ce.compare(this.e2, this.e5), -1);

    t.checkExpect(this.ce.compare(this.e3, this.e1), 1);
    t.checkExpect(this.ce.compare(this.e3, this.e2), 1);
    t.checkExpect(this.ce.compare(this.e3, this.e3), 0);
    t.checkExpect(this.ce.compare(this.e3, this.e4), -1);
    t.checkExpect(this.ce.compare(this.e3, this.e5), -1);

    t.checkExpect(this.ce.compare(this.e4, this.e1), 1);
    t.checkExpect(this.ce.compare(this.e4, this.e2), 1);
    t.checkExpect(this.ce.compare(this.e4, this.e3), 1);
    t.checkExpect(this.ce.compare(this.e4, this.e4), 0);
    t.checkExpect(this.ce.compare(this.e4, this.e5), -1);

    t.checkExpect(this.ce.compare(this.e5, this.e1), 1);
    t.checkExpect(this.ce.compare(this.e5, this.e2), 1);
    t.checkExpect(this.ce.compare(this.e5, this.e3), 1);
    t.checkExpect(this.ce.compare(this.e5, this.e4), 1);
    t.checkExpect(this.ce.compare(this.e5, this.e5), 0);
  }

  // TESTS FOR THE MAZE WORLD CLASS

  MazeWorld mw1;
  MazeWorld mw2;
  MazeWorld mw3;
  MazeWorld mw4;
  MazeWorld mw5;

  Random rand1 = new Random(1);
  Random rand2 = new Random(2);
  Random rand3 = new Random(3);
  Random rand4 = new Random(4);
  Random rand5 = new Random(5);

  ArrayList<Vertex> verticesTest = new ArrayList<Vertex>();


  // initialize maze worlds
  void initMazeWorlds() {
    this.mw1 = new MazeWorld(10, 10, rand1);
    this.mw2 = new MazeWorld(10, 20, rand2);
    this.mw3 = new MazeWorld(30, 50, rand3);
    this.mw4 = new MazeWorld(50, 50, rand4);
    this.mw5 = new MazeWorld(70, 60, rand5);
  }

  // test MazeWorld constructor
  void testMazeWorldConstructor(Tester t) {
    this.initMazeWorlds();
    // test mw1
    t.checkExpect(this.mw1.width, 10);
    t.checkExpect(this.mw1.height, 10);
    t.checkExpect(this.mw1.cellSize, 60);
    t.checkExpect(this.mw1.rand, rand1);
    ArrayList<ArrayList<Vertex>> verticesTest1 = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < mw1.width; i++) {
      verticesTest1.add(new ArrayList<Vertex>());
      for (int j = 0; j < mw1.height; j++) {
        Vertex v = new Vertex(i, j);
        verticesTest1.get(i).add(v);
      }
    }
    t.checkExpect(this.mw1.vertices, verticesTest1);
    t.checkExpect(this.mw1.representatives.keySet().size(), 100);
    t.checkExpect(mw1.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw1.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw1.vertices.get(mw1.width - 1).get(mw1.height - 1))));
    t.checkExpect(mw1.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw1.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw1.animatingDfs, false);
    t.checkExpect(mw1.reconstructDfs, false);
    t.checkExpect(mw1.finalStateDfs, false);
    t.checkExpect(mw1.solvingDfs, false);
    t.checkExpect(mw1.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw1.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw1.vertices.get(mw1.width - 1).get(mw1.height - 1))));
    t.checkExpect(mw1.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw1.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw1.animatingBfs, false);
    t.checkExpect(mw1.reconstructBfs, false);
    t.checkExpect(mw1.finalStateBfs, false);
    t.checkExpect(mw1.solvingBfs, false);
    t.checkExpect(mw1.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw1.vertices.get(mw1.width - 1).get(mw1.height - 1))));
    t.checkExpect(mw1.playerPath, new ArrayList<Vertex>(Arrays.asList(mw1.player)));
    t.checkExpect(mw1.player, mw1.vertices.get(0).get(0));
    t.checkExpect(mw1.playerReconstruct, false);
    t.checkExpect(mw1.finalStatePlayer, false);
    t.checkExpect(mw1.manualGameplay, false);
    t.checkExpect(mw1.playerWon, false);

    // test mw2
    t.checkExpect(this.mw2.width, 10);
    t.checkExpect(this.mw2.height, 20);
    t.checkExpect(this.mw2.cellSize, 30);
    t.checkExpect(this.mw2.rand, rand2);
    ArrayList<ArrayList<Vertex>> verticesTest2 = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < mw2.width; i++) {
      verticesTest2.add(new ArrayList<Vertex>());
      for (int j = 0; j < mw2.height; j++) {
        Vertex v = new Vertex(i, j);
        verticesTest2.get(i).add(v);
      }
    }
    t.checkExpect(this.mw2.vertices, verticesTest2);
    t.checkExpect(this.mw2.representatives.keySet().size(), 200);
    t.checkExpect(mw2.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw2.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw2.vertices.get(mw2.width - 1).get(mw2.height - 1))));
    t.checkExpect(mw2.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw2.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw2.animatingDfs, false);
    t.checkExpect(mw2.reconstructDfs, false);
    t.checkExpect(mw2.finalStateDfs, false);
    t.checkExpect(mw2.solvingDfs, false);
    t.checkExpect(mw2.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw2.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw2.vertices.get(mw2.width - 1).get(mw2.height - 1))));
    t.checkExpect(mw2.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw2.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw2.animatingBfs, false);
    t.checkExpect(mw2.reconstructBfs, false);
    t.checkExpect(mw2.finalStateBfs, false);
    t.checkExpect(mw2.solvingBfs, false);
    t.checkExpect(mw2.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw2.vertices.get(mw2.width - 1).get(mw2.height - 1))));
    t.checkExpect(mw2.playerPath, new ArrayList<Vertex>(Arrays.asList(mw2.player)));
    t.checkExpect(mw2.player, mw2.vertices.get(0).get(0));
    t.checkExpect(mw2.playerReconstruct, false);
    t.checkExpect(mw2.finalStatePlayer, false);
    t.checkExpect(mw2.manualGameplay, false);
    t.checkExpect(mw2.playerWon, false);

    // test mw3
    t.checkExpect(this.mw3.width, 30);
    t.checkExpect(this.mw3.height, 50);
    t.checkExpect(this.mw3.cellSize, 12);
    t.checkExpect(this.mw3.rand, rand3);
    ArrayList<ArrayList<Vertex>> verticesTest3 = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < mw3.width; i++) {
      verticesTest3.add(new ArrayList<Vertex>());
      for (int j = 0; j < mw3.height; j++) {
        Vertex v = new Vertex(i, j);
        verticesTest3.get(i).add(v);
      }
    }
    t.checkExpect(this.mw3.vertices, verticesTest3);
    t.checkExpect(this.mw3.representatives.keySet().size(), 1500);
    t.checkExpect(mw3.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw3.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw3.vertices.get(mw3.width - 1).get(mw3.height - 1))));
    t.checkExpect(mw3.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw3.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw3.animatingDfs, false);
    t.checkExpect(mw3.reconstructDfs, false);
    t.checkExpect(mw3.finalStateDfs, false);
    t.checkExpect(mw3.solvingDfs, false);
    t.checkExpect(mw3.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw3.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw3.vertices.get(mw3.width - 1).get(mw3.height - 1))));
    t.checkExpect(mw3.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw3.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw3.animatingBfs, false);
    t.checkExpect(mw3.reconstructBfs, false);
    t.checkExpect(mw3.finalStateBfs, false);
    t.checkExpect(mw3.solvingBfs, false);
    t.checkExpect(mw3.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw3.vertices.get(mw3.width - 1).get(mw3.height - 1))));
    t.checkExpect(mw3.playerPath, new ArrayList<Vertex>(Arrays.asList(mw3.player)));
    t.checkExpect(mw3.player, mw3.vertices.get(0).get(0));
    t.checkExpect(mw3.playerReconstruct, false);
    t.checkExpect(mw3.finalStatePlayer, false);
    t.checkExpect(mw3.manualGameplay, false);
    t.checkExpect(mw3.playerWon, false);

    // test mw4
    t.checkExpect(this.mw4.width, 50);
    t.checkExpect(this.mw4.height, 50);
    t.checkExpect(this.mw4.cellSize, 12);
    t.checkExpect(this.mw4.rand, rand4);
    ArrayList<ArrayList<Vertex>> verticesTest4 = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < mw4.width; i++) {
      verticesTest4.add(new ArrayList<Vertex>());
      for (int j = 0; j < mw4.height; j++) {
        Vertex v = new Vertex(i, j);
        verticesTest4.get(i).add(v);
      }
    }
    t.checkExpect(this.mw4.vertices, verticesTest4);
    t.checkExpect(this.mw4.representatives.keySet().size(), 2500);
    t.checkExpect(mw4.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw4.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw4.vertices.get(mw4.width - 1).get(mw4.height - 1))));
    t.checkExpect(mw4.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw4.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw4.animatingDfs, false);
    t.checkExpect(mw4.reconstructDfs, false);
    t.checkExpect(mw4.finalStateDfs, false);
    t.checkExpect(mw4.solvingDfs, false);
    t.checkExpect(mw4.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw4.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw4.vertices.get(mw4.width - 1).get(mw4.height - 1))));
    t.checkExpect(mw4.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw4.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw4.animatingBfs, false);
    t.checkExpect(mw4.reconstructBfs, false);
    t.checkExpect(mw4.finalStateBfs, false);
    t.checkExpect(mw4.solvingBfs, false);
    t.checkExpect(mw4.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw4.vertices.get(mw4.width - 1).get(mw4.height - 1))));
    t.checkExpect(mw4.playerPath, new ArrayList<Vertex>(Arrays.asList(mw4.player)));
    t.checkExpect(mw4.player, mw4.vertices.get(0).get(0));
    t.checkExpect(mw4.playerReconstruct, false);
    t.checkExpect(mw4.finalStatePlayer, false);
    t.checkExpect(mw4.manualGameplay, false);
    t.checkExpect(mw4.playerWon, false);

    // test mw5
    t.checkExpect(this.mw5.width, 70);
    t.checkExpect(this.mw5.height, 60);
    t.checkExpect(this.mw5.cellSize, 10);
    t.checkExpect(this.mw5.rand, rand5);
    ArrayList<ArrayList<Vertex>> verticesTest5 = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < mw5.width; i++) {
      verticesTest5.add(new ArrayList<Vertex>());
      for (int j = 0; j < mw5.height; j++) {
        Vertex v = new Vertex(i, j);
        verticesTest5.get(i).add(v);
      }
    }
    t.checkExpect(this.mw5.vertices, verticesTest5);
    t.checkExpect(this.mw5.representatives.keySet().size(), 4200);
    t.checkExpect(mw5.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw5.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw5.vertices.get(mw5.width - 1).get(mw5.height - 1))));
    t.checkExpect(mw5.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw5.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw5.animatingDfs, false);
    t.checkExpect(mw5.reconstructDfs, false);
    t.checkExpect(mw5.finalStateDfs, false);
    t.checkExpect(mw5.solvingDfs, false);
    t.checkExpect(mw5.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw5.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw5.vertices.get(mw5.width - 1).get(mw5.height - 1))));
    t.checkExpect(mw5.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw5.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw5.animatingBfs, false);
    t.checkExpect(mw5.reconstructBfs, false);
    t.checkExpect(mw5.finalStateBfs, false);
    t.checkExpect(mw5.solvingBfs, false);
    t.checkExpect(mw5.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw5.vertices.get(mw5.width - 1).get(mw5.height - 1))));
    t.checkExpect(mw5.playerPath, new ArrayList<Vertex>(Arrays.asList(mw5.player)));
    t.checkExpect(mw5.player, mw5.vertices.get(0).get(0));
    t.checkExpect(mw5.playerReconstruct, false);
    t.checkExpect(mw5.finalStatePlayer, false);
    t.checkExpect(mw5.manualGameplay, false);
    t.checkExpect(mw5.playerWon, false);
  }

  // test the kruskals method
  void testKruskals(Tester t) {
    this.initMazeWorlds();

    // test mw1
    t.checkExpect(this.mw1.moreThanOneTree(), false);
    for (Vertex v : this.mw1.representatives.keySet()) {
      t.checkExpect(this.mw1.containsCycle(v, v), false);
    }

    // test mw2
    t.checkExpect(this.mw2.moreThanOneTree(), false);
    for (Vertex v : this.mw2.representatives.keySet()) {
      t.checkExpect(this.mw2.containsCycle(v, v), false);
    }

    // test mw3
    t.checkExpect(this.mw3.moreThanOneTree(), false);
    for (Vertex v : this.mw3.representatives.keySet()) {
      t.checkExpect(this.mw3.containsCycle(v, v), false);
    }

    // test mw4
    t.checkExpect(this.mw4.moreThanOneTree(), false);
    for (Vertex v : this.mw4.representatives.keySet()) {
      t.checkExpect(this.mw4.containsCycle(v, v), false);
    }

    // test mw5
    t.checkExpect(this.mw5.moreThanOneTree(), false);
    for (Vertex v : this.mw5.representatives.keySet()) {
      t.checkExpect(this.mw5.containsCycle(v, v), false);
    }
  }

  // test the find method
  void testFind(Tester t) {
    this.initMazeWorlds();

    // test mw1
    // reset the representatives
    this.mw1.representatives = new HashMap<Vertex, Vertex>();
    for (ArrayList<Vertex> row : this.mw1.vertices) {
      for (Vertex v : row) {
        this.mw1.representatives.put(v, v);
      }
    }
    t.checkExpect(this.mw1.find(this.mw1.vertices.get(0).get(0)),
        this.mw1.representatives.get(this.mw1.vertices.get(0).get(0)));
    t.checkExpect(this.mw1.find(this.mw1.vertices.get(1).get(1)),
        this.mw1.representatives.get(this.mw1.vertices.get(1).get(1)));
    t.checkExpect(this.mw1.find(this.mw1.vertices.get(2).get(2)),
        this.mw1.representatives.get(this.mw1.vertices.get(2).get(2)));
    t.checkExpect(this.mw1.find(this.mw1.vertices.get(3).get(3)),
        this.mw1.representatives.get(this.mw1.vertices.get(3).get(3)));
    t.checkExpect(this.mw1.find(this.mw1.vertices.get(4).get(4)),
        this.mw1.representatives.get(this.mw1.vertices.get(4).get(4)));
    t.checkExpect(this.mw1.find(this.mw1.vertices.get(5).get(5)),
        this.mw1.representatives.get(this.mw1.vertices.get(5).get(5)));

    // test mw2
    // reset the representatives
    this.mw2.representatives = new HashMap<Vertex, Vertex>();
    for (ArrayList<Vertex> row : this.mw2.vertices) {
      for (Vertex v : row) {
        this.mw2.representatives.put(v, v);
      }
    }
    t.checkExpect(this.mw2.find(this.mw2.vertices.get(0).get(0)),
        this.mw2.representatives.get(this.mw2.vertices.get(0).get(0)));
    t.checkExpect(this.mw2.find(this.mw2.vertices.get(1).get(1)),
        this.mw2.representatives.get(this.mw2.vertices.get(1).get(1)));
    t.checkExpect(this.mw2.find(this.mw2.vertices.get(2).get(2)),
        this.mw2.representatives.get(this.mw2.vertices.get(2).get(2)));
    t.checkExpect(this.mw2.find(this.mw2.vertices.get(3).get(3)),
        this.mw2.representatives.get(this.mw2.vertices.get(3).get(3)));
    t.checkExpect(this.mw2.find(this.mw2.vertices.get(4).get(4)),
        this.mw2.representatives.get(this.mw2.vertices.get(4).get(4)));
    t.checkExpect(this.mw2.find(this.mw2.vertices.get(5).get(5)),
        this.mw2.representatives.get(this.mw2.vertices.get(5).get(5)));

    // test mw3
    // reset the representatives
    this.mw3.representatives = new HashMap<Vertex, Vertex>();
    for (ArrayList<Vertex> row : this.mw3.vertices) {
      for (Vertex v : row) {
        this.mw3.representatives.put(v, v);
      }
    }
    t.checkExpect(this.mw3.find(this.mw3.vertices.get(0).get(0)),
        this.mw3.representatives.get(this.mw3.vertices.get(0).get(0)));
    t.checkExpect(this.mw3.find(this.mw3.vertices.get(1).get(1)),
        this.mw3.representatives.get(this.mw3.vertices.get(1).get(1)));
    t.checkExpect(this.mw3.find(this.mw3.vertices.get(2).get(2)),
        this.mw3.representatives.get(this.mw3.vertices.get(2).get(2)));
    t.checkExpect(this.mw3.find(this.mw3.vertices.get(3).get(3)),
        this.mw3.representatives.get(this.mw3.vertices.get(3).get(3)));
    t.checkExpect(this.mw3.find(this.mw3.vertices.get(4).get(4)),
        this.mw3.representatives.get(this.mw3.vertices.get(4).get(4)));
    t.checkExpect(this.mw3.find(this.mw3.vertices.get(5).get(5)),
        this.mw3.representatives.get(this.mw3.vertices.get(5).get(5)));

    // test mw4
    // reset the representatives
    this.mw4.representatives = new HashMap<Vertex, Vertex>();
    for (ArrayList<Vertex> row : this.mw4.vertices) {
      for (Vertex v : row) {
        this.mw4.representatives.put(v, v);
      }
    }
    t.checkExpect(this.mw4.find(this.mw4.vertices.get(0).get(0)),
        this.mw4.representatives.get(this.mw4.vertices.get(0).get(0)));
    t.checkExpect(this.mw4.find(this.mw4.vertices.get(1).get(1)),
        this.mw4.representatives.get(this.mw4.vertices.get(1).get(1)));
    t.checkExpect(this.mw4.find(this.mw4.vertices.get(2).get(2)),
        this.mw4.representatives.get(this.mw4.vertices.get(2).get(2)));
    t.checkExpect(this.mw4.find(this.mw4.vertices.get(3).get(3)),
        this.mw4.representatives.get(this.mw4.vertices.get(3).get(3)));
    t.checkExpect(this.mw4.find(this.mw4.vertices.get(4).get(4)),
        this.mw4.representatives.get(this.mw4.vertices.get(4).get(4)));
    t.checkExpect(this.mw4.find(this.mw4.vertices.get(5).get(5)),
        this.mw4.representatives.get(this.mw4.vertices.get(5).get(5)));

    // test mw5
    // reset the representatives
    this.mw5.representatives = new HashMap<Vertex, Vertex>();
    for (ArrayList<Vertex> row : this.mw5.vertices) {
      for (Vertex v : row) {
        this.mw5.representatives.put(v, v);
      }
    }
    t.checkExpect(this.mw5.find(this.mw5.vertices.get(0).get(0)),
        this.mw5.representatives.get(this.mw5.vertices.get(0).get(0)));
    t.checkExpect(this.mw5.find(this.mw5.vertices.get(1).get(1)),
        this.mw5.representatives.get(this.mw5.vertices.get(1).get(1)));
    t.checkExpect(this.mw5.find(this.mw5.vertices.get(2).get(2)),
        this.mw5.representatives.get(this.mw5.vertices.get(2).get(2)));
    t.checkExpect(this.mw5.find(this.mw5.vertices.get(3).get(3)),
        this.mw5.representatives.get(this.mw5.vertices.get(3).get(3)));
    t.checkExpect(this.mw5.find(this.mw5.vertices.get(4).get(4)),
        this.mw5.representatives.get(this.mw5.vertices.get(4).get(4)));
    t.checkExpect(this.mw5.find(this.mw5.vertices.get(5).get(5)),
        this.mw5.representatives.get(this.mw5.vertices.get(5).get(5)));
  }

  // test generateWorklist method
  void testGenerateWorklist(Tester t) {
    this.initMazeWorlds();
    // test mw1
    t.checkExpect(this.mw1.generateWorklist().size(), 180);

    // test mw2
    t.checkExpect(this.mw2.generateWorklist().size(), 370);

    // test mw3
    t.checkExpect(this.mw3.generateWorklist().size(), 2920);

    // test mw4
    t.checkExpect(this.mw4.generateWorklist().size(), 4900);

    // test mw5
    t.checkExpect(this.mw5.generateWorklist().size(), 8270);
  }

  // test sortEdges method
  void testSortEdges(Tester t) {
    this.initMazeWorlds();
    this.initEdges();
    ArrayList<Edge> seq1 = new ArrayList<Edge>();
    seq1.add(this.e1);
    seq1.add(this.e2);
    seq1.add(this.e3);
    seq1.add(this.e4);
    seq1.add(this.e5);

    ArrayList<Edge> seq2 = new ArrayList<Edge>();
    seq2.add(this.e2);
    seq2.add(this.e1);
    seq2.add(this.e5);
    seq2.add(this.e4);
    seq2.add(this.e3);

    ArrayList<Edge> seq3 = new ArrayList<Edge>();
    seq3.add(this.e3);
    seq3.add(this.e4);
    seq3.add(this.e5);
    seq3.add(this.e1);
    seq3.add(this.e2);

    ArrayList<Edge> seq4 = new ArrayList<Edge>();
    seq4.add(this.e4);
    seq4.add(this.e3);
    seq4.add(this.e2);
    seq4.add(this.e1);
    seq4.add(this.e5);

    ArrayList<Edge> seq5 = new ArrayList<Edge>();
    seq5.add(this.e5);
    seq5.add(this.e4);
    seq5.add(this.e3);
    seq5.add(this.e2);
    seq5.add(this.e1);

    t.checkExpect(this.mw1.sortEdges(seq1), seq1);
    t.checkExpect(this.mw1.sortEdges(seq2), seq1);
    t.checkExpect(this.mw1.sortEdges(seq3), seq1);
    t.checkExpect(this.mw1.sortEdges(seq4), seq1);
    t.checkExpect(this.mw1.sortEdges(seq5), seq1);
  }

  // test moreThanOneTree method
  void testMoreThanOneTree(Tester t) {
    this.initMazeWorlds();
    // test mw1
    t.checkExpect(this.mw1.moreThanOneTree(), false);

    // test mw2
    t.checkExpect(this.mw2.moreThanOneTree(), false);

    // test mw3
    t.checkExpect(this.mw3.moreThanOneTree(), false);

    // test mw4
    t.checkExpect(this.mw4.moreThanOneTree(), false);

    // test mw5
    t.checkExpect(this.mw5.moreThanOneTree(), false);

    // test manipulated mw
    this.initVertices();
    HashMap<Vertex, Vertex> representativesTest = new HashMap<Vertex, Vertex>();
    representativesTest.put(this.v1, this.v1);
    representativesTest.put(this.v2, this.v1);
    representativesTest.put(this.v5, this.v1);
    this.mw1.representatives = representativesTest;
    t.checkExpect(this.mw1.moreThanOneTree(), false);

    representativesTest = new HashMap<Vertex, Vertex>();
    representativesTest.put(this.v1, this.v1);
    representativesTest.put(this.v2, this.v2);
    representativesTest.put(this.v5, this.v5);
    this.mw1.representatives = representativesTest;
    t.checkExpect(this.mw1.moreThanOneTree(), true);
  }

  // test drawEdge method
  void testDrawEdge(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    WorldScene testWS1 = this.mw1.getEmptyScene();
    WorldScene expectedWS1 = this.mw1.getEmptyScene();
    this.mw1.dfsVisited.add(this.v1);
    this.mw1.dfsVisited.add(this.v2);
    this.mw1.animatingDfs = true;
    mw1.drawEdge(e1, testWS1);
    Vertex v1 = (e1.source.x > e1.destination.x) ? e1.source : e1.destination;
    expectedWS1.placeImageXY(
        new RectangleImage(2, mw1.cellSize - 2, OutlineMode.SOLID, Color.CYAN),
        v1.x * mw1.cellSize,
        (v1.y * mw1.cellSize) + mw1.cellSize / 2);
    t.checkExpect(testWS1, expectedWS1);

    testWS1 = this.mw1.getEmptyScene();
    expectedWS1 = this.mw1.getEmptyScene();
    this.mw1.dfsPath.add(this.v1);
    mw1.drawEdge(e1, testWS1);
    expectedWS1.placeImageXY(
        new RectangleImage(2, mw1.cellSize - 2, OutlineMode.SOLID, Color.BLUE),
        v1.x * mw1.cellSize,
        (v1.y * mw1.cellSize) + mw1.cellSize / 2);
    t.checkExpect(testWS1, expectedWS1);

    testWS1 = this.mw1.getEmptyScene();
    expectedWS1 = this.mw1.getEmptyScene();
    this.mw1.animatingDfs = false;
    this.mw1.animatingBfs = true;
    this.mw1.bfsVisited.add(this.v1);
    this.mw1.bfsVisited.add(this.v2);
    mw1.drawEdge(e1, testWS1);
    expectedWS1.placeImageXY(
        new RectangleImage(2, mw1.cellSize - 2, OutlineMode.SOLID, Color.PINK),
        v1.x * mw1.cellSize,
        (v1.y * mw1.cellSize) + mw1.cellSize / 2);
    t.checkExpect(testWS1, expectedWS1);

    testWS1 = this.mw1.getEmptyScene();
    expectedWS1 = this.mw1.getEmptyScene();
    this.mw1.bfsPath.add(this.v1);
    mw1.drawEdge(e1, testWS1);
    expectedWS1.placeImageXY(
        new RectangleImage(2, mw1.cellSize - 2, OutlineMode.SOLID, Color.MAGENTA),
        v1.x * mw1.cellSize,
        (v1.y * mw1.cellSize) + mw1.cellSize / 2);
    t.checkExpect(testWS1, expectedWS1);

    // test mw2
    WorldScene testWS2 = this.mw2.getEmptyScene();
    WorldScene expectedWS2 = this.mw2.getEmptyScene();
    this.mw2.dfsVisited.add(this.v1);
    this.mw2.dfsVisited.add(this.v3);
    this.mw2.animatingDfs = true;
    mw2.drawEdge(e2, testWS2);
    Vertex v2 = (e2.source.y > e2.destination.y) ? e2.source : e2.destination;
    expectedWS2.placeImageXY(
        new RectangleImage(mw2.cellSize - 2, 2, OutlineMode.SOLID, Color.CYAN),
        (v2.x * mw2.cellSize) + mw2.cellSize / 2,
        v2.y * mw2.cellSize);
    t.checkExpect(testWS2, expectedWS2);

    testWS2 = this.mw2.getEmptyScene();
    expectedWS2 = this.mw2.getEmptyScene();
    this.mw2.dfsPath.add(this.v1);
    mw2.drawEdge(e2, testWS2);
    expectedWS2.placeImageXY(
        new RectangleImage(mw2.cellSize - 2, 2, OutlineMode.SOLID, Color.BLUE),
        (v2.x * mw2.cellSize) + mw2.cellSize / 2,
        v2.y * mw2.cellSize);
    t.checkExpect(testWS2, expectedWS2);

    testWS2 = this.mw2.getEmptyScene();
    expectedWS2 = this.mw2.getEmptyScene();
    this.mw2.animatingDfs = false;
    this.mw2.animatingBfs = true;
    this.mw2.bfsVisited.add(this.v1);
    this.mw2.bfsVisited.add(this.v3);
    mw2.drawEdge(e2, testWS2);
    expectedWS2.placeImageXY(
        new RectangleImage(mw2.cellSize - 2, 2, OutlineMode.SOLID, Color.PINK),
        (v2.x * mw2.cellSize) + mw2.cellSize / 2,
        v2.y * mw2.cellSize);
    t.checkExpect(testWS2, expectedWS2);

    testWS2 = this.mw2.getEmptyScene();
    expectedWS2 = this.mw2.getEmptyScene();
    this.mw2.bfsPath.add(this.v1);
    mw2.drawEdge(e2, testWS2);
    expectedWS2.placeImageXY(
        new RectangleImage(mw2.cellSize - 2, 2, OutlineMode.SOLID, Color.MAGENTA),
        (v2.x * mw2.cellSize) + mw2.cellSize / 2,
        v2.y * mw2.cellSize);
    t.checkExpect(testWS2, expectedWS2);


    // test mw3
    WorldScene testWS3 = this.mw3.getEmptyScene();
    WorldScene expectedWS3 = this.mw3.getEmptyScene();
    this.mw3.dfsVisited.add(this.v1);
    this.mw3.dfsVisited.add(this.v4);
    this.mw3.animatingDfs = true;
    mw3.drawEdge(e3, testWS3);
    Vertex v3 = (e3.source.y > e3.destination.y) ? e3.source : e3.destination;
    expectedWS3.placeImageXY(
        new RectangleImage(mw3.cellSize - 2, 2, OutlineMode.SOLID, Color.CYAN),
        (v3.x * mw3.cellSize) + mw3.cellSize / 2,
        v3.y * mw3.cellSize);
    t.checkExpect(testWS3, expectedWS3);

    testWS3 = this.mw3.getEmptyScene();
    expectedWS3 = this.mw3.getEmptyScene();
    this.mw3.dfsPath.add(this.v1);
    mw3.drawEdge(e3, testWS3);
    expectedWS3.placeImageXY(
        new RectangleImage(mw3.cellSize - 2, 2, OutlineMode.SOLID, Color.BLUE),
        (v3.x * mw3.cellSize) + mw3.cellSize / 2,
        v3.y * mw3.cellSize);
    t.checkExpect(testWS3, expectedWS3);

    testWS3 = this.mw3.getEmptyScene();
    expectedWS3 = this.mw3.getEmptyScene();
    this.mw3.animatingDfs = false;
    this.mw3.animatingBfs = true;
    this.mw3.bfsVisited.add(this.v1);
    this.mw3.bfsVisited.add(this.v4);
    mw3.drawEdge(e3, testWS3);
    expectedWS3.placeImageXY(
        new RectangleImage(mw3.cellSize - 2, 2, OutlineMode.SOLID, Color.PINK),
        (v3.x * mw3.cellSize) + mw3.cellSize / 2,
        v3.y * mw3.cellSize);
    t.checkExpect(testWS3, expectedWS3);

    testWS3 = this.mw3.getEmptyScene();
    expectedWS3 = this.mw3.getEmptyScene();
    this.mw3.bfsPath.add(this.v1);
    mw3.drawEdge(e3, testWS3);
    expectedWS3.placeImageXY(
        new RectangleImage(mw3.cellSize - 2, 2, OutlineMode.SOLID, Color.MAGENTA),
        (v3.x * mw3.cellSize) + mw3.cellSize / 2,
        v3.y * mw3.cellSize);
    t.checkExpect(testWS3, expectedWS3);

    // test mw4
    WorldScene testWS4 = this.mw4.getEmptyScene();
    WorldScene expectedWS4 = this.mw4.getEmptyScene();
    this.mw4.dfsVisited.add(this.v3);
    this.mw4.dfsVisited.add(this.v5);
    this.mw4.animatingDfs = true;
    mw4.drawEdge(e4, testWS4);
    Vertex v4 = (e4.source.x > e4.destination.x) ? e4.source : e4.destination;
    expectedWS4.placeImageXY(
        new RectangleImage(2, mw4.cellSize - 2, OutlineMode.SOLID, Color.CYAN),
        v4.x * mw4.cellSize,
        (v4.y * mw4.cellSize) + mw4.cellSize / 2);
    t.checkExpect(testWS4, expectedWS4);

    testWS4 = this.mw4.getEmptyScene();
    expectedWS4 = this.mw4.getEmptyScene();
    this.mw4.dfsPath.add(this.v3);
    mw4.drawEdge(e4, testWS4);
    expectedWS4.placeImageXY(
        new RectangleImage(2, mw4.cellSize - 2, OutlineMode.SOLID, Color.BLUE),
        v4.x * mw4.cellSize,
        (v4.y * mw4.cellSize) + mw4.cellSize / 2);
    t.checkExpect(testWS4, expectedWS4);

    testWS4 = this.mw4.getEmptyScene();
    expectedWS4 = this.mw4.getEmptyScene();
    this.mw4.animatingDfs = false;
    this.mw4.animatingBfs = true;
    this.mw4.bfsVisited.add(this.v3);
    this.mw4.bfsVisited.add(this.v5);
    mw4.drawEdge(e4, testWS4);
    expectedWS4.placeImageXY(
        new RectangleImage(2, mw4.cellSize - 2, OutlineMode.SOLID, Color.PINK),
        v4.x * mw4.cellSize,
        (v4.y * mw4.cellSize) + mw4.cellSize / 2);
    t.checkExpect(testWS4, expectedWS4);

    testWS4 = this.mw4.getEmptyScene();
    expectedWS4 = this.mw4.getEmptyScene();
    this.mw4.bfsPath.add(this.v3);
    mw4.drawEdge(e4, testWS4);
    expectedWS4.placeImageXY(
        new RectangleImage(2, mw4.cellSize - 2, OutlineMode.SOLID, Color.MAGENTA),
        v4.x * mw4.cellSize,
        (v4.y * mw4.cellSize) + mw4.cellSize / 2);
    t.checkExpect(testWS4, expectedWS4);

    // test mw5
    WorldScene testWS5 = this.mw5.getEmptyScene();
    WorldScene expectedWS5 = this.mw5.getEmptyScene();
    this.mw5.drawEdge(e5, testWS5);
    Vertex v5 = (e5.source.x > e5.destination.x) ? e5.source : e5.destination;
    expectedWS5.placeImageXY(
        new RectangleImage(2, mw5.cellSize - 2, OutlineMode.SOLID, Color.WHITE),
        v5.x * mw5.cellSize,
        (v5.y * mw5.cellSize) + mw5.cellSize / 2);
    t.checkExpect(testWS5, expectedWS5);
  }

  // test containsCycle method
  void testContainsCycle(Tester t) {
    this.initMazeWorlds();
    this.initVertices();

    // test mw1
    this.mw1.representatives = new HashMap<Vertex, Vertex>();
    this.mw1.representatives.put(this.v1, this.v1);
    t.checkExpect(this.mw1.containsCycle(this.v1, this.v1), false);

    // test mw2
    this.mw2.representatives = new HashMap<Vertex, Vertex>();
    this.mw2.representatives.put(this.v1, this.v2);
    this.mw2.representatives.put(this.v2, this.v2);
    t.checkExpect(this.mw2.containsCycle(this.v1, this.v2), false);

    // test mw3
    this.mw3.representatives = new HashMap<Vertex, Vertex>();
    this.mw3.representatives.put(this.v1, this.v2);
    this.mw3.representatives.put(this.v2, this.v3);
    this.mw3.representatives.put(this.v3, this.v1);
    t.checkExpect(this.mw3.containsCycle(this.v1, this.v2), true);

    // test mw4
    this.mw4.representatives = new HashMap<Vertex, Vertex>();
    this.mw4.representatives.put(this.v1, this.v2);
    this.mw4.representatives.put(this.v2, this.v3);
    this.mw4.representatives.put(this.v3, this.v4);
    this.mw4.representatives.put(this.v4, this.v4);
    t.checkExpect(this.mw4.containsCycle(this.v1, this.v2), false);

    // test mw5
    this.mw5.representatives = new HashMap<Vertex, Vertex>();
    this.mw5.representatives.put(this.v1, this.v2);
    this.mw5.representatives.put(this.v2, this.v3);
    this.mw5.representatives.put(this.v3, this.v4);
    this.mw5.representatives.put(this.v4, this.v5);
    this.mw5.representatives.put(this.v5, this.v1);
    t.checkExpect(this.mw5.containsCycle(this.v1, this.v3), true);
  }

  // test dfs method
  void testDfs(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    this.mw1.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw1.dfsVisited = new ArrayList<Vertex>();
    this.mw1.dfsWorklist = new Stack<Vertex>();
    this.mw1.dfsWorklist.add(this.v1);
    this.mw1.vertices.get(mw1.width - 1).set(mw1.height - 1, v1);
    this.mw1.solvingDfs = true;
    this.mw1.dfs();
    t.checkExpect(this.mw1.solvingDfs, false);
    t.checkExpect(this.mw1.reconstructDfs, true);
    t.checkExpect(this.mw1.dfsWorklist.size(), 0);
    t.checkExpect(this.mw1.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    // test mw2
    this.mw2.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw2.dfsVisited = new ArrayList<Vertex>();
    this.mw2.dfsWorklist = new Stack<Vertex>();
    this.mw2.dfsWorklist.add(this.v1);
    this.mw2.vertices.get(mw2.width - 1).set(mw2.height - 1, v2);
    this.mw2.dfs();
    t.checkExpect(this.mw2.dfsWorklist.size(), 2);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v3), true);
    t.checkExpect(this.mw2.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));


    this.mw2.dfs();
    t.checkExpect(this.mw2.dfsWorklist.size(), 3);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v1), true);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v4), true);
    t.checkExpect(this.mw2.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1, this.v3)));

    // test mw3
    this.mw3.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw3.dfsVisited = new ArrayList<Vertex>();
    this.mw3.dfsWorklist = new Stack<Vertex>();
    this.mw3.dfsWorklist.add(this.v1);
    this.mw3.vertices.get(mw3.width - 1).set(mw3.height - 1, v3);
    this.mw3.dfs();
    t.checkExpect(this.mw3.dfsWorklist.size(), 2);
    t.checkExpect(this.mw3.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw3.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw3.dfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw3.cameFromEdgeDfs.containsKey(this.v3), true);
    t.checkExpect(this.mw3.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    this.mw3.solvingDfs = true;
    this.mw3.dfs();
    t.checkExpect(this.mw3.solvingDfs, false);
    t.checkExpect(this.mw3.reconstructDfs, true);
    t.checkExpect(this.mw3.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1, this.v3)));

    // test mw4
    this.mw4.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw4.dfsVisited = new ArrayList<Vertex>(Arrays.asList(this.v4));
    this.mw4.dfsWorklist = new Stack<Vertex>();
    this.mw4.dfsWorklist.add(this.v3);
    this.mw4.vertices.get(mw4.width - 1).set(mw4.height - 1, v1);
    this.mw4.dfs();
    t.checkExpect(this.mw4.dfsWorklist.size(), 2);
    t.checkExpect(this.mw4.dfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw4.cameFromEdgeDfs.containsKey(this.v1), true);
    t.checkExpect(this.mw4.dfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw4.cameFromEdgeDfs.containsKey(this.v4), true);
    t.checkExpect(this.mw4.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v4, this.v3)));

    this.mw4.dfs();
    t.checkExpect(this.mw4.dfsWorklist.size(), 1);
    t.checkExpect(this.mw4.dfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw4.cameFromEdgeDfs.containsKey(this.v1), true);
    t.checkExpect(this.mw4.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v4, this.v3)));

    this.mw4.solvingDfs = true;
    this.mw4.dfs();
    t.checkExpect(this.mw4.dfsWorklist.size(), 0);
    t.checkExpect(this.mw4.solvingDfs, false);
    t.checkExpect(this.mw4.reconstructDfs, true);
    t.checkExpect(this.mw4.dfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v4, this.v3, this.v1)));

    // test mw5
    this.mw5.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw5.dfsVisited = new ArrayList<Vertex>();
    this.mw5.dfsWorklist = new Stack<Vertex>();
    this.mw5.dfsWorklist.add(this.v1);
    this.mw5.vertices.get(mw5.width - 1).set(mw5.height - 1, v5);
    this.mw5.dfs();
    t.checkExpect(this.mw5.dfsWorklist.size(), 2);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v3), true);
    t.checkExpect(this.mw5.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    this.mw5.dfs();
    t.checkExpect(this.mw5.dfsWorklist.size(), 3);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v1), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v4), true);
    t.checkExpect(this.mw5.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1, this.v3)));

    this.mw5.dfs();
    t.checkExpect(this.mw5.dfsWorklist.size(), 5);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v1), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v3), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v5), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v5), true);
    t.checkExpect(this.mw5.dfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v1, this.v3, this.v4)));

    this.mw5.dfs();
    t.checkExpect(this.mw5.dfsWorklist.size(), 4);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v1), true);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw5.dfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v1, this.v3, this.v4, this.v5)));

    this.mw5.dfs();
    t.checkExpect(this.mw5.dfsWorklist.size(), 3);
    t.checkExpect(this.mw5.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw5.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw5.dfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v1, this.v3, this.v4, this.v5)));
  }

  // test the bfs method
  void testBfs(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    this.mw1.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw1.bfsVisited = new ArrayList<Vertex>();
    this.mw1.bfsWorklist = new LinkedList<Vertex>();
    this.mw1.bfsWorklist.add(this.v1);
    this.mw1.vertices.get(mw1.width - 1).set(mw1.height - 1, v1);
    this.mw1.solvingBfs = true;
    this.mw1.bfs();
    t.checkExpect(this.mw1.solvingBfs, false);
    t.checkExpect(this.mw1.reconstructBfs, true);
    t.checkExpect(this.mw1.bfsWorklist.size(), 0);
    t.checkExpect(this.mw1.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));


    // test mw2
    this.mw2.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw2.bfsVisited = new ArrayList<Vertex>();
    this.mw2.bfsWorklist = new LinkedList<Vertex>();
    this.mw2.bfsWorklist.add(this.v1);
    this.mw2.vertices.get(mw2.width - 1).set(mw2.height - 1, v2);
    this.mw2.bfs();
    t.checkExpect(this.mw2.bfsWorklist.size(), 2);
    t.checkExpect(this.mw2.bfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw2.cameFromEdgeBfs.containsKey(this.v2), true);
    t.checkExpect(this.mw2.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw2.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw2.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    this.mw2.solvingBfs = true;
    this.mw2.bfs();
    t.checkExpect(this.mw2.bfsWorklist.size(), 1);
    t.checkExpect(this.mw2.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw2.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw2.solvingDfs, false);
    t.checkExpect(this.mw2.reconstructBfs, true);
    t.checkExpect(this.mw2.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2)));


    // test mw3
    this.mw3.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw3.bfsVisited = new ArrayList<Vertex>();
    this.mw3.bfsWorklist = new LinkedList<Vertex>();
    this.mw3.bfsWorklist.add(this.v1);
    this.mw3.vertices.get(mw3.width - 1).set(mw3.height - 1, v3);
    this.mw3.bfs();
    t.checkExpect(this.mw3.bfsWorklist.size(), 2);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v2), true);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw3.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    this.mw3.bfs();
    t.checkExpect(this.mw3.bfsWorklist.size(), 3);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v1), true);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v4), true);
    t.checkExpect(this.mw3.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2)));


    // test mw4
    this.mw4.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw4.bfsVisited = new ArrayList<Vertex>();
    this.mw4.bfsWorklist = new LinkedList<Vertex>();
    this.mw4.bfsWorklist.add(this.v1);
    this.mw4.vertices.get(mw4.width - 1).set(mw4.height - 1, v4);
    this.mw4.bfs();
    t.checkExpect(this.mw4.bfsWorklist.size(), 2);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v2), true);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw4.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    this.mw4.bfs();
    t.checkExpect(this.mw4.bfsWorklist.size(), 3);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v1), true);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v4), true);
    t.checkExpect(this.mw4.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2)));

    this.mw4.bfs();
    t.checkExpect(this.mw4.bfsWorklist.size(), 4);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v1), true);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v4), true);
    t.checkExpect(this.mw4.bfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2, this.v3)));

    this.mw4.bfs();
    t.checkExpect(this.mw4.bfsWorklist.size(), 3);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v1), true);
    t.checkExpect(this.mw4.bfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw4.cameFromEdgeBfs.containsKey(this.v4), true);
    t.checkExpect(this.mw4.bfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2, this.v3)));

    this.mw4.solvingBfs = true;
    this.mw4.bfs();
    t.checkExpect(this.mw4.bfsWorklist.size(), 2);
    t.checkExpect(this.mw4.solvingBfs, false);
    t.checkExpect(this.mw4.reconstructBfs, true);
    t.checkExpect(this.mw4.bfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v1, this.v2, this.v3, this.v4)));


    // test mw5
    this.mw5.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw5.bfsVisited = new ArrayList<Vertex>();
    this.mw5.bfsWorklist = new LinkedList<Vertex>();
    this.mw5.bfsWorklist.add(this.v4);
    this.mw5.vertices.get(mw5.width - 1).set(mw5.height - 1, v3);
    this.mw5.bfs();
    t.checkExpect(this.mw5.bfsWorklist.size(), 3);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v2), true);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v5), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v5), true);
    t.checkExpect(this.mw5.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v4)));

    this.mw5.bfs();
    t.checkExpect(this.mw5.bfsWorklist.size(), 4);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v1), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v1), true);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v4), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v4), true);
    t.checkExpect(this.mw5.bfsWorklist.contains(this.v5), true);
    t.checkExpect(this.mw5.cameFromEdgeBfs.containsKey(this.v5), true);
    t.checkExpect(this.mw5.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v4, this.v2)));

    this.mw5.solvingBfs = true;
    this.mw5.bfs();
    t.checkExpect(this.mw5.bfsWorklist.size(), 3);
    t.checkExpect(this.mw5.solvingBfs, false);
    t.checkExpect(this.mw5.reconstructBfs, true);
    t.checkExpect(this.mw5.bfsVisited,
        new ArrayList<Vertex>(Arrays.asList(this.v4, this.v2, this.v3)));
  }

  // test the reconstructDfs method
  void testReconstructDfs(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    this.mw1.reconstructDfs = true;
    this.mw1.finalStateDfs = false;
    this.mw1.vertices.get(0).set(0, v1);
    this.mw1.reconstructDfsPath(v1);
    t.checkExpect(this.mw1.reconstructDfs, false);
    t.checkExpect(this.mw1.finalStateDfs, true);

    // test mw2
    this.mw2.vertices.get(0).set(0, v2);
    this.mw2.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    this.mw2.cameFromEdgeDfs.put(v1, e1);
    this.mw2.dfsPath = new ArrayList<Vertex>();
    this.mw2.reconstructDfsPath(v1);
    t.checkExpect(this.mw2.dfsPath, new ArrayList<Vertex>(Arrays.asList(v2)));

    // test mw3
    this.mw3.vertices.get(0).set(0, v3);
    this.mw3.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    this.mw3.cameFromEdgeDfs.put(v1, e1);
    this.mw3.cameFromEdgeDfs.put(v2, e3);
    this.mw3.dfsPath = new ArrayList<Vertex>();
    this.mw3.reconstructDfsPath(v1);
    t.checkExpect(this.mw3.dfsPath, new ArrayList<Vertex>(Arrays.asList(v2)));
    this.mw3.reconstructDfsPath(v2);
    t.checkExpect(this.mw3.dfsPath, new ArrayList<Vertex>(Arrays.asList(v2, v4)));

    // test mw4
    this.mw4.reconstructDfs = true;
    this.mw4.finalStateDfs = false;
    this.mw4.vertices.get(0).set(0, v5);
    this.mw4.reconstructDfsPath(v5);
    t.checkExpect(this.mw4.reconstructDfs, false);
    t.checkExpect(this.mw4.finalStateDfs, true);

    // test mw5
    this.mw5.vertices.get(0).set(0, v5);
    this.mw5.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    this.mw5.cameFromEdgeDfs.put(v2, e3);
    this.mw5.cameFromEdgeDfs.put(v4, e5);
    this.mw5.dfsPath = new ArrayList<Vertex>();
    this.mw5.reconstructDfsPath(v2);
    t.checkExpect(this.mw5.dfsPath, new ArrayList<Vertex>(Arrays.asList(v4)));
    this.mw5.reconstructDfsPath(v4);
    t.checkExpect(this.mw5.dfsPath, new ArrayList<Vertex>(Arrays.asList(v4, v5)));
    this.mw5.reconstructDfs = true;
    this.mw5.finalStateDfs = false;
    this.mw5.reconstructDfsPath(v5);
    t.checkExpect(this.mw5.reconstructDfs, false);
    t.checkExpect(this.mw5.finalStateDfs, true);
  }

  // test the reconstructBfs method
  void testReconstructBfs(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    this.mw1.reconstructBfs = true;
    this.mw1.finalStateBfs = false;
    this.mw1.vertices.get(0).set(0, v1);
    this.mw1.reconstructBfsPath(v1);
    t.checkExpect(this.mw1.reconstructBfs, false);
    t.checkExpect(this.mw1.finalStateBfs, true);

    // test mw2
    this.mw2.vertices.get(0).set(0, v2);
    this.mw2.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    this.mw2.cameFromEdgeBfs.put(v1, e1);
    this.mw2.bfsPath = new ArrayList<Vertex>();
    this.mw2.reconstructBfsPath(v1);
    t.checkExpect(this.mw2.bfsPath, new ArrayList<Vertex>(Arrays.asList(v2)));

    // test mw3
    this.mw3.vertices.get(0).set(0, v3);
    this.mw3.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    this.mw3.cameFromEdgeBfs.put(v1, e1);
    this.mw3.cameFromEdgeBfs.put(v2, e3);
    this.mw3.bfsPath = new ArrayList<Vertex>();
    this.mw3.reconstructBfsPath(v1);
    t.checkExpect(this.mw3.bfsPath, new ArrayList<Vertex>(Arrays.asList(v2)));
    this.mw3.reconstructBfsPath(v2);
    t.checkExpect(this.mw3.bfsPath, new ArrayList<Vertex>(Arrays.asList(v2, v4)));

    // test mw4
    this.mw4.reconstructBfs = true;
    this.mw4.finalStateBfs = false;
    this.mw4.vertices.get(0).set(0, v5);
    this.mw4.reconstructBfsPath(v5);
    t.checkExpect(this.mw4.reconstructBfs, false);
    t.checkExpect(this.mw4.finalStateBfs, true);

    // test mw5
    this.mw5.vertices.get(0).set(0, v5);
    this.mw5.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    this.mw5.cameFromEdgeBfs.put(v2, e3);
    this.mw5.cameFromEdgeBfs.put(v4, e5);
    this.mw5.bfsPath = new ArrayList<Vertex>();
    this.mw5.reconstructBfsPath(v2);
    t.checkExpect(this.mw5.bfsPath, new ArrayList<Vertex>(Arrays.asList(v4)));
    this.mw5.reconstructBfsPath(v4);
    t.checkExpect(this.mw5.bfsPath, new ArrayList<Vertex>(Arrays.asList(v4, v5)));
    this.mw5.reconstructBfs = true;
    this.mw5.finalStateBfs = false;
    this.mw5.reconstructBfsPath(v5);
    t.checkExpect(this.mw5.reconstructBfs, false);
    t.checkExpect(this.mw5.finalStateBfs, true);
  }

  // test reconstructPlayerPath method
  void testReconstructPlayerPath(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    this.mw1.playerReconstruct = true;
    this.mw1.finalStatePlayer = false;
    this.mw1.playerPath = new ArrayList<Vertex>();
    this.mw1.reconstructPlayerPath();
    t.checkExpect(this.mw1.playerReconstruct, false);
    t.checkExpect(this.mw1.finalStatePlayer, true);

    // test mw2
    this.mw2.playerPath = new ArrayList<Vertex>(Arrays.asList(v1));
    this.mw2.playerPathAnimator = new ArrayList<Vertex>();
    this.mw2.reconstructPlayerPath();
    t.checkExpect(this.mw2.playerPathAnimator, new ArrayList<Vertex>(Arrays.asList(v1)));
    t.checkExpect(this.mw2.playerPath, new ArrayList<Vertex>());

    // test mw3
    this.mw3.playerReconstruct = true;
    this.mw3.finalStatePlayer = false;
    this.mw3.playerPath = new ArrayList<Vertex>();
    this.mw3.reconstructPlayerPath();
    t.checkExpect(this.mw3.playerReconstruct, false);
    t.checkExpect(this.mw3.finalStatePlayer, true);

    // test mw4
    this.mw4.playerPath = new ArrayList<Vertex>(Arrays.asList(v1, v2));
    this.mw4.playerPathAnimator = new ArrayList<Vertex>();
    this.mw4.reconstructPlayerPath();
    t.checkExpect(this.mw4.playerPathAnimator, new ArrayList<Vertex>(Arrays.asList(v2)));
    t.checkExpect(this.mw4.playerPath, new ArrayList<Vertex>(Arrays.asList(v1)));

    // test mw5
    this.mw5.playerPath = new ArrayList<Vertex>(Arrays.asList(v3, v1, v2));
    this.mw5.playerPathAnimator = new ArrayList<Vertex>();
    this.mw5.reconstructPlayerPath();
    t.checkExpect(this.mw5.playerPathAnimator, new ArrayList<Vertex>(Arrays.asList(v2)));
    t.checkExpect(this.mw5.playerPath, new ArrayList<Vertex>(Arrays.asList(v3, v1)));
  }

  // test the moveUp method
  void testMoveUp(Tester t) {
    this.initMazeWorlds();

    Vertex v1 = new Vertex(0, 0);
    Vertex v2 = new Vertex(0, 1);
    Vertex v3 = new Vertex(1, 0);
    Vertex v4 = new Vertex(1, 1);

    Edge e1 = new Edge(v1, v2, 1);
    Edge e2 = new Edge(v1, v3, 1);
    Edge e3 = new Edge(v2, v4, 1);
    Edge e4 = new Edge(v3, v4, 1);

    // test mw1
    this.mw1.width = 2;
    this.mw1.height = 2;
    this.mw1.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw1.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw1.playerPath = new ArrayList<Vertex>();
    this.mw1.player = v4;
    this.mw1.moveUp();
    t.checkExpect(this.mw1.player, v3);
    t.checkExpect(this.mw1.playerPath, new ArrayList<Vertex>(Arrays.asList(v3)));

    // test mw2
    this.mw2.width = 2;
    this.mw2.height = 2;
    this.mw2.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw2.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw2.playerPath = new ArrayList<Vertex>();
    this.mw2.player = v2;
    this.mw2.moveUp();
    t.checkExpect(this.mw2.player, v1);
    t.checkExpect(this.mw2.playerPath, new ArrayList<Vertex>(Arrays.asList(v1)));

    // test mw3
    this.mw3.width = 2;
    this.mw3.height = 2;
    this.mw3.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw3.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw3.playerPath = new ArrayList<Vertex>();
    this.mw3.player = v1;
    this.mw3.moveUp();
    t.checkExpect(this.mw3.player, v1);
    t.checkExpect(this.mw3.playerPath, new ArrayList<Vertex>());

    // test mw4
    this.mw4.width = 2;
    this.mw4.height = 2;
    this.mw4.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw4.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw4.playerPath = new ArrayList<Vertex>();
    this.mw4.player = v3;
    this.mw4.moveUp();
    t.checkExpect(this.mw4.player, v3);
    t.checkExpect(this.mw4.playerPath, new ArrayList<Vertex>());
  }

  // test the moveDown method
  void testMoveDown(Tester t) {
    this.initMazeWorlds();

    Vertex v1 = new Vertex(0, 0);
    Vertex v2 = new Vertex(0, 1);
    Vertex v3 = new Vertex(1, 0);
    Vertex v4 = new Vertex(1, 1);

    Edge e1 = new Edge(v1, v2, 1);
    Edge e2 = new Edge(v1, v3, 1);
    Edge e3 = new Edge(v2, v4, 1);
    Edge e4 = new Edge(v3, v4, 1);

    // test mw1
    this.mw1.width = 2;
    this.mw1.height = 2;
    this.mw1.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw1.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw1.playerPath = new ArrayList<Vertex>();
    this.mw1.player = v4;
    this.mw1.moveDown();
    t.checkExpect(this.mw1.player, v4);
    t.checkExpect(this.mw1.playerPath, new ArrayList<Vertex>());

    // test mw2
    this.mw2.width = 2;
    this.mw2.height = 2;
    this.mw2.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw2.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw2.playerPath = new ArrayList<Vertex>();
    this.mw2.player = v2;
    this.mw2.moveDown();
    t.checkExpect(this.mw2.player, v2);
    t.checkExpect(this.mw2.playerPath, new ArrayList<Vertex>());

    // test mw3
    this.mw3.width = 2;
    this.mw3.height = 2;
    this.mw3.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw3.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw3.playerPath = new ArrayList<Vertex>();
    this.mw3.player = v1;
    this.mw3.moveDown();
    t.checkExpect(this.mw3.player, v2);
    t.checkExpect(this.mw3.playerPath, new ArrayList<Vertex>(Arrays.asList(v2)));

    // test mw4
    this.mw4.width = 2;
    this.mw4.height = 2;
    this.mw4.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw4.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw4.playerPath = new ArrayList<Vertex>();
    this.mw4.player = v3;
    this.mw4.moveDown();
    t.checkExpect(this.mw4.player, v4);
    t.checkExpect(this.mw4.playerPath, new ArrayList<Vertex>(Arrays.asList(v4)));
  }

  // test the moveLeft method
  void testMoveLeft(Tester t) {
    this.initMazeWorlds();

    Vertex v1 = new Vertex(0, 0);
    Vertex v2 = new Vertex(0, 1);
    Vertex v3 = new Vertex(1, 0);
    Vertex v4 = new Vertex(1, 1);

    Edge e1 = new Edge(v1, v2, 1);
    Edge e2 = new Edge(v1, v3, 1);
    Edge e3 = new Edge(v2, v4, 1);
    Edge e4 = new Edge(v3, v4, 1);

    // test mw1
    this.mw1.width = 2;
    this.mw1.height = 2;
    this.mw1.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw1.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw1.playerPath = new ArrayList<Vertex>();
    this.mw1.player = v4;
    this.mw1.moveLeft();
    t.checkExpect(this.mw1.player, v2);
    t.checkExpect(this.mw1.playerPath, new ArrayList<Vertex>(Arrays.asList(v2)));

    // test mw2
    this.mw2.width = 2;
    this.mw2.height = 2;
    this.mw2.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw2.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw2.playerPath = new ArrayList<Vertex>();
    this.mw2.player = v2;
    this.mw2.moveLeft();
    t.checkExpect(this.mw2.player, v2);
    t.checkExpect(this.mw2.playerPath, new ArrayList<Vertex>());

    // test mw3
    this.mw3.width = 2;
    this.mw3.height = 2;
    this.mw3.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw3.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw3.playerPath = new ArrayList<Vertex>();
    this.mw3.player = v1;
    this.mw3.moveLeft();
    t.checkExpect(this.mw3.player, v1);
    t.checkExpect(this.mw3.playerPath, new ArrayList<Vertex>());

    // test mw4
    this.mw4.width = 2;
    this.mw4.height = 2;
    this.mw4.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw4.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw4.playerPath = new ArrayList<Vertex>();
    this.mw4.player = v3;
    this.mw4.moveLeft();
    t.checkExpect(this.mw4.player, v1);
    t.checkExpect(this.mw4.playerPath, new ArrayList<Vertex>(Arrays.asList(v1)));
  }

  // test the moveRight method
  void testMoveRight(Tester t) {
    this.initMazeWorlds();

    Vertex v1 = new Vertex(0, 0);
    Vertex v2 = new Vertex(0, 1);
    Vertex v3 = new Vertex(1, 0);
    Vertex v4 = new Vertex(1, 1);

    Edge e1 = new Edge(v1, v2, 1);
    Edge e2 = new Edge(v1, v3, 1);
    Edge e3 = new Edge(v2, v4, 1);
    Edge e4 = new Edge(v3, v4, 1);

    // test mw1
    this.mw1.width = 2;
    this.mw1.height = 2;
    this.mw1.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw1.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw1.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw1.playerPath = new ArrayList<Vertex>();
    this.mw1.player = v4;
    this.mw1.moveRight();
    t.checkExpect(this.mw1.player, v4);
    t.checkExpect(this.mw1.playerPath, new ArrayList<Vertex>());

    // test mw2
    this.mw2.width = 2;
    this.mw2.height = 2;
    this.mw2.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw2.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw2.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw2.playerPath = new ArrayList<Vertex>();
    this.mw2.player = v2;
    this.mw2.moveRight();
    t.checkExpect(this.mw2.player, v4);
    t.checkExpect(this.mw2.playerPath, new ArrayList<Vertex>(Arrays.asList(v4)));

    // test mw3
    this.mw3.width = 2;
    this.mw3.height = 2;
    this.mw3.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw3.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw3.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw3.playerPath = new ArrayList<Vertex>();
    this.mw3.player = v1;
    this.mw3.moveRight();
    t.checkExpect(this.mw3.player, v3);
    t.checkExpect(this.mw3.playerPath, new ArrayList<Vertex>(Arrays.asList(v3)));

    // test mw4
    this.mw4.width = 2;
    this.mw4.height = 2;
    this.mw4.edges = new ArrayList<Edge>(Arrays.asList(e1, e2, e3, e4));
    this.mw4.vertices = new ArrayList<ArrayList<Vertex>>();
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v1, v2)));
    this.mw4.vertices.add(new ArrayList<Vertex>(Arrays.asList(v3, v4)));
    this.mw4.playerPath = new ArrayList<Vertex>();
    this.mw4.player = v3;
    this.mw4.moveRight();
    t.checkExpect(this.mw4.player, v3);
    t.checkExpect(this.mw4.playerPath, new ArrayList<Vertex>());
  }

  // test the reset method
  void testReset(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    mw1.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    mw1.cameFromEdgeDfs.put(v1, e1);
    mw1.cameFromEdgeDfs.put(v2, e2);
    mw1.cameFromEdgeDfs.put(v3, e3);
    mw1.cameFromEdgeDfs.put(v4, e4);
    mw1.cameFromEdgeDfs.put(v5, e5);

    mw1.dfsPath = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw1.dfsVisited = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw1.dfsWorklist = new Stack<Vertex>();
    mw1.dfsWorklist.push(v1);
    mw1.dfsWorklist.push(v2);
    mw1.dfsWorklist.push(v3);
    mw1.dfsWorklist.push(v4);
    mw1.dfsWorklist.push(v5);

    mw1.animatingDfs = true;
    mw1.reconstructDfs = true;
    mw1.finalStateDfs = true;
    mw1.solvingDfs = true;

    mw1.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    mw1.cameFromEdgeBfs.put(v1, e1);
    mw1.cameFromEdgeBfs.put(v2, e2);
    mw1.cameFromEdgeBfs.put(v3, e3);
    mw1.cameFromEdgeBfs.put(v4, e4);
    mw1.cameFromEdgeBfs.put(v5, e5);

    mw1.bfsPath = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw1.bfsVisited = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw1.bfsWorklist = new LinkedList<Vertex>();
    mw1.bfsWorklist.add(v1);
    mw1.bfsWorklist.add(v2);
    mw1.bfsWorklist.add(v3);
    mw1.bfsWorklist.add(v4);
    mw1.bfsWorklist.add(v5);

    mw1.animatingBfs = true;
    mw1.reconstructBfs = true;
    mw1.finalStateBfs = true;
    mw1.solvingBfs = true;

    mw1.reset();

    t.checkExpect(mw1.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw1.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw1.vertices.get(mw1.width - 1).get(mw1.height - 1))));
    t.checkExpect(mw1.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw1.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw1.animatingDfs, false);
    t.checkExpect(mw1.reconstructDfs, false);
    t.checkExpect(mw1.finalStateDfs, false);
    t.checkExpect(mw1.solvingDfs, false);
    t.checkExpect(mw1.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw1.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw1.vertices.get(mw1.width - 1).get(mw1.height - 1))));
    t.checkExpect(mw1.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw1.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw1.animatingBfs, false);
    t.checkExpect(mw1.reconstructBfs, false);
    t.checkExpect(mw1.finalStateBfs, false);
    t.checkExpect(mw1.solvingBfs, false);
  }

  // test the resetPlayer method
  void testResetPlayer(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    mw1.playerPathAnimator = new ArrayList<Vertex>();
    mw1.playerPath = new ArrayList<Vertex>();
    mw1.player = v3;
    mw1.playerReconstruct = true;
    mw1.finalStatePlayer = true;
    mw1.manualGameplay = true;
    mw1.playerWon = true;

    mw1.resetPlayer();

    t.checkExpect(mw1.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw1.vertices.get(mw1.width - 1).get(mw1.height - 1))));
    t.checkExpect(mw1.playerPath, new ArrayList<Vertex>(Arrays.asList(mw1.player)));
    t.checkExpect(mw1.player, mw1.vertices.get(0).get(0));
    t.checkExpect(mw1.playerReconstruct, false);
    t.checkExpect(mw1.finalStatePlayer, false);
    t.checkExpect(mw1.manualGameplay, false);
    t.checkExpect(mw1.playerWon, false);
  }

  // test onKeyEvent method
  void testOnKeyEvent(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw1
    mw1.animatingBfs = true;
    mw1.onKeyEvent("d");
    t.checkExpect(mw1.animatingBfs, false);
    t.checkExpect(mw1.animatingDfs, true);

    // test mw2
    mw2.animatingDfs = true;
    mw2.onKeyEvent("b");
    t.checkExpect(mw2.animatingDfs, false);
    t.checkExpect(mw2.animatingBfs, true);

    // test mw3
    mw3.animatingBfs = false;
    mw3.animatingDfs = false;
    mw3.onKeyEvent("m");
    t.checkExpect(mw3.manualGameplay, true);

    // test mw4
    mw4.playerPathAnimator = new ArrayList<Vertex>();
    mw4.playerPath = new ArrayList<Vertex>();
    mw4.player = v3;
    mw4.playerReconstruct = true;
    mw4.finalStatePlayer = true;
    mw4.manualGameplay = true;
    mw4.playerWon = true;
    mw4.onKeyEvent("escape");
    t.checkExpect(mw4.playerPathAnimator,
        new ArrayList<Vertex>(Arrays.asList(mw4.vertices.get(mw4.width - 1).get(mw4.height - 1))));
    t.checkExpect(mw4.playerPath, new ArrayList<Vertex>(Arrays.asList(mw4.player)));
    t.checkExpect(mw4.player, mw4.vertices.get(0).get(0));
    t.checkExpect(mw4.playerReconstruct, false);
    t.checkExpect(mw4.finalStatePlayer, false);
    t.checkExpect(mw4.manualGameplay, false);
    t.checkExpect(mw4.playerWon, false);

    // test mw5
    mw5.cameFromEdgeDfs = new HashMap<Vertex, Edge>();
    mw5.cameFromEdgeDfs.put(v1, e1);
    mw5.cameFromEdgeDfs.put(v2, e2);
    mw5.cameFromEdgeDfs.put(v3, e3);
    mw5.cameFromEdgeDfs.put(v4, e4);
    mw5.cameFromEdgeDfs.put(v5, e5);

    mw5.dfsPath = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw5.dfsVisited = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw5.dfsWorklist = new Stack<Vertex>();
    mw5.dfsWorklist.push(v1);
    mw5.dfsWorklist.push(v2);
    mw5.dfsWorklist.push(v3);
    mw5.dfsWorklist.push(v4);
    mw5.dfsWorklist.push(v5);

    mw5.animatingDfs = true;
    mw5.reconstructDfs = true;
    mw5.finalStateDfs = true;
    mw5.solvingDfs = true;

    mw5.cameFromEdgeBfs = new HashMap<Vertex, Edge>();
    mw5.cameFromEdgeBfs.put(v1, e1);
    mw5.cameFromEdgeBfs.put(v2, e2);
    mw5.cameFromEdgeBfs.put(v3, e3);
    mw5.cameFromEdgeBfs.put(v4, e4);
    mw5.cameFromEdgeBfs.put(v5, e5);

    mw5.bfsPath = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw5.bfsVisited = new ArrayList<Vertex>(Arrays.asList(v1, v2, v3, v4, v5));

    mw5.bfsWorklist = new LinkedList<Vertex>();
    mw5.bfsWorklist.add(v1);
    mw5.bfsWorklist.add(v2);
    mw5.bfsWorklist.add(v3);
    mw5.bfsWorklist.add(v4);
    mw5.bfsWorklist.add(v5);

    mw5.animatingBfs = true;
    mw5.reconstructBfs = true;
    mw5.finalStateBfs = true;
    mw5.solvingBfs = true;

    mw5.onKeyEvent("r");

    t.checkExpect(mw5.cameFromEdgeDfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw5.dfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw5.vertices.get(mw5.width - 1).get(mw5.height - 1))));
    t.checkExpect(mw5.dfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw5.dfsWorklist, new Stack<Vertex>());
    t.checkExpect(mw5.animatingDfs, false);
    t.checkExpect(mw5.reconstructDfs, false);
    t.checkExpect(mw5.finalStateDfs, false);
    t.checkExpect(mw5.solvingDfs, false);
    t.checkExpect(mw5.cameFromEdgeBfs, new HashMap<Vertex, Edge>());
    t.checkExpect(mw5.bfsPath,
        new ArrayList<Vertex>(Arrays.asList(mw5.vertices.get(mw5.width - 1).get(mw5.height - 1))));
    t.checkExpect(mw5.bfsVisited, new ArrayList<Vertex>());
    t.checkExpect(mw5.bfsWorklist, new LinkedList<Vertex>());
    t.checkExpect(mw5.animatingBfs, false);
    t.checkExpect(mw5.reconstructBfs, false);
    t.checkExpect(mw5.finalStateBfs, false);
    t.checkExpect(mw5.solvingBfs, false);

  }

  // test onTick method
  void testOnTick(Tester t) {
    this.initMazeWorlds();
    this.initVertices();
    this.initEdges();

    // test mw2 dfs
    this.mw2.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw2.dfsVisited = new ArrayList<Vertex>();
    this.mw2.dfsWorklist = new Stack<Vertex>();
    this.mw2.dfsWorklist.add(this.v1);
    this.mw2.vertices.get(mw2.width - 1).set(mw2.height - 1, v2);
    this.mw2.solvingDfs = true;
    this.mw2.solvingBfs = false;
    this.mw2.reconstructDfs = false;
    this.mw2.reconstructBfs = false;
    this.mw2.playerReconstruct = false;
    this.mw2.onTick();
    t.checkExpect(this.mw2.dfsWorklist.size(), 2);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v2), true);
    t.checkExpect(this.mw2.dfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw2.cameFromEdgeDfs.containsKey(this.v3), true);
    t.checkExpect(this.mw2.dfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    // test mw3 dfs
    this.mw3.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw3.dfsVisited = new ArrayList<Vertex>();
    this.mw3.dfsWorklist = new Stack<Vertex>();
    this.mw3.dfsWorklist.add(this.v1);
    this.mw3.vertices.get(mw3.width - 1).set(mw3.height - 1, v3);
    this.mw3.onTick();
    this.mw3.solvingDfs = true;
    this.mw3.solvingBfs = false;
    this.mw3.reconstructDfs = false;
    this.mw3.reconstructBfs = false;
    this.mw3.playerReconstruct = false;
    t.checkExpect(this.mw3.dfsWorklist.size(), 1);
    t.checkExpect(this.mw3.dfsWorklist.contains(this.v2), false);
    t.checkExpect(this.mw3.cameFromEdgeDfs.containsKey(this.v2), false);
    t.checkExpect(this.mw3.dfsWorklist.contains(this.v3), false);
    t.checkExpect(this.mw3.cameFromEdgeDfs.containsKey(this.v3), false);
    t.checkExpect(this.mw3.dfsVisited, new ArrayList<Vertex>());

    // test mw2 bfs
    this.mw2.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw2.bfsVisited = new ArrayList<Vertex>();
    this.mw2.bfsWorklist = new LinkedList<Vertex>();
    this.mw2.bfsWorklist.add(this.v1);
    this.mw2.vertices.get(mw2.width - 1).set(mw2.height - 1, v2);
    this.mw2.solvingDfs = false;
    this.mw2.solvingBfs = true;
    this.mw2.reconstructDfs = false;
    this.mw2.reconstructBfs = false;
    this.mw2.playerReconstruct = false;
    this.mw2.onTick();
    t.checkExpect(this.mw2.bfsWorklist.size(), 2);
    t.checkExpect(this.mw2.bfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw2.cameFromEdgeBfs.containsKey(this.v2), true);
    t.checkExpect(this.mw2.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw2.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw2.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));

    // test mw3 bfs
    this.mw3.edges =
        new ArrayList<Edge>(Arrays.asList(this.e1, this.e2, this.e3, this.e4, this.e5));
    this.mw3.bfsVisited = new ArrayList<Vertex>();
    this.mw3.bfsWorklist = new LinkedList<Vertex>();
    this.mw3.bfsWorklist.add(this.v1);
    this.mw3.vertices.get(mw3.width - 1).set(mw3.height - 1, v3);
    this.mw3.solvingDfs = false;
    this.mw3.solvingBfs = true;
    this.mw3.reconstructDfs = false;
    this.mw3.reconstructBfs = false;
    this.mw3.playerReconstruct = false;
    this.mw3.onTick();
    t.checkExpect(this.mw3.bfsWorklist.size(), 2);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v2), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v2), true);
    t.checkExpect(this.mw3.bfsWorklist.contains(this.v3), true);
    t.checkExpect(this.mw3.cameFromEdgeBfs.containsKey(this.v3), true);
    t.checkExpect(this.mw3.bfsVisited, new ArrayList<Vertex>(Arrays.asList(this.v1)));
  }

}
