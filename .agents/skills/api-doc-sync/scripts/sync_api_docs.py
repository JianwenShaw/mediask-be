#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path


HTTP_METHODS = {"get", "post", "put", "delete", "patch", "options", "head"}
PROJECT_ROOT = Path(__file__).resolve().parents[4]
CONTROLLER_GLOB = "mediask-api/src/main/java/me/jianwen/mediask/api/controller/*Controller.java"
OPENAPI_PATH = PROJECT_ROOT / "api-docs/openapi.json"
README_PATH = PROJECT_ROOT / "api-docs/README.md"


@dataclass(frozen=True)
class Endpoint:
    method: str
    path: str

    def key(self) -> str:
        return f"{self.method} {self.path}"


def collect_controller_endpoints() -> set[Endpoint]:
    endpoints: set[Endpoint] = set()
    request_mapping_pattern = re.compile(r'@RequestMapping\("([^"]+)"\)')
    method_mapping_pattern = re.compile(
        r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(?:\("([^"]*)"\))?'
    )

    for file_path in sorted(PROJECT_ROOT.glob(CONTROLLER_GLOB)):
        base_path = ""
        text = file_path.read_text(encoding="utf-8")
        for line in text.splitlines():
            request_match = request_mapping_pattern.search(line)
            if request_match:
                base_path = request_match.group(1)
            method_match = method_mapping_pattern.search(line)
            if not method_match:
                continue
            method = method_match.group(1).replace("Mapping", "").upper()
            sub_path = method_match.group(2) if method_match.group(2) is not None else ""
            if sub_path == "":
                full_path = base_path
            elif sub_path.startswith("/"):
                full_path = base_path + sub_path
            else:
                full_path = f"{base_path}/{sub_path}"
            endpoints.add(Endpoint(method=method, path=full_path))
    return endpoints


def collect_openapi_endpoints() -> set[Endpoint]:
    data = json.loads(OPENAPI_PATH.read_text(encoding="utf-8"))
    endpoints: set[Endpoint] = set()
    for path, path_item in data.get("paths", {}).items():
        if not isinstance(path, str) or not path.startswith("/"):
            continue
        if not isinstance(path_item, dict):
            continue
        for method in path_item.keys():
            if method.lower() in HTTP_METHODS:
                endpoints.add(Endpoint(method=method.upper(), path=path))
    return endpoints


def module_count(endpoints: set[Endpoint], prefix: str) -> int:
    return sum(1 for endpoint in endpoints if endpoint.path.startswith(prefix))


def expected_readme_values(controller_endpoints: set[Endpoint]) -> dict[str, int]:
    return {
        "排班": module_count(controller_endpoints, "/api/v1/schedules"),
        "预约": module_count(controller_endpoints, "/api/v1/appointments"),
    }


def replace_count_line(content: str, module_name: str, count: int) -> tuple[str, bool]:
    pattern = re.compile(rf"^\| {module_name} \| \d+ \|", re.MULTILINE)
    replacement = f"| {module_name} | {count} |"
    new_content = pattern.sub(replacement, content, count=1)
    return new_content, new_content != content


def replace_module_header_count(
    content: str, english_module_name: str, chinese_name: str, count: int
) -> tuple[str, bool]:
    pattern = re.compile(rf"^### {english_module_name} {chinese_name}模块 \(\d+ 接口\)$", re.MULTILINE)
    replacement = f"### {english_module_name} {chinese_name}模块 ({count} 接口)"
    new_content = pattern.sub(replacement, content, count=1)
    return new_content, new_content != content


def sync_readme_content(content: str, controller_endpoints: set[Endpoint]) -> tuple[str, list[str]]:
    changes: list[str] = []
    expected_counts = expected_readme_values(controller_endpoints)
    updated = content

    updated, changed = replace_count_line(updated, "排班", expected_counts["排班"])
    if changed:
        changes.append("更新 README 快速索引的排班接口数量")
    updated, changed = replace_count_line(updated, "预约", expected_counts["预约"])
    if changed:
        changes.append("更新 README 快速索引的预约接口数量")

    updated, changed = replace_module_header_count(updated, "Schedule", "排班", expected_counts["排班"])
    if changed:
        changes.append("更新 Schedule 模块标题的接口数量")
    updated, changed = replace_module_header_count(updated, "Appointment", "预约", expected_counts["预约"])
    if changed:
        changes.append("更新 Appointment 模块标题的接口数量")

    replacements = {
        "/api/v1/schedules/{id}/close": "/api/v1/schedules/{scheduleId}/close",
        "/api/v1/schedules/{id}/open": "/api/v1/schedules/{scheduleId}/open",
        "/api/v1/schedules/{id}/slots": "/api/v1/schedules/{scheduleId}/slots",
        "/api/v1/schedules/{id}": "/api/v1/schedules/{scheduleId}",
        "/api/v1/appointments/{id}/pay": "/api/v1/appointments/{appointmentId}/pay",
        "/api/v1/appointments/{id}/visited": "/api/v1/appointments/{appointmentId}/visited",
        "/api/v1/appointments/{id}/absent": "/api/v1/appointments/{appointmentId}/absent",
        "/api/v1/appointments/{id}": "/api/v1/appointments/{appointmentId}",
    }
    for source, target in replacements.items():
        if source in updated:
            updated = updated.replace(source, target)
            changes.append(f"替换 README 路径参数占位符: {source} -> {target}")

    return updated, changes


def compare_endpoints(controller: set[Endpoint], openapi: set[Endpoint]) -> tuple[list[str], list[str]]:
    controller_keys = {endpoint.key() for endpoint in controller}
    openapi_keys = {endpoint.key() for endpoint in openapi}
    missing_in_openapi = sorted(controller_keys - openapi_keys)
    missing_in_controller = sorted(openapi_keys - controller_keys)
    return missing_in_openapi, missing_in_controller


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync and validate API docs against controllers.")
    parser.add_argument("--check", action="store_true", help="Only check, do not modify files.")
    parser.add_argument("--write", action="store_true", help="Write deterministic README fixes.")
    args = parser.parse_args()

    if not args.check and not args.write:
        parser.error("必须指定 --check 或 --write")

    controller_endpoints = collect_controller_endpoints()
    openapi_endpoints = collect_openapi_endpoints()
    missing_in_openapi, missing_in_controller = compare_endpoints(controller_endpoints, openapi_endpoints)

    has_error = False
    if missing_in_openapi:
        has_error = True
        print("❌ openapi.json 缺少以下接口：")
        for item in missing_in_openapi:
            print(f"  - {item}")
    if missing_in_controller:
        has_error = True
        print("❌ openapi.json 存在代码中已不存在的接口：")
        for item in missing_in_controller:
            print(f"  - {item}")

    readme_before = README_PATH.read_text(encoding="utf-8")
    readme_after, changes = sync_readme_content(readme_before, controller_endpoints)

    if args.write and changes:
        README_PATH.write_text(readme_after, encoding="utf-8")
        print("✅ 已写入 README 修复：")
        for change in changes:
            print(f"  - {change}")
    elif args.check and changes:
        has_error = True
        print("❌ README.md 存在可确定修复的问题：")
        for change in changes:
            print(f"  - {change}")
        print("  运行 `python3 skills/api-doc-sync/scripts/sync_api_docs.py --write` 自动修复")
    else:
        print("✅ README.md 与规则一致")

    if not has_error:
        print("✅ 校验通过：Controller / OpenAPI / README 已同步")
        return 0
    print("❌ 校验失败：请先同步文档后重试")
    return 1


if __name__ == "__main__":
    sys.exit(main())
