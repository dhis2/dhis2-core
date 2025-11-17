#!/usr/bin/env python3
"""
Calculate think times from nginx access logs stored in Elasticsearch.

This script:
1. Queries the tracker-local index for all requests
2. Groups requests by session (sessionid_hash)
3. Calculates think time between consecutive requests within each session
4. Indexes results to tracker-think-times index for Kibana visualization

Think time = time_between_requests - response_time_of_first_request
"""

import argparse
import json
import sys
from collections import defaultdict
from datetime import datetime
from typing import List, Dict, Any
from urllib.parse import urlparse

try:
    from elasticsearch import Elasticsearch, helpers
    from elasticsearch.exceptions import ConnectionError, AuthenticationException
except ImportError:
    print("Error: elasticsearch package not found", file=sys.stderr)
    print("Install with: pip install elasticsearch", file=sys.stderr)
    sys.exit(1)


def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="Calculate think times from nginx logs in Elasticsearch"
    )
    parser.add_argument(
        "--es-url",
        default="https://localhost:9200",
        help="Elasticsearch URL (default: https://localhost:9200)",
    )
    parser.add_argument(
        "--es-user",
        default="elastic",
        help="Elasticsearch username (default: elastic)",
    )
    parser.add_argument(
        "--es-password",
        default="elastic123",
        help="Elasticsearch password (default: elastic123)",
    )
    parser.add_argument(
        "--source-index",
        default="tracker-local",
        help="Source index name (default: tracker-local)",
    )
    parser.add_argument(
        "--dest-index",
        default="tracker-think-times",
        help="Destination index name (default: tracker-think-times)",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=500,
        help="Bulk indexing batch size (default: 500)",
    )
    parser.add_argument(
        "--clear-dest",
        action="store_true",
        help="Clear destination index before processing",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be calculated without indexing",
    )
    parser.add_argument(
        "--limit",
        type=int,
        help="Limit number of sessions to process (for testing)",
    )
    return parser.parse_args()


def connect_elasticsearch(url: str, user: str, password: str) -> Elasticsearch:
    """Connect to Elasticsearch with authentication."""
    print(f"Connecting to Elasticsearch at {url}...")

    es = Elasticsearch(
        url,
        basic_auth=(user, password),
        verify_certs=False,  # For local development with self-signed certs
        request_timeout=30,
    )

    try:
        info = es.info()
        print(f"Connected to Elasticsearch {info['version']['number']}")
        return es
    except ConnectionError as e:
        print(f"Error: Cannot connect to Elasticsearch: {e}", file=sys.stderr)
        sys.exit(1)
    except AuthenticationException as e:
        print(f"Error: Authentication failed: {e}", file=sys.stderr)
        sys.exit(1)


def fetch_sessions(es: Elasticsearch, index: str, limit: int = None) -> Dict[str, List[Dict]]:
    """Fetch all requests grouped by session."""
    print(f"Fetching sessions from {index}...")

    query = {
        "query": {
            "bool": {
                "must": [
                    {"exists": {"field": "sessionid_hash"}},
                    {"exists": {"field": "@timestamp"}},
                    {"exists": {"field": "upstream_response_time_ms"}},
                ]
            }
        },
        "sort": [
            {"sessionid_hash.keyword": "asc"},
            {"@timestamp": "asc"}
        ],
        "_source": [
            "@timestamp",
            "sessionid_hash",
            "uri",
            "request_method",
            "upstream_response_time_ms",
            "status"
        ],
        "size": 10000  # Max size per scroll
    }

    sessions = defaultdict(list)
    session_count = 0

    try:
        # Use scroll API for large result sets
        response = es.search(index=index, body=query, scroll="2m")
        scroll_id = response["_scroll_id"]
        hits = response["hits"]["hits"]

        while hits:
            for hit in hits:
                source = hit["_source"]
                session_id = source.get("sessionid_hash")

                if session_id:
                    sessions[session_id].append(source)

            # Check limit
            if limit and len(sessions) >= limit:
                print(f"Reached session limit of {limit}")
                es.clear_scroll(scroll_id=scroll_id)
                break

            # Get next batch
            response = es.scroll(scroll_id=scroll_id, scroll="2m")
            scroll_id = response["_scroll_id"]
            hits = response["hits"]["hits"]

        if not (limit and len(sessions) >= limit):
            es.clear_scroll(scroll_id=scroll_id)

        print(f"Found {len(sessions)} sessions with {sum(len(reqs) for reqs in sessions.values())} total requests")
        return dict(sessions)

    except Exception as e:
        print(f"Error fetching sessions: {e}", file=sys.stderr)
        sys.exit(1)


def calculate_think_times(sessions: Dict[str, List[Dict]]) -> List[Dict[str, Any]]:
    """Calculate think times for all sessions."""
    print("Calculating think times...")

    think_times = []
    total_pairs = 0
    skipped = 0

    for session_id, requests in sessions.items():
        # Sort by timestamp (should already be sorted, but ensure)
        requests.sort(key=lambda r: r["@timestamp"])

        # Need at least 2 requests to calculate think time
        if len(requests) < 2:
            continue

        # Calculate think time for each consecutive pair
        for i in range(len(requests) - 1):
            current = requests[i]
            next_req = requests[i + 1]

            # Parse timestamps (ISO 8601 format)
            try:
                current_time = datetime.fromisoformat(current["@timestamp"].replace("Z", "+00:00"))
                next_time = datetime.fromisoformat(next_req["@timestamp"].replace("Z", "+00:00"))
            except (ValueError, AttributeError) as e:
                skipped += 1
                continue

            # Calculate gap in seconds
            gap_seconds = (next_time - current_time).total_seconds()

            # Get response time in seconds
            response_time_seconds = current.get("upstream_response_time_ms", 0) / 1000.0

            # Think time = gap - response time
            think_time = gap_seconds - response_time_seconds

            # Skip negative think times (concurrent requests or clock issues)
            if think_time < 0:
                skipped += 1
                continue

            # Create think time document
            endpoint_from = current.get("uri", "unknown")
            endpoint_to = next_req.get("uri", "unknown")
            method_from = current.get("request_method", "GET")
            method_to = next_req.get("request_method", "GET")

            think_time_doc = {
                "@timestamp": next_req["@timestamp"],  # Timestamp of second request
                "sessionid_hash": session_id,
                "endpoint_from": endpoint_from,
                "endpoint_to": endpoint_to,
                "request_method_from": method_from,
                "request_method_to": method_to,
                "transition": f"{method_from} {endpoint_from} → {method_to} {endpoint_to}",
                "think_time_seconds": round(think_time, 3),
                "gap_seconds": round(gap_seconds, 3),
                "response_time_seconds": round(response_time_seconds, 3),
            }

            think_times.append(think_time_doc)
            total_pairs += 1

    print(f"Calculated {len(think_times)} think times from {total_pairs} request pairs")
    if skipped > 0:
        print(f"Skipped {skipped} pairs (negative think time or parse errors)")

    return think_times


def create_index_if_not_exists(es: Elasticsearch, index: str):
    """Create the destination index with appropriate mappings."""
    if es.indices.exists(index=index):
        print(f"Index {index} already exists")
        return

    print(f"Creating index {index}...")

    mappings = {
        "mappings": {
            "properties": {
                "@timestamp": {"type": "date"},
                "sessionid_hash": {"type": "keyword"},
                "endpoint_from": {"type": "keyword"},
                "endpoint_to": {"type": "keyword"},
                "request_method_from": {"type": "keyword"},
                "request_method_to": {"type": "keyword"},
                "transition": {"type": "keyword"},
                "think_time_seconds": {"type": "float"},
                "gap_seconds": {"type": "float"},
                "response_time_seconds": {"type": "float"},
            }
        }
    }

    es.indices.create(index=index, body=mappings)
    print(f"Created index {index}")


def bulk_index_think_times(es: Elasticsearch, index: str, think_times: List[Dict], batch_size: int):
    """Bulk index think time documents to Elasticsearch."""
    print(f"Indexing {len(think_times)} documents to {index}...")

    actions = [
        {
            "_index": index,
            "_source": doc
        }
        for doc in think_times
    ]

    success, failed = helpers.bulk(
        es,
        actions,
        chunk_size=batch_size,
        raise_on_error=False,
        stats_only=False
    )

    print(f"Successfully indexed {success} documents")
    if failed:
        print(f"Failed to index {len(failed)} documents", file=sys.stderr)
        for item in failed[:5]:  # Show first 5 failures
            print(f"  {item}", file=sys.stderr)


def show_sample_output(think_times: List[Dict], limit: int = 10):
    """Show sample think time calculations."""
    print("\nSample think time calculations:")
    print("-" * 100)

    for doc in think_times[:limit]:
        print(f"Session: {doc['sessionid_hash'][:12]}...")
        print(f"  Transition: {doc['transition']}")
        print(f"  Think time: {doc['think_time_seconds']}s "
              f"(gap: {doc['gap_seconds']}s - response: {doc['response_time_seconds']}s)")
        print()


def main():
    """Main execution function."""
    args = parse_args()

    # Connect to Elasticsearch
    es = connect_elasticsearch(args.es_url, args.es_user, args.es_password)

    # Clear destination index if requested
    if args.clear_dest and not args.dry_run:
        if es.indices.exists(index=args.dest_index):
            print(f"Deleting existing index {args.dest_index}...")
            es.indices.delete(index=args.dest_index)

    # Fetch sessions from source index
    sessions = fetch_sessions(es, args.source_index, limit=args.limit)

    if not sessions:
        print("No sessions found. Exiting.")
        return

    # Calculate think times
    think_times = calculate_think_times(sessions)

    if not think_times:
        print("No think times calculated. Exiting.")
        return

    # Show sample output
    show_sample_output(think_times, limit=5)

    if args.dry_run:
        print("\nDry run mode - no documents indexed")
        return

    # Create destination index if needed
    create_index_if_not_exists(es, args.dest_index)

    # Bulk index think times
    bulk_index_think_times(es, args.dest_index, think_times, args.batch_size)

    print("\nDone! Think times are now available in Kibana.")
    print(f"Open: http://localhost:5601/app/discover#/?_a=(index:'{args.dest_index}')")


if __name__ == "__main__":
    main()
