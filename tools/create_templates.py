#!/usr/bin/env python3
"""Create dependency-free Excel import templates. Codes are explicitly text cells."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]

def write(name, rows):
    cells = []
    for r, row in enumerate(rows, 1):
        items = ''.join(f'<c r="{chr(65+c)}{r}" t="inlineStr"><is><t>{escape(value)}</t></is></c>'
                        for c, value in enumerate(row))
        cells.append(f'<row r="{r}">{items}</row>')
    with ZipFile(ROOT / 'vorlagen' / name, 'w', ZIP_DEFLATED) as z:
        z.writestr('[Content_Types].xml', '''<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>''')
        z.writestr('_rels/.rels', '''<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>''')
        z.writestr('xl/workbook.xml', '''<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Import" sheetId="1" r:id="rId1"/></sheets></workbook>''')
        z.writestr('xl/_rels/workbook.xml.rels', '''<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>''')
        z.writestr('xl/worksheets/sheet1.xml', '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cols><col min="1" max="5" width="24" customWidth="1"/></cols><sheetData>' + ''.join(cells) + '</sheetData></worksheet>')

if __name__ == '__main__':
    write('Material-Import.xlsx', [
        ['Scancode', 'Mat Nummer', 'Beschreibung', 'Art', 'Einheit'],
        ['0001234567890', 'MAT-001', 'Beispiel Ventil', 'Material', 'Stk'],
        ['0001234567891', 'KM-032', 'Beispiel R32', 'Kältemittel', 'kg'],
    ])
    write('Personen-Import.xlsx', [
        ['Personencode', 'Name'], ['0007', 'Erika Muster'],
    ])
