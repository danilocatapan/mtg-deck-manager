import { expect, test } from '@playwright/test'

const SEMANTIC_PAIRS = [
  ['text on background', '#F2E6C8', '#06120C', 7],
  ['muted on background', '#B8AA8A', '#06120C', 4.5],
  ['value on surface', '#FFE7A3', '#0D1B12', 7],
  ['danger text on danger surface', '#FFE1D8', '#3A1712', 4.5],
  ['warning text on dark surface', '#FFE7A3', '#0D1B12', 7],
  ['good status on dark surface', '#8FCB6B', '#0D1B12', 4.5],
  ['owned pill text on dark surface', '#BAE6FD', '#0D1B12', 4.5],
]

test('semantic color pairs keep AA contrast or better', () => {
  const failures = SEMANTIC_PAIRS
    .map(([name, foreground, background, minimum]) => {
      const ratio = contrastRatio(foreground, background)
      return { name, ratio, minimum }
    })
    .filter((item) => item.ratio < item.minimum)

  expect(
    failures,
    failures.map((item) => `${item.name}: ${item.ratio.toFixed(2)} < ${item.minimum}`).join('\n'),
  ).toEqual([])
})

function contrastRatio(foreground, background) {
  const fg = relativeLuminance(hexToRgb(foreground))
  const bg = relativeLuminance(hexToRgb(background))
  const lighter = Math.max(fg, bg)
  const darker = Math.min(fg, bg)
  return (lighter + 0.05) / (darker + 0.05)
}

function relativeLuminance([r, g, b]) {
  return [r, g, b]
    .map((channel) => {
      const value = channel / 255
      return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4
    })
    .reduce((sum, value, index) => sum + value * [0.2126, 0.7152, 0.0722][index], 0)
}

function hexToRgb(hex) {
  const value = hex.replace('#', '')
  return [0, 2, 4].map((offset) => Number.parseInt(value.slice(offset, offset + 2), 16))
}
