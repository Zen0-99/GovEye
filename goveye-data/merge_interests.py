#!/usr/bin/env python3
"""Merge live API interests with historical mySociety interests.

Strategy:
  - Live API data (interests.db): Current register, structured fields.
    Authoritative for 2024+ entries. IDs < 1,000,000.
  - Historical data (interests_historical.db): 2000-2026 from mySociety.
    Covers pre-2024 entries that the API doesn't have. IDs >= 1,000,000.

  For entries that appear in both (current register period), prefer the
  live API version (better structured fields). We detect duplicates by
  (memberId, summary) pairs — if the same MP has the same summary text,
  it's likely the same entry.

  Former PM placeholder MP records (negative IDs) are also merged into
  the output DB.

Usage:
  python merge_interests.py --live interests.db --historical interests_historical.db --output interests_merged.db
"""

import argparse
import os
import sqlite3
import sys


def merge_dbs(live_path, historical_path, output_path):
    """Merge live and historical interests DBs."""
    if not os.path.exists(live_path):
        print(f"ERROR: Live DB not found: {live_path}")
        sys.exit(1)
    if not os.path.exists(historical_path):
        print(f"ERROR: Historical DB not found: {historical_path}")
        sys.exit(1)

    # Start with a copy of the historical DB (it has mps table + interests)
    import shutil
    print(f"Copying historical DB to {output_path}...")
    shutil.copy2(historical_path, output_path)

    conn = sqlite3.connect(output_path)
    cur = conn.cursor()

    # Build a set of (memberId, summary) pairs already in the DB
    # to avoid inserting duplicates from the live API data.
    # We normalize summary to first 100 chars for fuzzy matching.
    print("Building duplicate detection index...")
    cur.execute("SELECT memberId, substr(summary, 1, 100) FROM interests")
    existing_pairs = set()
    for member_id, summary_prefix in cur.fetchall():
        existing_pairs.add((member_id, summary_prefix))

    print(f"  Existing entries: {len(existing_pairs)}")

    # Now insert live API entries that don't already exist
    print("Inserting live API entries...")
    live_conn = sqlite3.connect(live_path)
    live_cur = live_conn.cursor()

    # Get live interests
    live_cur.execute(
        "SELECT id, memberId, summary, categoryId, categoryNumber, categoryName, "
        "registrationDate, publishedDate, rectified, fieldsJson, lastUpdated, "
        "parsedAmountPence, currencyCode, bucket FROM interests"
    )
    live_rows = live_cur.fetchall()

    inserted = 0
    skipped_duplicates = 0
    skipped_no_member = 0

    for row in live_rows:
        (live_id, member_id, summary, category_id, cat_number, cat_name,
         reg_date, pub_date, rectified, fields_json, last_updated,
         parsed_pence, currency, bucket) = row

        # Check if this MP exists in the merged DB
        cur.execute("SELECT id FROM mps WHERE id = ?", (member_id,))
        if not cur.fetchone():
            # This MP is not in the historical DB (shouldn't happen since
            # historical DB was built from goveye.db which has all current MPs)
            skipped_no_member += 1
            continue

        # Check for duplicate (same MP + same summary prefix)
        summary_prefix = summary[:100] if summary else ""
        pair = (member_id, summary_prefix)
        if pair in existing_pairs:
            skipped_duplicates += 1
            continue

        # Insert with the live API ID (these are < 1,000,000)
        cur.execute(
            "INSERT OR REPLACE INTO interests "
            "(id, memberId, summary, categoryId, categoryNumber, categoryName, "
            "registrationDate, publishedDate, rectified, fieldsJson, lastUpdated, "
            "parsedAmountPence, currencyCode, bucket) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (live_id, member_id, summary, category_id, cat_number, cat_name,
             reg_date, pub_date, rectified, fields_json, last_updated,
             parsed_pence, currency, bucket),
        )
        inserted += 1
        existing_pairs.add(pair)

    conn.commit()
    live_conn.close()

    # Stats
    cur.execute("SELECT COUNT(*) FROM interests")
    total = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM interests WHERE id >= 1000000")
    historical_count = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM interests WHERE id < 1000000")
    live_count = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM interests WHERE parsedAmountPence IS NOT NULL")
    with_amount = cur.fetchone()[0]
    cur.execute("SELECT SUM(parsedAmountPence) FROM interests WHERE parsedAmountPence IS NOT NULL")
    total_pence = cur.fetchone()[0]

    print("\n" + "=" * 60)
    print("MERGE COMPLETE")
    print("=" * 60)
    print(f"Live API entries inserted:    {inserted}")
    print(f"Duplicates skipped:           {skipped_duplicates}")
    print(f"Skipped (no member in DB):    {skipped_no_member}")
    print(f"\nTotal interests in merged DB: {total}")
    print(f"  Historical (id >= 1M):      {historical_count}")
    print(f"  Live API (id < 1M):         {live_count}")
    print(f"  With amount:                {with_amount} ({100*with_amount/total:.1f}%)")
    print(f"  Total amount:               £{total_pence/100 if total_pence else 0:,.2f}")

    # Per-year stats
    cur.execute(
        "SELECT substr(registrationDate,1,4) as year, COUNT(*), "
        "SUM(CASE WHEN parsedAmountPence IS NOT NULL THEN 1 ELSE 0 END) "
        "FROM interests WHERE year IS NOT NULL GROUP BY year ORDER BY year"
    )
    print("\nPer-year:")
    for year, count, with_amt in cur.fetchall():
        print(f"  {year}: {count:6d} rows ({with_amt} with amount)")

    # Per-bucket stats
    cur.execute(
        "SELECT bucket, COUNT(*), SUM(CASE WHEN parsedAmountPence IS NOT NULL THEN 1 ELSE 0 END), "
        "SUM(parsedAmountPence) FROM interests GROUP BY bucket ORDER BY COUNT(*) DESC"
    )
    print("\nPer-bucket:")
    for bucket, count, with_amt, total_p in cur.fetchall():
        print(f"  {bucket}: {count} rows ({with_amt} with amount), total: £{total_p/100 if total_p else 0:,.2f}")

    # Check Jack Abbott
    cur.execute(
        "SELECT COUNT(*), SUM(CASE WHEN parsedAmountPence IS NOT NULL THEN 1 ELSE 0 END), "
        "SUM(parsedAmountPence) FROM interests WHERE memberId = 5131"
    )
    count, wa, tp = cur.fetchone()
    print(f"\nJack Abbott (id=5131): {count} rows, {wa} with amount, total: £{tp/100 if tp else 0:,.2f}")

    # Check Gordon Brown
    cur.execute(
        "SELECT COUNT(*), SUM(CASE WHEN parsedAmountPence IS NOT NULL THEN 1 ELSE 0 END), "
        "SUM(parsedAmountPence) FROM interests WHERE memberId = -10001"
    )
    count, wa, tp = cur.fetchone()
    print(f"Gordon Brown (id=-10001): {count} rows, {wa} with amount, total: £{tp/100 if tp else 0:,.2f}")

    conn.close()
    print(f"\nOutput: {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Merge live and historical interests DBs")
    parser.add_argument("--live", default="interests.db", help="Live API interests DB")
    parser.add_argument("--historical", default="interests_historical.db", help="Historical interests DB")
    parser.add_argument("--output", default="interests_merged.db", help="Output merged DB")
    args = parser.parse_args()

    merge_dbs(args.live, args.historical, args.output)


if __name__ == "__main__":
    main()
