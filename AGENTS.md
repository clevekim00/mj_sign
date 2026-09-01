# Project analysis entrypoint

프로젝트 구조나 코드 관계를 분석할 때 전체 저장소를 무작정 읽지 않는다.

1. 먼저 `python3 scripts/project_analysis_harness.py context --changed`를 실행한다.
2. 특정 기능을 분석한다면 변경분 대신 관련 경로를 넘긴다.
   예: `python3 scripts/project_analysis_harness.py context sign/backend/bridge sample/backend/model_server`
3. 출력된 `FILE_INDEX.md`에서 관련 파일을 선택한 후 그 파일과 직접 연결된
   스키마·설정·테스트만 읽는다.
4. 파일을 추가·삭제·변경한 작업의 마지막에는 다음을 실행한다.
   `python3 scripts/project_analysis_harness.py refresh`
5. `python3 scripts/project_analysis_harness.py check`가 통과해야 인덱스가 코드와
   동기화된 것으로 본다.

`FILE_INDEX.md`는 생성 파일이다. 역할 설명이 부족하면 원본 파일의 module
docstring/Javadoc/상단 주석을 개선한 후 인덱스를 다시 생성한다.
