<template>
  <div ref="containerRef" class="text-preview-pane"></div>
</template>

<script setup lang="ts">
import { EditorState } from '@codemirror/state'
import { EditorView, lineNumbers } from '@codemirror/view'
import { defaultHighlightStyle, syntaxHighlighting } from '@codemirror/language'
import { markdown } from '@codemirror/lang-markdown'
import { json } from '@codemirror/lang-json'
import { html } from '@codemirror/lang-html'
import { css } from '@codemirror/lang-css'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { java } from '@codemirror/lang-java'
import { sql } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  content: string
  extension?: string
  dark?: boolean
}>()

const containerRef = ref<HTMLElement | null>(null)
let editorView: EditorView | null = null

const getLanguageExtension = () => {
  switch ((props.extension || '').toLowerCase()) {
    case 'md': return markdown()
    case 'json': return json()
    case 'html':
    case 'xml': return html()
    case 'css': return css()
    case 'js':
    case 'ts': return javascript()
    case 'py': return python()
    case 'java': return java()
    case 'sql': return sql()
    default: return null
  }
}

const mountEditor = () => {
  if (!containerRef.value) return
  editorView?.destroy()
  const languageExtension = getLanguageExtension()
  editorView = new EditorView({
    state: EditorState.create({
      doc: props.content || '',
      extensions: [
        lineNumbers(),
        syntaxHighlighting(defaultHighlightStyle),
        EditorView.editable.of(false),
        EditorState.readOnly.of(true),
        EditorView.lineWrapping,
        ...(languageExtension ? [languageExtension] : []),
        ...(props.dark ? [oneDark] : [])
      ]
    }),
    parent: containerRef.value
  })
}

onMounted(mountEditor)
watch(() => [props.content, props.extension, props.dark], mountEditor)
onBeforeUnmount(() => editorView?.destroy())
</script>

<style scoped>
.text-preview-pane {
  width: 100%;
  min-height: 420px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
</style>
