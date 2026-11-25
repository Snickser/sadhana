#!/usr/bin/env python3
"""Печатает строки из пула бинарного XML внутри APK — чтобы видеть ТОЧНОЕ значение
android:text, без форматирования, которое добавляет `aapt2 dump xmltree`.

Использование: python3 dump_xml_strings.py <apk> <путь-внутри-apk>
"""
import struct
import sys
import zipfile


def read_string_pool(data, offset):
    # ResChunk_header: type(2) headerSize(2) size(4)
    ctype, header_size, size = struct.unpack_from("<HHI", data, offset)
    if ctype != 0x0001:  # RES_STRING_POOL_TYPE
        raise ValueError("не пул строк, тип 0x%04x" % ctype)
    string_count, style_count, flags, strings_start, styles_start = struct.unpack_from(
        "<IIIII", data, offset + 8)
    utf8 = bool(flags & 0x100)
    offsets = struct.unpack_from("<%dI" % string_count, data, offset + header_size)
    base = offset + strings_start
    out = []
    for off in offsets:
        p = base + off
        if utf8:
            # u16len (модифицированный UTF-8), затем u8len, затем байты
            n = data[p]
            p += 2 if n & 0x80 else 1
            n = data[p]
            if n & 0x80:
                p += 1
                n = ((n & 0x7F) << 8) | data[p]
            p += 1
            out.append(data[p:p + n].decode("utf-8", "replace"))
        else:
            n = struct.unpack_from("<H", data, p)[0]
            p += 2
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, p)[0]
                p += 2
            out.append(data[p:p + n * 2].decode("utf-16-le", "replace"))
    return out


def main():
    apk, member = sys.argv[1], sys.argv[2]
    data = zipfile.ZipFile(apk).read(member)
    ctype, header_size, size = struct.unpack_from("<HHI", data, 0)
    if ctype != 0x0003:  # RES_XML_TYPE
        raise ValueError("не бинарный XML, тип 0x%04x" % ctype)
    strings = read_string_pool(data, header_size)
    for s in strings:
        print(repr(s))


if __name__ == "__main__":
    main()
