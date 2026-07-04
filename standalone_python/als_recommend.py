#!/usr/bin/env python3
"""ALS Movie Recommendation - Python Standalone"""
import csv, os, sys, random
from collections import defaultdict

def load_ratings(p):
    with open(p, encoding="utf-8") as f:
        return [{k: int(v) if k != "rating" else float(v) for k,v in row.items()} for row in csv.DictReader(f)]

def load_movies(p):
    with open(p, encoding="utf-8") as f:
        return {int(row["movieId"]): {"title": row["title"], "category": row["category"]} for row in csv.DictReader(f)}

def solve(mat, vec):
    n = len(mat)
    a = [r[:]+[v] for r,v in zip(mat, vec)]
    for i in range(n):
        mr = max(range(i,n), key=lambda r: abs(a[r][i]))
        a[i], a[mr] = a[mr], a[i]
        p = a[i][i]
        if abs(p) < 1e-10: continue
        for j in range(i,n+1): a[i][j] /= p
        for k in range(n):
            if k != i:
                f = a[k][i]
                for j in range(i,n+1): a[k][j] -= f*a[i][j]
    return [r[-1] for r in a]

def als_train(ratings, movies, rank=10, iterations=10, reg=0.1):
    users = sorted(set(r["userId"] for r in ratings))
    items = sorted(set(r["movieId"] for r in ratings))
    u2i = {u:i for i,u in enumerate(users)}
    m2i = {m:i for i,m in enumerate(items)}
    i2m = {i:m for i,m in enumerate(items)}
    uf = [[random.random()*0.1 for _ in range(rank)] for _ in range(len(users))]
    vf = [[random.random()*0.1 for _ in range(rank)] for _ in range(len(items))]
    rp = defaultdict(list)
    for r in ratings: rp[u2i[r["userId"]]].append((m2i[r["movieId"]], r["rating"]))
    for it in range(iterations):
        for u in range(len(users)):
            mat = [[0]*rank for _ in range(rank)]
            vec = [0]*rank
            for m,r in rp[u]:
                for i in range(rank):
                    vec[i] += vf[m][i]*r
                    for j in range(rank): mat[i][j] += vf[m][i]*vf[m][j]
            for i in range(rank): mat[i][i] += reg
            uf[u] = solve(mat, vec)
        ip = defaultdict(list)
        for u,ps in rp.items():
            for m,r in ps: ip[m].append((u,r))
        for m in range(len(items)):
            mat = [[0]*rank for _ in range(rank)]
            vec = [0]*rank
            for u,r in ip[m]:
                for i in range(rank):
                    vec[i] += uf[u][i]*r
                    for j in range(rank): mat[i][j] += uf[u][i]*uf[u][j]
            for i in range(rank): mat[i][i] += reg
            vf[m] = solve(mat, vec)
        if (it+1)%3==0: print(f"  iter {it+1}/{iterations}")
    recs = {}
    for ui,uid in enumerate(users):
        rated = set(r["movieId"] for r in ratings if r["userId"]==uid)
        scores = [(i2m[mi], sum(uf[ui][k]*vf[mi][k] for k in range(rank))) for mi in range(len(items)) if i2m[mi] not in rated]
        scores.sort(key=lambda x:-x[1])
        recs[uid] = [{"movieId":m,"title":movies[m]["title"],"category":movies[m]["category"],"predictedRating":round(s,2)} for m,s in scores[:3]]
    return recs

def main():
    print("="*60)
    print("ALS Movie Recommendation (Python Standalone)")
    print("="*60)
    d = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    rp = os.path.join(d,"server","data","ratings.csv")
    mp = os.path.join(d,"server","data","movies.csv")
    for f in [rp,mp]:
        if not os.path.exists(f): print(f"Missing: {f}"); return
    ratings = load_ratings(rp); movies = load_movies(mp)
    print(f"Data: {len(ratings)} ratings, {len(movies)} movies")
    recs = als_train(ratings, movies)
    for uid in sorted(recs.keys()):
        print(f"\nUser {uid}:")
        for i,r in enumerate(recs[uid],1): print(f"  {i}. {r['title']} [{r['category']}] - {r['predictedRating']}")

if __name__=="__main__": main()