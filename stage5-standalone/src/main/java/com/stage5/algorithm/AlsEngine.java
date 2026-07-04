package com.stage5.algorithm;
import com.stage5.model.Rating;
import java.util.*;
public class AlsEngine {
    private final int rank;
    private final int iterations;
    private final double regParam;
    private final Random random;
    public AlsEngine(int rank, int iterations, double regParam) {
        this.rank=rank; this.iterations=iterations; this.regParam=regParam;
        this.random=new Random(42L);
    }
    public Map<Long, List<MovieScore>> train(List<Rating> ratings, int topN) {
        Set<Long> userIds=new HashSet<>(), movieIds=new HashSet<>();
        for (Rating r : ratings) { userIds.add(r.getUserId()); movieIds.add(r.getMovieId()); }
        List<Long> userList=new ArrayList<>(userIds), movieList=new ArrayList<>(movieIds);
        Collections.sort(userList); Collections.sort(movieList);
        Map<Long,Integer> userIndex=new HashMap<>(), movieIndex=new HashMap<>();
        Map<Integer,Long> indexToMovie=new HashMap<>();
        for (int i=0;i<userList.size();i++) userIndex.put(userList.get(i),i);
        for (int i=0;i<movieList.size();i++) { movieIndex.put(movieList.get(i),i); indexToMovie.put(i,movieList.get(i)); }
        int nu=userList.size(), nm=movieList.size();
        double[][] uf=new double[nu][rank], mf=new double[nm][rank];
        for (int u=0;u<nu;u++) for (int f=0;f<rank;f++) uf[u][f]=random.nextDouble()*0.1;
        for (int m=0;m<nm;m++) for (int f=0;f<rank;f++) mf[m][f]=random.nextDouble()*0.1;
        Map<Integer,List<int[]>> uRatings=new HashMap<>(), mRatings=new HashMap<>();
        for (Rating r : ratings) {
            int u=userIndex.get(r.getUserId()), m=movieIndex.get(r.getMovieId()), v=(int)(r.getRating()*100);
            uRatings.computeIfAbsent(u,k->new ArrayList<>()).add(new int[]{m,v});
            mRatings.computeIfAbsent(m,k->new ArrayList<>()).add(new int[]{u,v});
        }
        for (int it=0;it<iterations;it++) {
            for (int u=0;u<nu;u++) solve(uf[u],mf,uRatings.getOrDefault(u,Collections.emptyList()),regParam);
            for (int m=0;m<nm;m++) solve(mf[m],uf,mRatings.getOrDefault(m,Collections.emptyList()),regParam);
            if ((it+1)%3==0) System.out.println("  ALS iter "+(it+1)+"/"+iterations);
        }
        Map<Long,Set<Long>> ratedByUser=new HashMap<>();
        for (Rating r : ratings) ratedByUser.computeIfAbsent(r.getUserId(),k->new HashSet<>()).add(r.getMovieId());
        Map<Long,List<MovieScore>> recs=new HashMap<>();
        for (Long uid : userList) {
            int u=userIndex.get(uid);
            Set<Long> rated=ratedByUser.getOrDefault(uid,Collections.emptySet());
            List<MovieScore> scores=new ArrayList<>();
            for (Long mid : movieList) {
                if (rated.contains(mid)) continue;
                int m=movieIndex.get(mid);
                double pred=0;
                for (int f=0;f<rank;f++) pred+=uf[u][f]*mf[m][f];
                scores.add(new MovieScore(mid,Math.min(5.0,Math.max(0.5,pred))));
            }
            scores.sort((a,b)->Double.compare(b.score,a.score));
            recs.put(uid,scores.subList(0,Math.min(topN,scores.size())));
        }
        return recs;
    }
    private void solve(double[] target, double[][] fixed, List<int[]> ratings, double reg) {
        int r=target.length;
        double[][] A=new double[r][r];
        double[] b=new double[r];
        for (int[] rl : ratings) {
            double v=rl[1]/100.0;
            double[] feat=fixed[rl[0]];
            for (int i=0;i<r;i++) { b[i]+=feat[i]*v; for (int j=0;j<r;j++) A[i][j]+=feat[i]*feat[j]; }
        }
        for (int i=0;i<r;i++) A[i][i]+=reg;
        double[][] aug=new double[r][r+1];
        for (int i=0;i<r;i++) { System.arraycopy(A[i],0,aug[i],0,r); aug[i][r]=b[i]; }
        for (int col=0;col<r;col++) {
            int mr=col;
            for (int row=col+1;row<r;row++) if(Math.abs(aug[row][col])>Math.abs(aug[mr][col])) mr=row;
            double[] tmp=aug[col]; aug[col]=aug[mr]; aug[mr]=tmp;
            double p=aug[col][col];
            if (Math.abs(p)<1e-10) continue;
            for (int j=col;j<=r;j++) aug[col][j]/=p;
            for (int row=0;row<r;row++) if(row!=col) { double f=aug[row][col]; for (int j=col;j<=r;j++) aug[row][j]-=f*aug[col][j]; }
        }
        for (int i=0;i<r;i++) target[i]=aug[i][r];
    }
    public static class MovieScore {
        private final long movieId;
        private final double score;
        public MovieScore(long movieId, double score) { this.movieId=movieId; this.score=score; }
        public long getMovieId() { return movieId; }
        public double getScore() { return score; }
    }
}