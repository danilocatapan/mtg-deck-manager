#!/usr/bin/env node
import fs from 'node:fs/promises';
import path from 'node:path';

const args = process.argv.slice(2);
const commander = args[args.indexOf('--commander') + 1];
const slug = args[args.indexOf('--slug') + 1];

if (!commander || !slug) {
  console.error('Usage: node import-edhrec.mjs --commander "Name" --slug commander-slug');
  process.exit(1);
}

// Placeholder: implementar scraping/parsing EDHREC de forma controlada e resiliente.
// 1) Buscar página do comandante
// 2) Extrair top cards e taxa de inclusão
// 3) Normalizar nomes (ex.: A- prefix)
// 4) Salvar JSON no formato esperado pelo backend

const outDir = path.resolve('backend/src/main/resources/meta/commanders');
await fs.mkdir(outDir, { recursive: true });
const outFile = path.join(outDir, `${slug}.json`);

const template = {
  commander,
  colors: [],
  cards: []
};

await fs.writeFile(outFile, `${JSON.stringify(template, null, 2)}\n`, 'utf8');
console.log(`Template generated: ${outFile}`);
