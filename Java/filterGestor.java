package bcif;

/**
 * <p> Class containing the various predictive filtering functions. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class filterGestor {

  private filterer[] filters = null;
  private colorFilterer[] cfilters = null;

  public filterGestor() {
    filters = new filterer[12];
    cfilters = new colorFilterer[6];
    filters[0] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int res = 0;
        int l = (int)left;
        int d = (int)low;
        int ld = (int)ll;
        l = l < 0 ? l + 256 : l;
        d = d < 0 ? d + 256 : d;
        ld = ld < 0 ? ld + 256 : ld;
        if (ld >= l && ld >= d) {
          if (l > d) { res = d; } else { res = l; }
        } else if (ld <= l && ld <= d) {
          if (l > d) { res = l; } else { res = d; }
        } else {
          res = d + l - ld;
        }
        return (byte)res;
      }
    };
    filters[1] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        return 0;
      }
    };
    filters[2] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        return left;
      }
    };
    filters[3] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int lw = (int)left;
        int hl = (int)low;
        lw = lw < 0 ? lw + 256 : lw;
        hl = hl < 0 ? hl + 256 : hl;
        int newVal = (lw + hl) >> 1;
        return (byte)newVal;
      }
    };
    filters[4] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        return low;
      }
    };
    filters[5] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        return ll;
      }
    };
    filters[6] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int newValx = (int)left;
        int newValy = (int)low;
        int newValxy = (int)ll;
        newValx = newValx < 0 ? newValx + 256 : newValx;
        newValy = newValy < 0 ? newValy + 256 : newValy;
        newValxy = newValxy < 0 ? newValxy + 256 : newValxy;
        int newVal = newValx + newValy - newValxy;
        if (newVal > 255) { newVal = 255; }
        if (newVal < 0) { newVal = 0;}
        return (byte)newVal;
      }
    };
    filters[7] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        return lr;
      }
    };
    filters[8] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int lf = (int)left;
        int llf = (int)ll;
        int lw = (int)low;
        lf = lf < 0 ? lf + 256 : lf;
        llf = llf < 0 ? llf + 256 : llf;
        lw = lw < 0 ? lw + 256 : lw;
        return (byte)(lf + ((lw - llf) >> 1));
      }
    };
    filters[9] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int lf = (int)left;
        int llf = (int)ll;
        int lw = (int)low;
        lw = lw < 0 ? lw + 256 : lw;
        lf = lf < 0 ? lf + 256 : lf;
        llf = llf < 0 ? llf + 256 : llf;
        return (byte)(lw + ((lf - llf) >> 1));
      }
    };
    filters[10] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int lf = (int)left;
        int llf = (int)ll;
        int lw = (int)low;
        int hl = (int)lr;
        lw = lw < 0 ? lw + 256 : lw;
        lf = lf < 0 ? lf + 256 : lf;
        llf = llf < 0 ? llf + 256 : llf;
        hl = hl < 0 ? hl + 256 : hl;
        return (byte) ( (lf + llf + lw + hl + 1) >> 2);
      }
    };
    filters[11] = new filterer() {
      public byte filter(byte left, byte low, byte ll, byte lr) {
        int lw = (int)low;
        int hl = (int)lr;
        lw = lw < 0 ? lw + 256 : lw;
        hl = hl < 0 ? hl + 256 : hl;
        return (byte)((lw + hl) >> 1);
      }
    };

    cfilters[0] = new colorFilterer() {
      public byte colFilter(byte c0, byte c1, byte c2, int pos) {
        if (pos == 0) { return 0; } else
          if (pos == 1) { return c0; } else {
            return c1;
          }
      }
    };
    cfilters[1] = new colorFilterer() {
      public byte colFilter(byte c0, byte c1, byte c2, int pos) {
        return 0;
      }
    };
    cfilters[2] = new colorFilterer() {
      public byte colFilter(byte c0, byte c1, byte c2, int pos) {
        if (pos == 0) { return 0; } else
          return c0;
      }
    };
    cfilters[3] = new colorFilterer() {
      public byte colFilter(byte c0, byte c1, byte c2, int pos) {
        if (pos == 2) { return 0; } else {
          return c2;
        }
      }
    };
    cfilters[4] = new colorFilterer() {
      public byte colFilter(byte c0, byte c1, byte c2, int pos) {
        if (pos == 2) { return 0; } else
          if (pos == 1) { return c2; } else {
            return (byte)(c1 + c2);
          }
      }
    };
    cfilters[5] = new colorFilterer() {
      public byte colFilter(byte c0, byte c1, byte c2, int pos) {
        if (pos == 1) { return 0; } else {
          return c1;
        }
      }
    };

 }

  public filterer getFilter(int f) {
    return filters[f];
  }

  public colorFilterer getColFilter(int f) {
    return cfilters[f];
  }

}
