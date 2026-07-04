#!/usr/bin/env python3
"""Streaming Hot Movies Simulator - Python Standalone"""
import csv, os, sys, time
from collections import defaultdict
from datetime import datetime, timedelta

def load_clicks(p):
    with open(p, encoding="utf-8") as f:
        return [row for row in csv.DictReader(f)]

def load_movies(p):
    with open(p, encoding="utf-8") as f:
        return {row["movieId"]: row["title"] for row in csv.DictReader(f)}

def simulate_window(clicks, movies, ws=10, ss=5):
    print("="*60)
    print("Real-time Hot Movie Monitor - Window Simulator")
    print(f"Window: {ws}s, Slide: {ss}s")
    print("="*60)
    for c in clicks:
        c["pt"] = datetime.strptime(c["timestamp"], "%Y-%m-%d %H:%M:%S")
    clicks.sort(key=lambda x: x["pt"])
    st, et = clicks[0]["pt"], clicks[-1]["pt"]
    print(f"Range: {st} ~ {et}, Events: {len(clicks)}")
    cur = st; rn = 0
    while cur + timedelta(seconds=ws) <= et + timedelta(seconds=ss):
        wb, we = cur, cur + timedelta(seconds=ws)
        rn += 1
        wc = defaultdict(int)
        for c in clicks:
            if wb <= c["pt"] < we:
                wc[c["movieId"]] += 1
        sm = sorted(wc.items(), key=lambda x:-x[1])
        if sm:
            print(f"\nWindow {rn}: {wb.strftime('%H:%M:%S')} - {we.strftime('%H:%M:%S')}")
            print(f"{'Rank':<5}{'Movie':<30}{'Clicks':<10}")
            print("-"*45)
            for rk,(mid,cnt) in enumerate(sm[:5],1):
                print(f"{rk:<5}{movies.get(mid,mid):<30}{cnt:<10}")
        cur += timedelta(seconds=ss)
        time.sleep(0.3)
    print(f"\nTotal windows processed: {rn}")

def main():
    d = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    cp = os.path.join(d,"server","data","kafka_click_logs.csv")
    mp = os.path.join(d,"server","data","movies.csv")
    for f in [cp,mp]:
        if not os.path.exists(f): print(f"Missing: {f}"); return
    clicks = load_clicks(cp); movies = load_movies(mp)
    simulate_window(clicks, movies)

if __name__=="__main__":
    main()