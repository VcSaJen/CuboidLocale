package cuboidLocale;

public class Test{
  public static void main(String[] argc){
    QuadTree tree = new QuadTree();
    java.util.Random rng = new java.util.Random();
    PrimitiveCuboid test;
    int x, y, z;
    int i;
    int ct = 0;
    for(i = 0; i < 500000; i++){
      x = (rng.nextInt()) % 10000;
      y = (rng.nextInt()) % 10000;
      z = (rng.nextInt()) % 10000;
      test = new PrimitiveCuboid(
          x, y, z,
          x + (rng.nextInt() % 256),
          y + (rng.nextInt() % 256),
          z + (rng.nextInt() % 256)
          );
      if(!tree.cuboidOverlapsExisting(test)){
        tree.insert(test);
        if(i % 47 == 0){
          tree.delete(test);
        }
      }else{
        ct++;
      }
      if(i % 1000 == 0){
        System.err.println(i);
      }
    }
    System.out.println(ct + " overlaped");
    BookmarkedResult res = BookmarkedResult.emptyBookmark;
    x = (rng.nextInt()) % 10000;
    y = (rng.nextInt()) % 10000;
    z = (rng.nextInt()) % 10000;
    for(i = 0; i < 10000000; i++){
      x += (rng.nextInt() % 64);
      y += (rng.nextInt() % 64);
      z += (rng.nextInt() % 64);
      res = tree.relatedSearch(res.bookmark, x, y, z);
      int size = res.results.size();
      if(size > 1){
        System.err.println("Point: " + x + ":" + y + ":" + z);
        for(PrimitiveCuboid pc : res.results){
          System.err.println("In " + pc);
        }
        System.err.println("S:" + size);
        QuadNode target = res.bookmark;
        while(target != null){
          System.err.println(target);
          for(PrimitiveCuboid pc : target.cuboids){
            System.err.println(pc);
          }
          target = target.nextListHolder;
        }
        System.err.println();
      }
    }
  }
}
