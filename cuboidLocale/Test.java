package cuboidLocale;

public class Test{
  public static void main(String[] argc){
    QuadTree tree = new QuadTree();
    java.util.Random rng = new java.util.Random();
    PrimitiveCuboid test;
    int x, y, z;
    int i;
    int ct = 0;
    for(i = 0; i < 100000; i++){
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
        // System.out.println("Overlap");
        ct++;
      }
      if(i % 1000 == 0){
        System.err.println(i);
      }
    }
    System.out.println(ct + "overlaped");
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
        System.err.println("S:" + size);
        QuadNode target = res.bookmark;
        while(target != null){
          System.err.println(target);
          for(PrimitiveCuboid pc : target.cuboids){
            System.err.println(pc);
          }
          target = target.nextListHolder;
        }
      }
    }

  }
}
