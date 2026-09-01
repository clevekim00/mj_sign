#!/usr/bin/env python3
"""Generate and select compact per-folder file indexes for repository analysis."""

from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INDEX_NAME = "FILE_INDEX.md"
INDEX_DIRECTORIES = (
    ".",
    "sign/embedded",
    "sign/backend",
    "sign/common",
    "sample/embedded",
    "sample/backend",
    "docs/embedded",
    "docs/backend",
    "scripts",
)
SKIP_DIRS = {
    ".git", ".gradle", ".kotlin", ".dart_tool", ".idea", ".venv", "__pycache__",
    "build", "dist", "node_modules", "Pods", "graphify-out", "coverage",
    "ephemeral", ".plugin_symlinks",
}
SKIP_FILES = {".DS_Store", INDEX_NAME}
BINARY_SUFFIXES = {
    ".zip", ".jar", ".class", ".png", ".jpg", ".jpeg", ".gif", ".webp",
    ".ico", ".pdf", ".so", ".dylib", ".a", ".framework",
}
DECLARATION_PATTERNS = (
    re.compile(r"^\s*(?:public\s+)?(?:abstract\s+|final\s+)?(?:class|interface|record|enum)\s+([A-Za-z_]\w*)", re.M),
    re.compile(r"^\s*(?:export\s+)?(?:default\s+)?(?:class|function|interface|type|const)\s+([A-Za-z_$][\w$]*)", re.M),
    re.compile(r"^\s*(?:async\s+)?def\s+([A-Za-z_]\w*)", re.M),
    re.compile(r"^\s*class\s+([A-Za-z_]\w*)", re.M),
    re.compile(r"^\s*(?:abstract\s+)?class\s+([A-Za-z_]\w*)", re.M),
    re.compile(r"^\s*message\s+([A-Za-z_]\w*)", re.M),
)


def is_skipped(path: Path) -> bool:
    try:
        relative = path.relative_to(ROOT)
    except ValueError:
        return True
    if any(part in SKIP_DIRS for part in relative.parts):
        return True
    current = path
    while current != ROOT:
        if current.is_symlink():
            return True
        current = current.parent
    return False


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()[:12]


def readable_text(path: Path) -> str:
    if path.suffix.lower() in BINARY_SUFFIXES:
        return ""
    try:
        return path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def first_meaningful_line(text: str) -> str:
    in_frontmatter = False
    for raw in text.splitlines():
        if raw.startswith("#!"):
            continue
        line = raw.strip()
        if line == "---":
            in_frontmatter = not in_frontmatter
            continue
        if in_frontmatter or not line:
            continue
        line = re.sub(r"^(?:#+|//+|/\*+|\*+|<!--|#)\s*", "", line)
        line = re.sub(r"(?:\*/|-->)$", "", line).strip()
        if line and not line.startswith(("import ", "package ", "from ", "syntax =")):
            return line[:180]
    return ""


def declarations(text: str) -> list[str]:
    found: list[str] = []
    for pattern in DECLARATION_PATTERNS:
        for name in pattern.findall(text):
            if name not in found:
                found.append(name)
            if len(found) == 8:
                return found
    return found


def fallback_purpose(path: Path) -> str:
    name = path.name.lower()
    suffix = path.suffix.lower()
    stem_words = " ".join(
        word.lower()
        for word in re.findall(r"[A-Z]+(?=[A-Z][a-z]|\b)|[A-Z]?[a-z]+|\d+", path.stem)
    ) or path.stem.replace("_", " ")
    if name.startswith("readme"):
        return "이 폴더의 책임, 사용법, 의존 관계를 설명한다."
    if name in {"application.yml", "application.yaml"}:
        return "Spring 애플리케이션의 기본 런타임 설정을 정의한다."
    if "docker-compose" in name:
        return "로컬 통합 환경의 컨테이너와 네트워크 구성을 정의한다."
    if name.startswith("dockerfile"):
        return "서비스 실행용 컨테이너 이미지를 구성한다."
    if name.endswith(".proto"):
        return "클라이언트·서버 사이의 Protobuf 메시지 계약을 정의한다."
    if name.endswith("test.java") or name.startswith("test_") or "_test." in name:
        return "해당 기능의 동작과 회귀 조건을 자동 검증한다."
    if suffix in {".yml", ".yaml", ".properties", ".json", ".toml"}:
        return "빌드 또는 런타임 구성을 선언한다."
    if suffix in {".sh", ".command"}:
        return "개발·검증 작업을 자동 실행하는 명령 스크립트다."
    if suffix in {".md", ".txt"}:
        return "설계, 사용법 또는 운영 지식을 문서화한다."
    if suffix in {".lock"}:
        return "재현 가능한 의존성 버전을 고정한다."
    role_by_name = (
        ("websockethandler", "WebSocket 수어 스트림의 수신, 검증과 응답 전송을 처리한다."),
        ("controller", f"{stem_words} HTTP API endpoint를 제공한다."),
        ("validator", f"{stem_words} 입력 또는 응답 계약을 검증한다."),
        ("gateway", f"{stem_words} 외부 추론 provider 경계를 구현한다."),
        ("transport", f"{stem_words} 전송 계층과 요청·응답 상관관계를 구현한다."),
        ("consumer", f"{stem_words} 브로커 메시지를 소비하고 결과를 발행한다."),
        ("adapter", f"{stem_words} 외부 시스템 연동 규격을 내부 인터페이스로 변환한다."),
        ("client", f"{stem_words} 외부 서비스 호출을 담당한다."),
        ("properties", f"{stem_words} 런타임 설정 값을 바인딩한다."),
        ("config", f"{stem_words} 컴포넌트와 인프라 설정을 구성한다."),
        ("service", f"{stem_words} 핵심 업무 흐름을 조정한다."),
        ("scheduler", f"{stem_words} 예약 실행과 시간 기반 처리를 담당한다."),
        ("repository", f"{stem_words} 데이터 저장·조회 경계를 제공한다."),
        ("test", f"{stem_words} 동작과 회귀 조건을 자동 검증한다."),
    )
    compact = re.sub(r"[^a-z0-9]", "", name)
    for marker, purpose in role_by_name:
        if marker in compact:
            return purpose
    if suffix in {".java", ".kt", ".dart", ".py", ".ts", ".tsx", ".js"}:
        return f"{stem_words} 기능과 관련 타입을 구현한다."
    return f"{stem_words} 구현 또는 지원 요소를 제공한다."


def describe(path: Path) -> tuple[str, list[str]]:
    text = readable_text(path)
    names = declarations(text)
    line = first_meaningful_line(text)
    looks_like_code = (
        line.startswith(("@", "class ", "interface ", "record ", "enum ", "def ", "export ", "const ", "function "))
        or line.endswith(("{", "};"))
        or line in {"#!/usr/bin/env python3", "#!/bin/sh", "!/bin/sh"}
    )
    if line and len(line) >= 8 and not looks_like_code:
        purpose = line
    else:
        purpose = fallback_purpose(path)
    return purpose.replace("|", "\\|"), names


def indexed_files(directory: Path) -> list[Path]:
    if directory == ROOT:
        candidates = directory.iterdir()
    else:
        candidates = directory.rglob("*")
    return sorted(
        (
            path for path in candidates
            if path.is_file()
            and path.name not in SKIP_FILES
            and not path.name.startswith(".DS_Store")
            and not is_skipped(path)
        ),
        key=lambda path: path.relative_to(directory).as_posix().lower(),
    )


def candidate_directories() -> list[Path]:
    return [ROOT / relative for relative in INDEX_DIRECTORIES if (ROOT / relative).is_dir()]


def render_index(directory: Path) -> str:
    relative_dir = directory.relative_to(ROOT)
    label = "." if relative_dir == Path(".") else relative_dir.as_posix()
    files = indexed_files(directory)
    lines = [
        "<!-- GENERATED by scripts/project_analysis_harness.py; DO NOT EDIT BY HAND. -->",
        f"# Folder index: `{label}`",
        "",
        "이 문서는 프로젝트 분석 시 이 폴더의 파일 역할을 빠르게 선택하기 위한 인덱스다.",
        "내용 해시가 바뀌면 `python3 scripts/project_analysis_harness.py refresh`로 갱신한다.",
        "",
        "| 파일 | 기능 | 주요 선언 | SHA-256 |",
        "|---|---|---|---|",
    ]
    for path in files:
        purpose, names = describe(path)
        declared = ", ".join(f"`{name}`" for name in names) if names else "—"
        display_path = path.relative_to(directory).as_posix()
        lines.append(f"| `{display_path}` | {purpose} | {declared} | `{digest(path)}` |")
    child_indexes = sorted(
        child.relative_to(directory).as_posix()
        for child in candidate_directories()
        if child != directory and directory in child.parents
    )
    if child_indexes:
        lines.extend(["", "## 하위 인덱스", ""])
        lines.extend(f"- [`{child}/{INDEX_NAME}`]({child}/{INDEX_NAME})" for child in child_indexes)
    lines.extend([
        "",
        "## 분석 규칙",
        "",
        "1. 먼저 이 인덱스에서 관련 파일을 고른다.",
        "2. 선택한 파일과 직접 연결된 계약·테스트만 추가로 읽는다.",
        "3. 구현 변경 후 `check`가 실패하면 인덱스를 다시 생성한다.",
        "",
    ])
    return "\n".join(lines)


def refresh() -> int:
    directories = candidate_directories()
    expected = {directory / INDEX_NAME for directory in directories}
    for index in ROOT.rglob(INDEX_NAME):
        if index in expected or is_skipped(index):
            continue
        try:
            first_line = index.read_text(encoding="utf-8").splitlines()[0]
        except (OSError, IndexError):
            continue
        if first_line == "<!-- GENERATED by scripts/project_analysis_harness.py; DO NOT EDIT BY HAND. -->":
            index.unlink()
    for directory in directories:
        (directory / INDEX_NAME).write_text(render_index(directory), encoding="utf-8")
    print(f"Refreshed {len(directories)} folder indexes.")
    return 0


def check() -> int:
    failures: list[str] = []
    expected_dirs = candidate_directories()
    for directory in expected_dirs:
        index = directory / INDEX_NAME
        expected = render_index(directory)
        if not index.exists():
            failures.append(f"missing: {index.relative_to(ROOT)}")
        elif index.read_text(encoding="utf-8") != expected:
            failures.append(f"stale: {index.relative_to(ROOT)}")
    for index in ROOT.rglob(INDEX_NAME):
        if not is_skipped(index) and index.parent not in expected_dirs:
            failures.append(f"orphan: {index.relative_to(ROOT)}")
    if failures:
        print("\n".join(failures), file=sys.stderr)
        print("Run: python3 scripts/project_analysis_harness.py refresh", file=sys.stderr)
        return 1
    print(f"Verified {len(expected_dirs)} folder indexes.")
    return 0


def git_changed_paths() -> list[str]:
    result = subprocess.run(
        ["git", "status", "--porcelain=v1", "-uall"],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=True,
    )
    paths: list[str] = []
    for line in result.stdout.splitlines():
        raw = line[3:]
        if " -> " in raw:
            raw = raw.split(" -> ", 1)[1]
        if raw and not raw.endswith(INDEX_NAME):
            paths.append(raw)
    return paths


def select_indexes(paths: list[str]) -> list[Path]:
    selected: list[Path] = [ROOT / INDEX_NAME]
    seen = {ROOT / INDEX_NAME}
    for raw in paths:
        target = (ROOT / raw).resolve()
        directory = target if target.is_dir() else target.parent
        while directory == ROOT or ROOT in directory.parents:
            index = directory / INDEX_NAME
            if index.exists() and index not in seen:
                selected.append(index)
                seen.add(index)
            if directory == ROOT:
                break
            directory = directory.parent
    return selected


def context(paths: list[str], changed: bool, budget: int) -> int:
    requested = list(paths)
    if changed:
        requested.extend(git_changed_paths())
    if not requested:
        requested = ["README.md"]
    indexes = select_indexes(requested)
    remaining = max(1000, budget)
    for index in indexes:
        content = index.read_text(encoding="utf-8")
        block = f"\n<!-- {index.relative_to(ROOT)} -->\n{content}\n"
        if len(block) > remaining and index != indexes[0]:
            selected_relative_paths: list[str] = []
            for raw in requested:
                target = (ROOT / raw).resolve()
                try:
                    relative = target.relative_to(index.parent.resolve()).as_posix()
                except ValueError:
                    continue
                selected_relative_paths.append(relative)
            matching_rows = [
                line for line in content.splitlines()
                if line.startswith("| `")
                and any(
                    line.startswith(f"| `{relative}` |")
                    or line.startswith(f"| `{relative.rstrip('/')}/")
                    for relative in selected_relative_paths
                )
            ]
            if matching_rows:
                block = "\n".join([
                    f"\n<!-- compact {index.relative_to(ROOT)} -->",
                    f"# Selected files: `{index.parent.relative_to(ROOT)}`",
                    "",
                    "| 파일 | 기능 | 주요 선언 | SHA-256 |",
                    "|---|---|---|---|",
                    *matching_rows,
                    "",
                ])
            else:
                print(f"\n<!-- budget reached; skipped {index.relative_to(ROOT)} -->")
                continue
        print(block)
        remaining -= len(block)
    print("\n## Requested paths")
    for path in sorted(set(requested)):
        print(f"- `{path}`")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("refresh", help="regenerate every FILE_INDEX.md")
    subparsers.add_parser("check", help="fail when an index is missing or stale")
    context_parser = subparsers.add_parser("context", help="print only relevant folder indexes")
    context_parser.add_argument("paths", nargs="*", help="repository-relative files or folders")
    context_parser.add_argument("--changed", action="store_true", help="include current Git changes")
    context_parser.add_argument("--budget", type=int, default=30000, help="maximum output characters")
    args = parser.parse_args()
    if args.command == "refresh":
        return refresh()
    if args.command == "check":
        return check()
    return context(args.paths, args.changed, args.budget)


if __name__ == "__main__":
    raise SystemExit(main())
