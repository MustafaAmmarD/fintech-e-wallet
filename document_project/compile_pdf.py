import typst
import os
import sys

os.chdir(os.path.dirname(os.path.abspath(__file__)))

print("Compiling documentation.typ to PDF...")
try:
    pdf_bytes = typst.compile("documentation.typ")
    output_path = "ewallet_documentation.pdf"
    with open(output_path, "wb") as f:
        f.write(pdf_bytes)
    print(f"SUCCESS! PDF created: {output_path}")
    print(f"Size: {len(pdf_bytes) / 1024:.1f} KB")
except Exception as e:
    print(f"ERROR: {e}")
    import traceback
    traceback.print_exc()
