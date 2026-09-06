# Dual Mode Sign Recognition Codex Automation Blueprint
> Created: 2026-09-05
> Purpose: Codex implementation blueprint

## 0. Goals and Deliverables

### Primary Goal
내장형과 서버 연결형 수어 인식을 하나의 공통 계약으로 설계·구현·검증한다.

### Success Definition
- 동일한 landmark 입력과 결과 타입을 두 실행 모드가 사용한다.
- 설계 문서, 구현, 단위 테스트와 실행 가능한 전환 샘플이 함께 제공된다.
- 공식 모델 자산이 없을 때 실제 모델로 오인하거나 silently fallback하지 않는다.

### Out of Scope
- 비공개 SignGemma weight 제작 또는 배포
- 실제 사용자 데이터 수집과 모델 품질 인증
- 운영 TLS 인증서와 외부 identity provider 구축

## 1. Working Context

### Background
기존 SDK는 embedded 폴더에 있지만 WebSocket backend client에 직접 결합되어 두
실행 방식을 일관되게 선택할 경계가 부족하다.

### Objective
Codex가 인덱스 기반으로 관련 파일만 분석하고 공통 엔진 경계, adapter, 샘플,
문서와 테스트를 동기화한다.

### Scope
- Included: Flutter engine API, backend Protocol v2 adapter, external model adapter, 문서와 테스트
- Excluded: 모델 학습, checkpoint 저장, 클라우드 운영 인프라 배포

### Inputs
| Item | Format | Source | Notes |
|---|---|---|---|
| 프로젝트 인덱스 | md | `FILE_INDEX.md` | 분석 대상 선택 |
| landmark 계약 | proto | `sign/common/schema` | canonical input/result |
| 모델 자산 | file/API | external provider | 저장소 밖에서 주입 |

### Outputs
| Item | Format | Destination | Notes |
|---|---|---|---|
| 아키텍처 설계 | md | `docs/DUAL_MODE_SIGN_RECOGNITION_DESIGN_KO.md` | 제품 설계 |
| 공통 엔진 | dart | `sign/embedded/lib/src` | embedded/backend adapter |
| 전환 샘플 | dart | `sample/embedded/flutter_app` | 동일 UI 비교 |
| 검증 결과 | test output | local/CI | Flutter/Python/Spring |

### Constraints
- 기존 backend API 하위 호환성을 유지한다.
- 모델 weight와 개인정보성 데이터는 Git에 넣지 않는다.
- 실제 모델이 없으면 mock/demo임을 명시하고 real readiness는 실패해야 한다.

### Terms
| Term | Definition |
|---|---|
| Embedded | 네트워크 없이 앱 프로세스 또는 native runtime에서 실행하는 인식 |
| Backend | WebSocket으로 SignBridge에 landmark를 전송하는 인식 |
| Engine | 입력·결과·생명주기를 통합하는 SDK 경계 |

## 2. Workflow Definition

### End-to-End Flow
`[Requirements] -> [Index Analysis] -> [Architecture] -> [Implementation] -> [Validation] -> [Final Output]`

### LLM vs Code Boundary
| LLM handles | Code handles |
|---|---|
| 구조 판단, 위험 분류, 문서 요약, 누락 판단 | 파일 탐색, protobuf, API 호출, format, test, index hash 검증 |

#### Step 01: Analyze and Plan
1) Step Goal:
두 실행 경로의 결합과 계약 공백을 식별한다.

2) Input / Output:
- Input: folder indexes, source, tests
- Output: 우선순위 개선 목록

3) LLM Decision Area:
호환성, 보안, 모델 경계의 우선순위를 판단한다.

4) Code Processing Area:
하네스가 관련 index와 변경 파일을 결정적으로 선택한다.

5) Success Criteria:
embedded/backend/common 책임과 P0 결함이 식별된다.

6) Validation Method:
인덱스 hash와 실제 선언을 대조한다.

7) Failure Handling:
인덱스가 stale이면 한 번 refresh한 뒤 실패 시 중단한다.

8) Skills / Scripts:
- Skill: graphify
- Script: `scripts/project_analysis_harness.py`

9) Intermediate Artifact Rule:
`output/step01_gap_analysis.md`

#### Step 02: Design Contracts
1) Step Goal:
공통 엔진 API, 상태, 폴더와 adapter 경계를 설계한다.

2) Input / Output:
- Input: 개선 목록과 canonical proto
- Output: 이중 모드 설계 문서

3) LLM Decision Area:
공개 API와 migration 정책을 결정한다.

4) Code Processing Area:
blueprint validator가 필수 문서 구조를 검사한다.

5) Success Criteria:
입력·결과·상태·실패·보안·테스트 계약이 정의된다.

6) Validation Method:
설계 checklist와 blueprint structural validation을 사용한다.

7) Failure Handling:
필수 section 누락은 수정 후 최대 두 번 재검증한다.

8) Skills / Scripts:
- Skill: blueprint
- Script: `validate_blueprint_doc.py`

9) Intermediate Artifact Rule:
`output/step02_architecture.md`

#### Step 03: Implement Modes and Samples
1) Step Goal:
공통 엔진, 두 adapter, v2 client와 전환 샘플을 구현한다.

2) Input / Output:
- Input: 설계 문서
- Output: Dart/Python source와 sample

3) LLM Decision Area:
기존 API를 보존하는 최소 변경 지점을 선택한다.

4) Code Processing Area:
queue, event stream, protobuf envelope와 환경변수 처리를 구현한다.

5) Success Criteria:
network-free embedded와 backend engine을 같은 widget이 소비한다.

6) Validation Method:
formatter, analyzer와 unit test를 실행한다.

7) Failure Handling:
컴파일 실패는 관련 package 안에서 수정하고, 외부 model 부재는 adapter 상태로 보고한다.

8) Skills / Scripts:
- Skill: none
- Script: `scripts/regenerate_protobuf.sh`

9) Intermediate Artifact Rule:
`output/step03_implementation_summary.md`

#### Step 04: Validate and Handoff
1) Step Goal:
모든 계약과 회귀 테스트를 검증하고 남은 외부 의존성을 분리한다.

2) Input / Output:
- Input: 변경된 source, docs, samples
- Output: 검증 결과와 handoff

3) LLM Decision Area:
실패가 코드 결함인지 외부 자산 부재인지 판별한다.

4) Code Processing Area:
Flutter, Python, Gradle test와 analysis index check를 실행한다.

5) Success Criteria:
자동화된 테스트가 통과하고 실제 모델 미완료 범위가 명시된다.

6) Validation Method:
각 runtime의 공식 test runner와 `git diff --check`를 사용한다.

7) Failure Handling:
재현 가능한 실패는 수정하고, 권한·모델 자산 문제는 `NEEDS_USER_INPUT`으로 전환한다.

8) Skills / Scripts:
- Skill: none
- Script: `scripts/project_analysis_harness.py`

9) Intermediate Artifact Rule:
`output/step04_validation.md`

### State Model
| State | Entry Condition | Exit Condition | Next State |
|---|---|---|---|
| `COLLECTING_REQUIREMENTS` | 요청 수신 | 범위와 가정 확정 | `PLANNING` |
| `PLANNING` | 분석 시작 | 설계 승인 가능한 상태 | `RUNNING_SCRIPT` |
| `RUNNING_SCRIPT` | 구현·검증 실행 | 실행 결과 확보 | `VALIDATING` 또는 `FAILED` |
| `VALIDATING` | 산출물 존재 | 모든 검사 통과 | `DONE`, `NEEDS_USER_INPUT`, `FAILED` |
| `NEEDS_USER_INPUT` | 외부 모델·권한 필요 | 사용자가 자산 제공 | `PLANNING` 또는 `DONE` |
| `DONE` | 완료 기준 충족 | Terminal | none |
| `FAILED` | 자동 복구 불가 | Terminal | none |

## 3. Implementation Spec

### Recommended Folder Structure
```text
/project-root
  AGENTS.md
  /sign/common
  /sign/embedded
  /sign/backend
  /sample/embedded
  /sample/backend
  /docs
  /scripts
  /output
```

### AGENTS.md Responsibilities
- 분석 전 folder index 하네스를 실행한다.
- 모델 weight와 개인정보를 저장소에 추가하지 않는다.
- 변경 후 index refresh/check와 관련 runtime test를 실행한다.

### Custom Agent Definitions
| Name | Path | Role | Required Fields |
|---|---|---|---|
| none | none | 단일 Codex agent와 script로 충분 | none |

### Skill and Script Inventory
| Name | Type | Role | Trigger Condition |
|---|---|---|---|
| blueprint | skill | 설계 workflow 구조 검증 | 설계서 생성 시 |
| graphify | skill | 코드 관계 분석 | 아키텍처 분석 시 |
| project-analysis-harness | script | 관련 파일 index 선택·검증 | 모든 분석·변경 시 |

### Skill Creation Rules

> 이 설계서에 정의된 모든 스킬은 구현 시 반드시 `skill-creator` 스킬(`/skill-creator`)을 사용하여 생성할 것.
> 직접 SKILL.md를 수동 작성하지 말 것 — 규격 불일치 및 트리거 실패의 원인이 됨.

이번 구현은 새 스킬을 정의하지 않는다. 향후 스킬을 추가하면 `.agents/skills/<skill-name>/`에 생성하고 skill-creator로 검증한다.

### Core Artifacts
| Path | Format | Producer | Purpose |
|---|---|---|---|
| `output/step01_gap_analysis.md` | md | Step 01 | 개선 근거 |
| `output/step02_architecture.md` | md | Step 02 | 계약 결정 |
| `output/step03_implementation_summary.md` | md | Step 03 | 변경 추적 |
| `output/step04_validation.md` | md | Step 04 | 테스트 증거 |

## 4. Validation Checklist

- [ ] Every workflow step has all 9 required fields
- [ ] Intermediate artifacts use the `output/stepNN_<name>.<ext>` rule
- [ ] LLM vs code responsibilities are separated clearly
- [ ] Embedded mode performs no network call
- [ ] Backend mode uses Protocol v2 and explicit EOS
- [ ] Existing backend widget calls remain compatible
- [ ] Real model absence is fail-fast and visible
- [ ] Folder indexes and tests pass
