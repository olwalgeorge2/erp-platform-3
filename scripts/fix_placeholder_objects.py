from pathlib import Path
import sys


def normalize_placeholder(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if "Placeholder" not in text:
        return

    lines = text.splitlines()
    package_line = None
    if lines and lines[0].lstrip().startswith("package "):
        package_line = lines[0].rstrip()

    object_line = f"object {path.stem}Placeholder"
    if package_line:
        new_content = f"{package_line}\n\n{object_line}\n"
    else:
        new_content = f"{object_line}\n"

    if text != new_content:
        path.write_text(new_content, encoding="utf-8")


def main() -> None:
    roots = [Path(arg) for arg in sys.argv[1:]] or [Path(".")]
    for root in roots:
        for kt_file in root.rglob("*.kt"):
            normalize_placeholder(kt_file)


if __name__ == "__main__":
    main()
