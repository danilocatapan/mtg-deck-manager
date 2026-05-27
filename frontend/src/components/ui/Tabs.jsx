import { useId, useRef } from 'react'

export default function Tabs({ tabs, activeKey, onChange, label, className = '', panelClassName = '', renderPanel }) {
  const baseId = useId()
  const tabRefs = useRef([])
  const activeIndex = Math.max(0, tabs.findIndex((tab) => tab.key === activeKey))
  const activeTab = tabs[activeIndex] || tabs[0]

  function selectTab(index, focus = true) {
    const nextTab = tabs[index]
    if (!nextTab) return
    onChange(nextTab.key)
    if (focus) {
      queueMicrotask(() => tabRefs.current[index]?.focus())
    }
  }

  function handleKeyDown(event) {
    const lastIndex = tabs.length - 1
    let nextIndex = null

    if (event.key === 'ArrowRight') nextIndex = activeIndex === lastIndex ? 0 : activeIndex + 1
    if (event.key === 'ArrowLeft') nextIndex = activeIndex === 0 ? lastIndex : activeIndex - 1
    if (event.key === 'Home') nextIndex = 0
    if (event.key === 'End') nextIndex = lastIndex

    if (nextIndex !== null) {
      event.preventDefault()
      selectTab(nextIndex)
    }
  }

  if (!activeTab) return null

  return (
    <div className={`tabs-root ${className}`}>
      <div className="analysis-tabs" role="tablist" aria-label={label} onKeyDown={handleKeyDown}>
        {tabs.map((tab, index) => {
          const selected = activeTab.key === tab.key
          const tabId = `${baseId}-${tab.key}-tab`
          const panelId = `${baseId}-${tab.key}-panel`
          return (
            <button
              key={tab.key}
              ref={(node) => { tabRefs.current[index] = node }}
              id={tabId}
              type="button"
              role="tab"
              aria-selected={selected}
              aria-controls={panelId}
              tabIndex={selected ? 0 : -1}
              className={selected ? 'active' : ''}
              onClick={() => selectTab(index, false)}
            >
              {tab.label}
            </button>
          )
        })}
      </div>
      <section
        id={`${baseId}-${activeTab.key}-panel`}
        className={panelClassName}
        role="tabpanel"
        tabIndex={0}
        aria-labelledby={`${baseId}-${activeTab.key}-tab`}
      >
        {renderPanel(activeTab)}
      </section>
    </div>
  )
}
