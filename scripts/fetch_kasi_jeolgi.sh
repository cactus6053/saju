#!/usr/bin/env bash
# KASI 특일정보 API에서 연도별 24절기 시각을 받아 kasi_reference.csv 형식으로 출력
#
# 사용법:
#   .kasi-api-key 파일에 공공데이터포털 일반 인증키(Decoding) 저장 후
#   TOL=15 ./scripts/fetch_kasi_jeolgi.sh 2024 2025 2026 > rows.csv
#
# API 제공 범위: 2000 ~ 2028년 (2025-07 기준)
set -euo pipefail

KEY_FILE="$(dirname "$0")/../.kasi-api-key"
if [[ ! -f "$KEY_FILE" ]]; then
  echo "오류: .kasi-api-key 파일이 없습니다" >&2
  exit 1
fi
KEY=$(cat "$KEY_FILE")
TOL=${TOL:-15}

for YEAR in "$@"; do
  curl -sG "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/get24DivisionsInfo" \
    --data-urlencode "ServiceKey=$KEY" \
    --data-urlencode "solYear=$YEAR" \
    --data-urlencode "numOfRows=30" \
    --data-urlencode "_type=json" \
  | TOL="$TOL" python3 -c '
import json, os, sys

# 연초부터 고정된 24절기 순서. API의 dateName은 일부 연도에서 오류가
# 있어(예: 2000년 우수가 "입춘"으로 표기) 연대순 위치로 이름을 부여한다.
ORDER = ["소한","대한","입춘","우수","경칩","춘분","청명","곡우",
         "입하","소만","망종","하지","소서","대서","입추","처서",
         "백로","추분","한로","상강","입동","소설","대설","동지"]

data = json.load(sys.stdin)
body = data["response"]["body"]
count = body.get("totalCount", 0)
if count != 24:
    print(f"경고: 24건이 아님 — {count}건", file=sys.stderr)
    sys.exit(0)

tol = os.environ["TOL"]
items = sorted(body["items"]["item"], key=lambda it: it["locdate"])
for i, item in enumerate(items):
    name = ORDER[i]
    api_name = item["dateName"]
    locdate = item["locdate"]
    if api_name != name:
        print(f"경고: {locdate} dateName={api_name} → {name}으로 교정", file=sys.stderr)
    date = str(item["locdate"])
    kst = str(item["kst"]).strip()
    iso = f"{date[:4]}-{date[4:6]}-{date[6:8]}T{kst[:2]}:{kst[2:4]}"
    print(f"jeolgi,{date[:4]}:{name},{iso},{tol},KASI API")
'
  sleep 0.3
done
