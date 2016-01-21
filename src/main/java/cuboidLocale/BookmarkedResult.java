package cuboidLocale;

import java.util.Collections;
import java.util.List;

public class BookmarkedResult{
  final public List<PrimitiveCuboid> results;
  final QuadNode bookmark;

  BookmarkedResult(QuadNode node, List<PrimitiveCuboid> c){
    bookmark = node;
    results = Collections.unmodifiableList(c);
  }

  @SuppressWarnings("unchecked")
  public static final BookmarkedResult emptyBookmark = new BookmarkedResult(
      null, (List<PrimitiveCuboid>) (Collections.EMPTY_LIST)
      );

}
