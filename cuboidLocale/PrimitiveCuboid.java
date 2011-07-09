package cuboidLocale;

public class PrimitiveCuboid{
  public long[] xyzA = { 0, 0, 0 };
  public long[] xyzB = { 0, 0, 0 };
  long lowIndex[] = new long[3];
  long highIndex[] = new long[3];

  private int hashcode = 0;

  /**
   * Normalize the corners so that all A is <= B
   * This is CRITICAL to have for comparison to a point
   */
  final private void normalize(){
    for(int i = 0; i < 3; i++){
      if(xyzA[i] > xyzB[i]){
        long temp = xyzA[i];
        xyzA[i] = xyzB[i];
        xyzB[i] = temp;
      }
      hashcode ^= xyzB[i] ^ (~xyzA[i]);
    }
  }

  PrimitiveCuboid(long[] xyzA, long[] xyzB){
    xyzA = xyzA.clone();
    xyzB = xyzB.clone();
    this.normalize();
  }

  PrimitiveCuboid(long xA, long yA, long zA, long xB, long yB, long zB){
    xyzA[0] = xA;
    xyzA[1] = yA;
    xyzA[2] = zA;

    xyzB[0] = xB;
    xyzB[1] = yB;
    xyzB[2] = zB;

    this.normalize();
  }

  final public boolean includesPoint(long x, long y, long z){
    if(xyzA[0] <= x && xyzA[1] <= y && xyzA[2] <= z &&
       xyzB[0] >= x && xyzB[1] >= y && xyzB[2] >= z){
      return true;
    }
    return false;
  }

  final public boolean includesPoint(long[] pt){
    return this.includesPoint(pt[0], pt[1], pt[2]);
  }

  @Override
  public int hashCode(){
    return hashcode;
  }

  @Override
  public boolean equals(Object o){
    if(o == this){
      return true;
    }else if(!(o instanceof PrimitiveCuboid)){
      return false;
    }
    PrimitiveCuboid c = (PrimitiveCuboid) o;

    for(int i = 0; i < 3; i++){
      if(xyzA[i] != c.xyzA[i]){
        return false;
      }
      if(xyzB[i] != c.xyzB[i]){
        return false;
      }
    }
    return true;
  }

  final public boolean overlaps(PrimitiveCuboid c){
    for(int i = 0; i < 3; i++){
      if(xyzA[i] > c.xyzB[i] || c.xyzA[i] > xyzB[i]){
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString(){
    return new StringBuilder()
      .append("xA: ").append(xyzA[0])
      .append(" yA: ").append(xyzA[1])
      .append(" zA: ").append(xyzA[2])
      .append("  xB: ").append(xyzB[0])
      .append(" yB: ").append(xyzB[1])
      .append(" zB: ").append(xyzB[2])
      .toString();
  }
}
