# Convierte herramientas/informe-plantilla.html en www/informe.js (la app arma el informe con esta plantilla)
import json, pathlib
aqui = pathlib.Path(__file__).parent
t = (aqui / 'informe-plantilla.html').read_text(encoding='utf-8')
(aqui.parent / 'www' / 'informe.js').write_text('// Generado por herramientas/armar-informe.py: no editar a mano\nwindow.INFORME_TPL = ' + json.dumps(t, ensure_ascii=False) + ';\n', encoding='utf-8')
print('ok', len(t))
