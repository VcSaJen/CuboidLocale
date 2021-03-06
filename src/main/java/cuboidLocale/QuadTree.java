package cuboidLocale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class QuadTree{
  QuadNode root;

  private void beginTree(PrimitiveCuboid c){
    long size = 128;
    long minSize = Math.abs(c.xyzA[0] - c.xyzB[0]);
    long minSizeB = Math.abs(c.xyzA[2] - c.xyzB[2]);
    if(minSize < minSizeB){
      minSize = minSizeB;
    }
    while(size < minSize){
      size = size << 1;
    }
    root = new QuadNode(c.xyzA[0], c.xyzA[2], size - 1, null);
  }

  private boolean nodeFullyContainsCuboid(QuadNode node, PrimitiveCuboid c){
    if(node.x <= c.xyzA[0] &&
        node.z <= c.xyzA[2] &&
        (node.x + node.size) >= c.xyzB[0] &&
        (node.z + node.size) >= c.xyzB[2]){
      return true;
    }
    return false;
  }

  /**
   * Returns -1 for too small, 0 for minimal, 1 for larger than needed
   * <p>
   * Fit is based on the larger side. Means more tests but consumes an order of magnitude or less memory.
   * 
   * @param node
   * @param c
   * @return
   */
  private int containerFit(QuadNode node, PrimitiveCuboid c){
    long minSizeA = Math.abs(c.xyzA[0] - c.xyzB[0]);
    long minSizeB = Math.abs(c.xyzA[2] - c.xyzB[2]);
    long fitSize;
    if(minSizeA < minSizeB){
      fitSize = minSizeB;
    }else{
      fitSize = minSizeA;
    }

    if(node.size < fitSize){
      return -1;
    }else if(node.size == 1 || (node.size >> 1) < fitSize){
      return 0;
    }else{
      return 1;
    }
  }

  private QuadNode descendAndCreate(QuadNode start, PrimitiveCuboid c){
    QuadNode next = start;
    while(containerFit(next, c) > 0){
      int i = 0;
      long nX = 0;
      long nZ = 0;
      long half = (next.size >> 1);
      if(c.xyzA[0] > (next.x + half)){
        i++;
        nX = half + 1;
      }
      if(c.xyzA[2] > (next.z + half)){
        i += 2;
        nZ = half + 1;
      }
      if(next.quads[i] == null){
        next.quads[i] = new QuadNode(next.x + nX, next.z + nZ, half, next);
      }
      next = next.quads[i];
    }
    return next;
  }

  private QuadNode descendNoCreate(QuadNode start, PrimitiveCuboid c){
    QuadNode next = start;
    while(containerFit(next, c) > 0){
      int i = 0;
      long nX = 0;
      long nZ = 0;
      long half = (next.size >> 1);
      if(c.xyzA[0] > (next.x + half)){
        i++;
        nX = half + 1;
      }
      if(c.xyzA[2] > (next.z + half)){
        i += 2;
        nZ = half + 1;
      }
      if(next.quads[i] == null){
        next = new QuadNode(next.x + nX, next.z + nZ, half, next);
      }else{
        next = next.quads[i];
      }
    }
    return next;
  }

  private QuadNode descendAndSearch(QuadNode node, long x, long z){
    QuadNode next = node;
    while(next != null){
      node = next;
      long half = node.size >> 1;
      int i = 0;
      if(x > (node.x + half)){
        i++;
      }
      if(z > (node.z + half)){
        i += 2;
      }
      next = node.quads[i];
    }
    return node;
  }

  private QuadNode ascendFirstSearch(QuadNode node, long x, long z){
    while(node != null && (node.x > x || node.z > z ||
          (node.x + node.size) < x || (node.z + node.size) < z)){
      node = node.parent;
    }
    if(node == null){
      return null;
    }
    return descendAndSearch(node, x, z);
  }

  private List<PrimitiveCuboid> getMatchingCuboids(QuadNode target, long x, long y, long z){
    List<PrimitiveCuboid> matches = new ArrayList<PrimitiveCuboid>();
    while(target != null){
      for(PrimitiveCuboid potential : target.cuboids){
        if(potential.includesPoint(x, y, z)){
          matches.add(potential);
        }
      }
      target = target.nextListHolder;
    }
    return matches;
  }

  public List<PrimitiveCuboid> search(long x, long y, long z){
    QuadNode node = descendAndSearch(root, x, z);
    return getMatchingCuboids(node, x, y, z);
  }

  public BookmarkedResult relatedSearch(QuadNode bookmark, long x, long y, long z){
    if(bookmark == null){
      bookmark = root;
    }
    QuadNode node = ascendFirstSearch(bookmark, x, z);
    return new BookmarkedResult(node, getMatchingCuboids(node, x, y, z));
  }

  public BookmarkedResult relatedSearch(BookmarkedResult bookmark, long x, long y, long z){
    return relatedSearch(bookmark, x, y, z);
  }

  /**
   * grow the tree beyond the root in the direction of the target node
   * 
   * @param c
   */
  synchronized private void repotTree(PrimitiveCuboid c){
    QuadNode oldRoot;
    int i;
    do{
      oldRoot = root;
      root = new QuadNode(oldRoot.x, oldRoot.z, (oldRoot.size << 1) + 1, null);
      oldRoot.parent = root;
      // Figure out the best direction to grow in (that is, which quadrant is
      // the old root in the new root?)
      // We start at lower left (quad 0)
      i = 0;
      // The target is left of us
      if(c.xyzA[0] < root.x){
        i++;
        root.x -= oldRoot.size + 1;
      }
      // The target is below us
      if(c.xyzA[2] < root.z){
        i += 2;
        root.z -= oldRoot.size + 1;
      }
      root.quads[i] = oldRoot;
    }while(!nodeFullyContainsCuboid(root, c));
  }

  /**
   * Oftentimes a node will overlap with the neighbors of a node
   * Since we always search for the next node based on the lower left we know
   * that the left and bottom
   * will not go over the edge, leaving only the top, right, and upper right
   * possibilities need be regarded.
   * Spits out a list of cuboids that are fit for "insertion" although we just
   * use them for the search and actually
   * attach the original cuboid.
   * We also return the remainder shard if we generated any others. At the other
   * end we only include a node if it's
   * shard didn't re-shard. Keeps the tree search spaces minimal.
   */
  private List<PrimitiveCuboid> generateShards(QuadNode node, PrimitiveCuboid c){
    List<PrimitiveCuboid> shards = new ArrayList<PrimitiveCuboid>(4);
    long top = node.z + node.size;
    long right = node.x + node.size;
    long tmp;

    // find a shard above if it exists
    if(top < c.xyzB[2]){
      // Find out if it extends past the top only or the right and top
      // Limit the "top" shard to only directly above the original node
      if(right < c.xyzB[0]){
        tmp = right;
      }else{
        tmp = c.xyzB[0];
      }
      shards.add(new PrimitiveCuboid(c.xyzA[0], 0, top + 1,
          tmp, 0, c.xyzB[2]));
    }
    // Find a shard to the right
    if(right < c.xyzB[0]){
      // find if we extend past the top as well
      // Limit the "right" shard to only directly right
      if(top < c.xyzB[2]){
        tmp = top;
      }else{
        tmp = c.xyzB[2];
      }
      shards.add(new PrimitiveCuboid(right + 1, 0, c.xyzA[2],
          c.xyzB[0], 0, tmp));
    }
    // Check for a top right shard
    if(right < c.xyzB[0] && top < c.xyzB[2]){
      shards.add(new PrimitiveCuboid(right + 1, 0, top + 1,
          c.xyzB[0], 0, c.xyzB[2]));
    }
    // include the remainder as a shard if we generated any others
    if(shards.size() > 0){
      shards.add(new PrimitiveCuboid(c.xyzA[0], 0, c.xyzA[2],
          right, 0, top));
    }
    return shards;
  }

  // Finds all the nodes that a cuboid should reside in (handles sharding)
  private List<QuadNode> getAllTargets(QuadNode initialNode, PrimitiveCuboid c){
    List<QuadNode> targets = new ArrayList<QuadNode>();
    // Generate the initial shards
    Stack<PrimitiveCuboid> shards = new Stack<PrimitiveCuboid>();
    shards.addAll(generateShards(initialNode, c));

    QuadNode node;
    int i = 0;
    while(!shards.empty()){
      PrimitiveCuboid shard = shards.pop();
      node = descendAndCreate(root, shard);
      List<PrimitiveCuboid> newShards = generateShards(node, shard);
      // If no shards were made then this is is the bounding node for this
      // shard. Include it.
      if(newShards.size() == 0){
        targets.add(node);
      }else{
        shards.addAll(newShards);
      }
      i++;
    }

    // If the initial shard attempt turns out to not have had
    // to generate shards then we need to add the initial node
    if(targets.size() == 0){
      targets.add(initialNode);
    }

    return targets;
  }

  // Finds all the nodes that a cuboid should reside in (handles sharding)
  private List<QuadNode> getAllTargetsNoCreate(QuadNode initialNode, PrimitiveCuboid c){
    List<QuadNode> targets = new ArrayList<QuadNode>();
    // Generate the initial shards
    Stack<PrimitiveCuboid> shards = new Stack<PrimitiveCuboid>();
    shards.addAll(generateShards(initialNode, c));

    QuadNode node;
    int i = 0;
    while(!shards.empty()){
      PrimitiveCuboid shard = shards.pop();
      node = descendNoCreate(root, shard);
      List<PrimitiveCuboid> newShards = generateShards(node, shard);
      // If no shards were made then this is is the bounding node for this
      // shard. Include it.
      if(newShards.size() == 0){
        targets.add(node);
      }else{
        shards.addAll(newShards);
      }
      i++;
    }

    // If the initial shard attempt turns out to not have had
    // to generate shards then we need to add the initial node
    if(targets.size() == 0){
      targets.add(initialNode);
    }

    return targets;
  }

  /**
   * Adds the PrimitiveCuboid to the target node and fixes up the children's
   * list holder link
   * 
   * @param node
   * @param c
   */
  private void addAndFixListHolders(QuadNode node, PrimitiveCuboid c){
    node.cuboids.add(c);
    // This isn't our first cuboid, so no fix needed
    if(node.cuboids.size() > 1){
      return;
    }
    // Descend the tree. When we find a node with no cuboids it needs a new list
    // holder
    Stack<QuadNode> todo = new Stack<QuadNode>();
    todo.push(node);
    QuadNode current;
    do{
      current = todo.pop();
      for(QuadNode child : current.quads){
        if(child == null){
          continue;
        }
        // If the child isn't holding a list of cuboids itself
        // (which would make it the list holder link for its children)
        // Then we need to fix it's children as well
        if(child.cuboids.size() == 0){
          todo.push(child);
        }
        child.nextListHolder = node;
      }
    }while(!todo.empty());
  }

  public boolean cuboidOverlapsExisting(PrimitiveCuboid c){
    if(root == null){
      return false;
    }
    // if this cuboid falls outside of the tree, we need to repot the tree to
    // gain a wider perspective!
    if(!nodeFullyContainsCuboid(root, c)){
      repotTree(c);
    }
    QuadNode node = root;
    node = descendNoCreate(node, c);
    // Now that we have our target we potentially need to generate shards and
    // target their nodes as well
    List<QuadNode> targets = getAllTargetsNoCreate(node, c);
    Stack<QuadNode> children = new Stack<QuadNode>();
    Set<PrimitiveCuboid> cuboids = new HashSet<PrimitiveCuboid>(256); // Generous initial capacity for speed
    QuadNode childTarget;
    // Of note: adding all the cuboids to the set and then testing is faster
    // than testing as we go and potentially getting out faster
    // This is especially true when there is less likely to be an overlap anyway
    for(QuadNode target : targets){
      // Drill down to the children nodes to get the smaller cuboids contained therein
      children.add(target);
      do{
        childTarget = children.pop();
        for(QuadNode child : childTarget.quads){
          if(child == null){
            continue;
          }
          children.push(child);
          cuboids.addAll(child.cuboids);
        }
      }while(!children.isEmpty());
      // Then ascend backup and add the ones there
      while(target != null){
        cuboids.addAll(target.cuboids);
        target = target.nextListHolder;
      }
    }
    for(PrimitiveCuboid pc : cuboids){
      if(c.overlaps(pc)){
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public List<PrimitiveCuboid> cuboidOverlapsWith(PrimitiveCuboid c){
    if(root == null){
      return Collections.EMPTY_LIST;
    }
    // if this cuboid falls outside of the tree, we need to repot the tree to
    // gain a wider perspective!
    if(!nodeFullyContainsCuboid(root, c)){
      repotTree(c);
    }
    QuadNode node = root;
    node = descendNoCreate(node, c);
    // Now that we have our target we potentially need to generate shards and
    // target their nodes as well
    List<QuadNode> targets = getAllTargetsNoCreate(node, c);
    Stack<QuadNode> children = new Stack<QuadNode>();
    Set<PrimitiveCuboid> cuboids = new HashSet<PrimitiveCuboid>(256); // Generous initial capacity for speed
    QuadNode childTarget;
    // Of note: adding all the cuboids to the set and then testing is faster
    // than testing as we go and potentially getting out faster
    // This is especially true when there is less likely to be an overlap anyway
    for(QuadNode target : targets){
      // Drill down to the children nodes to get the smaller cuboids contained therein
      children.add(target);
      do{
        childTarget = children.pop();
        for(QuadNode child : childTarget.quads){
          if(child == null){
            continue;
          }
          children.push(child);
          cuboids.addAll(child.cuboids);
        }
      }while(!children.isEmpty());
      // Then ascend backup and add the ones there
      while(target != null){
        cuboids.addAll(target.cuboids);
        target = target.nextListHolder;
      }
    }
    List<PrimitiveCuboid> overlaps = new ArrayList<PrimitiveCuboid>();
    for(PrimitiveCuboid pc : cuboids){
      if(c.overlaps(pc)){
        overlaps.add(pc);
      }
    }
    return overlaps;
  }

  /**
   * Attempts to insert the node ONLY if there are no overlaps with existing nodes
   * 
   * @param c
   *          cuboid to insert
   * @return success or failure
   */
  public boolean insertOnlyWithoutOverlaps(PrimitiveCuboid c){
    if(root == null){
      insert(c);
      return true;
    }
    // if this cuboid falls outside of the tree, we need to repot the tree to
    // gain a wider perspective!
    if(!nodeFullyContainsCuboid(root, c)){
      repotTree(c);
    }
    QuadNode node = root;
    node = descendAndCreate(node, c);
    // Now that we have our target we potentially need to generate shards and
    // target their nodes as well
    List<QuadNode> targets = getAllTargets(node, c);
    Stack<QuadNode> children = new Stack<QuadNode>();
    Set<PrimitiveCuboid> cuboids = new HashSet<PrimitiveCuboid>(256); // Generous initial capacity for speed
    QuadNode childTarget;
    // Of note: adding all the cuboids to the set and then testing is faster
    // than testing as we go and potentially getting out faster
    // This is especially true when there is less likely to be an overlap anyway
    for(QuadNode target : targets){
      // Drill down to the children nodes to get the smaller cuboids contained therein
      children.add(target);
      do{
        childTarget = children.pop();
        for(QuadNode child : childTarget.quads){
          if(child == null){
            continue;
          }
          children.push(child);
          cuboids.addAll(child.cuboids);
        }
      }while(!children.isEmpty());
      // Then ascend backup and add the ones there
      while(target != null){
        cuboids.addAll(target.cuboids);
        target = target.nextListHolder;
      }
    }
    for(PrimitiveCuboid pc : cuboids){
      if(c.overlaps(pc)){
        for(QuadNode target : targets){
          if(target.cuboids.size() == 0){
            pruneTree(node);
          }
        }
        return false;
      }
    }
    // Add the cuboid everywhere it belongs
    for(QuadNode target : targets){
      addAndFixListHolders(target, c);
    }
    return true;
  }

  private void removeAndFixListHolders(QuadNode node, PrimitiveCuboid c){
    node.cuboids.remove(c);
    // This wasn't our only cuboid, so no fix needed
    if(node.cuboids.size() > 0){
      return;
    }
    // Descend the tree. When we find a node with no children we know it needs a
    // new list holder
    Stack<QuadNode> todo = new Stack<QuadNode>();
    todo.push(node);
    QuadNode current;
    do{
      current = todo.pop();
      for(QuadNode child : current.quads){
        if(child == null){
          continue;
        }
        // If the child isn't holding a list of cuboids itself
        // (which would make it the list holder link for its children)
        // Then we need to fix it's children as well
        if(child.cuboids.size() == 0){
          todo.push(child);
        }
        child.nextListHolder = node.nextListHolder;
      }
    }while(!todo.empty());
    pruneTree(node);
  }

  /**
   * Removes any node from the tree that no longer serves a purpose, starting
   * from the node given and moving up
   * 
   * @param node
   */
  private void pruneTree(QuadNode node){
    int i;
    while(node.parent != null && node.quads[0] == null && node.quads[1] == null && node.quads[2] == null
        && node.quads[3] == null){
      i = 0;
      if(node.x != node.parent.x){
        i++;
      }
      if(node.z != node.parent.z){
        i += 2;
      }
      node = node.parent;
      node.quads[i] = null;
    }
  }

  synchronized public void insert(PrimitiveCuboid c){
    if(root == null){
      beginTree(c);
    }
    // if this cuboid falls outside of the tree, we need to repot the tree to
    // gain a wider perspective!
    if(!nodeFullyContainsCuboid(root, c)){
      repotTree(c);
    }
    QuadNode node = root;
    node = descendAndCreate(node, c);
    // Now that we have our target we potentially need to generate shards and
    // target their nodes as well
    List<QuadNode> targets = getAllTargets(node, c);
    // Add the cuboid everywhere it belongs
    for(QuadNode target : targets){
      addAndFixListHolders(target, c);
    }
  }

  synchronized public void delete(PrimitiveCuboid c){
    // No root? No-Op!
    if(root == null){
      return;
    }
    QuadNode node;
    // Should not create any new nodes, but only if the cuboid is, in fact, in
    // the tree
    node = descendAndCreate(root, c);
    // Using the same algorithm that was used during creation will give us the
    // same list of nodes to examine
    List<QuadNode> targets = getAllTargets(node, c);
    for(QuadNode target : targets){
      removeAndFixListHolders(target, c);
    }
  }

}
