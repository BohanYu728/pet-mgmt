"""Render the Word-exported QA PDF using the bundled PDFium library."""
from pathlib import Path
import pypdfium2 as pdfium
from PIL import Image, ImageOps, ImageDraw

root = Path(__file__).resolve().parents[1]
out = root / '.local/report-qa'
pdf = pdfium.PdfDocument(out / 'report.pdf')
thumbnails = []
for i in range(len(pdf)):
    page = pdf[i]
    im = page.render(scale=1.4).to_pil()
    im.save(out / f'page-{i+1}.png')
    thumb = im.copy(); thumb.thumbnail((300, 400))
    sheet = Image.new('RGB', (320, 435), 'white')
    sheet.paste(thumb, ((320-thumb.width)//2, 15))
    ImageDraw.Draw(sheet).text((12, 414), f'Page {i+1}', fill='black')
    thumbnails.append(sheet)
for group in range(0, len(thumbnails), 12):
    items = thumbnails[group:group+12]
    contact = Image.new('RGB', (1280, 435*((len(items)+3)//4)), '#b5b5b5')
    for j, item in enumerate(items): contact.paste(item, ((j%4)*320, (j//4)*435))
    contact.save(out / f'contact-{group//12+1}.png')
print(f'Rendered {len(pdf)} pages')
