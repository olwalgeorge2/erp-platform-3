#!/usr/bin/env python3
"""
Utility script to seed placeholder Kotlin tests across modules.

Run from the repository root:

    python scripts/add_mock_tests.py

Use --dry-run to inspect the changes without writing files.
"""

from __future__ import annotations

import argparse
import pathlib
import re
import textwrap
from typing import Iterable


DEFAULT_PACKAGE = "com.erp.testsupport"
EXCLUDED_DIR_NAMES = {"build-logic", ".gradle", ".git"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=pathlib.Path,
        default=pathlib.Path("."),
        help="Repository root (defaults to current directory).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print intended changes without writing files.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite an existing placeholder test if present.",
    )
    parser.add_argument(
        "--package",
        default=DEFAULT_PACKAGE,
        help=f"Kotlin package to use for generated tests (default: {DEFAULT_PACKAGE}).",
    )
    return parser.parse_args()


def discover_modules(root: pathlib.Path) -> Iterable[pathlib.Path]:
    for build_file in root.rglob("build.gradle.kts"):
        module_dir = build_file.parent
        if module_dir == root:
            continue
        if any(part in EXCLUDED_DIR_NAMES for part in module_dir.parts):
            continue
        yield module_dir


def contains_kotlin_sources(module_dir: pathlib.Path) -> bool:
    return (module_dir / "src" / "main" / "kotlin").exists()


def has_meaningful_tests(module_dir: pathlib.Path) -> bool:
    kotlin_tests = module_dir / "src" / "test" / "kotlin"
    if kotlin_tests.exists():
        for candidate in kotlin_tests.rglob("*.kt"):
            try:
                if candidate.stat().st_size > 0:
                    return True
            except FileNotFoundError:
                # Raced with file removal; treat as absent and continue scanning.
                continue

    java_tests = module_dir / "src" / "test" / "java"
    if java_tests.exists():
        for candidate in java_tests.rglob("*.java"):
            try:
                if candidate.stat().st_size > 0:
                    return True
            except FileNotFoundError:
                continue

    return False


def slug_to_class_name(slug: str) -> str:
    tokens = [segment for segment in re.split(r"[^A-Za-z0-9]+", slug) if segment]
    if not tokens:
        return "Placeholder"
    compressed: list[str] = []
    for token in tokens:
        if compressed and compressed[-1].lower() == token.lower():
            continue
        compressed.append(token)
    # Keep only the tail end to avoid excessively long class names.
    compressed = compressed[-6:]
    return "".join(token[:1].upper() + token[1:] for token in compressed)


def build_test_content(class_name: str, package: str) -> str:
    return textwrap.dedent(
        f"""\
        package {package}

        import org.junit.jupiter.api.Assertions.assertTrue
        import org.junit.jupiter.api.Test

        class {class_name}MockTest {{
            @Test
            fun placeholder() {{
                assertTrue(true, "Placeholder test to keep the Gradle test task green until real tests exist.")
            }}
        }}
        """
    )


def ensure_placeholder_test(
    module_dir: pathlib.Path,
    module_slug: str,
    package: str,
    dry_run: bool,
    force: bool,
) -> tuple[bool, pathlib.Path | None]:
    class_name = slug_to_class_name(module_slug)
    test_root = module_dir / "src" / "test" / "kotlin"
    target_dir = test_root.joinpath(*package.split("."))
    target_file = target_dir / f"{class_name}MockTest.kt"

    if target_file.exists() and not force:
        return False, target_file

    if dry_run:
        return True, target_file

    target_dir.mkdir(parents=True, exist_ok=True)
    target_file.write_text(build_test_content(class_name, package), encoding="utf-8")
    return True, target_file


def main() -> None:
    args = parse_args()
    root = args.root.resolve()

    if not (root / "settings.gradle.kts").exists():
        raise SystemExit(f"{root} does not look like the ERP platform repo (settings.gradle.kts missing).")

    created = []
    skipped = []
    for module_dir in discover_modules(root):
        if not contains_kotlin_sources(module_dir):
            continue
        if has_meaningful_tests(module_dir) and not args.force:
            continue
        module_rel = module_dir.relative_to(root)

        wrote, file_path = ensure_placeholder_test(
            module_dir=module_dir,
            module_slug=module_rel.as_posix(),
            package=args.package,
            dry_run=args.dry_run,
            force=args.force,
        )

        if wrote:
            created.append((module_rel, file_path.relative_to(root) if file_path else None))
        else:
            skipped.append(module_rel)

    if not created and not args.dry_run:
        print("No placeholder tests were required.")
        return

    action = "Would create" if args.dry_run else "Created"
    for module_rel, file_rel in created:
        if file_rel is None:
            continue
        print(f"{action}: {file_rel}")

    if skipped:
        print("Skipped (existing placeholder present):")
        for module_rel in skipped:
            print(f"  - {module_rel}")


if __name__ == "__main__":
    main()
