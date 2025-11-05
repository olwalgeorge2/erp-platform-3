from pathlib import Path
import sys


def normalize(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    normalized = "\n".join(line.rstrip() for line in text.replace("\r\n", "\n").split("\n"))
    normalized = normalized.rstrip("\n") + "\n"
    if normalized != text:
        path.write_text(normalized, encoding="utf-8")


def main() -> None:
    roots = [Path(arg) for arg in sys.argv[1:]] or [Path(".")]
    for root in roots:
        for file_path in root.rglob("*.gradle.kts"):
            normalize(file_path)


if __name__ == "__main__":
    main()
