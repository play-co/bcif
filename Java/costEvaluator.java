package bcif;

/**
 * <p> Class for the heuristic determination of the best filter to apply to every zone.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class costEvaluator {


  private static int[] c = new int[]
                           {3, 6, 9, 12, 15, 18, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 32, 38, 41, 47, 36, 42, 45,
                            51, 48, 54, 57, 63, 22, 28, 31, 37, 34, 40, 43, 49, 38, 44, 47, 53, 50, 56, 59, 65, 42, 48, 51, 57, 54,
                            60, 63, 69, 58, 64, 67, 73, 70, 76, 79, 85, 24, 30, 33, 39, 36, 42, 45, 51, 40, 46, 49, 55, 52, 58, 61,
                            67, 44, 50, 53, 59, 56, 62, 65, 71, 60, 66, 69, 75, 72, 78, 81, 87, 46, 52, 55, 61, 58, 64, 67, 73, 62,
                            68, 71, 77, 74, 80, 83, 89, 66, 72, 75, 81, 78, 84, 87, 93, 82, 88, 91, 97, 94, 100, 103, 109, 110};

   private int[][] freqs = null;
   private int[] totFreqs = null;
   private int[] cost = null;
   private int[] fCost = null;
   private boolean enCalc = true;
   private int sc = 0;
   private int passedZones = 0;
   private int zoneLim = 20;
   private int enZoneLim = 10;

   public static int[] getCosts() {
     for (int i = 7; i < 129; i++) {
       c[i] = 22 + ((i - 7) >> 1);
     }
     return c;
   }

   public static int[][] getColCosts(int[][] f) {
     int [][] res = new int[3][256];
     res[0] = c; //enFillCost(f[0]);
     res[1] = c; //enFillCost(f[1]);
     res[2] = c; //enFillCost(f[2]);
     return res;
   }

   public costEvaluator(int fn) {
     freqs = new int[fn][256];
     totFreqs = new int[256];
   }

   public int putVal(int fn, int val) {
     freqs[fn][val]++;
     if (passedZones == 0) {
     }
     if (! enCalc) {
       fCost[fn] += cost[val];
     }
     return c[val < 128 ? val : 256 - val];
   }

   public int getFilCost(int fn) {
     if (enCalc) {
       int[] nf = new int[256];
       for (int i = 0; i < 256; i++) {
         nf[i] = totFreqs[i] + freqs[fn][i];
       }
       double enf = entropy(nf);
       return (int)(1000 * enf);
     } else {
       return fCost[fn];
     }
   }

   public void remSignalSel(int fn) {
     for (int i2 = 0; i2 < freqs.length; i2 ++) {
       freqs[i2] = new int[256];
     }
   }

   public void signalSel(int fn) {
     for (int i = 0; i < 256; i++) {
       totFreqs[i] = totFreqs[i] + freqs[fn][i];
     }
     fCost = new int[freqs.length];
    for (int i2 = 0; i2 < freqs.length; i2 ++) {
      freqs[i2] = new int[256];
    }
     if (! enCalc) {
       fCost = new int[freqs.length];
     }
     passedZones ++;
     if (! enCalc && (passedZones == zoneLim)) {
       fillCosts();
       if (zoneLim < 1000) { zoneLim = zoneLim << 1; } else {
         zoneLim += 1000;
       }
     }
     if (enCalc && (passedZones == enZoneLim)) {
       remEnCalc();
     }
   }

   public void remEnCalc() {
     enCalc = false;
     fillCosts();
   }

   public void fillCosts() {
     cost = new int[256];
     fCost = new int[totFreqs.length];
     int total = 0;
     for (int i = 0; i < 256; i++) {
       total += totFreqs[i];
     }
     for (int i = 0; i < 256; i++) {
       if (totFreqs[i] == 0) {
         cost[i] = total;
       } else {
         cost[i] = total / totFreqs[i];
       }
       cost[i] = (int)(Math.round(Math.log(cost[i]) / Math.log(2)));
     }
   }

   public static double entropy(int[] simbolsFreq) {
     double e = 0;
     long tot = 0;
     double res = 0;
     for (int i = 0; i < simbolsFreq.length; i ++) {
       tot += simbolsFreq[i];
     }
     double[] relFreq = new double[simbolsFreq.length];
     for (int i = 0; i < simbolsFreq.length; i++) {
       relFreq[i] = (double)simbolsFreq[i] / tot;
     }
     for (int i = 0; i < relFreq.length; i ++) {
       if (relFreq[i] > 0) {
           e -= relFreq[i] * (Math.log(relFreq[i]) / Math.log(2));
       }
     }
     return e;
   }

   private static int[] enFillCost(int[] f) {
     int [] cost = new int[256];
     int tot = 0;
     for (int i = 0; i < 256; i++) {
       if (f[i] == 0) { f[i] = 1; }
       tot += f[i];
     }
     for (int i = 0; i < 256; i++) {
       float freq = (float)f[i] / tot;
       cost[i] = -(int)Math.round(Math.log(freq) / Math.log(2));
     }
     return cost;
   }
}
