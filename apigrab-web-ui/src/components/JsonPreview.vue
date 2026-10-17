<template>
  <div class="json-preview">
    <el-input
      type="textarea"
      :rows="rows"
      :value="formattedJson"
      readonly
      style="font-family: 'Courier New', monospace;"
    />
  </div>
</template>

<script>
export default {
  name: 'JsonPreview',
  props: {
    data: {
      type: [Object, Array, String],
      default: null
    },
    rows: {
      type: Number,
      default: 10
    }
  },
  computed: {
    formattedJson() {
      if (!this.data) return ''
      try {
        if (typeof this.data === 'string') {
          return JSON.stringify(JSON.parse(this.data), null, 2)
        }
        return JSON.stringify(this.data, null, 2)
      } catch {
        return typeof this.data === 'string' ? this.data : String(this.data)
      }
    }
  }
}
</script>
