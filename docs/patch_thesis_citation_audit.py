"""Patch citation numbering and references in the current thesis DOCX.

The patch works on the WordprocessingML package directly so unrelated document
content, fields, images, and formatting remain unchanged.
"""

from __future__ import annotations

import re
import shutil
import tempfile
import zipfile
from copy import deepcopy
from pathlib import Path

from lxml import etree


ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / (
    "Bao_cao_khoa_luan_CinemaBooking_Final_Updated_Showtime_Checkin_"
    "Before_Citation_Audit_20260818.docx"
)
OUTPUT = ROOT / "Bao_cao_khoa_luan_CinemaBooking_Final_Updated_References_Audited.docx"

DOCUMENT_XML = "word/document.xml"
DOCUMENT_RELS = "word/_rels/document.xml.rels"

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
HYPERLINK_REL_TYPE = (
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink"
)
NS = {"w": W_NS, "r": R_NS}

OLD_TO_NEW = {
    1: 2,
    2: 4,
    3: 5,
    4: 10,
    5: 11,
    6: 12,
    7: 13,
    8: 15,
    9: 16,
    10: 17,
    11: 19,
    12: 20,
}

NEW_REFERENCES = {
    1: {
        "label": (
            "[1] R. T. Fielding, Architectural Styles and the Design of "
            "Network-based Software Architectures, Doctoral dissertation, "
            "University of California, Irvine, 2000, "
        ),
        "url": "https://ics.uci.edu/~fielding/pubs/dissertation/top.htm",
        "date": "18/08/2026",
    },
    3: {
        "label": "[3] Microsoft, The TypeScript Handbook – The Basics, ",
        "url": "https://www.typescriptlang.org/docs/handbook/2/basic-types.html",
        "date": "18/08/2026",
    },
    6: {
        "label": "[6] Eclipse Foundation, Jakarta Persistence 3.2 Specification, ",
        "url": "https://jakarta.ee/specifications/persistence/3.2/",
        "date": "18/08/2026",
    },
    7: {
        "label": "[7] Spring Team, Spring Data JPA Reference Documentation, ",
        "url": "https://docs.spring.io/spring-data/jpa/reference/",
        "date": "18/08/2026",
    },
    8: {
        "label": "[8] Hibernate Team, Hibernate ORM User Guide, ",
        "url": "https://docs.hibernate.org/orm/current/userguide/html_single/Hibernate_User_Guide.html",
        "date": "18/08/2026",
    },
    9: {
        "label": "[9] PostgreSQL Global Development Group, PostgreSQL Documentation – Transactions, ",
        "url": "https://www.postgresql.org/docs/current/tutorial-transactions.html",
        "date": "18/08/2026",
    },
    14: {
        "label": (
            "[14] T. Lodderstedt, J. Bradley, A. Labunets và D. Fett, "
            "Best Current Practice for OAuth 2.0 Security, RFC 9700, "
        ),
        "url": "https://www.rfc-editor.org/rfc/rfc9700.html",
        "date": "18/08/2026",
    },
    18: {
        "label": (
            "[18] R. W. Sinnott, Virtues of the Haversine, Sky & Telescope, "
            "vol. 68, no. 2, p. 159, 1984, "
        ),
        "url": "https://ui.adsabs.harvard.edu/abs/1984S%26T....68..159S/abstract",
        "date": "18/08/2026",
    },
}


def paragraph_text(paragraph: etree._Element) -> str:
    return "".join(paragraph.xpath(".//w:t/text()", namespaces=NS))


def replace_in_text_nodes(paragraph: etree._Element, transform) -> None:
    for text_node in paragraph.xpath(".//w:t", namespaces=NS):
        if text_node.text:
            text_node.text = transform(text_node.text)


def replace_phrase(paragraph: etree._Element, old: str, new: str) -> bool:
    for text_node in paragraph.xpath(".//w:t", namespaces=NS):
        text = text_node.text or ""
        if old in text:
            text_node.text = text.replace(old, new, 1)
            return True
    raise RuntimeError(
        f"Phrase {old!r} was split across runs in paragraph: {paragraph_text(paragraph)!r}"
    )


def remap_citations(paragraph: etree._Element) -> None:
    pattern = re.compile(r"\[(\d{1,2})\]")

    def replace(text: str) -> str:
        def marker(match: re.Match[str]) -> str:
            old = int(match.group(1))
            return f"[{OLD_TO_NEW.get(old, old)}]"

        return pattern.sub(marker, text)

    replace_in_text_nodes(paragraph, replace)


def insert_after(paragraph: etree._Element, anchor: str, marker: str) -> bool:
    if marker in paragraph_text(paragraph):
        return False

    for text_node in paragraph.xpath(".//w:t", namespaces=NS):
        text = text_node.text or ""
        position = text.find(anchor)
        if position >= 0:
            end = position + len(anchor)
            text_node.text = f"{text[:end]} {marker}{text[end:]}"
            return True

    raise RuntimeError(
        f"Anchor {anchor!r} was split across runs in paragraph: {paragraph_text(paragraph)!r}"
    )


def remap_reference_number(paragraph: etree._Element) -> int:
    current = paragraph_text(paragraph)
    match = re.match(r"\[(\d{1,2})\]", current)
    if not match:
        raise RuntimeError(f"Reference number not found: {current!r}")
    old = int(match.group(1))
    new = OLD_TO_NEW[old]

    for text_node in paragraph.xpath(".//w:t", namespaces=NS):
        if text_node.text and re.match(r"\[\d{1,2}\]", text_node.text):
            text_node.text = re.sub(r"^\[\d{1,2}\]", f"[{new}]", text_node.text, count=1)
            return new

    raise RuntimeError(f"Reference prefix was split across runs: {current!r}")


def next_relationship_id(rels_root: etree._Element) -> str:
    used = {
        int(match.group(1))
        for element in rels_root
        if (match := re.fullmatch(r"rId(\d+)", element.get("Id", "")))
    }
    candidate = max(used, default=0) + 1
    while candidate in used:
        candidate += 1
    return f"rId{candidate}"


def add_hyperlink_relationship(rels_root: etree._Element, url: str) -> str:
    relationship_id = next_relationship_id(rels_root)
    relationship = etree.SubElement(
        rels_root,
        f"{{{PKG_REL_NS}}}Relationship",
        Id=relationship_id,
        Type=HYPERLINK_REL_TYPE,
        Target=url,
        TargetMode="External",
    )
    return relationship_id


def make_reference_paragraph(
    template: etree._Element,
    rels_root: etree._Element,
    label: str,
    url: str,
    access_date: str,
) -> etree._Element:
    paragraph = deepcopy(template)

    for child in list(paragraph):
        if child.tag != f"{{{W_NS}}}pPr":
            paragraph.remove(child)

    label_run = etree.SubElement(paragraph, f"{{{W_NS}}}r")
    label_text = etree.SubElement(label_run, f"{{{W_NS}}}t")
    label_text.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    label_text.text = label

    relationship_id = add_hyperlink_relationship(rels_root, url)
    hyperlink = etree.SubElement(
        paragraph,
        f"{{{W_NS}}}hyperlink",
        {f"{{{R_NS}}}id": relationship_id},
    )
    hyperlink_run = etree.SubElement(hyperlink, f"{{{W_NS}}}r")
    hyperlink_properties = etree.SubElement(hyperlink_run, f"{{{W_NS}}}rPr")
    color = etree.SubElement(hyperlink_properties, f"{{{W_NS}}}color")
    color.set(f"{{{W_NS}}}val", "0563C1")
    underline = etree.SubElement(hyperlink_properties, f"{{{W_NS}}}u")
    underline.set(f"{{{W_NS}}}val", "single")
    hyperlink_text = etree.SubElement(hyperlink_run, f"{{{W_NS}}}t")
    hyperlink_text.text = url

    date_run = etree.SubElement(paragraph, f"{{{W_NS}}}r")
    date_text = etree.SubElement(date_run, f"{{{W_NS}}}t")
    date_text.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    date_text.text = f" (truy cập ngày {access_date})."
    return paragraph


def patch_document(document_xml: bytes, relationships_xml: bytes) -> tuple[bytes, bytes]:
    parser = etree.XMLParser(remove_blank_text=False)
    document_root = etree.fromstring(document_xml, parser)
    rels_root = etree.fromstring(relationships_xml, parser)
    paragraphs = document_root.xpath(".//w:p", namespaces=NS)

    reference_start = next(
        index
        for index, paragraph in enumerate(paragraphs)
        if paragraph_text(paragraph).startswith("[1] React Team")
    )
    reference_paragraphs = paragraphs[reference_start : reference_start + 12]
    if len(reference_paragraphs) != 12:
        raise RuntimeError("Expected 12 existing bibliography entries")

    for paragraph in paragraphs[:reference_start]:
        remap_citations(paragraph)

    client_server_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("Kiến trúc client-server")
    ]
    if len(client_server_paragraphs) != 1:
        raise RuntimeError(
            f"Expected one client-server paragraph, found {len(client_server_paragraphs)}"
        )
    insert_after(client_server_paragraphs[0], "tr\u00ecnh duy\u1ec7t", "[1]")

    react_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("ReactJS ")
        and "TypeScript" in paragraph_text(paragraph)
    ]
    if len(react_paragraphs) != 1:
        raise RuntimeError(f"Expected one React/TypeScript paragraph, found {len(react_paragraphs)}")
    insert_after(react_paragraphs[0], "bi\u1ec3u m\u1eabu", "[3]")

    jpa_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("JPA ")
        and "Hibernate" in paragraph_text(paragraph)
    ]
    if len(jpa_paragraphs) != 1:
        raise RuntimeError(f"Expected one JPA definition paragraph, found {len(jpa_paragraphs)}")
    insert_after(jpa_paragraphs[0], "m\u00f4 h\u00ecnh quan h\u1ec7", "[6]")
    insert_after(jpa_paragraphs[0], "native SQL", "[7]")
    insert_after(jpa_paragraphs[0], "t\u1ea3i quan h\u1ec7", "[8]")

    transaction_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("Transaction ")
    ]
    if len(transaction_paragraphs) != 1:
        raise RuntimeError(f"Expected one transaction paragraph, found {len(transaction_paragraphs)}")
    insert_after(transaction_paragraphs[0], "ho\u00e0n t\u00e1c", "[9]")

    rest_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("REST ")
        and "URI xác định" in paragraph_text(paragraph)
    ]
    if len(rest_paragraphs) != 1:
        raise RuntimeError(f"Expected one REST paragraph, found {len(rest_paragraphs)}")
    insert_after(rest_paragraphs[0], "ng\u1eef ngh\u0129a c\u1ee7a HTTP", "[1]")

    mvc_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("Trong \u1ee9ng d\u1ee5ng web truy\u1ec1n th\u1ed1ng")
    ]
    if len(mvc_paragraphs) != 1:
        raise RuntimeError(f"Expected one MVC paragraph, found {len(mvc_paragraphs)}")
    insert_after(mvc_paragraphs[0], "Model, View v\u00e0 Controller", "[1]")

    refresh_definition_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if paragraph_text(paragraph).startswith("Refresh token ")
        and "có thời hạn dài hơn" in paragraph_text(paragraph)
    ]
    if len(refresh_definition_paragraphs) != 1:
        raise RuntimeError(
            f"Expected one refresh-token definition paragraph, found {len(refresh_definition_paragraphs)}"
        )
    refresh_definition = refresh_definition_paragraphs[0]
    replace_phrase(
        refresh_definition,
        "Refresh token c\u00f3 th\u1eddi h\u1ea1n d\u00e0i h\u01a1n n\u00ean \u0111\u01b0\u1ee3c qu\u1ea3n l\u00fd nh\u01b0 m\u1ed9t phi\u00ean \u0111\u0103ng nh\u1eadp \u1edf ph\u00eda server:",
        "Trong CinemaBooking, refresh token \u0111\u01b0\u1ee3c qu\u1ea3n l\u00fd nh\u01b0 m\u1ed9t phi\u00ean \u0111\u0103ng nh\u1eadp \u1edf ph\u00eda server:",
    )
    replace_phrase(
        refresh_definition,
        "Tr\u00ecnh duy\u1ec7t nh\u1eadn refresh token qua cookie HttpOnly v\u1edbi ph\u1ea1m vi \u0111\u01b0\u1eddng d\u1eabn h\u1ea1n ch\u1ebf; JavaScript kh\u00f4ng \u0111\u1ecdc tr\u1ef1c ti\u1ebfp gi\u00e1 tr\u1ecb n\u00e0y.",
        "Front-end kh\u00f4ng l\u01b0u refresh token trong localStorage v\u00e0 g\u1eedi y\u00eau c\u1ea7u l\u00e0m m\u1edbi v\u1edbi cookie. C\u00e1c thu\u1ed9c t\u00ednh b\u1ea3o v\u1ec7 c\u1ee7a cookie nh\u01b0 HttpOnly, SameSite v\u00e0 Secure c\u1ea7n \u0111\u01b0\u1ee3c \u0111\u1ed1i chi\u1ebfu v\u1edbi c\u1ea5u h\u00ecnh tri\u1ec3n khai th\u1ef1c t\u1ebf; v\u00ec v\u1eady, b\u00e1o c\u00e1o kh\u00f4ng xem \u0111\u00e2y l\u00e0 \u0111\u1eb7c t\u00ednh \u0111\u00e3 \u0111\u01b0\u1ee3c x\u00e1c minh ch\u1ec9 t\u1eeb m\u00e3 ngu\u1ed3n client.",
    )
    replace_phrase(
        refresh_definition,
        "Thi\u1ebft k\u1ebf k\u1ebft h\u1ee3p hai lo\u1ea1i token c\u00e2n b\u1eb1ng gi\u1eefa kh\u1ea3 n\u0103ng m\u1edf r\u1ed9ng c\u1ee7a JWT v\u00e0 y\u00eau c\u1ea7u qu\u1ea3n l\u00fd phi\u00ean, thu h\u1ed3i th\u00f4ng tin \u0111\u0103ng nh\u1eadp.",
        "C\u00e1ch k\u1ebft h\u1ee3p hai lo\u1ea1i token nh\u1eb1m c\u00e2n b\u1eb1ng kh\u1ea3 n\u0103ng x\u00e1c th\u1ef1c request v\u1edbi y\u00eau c\u1ea7u qu\u1ea3n l\u00fd v\u00e0 thu h\u1ed3i phi\u00ean \u0111\u0103ng nh\u1eadp.",
    )
    insert_after(refresh_definition, "l\u00e0m m\u1edbi", "[14]")

    rotation_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if "C\u01a1 ch\u1ebf refresh token rotation" in paragraph_text(paragraph)
    ]
    if len(rotation_paragraphs) != 1:
        raise RuntimeError(f"Expected one refresh rotation paragraph, found {len(rotation_paragraphs)}")
    insert_after(rotation_paragraphs[0], "t\u00e1i s\u1eed d\u1ee5ng", "[14]")

    geolocation_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if "Browser Geolocation API" in paragraph_text(paragraph)
    ]
    if len(geolocation_paragraphs) != 2:
        raise RuntimeError(
            f"Expected two Browser Geolocation paragraphs, found {len(geolocation_paragraphs)}"
        )
    for paragraph in geolocation_paragraphs:
        insert_after(paragraph, "Browser Geolocation API", "[17]")

    haversine_paragraphs = [
        paragraph
        for paragraph in paragraphs[:reference_start]
        if "Haversine" in paragraph_text(paragraph) and "6.371 km" in paragraph_text(paragraph)
    ]
    if len(haversine_paragraphs) != 2:
        raise RuntimeError(
            f"Expected two Haversine radius paragraphs, found {len(haversine_paragraphs)}"
        )
    for paragraph in haversine_paragraphs:
        insert_after(paragraph, "Haversine", "[18]")
        replace_phrase(paragraph, "b\u00e1n k\u00ednh Tr\u00e1i \u0110\u1ea5t", "h\u1eb1ng s\u1ed1 b\u00e1n k\u00ednh")

    references_by_number: dict[int, etree._Element] = {}
    for paragraph in reference_paragraphs:
        references_by_number[remap_reference_number(paragraph)] = paragraph

    template = reference_paragraphs[0]
    for number, source in NEW_REFERENCES.items():
        references_by_number[number] = make_reference_paragraph(
            template,
            rels_root,
            source["label"],
            source["url"],
            source["date"],
        )

    if sorted(references_by_number) != list(range(1, 21)):
        raise RuntimeError(f"Invalid bibliography sequence: {sorted(references_by_number)}")

    parent = reference_paragraphs[0].getparent()
    insertion_index = parent.index(reference_paragraphs[0])
    for paragraph in reference_paragraphs:
        parent.remove(paragraph)
    for offset, number in enumerate(range(1, 21)):
        parent.insert(insertion_index + offset, references_by_number[number])

    xml_declaration = b'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
    return (
        xml_declaration + etree.tostring(document_root, encoding="UTF-8"),
        xml_declaration + etree.tostring(rels_root, encoding="UTF-8"),
    )


def main() -> None:
    if not SOURCE.exists():
        raise FileNotFoundError(SOURCE)

    with zipfile.ZipFile(SOURCE, "r") as source_zip:
        document_xml, relationships_xml = patch_document(
            source_zip.read(DOCUMENT_XML), source_zip.read(DOCUMENT_RELS)
        )

        with tempfile.NamedTemporaryFile(
            prefix="thesis-citations-", suffix=".docx", delete=False, dir=OUTPUT.parent
        ) as temporary_file:
            temporary_path = Path(temporary_file.name)

        try:
            with zipfile.ZipFile(temporary_path, "w") as output_zip:
                for item in source_zip.infolist():
                    payload = source_zip.read(item.filename)
                    if item.filename == DOCUMENT_XML:
                        payload = document_xml
                    elif item.filename == DOCUMENT_RELS:
                        payload = relationships_xml
                    output_zip.writestr(item, payload)
            shutil.move(temporary_path, OUTPUT)
        finally:
            temporary_path.unlink(missing_ok=True)

    print(OUTPUT)


if __name__ == "__main__":
    main()
