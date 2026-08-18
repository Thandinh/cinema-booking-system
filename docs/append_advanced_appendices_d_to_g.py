"""Append the selected source-traceable technical appendices to the current report.

The report already contains the formal appendices A-C.  The generator also keeps
the technical D-G content, so this utility renders that content once in a clean
document and copies only the D-G XML elements into the audited report.  This
avoids regenerating or reformatting the chapters that have already been checked.
"""

from __future__ import annotations

from copy import deepcopy
from pathlib import Path
import shutil
import zipfile

from docx import Document
from docx.oxml.ns import qn

from generate_final_thesis_from_source import (
    add_advanced_appendices_d_to_g,
    configure_document,
)


ROOT = Path(__file__).resolve().parents[1]
BASE_REPORT = ROOT / "docs" / "Bao_cao_khoa_luan_CinemaBooking_Final_Updated_References_Audited.docx"
REPORT = ROOT / "docs" / "Bao_cao_khoa_luan_CinemaBooking_Final_Updated_Showtime_Checkin.docx"
OUTPUT = ROOT / "docs" / "Bao_cao_khoa_luan_CinemaBooking_Final_Updated_With_Technical_Appendices.docx"
EXPECTED_SOURCE_HASH = "AE70319B566303AC93E803DEEA8D0BC903E297205A518C134B0675174B5A9475"


def paragraph_text(element) -> str:
    return "".join(node.text or "" for node in element.iter(qn("w:t")))


def body_index(body, element) -> int:
    for index, child in enumerate(body):
        if child is element:
            return index
    raise ValueError("The requested document element is not in the document body")


def locate_heading(body, title: str):
    for child in body:
        if child.tag == qn("w:p") and paragraph_text(child).strip() == title:
            return child
    return None


def main() -> None:
    if not BASE_REPORT.exists():
        raise FileNotFoundError(BASE_REPORT)
    with BASE_REPORT.open("rb") as stream:
        source_hash = __import__("hashlib").sha256(stream.read()).hexdigest().upper()
    if source_hash != EXPECTED_SOURCE_HASH:
        raise RuntimeError(
            f"Official report changed unexpectedly: {source_hash}; expected {EXPECTED_SOURCE_HASH}"
        )

    target = Document(str(BASE_REPORT))
    target_body = target.element.body
    if locate_heading(target_body, "PHỤ LỤC D. GIỮ GHẾ, BOOKING VÀ RACE CONDITION") is not None:
        raise RuntimeError("Appendices D-G are already present in the official report")

    rendered = Document()
    configure_document(rendered)
    add_advanced_appendices_d_to_g(rendered)
    rendered_body = rendered.element.body
    start_title = "PHỤ LỤC D. GIỮ GHẾ, BOOKING VÀ RACE CONDITION"
    start = locate_heading(rendered_body, start_title)
    if start is None:
        raise RuntimeError("Could not locate the rendered D-G appendix block")

    start_index = body_index(rendered_body, start)
    # The standalone renderer contains only D-G; sectPr is the final body child.
    end_index = len(rendered_body) - 1
    if end_index <= start_index:
        raise RuntimeError("Invalid D-G appendix element order")

    target.add_page_break()
    insert_before = target_body.sectPr
    for child in list(rendered_body)[start_index:end_index]:
        target_body.insert(target_body.index(insert_before), deepcopy(child))

    target.save(str(OUTPUT))
    with zipfile.ZipFile(OUTPUT) as archive:
        if archive.testzip() is not None:
            raise RuntimeError("The generated DOCX archive is corrupt")
    shutil.copy2(OUTPUT, REPORT)
    print(f"Wrote {OUTPUT}")
    print(f"Updated official report {REPORT}")


if __name__ == "__main__":
    main()
