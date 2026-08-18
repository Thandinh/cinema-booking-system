from __future__ import annotations

"""Render every source-verified PlantUML diagram used by the thesis report."""

import os
import re
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
SOURCE = DOCS / "CinemaBooking_PlantUML_Final.txt"
OUTPUT = DOCS / "thesis_assets" / "diagrams"
PUML_OUTPUT = OUTPUT / "puml"
PLANTUML_JAR = ROOT / "tmp" / "tools" / "plantuml.jar"
GRAPHVIZ_DOT = (
    ROOT
    / "tmp"
    / "tools"
    / "graphviz"
    / "Graphviz-15.1.0-win64"
    / "bin"
    / "dot.exe"
)


def diagram_blocks(source: str) -> list[tuple[str, str]]:
    marker_pattern = re.compile(r"^'\s*\[WORD:\s*Hình\s+(3\.\d+)\]", re.MULTILINE)
    matches = list(marker_pattern.finditer(source))
    blocks: list[tuple[str, str]] = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(source)
        section = source[match.end() : end]
        start_uml = section.find("@startuml")
        end_uml = section.find("@enduml")
        if start_uml < 0 or end_uml < 0:
            raise ValueError(f"Không tìm thấy khối PlantUML cho Hình {match.group(1)}")
        block = section[start_uml : end_uml + len("@enduml")]
        blocks.append((match.group(1), block))
    return blocks


def render() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(SOURCE)
    if not PLANTUML_JAR.exists():
        raise FileNotFoundError(PLANTUML_JAR)
    if not GRAPHVIZ_DOT.exists():
        raise FileNotFoundError(GRAPHVIZ_DOT)

    OUTPUT.mkdir(parents=True, exist_ok=True)
    PUML_OUTPUT.mkdir(parents=True, exist_ok=True)
    blocks = diagram_blocks(SOURCE.read_text(encoding="utf-8"))

    for figure_number, block in blocks:
        filename = f"figure_{figure_number.replace('.', '_')}"
        rendered = OUTPUT / f"{filename}.png"
        if rendered.exists() and rendered.stat().st_size > 0:
            continue
        puml_path = PUML_OUTPUT / f"{filename}.puml"
        puml_path.write_text(block + "\n", encoding="utf-8")

        environment = os.environ.copy()
        environment["GRAPHVIZ_DOT"] = str(GRAPHVIZ_DOT)
        subprocess.run(
            [
                "java",
                "-DPLANTUML_LIMIT_SIZE=16384",
                "-jar",
                str(PLANTUML_JAR),
                "-charset",
                "UTF-8",
                "-tpng",
                "-o",
                str(OUTPUT),
                str(puml_path),
            ],
            check=True,
            env=environment,
            timeout=60,
        )

        generated = OUTPUT / filename / f"{filename}.png"
        if generated.exists() and not rendered.exists():
            generated.replace(rendered)

    missing = [
        number
        for number, _ in blocks
        if not (OUTPUT / f"figure_{number.replace('.', '_')}.png").exists()
    ]
    if missing:
        raise RuntimeError(f"Thiếu ảnh sơ đồ: {', '.join(missing)}")
    print(f"Rendered {len(blocks)} PlantUML diagrams into {OUTPUT}")


if __name__ == "__main__":
    render()
