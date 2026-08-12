export type SelectedElementInfo = {
  tagName: string
  selector: string
  id?: string
  className?: string
  text?: string
  attributes?: Record<string, string>
}

export type VisualEditorController = {
  enable: () => void
  disable: () => void
  clearSelection: () => void
  destroy: () => void
}

type VisualEditorOptions = {
  iframe: HTMLIFrameElement
  onSelect: (info: SelectedElementInfo) => void
}

const SELECT_EVENT_TYPE = 'visual-editor:element-selected'
const STYLE_ID = 'visual-editor-style'

type VisualEditorWindow = Window & {
  __visualEditorEnable?: () => void
  __visualEditorDisable?: () => void
  __visualEditorClearSelection?: () => void
  __visualEditorCleanup?: () => void
}

const injectedScript = `
(function () {
  const SELECT_EVENT_TYPE = '${SELECT_EVENT_TYPE}';
  const STYLE_ID = '${STYLE_ID}';
  const HOVER_CLASS = 'visual-editor-hover';
  const SELECTED_CLASS = 'visual-editor-selected';
  const ENABLED_ATTR = 'data-visual-editor-enabled';
  const IGNORE_TAGS = new Set(['html', 'body', 'head', 'meta', 'link', 'style', 'script', 'title']);
  let hoverElement = null;
  let selectedElement = null;
  let enabled = false;

  function cssEscape(value) {
    if (window.CSS && typeof window.CSS.escape === 'function') {
      return window.CSS.escape(value);
    }
    return String(value).replace(/([ #;?%&,.+*~\\':"!^$[\\]()=>|/@])/g, '\\\\$1');
  }

  function getClassName(element) {
    if (!element || typeof element.className !== 'string') {
      return '';
    }
    return element.className
      .split(/\\s+/)
      .filter(function (name) {
        return name && name !== HOVER_CLASS && name !== SELECTED_CLASS;
      })
      .join(' ');
  }

  function getSelector(element) {
    if (!element || element.nodeType !== 1) {
      return '';
    }
    if (element.id) {
      return '#' + cssEscape(element.id);
    }
    const parts = [];
    let current = element;
    while (current && current.nodeType === 1 && current !== document.body) {
      const tag = current.tagName.toLowerCase();
      let index = 1;
      let previous = current.previousElementSibling;
      while (previous) {
        if (previous.tagName.toLowerCase() === tag) {
          index += 1;
        }
        previous = previous.previousElementSibling;
      }
      parts.unshift(tag + ':nth-of-type(' + index + ')');
      current = current.parentElement;
    }
    return parts.join(' > ');
  }

  function getAttributes(element) {
    const names = ['href', 'src', 'alt', 'title', 'aria-label', 'placeholder'];
    return names.reduce(function (result, name) {
      const value = element.getAttribute(name);
      if (value) {
        result[name] = value;
      }
      return result;
    }, {});
  }

  function getInfo(element) {
    const text = (element.innerText || element.textContent || '').replace(/\\s+/g, ' ').trim();
    return {
      tagName: element.tagName.toLowerCase(),
      selector: getSelector(element),
      id: element.id || undefined,
      className: getClassName(element) || undefined,
      text: text ? text.slice(0, 160) : undefined,
      attributes: getAttributes(element),
    };
  }

  function isEditableTarget(element) {
    if (!element || element.nodeType !== 1) {
      return false;
    }
    return !IGNORE_TAGS.has(element.tagName.toLowerCase());
  }

  function setHover(element) {
    if (hoverElement && hoverElement !== selectedElement) {
      hoverElement.classList.remove(HOVER_CLASS);
    }
    hoverElement = element;
    if (hoverElement && hoverElement !== selectedElement) {
      hoverElement.classList.add(HOVER_CLASS);
    }
  }

  function handleMouseOver(event) {
    if (!enabled || !isEditableTarget(event.target)) {
      return;
    }
    setHover(event.target);
  }

  function handleMouseOut(event) {
    if (!enabled || event.target !== hoverElement || hoverElement === selectedElement) {
      return;
    }
    hoverElement.classList.remove(HOVER_CLASS);
    hoverElement = null;
  }

  function handleClick(event) {
    if (!enabled || !isEditableTarget(event.target)) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    if (selectedElement) {
      selectedElement.classList.remove(SELECTED_CLASS);
    }
    selectedElement = event.target;
    selectedElement.classList.remove(HOVER_CLASS);
    selectedElement.classList.add(SELECTED_CLASS);
    window.parent.postMessage({
      type: SELECT_EVENT_TYPE,
      payload: getInfo(selectedElement),
    }, '*');
  }

  function ensureStyle() {
    if (document.getElementById(STYLE_ID)) {
      return;
    }
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = [
      '.' + HOVER_CLASS + ' { outline: 2px dashed #1677ff !important; outline-offset: 2px !important; cursor: crosshair !important; }',
      '.' + SELECTED_CLASS + ' { outline: 3px solid #0958d9 !important; outline-offset: 3px !important; box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.18) !important; }',
      'body[' + ENABLED_ATTR + '="true"] * { cursor: crosshair !important; }'
    ].join('\\n');
    document.head.appendChild(style);
  }

  function enable() {
    ensureStyle();
    enabled = true;
    document.body && document.body.setAttribute(ENABLED_ATTR, 'true');
    document.addEventListener('mouseover', handleMouseOver, true);
    document.addEventListener('mouseout', handleMouseOut, true);
    document.addEventListener('click', handleClick, true);
  }

  function clearSelection() {
    if (hoverElement) {
      hoverElement.classList.remove(HOVER_CLASS);
      hoverElement = null;
    }
    if (selectedElement) {
      selectedElement.classList.remove(SELECTED_CLASS);
      selectedElement = null;
    }
  }

  function disable() {
    enabled = false;
    document.body && document.body.removeAttribute(ENABLED_ATTR);
    document.removeEventListener('mouseover', handleMouseOver, true);
    document.removeEventListener('mouseout', handleMouseOut, true);
    document.removeEventListener('click', handleClick, true);
    clearSelection();
  }

  window.__visualEditorEnable = enable;
  window.__visualEditorDisable = disable;
  window.__visualEditorClearSelection = clearSelection;
  window.__visualEditorCleanup = disable;
})();
`

const getFrameWindow = (iframe: HTMLIFrameElement) =>
  iframe.contentWindow as VisualEditorWindow | null

const injectVisualEditor = (iframe: HTMLIFrameElement) => {
  const frameWindow = getFrameWindow(iframe)
  const frameDocument = iframe.contentDocument
  if (!frameWindow || !frameDocument) {
    return
  }
  if (frameWindow.__visualEditorEnable) {
    return
  }
  const script = frameDocument.createElement('script')
  script.textContent = injectedScript
  frameDocument.body.appendChild(script)
  script.remove()
}

export const createVisualEditor = ({ iframe, onSelect }: VisualEditorOptions): VisualEditorController => {
  const handleMessage = (event: MessageEvent) => {
    if (event.source !== iframe.contentWindow || event.data?.type !== SELECT_EVENT_TYPE) {
      return
    }
    onSelect(event.data.payload as SelectedElementInfo)
  }

  window.addEventListener('message', handleMessage)

  return {
    enable: () => {
      injectVisualEditor(iframe)
      getFrameWindow(iframe)?.__visualEditorEnable?.()
    },
    disable: () => {
      getFrameWindow(iframe)?.__visualEditorDisable?.()
    },
    clearSelection: () => {
      getFrameWindow(iframe)?.__visualEditorClearSelection?.()
    },
    destroy: () => {
      getFrameWindow(iframe)?.__visualEditorCleanup?.()
      window.removeEventListener('message', handleMessage)
    },
  }
}

export const formatSelectedElementSummary = (info: SelectedElementInfo) => {
  const id = info.id ? `#${info.id}` : ''
  const className = info.className ? `.${info.className.split(/\s+/).join('.')}` : ''
  return `${info.tagName}${id}${className}`
}

export const formatSelectedElementPrompt = (info: SelectedElementInfo) => {
  const attributes = info.attributes && Object.keys(info.attributes).length
    ? JSON.stringify(info.attributes)
    : '无'
  return [
    '',
    '',
    `- 选中元素：${formatSelectedElementSummary(info)}`,
    `- CSS 选择器：${info.selector}`,
    `- 元素文本：${info.text || '无'}`,
    `- 元素属性：${attributes}`,
  ].join('\n')
}
